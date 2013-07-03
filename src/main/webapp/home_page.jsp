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

<div id="header">
    <p id="headerP">
        <%=user.name%>
        <a href="<%=ReaderHandler.PATH_FEED_ADMIN%>">Feed list administration</a>
        <a href="<%=ReaderHandler.PATH_SETTINGS%>">Settings</a>
        <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>
    </p>
</div>
<br/><br/>


<h2>Bluehorn Reader</h2>

<p/>
<a href="<%=ReaderHandler.PATH_FEEDS%>">Feeds</a>

<p/>
<a href="<%=ReaderHandler.PATH_SETTINGS%>">Settings</a>

<p/>
<a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>

</body>
</html>
