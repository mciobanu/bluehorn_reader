package net.bluehornreader.web;

import net.bluehornreader.*;
import org.apache.commons.logging.*;
import org.eclipse.jetty.server.*;

import javax.servlet.http.*;
import java.io.*;
import java.security.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-06-16
 * Time: 22:57
 * <p/>
 */
public class WebUtils {

    private static final Log LOG = LogFactory.getLog(WebUtils.class);


    public static void redirectToError(String error, Request request, HttpServletResponse httpServletResponse) throws Exception {
        /*MultiMap<String> params = new MultiMap<>(); //ttt2 see if can get this to work: whatever params are set in request, they are lost at redirection
        params.put(PARAM_ERROR, error);
        request.setParameters(params);
        request.???
        httpServletResponse.sendRedirect(PATH_ERROR);
         */

        PrintWriter out = httpServletResponse.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Bluehorn Reader Error</title>");
        out.println("</head>");

        out.println("<body>"); //ttt0 add formatting
        out.println("<h2>Error: " + error + "</h2>");
        out.println("<p/>");
        out.println("<a href=\"" + ReaderHandler.PATH_LOGIN + "\">Log in</a>");
        out.println("<p/>");
        out.println("<a href=\"" + ReaderHandler.PATH_SIGNUP + "\">Sign up</a>");
        out.println("</body>");
        out.println("</html>");

        request.setHandled(true);
    }

    public static void showResult(String result, String nextPath, Request request, HttpServletResponse httpServletResponse) throws Exception {
        PrintWriter out = httpServletResponse.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Bluehorn Reader</title>");
        out.println("</head>");

        out.println("<body>"); //ttt0 add formatting
        out.println("<h2>" + result + "</h2>");
        out.println("<p/>");
        out.println("<a href=\"" + nextPath + "\">Continue</a>");
        out.println("<p/>");
        out.println("</body>");
        out.println("</html>");

        request.setHandled(true);
    }

    public static String cookieAsString(Cookie cookie) {
        StringBuilder bld = new StringBuilder();
        bld.append("Name=").append(cookie.getName()).append(" ");
        bld.append("Value=").append(cookie.getValue()).append(" ");
        bld.append("Domain=").append(cookie.getDomain()).append(" ");
        bld.append("MaxAge=").append(cookie.getMaxAge()).append(" ");
        bld.append("Path=").append(cookie.getPath()).append(" ");
        bld.append("Secure=").append(cookie.getSecure()).append(" ");
        bld.append("Comment=").append(cookie.getComment()).append(" ");
        bld.append("Version=").append(cookie.getVersion()).append(" ");
        return bld.toString().trim();
    }

    public static String addCookie(HttpServletResponse httpServletResponse, boolean secured) {
        SecureRandom secureRandom = new SecureRandom();
        String loginId = "" + secureRandom.nextLong();
        Cookie cookie = new Cookie(ReaderHandler.LOGIN_ID, loginId);
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (Config.getConfig().cookieExpireInterval / 1000));
        cookie.setPath("/");
        if (secured) {
            cookie.setSecure(true);
        }
        LOG.info(cookieAsString(cookie));
        httpServletResponse.addCookie(cookie);
        return loginId;
    }
}
