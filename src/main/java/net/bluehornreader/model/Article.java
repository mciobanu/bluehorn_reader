package net.bluehornreader.model;

import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.model.*;
import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;
import org.apache.commons.logging.*;

import java.text.*;
import java.util.*;

import static net.bluehornreader.data.CqlTable.*;
import static net.bluehornreader.data.CqlTable.ColumnType.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-20
 * Time: 15:57
 * <p/>
 */
public class Article {

    private static final Log LOG = LogFactory.getLog(Article.class);


    // key
    public String feedId;
    public int seq;

    // columns
    public String title;
    public String summary;
    public String url;
    public String content;
    public long publishTime;

    private static class Columns {
        private static final String FEED_ID_AND_SEQ = "feed_id_and_seq";
        private static final String TITLE = "title";
        private static final String SUMMARY = "summary";
        private static final String URL = "url";
        private static final String CONTENT = "content";
        private static final String PUBLISH_TIME = "publish_time";
    }

    public static CqlTable CQL_TABLE;
    static {
        List<ColumnInfo> columnInfos = new ArrayList<>();
        columnInfos.add(new ColumnInfo(Columns.FEED_ID_AND_SEQ, TEXT));
        columnInfos.add(new ColumnInfo(Columns.TITLE, TEXT));
        columnInfos.add(new ColumnInfo(Columns.SUMMARY, TEXT));
        columnInfos.add(new ColumnInfo(Columns.URL, TEXT));
        columnInfos.add(new ColumnInfo(Columns.CONTENT, TEXT));
        columnInfos.add(new ColumnInfo(Columns.PUBLISH_TIME, BIGINT));
        CQL_TABLE = new CqlTable("articles", columnInfos);
    }

    public Article(String feedId, int seq, String title, String summary, String url, String content, long publishTime) {
        this.feedId = feedId;
        this.seq = seq;
        this.title = title;
        this.summary = summary;
        this.url = url;
        this.content = content;
        this.publishTime = publishTime;
    }

    @Override
    public String toString() {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz", Locale.US);
        return "Article{" +
                "feedId='" + feedId + '\'' +
                ", seq=" + seq +
                ", title='" + title + '\'' +
                ", summary='" + summary + '\'' +
                ", url='" + url + '\'' +
                ", content='" + content + '\'' +
                ", publishTime=" + publishTime + "(" + fmt.format(new Date(publishTime)) + ")" +
                '}';
    }

    public static class DB {

        private LowLevelDbAccess lowLevelDbAccess;

        private static final String UPDATE_STATEMENT = CQL_TABLE.getUpdateStatement();
        private static final String SELECT_STATEMENT = CQL_TABLE.getSelectStatement();
        private static final String SELECT_MULTIPLE_STATEMENT = CQL_TABLE.getSelectMultipleStatement();
        private static final String DELETE_STATEMENT = CQL_TABLE.getDeleteStatement();


        public DB(LowLevelDbAccess lowLevelDbAccess) {
            this.lowLevelDbAccess = lowLevelDbAccess;
        }

        public void add(Collection<Article> articles) throws Exception {
            for (Article article : articles) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(UPDATE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(article.getKey())
                        .withStringValue(article.title)
                        .withStringValue(article.summary)
                        .withStringValue(article.url)
                        .withStringValue(article.content)
                        .withLongValue(article.publishTime)
                        .execute();
                CqlTable.checkResult(result);
            }
        }

        public Article get(String feedId, int seq) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(getKey(feedId, seq))
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                return new Article(
                        feedId,
                        seq,
                        columns.getStringValue(Columns.TITLE, ""),
                        columns.getStringValue(Columns.SUMMARY, ""),
                        columns.getStringValue(Columns.URL, ""),
                        columns.getStringValue(Columns.CONTENT, ""),
                        columns.getLongValue(Columns.PUBLISH_TIME, 0L));
            }

            if (rows.size() > 1) {
                throw new RuntimeException(String.format("Duplicate entries for key <%s, %s>", feedId, seq));
            }

            return null;
        }


        /**
         * @param feedId
         * @param minSeq
         * @param maxSeq
         * @return feeds from minSeq (inclusively) to maxSeq (exclusively)
         * @throws Exception
         */
        public List<Article> get(String feedId, int minSeq, int maxSeq) throws Exception {

            List<Article> res = new ArrayList<>();
            if (maxSeq <= minSeq) {
                return res;
            }
            OperationResult<CqlResult<Integer, String>> result;
            ArrayList<String> keys = new ArrayList<>();
            for (int i = minSeq; i < maxSeq; ++i) {
                keys.add(getKey(feedId, i));
            }

            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_MULTIPLE_STATEMENT.replace("?", CqlTable.createMultipleSelectParam(keys))) //ttt2 see if possible to use prepared statement
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            for (int i = 0; i < rows.size(); ++i) {

                ColumnList<String> columns = rows.getRowByIndex(i).getColumns();
                int seq = Integer.parseInt(columns.getStringValue(Columns.FEED_ID_AND_SEQ, "").substring(feedId.length() + 1));
                res.add(new Article(
                        feedId,
                        seq,
                        columns.getStringValue(Columns.TITLE, ""),
                        columns.getStringValue(Columns.SUMMARY, ""),
                        columns.getStringValue(Columns.URL, ""),
                        columns.getStringValue(Columns.CONTENT, ""),
                        columns.getLongValue(Columns.PUBLISH_TIME, 0L)));
            }

            if (res.size() != maxSeq - minSeq) {
                LOG.error("Mismatch between count of articles expected and found");
            }

            return res;
        }


        public void delete(Collection<Article> articles) throws Exception {
            for (Article article : articles) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(DELETE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(article.getKey())
                        .execute();
                CqlTable.checkResult(result);
            }
        }
    }

    private static String getKey(String feedId, int seq) {
        return feedId + Utils.LIST_SEPARATOR + seq;
    }

    private String getKey() {
        return getKey(feedId, seq);
    }
}
