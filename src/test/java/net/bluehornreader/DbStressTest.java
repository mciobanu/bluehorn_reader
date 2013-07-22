package net.bluehornreader;

import net.bluehornreader.data.*;
import net.bluehornreader.model.*;
import org.apache.commons.logging.*;

import java.util.*;
import java.util.concurrent.*;

/**
* Created with IntelliJ IDEA.
* User: ciobi
* Date: 2013-04-27
* Time: 14:01
* <p/>
*/
class DbStressTest {

    private static final Log LOG = LogFactory.getLog(DbStressTest.class);

    private LowLevelDbAccess lowLevelDbAccess;

    public static void main(String[] args) throws Exception {
        new DbStressTest().runDbTests();
    }


    private void runDbTests() throws Exception {
        lowLevelDbAccess = new LowLevelDbAccess();

        int threadCnt = 200; // !!! seems OK if larger than LowLevelDbAccess.MAX_CONN_PER_HOST
        //int rowCnt = 500;
        int rowCnt = 200;

        // threadCnt=200; rowCnt=500 or higher are OK except for testing articles, which tends to use a lot of memory,
        //    and for deleting ReadArticlesColl, which is very slow and will cause timeouts
        //
        // memory usage seems to be an issue tor the test program and also for Cassandra, which needs time to write to commitlog

        runTest("ArticleWriteBatch", new ArticleWriteBatchRunnableCreator(), threadCnt / 10, rowCnt);
        runTest("ArticleWriteSingle", new ArticleWriteSingleRunnableCreator(), threadCnt / 10, rowCnt);
        runTest("ArticleRead", new ArticleReadRunnableCreator(), threadCnt / 10, rowCnt);
        runTest("ArticleDelBatch", new ArticleDelBatchRunnableCreator(), threadCnt / 10, rowCnt);
        runTest("ArticleWriteBatch", new ArticleWriteBatchRunnableCreator(), threadCnt / 10, rowCnt);
        runTest("ArticleDelSingle", new ArticleDelSingleRunnableCreator(), threadCnt / 10, rowCnt);//*/

        runTest("FeedWrite", new FeedWriteRunnableCreator(), threadCnt, rowCnt);
        runTest("FeedRead", new FeedReadRunnableCreator(), threadCnt, rowCnt);
        runTest("FeedDelBatch", new FeedDelBatchRunnableCreator(), threadCnt, rowCnt);
        runTest("FeedDelSingle", new FeedDelSingleRunnableCreator(), threadCnt, rowCnt);
        //*/

        runTest("UserWrite", new UserWriteRunnableCreator(), threadCnt, rowCnt);
        runTest("UserRead", new UserReadRunnableCreator(), threadCnt, rowCnt);
        runTest("UserDel", new UserDelRunnableCreator(), threadCnt, rowCnt);
        //*/

        runTest("ReadArticlesCollWrite", new ReadArticlesCollWriteRunnableCreator(), threadCnt, rowCnt);
        runTest("ReadArticlesCollRead", new ReadArticlesCollReadRunnableCreator(), threadCnt, rowCnt);
        runTest("ReadArticlesCollDel", new ReadArticlesCollDelRunnableCreator(), threadCnt, rowCnt);
        //*/

        lowLevelDbAccess.shutDown();
    }

