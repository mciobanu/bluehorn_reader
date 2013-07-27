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

import javax.xml.bind.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-05-02
 * Time: 22:38
 * <p/>
 */
public class PrintUtils {

    public static String asString(Collection<?> coll) {
        StringBuilder bld = new StringBuilder("[");
        boolean first = true;
        for (Object o : coll) {
            if (first) {
                first = false;
            } else {
                bld.append(", ");
            }
            bld.append(o.toString());
        }
        return bld.append("]").toString();
    }

    public static <T> String asString(T[] objects) {
        return asString(Arrays.asList(objects));
    }

    /**
     * @param bytes
     * @return representation based on Base64 with URL-friendly characters
     */
    public static String byteArrayAsUrlString(byte[] bytes) {
        return DatatypeConverter.printBase64Binary(bytes).replace('/', '_').replace('+', '-').replace('=', '.');
    }
}
