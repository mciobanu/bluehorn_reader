package net.bluehornreader;

import org.apache.commons.logging.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-20
 * Time: 21:02
 * <p/>
 */
public class Utils {

    private static final Log LOG = LogFactory.getLog(Utils.class);

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

}
