package net.bluehornreader.service;

import net.bluehornreader.model.*;
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

    private SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz", Locale.US);

    public ArrayList<Article> parseRdf(String addr, String feedId, long earliestToInclude) throws Exception {
        //ttt0 review earliestToInclude; quite likely its usage adds duplicates or misses entries
        // ttt0 see if how to deal with new articles that have an older publish time
        ArrayList<Article> res = new ArrayList<>();
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
                List<Element> items = channel.getChildren("item");
                for (Element item : items) {
                    Element title = item.getChild("title");
                    Element link = item.getChild("link");
                    Element pubDate = item.getChild("pubDate");

                    long publishTime = fmt.parse(pubDate.getText()).getTime();
                    if (publishTime >= earliestToInclude) {
                        Article article = new Article(feedId, 0, title.getText(), "", link.getText(), "", publishTime);
                        res.add(article);
                    }
                }
            }
            in.close();
        }

        Collections.sort(res, new Comparator<Article>() {
            @Override
            public int compare(Article o1, Article o2) {
                return (int) (o1.publishTime - o2.publishTime);
            }
        });
        return res;
    }


    public static void main(String[] args) throws Exception {
        //parseRdf("http://rss.cnn.com/rss/edition_europe.rss");
        RssParser rssParser = new RssParser();
        //for (Article article : rssParser.parseRdf("http://rss.com.com/2547-12-0-20.xml", "feed1", 1372610355000L)) {
        for (Article article : rssParser.parseRdf("http://rss.cnn.com/rss/edition_europe.rss", "feed1", 1372503394000L)) {
            System.out.println(article.toString());
        }
    }
}

//ttt0 switch to Java 7 syntax
