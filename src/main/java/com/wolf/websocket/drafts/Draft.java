package com.wolf.websocket.drafts;

import com.wolf.websocket.exceptions.InvalidDataException;
import com.wolf.websocket.exceptions.InvalidHandshakeException;
import com.wolf.websocket.exceptions.LimitExedeedException;
import com.wolf.websocket.frame.CloseFrame;
import com.wolf.websocket.frame.Frame;
import com.wolf.websocket.handshake.ClientHandshake;
import com.wolf.websocket.handshake.Handshakedata;
import com.wolf.websocket.handshake.ServerHandshake;
import com.wolf.websocket.handshake.ServerHandshakeImpl;
import com.wolf.websocket.util.Charsetfunctions;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Base class for everything of a websocket specification which is not common
 * such as the way the handshake is read or frames are transfered.
 *
 */
public abstract class Draft {

    public enum HandshakeState {

        /**
         * Handshake matched this Draft successfully
         */
        MATCHED,
        /**
         * Handshake is does not match this Draft
         */
        NOT_MATCHED
    }

    public static int MAX_FAME_SIZE = 1000 * 1;
    public static int INITIAL_FAMESIZE = 64;
    public static final byte[] FLASH_POLICY_REQUEST = Charsetfunctions.utf8Bytes("<policy-file-request/>\0");

    public static ByteBuffer readLine(ByteBuffer buf) {
        ByteBuffer sbuf = ByteBuffer.allocate(buf.remaining());
        byte prev;
        byte cur = '0';
        while (buf.hasRemaining()) {
            prev = cur;
            cur = buf.get();
            sbuf.put(cur);
            if (prev == (byte) '\r' && cur == (byte) '\n') {
                sbuf.limit(sbuf.position() - 2);
                sbuf.position(0);
                return sbuf;

            }
        }
        // ensure that there wont be any bytes skipped
        buf.position(buf.position() - sbuf.position());
        return null;
    }

    public static String readStringLine(ByteBuffer buf) {
        ByteBuffer b = readLine(buf);
        return b == null ? null : Charsetfunctions.stringAscii(b.array(), 0, b.limit());
    }

    public static ServerHandshake translateHandshakeHttp(ByteBuffer buf) {
        ServerHandshake handshake;
        String line = readStringLine(buf);
        String[] firstLineTokens = line.split(" ", 3);// eg. HTTP/1.1 101 Switching the Protocols
        if (firstLineTokens.length != 3) {
            throw new RuntimeException("error server response:" + line);
        }
        // translating/parsing the response from the SERVER
        handshake = new ServerHandshakeImpl();
        handshake.setHttpStatus(Short.parseShort(firstLineTokens[1]));
        handshake.setHttpStatusMessage(firstLineTokens[2]);
        line = readStringLine(buf);
        while (line != null && line.length() > 0) {
            String[] pair = line.split(":", 2);
            if (pair.length != 2) {
                throw new RuntimeException("not an http header:" + line);
            }
            handshake.put(pair[ 0], pair[ 1].replaceFirst("^ +", ""));
            line = readStringLine(buf);
        }
        return handshake;
    }

    public abstract HandshakeState acceptHandshakeAsClient(ClientHandshake request, ServerHandshake response);

    protected boolean basicAccept(Handshakedata handshakedata) {
        return handshakedata.getFieldValue("Upgrade").equalsIgnoreCase("websocket") && handshakedata.getFieldValue("Connection").toLowerCase(Locale.ENGLISH).contains("upgrade");
    }

    public abstract ByteBuffer createBinaryFrame(Frame framedata); // TODO Allow to send data on the base of an Iterator or InputStream

    public abstract List<Frame> createFrames(String text);

    public abstract void reset();

    public ByteBuffer createHandshake(Handshakedata handshakedata) {
        return createHandshake(handshakedata, true);
    }

    public ByteBuffer createHandshake(Handshakedata handshakedata, boolean withcontent) {
        StringBuilder bui = new StringBuilder(100);
        bui.append("GET ");
        bui.append(((ClientHandshake) handshakedata).getResourceDescriptor());
        bui.append(" HTTP/1.1");
        bui.append("\r\n");
        Iterator<String> it = handshakedata.iterateHttpFields();
        while (it.hasNext()) {
            String fieldname = it.next();
            String fieldvalue = handshakedata.getFieldValue(fieldname);
            bui.append(fieldname);
            bui.append(": ");
            bui.append(fieldvalue);
            bui.append("\r\n");
        }
        bui.append("\r\n");
        byte[] httpheader = Charsetfunctions.asciiBytes(bui.toString());
        byte[] content = withcontent ? handshakedata.getContent() : null;
        ByteBuffer bytebuffer = ByteBuffer.allocate((content == null ? 0 : content.length) + httpheader.length);
        bytebuffer.put(httpheader);
        if (content != null) {
            bytebuffer.put(content);
        }
        bytebuffer.flip();
        return bytebuffer;
    }

    public abstract ClientHandshake postProcessHandshakeRequestAsClient(ClientHandshake request);

    public abstract ServerHandshake postProcessHandshakeResponseAsServer(ClientHandshake request, ServerHandshake response) throws InvalidHandshakeException;

    public abstract List<Frame> translateFrame(ByteBuffer buffer) throws InvalidDataException;

    /**
     * Drafts must only be by one websocket at all. To prevent drafts to be used
     * more than once the Websocket implementation should call this method in
     * order to create a new usable version of a given draft instance.<br> The
     * copy can be safely used in conjunction with a new websocket connection.
     *
     */
    public ServerHandshake translateHandshake(ByteBuffer buf) {
        return translateHandshakeHttp(buf);
    }

    public int checkAlloc(int bytecount) throws LimitExedeedException, InvalidDataException {
        if (bytecount < 0) {
            throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR, "Negative count");
        }
        return bytecount;
    }
}
