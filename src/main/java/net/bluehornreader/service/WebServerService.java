package net.bluehornreader.service;

import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;
import net.bluehornreader.web.*;
import org.apache.commons.logging.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.*;

import java.util.*;


/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-05-06
 * Time: 01:10
 * <p/>
 */
public class WebServerService extends Service {

    private static final Log LOG = LogFactory.getLog(WebServerService.class);

    private LowLevelDbAccess lowLevelDbAccess;
    private Server jettyServer;
    private String webDir;

    /*
    Create a self-signed certificate: http://www.sslshopper.com/article-how-to-create-a-self-signed-certificate-using-java-keytool.html
        keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048
    (the usage for keytool in JDK7 when starting without a param doesn't seem to list these arguments, but it works)
    see also: https://www.sslshopper.com/article-most-common-java-keytool-keystore-commands.html
    see also: http://docs.oracle.com/cd/E17952_01/mysql-monitor-2.3-en/mem-program-reference-server-ssl.html
     */

    public WebServerService(LowLevelDbAccess lowLevelDbAccess, String webDir) {
        this.lowLevelDbAccess = lowLevelDbAccess;
        this.webDir = webDir;
    }

    @Override
    public void run() {
        try {
            LOG.info("Starting web server ...");
            // see http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/ManyConnectors.java
            // see http://stackoverflow.com/questions/14362245/programatically-configure-ssl-for-jetty-9-embedded

            int httpPort = Config.getConfig().httpPort;
            int httpsPort = Config.getConfig().httpsPort;

            jettyServer = new Server();

            HttpConfiguration httpConfiguration = new HttpConfiguration();
            httpConfiguration.setSecureScheme("https");
            httpConfiguration.setSecurePort(httpsPort);
            httpConfiguration.setOutputBufferSize(Config.getConfig().httpOutputBufferSize);

            ServerConnector http = new ServerConnector(jettyServer, new HttpConnectionFactory(httpConfiguration));
            http.setPort(httpPort);
            http.setIdleTimeout(Config.getConfig().httpIdleTimeout);

            Connector[] connectors;

            String httpsKeystore = Config.getConfig().httpsKeystore;
            String httpsKeystorePassword = Config.getConfig().httpsKeystorePassword;
            if (httpsKeystore != null && httpsKeystorePassword != null) {
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(httpsKeystore);
                sslContextFactory.setKeyStorePassword(httpsKeystorePassword);

                HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
                httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

                ServerConnector https = new ServerConnector(jettyServer,
                        new SslConnectionFactory(sslContextFactory, "http/1.1"),
                        new HttpConnectionFactory(httpsConfiguration));
                https.setPort(httpsPort);
                https.setIdleTimeout(Config.getConfig().httpsIdleTimeout);

                connectors = new Connector[]{ http, https };
            } else {
                connectors = new Connector[]{ http };
            }

            jettyServer.setConnectors(connectors);

            setupReaderHandler(jettyServer, lowLevelDbAccess);
            //server.setStopAtShutdown(true);
            jettyServer.start();
            jettyServer.join();
        } catch (Exception e) {
            LOG.error("Error starting the web server", e);
        }
    }


    @Override
    public void signalExit() {
        LOG.info("Shutting down web server ...");
        // !!! nothing to do, as Jetty was told when it was created to shut down when the process wants to exit
        try {
            jettyServer.stop();
        } catch (Exception e) {
            LOG.error("Error stopping the web server", e);
        }
    }

    @Override
    protected Collection<Thread> getChildThreads() {
        return new ArrayList<>();
    }


    private void setupReaderHandler(Server server, LowLevelDbAccess lowLevelDbAccess) {
        ReaderHandler readerHandler = new ReaderHandler(lowLevelDbAccess, webDir);
        server.setHandler(readerHandler);
    }


}


/*

http://wiki.eclipse.org/Jetty/Feature/Jetty_Maven_Plugin

The Jetty Maven plugin is useful for rapid development and testing. You can add it to any webapp project that is structured
according to the usual Maven defaults. The plugin can then periodically scan your project for changes and automatically
redeploy the webapp if any are found. This makes the development cycle more productive by eliminating the build and deploy
steps: you use your IDE to make changes to the project, and the running web container automatically picks them up, allowing
you to test them straight away.

*/
