<%/*

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

*/%>

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
        <%=user.name%> |
        <a href="<%=ReaderHandler.PATH_FEEDS%>">Feeds</a> |
        <a href="<%=ReaderHandler.PATH_SETTINGS%>">Settings</a> |
        <a href="/">Home</a> |
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

