package arshin;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

final class MyRequestBuilder {

    private static final BitSet UNRESERVED = new BitSet(256);

    static {
        for (int i = 'a'; i <= 'z'; i++) {
            UNRESERVED.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            UNRESERVED.set(i);
        }
        // numeric characters
        for (int i = '0'; i <= '9'; i++) {
            UNRESERVED.set(i);
        }
        UNRESERVED.set('-');
        UNRESERVED.set('.');
        UNRESERVED.set('_');
        UNRESERVED.set('~');
        UNRESERVED.set(':');
    }

    private final String url;
    private final StringBuilder buf = new StringBuilder();

    MyRequestBuilder(String url) {
        this.url = url;
    }

    void addParameter(String key, String value) {
        if (buf.length() > 0) {
            buf.append("&");
        }
        encode(buf, key);
        buf.append('=');
        encode(buf, value);
    }

    ClassicHttpRequest build() {
        String uri;
        if (buf.length() > 0) {
            uri = url + "?" + buf;
        } else {
            uri = url;
        }
        return ClassicRequestBuilder.get(uri).build();
    }

    private static final int RADIX = 16;

    private static void encode(StringBuilder buf, CharSequence content) {
        encode(buf, content, UNRESERVED);
    }

    private static void encode(StringBuilder buf, CharSequence content, BitSet safechars) {
        CharBuffer cb = CharBuffer.wrap(content);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xFF;
            if (safechars.get(b)) {
                buf.append((char) b);
            } else {
                buf.append("%");
                buf.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, RADIX)));
                buf.append(Character.toUpperCase(Character.forDigit(b & 0xF, RADIX)));
            }
        }
    }
}
