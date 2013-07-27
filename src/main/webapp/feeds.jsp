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
        <%=user.name%> |
        <a href="<%=ReaderHandler.PATH_FEED_ADMIN%>">Feed list administration</a> |
        <a href="<%=ReaderHandler.PATH_SETTINGS%>">Settings</a> |
        <a href="/">Home</a> |
        <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>
    </p>
</div>
<br/><br/>

<p>
<%

Feed.DB feedDb = (Feed.DB)request.getAttribute(ReaderHandler.VAR_FEED_DB);
List<Feed> feeds = feedDb.get(user.feedIds);
Collections.sort(feeds, new Comparator<Feed>() {
    @Override
    public int compare(Feed o1, Feed o2) {
        return o1.name.compareToIgnoreCase(o2.name);
    }
});

for (Feed feed : feeds) {
    out.println("<a href=\"" + ReaderHandler.PATH_FEED + "/" + feed.feedId + "\">" + StringEscapeUtils.escapeHtml(feed.name) + "</a><br/>");
}
%>
</p>


<p/>



</body>

</html>
