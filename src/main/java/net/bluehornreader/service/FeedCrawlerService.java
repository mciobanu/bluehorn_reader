package net.bluehornreader.service;

import net.bluehornreader.*;
import net.bluehornreader.data.*;
import net.bluehornreader.model.*;
import org.apache.commons.logging.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-05-03
 * Time: 20:45
 * <p/>
 *
 * There are instances of CrawlingThread which do the actual crawling and store the results in articlesToBeSaved, without going to the DB <p/>
 *
 * Saving the articles from articlesToBeSaved to the DB happens on the main thread <p/>
 *
 * The main thread runs an infinite loop in which it saves new articles (if there are any) and periodically executes a "tick", which does several things:
 *
 * <ul> Increments crawlers.crawl_tick, so FeedManager can see that this crawler is alive </ul>
 *
 * <ul> Checks crawlers.feed_ids_seq to determine if its feed list has been updated (a probably needless optimization.) If it was, it adjusts
 * feedMap and availableFeeds accordingly </ul>
 *
 * <ul> Looks in the election table to see if the feed manager is alive </ul>
 *
 *
 * <p/>
 */
public class FeedCrawlerService extends Service {

    private static final Log LOG = LogFactory.getLog(FeedCrawlerService.class);

    // feedMap holds all the feeds for this crawler, while availableFeeds hold the feeds that are not being updated; feeds get removed from availableFeeds
    //  for the time interval when they are crawled
    private HashMap<String, FeedInfo> feedMap;  // access to this must be synchronized for search, iterate, add, remove, ...
    private FeedInfoComparator feedInfoComparator = new FeedInfoComparator();
    private PriorityQueue<FeedInfo> availableFeeds = new PriorityQueue<FeedInfo>(10, feedInfoComparator);  // access to this must be synchronized for search, iterate, add, remove, ...

    private Crawler crawler;
    private Crawler.DB crawlerDb;
    private Feed.DB feedDb;
    private Article.DB articleDb;
    private Election.DB electionDb;

    private ArrayDeque<Article> articlesToBeSaved = new ArrayDeque<Article>();

    private boolean shouldExit = false;
    private ArrayDeque<Election> lastElections = new ArrayDeque<Election>();

    private long nextTick;
    //private int seq;
    private ArrayList<Thread> crawlingRunnables = new ArrayList<Thread>();



    FeedCrawlerService(LowLevelDbAccess lowLevelDbAccess) throws Exception {
        crawlerDb = new Crawler.DB(lowLevelDbAccess);
        crawler = crawlerDb.getCrawler(IP);
        while (crawler == null) {
            synchronized (this) {
                wait(1000);
            }
            crawlerDb.updateCrawl(IP, 0);
            crawler = crawlerDb.getCrawler(IP);
        }
        crawler.feedIdsSeq = -1; // to force feeds to be read
        feedDb = new Feed.DB(lowLevelDbAccess);
        articleDb = new Article.DB(lowLevelDbAccess);
        electionDb = new Election.DB(lowLevelDbAccess);

        for (int i = 0; i < Config.getConfig().threadsPerCrawler; ++i) {
            Thread t = new CrawlingThread();
            t.setDaemon(true);
            t.setName(String.format("CrawlingThread/%s/%s", IP, i));
            crawlingRunnables.add(t);
            t.start();
        }
    }


    /**
     * To be called by the ServiceManager
     */
    synchronized public void signalExit() {
        shouldExit = true;
        notify();
    }


