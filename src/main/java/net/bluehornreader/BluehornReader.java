package net.bluehornreader;

import net.bluehornreader.data.*;
import net.bluehornreader.model.*;
import org.apache.commons.logging.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-27
 * Time: 12:17
 * <p/>
 */
public class BluehornReader {

    private static final Log LOG = LogFactory.getLog(BluehornReader.class);

    public static void main(String[] args) throws Exception {
        //tst2();
    }

    static void tst1() throws Exception {
        LowLevelDbAccess lowLevelDbAccess = new LowLevelDbAccess();

        Article.DB articleDb = new Article.DB(lowLevelDbAccess);
        LOG.info(articleDb.get("feed65", 995));

        //LOG.info("duration (ms): " + (e - b));

        lowLevelDbAccess.shutDown();

    }

    static void tst2() throws Exception {
        LowLevelDbAccess lowLevelDbAccess = new LowLevelDbAccess();

        ReadArticlesColl.DB db = new ReadArticlesColl.DB(lowLevelDbAccess);

        ReadArticlesColl rd = new ReadArticlesColl("usr1", "feed1");
        rd.markRead(10, 20);
        ArrayList<ReadArticlesColl> ra = new ArrayList<ReadArticlesColl>();
        ra.add(rd);
        db.add(ra);


        ReadArticlesColl get = db.get("usr1", "feed1");
        LOG.info(get);

        //LOG.info("duration (ms): " + (e - b));

        lowLevelDbAccess.shutDown();
    }
}
