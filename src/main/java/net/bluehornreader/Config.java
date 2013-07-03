package net.bluehornreader;

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

    //ttt0 read from files

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

    public int maxConnPerHost = 100;
    public String dbSeeds = "127.0.0.1:9160";
    public int dbPort = 9160;

    public String cqlVersion = "3.0.0";
    public String targetCassandraVersion = "1.2";

    public String clusterName = "Cluster01"; // not sure when it's used; when just running on localhost doesn't seem to matter

    public int httpPort = 8080;
    public int httpOutputBufferSize = 32768;
    public long httpIdleTimeout = Utils.stringToDuration("30s");
    public int httpsPort = 8443;
    public long httpsIdleTimeout = Utils.stringToDuration("30s");
    public String httpsKeystore;
    public String httpsKeystorePassword;

    public boolean activateAccountsAtCreation = true;

    public long getCrawlerTickInterval() {
        return 2 * feedManagerTickInterval;   // CrawlerTickInterval should be bigger than feedManagerTickInterval to allow detection of a dead feed manager
    }

    //public long sleepWhenNothingToCrawl = Utils.stringToDuration("1m");
    public long sleepWhenNothingToCrawl = Utils.stringToDuration("10s");
    public int threadsPerCrawler = 1; //ttt0 increase
    public long crawlPeriod = Utils.stringToDuration("15m");

    public long feedDistributionInterval = Utils.stringToDuration("10m");

    public long loginExpireInterval = Utils.stringToDuration("2w");
    public long cookieExpireInterval = Utils.stringToDuration("52w");

    public int getTicksBetweenCrawls() { // number of ticks that have to pass before a feed is crawled again
        return (int) (crawlPeriod / getCrawlerTickInterval());
    }

    public long crawlerWaitBetweenSaves = Utils.stringToDuration("1s");

    public int feedManagerTicksBeforeBecomingLeader = 4;

    //public long feedManagerTickInterval = Utils.stringToDuration("1m");
    public long feedManagerTickInterval = Utils.stringToDuration("2s");

    private static Config CONFIG;

    private Config(String propertiesFileName) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFileName));  // ttt0 add the other settings
            httpsKeystore = (String) properties.get("httpsKeystore");
            httpsKeystorePassword  = (String) properties.get("httpsKeystorePassword");
        } catch (IOException e) {
            LOG.error("Error trying to initialize the configuration", e);
        }
    }

    public static void setup(String propertiesFile) {
        CONFIG = new Config(propertiesFile);
    }

    public static Config getConfig() { //ttt0 make auto-reload
        return CONFIG;
    }
}
