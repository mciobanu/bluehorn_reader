<%@ page import="java.util.*" %>
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

<form action="<%=ReaderHandler.ACTION_ADD_FEED%>" method="post">
    <table border="0">
        <tr>
            <td><input type="text" name="<%=ReaderHandler.PARAM_NEW_FEED_URL%>" size="100"/></td>
            <td align="center"><input type="submit" value="Add feed"/></td>
        </tr>
    </table>
</form>


<p>

<%
Feed.DB feedDb = (Feed.DB)request.getAttribute(ReaderHandler.VAR_FEED_DB);
List<Feed> feeds = feedDb.get(user.feedIds);
%>

<table border="0">

<%
for (Feed feed : feeds) {
%>

    <tr>
        <td>
            <%=feed.name%>
        </td>
        <td>
            <form action="<%=ReaderHandler.ACTION_REMOVE_FEED%>" method="post">
                <input type="hidden" name="<%=ReaderHandler.PARAM_FEED_ID%>" value="<%=feed.feedId%>"/>
                <input type="submit" value="Remove feed"/>
            </form>
        </td>
    </tr>

<%
}
%>

</table>


</p>


</body>

</html>

