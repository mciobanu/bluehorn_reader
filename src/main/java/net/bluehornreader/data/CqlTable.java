package net.bluehornreader.data;

import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.model.*;
import org.apache.commons.logging.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-27
 * Time: 14:18
 * <p/>
 *
 * Contains info about the columns of a table. Generates CQL statements for UPDATE/SELECT/DELETE
 */
public class CqlTable {

    private static final Log LOG = LogFactory.getLog(CqlTable.class);

    public String tableName;
    public List<ColumnInfo> columnInfos;
    private int primaryKeySize = 1;
    // a composed primary key is nice, but there are big performance issues deleting such rows, meaning it's
    //   100 times slower than deleting rows with a simple primary key; so, feature dropped

    public CqlTable(String tableName, List<ColumnInfo> columnInfos/*, int primaryKeySize*/) {
        this.tableName = tableName;
        this.columnInfos = columnInfos;
        //this.primaryKeySize = primaryKeySize;
    }

    public enum ColumnType { INT, TEXT, BIGINT, BLOB, BOOLEAN }

    public static class ColumnInfo {
        public String columnName;
        public ColumnType columnType;

        public ColumnInfo(String columnName, ColumnType columnType) {
            this.columnName = columnName;
            this.columnType = columnType;
        }
    }

    public String getCreateStatement() {
        StringBuilder bld = new StringBuilder("CREATE TABLE ");
        bld.append(tableName).append(" (");
        boolean first = true;
        for (ColumnInfo columnInfo : columnInfos) {
            if (first) {
                first = false;
            } else {
                bld.append(", ");
            }
            bld.append(columnInfo.columnName).append((" " + columnInfo.columnType).toLowerCase());
        }
        bld.append(", PRIMARY KEY (");
        first = true;
        for (int i = 0; i < primaryKeySize; ++i) {
            ColumnInfo columnInfo = columnInfos.get(i);
            if (first) {
                first = false;
            } else {
                bld.append(", ");
            }
            bld.append(columnInfo.columnName);
        }
        bld.append("));");
        String res = bld.toString();
        LOG.info("Generated CREATE TABLE statement: " + res);
        return bld.toString();
    }

    private String[] getAllColumnNames() {
        String[] columnNames = new String[columnInfos.size()];
        for (int i = 0; i < columnInfos.size(); ++i) {
            columnNames[i] = columnInfos.get(i).columnName;
        }
        return columnNames;
    }

    public String getUpdateStatement() {
        return getUpdateStatement(getAllColumnNames());
    }

    public String getUpdateStatement(String... columnNames) {
        StringBuilder bld = new StringBuilder("INSERT INTO ");
        bld.append(tableName).append(" (");
        boolean first = true;
        for (String columnName : columnNames) {
            if (first) {
                first = false;
            } else {
                bld.append(", ");
            }
            bld.append(columnName);
        }
        bld.append(") VALUES (");
        first = true;
        for (int i = 0; i < columnNames.length; ++i) {
            if (first) {
                first = false;
            } else {
                bld.append(", ");
            }
            bld.append("?");
        }
        bld.append(")");
        bld.append(";");
        String res = bld.toString();
        LOG.info("Generated INSERT statement: " + res);
        return bld.toString();
    }


    public String getSelectStatement() {
        StringBuilder bld = new StringBuilder("SELECT * FROM ");
        bld.append(tableName).append(" WHERE");
        boolean first = true;
        for (int i = 0; i < primaryKeySize; ++i) {
            ColumnInfo columnInfo = columnInfos.get(i);
            if (first) {
                first = false;
            } else {
                bld.append(" AND");
            }
            bld.append(" ").append(columnInfo.columnName).append("=?");
        }
        bld.append(";");
        String res = bld.toString();
        LOG.info("Generated SELECT statement: " + res);
        return bld.toString();
    }


    public String getSelectMultipleStatement() {
        if (primaryKeySize != 1) {
            throw new RuntimeException("Multiple select only supported on tables with a single field in the primary key");
        }
        StringBuilder bld = new StringBuilder("SELECT * FROM ");
        bld.append(tableName).append(" WHERE");
        ColumnInfo columnInfo = columnInfos.get(0);
        bld.append(" ").append(columnInfo.columnName).append(" in (?)");
        bld.append(";");
        String res = bld.toString();
        LOG.info("Generated multiple SELECT statement: " + res);
        return bld.toString();
    }


    public static String createMultipleSelectParam(Collection<String> values) {
        StringBuilder bld = new StringBuilder();
        boolean first = true;
        for (String s : values) {
            if (first) {
                first = false;
            } else {
                bld.append(", ");
            }
            bld.append(" '").append(s).append("'");
        }
        return bld.toString();
    }


    public String getSelectAllStatement() {
        StringBuilder bld = new StringBuilder("SELECT * FROM ");
        bld.append(tableName).append(";");
        String res = bld.toString();
        LOG.info("Generated SELECT statement: " + res);
        return bld.toString();
    }


    public String getSelectStatement(String... columnNames) {
        StringBuilder bld = new StringBuilder("SELECT ");
        boolean first = true;
        for (String columnName : columnNames) {
            if (first) {
                first = false;
            } else {
                bld.append(", ");
            }
            bld.append(columnName);
        }
        bld.append(" FROM ");
        bld.append(tableName).append(" WHERE");
        first = true;
        for (int i = 0; i < primaryKeySize; ++i) {
            ColumnInfo columnInfo = columnInfos.get(i);
            if (first) {
                first = false;
            } else {
                bld.append(" AND");
            }
            bld.append(" ").append(columnInfo.columnName).append("=?");
        }
        bld.append(";");
        String res = bld.toString();
        LOG.info("Generated SELECT statement: " + res);
        return bld.toString();
    }


    public String getDeleteStatement() {
        StringBuilder bld = new StringBuilder("DELETE FROM ");
        bld.append(tableName).append(" WHERE");
        boolean first = true;
        for (int i = 0; i < primaryKeySize; ++i) {
            ColumnInfo columnInfo = columnInfos.get(i);
            if (first) {
                first = false;
            } else {
                bld.append(" AND");
            }
            bld.append(" ").append(columnInfo.columnName).append("=?");
        }
        bld.append(";");
        String res = bld.toString();
        LOG.info("Generated DELETE statement: " + res);
        return bld.toString();
    }

    public static void checkResult(OperationResult<CqlResult<Integer, String>> result) throws Exception {
        if (result.getResult() == null) {
            return;
        }
        Rows<Integer, String> rows = result.getResult().getRows();
        if (rows.isEmpty()) { // ttt0 review this
            throw new Exception("DB Exception");
        }
    }
}
