package net.bluehornreader.web;

import net.bluehornreader.model.*;
import org.apache.commons.logging.*;
import org.eclipse.jetty.server.*;

import javax.servlet.http.*;
import java.util.*;

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

    public static String getLoginId(Request request) {
        Cookie[] cookies = request.getCookies();
        String loginId = null;
        String cookieRepr = "";
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookieRepr += WebUtils.cookieAsString(cookie);
                if (cookie.getName().equals(ReaderHandler.LOGIN_ID)) {
                    try {
                        cookieRepr += " " + new Date(Long.parseLong(cookie.getValue())).toString();
                        loginId = cookie.getValue();
                    } catch (Exception e) {
                        LOG.error("Exception parsing cookie", e);
                    }
                }
                cookieRepr += "       ";
            }
        }
        LOG.info("cookies: " + cookieRepr);
        return loginId;
    }

    /**
     * @param loginId
     * @return LoginInfo, if found; if not found or loginId is null, returns null
     *
     * @throws Exception only if the underlying DB throws; otherwise it returns null
     */
    public LoginInfo getLoginInfo(String loginId) throws Exception {
        if (loginId == null) {
            return null;
        }
        return loginInfoDb.get(loginId);
    }

    /**
     * @param request
     * @return the login info corresponding to the cookie in request; normally it returns null rather than throwing
     * @throws Exception only if the underlying DB throws; otherwise it returns null
     */
    public LoginInfo getLoginInfo(Request request) throws Exception {
        return getLoginInfo(getLoginId(request));
    }


    public User getUser(Request request) throws Exception {
        LoginInfo loginInfo = getLoginInfo(request);
        if (loginInfo == null) {
            return null;
        }
        return userDb.get(loginInfo.userId);
    }
}
