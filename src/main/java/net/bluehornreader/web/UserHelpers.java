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

import net.bluehornreader.model.*;
import org.apache.commons.logging.*;
import org.eclipse.jetty.server.*;

import javax.servlet.http.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-06-16
 * Time: 23:03
 * <p/>
 */
public class UserHelpers {

    private static final Log LOG = LogFactory.getLog(UserHelpers.class);

    private LoginInfo.DB loginInfoDb;
    private User.DB userDb;

    public UserHelpers(LoginInfo.DB loginInfoDb, User.DB userDb) {
        this.loginInfoDb = loginInfoDb;
        this.userDb = userDb;
    }

    public static LoginInfo.SessionInfo getSessionInfo(Request request) {
        Cookie[] cookies = request.getCookies();
        LoginInfo.SessionInfo res = new LoginInfo.SessionInfo();
        String cookieRepr = "";
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookieRepr += WebUtils.cookieAsString(cookie);
                if (cookie.getName().equals(ReaderHandler.SESSION_ID)) {
                    res.sessionId = cookie.getValue();
                }
                if (cookie.getName().equals(ReaderHandler.BROWSER_ID)) {
                    res.browserId = cookie.getValue();
                }
                cookieRepr += "       ";
            }
        }
        LOG.info("cookies: " + cookieRepr);
        return res;
    }

    /**
     * @return LoginInfo, if found; if not found or sessionInfo has null fields, returns null
     *
     * @throws Exception only if the underlying DB throws; otherwise it returns null
     */
    private LoginInfo getLoginInfo(LoginInfo.SessionInfo sessionInfo) throws Exception {
        if (sessionInfo.isNull()) {
            return null;
        }
        return loginInfoDb.get(sessionInfo.browserId, sessionInfo.sessionId);
    }

    /**
     * @param request
     * @return the login info corresponding to the cookie in request; normally it returns null rather than throwing
     * @throws Exception only if the underlying DB throws; otherwise it returns null
     */
    public LoginInfo getLoginInfo(Request request) throws Exception {
        LoginInfo.SessionInfo sessionInfo = getSessionInfo(request);
        return getLoginInfo(sessionInfo);
    }


    /**
     * @param request
     * @return null if user not found
     * @throws Exception
     */
    public User getUser(Request request) throws Exception {
        LoginInfo loginInfo = getLoginInfo(request);
        if (loginInfo == null) {
            return null;
        }
        return userDb.get(loginInfo.userId);
    }
}
