<%@ page import="java.util.*" %>
<%@ page import="net.bluehornreader.web.*" %>

<html>

    <head>
    <title>Sign up for Bluehorn Reader</title>
    <%=ReaderHandler.getStyle(null)%>
    </head>

    <body>
        <form action="<%=ReaderHandler.ACTION_SIGNUP%>" method="post">
            <table border="0" align="right">
                <tr>
                    <td></td>
                    <td align="center"><b>Bluehorn Reader Sign Up</b></td>
                </tr>
                <tr>
                    <td align="right"><b>User ID</b></td>
                    <td><input type="text" name="<%=ReaderHandler.PARAM_USER_ID%>" autocomplete="off" size="50"/></td>
                </tr>
                <tr>
                    <td align="right"><b>User name</b></td>
                    <td><input type="text" name="<%=ReaderHandler.PARAM_USER_NAME%>" autocomplete="off" size="50"/></td>
                </tr>
                <tr>
                    <td align="right"><b>Email</b></td>
                    <td><input type="text" name="<%=ReaderHandler.PARAM_EMAIL%>" autocomplete="off" size="50"/></td>
                </tr>
                <tr>
                    <td align="right"><b>Password</b></td>
                    <td><input type="password" name="<%=ReaderHandler.PARAM_PASSWORD%>" autocomplete="off" size="50"/></td>
                </tr>
                <tr>
                    <td align="right"><b>Password confirmation</b></td>
                    <td><input type="password" name="<%=ReaderHandler.PARAM_PASSWORD_CONFIRM%>" autocomplete="off" size="50"/></td>
                </tr>
                <tr>
                    <td></td>
                    <td align="center"><input type="submit" value="Sign up for Bluehorn Reader"/></td>
                </tr>
            </table>
        </form>
        <p/>
        <a href="<%=ReaderHandler.PATH_LOGIN%>">Log in</a>
    </body>
</html>