    /*

13:39:51,543 (   650)  INFO [main] DbStressTest:85 - test: ArticleWriteBatch     ;   duration (ms):    419 ;   ops:   4000 ;   ops/second:   9546
13:39:51,809 (   916)  INFO [main] DbStressTest:85 - test: ArticleWriteSingle    ;   duration (ms):    265 ;   ops:   4000 ;   ops/second:  15094
13:39:52,172 (  1279)  INFO [main] DbStressTest:85 - test: ArticleRead           ;   duration (ms):    362 ;   ops:   4000 ;   ops/second:  11049
13:39:52,305 (  1412)  INFO [main] DbStressTest:85 - test: ArticleDelBatch       ;   duration (ms):    131 ;   ops:   4000 ;   ops/second:  30534
13:39:52,718 (  1825)  INFO [main] DbStressTest:85 - test: ArticleWriteBatch     ;   duration (ms):    412 ;   ops:   4000 ;   ops/second:   9708
13:39:52,846 (  1953)  INFO [main] DbStressTest:85 - test: ArticleDelSingle      ;   duration (ms):    128 ;   ops:   4000 ;   ops/second:  31250
13:39:53,996 (  3103)  INFO [main] DbStressTest:85 - test: FeedWrite             ;   duration (ms):   1150 ;   ops:  40000 ;   ops/second:  34782
13:39:55,252 (  4359)  INFO [main] DbStressTest:85 - test: FeedRead              ;   duration (ms):   1256 ;   ops:  40000 ;   ops/second:  31847
13:39:55,959 (  5066)  INFO [main] DbStressTest:85 - test: FeedDelBatch          ;   duration (ms):    707 ;   ops:  40000 ;   ops/second:  56577
13:39:56,676 (  5783)  INFO [main] DbStressTest:85 - test: FeedDelSingle         ;   duration (ms):    716 ;   ops:  40000 ;   ops/second:  55865
13:39:57,578 (  6685)  INFO [main] DbStressTest:85 - test: UserWrite             ;   duration (ms):    901 ;   ops:  40000 ;   ops/second:  44395
13:39:58,872 (  7979)  INFO [main] DbStressTest:85 - test: UserRead              ;   duration (ms):   1293 ;   ops:  40000 ;   ops/second:  30935
13:39:59,948 (  9055)  INFO [main] DbStressTest:85 - test: UserDel               ;   duration (ms):   1075 ;   ops:  40000 ;   ops/second:  37209
13:40:00,945 ( 10052)  INFO [main] DbStressTest:85 - test: ReadArticlesCollWrite ;   duration (ms):    996 ;   ops:  40000 ;   ops/second:  40160
13:40:02,192 ( 11299)  INFO [main] DbStressTest:85 - test: ReadArticlesCollRead  ;   duration (ms):   1246 ;   ops:  40000 ;   ops/second:  32102
13:40:02,902 ( 12009)  INFO [main] DbStressTest:85 - test: ReadArticlesCollDel   ;   duration (ms):    710 ;   ops:  40000 ;   ops/second:  56338


13:41:04,677 (   617)  INFO [main] DbStressTest:110 - test: ArticleWriteBatch     ;   duration (ms):    394 ;   ops:   4000 ;   ops/second:  10152
13:41:04,970 (   910)  INFO [main] DbStressTest:110 - test: ArticleWriteSingle    ;   duration (ms):    292 ;   ops:   4000 ;   ops/second:  13698
13:41:05,308 (  1248)  INFO [main] DbStressTest:110 - test: ArticleRead           ;   duration (ms):    337 ;   ops:   4000 ;   ops/second:  11869
13:41:05,441 (  1381)  INFO [main] DbStressTest:110 - test: ArticleDelBatch       ;   duration (ms):    131 ;   ops:   4000 ;   ops/second:  30534
13:41:05,765 (  1705)  INFO [main] DbStressTest:110 - test: ArticleWriteBatch     ;   duration (ms):    324 ;   ops:   4000 ;   ops/second:  12345
13:41:05,879 (  1819)  INFO [main] DbStressTest:110 - test: ArticleDelSingle      ;   duration (ms):    112 ;   ops:   4000 ;   ops/second:  35714
13:41:07,018 (  2958)  INFO [main] DbStressTest:110 - test: FeedWrite             ;   duration (ms):   1138 ;   ops:  40000 ;   ops/second:  35149
13:41:08,353 (  4293)  INFO [main] DbStressTest:110 - test: FeedRead              ;   duration (ms):   1335 ;   ops:  40000 ;   ops/second:  29962
13:41:09,085 (  5025)  INFO [main] DbStressTest:110 - test: FeedDelBatch          ;   duration (ms):    731 ;   ops:  40000 ;   ops/second:  54719
13:41:11,564 (  7504)  INFO [main] DbStressTest:110 - test: FeedDelSingle         ;   duration (ms):   2478 ;   ops:  40000 ;   ops/second:  16142
13:41:12,382 (  8322)  INFO [main] DbStressTest:110 - test: UserWrite             ;   duration (ms):    818 ;   ops:  40000 ;   ops/second:  48899
13:41:13,474 (  9414)  INFO [main] DbStressTest:110 - test: UserRead              ;   duration (ms):   1091 ;   ops:  40000 ;   ops/second:  36663
13:41:14,135 ( 10075)  INFO [main] DbStressTest:110 - test: UserDel               ;   duration (ms):    661 ;   ops:  40000 ;   ops/second:  60514
13:41:15,015 ( 10955)  INFO [main] DbStressTest:110 - test: ReadArticlesCollWrite ;   duration (ms):    879 ;   ops:  40000 ;   ops/second:  45506
13:41:16,146 ( 12086)  INFO [main] DbStressTest:110 - test: ReadArticlesCollRead  ;   duration (ms):   1130 ;   ops:  40000 ;   ops/second:  35398
13:41:16,834 ( 12774)  INFO [main] DbStressTest:110 - test: ReadArticlesCollDel   ;   duration (ms):    688 ;   ops:  40000 ;   ops/second:  58139

     */


