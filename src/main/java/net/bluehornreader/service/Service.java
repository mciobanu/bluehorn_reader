package net.bluehornreader.service;

import net.bluehornreader.*;
import org.apache.commons.logging.*;

import java.net.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-05-06
 * Time: 01:03
 * <p/>
 *
 * A Service has its own thread but may start additional threads, if needed. <p/>
 */
public abstract class Service extends Thread {

    /*
    Implemented as "extends Thread" rather than "implements Runnable" to have an object that does 2 things:
    - can be queried if it's still alive
    - can have additional methods, like signalExit() or waitForThreadsToFinish()
    To use "implements Runnable" we'd have to carry around a pair of a Thread and the Runnable it was used to create it.

    ttt1 see if having a "RunnableThread" extending Thread only to give access to its Runnable would be better
     */

    private static final Log LOG = LogFactory.getLog(Service.class);
    protected static final String IP;
    static {
        try {
            IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            LOG.error("Failed to determine IP. Exiting ...", e);
            Utils.exit("Failed to determine IP", 2);
            throw new RuntimeException(""); // to appease the compiler
        }
    }

    /**
     * To be called by the ServiceManager
     */
    abstract public void signalExit();

    public void waitForThreadsToFinish() {
        long start = System.currentTimeMillis();
        for (;;) {
            boolean foundAlive = false;
            if (isAlive()) {
                foundAlive = true;
                LOG.info(String.format("Thread %s is still alive", getName()));
                synchronized (this) {
                    notify();
                }
            }
            for (Thread thread : getChildThreads()) {
                if (thread.isAlive()) {
                    foundAlive = true;
                    LOG.info(String.format("Thread %s is still alive", thread.getName()));
                    synchronized (thread) { //ttt0 make sure this works, as Idea complains: create a separate test case
                        thread.notify();
                    }
                }
            }
            if (foundAlive) {
                try {
                    if (System.currentTimeMillis() - start > 10000) { // ttt2 maybe make configurable
                        LOG.warn("Some threads are still running. Giving up on shutting them down.");
                        return;
                    }

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.error("Exception sleeping while trying to shut down child threads", e);
                }
            } else {
                return;
            }
        }
    }

    /**
     * @return This should return threads created by extending Thread rather than created by passing a Runnable on the constructor, as they
     * are expected to pause using wait(), so they can be awaken with notify()
     */
    abstract protected Collection<Thread> getChildThreads();
}
