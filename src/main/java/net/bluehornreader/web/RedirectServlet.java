package net.bluehornreader.web;

import javax.servlet.http.*;
import java.io.*;

/**
* Created with IntelliJ IDEA.
* User: ciobi
* Date: 2013-06-16
* Time: 22:59
* <p/>
*/ // ttt2 see how to get rid of this; seems needed for passing URL paths as params to JSPs
class RedirectServlet extends HttpServlet {

    private String target;

    RedirectServlet(String target) {
        this.target = target;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            request.getRequestDispatcher(target).forward(request, response);
        }
        catch (Throwable e1) {
            e1.printStackTrace();
        }
    }
}
