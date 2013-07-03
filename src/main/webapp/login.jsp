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
    <%@include file="style.jsp" %>
    </head>


    <body>
        <form action=<%=ReaderHandler.ACTION_LOGIN%> method=post>
            <table border=0 align=right>
                <tr>
                    <td></td>
                    <td align=center><b>Bluehorn Reader Login</b></td>
                </tr>
                <tr>
                    <td align=right><b>User ID</b></td>
                    <td><input type=text name=<%=ReaderHandler.PARAM_USER_ID%> size=50 value="<%=userId%>"/></td>
                </tr>
                <tr>
                    <td align=right><b>Password</b></td>
                    <td><input type=password name=<%=ReaderHandler.PARAM_PASSWORD%> size=50/></td>
                </tr>
                <tr>
                    <td align=right><b>Remember Account</b></td>
                    <td><input type=checkbox name=<%=ReaderHandler.PARAM_REMEMBER_ACCOUNT%> value="true"></td>
                </tr>
                <tr>
                    <td></td>
                    <td align=center><input type=submit value="Log in to Bluehorn Reader"/></td>
                </tr>
            </table>
            <input type=hidden name=<%=ReaderHandler.PARAM_OLD_LOGIN_ID%> size=50 value="<%=request.getParameter(ReaderHandler.PARAM_OLD_LOGIN_ID)%>"/>
        </form>
        <p/>
        <a href="/signup">Sign up</a>
    </body>
</html>
