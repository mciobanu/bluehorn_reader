/*
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
 */

package net.bluehornreader.web;

import org.apache.commons.logging.*;

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

    private static final Log LOG = LogFactory.getLog(RedirectServlet.class);


    private String target;

    RedirectServlet(String target) {
        this.target = target;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LOG.info("Redirecting to " + target);
            request.getRequestDispatcher(target).forward(request, response);
        }
        catch (Throwable e1) {
            e1.printStackTrace();
        }
    }
}
