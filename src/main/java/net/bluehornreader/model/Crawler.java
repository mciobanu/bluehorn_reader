package net.bluehornreader.model;

import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.model.*;
import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;

import java.util.*;

import static net.bluehornreader.data.CqlTable.ColumnInfo;
import static net.bluehornreader.data.CqlTable.ColumnType.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-05-02
 * Time: 22:28
 * <p/>
 */
public class Crawler {

    // key
    public String crawlerId;   // IP address

    // columns
    public HashSet<String> feedIds; // written by CrawlerManager
    public int feedIdsSeq; // written by CrawlerManager when feedIds changes, to avoid parsing the IDs, comparing to previous, ...
    public int crawlTick; // written by Crawler periodically, so CrawlerManager knows that the feed is being crawled

    private static class Columns {
        private static final String CRAWLER_ID = "crawler_id";
        private static final String FEED_IDS = "feed_ids";
        private static final String FEED_IDS_SEQ = "feed_ids_seq";
        private static final String CRAWL_TICK = "crawl_tick";
    }

    public static CqlTable CQL_TABLE;
    static {
        List<ColumnInfo> columnInfos = new ArrayList<>();
        columnInfos.add(new ColumnInfo(Columns.CRAWLER_ID, TEXT));
        columnInfos.add(new ColumnInfo(Columns.FEED_IDS, TEXT));
        columnInfos.add(new ColumnInfo(Columns.FEED_IDS_SEQ, INT));
        columnInfos.add(new ColumnInfo(Columns.CRAWL_TICK, INT));
        CQL_TABLE = new CqlTable("crawlers", columnInfos);
    }

    public Crawler(String crawlerId, Collection<String> feedIds, int feedIdsSeq, int crawlTick) {
        this.crawlerId = crawlerId;
        this.feedIds = new HashSet<>(feedIds);
        this.feedIdsSeq = feedIdsSeq;
        this.crawlTick = crawlTick;
    }

    @Override
    public String toString() {
        return "Crawler{" +
                "crawlerId='" + crawlerId + '\'' +
                ", feedIds=" + PrintUtils.asString(feedIds) +
                ", feedIdsSeq=" + feedIdsSeq +
                ", crawlTick=" + crawlTick +
                '}';
    }

    public static class DB {

        private LowLevelDbAccess lowLevelDbAccess;

        private static final String UPDATE_FEEDS_STATEMENT = CQL_TABLE.getUpdateStatement(Columns.CRAWLER_ID, Columns.FEED_IDS, Columns.FEED_IDS_SEQ);
        private static final String UPDATE_CRAWL_STATEMENT = CQL_TABLE.getUpdateStatement(Columns.CRAWLER_ID, Columns.CRAWL_TICK);
        private static final String SELECT_FULL_STATEMENT = CQL_TABLE.getSelectStatement();
        private static final String SELECT_FEED_IDS_SEQ_STATEMENT = CQL_TABLE.getSelectSpecificColumnsStatement(Columns.FEED_IDS_SEQ);
        private static final String SELECT_ALL_STATEMENT = CQL_TABLE.getSelectAllStatement();
        private static final String DELETE_STATEMENT = CQL_TABLE.getDeleteStatement();

        public DB(LowLevelDbAccess lowLevelDbAccess) {
            this.lowLevelDbAccess = lowLevelDbAccess;
        }

        public void updateCrawl(String crawlerId, int crawlSeq) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_CRAWL_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(crawlerId)
                    .withIntegerValue(crawlSeq)
                    .execute();
            CqlTable.checkResult(result);
        }

        public void updateFeedList(String crawlerId, Collection<String> feedIds, int feedIdsSeq) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(UPDATE_FEEDS_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(crawlerId)
                    .withStringValue(Utils.listAsString(feedIds))
                    .withIntegerValue(feedIdsSeq)
                    .execute();
            CqlTable.checkResult(result);
        }

        public Crawler getCrawler(String crawlerId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_FULL_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(crawlerId)
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                return new Crawler(
                        crawlerId,
                        Utils.stringAsList(columns.getStringValue(Columns.FEED_IDS, "")),
                        columns.getIntegerValue(Columns.FEED_IDS_SEQ, 0),
                        columns.getIntegerValue(Columns.CRAWL_TICK, 0));
            }

            if (rows.size() > 1) {
                throw new RuntimeException(String.format("Duplicate entries for key <%s>", crawlerId));
            }

            return null;
        }


        public ArrayList<Crawler> getAll() throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_ALL_STATEMENT)
                    .asPreparedStatement()
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            ArrayList<Crawler> res = new ArrayList<>();
            for (int i = 0; i < rows.size(); ++i) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                res.add(new Crawler(
                        columns.getStringValue(Columns.CRAWLER_ID, ""),
                        Utils.stringAsList(columns.getStringValue(Columns.FEED_IDS, "")),
                        columns.getIntegerValue(Columns.FEED_IDS_SEQ, 0),
                        columns.getIntegerValue(Columns.CRAWL_TICK, 0)));
            }

            return res;
        }


        public int getFeedIdsSeq(String crawlerId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_FEED_IDS_SEQ_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(crawlerId)
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();

            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                return columns.getIntegerValue(Columns.FEED_IDS_SEQ, 0);
            }

            if (rows.size() > 1) {
                throw new RuntimeException(String.format("Duplicate entries for key <%s>", crawlerId));
            }

            return 0;
        }

        public void delete(Collection<Crawler> crawlers) throws Exception {
            for (Crawler crawler : crawlers) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(DELETE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(crawler.crawlerId)
                        .execute();
                CqlTable.checkResult(result);
            }
        }
    }
}
