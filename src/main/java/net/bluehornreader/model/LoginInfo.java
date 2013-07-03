package net.bluehornreader.model;

import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.model.*;
import net.bluehornreader.data.*;

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

    // key
    public String loginId;

    // columns
    public String userId;
    public long expiresOn;
    public boolean rememberAccount;
    public String style;
    public int itemsPerPage;


    private static class Columns {
        private static final String LOGIN_ID = "login_id";
        private static final String USER_ID = "user_id";
        private static final String EXPIRES_ON = "expires_on";
        private static final String REMEMBER_ACCOUNT = "remember_account";
        private static final String STYLE = "style_on";
        private static final String ITEMS_PER_PAGE = "items_per_page";
    }

    public static CqlTable CQL_TABLE;
    static {
        List<CqlTable.ColumnInfo> columnInfos = new ArrayList<CqlTable.ColumnInfo>();
        columnInfos.add(new CqlTable.ColumnInfo(Columns.LOGIN_ID, TEXT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.USER_ID, TEXT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.EXPIRES_ON, BIGINT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.REMEMBER_ACCOUNT, BOOLEAN));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.STYLE, TEXT));
        columnInfos.add(new CqlTable.ColumnInfo(Columns.ITEMS_PER_PAGE, INT));
        CQL_TABLE = new CqlTable("login_infos", columnInfos);
    }

    public LoginInfo(String loginId, String userId, long expiresOn, boolean rememberAccount, String style, int itemsPerPage) {
        this.loginId = loginId;
        this.userId = userId;
        this.expiresOn = expiresOn;
        this.rememberAccount = rememberAccount;
        this.style = style;
        this.itemsPerPage = itemsPerPage;
    }

    @Override
    public String toString() {
        return "LoginInfo{" +
                "loginId='" + loginId + '\'' +
                ", userId='" + userId + '\'' +
                ", expiresOn=" + expiresOn +
                ", rememberAccount=" + rememberAccount +
                ", style=" + style +
                ", itemsPerPage=" + itemsPerPage +
                '}';
    }

    public static class DB {

        private LowLevelDbAccess lowLevelDbAccess;

        private static final String UPDATE_STATEMENT = CQL_TABLE.getUpdateStatement();
        private static final String SELECT_STATEMENT = CQL_TABLE.getSelectStatement();
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
                    .withStringValue(loginInfo.loginId)
                    .withStringValue(loginInfo.userId)
                    .withLongValue(loginInfo.expiresOn)
                    .withBooleanValue(loginInfo.rememberAccount)
                    .withStringValue(loginInfo.style)
                    .withIntegerValue(loginInfo.itemsPerPage)
                    .execute();
            CqlTable.checkResult(result);
        }

        public LoginInfo get(String loginId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(loginId)
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                return new LoginInfo(
                        loginId,
                        columns.getStringValue(Columns.USER_ID, ""),
                        columns.getLongValue(Columns.EXPIRES_ON, 0L),
                        columns.getBooleanValue(Columns.REMEMBER_ACCOUNT, false),
                        columns.getStringValue(Columns.STYLE, ""),
                        columns.getIntegerValue(Columns.ITEMS_PER_PAGE, 0));
            }

            if (rows.size() > 1) {
                throw new RuntimeException(String.format("Duplicate entries for key <%s>", loginId));
            }

            return null;
        }


        public void delete(Collection<String> loginIds) throws Exception { //ttt2 clean up when expire
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
        }
    }

}
