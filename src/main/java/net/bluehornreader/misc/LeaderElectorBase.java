/*
Copyright (c) 2013 Marian Ciobanu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package net.bluehornreader.misc;

import net.bluehornreader.model.*;
import org.apache.commons.logging.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-28
 * Time: 22:58
 * <p/>
 *
 * Leader elector base class. Mainly responds to the question if it is the leader or not. It should be extended by overriding at least one of
 * the methods onBeginLeadership() and onEndLeadership(), or by using updateExternalState(). <p/>
 *
 * A cleaner design would have a base class with these methods and the constructor protected, and a few children exposing some of
 * them to cover one use case or another, but what a "using" can do in C++ to increase visibility doesn't seem to have an equivalent in Java. <p/>
 *
 * Relies on Cassandra's quorum and on timings to achieve its goals. Assumptions:
 *   <ul> Cassandra's quorum works </ul>
 *   <ul> The leader has its work divided into parts small enough that they fit between ticks. Before starting any action that might affect
 *      the outside world it should make sure that less than <b>tickInterval</b> has passed since the last tick. </ul>
 *   <ul> The leader's actions that change external state (e.g. saving to a DB) take significantly less than the interval between ticks </ul>
 *   <ul> A thread is never frozen for more than tickInterval (for GC or whatever reason) </ul>
 *   <ul> The local time on the computer changes reasonably, without skips that are the magnitude of tickInterval. If tickInterval is 1 minute and
 *      the clock is moved forward or backwards by several minutes, this is normally detected (assuming Thread.sleep() cares about the actual
 *      duration, ignoring clock changes once it gets started.) </ul>
 *
 * As an extra safety, changing of external state should happen inside updateExternalState(), which has its own checks that the timing is right. <p/>
 *
 * For a host to act as a leader when it shouldn't, several things need to go wrong so it takes longer or shorter to do things but
 * at the same time the clock changes and it doesn't notice. <p/>
 *
 * Note that this is somehow related to a general problem with the leader election: Even if this works perfectly, the leader has to do something
 * to change the external world. This might take longer than expected, or the local time may be changed, or whatever, so in the end, if the leader's
 * processing lingers any longer after the perfect detection of loss of leadership, this processing may change the external world incorrectly and
 * doesn't seem to be a good way aroud your JVM being frozen for 10 minutes and then the next thing to get executed to be a DB write. <p/>
 *
 * The basic idea is that to become and stay a leader a row in a table is used. It is read and maybe written to periodically, every tickInterval.
 * Normally it is just read for the value of the "tick" field. As long as that keeps changing, there's another leader, identified by the "leader_id" field.
 * If a time of at least ticksBeforeBecomingLeader*tickInterval passes without a change in "tick", the current object decides that it might want to
 * take the leadership and writes its own id in the "id" field, along with a tick of 0. Then every tickInterval it increments the "tick" field.
 * It only becomes leader after tick reaches the value of ticksBeforeBecomingLeader. <p/>
 *
 * So what happens if several objects want to become leaders? Basically "the last one wins". Before writing a new tick to the DB, the candidate
 * checks to see that the database has what it has written. If it doesn't, it ends its leadership. Since there's usually very little time between
 * reading a value and writing something back, there shouldn't be a lot of contention. But even then, since a candidate waits a lot before
 * attempting leadership if it detects that others wrote to the DB after it, the last to write will very likely become the leader. <p/>
 *
 * Reading and writing to the DB for leader election purposes must use a QUORUM... consistency and a large enough replication factor to prevent
 * multiple candidates to believe that they have written successfully to the DB.
 */
public abstract class LeaderElectorBase implements Runnable {

    private static final Log LOG = LogFactory.getLog(LeaderElectorBase.class);

    private enum State { YES, NO, MAYBE }

    // this is somehow misleading, as it refers to its own ticks, once it starts writing to the database; but it waits the same
    // number of ticks for the database to stay unchanged before it starts writing, so if there's no leader it will take
    // 2*ticksBeforeBecomingLeader*tickInterval to actually become a leader
    private int ticksBeforeBecomingLeader;
    private long tickInterval;

    private State state;

    private String id;
    private String dbId;
    private int tick;
    private long nextTick;
    private long lastChange;
    private long lastEnterRead;
    private long lastExitRead;
    private long lastEnterWrite;
    private long lastExitWrite;
    private boolean lastReadHasChanges;
    private String electionId;
    protected Election.DB db;
    private boolean shouldExit = false;

    protected LeaderElectorBase(String electionId, String id, int ticksBeforeBecomingLeader, long tickInterval, Election.DB db) {
        this.electionId = electionId;
        this.id = id;
        this.ticksBeforeBecomingLeader = ticksBeforeBecomingLeader;
        this.tickInterval = tickInterval;
        this.db = db;
    }

    /**
     * Gets called when the current object becomes leader
     */
    protected void onBeginLeadership() {
    }

    /**
     * Gets called when the current object becomes leader
     */
    protected void onEndLeadership() {
    }

    /**
     * Called on every tick when the current Elector is leader (including when it becomes a leader)
     */
    protected void onLeaderTick() {
    }


    public interface ExternalStateUpdater {
        void update();
        public static class CallTooLateException extends Exception {
        }
    }

