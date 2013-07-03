package net.bluehornreader;

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

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String byteArrayAsString(byte[] bytes) {  //ttt1 look at DatatypeConverter.printHexBinary(byte[])
        char[] bfr = new char[bytes.length * 2];
        int k;
        for (int i = 0; i < bytes.length; i++ ) {
            k = bytes[i] & 0xFF;
            bfr[i * 2] = HEX_DIGITS[k >>> 4];
            bfr[i * 2 + 1] = HEX_DIGITS[k & 0x0F];
        }
        return new String(bfr);
    }

    public static String byteArrayAsUrlString(byte[] bytes) {
        return DatatypeConverter.printBase64Binary(bytes).replace('/', '_').replace('+', '-').replace('=', '.');
    }

}
