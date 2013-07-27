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

import net.bluehornreader.model.*;
import org.apache.commons.logging.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.jdom2.*;
import org.jdom2.input.*;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-06-30
 * Time: 20:02
 * <p/>
 */
public class RssParser {

    private static final Log LOG = LogFactory.getLog(RssParser.class);

    private SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz", Locale.US);

    public static class Results {
        public ArrayList<Article> articles = new ArrayList<>();
        public String feedName = "";
    }

    public Results parseRdf(String addr, String feedId, long earliestToInclude) throws Exception {
        //ttt1 review earliestToInclude; quite likely its usage adds duplicates or misses entries; probably don't add 1 to avoid losses and have
        //      FeedCrawlerService check before saving if an article already exists
        // ttt2 see if how to deal with new articles that have an older publish time; currently they are discarded

        LOG.info(String.format("Parsing feed %s from %s (%s)", feedId, earliestToInclude, fmt.format(new Date(earliestToInclude))));
        Results results = new Results();

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(addr);
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            InputStream in = entity.getContent();
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(in);

            Element rootNode = document.getRootElement();
            List<Element> channels = rootNode.getChildren("channel");
            for (Element channel : channels) {
                if (results.feedName.isEmpty()) {
                    Element feedTitle = channel.getChild("title");
                    if (feedTitle != null) {
                        results.feedName = feedTitle.getText();
                        LOG.info(String.format("Found name %s for feed %s", results.feedName, feedId));
                        //results.feedName = "";
                    }
                }
                List<Element> items = channel.getChildren("item");
                for (Element item : items) {
                    Element title = item.getChild("title");
                    Element link = item.getChild("link");
                    Element pubDate = item.getChild("pubDate");

                    if (pubDate == null) {
                        // ttt2 apparently pubDate is optional; not sure what to do in such a case
                        LOG.error("Missing pubDate when parsing feed " + feedId);
                    } else {
                        long publishTime = fmt.parse(pubDate.getText()).getTime();
                        if (publishTime >= earliestToInclude) {
                            Article article = new Article(feedId, 0, cleanTitle(title.getText()), "", link.getText(), "", publishTime);
                            results.articles.add(article);
                        }
                    }
                }
            }
            in.close();
        }

        Collections.sort(results.articles, new Comparator<Article>() {
            @Override
            public int compare(Article o1, Article o2) {
                long a = o1.publishTime - o2.publishTime;
                return a > 0 ? 1 : a < 0 ? -1 : 0;
            }
        });
        LOG.info(String.format("Done parsing feed %s from %s (%s)", feedId, earliestToInclude, fmt.format(new Date(earliestToInclude))));
        return results;
    }

    //ttt1 see how better to deal with these
    static String[] TO_REMOVE = new String[] { "</?[a-zA-Z]+>", "<[a-zA-Z]+/>" };
    public static String cleanTitle(String s) {
        for (String del : TO_REMOVE) {
            s = s.replaceAll(del, "");
        }
        return s;
    }



    public static void main(String[] args) throws Exception {
        //parseRdf("http://rss.cnn.com/rss/edition_europe.rss");
        RssParser rssParser = new RssParser();
        //for (Article article : rssParser.parseRdf("http://rss.com.com/2547-12-0-20.xml", "feed1", 1372610355000L)) {
        //Results results = rssParser.parseRdf("http://rss.cnn.com/rss/edition_europe.rss", "feed1", 1372503394000L);
        Results results = rssParser.parseRdf("http://newsrss.bbc.co.uk/rss/newsonline_world_edition/europe/rss.xml", "feed1", 0);
        for (Article article : results.articles) {
            System.out.println(article.toString());
        }
    }
}