    private interface RunnableCreator {
        Runnable create(int threadNumber, int rowCnt);
    }

    private void runTest(String testName, RunnableCreator runnableCreator, int threadCnt, int rowCnt) throws Exception {
        long b = System.currentTimeMillis();

        //ExecutorService es = Executors.newCachedThreadPool();
        ExecutorService es = Executors.newFixedThreadPool(threadCnt);
        for (int i = 0; i < threadCnt; ++i) {
            es.execute(runnableCreator.create(i, rowCnt));
        }
        es.shutdown();
        boolean finshed = es.awaitTermination(1, TimeUnit.MINUTES);
        long e = System.currentTimeMillis();
        if (!finshed) {
            throw new RuntimeException("not finished");
        }
        LOG.info(String.format("test: %-22s;   duration (ms): %6d ;   ops: %6s ;   ops/second: %6d",
                testName, e - b, threadCnt * rowCnt, threadCnt * rowCnt * 1000 / (e - b)));
    }


    private static final String BIG_CONTENT;
    static {
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            bld.append("content ");
        }
        BIG_CONTENT = bld.toString();
    }

    private class ArticleWriteBatchRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        String feed = "feed" + threadNumber;
                        LOG.debug("starting " + Article.CQL_TABLE.tableName + " - " + feed);
                        ArrayList<Article> objects = new ArrayList<>();
                        for (int i = 0; i < rowCnt; ++i) {
                            objects.add(new Article(feed, i, "title " + i, "summary " + i,
                                    "http://site" + i, "content " + i + " " + threadNumber + BIG_CONTENT, System.currentTimeMillis()));
                        }
                        Article.DB db = new Article.DB(lowLevelDbAccess);
                        db.add(objects);
                        LOG.debug("done " + Article.CQL_TABLE.tableName + " - " + feed);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }

    private class ArticleWriteSingleRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        String feed = "feed" + threadNumber;
                        LOG.debug("starting " + Article.CQL_TABLE.tableName + " - " + feed);
                        ArrayList<Article> objects = new ArrayList<>();
                        for (int i = 0; i < rowCnt; ++i) {
                            objects.add(new Article(feed, i, "title " + i, "summary " + i,
                                    "http://site" + i, "content " + i + " " + threadNumber + BIG_CONTENT, System.currentTimeMillis()));
                            Article.DB db = new Article.DB(lowLevelDbAccess);
                            db.add(objects);
                            objects.clear();
                        }
                        LOG.debug("done " + Article.CQL_TABLE.tableName + " - " + feed);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }

    private class ArticleReadRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        String feed = "feed" + threadNumber;
                        LOG.debug("starting " + Article.CQL_TABLE.tableName + " - " + feed);
                        Article.DB db = new Article.DB(lowLevelDbAccess);

                        for (int i = 0; i < rowCnt; ++i) {
                            Article article = db.get(feed, i);
                            if (!article.title.equals("title " + i) ||
                                    !article.summary.equals("summary " + i) ||
                                    !article.url.equals("http://site" + i) ||
                                    !article.content.equals("content " + i + " " + threadNumber + BIG_CONTENT)) {
                                throw new RuntimeException(String.format(Article.CQL_TABLE.tableName + ": read mismatch for key <%s, %s>", feed, i));
                            }
                        }
                        LOG.debug("done " + Article.CQL_TABLE.tableName + " - " + feed);
                    } catch (Exception e) {
                        throw new RuntimeException("excp reading data", e);
                    }
                }
            };
        }
    }


    private class ArticleDelBatchRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        String feed = "feed" + threadNumber;
                        LOG.debug("starting " + Article.CQL_TABLE.tableName + " - " + feed);
                        ArrayList<Article> objects = new ArrayList<>();
                        for (int i = 1; i < rowCnt; ++i) {  // !!! i starts from "1" to leave some records there
                            objects.add(new Article(feed, i, null, null, null, null, 0));
                        }
                        Article.DB db = new Article.DB(lowLevelDbAccess);
                        db.delete(objects);
                        LOG.debug("done " + Article.CQL_TABLE.tableName + " - " + feed);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }

    private class ArticleDelSingleRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        String feed = "feed" + threadNumber;
                        LOG.debug("starting " + Article.CQL_TABLE.tableName + " - " + feed);
                        ArrayList<Article> objects = new ArrayList<>();
                        for (int i = 1; i < rowCnt; ++i) {
                            objects.add(new Article(feed, i, null, null, null, null, 0));
                            Article.DB db = new Article.DB(lowLevelDbAccess);
                            db.delete(objects);
                            objects.clear();
                        }
                        LOG.debug("done " + Article.CQL_TABLE.tableName + " - " + feed);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }



    private class FeedWriteRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + Feed.CQL_TABLE.tableName);
                        ArrayList<Feed> objects = new ArrayList<>();
                        for (int i = 0; i < rowCnt; ++i) {
                            String id = "feed" + (threadNumber * rowCnt + i);
                            objects.add(new Feed(id, "http://" + id));
                        }
                        Feed.DB db = new Feed.DB(lowLevelDbAccess);
                        db.add(objects);
                        LOG.debug("done " + Feed.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }

    private class FeedReadRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + Feed.CQL_TABLE.tableName);
                        Feed.DB db = new Feed.DB(lowLevelDbAccess);

                        for (int i = 0; i < rowCnt; ++i) {
                            String id = "feed" + (threadNumber * rowCnt + i);
                            Feed feed = db.get(id);
                            if (!feed.name.equals("name " + id) ||
                                    !feed.url.equals("http://" + id)) {
                                throw new RuntimeException(String.format(Feed.CQL_TABLE.tableName + ": read mismatch for key <%s>", id));
                            }
                        }
                        LOG.debug("done " + Feed.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp reading data", e);
                    }
                }
            };
        }
    }

    private class FeedDelBatchRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + Feed.CQL_TABLE.tableName);
                        ArrayList<Feed> objects = new ArrayList<>();
                        for (int i = 1; i < rowCnt; ++i) {
                            String id = "feed" + (threadNumber * rowCnt + i);
                            objects.add(new Feed(id, null));
                        }
                        Feed.DB db = new Feed.DB(lowLevelDbAccess);
                        db.delete(objects);
                        LOG.debug("done " + Feed.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }

    private class FeedDelSingleRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + Feed.CQL_TABLE.tableName);
                        ArrayList<Feed> objects = new ArrayList<>();
                        Feed.DB db = new Feed.DB(lowLevelDbAccess);
                        for (int i = 1; i < rowCnt; ++i) {
                            String id = "feed" + (threadNumber * rowCnt + i);
                            objects.add(new Feed(id, null));
                            db.delete(objects);
                            objects.clear();
                        }
                        LOG.debug("done " + Feed.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }



    private class UserWriteRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + User.CQL_TABLE.tableName);
                        ArrayList<User> objects = new ArrayList<>();
                        for (int i = 0; i < rowCnt; ++i) {
                            String id = "user" + (threadNumber * rowCnt + i);
                            byte[] passwd = new byte[15];
                            String textPasswd = "passwd" + id;
                            for (int j = 0; j < textPasswd.length(); ++j) {
                                passwd[j] = (byte) textPasswd.charAt(j);
                            }
                            objects.add(new User(id, "name " + id, passwd, "salt" + id, "name " + id + "@test.com", new ArrayList<String>(), true, false));
                        }
                        User.DB db = new User.DB(lowLevelDbAccess);
                        db.add(objects);
                        LOG.debug("done " + User.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }

    private class UserReadRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + User.CQL_TABLE.tableName);
                        User.DB db = new User.DB(lowLevelDbAccess);

                        for (int i = 0; i < rowCnt; ++i) {
                            String id = "user" + (threadNumber * rowCnt + i);
                            User user = db.get(id);
                            byte[] passwd = new byte[15];
                            String textPasswd = "passwd" + id;
                            for (int j = 0; j < textPasswd.length(); ++j) {
                                passwd[j] = (byte) textPasswd.charAt(j);
                            }
                            if (!user.name.equals("name " + id) || !Arrays.equals(user.password, passwd)) {
                                throw new RuntimeException(String.format(User.CQL_TABLE.tableName + ": read mismatch for key <%s>", id));
                            }
                        }
                        LOG.debug("done " + User.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp reading data", e);
                    }
                }
            };
        }
    }


    private class UserDelRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + User.CQL_TABLE.tableName);
                        ArrayList<User> objects = new ArrayList<>();
                        for (int i = 1; i < rowCnt; ++i) {
                            String id = "user" + (threadNumber * rowCnt + i);
                            objects.add(new User(id, null, null, null, null, null, true, false));
                        }
                        User.DB db = new User.DB(lowLevelDbAccess);
                        db.delete(objects);
                        LOG.debug("done " + User.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }


    private static final int MAX_BMP_SIZE = 100;


    private class ReadArticlesCollWriteRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + ReadArticlesColl.CQL_TABLE.tableName);
                        String userId = "user" + threadNumber;
                        ArrayList<ReadArticlesColl> objects = new ArrayList<>();
                        Random rnd = new Random(threadNumber);
                        for (int i = 0; i < rowCnt; ++i) {
                            ReadArticlesColl obj = new ReadArticlesColl(userId, "feed" + i);
                            for (int j = 0; j < 10; ++j) {
                                obj.markRead(rnd.nextInt(1000), MAX_BMP_SIZE);
                            }
                            objects.add(obj);
                        }
                        ReadArticlesColl.DB db = new ReadArticlesColl.DB(lowLevelDbAccess);
                        db.add(objects);
                        LOG.debug("done " + ReadArticlesColl.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }


    private class ReadArticlesCollReadRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + ReadArticlesColl.CQL_TABLE.tableName);
                        ReadArticlesColl.DB db = new ReadArticlesColl.DB(lowLevelDbAccess);
                        String userId = "user" + threadNumber;
                        Random rnd = new Random(threadNumber);
                        for (int i = 0; i < rowCnt; ++i) {
                            String feedId = "feed" + i;
                            ReadArticlesColl newObj = new ReadArticlesColl(userId, "feed" + i);
                            for (int j = 0; j < 10; ++j) {
                                newObj.markRead(rnd.nextInt(1000), MAX_BMP_SIZE);
                            }
                            ReadArticlesColl readObj = db.get(userId, feedId);
                            if (!readObj.testEqual(newObj)) {
                                throw new RuntimeException(String.format(ReadArticlesColl.CQL_TABLE.tableName + ": read mismatch for key <%s, %s>", userId, feedId));
                            }
                        }
                        LOG.debug("done " + ReadArticlesColl.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp reading data", e);
                    }
                }
            };
        }
    }

    private class ReadArticlesCollDelRunnableCreator implements RunnableCreator {
        @Override
        public Runnable create(final int threadNumber, final int rowCnt) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("starting " + ReadArticlesColl.CQL_TABLE.tableName);
                        String userId = "user" + threadNumber;
                        ArrayList<ReadArticlesColl> objects = new ArrayList<>();
                        for (int i = 1; i < rowCnt; ++i) {
                            ReadArticlesColl obj = new ReadArticlesColl(userId, "feed" + i);
                            objects.add(obj);
                        }
                        ReadArticlesColl.DB db = new ReadArticlesColl.DB(lowLevelDbAccess);
                        db.delete(objects);
                        LOG.debug("done " + ReadArticlesColl.CQL_TABLE.tableName);
                    } catch (Exception e) {
                        throw new RuntimeException("excp generating data", e);
                    }
                }
            };
        }
    }


}

