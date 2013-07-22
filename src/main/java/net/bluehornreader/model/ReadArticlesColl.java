package net.bluehornreader.model;

import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.model.*;
import net.bluehornreader.data.*;
import net.bluehornreader.misc.*;
import org.apache.commons.logging.*;

import java.util.*;

import static net.bluehornreader.misc.Utils.*;
import static net.bluehornreader.data.CqlTable.ColumnInfo;
import static net.bluehornreader.data.CqlTable.ColumnType.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-20
 * Time: 16:03
 * <p/>
 */
public class ReadArticlesColl {

    private static final Log LOG = LogFactory.getLog(ReadArticlesColl.class);

    // key
    public String userId;
    public String feedId;

    // columns
    int first;     // all before first are read; "first" is a multiple of 8
    byte[] bitmap; // 1 means "read"; what's newer than the bitmap is consideren unread
        // when bitmap is null first must be 0 and it means all are unread


    /**
     * for testing only
     *
     * @param o
     * @return
     */
    public boolean testEqual(ReadArticlesColl o) {
        if (first != o.first) return false;
        if (!Arrays.equals(bitmap, o.bitmap)) return false;

        return true;
    }

    public boolean isRead(int seq) {
        if (seq < first) {
            return true;
        }
        if (bitmap == null || seq >= first + bitmap.length * 8) {
            return false;
        }
        int p = (seq - first) / 8;
        byte k = (byte) (1 << (seq % 8));
        byte b = bitmap[p];
        return (b & k) != 0;
    }


    /**
     * Tries to expand the bitmap (or creates it) and maybe changes "first" so that the given value for seq can be
     * represented. It's not always possible. If it's not, just does nothing.
     *
     * @param seq
     * @param maxBitmapSize
     */
    private void makeRoomForRead(int seq, int maxBitmapSize) {
        if (seq < first) {
            return; // all is considered read before first
        }
        if (bitmap == null || // all is unread
                seq >= nextNotInBitmap() + 8 * maxBitmapSize) { // nothing from bitmap can be kept
            first = ((seq / 8) - maxBitmapSize + 1) * 8;
            if (first < 0) {
                maxBitmapSize += first / 8;
                first = 0;
            }
            bitmap = new byte[maxBitmapSize];
            return;
        }
        if (seq < nextNotInBitmap()) { // nothing to do; value fits in existing bitmap
            return;
        }

        seq = (seq / 8) * 8;
        // buffer has to be extended
//ttt2 maybe grow a few bytes more, if possible, to avoid constant resizing
        int bitmapSize = maxBitmapSize;
        if (bitmapSize > (seq - first) / 8 + 1) {
            bitmapSize = (seq - first) / 8 + 1;
        }
        byte[] bitmap2 = new byte[bitmapSize];
        int firstToCopy = seq / 8 - bitmapSize + 1;
        int lastToCopy = first / 8 + bitmap.length - 1;
        int countToCopy = lastToCopy - firstToCopy + 1;
        try {
            System.arraycopy(bitmap, bitmap.length - countToCopy, bitmap2, 0, countToCopy);
        } catch (Exception e) {
            LOG.error("Error in makeRoomForRead()", e);
            throw new RuntimeException("ppp");
        }
        bitmap = bitmap2;
        first = seq - bitmapSize * 8 + 8;

        trimBitmap(maxBitmapSize);
    }


    private void makeRoomForUnread(int seq, int maxBitmapSize) {
        if (bitmap == null) {
            return; // all are unread
        }
        if (seq >= first) { // either it's unread anyway or there's space in the bitmap
            return;
        }
        seq = (seq / 8) * 8;
        if (seq < nextNotInBitmap() - 8 * maxBitmapSize) { // would need to extend the bitmap too much
            return;
        }
        int extraBytes = (first - seq) / 8;
        byte[] bitmap2 = new byte[bitmap.length + extraBytes];
        for (int i = 0; i < extraBytes; ++i) {
            bitmap2[i] = (byte) 0xff;
        }
        System.arraycopy(bitmap, 0, bitmap2, extraBytes, bitmap.length);
        bitmap = bitmap2;
        first = seq;
    }


    /**
     * @param seq
     * @param maxBitmapSize - in bytes
     */
    public void markRead(int seq, int maxBitmapSize) {
        makeRoomForRead(seq, maxBitmapSize);
        if (seq < first) {
            return;
        }
        int p = (seq - first) / 8;
        byte k = (byte) (1 << (seq % 8));
        cbAssert(p < bitmap.length, "Bitmap was incorrectly resized for markRead");
        bitmap[p] = (byte) (bitmap[p] | k);
        trimBitmap(maxBitmapSize);
    }

    /**
     *
     * @param seqFrom
     * @param seqTo
     * @param maxBitmapSize - in bytes
     */
    public void markRead(int seqFrom, int seqTo, int maxBitmapSize) {

        cbAssert(seqFrom <= seqTo, "seqFrom > seqTo");
        if (seqTo < first) {
            return;
        }

        makeRoomForRead(seqFrom, maxBitmapSize);
        makeRoomForRead(seqTo, maxBitmapSize);

        seqFrom = Math.max(first, seqFrom);

        int pFrom = (seqFrom - first) / 8;
        int pTo = (seqTo - first) / 8;
        cbAssert(pTo < bitmap.length, "Bitmap was incorrectly resized for markRead");

        {
            byte k = (byte) (1 << (seqFrom % 8));
            do {
                bitmap[pFrom] = (byte) (bitmap[pFrom] | k);
                k = (byte) (k << 1);
                ++seqFrom;
            } while (seqFrom % 8 != 0 && seqFrom <= seqTo);
            if (seqFrom >= seqTo) { // to not assign 0xff to anything
                seqFrom += 8;
            }
            pFrom = (seqFrom - first) / 8;
        }

        {
            byte k = (byte) (1 << (seqTo % 8));
            do {
                bitmap[pTo] = (byte) (bitmap[pTo] | k);
                k = (byte) (k >> 1);
                --seqTo;
            } while (seqTo % 8 != 7 && seqFrom <= seqTo);
        }
        pTo = (seqTo - first) / 8;

        for (int i = pFrom; i <= pTo; ++i) {
            bitmap[i] = (byte) 0xff;
        }
        trimBitmap(maxBitmapSize);
    }

