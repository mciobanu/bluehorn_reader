<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="net.bluehornreader.web.*" %>
<%@ page import="net.bluehornreader.model.*" %>


<% User user = (User)request.getAttribute(ReaderHandler.VAR_USER); %>
<% LoginInfo loginInfo = (LoginInfo)request.getAttribute(ReaderHandler.VAR_LOGIN_INFO); %>

<html>

<head>
<title>Bluehorn Reader</title>
<%=ReaderHandler.getStyle(loginInfo)%>
</head>


<body>

<%@include file="header.jsp" %>



<!-- move to header.jsp -->


<div id="header">
    <p id="headerP">
        <%=user.name%>
        <a href="<%=ReaderHandler.PATH_FEED_ADMIN%>">Feed list administration</a>
        <a href="<%=ReaderHandler.PATH_SETTINGS%>">Settings</a>
        <a href="/">Home</a>
        <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>
    </p>
</div>
<br/><br/>

<p>
<%

Feed.DB feedDb = (Feed.DB)request.getAttribute(ReaderHandler.VAR_FEED_DB);
List<Feed> feeds = feedDb.get(user.feedIds);
for (Feed feed : feeds) {
    out.println("<a href=\"" + ReaderHandler.PATH_FEED + "/" + feed.feedId + "\">" + StringEscapeUtils.escapeHtml(feed.name) + "</a><br/>");
}
%>
</p>


<p/>



</body>

</html>
