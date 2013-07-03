package net.bluehornreader.web;

import net.bluehornreader.*;
import net.bluehornreader.data.*;
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

import static net.bluehornreader.web.WebUtils.showResult;

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
    public static final String ACTION_UPDATE_FEED_LIST = "update_feed_list"; // for ordering, //ttt2

    public static final String PATH_LOGIN = "/" + ACTION_LOGIN;
    public static final String PATH_CHANGE_PASSWORD = "/" + ACTION_CHANGE_PASSWORD;
    public static final String PATH_CHANGE_SETTINGS = "/" + ACTION_CHANGE_SETTINGS;
    public static final String PATH_SIGNUP = "/" + ACTION_SIGNUP;
    public static final String PATH_ADD_FEED = "/" + ACTION_ADD_FEED;
    public static final String PATH_UPDATE_FEED_LIST = "/" + ACTION_UPDATE_FEED_LIST;
    public static final String PATH_ERROR = "/error";
    public static final String PATH_LOGOUT = "/logout";
    public static final String PATH_SETTINGS = "/settings";
    public static final String PATH_FEEDS = "/feeds";
    public static final String PATH_FEED = "/feed";
    public static final String PATH_ADMIN = "/admin";
    public static final String PATH_FEED_ADMIN = "/feed_admin";

    // params we use to send strings to the JSPs or to get user input in POST, via request.getParameter(), or both
    public static final String PARAM_USER_ID = "userId";
    public static final String PARAM_USER_NAME = "name";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_CURRENT_PASSWORD = "currentPassword";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_PASSWORD_CONFIRM = "passwordConfirm";
    public static final String PARAM_PATH = "path";
    //public static final String PARAM_ERROR = "error";
    public static final String PARAM_OLD_LOGIN_ID = "oldLoginId";
    public static final String PARAM_REMEMBER_ACCOUNT = "rememberAccount";
    public static final String PARAM_NEW_FEED = "newFeed";

    // variable names, used to give JSPs access to Java objects in the handler via request.getAttribute(()
    public static final String VAR_USER = "user";
    public static final String VAR_LOGIN_INFO = "loginInfo";
    public static final String VAR_FEED_DB = "feedDb";
    public static final String VAR_USER_DB = "userDb";
    public static final String VAR_READ_ARTICLES_COLL_DB = "readArticlesCollDb";
    public static final String VAR_ARTICLE_DB = "articleDb";

    public static final String LOGIN_ID = "readerId";

    private LoginInfo.DB loginInfoDb;
    private User.DB userDb;
    private Feed.DB feedDb;
    private Article.DB articleDb;
    private ReadArticlesColl.DB readArticlesCollDb;

    private UserHelpers userHelpers;

    private static final String DEFAULT_STYLE = "";


    private static class ReaderErrorHandler extends ErrorHandler {
        @Override  //!!! note that this gets called for missing pages, but not if exceptions are thrown; exceptions are handled separately
        public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
            request.setHandled(true);
            httpServletResponse.getWriter().println(String.format("<h1>Page doesn't exist: %s</h1>",
                    request.getUri().getDecodedPath()));
        }
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

        //addServlet(new ServletHolder(new RedirectServlet("/home_page.jsp")), "/*"); //ttt2 see why this recurse infinitely
        //addServlet(new ServletHolder(new RedirectServlet("/aabb.jsp")), "/aabb/*");

        addServlet(new ServletHolder(new RedirectServlet("/home_page.jsp")), "");

        addServlet(new ServletHolder(new RedirectServlet("/login.jsp")), PATH_LOGIN);
        addServlet(new ServletHolder(new RedirectServlet("/login.jsp")), PATH_LOGOUT); // !!! after logout we get redirected to /login
        addServlet(new ServletHolder(new RedirectServlet("/signup.jsp")), PATH_SIGNUP);
        addServlet(new ServletHolder(new RedirectServlet("/error.jsp")), PATH_ERROR);
        addServlet(new ServletHolder(new RedirectServlet("/feed_admin.jsp")), PATH_FEED_ADMIN);

        addServlet(new ServletHolder(new RedirectServlet("/settings.jsp")), PATH_SETTINGS);
        addServlet(new ServletHolder(new RedirectServlet("/feeds.jsp")), PATH_FEEDS);
        addServlet(new ServletHolder(new RedirectServlet("/feed.jsp")), PATH_FEED + "/*");

        addServlet(new ServletHolder(new RedirectServlet("/admin.jsp")), PATH_ADMIN);


        setErrorHandler(new ReaderErrorHandler());
    }


    @Override
    public void doHandle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

        LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
