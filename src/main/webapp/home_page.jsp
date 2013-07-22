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

<div id="header">
    <p id="headerP">
        <%=user.name%>
        <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>
    </p>
</div>
<br/><br/>


<h2>Bluehorn Reader</h2>

<p/>
<a href="<%=ReaderHandler.PATH_FEEDS%>">Feeds</a>

<p/>
<a href="<%=ReaderHandler.PATH_FEED_ADMIN%>">Feed list administration</a>

<p/>
<a href="<%=ReaderHandler.PATH_SETTINGS%>">Settings</a>

<p/>
<a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>



</body>
</html>
