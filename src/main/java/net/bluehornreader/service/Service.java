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

package net.bluehornreader.service;

import net.bluehornreader.misc.*;
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
                    synchronized (thread) { //ttt2 make sure this works, as Idea complains: create a separate test case; (the thing is that the object is not
                        // local, but taken from a global collection)
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
