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

import com.netflix.astyanax.model.*;
import org.apache.commons.logging.*;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-30
 * Time: 00:17
 * <p/>
 */
public class Config {

    private static final Log LOG = LogFactory.getLog(Config.class);

    private Properties properties = new Properties();


    public int mainReplicationFactor = 1;
    public String mainStrategyClass = "SimpleStrategy";
    public ConsistencyLevel mainReadConsistency = ConsistencyLevel.CL_ONE;
    public ConsistencyLevel mainWriteConsistency = ConsistencyLevel.CL_ONE;

    public int electionReplicationFactor = 1;
    public String electionStrategyClass = "SimpleStrategy";
    public ConsistencyLevel electionReadConsistency = ConsistencyLevel.CL_QUORUM;
    public ConsistencyLevel electionWriteConsistency = ConsistencyLevel.CL_QUORUM;

    public boolean startWebServer = true;
    public boolean startFeedCrawler = true;
    public boolean startFeedManager = true;

    public int cassandraMaxConnPerHost = 100;
    public String cassandraDbSeeds = "127.0.0.1:9160";
    public int cassandraDbPort = 9160;

    public String cqlVersion = "3.0.0";
    public String targetCassandraVersion = "1.2";

    public String clusterName = "Cluster01"; // not sure when it's used; when just running on localhost doesn't seem to matter

    public int httpPort = 8080;
    public int httpOutputBufferSize = 32768;
    public long httpIdleTimeout = Utils.stringToDuration("30s");
    public int httpsPort = 8443;
    public long httpsIdleTimeout = Utils.stringToDuration("30s");
    public String httpsKeystore;  // !!! no default value
    public String httpsKeystorePassword;  // !!! no default value

    public boolean activateAccountsAtCreation = true;

    public long getCrawlerTickInterval() {
        return 2 * feedManagerTickInterval;   // CrawlerTickInterval should be bigger than feedManagerTickInterval to allow detection of a dead feed manager
    }

    //public long sleepWhenNothingToCrawl = Utils.stringToDuration("1m");
    public long sleepWhenNothingToCrawl = Utils.stringToDuration("10s");
    public int threadsPerCrawler = 1;
    public long crawlPeriod = Utils.stringToDuration("1h");

    public long feedDistributionInterval = Utils.stringToDuration("10m");
    public int feedIdSize = 16;

    public long loginExpireInterval = Utils.stringToDuration("2w");
    public long cookieExpireInterval = Utils.stringToDuration("52w"); //!!! this is needed and it should be pretty long; by setting a short life for the cookie,
        // we effectively disable the "remember me" option, as an expired cookie isn't sent by the browser; what matters for not allowing access is the
        // value of loginExpireInterval

    public int getTicksBetweenCrawls() { // number of ticks that have to pass before a feed is crawled again
        return (int) (crawlPeriod / getCrawlerTickInterval());
    }

    public long crawlerWaitBetweenSaves = Utils.stringToDuration("1s");

    public int feedManagerTicksBeforeBecomingLeader = 4;

    //public long feedManagerTickInterval = Utils.stringToDuration("1m");
    public long feedManagerTickInterval = Utils.stringToDuration("2s");

