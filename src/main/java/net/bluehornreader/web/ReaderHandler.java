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

import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;
import net.bluehornreader.model.*;
import org.apache.commons.logging.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.*;
import org.eclipse.jetty.webapp.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.security.*;
import java.util.*;

import static net.bluehornreader.web.WebUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-06-15
 * Time: 09:56
 * <p/>
 */
public class ReaderHandler extends WebAppContext {

    public static final Log LOG = LogFactory.getLog(ReaderHandler.class);
//ttt1 option that on http only redirects to https, for all paths

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_SIGNUP = "signup";
    public static final String ACTION_CHANGE_PASSWORD = "change_password";
    public static final String ACTION_CHANGE_SETTINGS = "change_settings";
    public static final String ACTION_ADD_FEED = "add_feed";
    public static final String ACTION_REMOVE_FEED = "remove_feed";
    public static final String ACTION_UPDATE_FEED_LIST = "update_feed_list"; // for ordering, //ttt2

    public static final String PATH_LOGIN = "/" + ACTION_LOGIN;
    public static final String PATH_CHANGE_PASSWORD = "/" + ACTION_CHANGE_PASSWORD;
    public static final String PATH_CHANGE_SETTINGS = "/" + ACTION_CHANGE_SETTINGS;
    public static final String PATH_SIGNUP = "/" + ACTION_SIGNUP;
    public static final String PATH_ADD_FEED = "/" + ACTION_ADD_FEED;
    public static final String PATH_REMOVE_FEED = "/" + ACTION_REMOVE_FEED;
    public static final String PATH_UPDATE_FEED_LIST = "/" + ACTION_UPDATE_FEED_LIST;
    public static final String PATH_ERROR = "/error";
    public static final String PATH_LOGOUT = "/logout";
    public static final String PATH_SETTINGS = "/settings";
    public static final String PATH_FEEDS = "/feeds";
    public static final String PATH_FEED = "/feed";
    public static final String PATH_ADMIN = "/admin";
    public static final String PATH_FEED_ADMIN = "/feed_admin";
    public static final String PATH_OPEN_ARTICLE = "/open_article/"; // !!! it's easier to end this one with a slash


    // params we use to send strings to the JSPs or to get user input in POST, via request.getParameter(), or both
    public static final String PARAM_USER_ID = "userId";
    public static final String PARAM_USER_NAME = "name";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_CURRENT_PASSWORD = "currentPassword";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_PASSWORD_CONFIRM = "passwordConfirm";
    public static final String PARAM_PATH = "path";
    //public static final String PARAM_ERROR = "error";
    public static final String PARAM_REMEMBER_ACCOUNT = "rememberAccount";
    public static final String PARAM_NEW_FEED_URL = "feedUrl";
    public static final String PARAM_FEED_ID = "feedId";
    public static final String PARAM_ITEMS_PER_PAGE = "itemsPerPage";
    public static final String PARAM_STYLE = "style";
    public static final String PARAM_FEED_DATE_FORMAT = "feedDateFormat";

    // variable names, used to give JSPs access to Java objects in the handler via request.getAttribute(()
    public static final String VAR_USER = "user";
    public static final String VAR_LOGIN_INFO = "loginInfo";
    public static final String VAR_USER_DB = "userDb";
    public static final String VAR_FEED_DB = "feedDb";
    public static final String VAR_ARTICLE_DB = "articleDb";
    public static final String VAR_READ_ARTICLES_COLL_DB = "readArticlesCollDb";

    public static final String BROWSER_ID = "browserId";
    public static final String SESSION_ID = "sessionId";

    private LoginInfo.DB loginInfoDb;
    private User.DB userDb;
    private Feed.DB feedDb;
    private Article.DB articleDb;
    private ReadArticlesColl.DB readArticlesCollDb;

    private UserHelpers userHelpers;

    private boolean isInJar = Utils.isInJar();


