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

package net.bluehornreader.model;

import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.model.*;
import net.bluehornreader.data.*;

import java.util.*;

import static net.bluehornreader.data.CqlTable.*;
import static net.bluehornreader.data.CqlTable.ColumnType.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-29
 * Time: 21:45
 * <p/>
 */
public class Election {

    // key
    public String electionId;   // multiple elections can be hosted, each in its own row

    // columns
    public String leaderId;
    public int tick;
    public String dataVersion;

    private static class Columns {
        private static final String ELECTION_ID = "election_id";
        private static final String LEADER_ID = "leader_id";
        private static final String TICK = "tick";
        private static final String DATA_VERSION = "data_version";
    }

    public static CqlTable CQL_TABLE;
    static {
        List<ColumnInfo> columnInfos = new ArrayList<>();
        columnInfos.add(new ColumnInfo(Columns.ELECTION_ID, TEXT));
        columnInfos.add(new ColumnInfo(Columns.LEADER_ID, TEXT));
        columnInfos.add(new ColumnInfo(Columns.TICK, INT));
        columnInfos.add(new ColumnInfo(Columns.DATA_VERSION, TEXT));
        CQL_TABLE = new CqlTable("elections", columnInfos);
    }

    public Election(String electionId, String leaderId, int tick, String dataVersion) {
        this.electionId = electionId;
        this.leaderId = leaderId;
        this.tick = tick;
        this.dataVersion = dataVersion;
    }

    @Override
    public String toString() {
        return "Election{" +
                "electionId='" + electionId + '\'' +
                ", leaderId='" + leaderId + '\'' +
                ", tick=" + tick +
                ", dataVersion='" + dataVersion + '\'' +
                '}';
    }

    public static class DB {

        private LowLevelDbAccess lowLevelDbAccess;

        private final String UPDATE_TICK_STATEMENT;
        private final String UPDATE_DATA_VERSION_STATEMENT;
        private final String SELECT_STATEMENT;

        public DB(LowLevelDbAccess lowLevelDbAccess) {
            this.lowLevelDbAccess = lowLevelDbAccess;
            UPDATE_TICK_STATEMENT = CQL_TABLE.getUpdateStatement(Columns.ELECTION_ID, Columns.LEADER_ID, Columns.TICK);
            UPDATE_DATA_VERSION_STATEMENT = CQL_TABLE.getUpdateStatement(Columns.ELECTION_ID, Columns.DATA_VERSION);
            SELECT_STATEMENT = CQL_TABLE.getSelectStatement();
        }

        public void updateTick(String electionId, String leaderId, int tick) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getElectionKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_TICK_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(electionId)
                    .withStringValue(leaderId)
                    .withIntegerValue(tick)
                    .execute();
            CqlTable.checkResult(result);
        }

        public void updateDataVersion(String electionId, String dataVersion) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getElectionKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_DATA_VERSION_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(electionId)
                    .withStringValue(dataVersion)
                    .execute();
            CqlTable.checkResult(result);
        }

        public Election get(String electionId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getElectionKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(electionId)
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                return new Election(
                        electionId,
                        columns.getStringValue(Columns.LEADER_ID, ""),
                        columns.getIntegerValue(Columns.TICK, Integer.MAX_VALUE),
                        columns.getStringValue(Columns.DATA_VERSION, ""));
            }

            if (rows.size() > 1) {
                throw new RuntimeException(String.format("Duplicate entries for key <%s>", electionId));
            }

            return null;
        }
    }
}
