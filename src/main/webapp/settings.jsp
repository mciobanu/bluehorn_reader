<%@ page import="java.util.*" %>
<%@ page import="net.bluehornreader.web.*" %>
<%@ page import="net.bluehornreader.model.*" %>

<% User user = (User)request.getAttribute(ReaderHandler.VAR_USER); %>

<html>

    <head>
    <title>Bluehorn Reader Settings</title>
    <%@include file="style.jsp" %>
    </head>

    <body>
        <div id="header">
            <p id="headerP">
                <%=user.name%>
                <a href="<%=ReaderHandler.PATH_FEEDS%>">Feeds</a>
                <a href="/">Home</a>
                <a href="<%=ReaderHandler.PATH_LOGOUT%>">Log out</a>
            </p>
        </div>
        <br/><br/>

        <form action=<%=ReaderHandler.ACTION_CHANGE_PASSWORD%> method=post>
            <table border=0>
                <tr>
                    <td align=right><b>Current password</b></td>
                    <td><input type=password name=<%=ReaderHandler.PARAM_CURRENT_PASSWORD%> size=50/></td>
                </tr>
                <tr>
                    <td align=right><b>New password</b></td>
                    <td><input type=password name=<%=ReaderHandler.PARAM_PASSWORD%> size=50/></td>
                </tr>
                <tr>
                    <td align=right><b>New password confirmation</b></td>
                    <td><input type=password name=<%=ReaderHandler.PARAM_PASSWORD_CONFIRM%> size=50/></td>
                </tr>
                <tr>
                    <td></td>
                    <td align=center><input type=submit value="Change password"/></td>
                </tr>
            </table>
        </form>
        <br/>
        <br/>
        <br/>
        <form action=<%=ReaderHandler.ACTION_CHANGE_SETTINGS%> method=post>
            <table border=1>
                <tr>
                    <td></td>
                    <td align=center><input type=submit value="Change settings"/></td>
                </tr>
            </table>
        </form>
    </body>
</html>
