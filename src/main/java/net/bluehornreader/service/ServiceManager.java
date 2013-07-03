package net.bluehornreader.service;

import net.bluehornreader.*;
import net.bluehornreader.data.*;
import org.apache.commons.logging.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-05-06
 * Time: 00:43
 * <p/>
 *
 * Starts / stops services
 */
public class ServiceManager {

    private static final Log LOG = LogFactory.getLog(ServiceManager.class);

    private ArrayList<Service> services = new ArrayList<Service>();
    private LowLevelDbAccess lowLevelDbAccess;

    public ServiceManager(String webDir) {

        try {
            lowLevelDbAccess = new LowLevelDbAccess();

            if (Config.getConfig().startFeedManager) {
                LOG.info("Starting FeedManagerService");
                FeedManagerService feedManagerService = new FeedManagerService(lowLevelDbAccess);
                services.add(feedManagerService);
                feedManagerService.start();
                /*Thread t = new Thread(feedManagerService);
                t.setName("FeedManagerService");
                t.start();*/
            }

            if (Config.getConfig().startFeedCrawler) {
                LOG.info("Starting FeedCrawlerService");
                FeedCrawlerService feedCrawlerService = new FeedCrawlerService(lowLevelDbAccess);
                services.add(feedCrawlerService);
                /*Thread t = new Thread(feedCrawlerService);
                t.setName("FeedCrawlerService");
                t.start();*/
                feedCrawlerService.start();
            }

            if (Config.getConfig().startWebServer) {
                LOG.info("Starting WebServerService");
                WebServerService webServerService = new WebServerService(lowLevelDbAccess, webDir);
                services.add(webServerService);
                /*Thread t = new Thread(feedCrawlerService);
                t.setName("FeedCrawlerService");
                t.start();*/
                webServerService.start();
            }

        } catch (Exception e) {
            LOG.fatal("Failed to start services. Exiting ...", e);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                LOG.error("Failed sleep before exiting ...");
            }
        }
    }

    private void notifyThreadsToExit() {
        for (Service service : services) {
            service.signalExit();
        }
    }

    private void waitForThreadsToFinish() {
        for (Service service : services) {
            service.waitForThreadsToFinish();
        }
    }

    private void shutdown() {
        notifyThreadsToExit();
        waitForThreadsToFinish();
        if (lowLevelDbAccess != null) {
            lowLevelDbAccess.shutDown();
        }
    }

    public static void main(String[] args) throws InterruptedException {

        if (args.length != 2) {
            System.err.println("Usage: ServiceManager <config_file> <jsp_dir>"); //ttt0 see about compiling the JSPs
            System.exit(1);
        }

        Config.setup(args[0]);
        final ServiceManager serviceManager = new ServiceManager(args[1]);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOG.info("***************************************************************************\n\n");
                LOG.info("Shutting down ServiceManager ...");
                serviceManager.shutdown();
            }
        }));
    }
}
