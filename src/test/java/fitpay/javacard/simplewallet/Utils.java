package fitpay.javacard.simplewallet;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Hex encoding modeled after the bitcoin hex encoder:
 * https://github.com/bitcoin-labs/bitcoinj-minimal/blob/master/bouncycastle/util/encoders/HexEncoder.java
 * 
 * @author ssteveli
 *
 */
public class Utils {
    
    private final static byte[] encodingTable = {
        (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
        (byte)'8', (byte)'9', (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f'
    };
    
    private final static byte[] decodingTable = new byte[128];
    
    static {
        for (int i=0; i<encodingTable.length; i++) {
            decodingTable[encodingTable[i]] = (byte)i;
        }
        
        decodingTable['A'] = decodingTable['a'];
        decodingTable['B'] = decodingTable['b'];
        decodingTable['C'] = decodingTable['c'];
        decodingTable['D'] = decodingTable['d'];
        decodingTable['E'] = decodingTable['e'];
        decodingTable['F'] = decodingTable['f'];
    }
    
    public static String bytesToHexString(byte[] bytes) {
        StringWriter out = new StringWriter();
        for (int i=0; i<bytes.length; i++) {
            int v = bytes[i] & 0xff;
            out.write(encodingTable[(v >>> 4)]);
            out.write(encodingTable[v & 0xf]);
        }
        
        return out.toString();
    }

    public static byte[] hexStringToBytes(String s) {
        if (s == null || s.length() == 0) {
            throw new IllegalArgumentException("invalid hex string, it's empty");
        }
        
        if ((s.length() % 2) != 0) {
            throw new IllegalArgumentException("invalid hex string, length of " + s.length() + " is not an even number");
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        byte b1, b2;
        int end = s.length();
        
        while (end > 0) {
            if (!ignore(s.charAt(end-1))) {
                break;
            }
            end--;
        }
        
        int i=0;
        while (i<end) {
            while (i < end && ignore(s.charAt(i))) {
                i++;
            }
            
            try {
                b1 = decodingTable[s.charAt(i++)];
            } catch (StringIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid hex string (" + s + "), invalid character at position: " + i);
            }
            
            while (i < end && ignore(s.charAt(i))) {
                i++;
            }
            
            try {
                b2 = decodingTable[s.charAt(i++)];
            } catch (StringIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid hex string (" + s + "), invalid character at position: " + i);
            }
            
            out.write((b1 << 4) | b2);
        }
        
        return out.toByteArray();
    }
    
    private static boolean ignore(char c) {
        return (c == '\n' || c == '\r' || c == '\t' || c == ' ');
    }
    
    /**
     * borrowed from here: http://grepcode.com/file/repo1.maven.org/maven2/com.nimbusds/nimbus-jose-jwt/2.22/com/nimbusds/jose/util/BigIntegerUtils.java
     * 
     * @param   i
     *          The {@code BigInteger} to convert to an unsigned array of bytes.
     *
     * @return  The unsigned array of bytes of the {@code BigInteger}
     */
    public static byte[] toBytesUnsigned(final BigInteger i) {
        int bitlen = i.bitLength();
        bitlen = ((bitlen +7) >> 3) << 3;
        final byte[] bigBytes = i.toByteArray();
        
        if (((i.bitLength() % 8) != 0) && (((i.bitLength() / 8) + 1) == (bitlen /8))) {
            return bigBytes;
        }
        
        int startSrc = 0;
        int len = bigBytes.length;
        
        if ((i.bitLength() % 8) == 0) {
            startSrc = 1;
            len--;
        }
        
        final int startDst = bitlen / 8 - len;
        final byte[] resizedBytes = new byte[bitlen / 8];
        System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
        return resizedBytes;
    }

    
    // ssteveli: these next two methods were based on a sample provided by bionym that apparently swap byte order for a 32 byte array
    //
    // I tried using the standard java ByteBuffer to transfer from little endian to big endian, which I assume is what we're getting 
    // from the nymi, but that doesn't work.  Only this swap byte order seems to work.   Having said that they only provided a sample to
    // swap a 32 byte array, I created the 2nd method to swap a 64 byte array.   I'm not going to pretend to know why it works, I just
    // haven't done much hardware level programming that required byte order swapping, but these two methods yield EDSCA keys that work
    // with java.  It's magic... ;)
    
    public static byte[] swapOrder32Bit(byte[] cx) {
        byte[] x = new byte[cx.length];
        System.arraycopy(cx, 0, x, 0, cx.length);

        byte t;
        for (int i = 0; i < 8; i++) {
            t = x[i * 4];
            x[i * 4] = x[(i + 1) * 4 - 1];
            x[(i + 1) * 4 - 1] = t;
            t = x[i * 4 + 1];
            x[i * 4 + 1] = x[(i + 1) * 4 - 2];
            x[(i + 1) * 4 - 2] = t;
        }

        byte[] t2;
        for (int i = 0; i < 4; i++) {
            t2 = Arrays.copyOfRange(x, i * 4, (i + 1) * 4);
            for (int j = 0; j < 4; j++) {
                x[i * 4 + j] = x[(7 - i) * 4 + j];
            }

            for (int j = 0; j < 4; j++) {
                x[(7 - i) * 4 + j] = t2[j];
            }
        }

        return x;
    }
    
    public static byte[] swapOrder64Byte(byte[] cx) {
        byte[] first = new byte[cx.length/2];
        byte[] second = new byte[cx.length/2];
        
        System.arraycopy(cx, 0, first, 0, cx.length/2);
        System.arraycopy(cx, cx.length/2, second, 0, cx.length/2);
        
        first = swapOrder32Bit(first);
        second = swapOrder32Bit(second);
        
        byte[] x = new byte[cx.length];
        System.arraycopy(first, 0, x, 0, first.length);
        System.arraycopy(second, 0, x, cx.length/2, second.length);
        
        return x;
    }

    // borrowed from: http://stackoverflow.com/questions/3985392/generate-random-date-of-birth
    public static Date randomDate() {
        GregorianCalendar gc = new GregorianCalendar();
        int year = randBetween(2010, 2014);
        gc.set(GregorianCalendar.YEAR, year);

        int dayOfYear = randBetween(1, gc.getActualMaximum(GregorianCalendar.DAY_OF_YEAR));
        gc.set(GregorianCalendar.DAY_OF_YEAR, dayOfYear);

        return gc.getTime();
    }

    public static int randBetween(int start, int end) {
        return start + (int)Math.round(Math.random() * (end - start));
    }

}

