<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="net.bluehornreader.web.*" %>
<%@ page import="net.bluehornreader.model.*" %>


<html>

<head>
<title>Bluehorn Reader</title>
<%@include file="style.jsp" %>
</head>

<body>

<%@include file="header.jsp" %>

<% User user = (User)request.getAttribute(ReaderHandler.VAR_USER); %>

<div id="header">
    <p id="headerP">
        <%=user.name%>
        <a href="<%=ReaderHandler.PATH_FEEDS%>">Feeds</a>
        <a href="/">Home</a>
        <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>

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
        LoginInfo loginInfo = (LoginInfo)request.getAttribute(ReaderHandler.VAR_LOGIN_INFO);
        minSeq = Math.max(maxSeq + 1 - loginInfo.itemsPerPage, 0);
    }
}


if (maxSeq + 1 > minSeq && minSeq > 0) {
    out.println("<a href=\"" + ReaderHandler.PATH_FEED + "/" + feedId + "/" + (minSeq - 1) + "\">Next page</a>");
}
%>

    </p>
</div>

<br/><br/>

</p>

<%
if (feed == null) {
    out.println("Feed " + feedId + " not found");
} else {
    if (maxSeq + 1 > minSeq && minSeq >=0) {
        Article.DB articleDb = (Article.DB)request.getAttribute(ReaderHandler.VAR_ARTICLE_DB);
        List<Article> articles = articleDb.get(feedId, minSeq, maxSeq + 1);
        for (Article article : articles) {
            out.println("<a target=_blank href=\"" + article.url + "\">" + StringEscapeUtils.escapeHtml(article.title) + "</a><br/>");
            // ttt0 use StringEscapeUtils.escapeHtml where needed
        }
    } else {
        out.println("Feed " + feed.name + " is empty");
    }
}
%>


<p/>

</body>

</html>

