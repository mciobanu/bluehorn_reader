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
                <%=user.name%>
                <a href="<%=ReaderHandler.PATH_FEEDS%>">Feeds</a>
                <a href="/">Home</a>
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
            </textarea> <br/>
            <input type="submit" value="Change settings"/>
        </form>
    </body>
</html>
