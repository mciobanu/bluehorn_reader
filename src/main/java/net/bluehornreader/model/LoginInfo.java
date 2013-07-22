package net.bluehornreader.model;

import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.model.*;
import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;
import org.apache.commons.logging.*;

import java.util.*;

import static net.bluehornreader.data.CqlTable.ColumnType.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-06-15
 * Time: 20:06
 * <p/>
 */
public class LoginInfo {

    public static final Log LOG = LogFactory.getLog(LoginInfo.class);

    // key
    public String browserId;
    public String sessionId;

    // columns
    public String userId;
    public long expiresOn;
    public boolean rememberAccount;
    public String style;
    public int itemsPerPage;


    public static class SessionInfo {
        public String browserId;
        public String sessionId;
        public boolean isNull() {
            return browserId == null || sessionId == null;
        }
    }


    private static class Columns {
        private static final String BROWSER_ID_AND_SESSION_ID = "browser_id_and_session_id";
        private static final String BROWSER_ID = "browser_id"; // a composite key hurts performance, we want to search by browserId, so we duplicate
        private static final String USER_ID = "user_id";
        private static final String EXPIRES_ON = "expires_on";
        private static final String REMEMBER_ACCOUNT = "remember_account";
        private static final String STYLE = "style";
        private static final String ITEMS_PER_PAGE = "items_per_page";
    }

    public static CqlTable CQL_TABLE;
    static {
        List<CqlTable.ColumnInfo> columnInfos = new ArrayList<>();
        columnInfos.add(new CqlTable.ColumnInfo(Columns.BROWSER_ID_AND_SESSION_ID, TEXT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.BROWSER_ID, TEXT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.USER_ID, TEXT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.EXPIRES_ON, BIGINT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.REMEMBER_ACCOUNT, BOOLEAN));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.STYLE, TEXT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.ITEMS_PER_PAGE, INT));
        CqlTable.IndexInfo indexInfo = new CqlTable.IndexInfo(Columns.BROWSER_ID);
        CQL_TABLE = new CqlTable("login_infos", columnInfos, indexInfo);
    }

    public LoginInfo(String browserId, String sessionId, String userId, long expiresOn, boolean rememberAccount, String style, int itemsPerPage) {
        this.browserId = browserId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.expiresOn = expiresOn;
        this.rememberAccount = rememberAccount;
        this.style = style;
        this.itemsPerPage = itemsPerPage;
    }


    @Override
    public String toString() {
        return "LoginInfo{" +
                "browserId='" + browserId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", expiresOn=" + expiresOn +
                ", rememberAccount=" + rememberAccount +
                ", style=" + style.replaceAll("\n", " ") +
                ", itemsPerPage=" + itemsPerPage +
                '}';
    }

    public static class DB {

        private LowLevelDbAccess lowLevelDbAccess;

        private static final String UPDATE_STATEMENT = CQL_TABLE.getUpdateStatement();
        private static final String UPDATE_EXPIRE_ON_STATEMENT = CQL_TABLE.getUpdateStatement(Columns.BROWSER_ID_AND_SESSION_ID, Columns.EXPIRES_ON);
        private static final String SELECT_STATEMENT = CQL_TABLE.getSelectStatement();
        private static final String SELECT_FOR_BROWSER_STATEMENT = CQL_TABLE.getSelectByColumnsStatement(Columns.BROWSER_ID);
        private static final String DELETE_STATEMENT = CQL_TABLE.getDeleteStatement();

        public DB(LowLevelDbAccess lowLevelDbAccess) {
            this.lowLevelDbAccess = lowLevelDbAccess;
        }

        public void add(LoginInfo loginInfo) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(loginInfo.getKey())
                    .withStringValue(loginInfo.browserId)
                    .withStringValue(loginInfo.userId)
                    .withLongValue(loginInfo.expiresOn)
                    .withBooleanValue(loginInfo.rememberAccount)
                    .withStringValue(loginInfo.style)
                    .withIntegerValue(loginInfo.itemsPerPage)
                    .execute();
            CqlTable.checkResult(result);
        }

        public void updateExpireTime(String browserId, String sessionId, long expiresOn) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_EXPIRE_ON_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(getKey(browserId, sessionId))
                    .withLongValue(expiresOn)
                    .execute();
            CqlTable.checkResult(result);
        }

        public LoginInfo get(String browserId, String sessionId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(getKey(browserId, sessionId))
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                return new LoginInfo(
                        browserId,
                        sessionId,
                        columns.getStringValue(Columns.USER_ID, ""),
                        columns.getLongValue(Columns.EXPIRES_ON, 0L),
                        columns.getBooleanValue(Columns.REMEMBER_ACCOUNT, false),
                        columns.getStringValue(Columns.STYLE, ""),
                        columns.getIntegerValue(Columns.ITEMS_PER_PAGE, 0));
            }

            if (rows.size() > 1) {
                throw new RuntimeException(String.format("Duplicate entries for key <%s, %s>", browserId, sessionId));
            }

            return null;
        }


        public List<LoginInfo> getLoginsForBrowser(String browserId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_FOR_BROWSER_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(browserId)
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            ArrayList<LoginInfo> res = new ArrayList<>();
            for (int i = 0; i < rows.size(); ++i) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                String browserIdAndSessionId = columns.getStringValue(Columns.BROWSER_ID_AND_SESSION_ID, "");
                res.add(new LoginInfo(
                        getBrowserId(browserIdAndSessionId),
                        getSessionId(browserIdAndSessionId),
                        columns.getStringValue(Columns.USER_ID, ""),
                        columns.getLongValue(Columns.EXPIRES_ON, 0L),
                        columns.getBooleanValue(Columns.REMEMBER_ACCOUNT, false),
                        columns.getStringValue(Columns.STYLE, ""),
                        columns.getIntegerValue(Columns.ITEMS_PER_PAGE, 0)));
            }

            return res;
        }

        //ttt2 clean up when expire
        /*public void delete(Collection<String> loginIds) throws Exception { //ttt2 probably make getKey() public to allow composite key to work
            for (String loginId : loginIds) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(DELETE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(loginId)
                        .execute();
                CqlTable.checkResult(result);
            }
        }*/
    }

    private static String getKey(String browserId, String sessionId) {
        return browserId + Utils.LIST_SEPARATOR + sessionId;
    }

    private String getKey() {
        return getKey(browserId, sessionId);
    }

    private static String getBrowserId(String browserIdAndSessionId) {
        String[] s = Utils.stringAsArray(browserIdAndSessionId);
        if (s.length != 2) {
            LOG.error("Invalid key in LoginInfo: " + browserIdAndSessionId);
            return null;
        }
        return s[0];
    }

    private static String getSessionId(String browserIdAndSessionId) {
        String[] s = Utils.stringAsArray(browserIdAndSessionId);
        if (s.length != 2) {
            LOG.error("Invalid key in LoginInfo: " + browserIdAndSessionId);
            return null;
        }
        return s[1];
    }
}
