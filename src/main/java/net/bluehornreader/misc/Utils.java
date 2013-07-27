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

import org.apache.commons.logging.*;

import java.net.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-20
 * Time: 21:02
 * <p/>
 */
public class Utils {

    private static final Log LOG = LogFactory.getLog(Utils.class);

    public static final String LIST_SEPARATOR = ":";

    public static void cbAssert(boolean cond, String message) {
        if (!cond) {
            AssertionError e = new AssertionError(message);
            LOG.fatal("Assertion failure", e);
            exit("Assertion failure", 1);
        }
    }


    /**
     * Terminates the VM. Waits 10s to avoid situations when a process crashes and it gets immediately restarted, killing the CPU.
     *
     * @param message
     * @param exitCode
     */
    public static void exit(String message, int exitCode) {
        long sleep = 10000;
        LOG.info("Exiting due to: " + message);
        LOG.info(String.format("Sleeping for %d ms", sleep));
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LOG.error("Failed to sleep. Exiting anyway ...", e);
        }
        System.exit(exitCode);

    }

    public static long ONE_SECOND = 1000;
    public static long ONE_MINUTE = 60 * ONE_SECOND;
    public static long ONE_HOUR = 60 * ONE_MINUTE;
    public static long ONE_DAY = 24 * ONE_HOUR;
    public static long ONE_WEEK = 7 * ONE_DAY;
    public static long ONE_MONTH = 30 * ONE_DAY;
    public static long ONE_YEAR = 365 * ONE_DAY;


    public static long stringToDuration(String s) {
        long mul = 1;
        if (s.endsWith("s")) {
            mul = ONE_SECOND;
        } else if (s.endsWith("m")) {
            mul = ONE_MINUTE;
        } else if (s.endsWith("h")) {
            mul = ONE_HOUR;
        } else if (s.endsWith("d")) {
            mul = ONE_DAY;
        } else if (s.endsWith("w")) {
            mul = ONE_WEEK;
        }
        if (mul != 1) {
            s = s.substring(0, s.length() - 1);
        }
        return mul * Long.parseLong(s);
    }

    public static String durationToString(long x) {
        String suffix = "";
        if (x % 1000 == 0) {
            x /= 1000;
            suffix = "s";
        }
        if (x % 60 == 0) {
            x /= 60;
            suffix = "m";
        }
        if (x % 60 == 0) {
            x /= 60;
            suffix = "h";
        }
        if (x % 24 == 0) {
            x /= 24;
            suffix = "d";
        }
        if (x % 7 == 0) {
            x /= 7;
            suffix = "w";
        }

        return x + suffix;
    }

    /**
     * @param baseDuration
     * @param minMultiplier
     * @param maxMultiplier
     * @return a value that is basically between baseDuration*minMultiplier and baseDuration*maxMultiplier; if this leads to something <=0, it returns 1
     *      instead, so the value can be used in wait()
     */
    public static long getRandomDuration(long baseDuration, double minMultiplier, double maxMultiplier) {
        long res = (long) (baseDuration * (minMultiplier + Math.random() * (maxMultiplier - minMultiplier)));
        if (res <= 0) {
            res = 1;
        }
        return res;
    }


    public static String listAsString(Collection<String> list) {
        StringBuilder bld = new StringBuilder();
        boolean first = true;
        for (String s : list) {
            if (first) {
                first = false;
            } else {
                bld.append(LIST_SEPARATOR);
            }
            bld.append(s);
        }
        return bld.toString();
    }

    /**
     * @param s can be null or empty, in which case an empty list is returned
     * @return
     */
    public static ArrayList<String> stringAsList(String s) {
        return new ArrayList<>(Arrays.asList(stringAsArray(s)));
    }

    /**
     * @param s can be null or empty, in which case an empty array is returned
     * @return
     */
    public static String[] stringAsArray(String s) {
        return s == null || s.isEmpty() ? new String[0] : s.split(LIST_SEPARATOR, 0);
    }


    public static boolean isInJar() {
        URL url = Utils.class.getClassLoader().getResource("net/bluehornreader/model/Article.class");
        String s = url == null ? "" : url.toExternalForm();
        return !s.startsWith("file:");
    }
}
