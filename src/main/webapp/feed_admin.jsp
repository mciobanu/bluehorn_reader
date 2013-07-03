<%@ page import="java.util.*" %>
<%@ page import="net.bluehornreader.web.*" %>
<%@ page import="net.bluehornreader.model.*" %>

<% ReaderHandler.LOG.info("  JSP >>>>>>>>>>>>>>>>>>>>>>"); %>



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
        <a href="<%=ReaderHandler.PATH_SETTINGS%>">Settings</a>
        <a href="/">Home</a>
        <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>
    </p>
</div>

<br/><br/>

<form action=<%=ReaderHandler.ACTION_ADD_FEED%> method=post>
    <table border=0>
        <tr>
            <td><input type=text name=<%=ReaderHandler.PARAM_NEW_FEED%> size=100/></td>
            <td align=center><input type=submit value="Add feed"/></td>
        </tr>
    </table>
</form>


<p>

<%
Feed.DB feedDb = (Feed.DB)request.getAttribute(ReaderHandler.VAR_FEED_DB);
List<Feed> feeds = feedDb.get(user.feedIds);
for (Feed feed : feeds) {
    out.println(feed.name + "<br/>");
}
%>
</p>


</body>

</html>

<% ReaderHandler.LOG.info("  JSP <<<<<<<<<<<<<<<<<<<<<<"); %>
