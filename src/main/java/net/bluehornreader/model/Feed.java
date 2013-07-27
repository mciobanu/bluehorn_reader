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
import org.apache.commons.logging.*;

import java.util.*;

import static net.bluehornreader.data.CqlTable.ColumnInfo;
import static net.bluehornreader.data.CqlTable.ColumnType.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-20
 * Time: 15:55
 * <p/>
 */
public class Feed {

    private static final Log LOG = LogFactory.getLog(Feed.class);


    // key
    public String feedId;   // hash of the name

    // columns
    public String name;
    public String url;
    public int maxSeq; // article count in the feed - 1 (or the seq of the latest article); needed to easily get the last articles; -1 if there are no articles

    private static class Columns {
        private static final String FEED_ID = "feed_id";
        private static final String NAME = "name";
        private static final String URL = "url";
        private static final String MAX_SEQ = "max_seq";
    }

    private static String[] DISCARD_PREFIXES = new String[] { "www." };
    private static String[] DISCARD_SUFFIXES = new String[] { ".com", ".co" };

    public static CqlTable CQL_TABLE;
    static {
        List<ColumnInfo> columnInfos = new ArrayList<>();
        columnInfos.add(new ColumnInfo(Columns.FEED_ID, TEXT));
        columnInfos.add(new ColumnInfo(Columns.NAME, TEXT));
        columnInfos.add(new ColumnInfo(Columns.URL, TEXT));
        columnInfos.add(new ColumnInfo(Columns.MAX_SEQ, INT));
        CQL_TABLE = new CqlTable("feeds", columnInfos);
    }


    public Feed(String feedId, String url) {
        this.feedId = feedId;
        this.name = computeName(url);
        this.url = url;
        this.maxSeq = -1;
    }


    private Feed(String feedId, String name, String url, int maxSeq) {
        this.feedId = feedId;
        this.name = name;
        this.url = url;
        this.maxSeq = maxSeq;
    }


    private static String computeName(String url) { //ttt1 review this and make it better (e.g. connect and get the name from the actual feed)
        url = url.replaceAll("^.*//", "");
        url = url.replaceAll("/.*", "");
        url = url.replaceAll("\\.[^\\.]\\+", "");
        for (String s : DISCARD_PREFIXES) {
            if (url.startsWith(s)) {
                url = url.substring(s.length());
            }
        }
        for (String s : DISCARD_SUFFIXES) {
            if (url.startsWith(s)) {
                url = url.substring(0, url.length() - s.length());
            }
        }
        return url;
    }


    @Override
    public String toString() {
        return "Feed{" +
                "feedId='" + feedId + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", maxSeq=" + maxSeq +
                '}';
    }

    public static class DB {

        private LowLevelDbAccess lowLevelDbAccess;

        private static final String ADD_STATEMENT = CQL_TABLE.getUpdateStatement();
        private static final String UPDATE_MAX_SEQ_STATEMENT = CQL_TABLE.getUpdateStatement(Columns.FEED_ID, Columns.MAX_SEQ);
        private static final String UPDATE_NAME_STATEMENT = CQL_TABLE.getUpdateStatement(Columns.FEED_ID, Columns.NAME);

        private static final String SELECT_STATEMENT = CQL_TABLE.getSelectStatement();
        private static final String SELECT_MULTIPLE_STATEMENT = CQL_TABLE.getSelectMultipleStatement();
        private static final String SELECT_ALL_STATEMENT = CQL_TABLE.getSelectAllStatement();
        private static final String DELETE_STATEMENT = CQL_TABLE.getDeleteStatement();

        public DB(LowLevelDbAccess lowLevelDbAccess) {
            this.lowLevelDbAccess = lowLevelDbAccess;
        }

        public void add(Feed feed) throws Exception {
            add(Arrays.asList(feed));
        }

        public void add(Collection<Feed> feeds) throws Exception {
            for (Feed feed : feeds) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(ADD_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(feed.feedId)
                        .withStringValue(feed.name)
                        .withStringValue(feed.url)
                        .withIntegerValue(feed.maxSeq)
                        .execute();
                CqlTable.checkResult(result);
            }
        }

