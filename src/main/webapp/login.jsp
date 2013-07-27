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

<%
String userId = request.getParameter(ReaderHandler.PARAM_USER_ID);
if (userId == null) {
    userId = "";
}
%>

<html>

    <head>
    <title>Bluehorn Reader Login</title>
    <%=ReaderHandler.getStyle(null)%>
    </head>

    <body>
        <form action="<%=ReaderHandler.ACTION_LOGIN%>" method="post">
            <table border="0" align="right">
                <tr>
                    <td></td>
                    <td align="center"><b>Bluehorn Reader Login</b></td>
                </tr>
                <tr>
                    <td align="right"><b>User ID</b></td>
                    <!-- ttt0 make sure userId is sent only when it should -->
                    <td><input type="text" name="<%=ReaderHandler.PARAM_USER_ID%>" size="50" autocomplete="off" value="<%=userId%>"/></td>
                </tr>
                <tr>
                    <td align="right"><b>Password</b></td>
                    <td><input type="password" name="<%=ReaderHandler.PARAM_PASSWORD%>" size="50"/></td>
                </tr>
                <tr>
                    <td align="right"><b>Remember Account</b></td>
                    <td><input type="checkbox" name="<%=ReaderHandler.PARAM_REMEMBER_ACCOUNT%>" value="true"></td>
                </tr>
                <tr>
                    <td></td>
                    <td align="center"><input type="submit" value="Log in to Bluehorn Reader"/></td>
                </tr>
            </table>
        </form>
        <p/>
        <a href="/signup">Sign up</a>
    </body>
</html>
