<html>
<head>
<title>Bluehorn Reader Error</title>
</head>
<body>

<%@ page import="java.util.*" %>
<%@ page import="net.bluehornreader.web.*" %>

<h2>Error: <%=request.getParameter(ReaderHandler.PARAM_ERROR)%></h2>

<p/>
<a href="/login">Log in</a>

<p/>
<a href="/signup">Sign up</a>

</body>
</html>
