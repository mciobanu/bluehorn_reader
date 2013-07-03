package net.bluehornreader.model;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-20
 * Time: 21:30
 * <p/>
 *
 * Tests the bitmap functionality in ReadArticlesColl
 */
public class ReadArticlesTst {

    ReadArticlesColl readArticlesColl;
    boolean[] b;
    int maxBitmapSize = 10;

    public static void main(String[] args) {
        ArrayList<Long> times = new ArrayList<Long>();
        times.add(System.currentTimeMillis());

        ReadArticlesTst tst = new ReadArticlesTst();

        tst.testMarkReadComprehensive(); times.add(System.currentTimeMillis());
        tst.testMarkReadIntervalComprehensive(); times.add(System.currentTimeMillis());

        tst.testMarkUnreadOnePoint(); times.add(System.currentTimeMillis());
        tst.testMarkUnreadTwoPoints(); times.add(System.currentTimeMillis());
        tst.testMarkUnreadThreePoints(); times.add(System.currentTimeMillis());
        tst.testMarkUnreadComprehensive(); times.add(System.currentTimeMillis());

        System.out.println("\n\nTest durations (ms):");
        for (int i = 1; i < times.size(); ++i) {
            System.out.printf("%5d) %,9d%n", i, times.get(i) - times.get(i - 1));
        }
        System.out.printf("----------------\nTotal: %,9d%n", times.get(times.size() - 1) - times.get(0));

/*
Test durations (ms):
    1)   124,675
    2)    26,435
    3)         4
    4)        57
    5)     9,397
    6)    51,878
----------------
Total:   212,446
*/
    }

    void testMarkReadComprehensive() {
        for (int maxArtId = 30; maxArtId < 3000000; maxArtId = maxArtId * 2) {
            Random rnd = new Random(0);
            //Random rnd = new Random();
            for (int t = 0; t < 100000; ++t) {
                if (t == 588) {
                    nop();
                }
                int testSize = 1 + rnd.nextInt(100);
                readArticlesColl = new ReadArticlesColl("user1", "feed1");
                b = new boolean[maxArtId];
                //int testSize = 1;
                for (int i = 0; i < testSize; ++i) {
                    if (i == 16) {
                        nop();
                    }
                    int seq = rnd.nextInt(maxArtId);
                    //System.out.println("seq=" + seq);
                    b[seq] = true;
                    readArticlesColl.markRead(seq, maxBitmapSize);
                }
                System.out.printf("%d. test size: %d. first: %d%n", t, testSize, readArticlesColl.first);
                compare();
            }
        }
    }

    void testMarkReadTwoPoints() {
        int maxArtId = 300;
        for (int i = 0; i < maxArtId; ++i) {
            for (int j = 0; j < maxArtId; ++j) {
                b = new boolean[maxArtId];
                readArticlesColl = new ReadArticlesColl("user1", "feed1");
                readArticlesColl.markRead(i, maxBitmapSize);
                readArticlesColl.markRead(j, maxBitmapSize);
                b[i] = true;
                b[j] = true;

                compare();
            }
        }
    }


    void testMarkReadThreePoints() {
        int maxArtId = 300;
        for (int i = 0; i < maxArtId; ++i) {
            for (int j = 0; j < maxArtId; ++j) {
                for (int k = 0; k < maxArtId; ++k) {
                    b = new boolean[maxArtId];
                    readArticlesColl = new ReadArticlesColl("user1", "feed1");
                    readArticlesColl.markRead(i, maxBitmapSize);
                    readArticlesColl.markRead(j, maxBitmapSize);
                    readArticlesColl.markRead(k, maxBitmapSize);
                    b[i] = true;
                    b[j] = true;
                    b[k] = true;

                    //System.out.println(i + " " + j);
                    compare();
                }
            }
        }
    }


    void testMarkReadIntervalTwoPoints() {
        int maxArtId = 300;
        for (int i = 0; i < maxArtId; ++i) {
            for (int j = i; j < maxArtId; ++j) {
                b = new boolean[maxArtId];
                readArticlesColl = new ReadArticlesColl("user1", "feed1");
                readArticlesColl.markRead(i, j, maxBitmapSize);
                System.out.printf("i=%d  j=%d%n", i, j);
                for (int k = i; k <= j; ++k) {
                    b[k] = true;
                }

                compare();
            }
        }
    }



    void testMarkReadIntervalComprehensive() {
        for (int maxArtId = 30; maxArtId < 3000000; maxArtId = maxArtId * 2) {
            Random rnd = new Random(120);
            //Random rnd = new Random();
            for (int t = 0; t < 1000; ++t) {
                if (t == 13) {
                    nop();
                }
                int testSize = 1 + rnd.nextInt(100);
                readArticlesColl = new ReadArticlesColl("user1", "feed1");
                b = new boolean[maxArtId];
                //int testSize = 1;
                for (int i = 0; i < testSize; ++i) {
                    if (i == 16) {
                        nop();
                    }
                    int seqFrom = rnd.nextInt(maxArtId);
                    int seqTo = rnd.nextInt(maxArtId);
                    if (seqTo < seqFrom) {
                        int a = seqFrom;
                        seqFrom = seqTo;
                        seqTo = a;
                    }
                    readArticlesColl.markRead(seqFrom, seqTo, maxBitmapSize);
                    for (int j = seqFrom; j <= seqTo; ++j) {
                        b[j] = true;
                    }
                    nop();
                }
                System.out.printf("%d. test size: %d. first: %d%n", t, testSize, readArticlesColl.first);
                compare();
            }
        }
    }