    /**
     * Normally should be used by a worker thread, which is different from the thread running the LeaderElectorBase-derived object.
     * Calling db.updateDataVersion() is one thing externalStateUpdater might want to do, but it doesn't have to.
     */
    public final void updateExternalState(ExternalStateUpdater externalStateUpdater) throws ExternalStateUpdater.CallTooLateException {
        if (System.currentTimeMillis() > lastEnterWrite + tickInterval * 3 / 2) {
            throw new ExternalStateUpdater.CallTooLateException();
        }
        externalStateUpdater.update();
    }

    public String getId() {
        return id;
    }

    public String getElectionId() {
        return electionId;
    }

    public synchronized void signalExit() {
        LOG.info(String.format("LeaderElectorBase %s got an exit request", id));
        shouldExit = true;
    }

    @Override
    public void run() {
        dbId = "";
        tick = -1;
        lastChange = System.currentTimeMillis();
        state = State.NO;

        for (;;) {
            try {
                synchronized (this) {
                    if (shouldExit) {
                        break;
                    }
                }
                read();
                if (state == State.NO) {
                    if (lastChange + ticksBeforeBecomingLeader * tickInterval < System.currentTimeMillis()) {
                        //ttt2 the test doesn't really measure ticks, which might be smaller than tickInterval, so becoming leader may get delayed a tick
                        dbId = id;
                        tick = 0;
                        if (storeTick()) {
                            LOG.info("Trying to become the leader");
                            state = State.MAYBE;
                            // nextTick has an invalid value, as until now there have been random waits
                            long crtTime = System.currentTimeMillis();
                            nextTick = crtTime + Utils.getRandomDuration(tickInterval, 0.9, 1.0);
                            synchronized (this) {
                                wait(nextTick - crtTime);
                            }
                            continue;
                        } else {
                            LOG.info("Starting over due to database issues (1)");
                        }
                    } else {
                        LOG.info("leader has ID: " + dbId);
                    }
                    synchronized (this) {
                        wait(Utils.getRandomDuration(tickInterval, 0.75, 1.25));
                    }
                    continue;
                }

                if (lastReadHasChanges ||
                        lastExitRead - lastEnterWrite > tickInterval * 3 / 2 ||  // waited too long
                        lastEnterRead - lastExitWrite < tickInterval * 8 / 10) { // the clock is messed up
                    LOG.info(String.format("Starting over due to timing issues: lastReadHasChanges=%s, lastEnterRead=%s, lastExitRead=%s, " +
                            "lastEnterWrite=%s, lastExitWrite=%s", lastReadHasChanges, lastEnterRead, lastExitRead, lastEnterWrite, lastExitWrite));
                    doEndLeader(); // also waits a while
                    continue; // will read again, then sleep
                }

                ++tick;
                if (storeTick()) {
                    if (tick > ticksBeforeBecomingLeader) {
                        LOG.info("Ticking as leader ...");
                        onLeaderTick();
                    }
                } else {
                    LOG.info("Starting over due to database issues (2)");
                    doEndLeader();
                    continue;
                }

                if (tick == ticksBeforeBecomingLeader) {
                    doStartLeader();
                }
                long crtTime = System.currentTimeMillis();
                if (nextTick < crtTime + tickInterval / 2) { // we lost a lot of time somewhere
                    LOG.error("Lost time since last tick. Ending leadership ...");
                    doEndLeader();
                } else {
                    synchronized (this) {
                        wait(nextTick - crtTime);
                    }
                }
            } catch (Exception e) {
                LOG.error("Exception. Starting over after a pause ...", e);
                state = State.NO;
                try {
                    synchronized (this) {
                        wait(tickInterval * 2 * ticksBeforeBecomingLeader);
                    }
                } catch (InterruptedException e1) {
                    // !!! nothing
                }
            }
        }
        LOG.info(String.format("LeaderElectorBase %s exiting", id));
    }

    private void doStartLeader() {
        LOG.info("Beginning leadership");
        state = State.YES;
        onBeginLeadership();
        onLeaderTick();
    }

    /**
     * It's OK to call this if not actually leader
     * @throws Exception
     */
    private void doEndLeader() throws Exception {
        LOG.info("Ending leadership");
        state = State.NO;
        onEndLeadership();
        synchronized (this) {
            wait(tickInterval * 2 * ticksBeforeBecomingLeader); // let others take over
        }
    }

    private boolean storeTick() throws Exception {
        lastEnterWrite = System.currentTimeMillis();
        db.updateTick(electionId, id, tick);
        lastExitWrite = System.currentTimeMillis();
        boolean res = lastExitWrite - lastEnterRead < tickInterval / 4;
        LOG.info("storeTick() returning " + res);
        nextTick = nextTick + Utils.getRandomDuration(tickInterval, 0.9, 1.0);
        return res;
    }


    private void read() throws Exception {
        lastEnterRead = System.currentTimeMillis();
        Election election = db.get(electionId);
        if (election == null) {
            LOG.info("No database entry found");
            storeTick(); // there has to be something in the DB for the algorithm to work; the worst this can do is
                // delay a little choosing a leader; it doesn't matter that it is its own ID
            //lastReadHasChanges = true;
        } else {
            lastReadHasChanges = !dbId.equals(election.leaderId) || tick != election.tick;
            if (!lastReadHasChanges) {
                LOG.info("No changes since last read");
            }
            dbId = election.leaderId;
            tick = election.tick;
        }
        lastExitRead = System.currentTimeMillis();
        if (lastReadHasChanges) {
            lastChange = lastExitRead;
        }
    }
}

