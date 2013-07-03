package net.bluehornreader;

import net.bluehornreader.data.*;
import net.bluehornreader.model.*;
import org.apache.commons.logging.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-30
 * Time: 23:59
 * <p/>
 */
public class ElectionTest extends LeaderElectorBase {

    private static final Log LOG = LogFactory.getLog(ElectionTest.class);

    private static final long TICK_INTERVAL = 2000;
    private static final int TICKS_BEFORE_BECOMING_LEADER = 3;

    private long lastLeaderTick = Long.MAX_VALUE;

    private Thread workerThread = new Thread(new Worker());

    public static void main(String[] args) throws Exception {
        runTest();
    }

    protected ElectionTest(String electionId, String id, int ticksBeforeBecomingLeader, long tickInterval, Election.DB db) {
        super(electionId, id, ticksBeforeBecomingLeader, tickInterval, db);
        workerThread.setDaemon(true);
    }

    private static void runTest() throws Exception {
        LowLevelDbAccess lowLevelDbAccess = new LowLevelDbAccess();
        ElectionTest electionTest = new ElectionTest("election1", "" + System.currentTimeMillis(), TICKS_BEFORE_BECOMING_LEADER, TICK_INTERVAL,
                new Election.DB(lowLevelDbAccess));

        Thread mainThread = new Thread(electionTest);
        mainThread.setDaemon(true);
        mainThread.start();

        electionTest.workerThread.start();

        Thread.sleep(30000);

        lowLevelDbAccess.shutDown();
    }

    @Override
    protected void onLeaderTick() {
        LOG.info("ElectionTest.onLeaderTick()");
        lastLeaderTick = System.currentTimeMillis();
    }

    @Override
    protected void onBeginLeadership() {
        LOG.info("ElectionTest.onBeginLeadership()");
    }

    @Override
    protected void onEndLeadership() {
        LOG.info("ElectionTest.onEndLeadership()");
        lastLeaderTick = Long.MAX_VALUE;
    }

    private class Worker implements Runnable {

        @Override
        public void run() {
            for (;;) {
                try {
                    final long now = System.currentTimeMillis();
                    if (lastLeaderTick == Long.MAX_VALUE) {
                        //LOG.info("Worker resting. Master is not leader.");
                    }
                    else if (now - TICK_INTERVAL / 2 < lastLeaderTick) {
                        updateExternalState(new ExternalStateUpdater() {
                            @Override
                            public void update() {
                                LOG.info("Worker doing work and writing to DB, as only " + (now - lastLeaderTick) + " ms have passed since the latest leader tick");
                                try {
                                    String s = getId() + "/" + now;
                                    LOG.info("Storing new data to db, as data version: \"" + s + "\"");
                                    db.updateDataVersion(getElectionId(), s);
                                } catch (Exception e) {
                                    LOG.error("Error writing to the database", e);
                                }
                            }
                        });
                    } else {
                        LOG.info("Worker doing work, but without writing to DB. Too much time has passed since the latest leader tick: " + (now - lastLeaderTick)); // occasionally
                            // the diff will be negative, but that's fine; still access to lastLeaderTick should be synchronized
                    }
                    Thread.sleep((TICK_INTERVAL / 4));
                } catch (Exception e) {
                    LOG.error("Error writing to the database", e);
                }
            }
        }
    }
}

