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
        <title>Bluehorn Reader Settings</title>
        <%=ReaderHandler.getStyle(null)%>
    </head>

    <body>
        <div id="header">
            <p id="headerP">
                <%=user.name%> |
                <a href="<%=ReaderHandler.PATH_FEEDS%>">Feeds</a> |
                <a href="/">Home</a> |
                <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>
            </p>
        </div>
        <br/><br/>

        <form action="<%=ReaderHandler.ACTION_CHANGE_PASSWORD%>" method="post">
            <table border="0">
                <tr>
                    <td align="right"><b>Current password</b></td>
                    <td><input type="password" name="<%=ReaderHandler.PARAM_CURRENT_PASSWORD%>" autocomplete="off" size="50"/></td>
                </tr>
                <tr>
                    <td align="right"><b>New password</b></td>
                    <td><input type="password" name="<%=ReaderHandler.PARAM_PASSWORD%>" autocomplete="off" size="50"/></td>
                </tr>
                <tr>
                    <td align="right"><b>New password confirmation</b></td>
                    <td><input type="password" name="<%=ReaderHandler.PARAM_PASSWORD_CONFIRM%>" autocomplete="off" size="50"/></td>
                </tr>
                <tr>
                    <td></td>
                    <td align="center"><input type="submit" value="Change password"/></td>
                </tr>
            </table>
        </form>
        <br/>
        <br/>
        <br/>
        <form action="<%=ReaderHandler.ACTION_CHANGE_SETTINGS%>" method="post">
            Items per page:
            <input type="text" name="<%=ReaderHandler.PARAM_ITEMS_PER_PAGE%>" size="10" value="<%=loginInfo.itemsPerPage%>"/> <br/><br/>
            CSS style:<br/>
            <textarea name="<%=ReaderHandler.PARAM_STYLE%>" cols="100" rows="30">
                <%=loginInfo.style%>
            </textarea> <br/><br/>
            Date format:
            <input type="text" name="<%=ReaderHandler.PARAM_FEED_DATE_FORMAT%>" size="20" value="<%=loginInfo.feedDateFormat%>"/> <br/><br/>
            <input type="submit" value="Change settings"/>
        </form>
    </body>
</html>