        public void updateMaxSeq(String feedId, int maxSeq) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_MAX_SEQ_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(feedId)
                    .withIntegerValue(maxSeq)
                    .execute();
            CqlTable.checkResult(result);
        }

        public void updateName(String feedId, String name) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_NAME_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(feedId)
                    .withStringValue(name)
                    .execute();
            CqlTable.checkResult(result);
        }

        public Feed get(String feedId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(feedId)
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                return new Feed(
                        feedId,
                        columns.getStringValue(Columns.NAME, ""),
                        columns.getStringValue(Columns.URL, ""),
                        columns.getIntegerValue(Columns.MAX_SEQ, 0));
            } else if (rows.size() > 1) { //ttt1 use the same pattern, includin error logging, for all types
                throw new RuntimeException(String.format("Duplicate entries for key <%s>", feedId));
            } else {
                LOG.error(String.format("No feed with id %s found.", feedId));
            }

            return null;
        }


        /*public ArrayList<Feed> get(Collection<String> feedIds) throws Exception {
            ArrayList<Feed> res = new ArrayList<>();
            for (String feedId : feedIds) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(SELECT_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(feedId)
                        .execute();
                Rows<Integer, String> rows = result.getResult().getRows();

                if (rows.size() == 1) {
                    ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                    res.add(new Feed(
                            feedId,
                            columns.getStringValue(Columns.NAME, ""),
                            columns.getStringValue(Columns.URL, ""),
                            columns.getIntegerValue(Columns.MAX_SEQ, 0)));
                } else if (rows.size() > 1) {
                    throw new RuntimeException(String.format("Duplicate entries for key <%s>", feedId));
                } else {
                    LOG.error(String.format("No feed with id %s found.", feedId));
                }
            }

            return res;
        }*/


        public ArrayList<Feed> get(Collection<String> feedIds) throws Exception {
            ArrayList<Feed> res = new ArrayList<>();
            if (feedIds.isEmpty()) {
                return res;
            }

            OperationResult<CqlResult<Integer, String>> result;

            // ttt2 this doesn't work, as we're trying to have multiple values in a single param; see if it can be made to work and if not maybe look at
            //  alternatives, although performance didn't seem to be a problem;
            // see http://stackoverflow.com/questions/178479/preparedstatement-in-clause-alternatives
            // see http://www.javaranch.com/journal/200510/Journal200510.jsp#a2
            /*result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_MULTIPLE_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(CqlTable.createMultipleSelectParam(feedIds))
                    .execute();*/


            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_MULTIPLE_STATEMENT.replace("?", CqlTable.createMultipleSelectParam(feedIds)))
                    .execute();

            Rows<Integer, String> rows = result.getResult().getRows();

            for (int i = 0; i < rows.size(); ++i) {
                ColumnList<String> columns = rows.getRowByIndex(i).getColumns();
                res.add(new Feed(
                        columns.getStringValue(Columns.FEED_ID, ""),
                        columns.getStringValue(Columns.NAME, ""),
                        columns.getStringValue(Columns.URL, ""),
                        columns.getIntegerValue(Columns.MAX_SEQ, 0)));
            }

            if (res.size() != feedIds.size()) {
                LOG.error("Mismatch between count of feeds expected and found");
            }

            return res;
        }


        public ArrayList<Feed> getAll() throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_ALL_STATEMENT)
                    .asPreparedStatement()
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            ArrayList<Feed> res = new ArrayList<>();

            for (int i = 0; i < rows.size(); ++i) {
                ColumnList<String> columns = rows.getRowByIndex(i).getColumns();
                res.add(new Feed(
                        columns.getStringValue(Columns.FEED_ID, ""),
                        columns.getStringValue(Columns.NAME, ""),
                        columns.getStringValue(Columns.URL, ""),
                        columns.getIntegerValue(Columns.MAX_SEQ, 0)));
            }

            return res;
        }


        public void delete(Collection<Feed> feeds) throws Exception {
            for (Feed feed : feeds) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(DELETE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(feed.feedId)
                        .execute();
                CqlTable.checkResult(result);
            }
        }
    }

}