try {
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
                if (target.endsWith(".jsp")) {
                    // !!! no need to do anything about params if it's not a .jsp, as this will get called again for the corresponding .jsp
                    if (prepareForJspGet(target, request, httpServletResponse, secured)) {
                        return;
                    }
                }
                super.doHandle(target, request, httpServletRequest, httpServletResponse);
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
                httpServletResponse.getWriter().println(String.format("<h1>Unable to process request %s</h1>",      //ttt0 generate some HTML
                        request.getUri().getDecodedPath()));
                request.setHandled(true);
            } catch (Exception e1) {
                LOG.error("Error redirecting", e1);
            }
        }

} finally {
    LOG.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
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

        String loginId = UserHelpers.getLoginId(request);

        LOG.info(String.format("hndl - %s ; %s; %s ; %s",
                target, request.getPathInfo(), request.getMethod(), secured ? "secured" : "not secured"));

        String path = request.getUri().getDecodedPath();

        boolean redirectToLogin = path.equals(PATH_LOGOUT);
        LoginInfo loginInfo = null;
        if (loginId == null) {
            redirectToLogin = true;
        } else {
            loginInfo = loginInfoDb.get(loginId);   //ttt2 use a cache, to avoid going to DB
            if (loginInfo == null || loginInfo.expiresOn < System.currentTimeMillis()) {
                redirectToLogin = true;
            }
        }

        if (!path.equals(PATH_LOGIN) && !path.equals(PATH_SIGNUP) && !path.equals(PATH_ERROR)) {

            if (redirectToLogin) {
                //ttt2 perhaps store URI, to return to it after login
                if (loginId != null) {
                    if (loginInfo == null) {
                        logOut(loginId, null, false);
                    } else {
                        logOut(loginId, loginInfo.userId, loginInfo.rememberAccount);
                    }
                }
                addLoginParams(request, loginId, loginInfo);
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
            addLoginParams(request, loginId, loginInfo);
        }
        return false;
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
        userDb.add(Arrays.asList(user));

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

        User user = userDb.get(userId); // ttt0 crashes for wrong ID
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
        userDb.add(Arrays.asList(user));

        //httpServletResponse.sendRedirect(PATH_SETTINGS);
        showResult("Password changed", PATH_SETTINGS, request, httpServletResponse);
    }


    private void handleUpdateFeedListPost(Request request, HttpServletResponse httpServletResponse) throws Exception {
        LOG.info("updating feed list"); //ttt2 implement
        httpServletResponse.sendRedirect(PATH_FEED_ADMIN);
    }


    private void handleAddFeedPost(Request request, HttpServletResponse httpServletResponse) throws Exception {
        LOG.info("adding feed");
        User user = userHelpers.getUser(request);

        String url = request.getParameter(PARAM_NEW_FEED);

        MessageDigest digest = MessageDigest.getInstance("MD5");
        String feedId = PrintUtils.byteArrayAsUrlString(digest.digest(url.getBytes("UTF-8"))).substring(10);

        Feed feed = feedDb.get(feedId);
        if (feed == null) {
            feed = new Feed(feedId, url);
            feedDb.add(Arrays.asList(feed));
        }

        boolean alreadyExists = false;
        for (String s : user.feedIds) {
            if (s.equals(feedId)) {
                alreadyExists = true;
                break;
            }
        }

        if (!alreadyExists) {
            user.feedIds.add(feedId);
            userDb.add(Arrays.asList(user)); // ttt0 look for all Arrays.asList() and add "add()" that takes single param
        }


        httpServletResponse.sendRedirect(PATH_FEED_ADMIN);
    }


    private void handleLoginPost(Request request, HttpServletResponse httpServletResponse, boolean secured) throws Exception {
        String userId = request.getParameter(PARAM_USER_ID);
        String password = request.getParameter(PARAM_PASSWORD);
        String oldLoginId = request.getParameter(PARAM_OLD_LOGIN_ID);
        String rememberAccountStr = request.getParameter(PARAM_REMEMBER_ACCOUNT);
        boolean rememberAccount = Boolean.parseBoolean(rememberAccountStr);

        User user = userDb.get(userId);
        if (user == null) {
            logOut(oldLoginId, null, false);
            WebUtils.redirectToError("User " + userId + " not found", request, httpServletResponse);
            return;
        }

        if (!user.checkPassword(password)) {
            logOut(oldLoginId, userId, rememberAccount);
            WebUtils.redirectToError("Invalid password", request, httpServletResponse);
            return;
        }

        if (!user.active) {
            WebUtils.redirectToError("Account for User " + userId + " needs to be activated", request, httpServletResponse);
            return;
        }

        LOG.info("Logged in user " + userId);

        //ttt0 every time you log in the previous settings are lost; see how to fix this (perhaps use multiple cookies, one for user)
        String newLoginId = WebUtils.addCookie(httpServletResponse, secured);
        //LoginInfo loginInfo = loginInfoDb.get();

        if (oldLoginId != null) {
            loginInfoDb.delete(Arrays.asList(oldLoginId));
        }

        loginInfoDb.add(new LoginInfo(newLoginId, userId, System.currentTimeMillis() + Config.getConfig().loginExpireInterval, rememberAccount,
                DEFAULT_STYLE, 10));

        httpServletResponse.sendRedirect("/");
    }

    private void addLoginParams(Request request, String loginId, LoginInfo loginInfo) {
        MultiMap<String> params = new MultiMap<>();
        if (loginInfo != null && loginInfo.rememberAccount) {
            params.put(PARAM_USER_ID, loginInfo.userId);
        }
        if (loginId != null) {
            params.put(PARAM_OLD_LOGIN_ID, loginId);
        }
        request.setParameters(params);
    }

    private void logOut(String loginId, String userId, boolean rememberAccount) throws Exception {
        if (loginId == null) {
            return;
        }
        if (userId == null) {
            LoginInfo loginInfo = loginInfoDb.get(loginId);
            if (loginInfo == null) {
                return;
            }
            userId = loginInfo.userId;
        }
        //ttt0 don't delete settings at logout
        loginInfoDb.add(new LoginInfo(loginId, userId, System.currentTimeMillis() - Utils.ONE_MINUTE, rememberAccount, "", 10));
    }


    public static class FeedInfo {
        public String feedId;
        public int maxSeq;

        public FeedInfo(String feedId, int maxSeq) {
            this.feedId = feedId;
            this.maxSeq = maxSeq;
        }
    }

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

todo ttt0

log out everywhere
admin ...

 */

