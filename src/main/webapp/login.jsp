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