    /**
     * Marks an article as "unread". A special set of params (0, 0) allows marking all articles as unread (which seems more important for testing than as a
     * real use case.
     *
     * @param seq
     * @param maxBitmapSize - in bytes
     */
    public void markUnread(int seq, int maxBitmapSize) {
        if (seq == 0 && maxBitmapSize == 0) {
            bitmap = null;
            first = 0;
            return;
        }

        makeRoomForUnread(seq, maxBitmapSize);
        if (seq < first) { // no room
            return;
        }
        if (seq >= nextNotInBitmap()) { // unread anyway
            return;
        }
        int p = (seq - first) / 8;
        byte k = (byte) (~(1 << (seq % 8)));
        cbAssert(p < bitmap.length, "Bitmap was incorrectly resized for markRead");
        bitmap[p] = (byte) (bitmap[p] & k);
        trimBitmap(maxBitmapSize);
    }


    private int nextNotInBitmap() {
        return bitmap == null ? 0 : first + 8 * bitmap.length;
    }


    private void trimBitmap(int maxBitmapSize) {
        int firstToKeep = bitmap.length > maxBitmapSize ? bitmap.length - maxBitmapSize : 0;
        while (firstToKeep < bitmap.length - 1 && bitmap[firstToKeep] == (byte) 0xff) {
            ++firstToKeep;
            first += 8;
        }
        if (firstToKeep > 0) {
            bitmap = Arrays.copyOfRange(bitmap, firstToKeep, bitmap.length);
        }
    }


    private static class Columns {
        private static final String USER_ID_AND_FEED_ID = "user_id_and_feed_id";
        private static final String FIRST = "first";
        private static final String BITMAP = "bitmap";
    }

    public static CqlTable CQL_TABLE;
    static {
        List<ColumnInfo> columnInfos = new ArrayList<>();
        columnInfos.add(new ColumnInfo(Columns.USER_ID_AND_FEED_ID, TEXT));
        columnInfos.add(new ColumnInfo(Columns.FIRST, INT));
        columnInfos.add(new ColumnInfo(Columns.BITMAP, BLOB));
        CQL_TABLE = new CqlTable("read_articles", columnInfos);
    }

    public ReadArticlesColl(String userId, String feedId) {
        this.userId = userId;
        this.feedId = feedId;
    }

    @Override
    public String toString() {
        String bmp = bitmap == null ? "<NULL>" : PrintUtils.byteArrayAsString(bitmap);
        return "ReadArticlesColl{" +
                "userId='" + userId + '\'' +
                ", feedId='" + feedId + '\'' +
                ", first=" + first +
                ", bitmap=" + bmp +
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

        public void add(ReadArticlesColl readArticlesColl) throws Exception {
            add(Arrays.asList(readArticlesColl));
        }

        public void add(Collection<ReadArticlesColl> readArticlesColls) throws Exception {
            for (ReadArticlesColl readArticlesColl : readArticlesColls) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(UPDATE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(readArticlesColl.getKey())
                        .withIntegerValue(readArticlesColl.first)
                        .withByteBufferValue(readArticlesColl.bitmap, LowLevelDbAccess.BYTE_BUFFER_SERIALIZER)
                        //.withValue()
                        //.withLongValue(13L)
                        //.withStringValue("0xaabbccddee")
                        .execute();
                CqlTable.checkResult(result);
            }
        }

        public ReadArticlesColl get(String userId, String feedId) throws Exception {
            OperationResult<CqlResult<Integer, String>> result;
            result = lowLevelDbAccess.getMainKeyspace()
                    .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                    .withCql(SELECT_STATEMENT)
                    .asPreparedStatement()
                    .withStringValue(getKey(userId, feedId))
                    .execute();
            Rows<Integer, String> rows = result.getResult().getRows();


            if (rows.size() == 1) {
                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                ReadArticlesColl res = new ReadArticlesColl(userId, feedId);
                res.first = columns.getIntegerValue(Columns.FIRST, 0);
                res.bitmap = columns.getByteArrayValue(Columns.BITMAP, null);
                return res;
            }

            if (rows.size() > 1) {
                throw new RuntimeException(String.format("Duplicate entries for key <%s, %s>", userId, feedId));
            }

            return null;
        }


        public void delete(Collection<ReadArticlesColl> readArticlesColls) throws Exception {
            for (ReadArticlesColl readArticlesColl : readArticlesColls) {
                OperationResult<CqlResult<Integer, String>> result;
                result = lowLevelDbAccess.getMainKeyspace()
                        .prepareQuery(LowLevelDbAccess.RESULTS_CF)
                        .withCql(DELETE_STATEMENT)
                        .asPreparedStatement()
                        .withStringValue(readArticlesColl.getKey())
                        .execute();
                CqlTable.checkResult(result);
            }
        }
    }

    private static String getKey(String userId, String feedId) {
        return userId + Utils.LIST_SEPARATOR + feedId;
    }

    private String getKey() {
        return getKey(userId, feedId);
    }


}