    public String defaultStyle =
            "#header {\n" +
            "    position:fixed;\n" +
            "    left:0px;\n" +
            "    top:0px;\n" +
            "    width:100%;\n" +
            "}\n" +
            "\n" +
            "#headerP {\n" +
            "    clear:both;\n" +
            "    background: #000040;\n" +
            "    margin:0;\n" +
            "    padding:6px 15px !important;\n" +
            "    text-align:right;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "body {\n" +
            "    font-family: Verdana, Geneva, sans-serif;\n" +
            "    font-size: 24px;\n" +
            "    background: #000; color: #ffffff;\n" +
            "\n" +
            "    border: 0px;\n" +
            "    margin: 0px;\n" +
            "    padding: 0px;\n" +
            "}\n" +
            "\n" +
            "a:link     { color: #ffffc0; text-decoration: none; }\n" +
            "a:visited  { color: #dddd80; text-decoration: none; }\n" +
            "a:hover    { color: #dddd80; text-decoration: none; }\n" +
            "a:active   { color: #dddd80; text-decoration: none; }\n" +
            "}\n" +
            "\n" +
            "a.visitedLink:link     { color: #dddd80; text-decoration: none; }\n" +
            "a.visitedLink:visited  { color: #dddd80; text-decoration: none; }\n" +
            "a.visitedLink:hover    { color: #dddd80; text-decoration: none; }\n" +
            "a.visitedLink:active   { color: #dddd80; text-decoration: none; }\n" +
            "}\n";
    public int defaultItemsPerPage = 50;
    public String defaultFeedDateFormat = "MM/dd HH:mm";

    public int maxSizeForReadArticles = 80;

    private static Config CONFIG;

    private Config(String propertiesFileName) {
        try {
            properties.load(new FileInputStream(propertiesFileName));

            mainReplicationFactor = getInt("mainReplicationFactor", mainReplicationFactor);
            mainStrategyClass = getString("mainStrategyClass", mainStrategyClass);
            mainReadConsistency = getConsistencyLevel("mainReadConsistency", mainReadConsistency);
            mainWriteConsistency = getConsistencyLevel("mainWriteConsistency", mainWriteConsistency);

            electionReplicationFactor = getInt("electionReplicationFactor", electionReplicationFactor);
            electionStrategyClass = getString("electionStrategyClass", electionStrategyClass);
            electionReadConsistency = getConsistencyLevel("electionReadConsistency", electionReadConsistency);
            electionWriteConsistency = getConsistencyLevel("electionWriteConsistency", electionWriteConsistency);

            startWebServer = getBoolean("startWebServer", startWebServer);
            startFeedCrawler = getBoolean("startFeedCrawler", startFeedCrawler);
            startFeedManager = getBoolean("startFeedManager", startFeedManager);

            cassandraMaxConnPerHost = getInt("cassandraMaxConnPerHost", cassandraMaxConnPerHost);
            cassandraDbSeeds = getString("cassandraDbSeeds", cassandraDbSeeds);
            cassandraDbPort = getInt("cassandraDbPort", cassandraDbPort);

            cqlVersion = getString("cqlVersion", cqlVersion);
            targetCassandraVersion = getString("targetCassandraVersion", targetCassandraVersion);

            clusterName = getString("clusterName", clusterName);

            httpPort = getInt("httpPort", httpPort);
            httpOutputBufferSize = getInt("httpOutputBufferSize", httpOutputBufferSize);
            httpIdleTimeout = getDuration("httpIdleTimeout", httpIdleTimeout);
            httpsPort = getInt("httpsPort", httpsPort);
            httpsIdleTimeout = getDuration("httpsIdleTimeout", httpsIdleTimeout);
            httpsKeystore = getString("httpsKeystore", httpsKeystore);
            httpsKeystorePassword  = getString("httpsKeystorePassword", httpsKeystorePassword);

            activateAccountsAtCreation = getBoolean("activateAccountsAtCreation", activateAccountsAtCreation);

            sleepWhenNothingToCrawl = getDuration("sleepWhenNothingToCrawl", sleepWhenNothingToCrawl);
            threadsPerCrawler = getInt("threadsPerCrawler", threadsPerCrawler);
            crawlPeriod = getDuration("crawlPeriod", crawlPeriod);

            feedDistributionInterval = getDuration("feedDistributionInterval", feedDistributionInterval);
            feedIdSize = getInt("feedIdSize", feedIdSize);

            loginExpireInterval = getDuration("loginExpireInterval", loginExpireInterval);
            cookieExpireInterval = getDuration("cookieExpireInterval", cookieExpireInterval);

            crawlerWaitBetweenSaves = getDuration("crawlerWaitBetweenSaves", crawlerWaitBetweenSaves);

            feedManagerTicksBeforeBecomingLeader = getInt("feedManagerTicksBeforeBecomingLeader", feedManagerTicksBeforeBecomingLeader);

            feedManagerTickInterval = getDuration("feedManagerTickInterval", feedManagerTickInterval);

            defaultStyle = getString("defaultStyle", defaultStyle);
            //defaultStyle = defaultStyle.replaceAll("       +", "");
            defaultStyle = defaultStyle.replaceAll("      +", ""); //ttt1 see how to do this right (so the leading spaces are kept and the leading ones
                // are discarded )

            defaultItemsPerPage = getInt("defaultItemsPerPage", defaultItemsPerPage);

            defaultFeedDateFormat = getString("defaultFeedDateFormat", defaultFeedDateFormat);

            maxSizeForReadArticles = getInt("maxSizeForReadArticles", maxSizeForReadArticles);
        } catch (IOException e) {
            LOG.error("Error trying to initialize the configuration", e);
        }
    }

    public static void setup(String propertiesFile) {
        CONFIG = new Config(propertiesFile);
    }

    public static Config getConfig() { //ttt1 make auto-reload
        return CONFIG;
    }

    private int getInt(String key, int defaultVal) {
        String s = properties.getProperty(key, "" + defaultVal);
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            LOG.error("Error parsing as int: " + s);
            return defaultVal;
        }
    }

    private boolean getBoolean(String key, boolean defaultVal) {
        String s = properties.getProperty(key, "" + defaultVal);
        return "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    private long getDuration(String key, long defaultVal) {
        String s = properties.getProperty(key, "" + defaultVal);
        try {
            return Utils.stringToDuration(s);
        } catch (Exception e) {
            LOG.error("Error parsing as duration: " + s);
            return defaultVal;
        }
    }

    private String getString(String key, String defaultVal) {
        String s = properties.getProperty(key, defaultVal);
        return s;
    }

    private ConsistencyLevel getConsistencyLevel(String key, ConsistencyLevel defaultVal) {
        String s = properties.getProperty(key, "" + defaultVal);
        try {
            return ConsistencyLevel.valueOf(s);
        } catch (Exception e) {
            LOG.error("Error parsing as ConsistencyLevel: " + s);
            return defaultVal;
        }
    }
}
