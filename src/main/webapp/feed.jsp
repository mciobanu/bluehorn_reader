<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="net.bluehornreader.web.*" %>
<%@ page import="net.bluehornreader.model.*" %>

<% User user = (User)request.getAttribute(ReaderHandler.VAR_USER); %>
<% LoginInfo loginInfo = (LoginInfo)request.getAttribute(ReaderHandler.VAR_LOGIN_INFO); %>

<html>


<head>
<title>Bluehorn Reader</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<%=ReaderHandler.getStyle(loginInfo)%>
</head>

<body>

<%@include file="header.jsp" %>


<%
String path = request.getParameter(ReaderHandler.PARAM_PATH);

// maxSeq is the newest to be shown

ReaderHandler.FeedInfo feedInfo = ReaderHandler.getFeedInfo(path);
String feedId = feedInfo.feedId;
int maxSeq = feedInfo.maxSeq;
int minSeq = -1;

Feed.DB feedDb = (Feed.DB)request.getAttribute(ReaderHandler.VAR_FEED_DB);
Feed feed = feedDb.get(feedId);
if (feed != null) {
    if (maxSeq == -1) {
        maxSeq = feed.maxSeq;
    }
    if (maxSeq >= 0) {
        minSeq = Math.max(maxSeq + 1 - loginInfo.itemsPerPage, 0);
    }
}

%>


<div id="header">
    <p id="headerP">
        <%=feed.name%>
        <%=user.name%>
        <a href="<%=ReaderHandler.PATH_FEEDS%>">Feeds</a>
        <a href="/">Home</a>
        <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>


<%
if (maxSeq + 1 > minSeq && minSeq > 0) {
    out.println("<a href=\"" + ReaderHandler.PATH_FEED + "/" + feedId + "/" + (minSeq - 1) + "\">Next page</a>");
}
%>

    </p>
</div>

<br/><br/>

<p></p>

<%
if (feed == null) {
    out.println("Feed " + feedId + " not found");
} else {
    //SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.YYYY HH:mm");
    SimpleDateFormat fmt = new SimpleDateFormat("dd.MM HH:mm");
    if (maxSeq + 1 > minSeq && minSeq >= 0) {
        Article.DB articleDb = (Article.DB)request.getAttribute(ReaderHandler.VAR_ARTICLE_DB);
        ReadArticlesColl.DB readArticlesCollDb = (ReadArticlesColl.DB)request.getAttribute(ReaderHandler.VAR_READ_ARTICLES_COLL_DB);
        ReadArticlesColl readArticlesColl = readArticlesCollDb.get(user.userId, feedId);

        List<Article> articles = articleDb.get(feedId, minSeq, maxSeq + 1);
        //System.out.printf("########################## retrieved from %d to %d%n", minSeq, maxSeq + 1);

        /*Collections.sort(articles, new Comparator<Article>() {
            @Override
            public int compare(Article o1, Article o2) {
                return o1.seq - o2.seq;
            }
        });*/

        /*Collections.sort(articles, new Comparator<Article>() {
            @Override
            public int compare(Article o1, Article o2) {
                return o2.seq - o1.seq;
            }
        });*/

        Collections.reverse(articles);

        for (Article article : articles) {
            //out.println(article.seq + " " + fmt.format(new Date(article.publishTime)) + " <a target=_blank href=\"" + StringEscapeUtils.escapeHtml(article.url) + "\">" + StringEscapeUtils.escapeHtml(article.title) + "</a><br/>");
            //out.println(article.seq + " " + fmt.format(new Date(article.publishTime)) + " <a target=_blank href=\"" + ReaderHandler.PATH_OPEN_ARTICLE + article.feedId + "/" + article.seq + "\">" + StringEscapeUtils.escapeHtml(article.title) + "</a><br/>");
            out.println(/*article.seq + " " +*/ fmt.format(new Date(article.publishTime)) +
                    (readArticlesColl != null && readArticlesColl.isRead(article.seq) ? " * " : "") +
                    " <a target=_blank href=\"" + ReaderHandler.PATH_OPEN_ARTICLE + article.feedId + "/" + article.seq + "\">" + StringEscapeUtils.escapeHtml(article.title) + "</a><br/>");

            // ttt1 use StringEscapeUtils.escapeHtml where needed
        }
    } else {
        out.println("Feed " + feed.name + " is empty");
    }
}
%>


<p/>

</body>

</html>

