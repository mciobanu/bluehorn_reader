package net.bluehornreader.model;

import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.model.*;
import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;
import org.apache.commons.logging.*;

import java.security.*;
import java.util.*;

import static net.bluehornreader.data.CqlTable.*;
import static net.bluehornreader.data.CqlTable.ColumnType.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-20
 * Time: 16:01
 * <p/>
 */
public class User {

    private static final Log LOG = LogFactory.getLog(User.class);

    // key
    public String userId;

    // columns
    public String name;
    public byte[] password; // salted & hashed
    public String salt;
    public String email;
    public List<String> feedIds;
    public boolean active;
    public boolean admin;

    private static class Columns {
        private static final String USER_ID = "user_id";
        private static final String NAME = "name";
        private static final String PASSWORD = "password";
        private static final String SALT = "salt";
        private static final String EMAIL = "email";
        private static final String FEED_IDS = "feed_ids";
        private static final String ACTIVE = "active";
        private static final String ADMIN = "admin";
    }

    public static CqlTable CQL_TABLE;
    static {
        List<ColumnInfo> columnInfos = new ArrayList<>();
        columnInfos.add(new ColumnInfo(Columns.USER_ID, TEXT));
        columnInfos.add(new ColumnInfo(Columns.NAME, TEXT));
        columnInfos.add(new ColumnInfo(Columns.PASSWORD, BLOB));
        columnInfos.add(new ColumnInfo(Columns.SALT, TEXT));
        columnInfos.add(new ColumnInfo(Columns.EMAIL, TEXT));
        columnInfos.add(new ColumnInfo(Columns.FEED_IDS, TEXT));
        columnInfos.add(new ColumnInfo(Columns.ACTIVE, BOOLEAN));
        columnInfos.add(new ColumnInfo(Columns.ADMIN, BOOLEAN));
        CQL_TABLE = new CqlTable("users", columnInfos);
    }

    public User(String userId, String name, byte[] password, String salt, String email, List<String> feedIds, boolean active, boolean admin) {
        this.userId = userId;
        this.name = name;
        this.password = password;
        this.salt = salt;
        this.email = email;
        this.feedIds = feedIds;
        this.active = active;
        this.admin = admin;
    }

    public static byte[] computeHashedPassword(String password, String salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");    // ttt2 maybe use bcrypt, but initially 64-bit salt should be enough
        String text = salt + password;
        return digest.digest(text.getBytes("UTF-8"));
    }

    public boolean checkPassword(String password) {
        try {
            byte[] hash = computeHashedPassword(password, salt);
            return Arrays.equals(hash, this.password);
        } catch (Exception e) {
            LOG.error("Error checking password", e);
        }
        return false;
    }

    /**
     * Checks that the fields have valid values
     *
     * @return a list with the errors found
     */
    public List<String> checkFields() {
        ArrayList<String> res = new ArrayList<>();
        if (userId.length() < 5) {
            res.add("Field ID is too short");
        }
        //ttt2 add checks for other fields
        return res;
    }


    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", password=" + PrintUtils.byteArrayAsString(password) +
                ", salt='" + salt + '\'' +
                ", email='" + email + '\'' +
                ", feedIds='" + feedIds + '\'' +
                ", active=" + active +
                ", admin=" + admin +
                '}';
    }

    public static class DB {

        private LowLevelDbAccess lowLevelDbAccess;

        private static final String UPDATE_STATEMENT = CQL_TABLE.getUpdateStatement();
        private static final String UPDATE_FEEDS_STATEMENT = CQL_TABLE.getUpdateStatement(Columns.USER_ID, Columns.FEED_IDS);
        private static final String SELECT_STATEMENT = CQL_TABLE.getSelectStatement();
        private static final String DELETE_STATEMENT = CQL_TABLE.getDeleteStatement();

        public DB(LowLevelDbAccess lowLevelDbAccess) {
            this.lowLevelDbAccess = lowLevelDbAccess;
        }

        public void add(User user) throws Exception {
            add(Arrays.asList(user));
        }

        public void add(Collection<User> users) throws Exception {
            for (User user : users) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(UPDATE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(user.userId)
                        .withStringValue(user.name)
                        .withByteBufferValue(user.password, LowLevelDbAccess.BYTE_BUFFER_SERIALIZER)
                        .withStringValue(user.salt)
                        .withStringValue(user.email)
                        .withStringValue(Utils.listAsString(user.feedIds))
                        .withBooleanValue(user.active)
                        .withBooleanValue(user.admin)
                        .execute();
                CqlTable.checkResult(result);
            }
        }

        //ttt1 search for all "add()" that are actually used to update some of the fields; or maybe better, allow columns to be specified for update() and
        //   get(); the thing is this requires exposing guts (the actual DB column names) and maybe some reflection
        public void updateFeeds(User user) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_FEEDS_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(user.userId)
                    .withStringValue(Utils.listAsString(user.feedIds))
                    .execute();
            CqlTable.checkResult(result);
        }

        //ttt1 more get() functions to avoid retrieving and parsing useless data; so some fields might be left empty
        public User get(String userId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(userId)
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                return new User(
                        userId,
                        columns.getStringValue(Columns.NAME, ""),
                        columns.getByteArrayValue(Columns.PASSWORD, null),
                        columns.getStringValue(Columns.SALT, ""),
                        columns.getStringValue(Columns.EMAIL, ""),
                        Utils.stringAsList(columns.getStringValue(Columns.FEED_IDS, "")),
                        columns.getBooleanValue(Columns.ACTIVE, false),
                        columns.getBooleanValue(Columns.ADMIN, false));
            }

            if (rows.size() > 1) {
                throw new RuntimeException(String.format("Duplicate entries for key <%s>", userId));
            }

            return null;
        }


        public void delete(Collection<User> users) throws Exception {
            for (User user : users) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(DELETE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(user.userId)
                        .execute();
                CqlTable.checkResult(result);
            }
        }
    }
}
