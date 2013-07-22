package net.bluehornreader.service;

import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;
import net.bluehornreader.model.*;
import org.apache.commons.logging.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-05-06
 * Time: 01:09
 * <p/>
 *
 * Assigns feeds to Crawlers, by periodically inspecting the feed and the crawler lists and determining which feeds are new / removed
 * or which crawlers are alive / dead.
 *
 * It doesn't do much on its thread; merely starts a LeaderElector
 */
public class FeedManagerService extends Service {

    private static final Log LOG = LogFactory.getLog(FeedManagerService.class);

    public static final String ELECTION_ID = "feed_manager";
    private ArrayList<Feed> feeds = new ArrayList<>();

    private Election.DB electionDb;
    private Feed.DB feedDb;
    private Crawler.DB crawlerDb;
    private HashMap<String, ArrayList<String>> feedMap = new HashMap<>();
    private HashMap<String, Integer> crawlerSeq = new HashMap<>();
    private long nexFeedDistribution;
    private LeaderElector leaderElector;


    FeedManagerService(LowLevelDbAccess lowLevelDbAccess) {
        electionDb = new Election.DB(lowLevelDbAccess);
        feedDb = new Feed.DB(lowLevelDbAccess);
        crawlerDb = new Crawler.DB(lowLevelDbAccess);
        setName("FeedManagerService-main/" + IP);
    }

    @Override
    public synchronized void signalExit() {
        LOG.info(String.format("FeedManagerService %s received exit request", IP));
        leaderElector.signalExit();
    }

    @Override
    protected Collection<Thread> getChildThreads() {
        return new ArrayList<>(); // there are no secondary threads for now (the leader election is joined, so it doesn't matter)
    }

    /**
     * Just creates the leader elector and lets it take over
     */
    @Override
    public void run() {
        LOG.info("Starting LeaderElector ...");
        leaderElector = new LeaderElector(Config.getConfig().feedManagerTicksBeforeBecomingLeader, Config.getConfig().feedManagerTickInterval);
        Thread t = new Thread(leaderElector);
        t.setDaemon(true);
        t.setName("FeedManagerService/" + IP);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            LOG.error("Exception running LeaderElector", e);
        }
        LOG.info(String.format("FeedManagerService %s exiting", IP));
    }


    /**
     * Gets all the feeds from the DB
     *
     * @return true if the feed list got updated
     * @throws Exception
     */
    private boolean getNewFeeds() {
        try {
            ArrayList<Feed> newFeeds = feedDb.getAll();
            Collections.sort(newFeeds, new Comparator<Feed>() {
                @Override
                public int compare(Feed o1, Feed o2) {
                    return o1.feedId.compareTo(o2.feedId);
                }
            });
            boolean eq = true;
            if (newFeeds.size() != feeds.size()) {
                eq = false;
            } else {
                for (int i = 0; i < feeds.size(); ++i) {
                    if (!feeds.get(i).feedId.equals(newFeeds.get(i).feedId)) {
                        eq = false;
                        break;
                    }
                }
            }
            if (!eq) {
                feeds = newFeeds;
            }

            return !eq;
        } catch (Exception e) {
            LOG.error("Exception trying to update feed list");
            return false;
        }
    }

    /**
     * Assigns the feeds to crawlers. <p/>
     *
     * Should work but there are many ways to improve it:
     *
     * <ul> It's probably a good idea to shuffle the feeds every once in a while (like 1 hour) </ul>
     *
     * <ul> When there's any change, everything is computed from scratch and marked as new, causing various things to restart at once </ul>
     *
     * <ul> In a big deployment it may take too long to assign the feeds (note: 100000 feeds is not much) </ul>
     */
    private void distributeFeeds() throws Exception {
        long begin = System.currentTimeMillis();

        ArrayList<Crawler> crawlers = crawlerDb.getAll();   // ttt1 reads all fields but doesn't need them
        ArrayList<Crawler> liveCrawlers = new ArrayList<>();
        ArrayList<Crawler> deadCrawlers = new ArrayList<>();
        for (Crawler crawler : crawlers) {
            if (checkAndStoreAlive(crawler)) {
                liveCrawlers.add(crawler);
            } else {
                deadCrawlers.add(crawler);
            }
        }

        HashMap<String, ArrayList<String>> newFeedMap = new HashMap<>();
        if (liveCrawlers.isEmpty()) {
            LOG.warn("No live crawlers found");
        } else {
            Collections.sort(liveCrawlers, new Comparator<Crawler>() {
                @Override
                public int compare(Crawler o1, Crawler o2) {
                    return o1.crawlerId.compareTo(o2.crawlerId);
                }
            });
            for (Crawler crawler : liveCrawlers) {
                newFeedMap.put(crawler.crawlerId, new ArrayList<String>());
            }
            int k = 0;
            for (Feed feed : feeds) {
                newFeedMap.get(liveCrawlers.get(k).crawlerId).add(feed.feedId);
                ++k;
                if (k >= liveCrawlers.size()) {
                    k = 0;
                }
            }
        }

        if (!newFeedMap.equals(feedMap)) {
            LOG.info("new feed map");
            feedMap = newFeedMap;
            for (Crawler crawler : liveCrawlers) {
                crawlerDb.updateFeedList(crawler.crawlerId, feedMap.get(crawler.crawlerId), crawler.feedIdsSeq + 1);
            }
        }

        crawlerDb.delete(deadCrawlers);

        long end = System.currentTimeMillis();
        if (end - begin > Config.getConfig().feedManagerTickInterval / 4) {
            LOG.error("FeedManagerTickInterval is too low"); // ttt1 maybe throw
            // ttt1 perhaps have worker threads or something, if using more than 10000 feeds
        }
    }

    private boolean checkAndStoreAlive(Crawler crawler) {
        Integer seq = crawlerSeq.get(crawler.crawlerId);
        if (seq == null || seq != crawler.crawlTick) {
            crawlerSeq.put(crawler.crawlerId, crawler.crawlTick);
            return true;
        }
        return false;
    }

    private class LeaderElector extends LeaderElectorBase {

        protected LeaderElector(int ticksBeforeBecomingLeader, long tickInterval) {
            super(ELECTION_ID, IP, ticksBeforeBecomingLeader, tickInterval, electionDb);
        }

        @Override
        protected void onLeaderTick() {
            if (getNewFeeds() || nexFeedDistribution < System.currentTimeMillis()) {
                try {
                    nexFeedDistribution = System.currentTimeMillis() + Config.getConfig().feedDistributionInterval;
                    distributeFeeds();
                } catch (Exception e) {
                    LOG.error("Error distributing feeds", e);
                }
            }
        }
    }
}