    private static class ReaderErrorHandler extends ErrorHandler {
        @Override  //!!! note that this gets called for missing pages, but not if exceptions are thrown; exceptions are handled separately
        public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
            request.setHandled(true);
            httpServletResponse.getWriter().println(String.format("<h1>Page doesn't exist: %s</h1>",
                    request.getUri().getDecodedPath()));
        }
    }

    private static HashMap<String, String> PATH_MAPPING = new HashMap<>();
    static {
        PATH_MAPPING.put("", "home_page");
        PATH_MAPPING.put(PATH_LOGIN, "login");
        PATH_MAPPING.put(PATH_LOGOUT, "login"); // !!! after logout we get redirected to /login
        PATH_MAPPING.put(PATH_SIGNUP, "signup");
        PATH_MAPPING.put(PATH_ERROR, "error");
        PATH_MAPPING.put(PATH_FEED_ADMIN, "feed_admin");
        PATH_MAPPING.put(PATH_SETTINGS, "settings");
        PATH_MAPPING.put(PATH_FEEDS, "feeds");
        PATH_MAPPING.put(PATH_FEED + "/*", "feed");
        PATH_MAPPING.put(PATH_ADMIN, "admin");
    }

    public ReaderHandler(LowLevelDbAccess lowLevelDbAccess, String webDir) {

        loginInfoDb = new LoginInfo.DB(lowLevelDbAccess);
        userDb = new User.DB(lowLevelDbAccess);
        feedDb = new Feed.DB(lowLevelDbAccess);
        articleDb = new Article.DB(lowLevelDbAccess);
        readArticlesCollDb = new ReadArticlesColl.DB(lowLevelDbAccess);
        userHelpers = new UserHelpers(loginInfoDb, userDb);

        setContextPath("/");

        File warPath = new File(webDir);
        setWar(warPath.getAbsolutePath());

        if (isInJar) {
            for (Map.Entry<String, String> entry : PATH_MAPPING.entrySet()) {
                addPrebuiltJsp(entry.getKey(), "jsp." + entry.getValue().replaceAll("_", "_005f") + "_jsp");
            }
        } else {
            for (Map.Entry<String, String> entry : PATH_MAPPING.entrySet()) {
                addServlet(new ServletHolder(new RedirectServlet("/" + entry.getValue() + ".jsp")), entry.getKey());
            }
        }

        setErrorHandler(new ReaderErrorHandler());
    }


    private void addPrebuiltJsp(String path, String className) {
        try {
            Class clazz = Class.forName(className);    //ttt2 see if possible to not use this, preferably without doing redirections like RedirectServlet
            Object obj = clazz.newInstance();
            addServlet(new ServletHolder((Servlet)obj), path);
            LOG.info("Added prebuilt JSP: " + obj.toString());
        } catch (Exception e) {
            LOG.fatal(String.format("Failed to load prebuilt JSP for %s and %s", path, className), e);
        }
    }


    @Override
    public void doHandle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

        LOG.info("handling " + target);

        //!!! doHandle() is called twice for a request when using redirectiion, first time with request.getPathInfo()
        // set to the URI and target set to the path, then with request.getPathInfo() set to null and target set to the .jsp
        try {
            //request.setHandled(true);
            boolean secured;
            if (request.getScheme().equals("https")) {
                secured = true;
            } else if (request.getScheme().equals("http")) {
                secured = false;
            } else {
                httpServletResponse.getWriter().println(String.format("<h1>Unknown scheme %s at %s</h1>",
                        request.getScheme(), request.getUri().getDecodedPath()));
                return;
            }


            if (request.getMethod().equals("GET")) {
                if (isInJar || target.endsWith(".jsp")) {
                    // !!! when not in jar there's no need to do anything about params if it's not a .jsp,
                    // as this will get called again for the corresponding .jsp
                    if (prepareForJspGet(target, request, httpServletResponse, secured)) {
                        return;
                    }
                }
                if (target.startsWith(PATH_OPEN_ARTICLE)) {
                    handleOpenArticle(request, httpServletResponse, target);
                    return;
                }
                super.doHandle(target, request, httpServletRequest, httpServletResponse);
                LOG.info("handling of " + target + " went to super");

                //httpServletResponse.setDateHeader("Date", System.currentTimeMillis());     //ttt2 review these, probably not use
                //httpServletResponse.setDateHeader("Expires", System.currentTimeMillis() + 60000);

                return;
            }

            if (request.getMethod().equals("POST")) {
                if (request.getUri().getDecodedPath().equals(PATH_LOGIN)) {
                    handleLoginPost(request, httpServletResponse, secured);
                } else if (request.getUri().getDecodedPath().equals(PATH_SIGNUP)) {
                    handleSignupPost(request, httpServletResponse);
                } else if (request.getUri().getDecodedPath().equals(PATH_CHANGE_PASSWORD)) {
                    handleChangePasswordPost(request, httpServletResponse);
                } else if (request.getUri().getDecodedPath().equals(PATH_UPDATE_FEED_LIST)) {
                    handleUpdateFeedListPost(request, httpServletResponse);
                } else if (request.getUri().getDecodedPath().equals(PATH_ADD_FEED)) {
                    handleAddFeedPost(request, httpServletResponse);
                } else if (request.getUri().getDecodedPath().equals(PATH_REMOVE_FEED)) {
                    handleRemoveFeedPost(request, httpServletResponse);
                } else if (request.getUri().getDecodedPath().equals(PATH_CHANGE_SETTINGS)) {
                    handleChangeSettingsPost(request, httpServletResponse);
                }

            }

            /*{ // for tests only;
                httpServletResponse.getWriter().println(String.format("<h1>Unable to process request %s</h1>",
                        request.getUri().getDecodedPath()));
                request.setHandled(true);
            }*/
        } catch (Exception e) {
            LOG.error("Error processing request", e);
            try {
                //redirectToError(e.toString(), request, httpServletResponse); //!!! redirectToError leads to infinite loop, probably related to
                // the fact that we get 2 calls for a regular request when redirecting
                httpServletResponse.getWriter().println(String.format("<h1>Unable to process request %s</h1>",      //ttt1 generate some HTML
                        request.getUri().getDecodedPath()));
                request.setHandled(true);
            } catch (Exception e1) {
                LOG.error("Error redirecting", e1);
            }
        }
    }


    /**
     * Normally sets the path and a few attributes that the JSPs are likely to need. Also verifies the login information. If necessary, just redirects to the
     * login page.
     *
     * @param target
     * @param request
     * @param httpServletResponse
     * @param secured
     * @return true if the request is already handled so the .jsp shouldn't get called
     * @throws Exception
     */
    private boolean prepareForJspGet(String target, Request request, HttpServletResponse httpServletResponse, boolean secured) throws Exception {

        LoginInfo.SessionInfo sessionInfo = UserHelpers.getSessionInfo(request);

        LOG.info(String.format("hndl - %s ; %s; %s ; %s",
                target, request.getPathInfo(), request.getMethod(), secured ? "secured" : "not secured"));

        String path = request.getUri().getDecodedPath();

        boolean redirectToLogin = path.equals(PATH_LOGOUT);
        LoginInfo loginInfo = null;
        if (sessionInfo.isNull()) {
            redirectToLogin = true;
            LOG.info("Null session info. Logging in again.");
        } else {
            loginInfo = loginInfoDb.get(sessionInfo.browserId, sessionInfo.sessionId);   //ttt2 use a cache, to avoid going to DB
            if (loginInfo == null || loginInfo.expiresOn < System.currentTimeMillis()) {
                LOG.info("Session has expired. Logging in again. Info: " + loginInfo);
                redirectToLogin = true;
            }
        }

        if (!path.equals(PATH_LOGIN) && !path.equals(PATH_SIGNUP) && !path.equals(PATH_ERROR)) {

            if (redirectToLogin) {
                //ttt2 perhaps store URI, to return to it after login
                logOut(sessionInfo.browserId);
                addLoginParams(request, loginInfo);
                httpServletResponse.sendRedirect(PATH_LOGIN);
                return true;
            }

            User user = userDb.get(loginInfo.userId);
            if (user == null) {
                WebUtils.redirectToError("Unknown user", request, httpServletResponse);
                return true;
            }
            if (!user.active) {
                WebUtils.redirectToError("Account is not active", request, httpServletResponse);
                return true;
            }
            request.setAttribute(VAR_FEED_DB, feedDb);
            request.setAttribute(VAR_USER_DB, userDb);
            request.setAttribute(VAR_ARTICLE_DB, articleDb);
            request.setAttribute(VAR_READ_ARTICLES_COLL_DB, readArticlesCollDb);

            request.setAttribute(VAR_USER, user);
            request.setAttribute(VAR_LOGIN_INFO, loginInfo);

            MultiMap<String> params = new MultiMap<>();
            params.put(PARAM_PATH, path);
            request.setParameters(params);
        }

        if (path.equals(PATH_LOGIN)) {
            addLoginParams(request, loginInfo);
        }
        return false;
    }


    private void handleOpenArticle(Request request, HttpServletResponse httpServletResponse, String target) throws Exception {
        try {
            int k1 = target.indexOf('/', 1);
            int k2 = target.indexOf('/', k1 + 1);
            String feedId = target.substring(k1 + 1, k2);
            String strSeq = target.substring(k2 + 1);
            int seq = Integer.parseInt(strSeq);
            Article article = articleDb.get(feedId, seq);
            LoginInfo loginInfo = userHelpers.getLoginInfo(request);
            // ttt2 using the link from a non-authenticated browser causes a NPE; maybe do something better, e.g. sign up
            ReadArticlesColl readArticlesColl = readArticlesCollDb.get(loginInfo.userId, feedId);
            if (readArticlesColl == null) {
                readArticlesColl = new ReadArticlesColl(loginInfo.userId, feedId);
            }
            if (!readArticlesColl.isRead(seq)) {
                readArticlesColl.markRead(seq, Config.getConfig().maxSizeForReadArticles);
                readArticlesCollDb.add(readArticlesColl);
            }
            String s = URIUtil.encodePath(article.url).replace("%3F", "?").replace("%23", "#"); //ttt2 see how to do this right
            httpServletResponse.sendRedirect(s);
        } catch (Exception e) {
            WebUtils.showResult(String.format("Failed to get article for path %s. %s", target, e), "/", request, httpServletResponse);
        }
    }


    private void handleSignupPost(Request request, HttpServletResponse httpServletResponse) throws Exception {
        String userId = request.getParameter(PARAM_USER_ID);
        String userName = request.getParameter(PARAM_USER_NAME);
        String email = request.getParameter(PARAM_EMAIL);
        String stringPassword = request.getParameter(PARAM_PASSWORD);
        String stringPasswordConfirm = request.getParameter(PARAM_PASSWORD_CONFIRM);

        if (!stringPassword.equals(stringPasswordConfirm)) {
            WebUtils.redirectToError("Mismatch between password and password confirmation", request, httpServletResponse);
            return;
        }

        SecureRandom secureRandom = new SecureRandom();
        String salt = "" + secureRandom.nextLong();
        byte[] password = User.computeHashedPassword(stringPassword, salt);
        User user = userDb.get(userId);
        if (user != null) {
            WebUtils.redirectToError("There already exists a user with the ID " + userId, request, httpServletResponse);
            return;
        }

        user = new User(userId, userName, password, salt, email, new ArrayList<String>(), Config.getConfig().activateAccountsAtCreation, false);
        //ttt2 add confirmation by email, captcha, ...
        List<String> fieldErrors = user.checkFields();
        if (!fieldErrors.isEmpty()) {
            StringBuilder bld = new StringBuilder("Invalid values when trying to create user with ID ").append(userId).append("<br/>");
            for (String s : fieldErrors) {
                bld.append(s).append("<br/>");
            }
            WebUtils.redirectToError(bld.toString(), request, httpServletResponse);
            return;
        }

        //ttt2 2 clients can add the same userId simultaneously
        userDb.add(user);

        httpServletResponse.sendRedirect("/");
    }


    private void handleChangePasswordPost(Request request, HttpServletResponse httpServletResponse) throws Exception {

        LoginInfo loginInfo = userHelpers.getLoginInfo(request);
        if (loginInfo == null) {
            WebUtils.redirectToError("Couldn't determine the current user", request, httpServletResponse);
            return;
        }

        String userId = loginInfo.userId;
        String stringCrtPassword = request.getParameter(PARAM_CURRENT_PASSWORD);
        String stringNewPassword = request.getParameter(PARAM_PASSWORD);
        String stringNewPasswordConfirm = request.getParameter(PARAM_PASSWORD_CONFIRM);

        if (!stringNewPassword.equals(stringNewPasswordConfirm)) {
            showResult("Mismatch between password and password confirmation", PATH_SETTINGS, request, httpServletResponse);
            return;
        }

        User user = userDb.get(userId); // ttt1 crashes for wrong ID; 2013.07.20 - no longer have an idea what this is about
        if (user == null) {
            WebUtils.redirectToError("Couldn't find the current user", request, httpServletResponse);
            return;
        }

        if (!user.checkPassword(stringCrtPassword)) {
            showResult("Incorrect current password", PATH_SETTINGS, request, httpServletResponse);
            return;
        }

        SecureRandom secureRandom = new SecureRandom();
        String salt = "" + secureRandom.nextLong();
        byte[] password = User.computeHashedPassword(stringNewPassword, salt);
        user.salt = salt;
        user.password = password;

        //ttt3 2 clients can change the password simultaneously
        userDb.add(user);

        //httpServletResponse.sendRedirect(PATH_SETTINGS);
        showResult("Password changed", PATH_SETTINGS, request, httpServletResponse);
    }


    private void handleChangeSettingsPost(Request request, HttpServletResponse httpServletResponse) throws Exception {

        LoginInfo loginInfo = userHelpers.getLoginInfo(request);
        if (loginInfo == null) {
            WebUtils.redirectToError("Couldn't determine the current user", request, httpServletResponse);
            return;
        }

        String stringItemsPerPage = request.getParameter(PARAM_ITEMS_PER_PAGE);
        try {
            loginInfo.itemsPerPage = Integer.parseInt(stringItemsPerPage);
        } catch (Exception e) {
            showResult("Error trying to set the items per page. Expected integer value but got " + stringItemsPerPage,
                    PATH_SETTINGS, request, httpServletResponse);
            return;
        }
        loginInfo.style = request.getParameter(PARAM_STYLE);
        loginInfo.feedDateFormat = request.getParameter(PARAM_FEED_DATE_FORMAT); //ttt2 validate, better in JSP

        loginInfoDb.add(loginInfo);

        //httpServletResponse.sendRedirect(PATH_SETTINGS);
        showResult("Settings changed", "/", request, httpServletResponse);
    }


    private void handleUpdateFeedListPost(Request request, HttpServletResponse httpServletResponse) throws Exception {
        LOG.info("updating feed list"); //ttt2 implement
        httpServletResponse.sendRedirect(PATH_FEED_ADMIN);
    }


    private void handleAddFeedPost(Request request, HttpServletResponse httpServletResponse) throws Exception {
        LOG.info("adding feed");
        User user = userHelpers.getUser(request);

        try {
            if (user == null) {
                LOG.error("User not found");
                return;
            }

            String url = request.getParameter(PARAM_NEW_FEED_URL);
            //ttt1 add some validation; probably best try to actually get data, set the title, ...
            if (url == null || url.equals("")) {
                LOG.error("New feed not specified");
                //ttt1 show some error
                return;
            }

            MessageDigest digest = MessageDigest.getInstance("MD5");
            String feedId = PrintUtils.byteArrayAsUrlString(digest.digest(url.getBytes("UTF-8")));
            feedId = feedId.substring(0, Config.getConfig().feedIdSize);

            Feed feed = feedDb.get(feedId);
            if (feed == null) {
                feed = new Feed(feedId, url);
                feedDb.add(feed);
            }

            if (user.feedIds.contains(feedId)) {
                LOG.error(String.format("Trying to add existing feed %s to user %s", feedId, user));
            } else {
                user.feedIds.add(feedId);
                userDb.updateFeeds(user);
            }
        } finally {
            httpServletResponse.sendRedirect(PATH_FEED_ADMIN);
        }
    }


    private void handleRemoveFeedPost(Request request, HttpServletResponse httpServletResponse) throws Exception {
        LOG.info("removing feed");
        User user = userHelpers.getUser(request);

        try {
            if (user == null) {
                LOG.error("User not found");
                return;
            }

            String feedId = request.getParameter(PARAM_FEED_ID);

            LOG.info(String.format("Removing feed %s for user %s", feedId, user));

            //ttt1 add some validation; probably best try to actually get data, set the title, ...
            if (feedId == null || feedId.equals("")) {
                LOG.error("feed not specified");
                //ttt1 show some error
                return;
            }

            if (user.feedIds.remove(feedId)) {// ttt2 clean up the global feed table; that's probably better done if nobody accesses a feed for 3 months or so
                userDb.updateFeeds(user);
                LOG.info(String.format("Removed feed %s for user %s", feedId, user));
            } else {
                LOG.info(String.format("No feed found with ID %s for user %s", feedId, user));
            }
        } finally {
            httpServletResponse.sendRedirect(PATH_FEED_ADMIN);
        }
    }


    private void handleLoginPost(Request request, HttpServletResponse httpServletResponse, boolean secured) throws Exception {
        String userId = request.getParameter(PARAM_USER_ID);
        String password = request.getParameter(PARAM_PASSWORD);
        String rememberAccountStr = request.getParameter(PARAM_REMEMBER_ACCOUNT);
        boolean rememberAccount = Boolean.parseBoolean(rememberAccountStr);
        LoginInfo.SessionInfo sessionInfo = UserHelpers.getSessionInfo(request);

        logOut(sessionInfo.browserId);

        User user = userDb.get(userId);
        if (user == null) {
            WebUtils.redirectToError("User " + userId + " not found", request, httpServletResponse);
            return;
        }

        if (!user.checkPassword(password)) {
            WebUtils.redirectToError("Invalid password", request, httpServletResponse);
            return;
        }

        if (!user.active) {
            WebUtils.redirectToError("Account for User " + userId + " needs to be activated", request, httpServletResponse);
            return;
        }

        LOG.info("Logged in user " + userId);

        sessionInfo.sessionId = null;
        if (sessionInfo.browserId == null) {
            sessionInfo.browserId = getRandomId();
        } else {
            for (LoginInfo loginInfo : loginInfoDb.getLoginsForBrowser(sessionInfo.browserId)) {
                if (userId.equals(loginInfo.userId)) {
                    sessionInfo.sessionId = loginInfo.sessionId;
                    break;
                }
            }
        }

        long expireOn = System.currentTimeMillis() + Config.getConfig().loginExpireInterval;
        if (sessionInfo.sessionId == null) {
            sessionInfo.sessionId = getRandomId();
            Config config = Config.getConfig();
            loginInfoDb.add(new LoginInfo(sessionInfo.browserId, sessionInfo.sessionId, userId, expireOn, rememberAccount,
                    config.defaultStyle, config.defaultItemsPerPage, config.defaultFeedDateFormat));
            LOG.info(String.format("Logging in in a new session. User: %s", user));
        } else {
            loginInfoDb.updateExpireTime(sessionInfo.browserId, sessionInfo.sessionId, expireOn);
            LOG.info(String.format("Logging in in an existing session. User: %s", user));
        }

        WebUtils.saveCookies(httpServletResponse, secured, sessionInfo.browserId, sessionInfo.sessionId);

        httpServletResponse.sendRedirect("/");
    }


    private String getRandomId() {
        SecureRandom secureRandom = new SecureRandom();
        return "" + secureRandom.nextLong();
    }

    private void addLoginParams(Request request, LoginInfo loginInfo) {
        MultiMap<String> params = new MultiMap<>();
        if (loginInfo != null && loginInfo.rememberAccount) {
            params.put(PARAM_USER_ID, loginInfo.userId);
        }
        request.setParameters(params);
    }

    private void logOut(String browserId) throws Exception {
        //ttt2 the right way to do it is to go through all the sessions of the current browser, which would require a new field and a new index;
        // not sure if it's worth it, but this would work: A logs in, forgets to log out, B delets the cookies, logs in, A sees B is logged in, then B
        // restores the cookies and uses A's account
        if (browserId == null) {
            return;
        }

        List<LoginInfo> loginInfos = loginInfoDb.getLoginsForBrowser(browserId);
        long expireTarget = System.currentTimeMillis() - Utils.ONE_DAY;
        for (LoginInfo loginInfo : loginInfos) {
            if (loginInfo.expiresOn <= expireTarget) {
                LOG.info(String.format("LoginInfo %s is enough in the past", loginInfo));
            } else {
                LOG.info(String.format("Logging out: %s", loginInfo));
                loginInfoDb.updateExpireTime(browserId, loginInfo.sessionId, expireTarget);
            }
        }
    }


    public static class FeedInfo {
        public String feedId;
        public int maxSeq;

        public FeedInfo(String feedId, int maxSeq) {
            this.feedId = feedId;
            this.maxSeq = maxSeq;
        }
    }

    //!!! IDEA reports this as unused, but it is called from JSP
    public static FeedInfo getFeedInfo(String feedPath) {
        if (feedPath.startsWith(PATH_FEED + "/")) {
            try {
                if (feedPath.endsWith("/")) {
                    feedPath = feedPath.substring(0, feedPath.length() - 1);
                }
                int k = PATH_FEED.length() + 1;
                int p = feedPath.indexOf('/', k);
                return p >= 0 ? new FeedInfo(feedPath.substring(k, p), Integer.parseInt(feedPath.substring(p + 1))) :
                        new FeedInfo(feedPath.substring(k), -1);
            } catch (Exception e) {
                LOG.error("Exception trying to parse the feed info", e);
            }
        }

        LOG.error("Invalid path from feed: " + feedPath);
        return new FeedInfo("INVALID", -1);
    }


    //!!! IDEA reports this as unused, but it is called from JSP
    public static String getStyle(LoginInfo loginInfo) {
        StringBuilder bld = new StringBuilder();
        bld.append("<style media=\"screen\" type=\"text/css\">\n\n");
        if (loginInfo == null) {
            bld.append(Config.getConfig().defaultStyle);
        } else {
            bld.append(loginInfo.style); // ttt3 detect broken styles and return default
        }
        bld.append("</style>\n");
        return bld.toString();
    }

