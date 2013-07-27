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

package net.bluehornreader.misc;

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
        ArrayList<ReadArticlesColl> ra = new ArrayList<>();
        ra.add(rd);
        db.add(ra);


        ReadArticlesColl get = db.get("usr1", "feed1");
        LOG.info(get);

        //LOG.info("duration (ms): " + (e - b));

        lowLevelDbAccess.shutDown();
    }
}