    private void compare() {
        for (int seq = readArticlesColl.first; seq < b.length; ++seq) {
            if (b[seq] != readArticlesColl.isRead(seq)) {
                System.out.printf("diff for %d: b:%s vs readArticlesColl:%s%n", seq, b[seq], readArticlesColl.isRead(seq));
                throw new RuntimeException("eeeee");
            }
        }
    }


    void testMarkUnreadOnePoint() {
        int maxArtId = 300;
        for (int j = 0; j < maxArtId; ++j) {
            b = new boolean[maxArtId];
            readArticlesColl = new ReadArticlesColl("user1", "feed1");
            for (int k = 0; k < maxArtId; ++k) {
                b[k] = true;
            }
            readArticlesColl.markRead(0, maxArtId - 1, maxBitmapSize);

            readArticlesColl.markUnread(j, maxBitmapSize);
            b[j] = false;

            System.out.printf("%d. first=%d%n", j, readArticlesColl.first);
            compare();
        }
    }



    void testMarkUnreadTwoPoints() {
        int maxArtId = 300;
        for (int i = 0; i < maxArtId; ++i) {
            for (int j = 0; j < maxArtId; ++j) {
                b = new boolean[maxArtId];
                readArticlesColl = new ReadArticlesColl("user1", "feed1");
                for (int k = 0; k < maxArtId; ++k) {
                    b[k] = true;
                }
                readArticlesColl.markRead(0, maxArtId - 1, maxBitmapSize);

                if (j == 224) {
                    nop();
                }

                readArticlesColl.markUnread(i, maxBitmapSize);
                readArticlesColl.markUnread(j, maxBitmapSize);
                b[i] = false;
                b[j] = false;

                //System.out.printf("i=%d, j=%d, first=%d%n", i, j, readArticlesColl.first);
                compare();
            }
        }
    }


    void testMarkUnreadThreePoints() {
        int maxArtId = 300;
        for (int i = 0; i < maxArtId; ++i) {
            for (int j = 0; j < maxArtId; ++j) {
                for (int k = 0; k < maxArtId; ++k) {
                    b = new boolean[maxArtId];
                    readArticlesColl = new ReadArticlesColl("user1", "feed1");
                    for (int q = 0; q < maxArtId; ++q) {
                        b[q] = true;
                    }
                    readArticlesColl.markRead(0, maxArtId - 1, maxBitmapSize);

                    if (k == 224) {
                        nop();
                    }

                    readArticlesColl.markUnread(i, maxBitmapSize);
                    readArticlesColl.markUnread(j, maxBitmapSize);
                    readArticlesColl.markUnread(k, maxBitmapSize);
                    b[i] = false;
                    b[j] = false;
                    b[k] = false;

                    //System.out.printf("i=%d, j=%d, k=%d, first=%d%n", i, j, k, readArticlesColl.first);
                    compare();
                }
            }
        }
    }



    void testMarkUnreadComprehensive() {

        int origMaxBitmapSize = maxBitmapSize;
        for (maxBitmapSize = 5; maxBitmapSize < 200; maxBitmapSize += 13) {
            for (int maxArtId = 30; maxArtId < 300000; maxArtId = maxArtId * 2) {
                Random rnd = new Random(120);
                //Random rnd = new Random();
                for (int t = 0; t < 1000; ++t) {
                    if (t == 17) {
                        nop();
                    }
                    int testSizeRead = 1 + rnd.nextInt(100);
                    readArticlesColl = new ReadArticlesColl("user1", "feed1");
                    b = new boolean[maxArtId];
                    //int testSizeRead = 1;
                    for (int i = 0; i < testSizeRead; ++i) {
                        if (i == 16) {
                            nop();
                        }
                        int seqFrom = rnd.nextInt(maxArtId);
                        int seqTo = rnd.nextInt(maxArtId);
                        if (seqTo < seqFrom) {
                            int a = seqFrom;
                            seqFrom = seqTo;
                            seqTo = a;
                        }
                        readArticlesColl.markRead(seqFrom, seqTo, maxBitmapSize);
                        for (int j = seqFrom; j <= seqTo; ++j) {
                            b[j] = true;
                        }
                        nop();
                    }

                    int testSizeUnread = 1 + rnd.nextInt(100);
                    for (int i = 0; i < testSizeUnread; ++i) {
                        int seq = rnd.nextInt(maxArtId);
                        readArticlesColl.markUnread(seq, maxBitmapSize);
                        b[seq] = false;
                    }

                    System.out.printf("%d. read size: %d, unread size: %d, first: %d%n", t, testSizeRead, testSizeUnread, readArticlesColl.first);
                    compare();
                }
            }
        }

        maxBitmapSize = origMaxBitmapSize;
    }


    private static void nop() {
    }
}
