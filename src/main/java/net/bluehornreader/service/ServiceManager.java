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

import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;
import org.apache.commons.logging.*;

import java.io.*;
import java.net.*;
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

    private ArrayList<Service> services = new ArrayList<>();
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

    public static void main(String[] args) throws Exception {

        String webappPath = null;

        if (Utils.isInJar()) {
            if (args.length != 1 || isHelp(args)) {

                System.err.println("Usage:\njava -Dlog4j.configuration=file:<PATH>/log4j.properties " +
                        "-jar bluehorn-reader-<VERSION>.one-jar.jar <PATH>/config.properties");
                System.exit(1);
            }
        } else {
            if ((args.length != 2 && args.length != 1) || isHelp(args)) {
                System.err.println("Usage:\njava -Dlog4j.configuration=file:<PATH>/log4j.properties " +
                        "net.bluehornreader.service.ServiceManager <PATH>/config.properties <PATH>/webapp");
                System.exit(1);
            }

            if (args.length == 2) {
                webappPath = args[1];
            } else {
                URL url = ServiceManager.class.getClassLoader().getResource("net/bluehornreader/model/Article.class");
                if (url != null) {
                    webappPath = url.getPath().replace("target/classes/net/bluehornreader/model/Article.class", "") + "src/main/webapp";
                    if (new File(webappPath).isDirectory()) {
                        LOG.info("Found webapp folder at " + webappPath);
                    } else {
                        webappPath = null;
                    }
                }

                if (webappPath == null) {
                    System.err.println("Cannot locate the webapp folder. You'll have to specify it manually, as the second parameter.");
                    System.exit(1);
                }
            }
        }

        if (webappPath == null) {
            webappPath = new File(".").getCanonicalPath(); // just to have a valid folder to pass
        }

        String configFileName = args[0];
        Config.setup(configFileName);
        final ServiceManager serviceManager = new ServiceManager(webappPath);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOG.info("***************************************************************************\n\n");
                LOG.info("Shutting down ServiceManager ...");
                serviceManager.shutdown();
            }
        }));
    }

    private static boolean isHelp(String[] args) {
        if (args.length == 0) {
            return true;
        }
        for (String s : args) {
            if (s.equalsIgnoreCase("-h") || s.equalsIgnoreCase("--help") || s.equalsIgnoreCase("-?")) {
                return true;
            }
        }
        return false;
    }
}