    @Override
    protected Collection<Thread> getChildThreads() {
        return crawlingRunnables;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                if (System.currentTimeMillis() >= nextTick) {
                    tick();
                }

                for (;;) {
                    if (shouldExit()) {
                        break;
                    }

                    long left = nextTick - System.currentTimeMillis();
                    if (left < Config.getConfig().getCrawlerTickInterval() / 2) {
                        break;
                    }
                    if (saveArticleBatch()) {
                        long waitBetweenSaves = Config.getConfig().crawlerWaitBetweenSaves;
                        if (waitBetweenSaves > 0) {
                            synchronized (this) {
                                wait(waitBetweenSaves);
                            }
                        }
                    } else {
                        break;
                    }
                }

                if (shouldExit()) {
                    break;
                }

                long left = nextTick - System.currentTimeMillis();
                if (left > 0) {
                    synchronized (this) {
                        wait(Utils.getRandomDuration(left, 0.9, 1.0));
                    }
                }

            } catch (Exception e) {
                LOG.info("Exception running crawling service", e);
                try {
                    synchronized (this) {
                        wait(10000);
                    }
                } catch (InterruptedException e1) {
                    LOG.error("Exception sleeping after exception in crawling service " + IP, e1);
                }
            }
        }

        LOG.info(String.format("FeedCrawlerService %s exiting", IP));
    }


    /**
     * Saves articles in the same feed to the DB. Should be called again to save articles from a different feed
     *
     * @return true if it saved anything
     * @throws Exception
     */
    private boolean saveArticleBatch() throws Exception {

        ArrayList<Article> articles = new ArrayList<Article>();
        String feedId;
        synchronized (this) {
            if (articlesToBeSaved.isEmpty()) {
                return false;
            }
            feedId = articlesToBeSaved.peek().feedId;
            while (!articlesToBeSaved.isEmpty()) {
                String feedId1 = articlesToBeSaved.peek().feedId;
                if (feedId1.equals(feedId)) {
                    if (feedMap.containsKey(feedId)) { // don't save articles in feeds that this crawler no longer processes
                        articles.add(articlesToBeSaved.poll());
                    } else {
                        articlesToBeSaved.poll();
                    }
                } else {
                    break;
                }
            }
        }

        if (articles.isEmpty()) {
            return false;
        }

        FeedInfo feedInfo = feedMap.get(feedId);
        if (feedInfo != null) {
            articleDb.add(articles);
            feedDb.update(feedId, articles.get(articles.size() - 1).seq);
            LOG.info(String.format("saved %d articles for feed %s", articles.size(), feedId));
        } else {
            LOG.warn(String.format("Not saving articles for feed %s, which is no longer handled by this crawler", feedId));
        }

        return true;
    }

    synchronized private void tick() throws Exception {
        nextTick = System.currentTimeMillis() + Config.getConfig().getCrawlerTickInterval();
        crawlerDb.updateCrawl(IP, incSeq());
        updateFeedList();
        Election election = electionDb.get(FeedManagerService.ELECTION_ID);
        lastElections.add(election);
        while (lastElections.size() > Config.getConfig().feedManagerTicksBeforeBecomingLeader) {
            lastElections.pollFirst();
        }
    }


    private static class FeedInfo {
        Feed feed;
        int sinceTick; // the tick of this crawler when this feed was added
        int startedUpdateAtTick;
        int finishedUpdateAtTick;

        private FeedInfo(Feed feed, int sinceTick) {
            this.feed = feed;
            this.sinceTick = sinceTick;
        }

        @Override
        public String toString() {
            return "FeedInfo{" +
                    "feed=" + feed +
                    ", sinceTick=" + sinceTick +
                    ", startedUpdateAtTick=" + startedUpdateAtTick +
                    ", finishedUpdateAtTick=" + finishedUpdateAtTick +
                    '}';
        }
    }

    private static class FeedInfoComparator implements Comparator<FeedInfo> {

        @Override
        public int compare(FeedInfo o1, FeedInfo o2) {
            return o1.startedUpdateAtTick - o2.startedUpdateAtTick;
        }
    }


    /**
     * @return true if there are enough ticks for the feed manager and all the ticks are different and coming from the same manager
     */
    synchronized private boolean isFeedManagerAlive() {
        int n = Config.getConfig().feedManagerTicksBeforeBecomingLeader;
        LOG.debug("Elections: " + PrintUtils.asString(lastElections));
        if (lastElections.size() < n) {
            return false;
        }
        Election prev = null;
        for (Election election : lastElections) {
            if (prev != null &&
                    (prev.tick == election.tick || !prev.leaderId.equals(election.leaderId))) {
                return false;
            }
            prev = election;
        }
        return true;
    }


    /**
     * @return the id of the next feed to be crawled; might return null if all feeds have been crawled recently enough
     */
    synchronized private String getNextFeed() {
        LOG.debug("availableFeeds " + PrintUtils.asString(availableFeeds));
        if (!isFeedManagerAlive()) {
            return null;
        }
        if (availableFeeds.isEmpty()) {
            return null;
        }
        if (availableFeeds.peek().startedUpdateAtTick + Config.getConfig().getTicksBetweenCrawls() > crawler.crawlTick) {
            return null; // not enough time passed since last crawl
        }
        return availableFeeds.poll().feed.feedId;
    }


    synchronized private int getSeq() {
        return crawler.crawlTick;
    }

    synchronized private int incSeq() {
        return ++crawler.crawlTick;
    }

    /**
     * Doesn't do anything if the feed has already been removed
     * @param feedId
     */
    synchronized private void setLastCrawl(String feedId, int startedUpdateAtTick) {
        FeedInfo feedInfo = feedMap.get(feedId);
        if (feedInfo != null) {
            feedInfo.startedUpdateAtTick = startedUpdateAtTick;
            feedInfo.finishedUpdateAtTick = getSeq();
            availableFeeds.add(feedInfo);
        }
    }


    /**
     * If there's any change, it deletes all previous feeds. Whould be nicer to keep what already exists but not sure it's worth it
     *
     * @throws Exception
     */
    private void updateFeedList() throws Exception {

        int feedIdsSeq = crawlerDb.getFeedIdsSeq(crawler.crawlerId);
        if (crawler.feedIdsSeq == feedIdsSeq) {
            return;
        }

        HashMap<String, FeedInfo> newFeedMap = new HashMap<String, FeedInfo>();
        PriorityQueue<FeedInfo> newAvailableFeedList = new PriorityQueue<FeedInfo>(1, feedInfoComparator);
        Crawler newCrawler = crawlerDb.getCrawler(crawler.crawlerId);

        for (String feedId : newCrawler.feedIds) {
            Feed feed = feedDb.get(feedId);
            if (feed == null) {
                LOG.warn(String.format("FeedCrawlerService %s was asked to crawl feed %s but couldn't find such a feed", IP, feedId));
            } else {
                FeedInfo feedInfo = new FeedInfo(feed, getSeq());
                newAvailableFeedList.add(feedInfo);
                newFeedMap.put(feedId, feedInfo);
            }
        }

        synchronized (this) {
            crawler.feedIdsSeq = feedIdsSeq;
            feedMap = newFeedMap;
            availableFeeds = newAvailableFeedList;
        }
    }

    //ttt2 See how to deal with feed updates, when the ID is kept but the URL is changed. FeedManagerService should detect this and force the crawler to
    // restart. This is important if the crawler code in updateFeedList() is changed to keep old entries

    synchronized private boolean shouldExit() {
        return shouldExit;
    }

    private class CrawlingThread extends Thread {

        private RssParser rssParser = new RssParser();
		
        /*
        Extending Thread rather than implementing Runnable so we can call notify() on this class. (Once a Runnable is passed to a Thread on the
        constructor there seems to be no way to get it, so we'd have to keep the pairs of <Thread, Runnable> to be able to see if a thread is
        still alive and also to send a notify() to the underlying object.)
         */

        @Override
        public void run() {
            LOG.info("Starting crawler " + Thread.currentThread().getName());
            for (;;) {
                if (shouldExit()) {
                    LOG.info("Exiting crawler " + Thread.currentThread().getName());
                    return;
                }

                int startedUpdateAtTick = getSeq();
                String feedId = "N/A";
                try {
                    feedId = getNextFeed();
                    if (feedId == null) {
                        long sleep = Config.getConfig().sleepWhenNothingToCrawl;
                        LOG.info("Found no new feed to crawl. Sleeping for around " + Utils.durationToString(sleep));
                        sleep = Utils.getRandomDuration(sleep, 0.9, 1.1);
                        synchronized (this) {
                            wait(sleep);
                        }
                        continue;
                    }
                    crawlFeed(feedId);
                    synchronized (FeedCrawlerService.this) {
                        FeedCrawlerService.this.notify();
                    }
                } catch (Exception e) { //ttt1 catch Throwable? then what?
                    LOG.error("Exception crawling feed " + feedId, e);
                    try {
                        synchronized (this) {
                            wait(10000);
                        }
                    } catch (InterruptedException e1) {
                        LOG.error("Exception sleeping after exception crawling feed " + feedId, e1);
                    }
                } finally {
                    if (feedId != null) {
                        setLastCrawl(feedId, startedUpdateAtTick); // even if there was an exception, we tried; this also makes the feed available for
                            // crawling again; otherwise it would be lost until the crawler is restarted, as nothing else would add it to availableFeeds
                            // (we could force FeedManagerService to check that all feeds have been updated, but that's quite fragile); this looks like
                            // it should work fine
                    }
                }
            }
        }

        private void crawlFeed(String feedId) throws Exception {
            LOG.debug("crawling " + feedId);

            Feed feed = feedDb.get(feedId);
            if (feed == null) {
                return;
            }
            long earliestToInclude = 0;
            if (feed.maxSeq >= 0) {
                Article article = articleDb.get(feedId, feed.maxSeq);
                earliestToInclude = article.publishTime + 1;
            }
            ArrayList<Article> articles = rssParser.parseRdf(feed.url, feedId, earliestToInclude);
            for (Article article : articles) {
                article.seq = ++feed.maxSeq;
            }

            synchronized (FeedCrawlerService.this) {
                articlesToBeSaved.addAll(articles);
            }
        }
    }
}