/*    private void jspCodeCheck() throws Exception {
        Article.DB articleDb;
        Request request;
        String path = "";

        String feedId = ReaderHandler.getFeedId(path);
        int maxSeq = ReaderHandler.getSeq(path);

        Feed.DB feedDb = (Feed.DB)request.getAttribute(ReaderHandler.VAR_FEED_DB);

        Feed feed = feedDb.get(feedId);
        if (feed == null) {
            out.println("Feed " + feedId + " not found");
        } else {
            if (maxSeq == -1) {
                maxSeq = feed.maxSeq;
            }
            if (maxSeq < 0) {
                out.println("Feed " + feedId + " is empty");
            } else {
                ++maxSeq;
                LoginInfo loginInfo = (LoginInfo)request.getAttribute(ReaderHandler.VAR_LOGIN_INFO);
                int minSeq = Math.max(maxSeq - loginInfo.itemsPerPage, 0);
                List<Article> articles = articleDb.get(feedId, minSeq, maxSeq);
                for (Article article : articles) {
                    out.println("<a href=\"" + article.url + "\">" + article.title + "</a><br/>");
                }
            }
        }

    }
    //*/
}

//ttt2 see how to submit secured login even when using HTTP

/*

todo

ttt2 log out everywhere

ttt0 more admin things (e.g. disable user, block feeds globally)

ttt1 see about page compression: http://serverfault.com/questions/279057/how-can-i-enable-gzip-compression-in-jetty

ttt1 see about compiling the JSPs

ttt0 see if possible to disable back button after logout

ttt0 encoding seems to be ISO-8859-1 when viewing page info in FF`

ttt1 save summary and render it with truncated text

ttt1 see if params are needed in html tag in JSP

 */

