<%@ page import="java.util.*" %>
<%@ page import="net.bluehornreader.web.*" %>
<%@ page import="net.bluehornreader.model.*" %>

<% User user = (User)request.getAttribute(ReaderHandler.VAR_USER); %>


<html>

<head>
<title>Bluehorn Reader</title>
<%@include file="style.jsp" %>
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
    out.println("<a href=\"" + ReaderHandler.PATH_FEED + "/" + feed.feedId + "\">" + feed.name + "</a><br/>");
}
%>
</p>


<p/>



</body>

</html>
