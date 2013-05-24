package com.wolf.websocket.drafts;

import com.wolf.websocket.exceptions.InvalidDataException;
import com.wolf.websocket.exceptions.InvalidFrameException;
import com.wolf.websocket.exceptions.InvalidHandshakeException;
import com.wolf.websocket.exceptions.LimitExedeedException;
import com.wolf.websocket.exceptions.NotSendableException;
import com.wolf.websocket.frame.CloseFrameImpl;
import com.wolf.websocket.frame.FrameData;
import com.wolf.websocket.frame.Frame;
import com.wolf.websocket.frame.Frame.Opcode;
import com.wolf.websocket.frame.FrameDataImpl;
import com.wolf.websocket.handshake.ClientHandshake;
import com.wolf.websocket.handshake.ServerHandshake;
import com.wolf.websocket.util.Base64;
import com.wolf.websocket.util.Charsetfunctions;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Draft_17 extends Draft {

    private class IncompleteException extends Throwable {

        /**
         * It's Serializable.
         */
        private static final long serialVersionUID = 7330519489840500997L;
        private int preferedsize;

        public IncompleteException(int preferedsize) {
            this.preferedsize = preferedsize;
        }

        public int getPreferedSize() {
            return preferedsize;
        }
    }

    private ByteBuffer incompleteframe;
    private Frame fragmentedframe;
    private final Random reuseableRandom = new Random();

    @Override
    public HandshakeState acceptHandshakeAsClient(ClientHandshake request, ServerHandshake response) {
        if (request.hasFieldValue("Sec-WebSocket-Key") == false || response.hasFieldValue("Sec-WebSocket-Accept") == false) {
            return HandshakeState.NOT_MATCHED;
        }
        String seckey_answere = response.getFieldValue("Sec-WebSocket-Accept");
        String seckey_challenge = request.getFieldValue("Sec-WebSocket-Key");
        seckey_challenge = generateFinalKey(seckey_challenge);
        if (seckey_challenge.equals(seckey_answere)) {
            return HandshakeState.MATCHED;
        }
        return HandshakeState.NOT_MATCHED;
    }

    @Override
    public ByteBuffer createBinaryFrame(Frame framedata) {
        ByteBuffer mes = framedata.getPayloadData();
        int sizebytes = mes.remaining() <= 125 ? 1 : mes.remaining() <= 65535 ? 2 : 8;
        ByteBuffer buf = ByteBuffer.allocate(1 + (sizebytes > 1 ? sizebytes + 1 : sizebytes) + 4 + mes.remaining());
        byte optcode = fromOpcode(framedata.getOpcode());
        byte one = (byte) (framedata.isFin() ? -128 : 0);
        one |= optcode;
        buf.put(one);
        byte[] payloadlengthbytes = toByteArray(mes.remaining(), sizebytes);
        assert (payloadlengthbytes.length == sizebytes);

        if (sizebytes == 1) {
            buf.put((byte) ((byte) payloadlengthbytes[0] | (byte) -128));
        } else if (sizebytes == 2) {
            buf.put((byte) ((byte) 126 | (byte) -128));
            buf.put(payloadlengthbytes);
        } else if (sizebytes == 8) {
            buf.put((byte) ((byte) 127 | (byte) -128));
            buf.put(payloadlengthbytes);
        } else {
            throw new RuntimeException("Size representation not supported/specified");
        }
        ByteBuffer maskkey = ByteBuffer.allocate(4);
        maskkey.putInt(reuseableRandom.nextInt());
        buf.put(maskkey.array());
        for (int i = 0; i < mes.limit(); i++) {
            buf.put((byte) (mes.get() ^ maskkey.get(i % 4)));
        }
        // translateFrame ( buf.array () , buf.array ().length );
        assert (buf.remaining() == 0) : buf.remaining();
        buf.flip();
        return buf;
    }

    @Override
    public List<Frame> createFrames(String text) {
        FrameData curframe = new FrameDataImpl();
        try {
            curframe.setPayload(ByteBuffer.wrap(Charsetfunctions.utf8Bytes(text)));
        } catch (InvalidDataException e) {
            throw new NotSendableException(e);
        }
        curframe.setFin(true);
        curframe.setOptcode(Opcode.TEXT);
        return Collections.singletonList((Frame) curframe);
    }

    private byte fromOpcode(Opcode opcode) {
        if (opcode == Opcode.TEXT) {
            return 1;
        } else if (opcode == Opcode.CLOSING) {
            return 8;
        }
        throw new RuntimeException("Don't know how to handle " + opcode.toString());
    }

    private String generateFinalKey(String in) {
        String seckey = in.trim();
        String acc = seckey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest sh1;
        try {
            sh1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return Base64.encodeBytes(sh1.digest(acc.getBytes()));
    }

    @Override
    public ClientHandshake postProcessHandshakeRequestAsClient(ClientHandshake request) {
        request.put("Upgrade", "websocket");
        request.put("Connection", "Upgrade"); // to respond to a Connection keep alives
        request.put("Sec-WebSocket-Version", "13");
        byte[] random = new byte[16];
        reuseableRandom.nextBytes(random);
        request.put("Sec-WebSocket-Key", Base64.encodeBytes(random));
        return request;
    }

    @Override
    public ServerHandshake postProcessHandshakeResponseAsServer(ClientHandshake request, ServerHandshake response) throws InvalidHandshakeException {
        response.put("Upgrade", "websocket");
        response.put("Connection", request.getFieldValue("Connection")); // to respond to a Connection keep alives
        response.setHttpStatusMessage("Switching Protocols");
        String seckey = request.getFieldValue("Sec-WebSocket-Key");
        if (seckey == null) {
            throw new InvalidHandshakeException("missing Sec-WebSocket-Key");
        }
        response.put("Sec-WebSocket-Accept", generateFinalKey(seckey));
        return response;
    }

    private byte[] toByteArray(long val, int bytecount) {
        byte[] buffer = new byte[bytecount];
        int highest = 8 * bytecount - 8;
        for (int i = 0; i < bytecount; i++) {
            buffer[ i] = (byte) (val >>> (highest - 8 * i));
        }
        return buffer;
    }

    private Opcode toOpcode(byte opcode) throws InvalidFrameException {
        switch (opcode) {
            case 1:
                return Opcode.TEXT;
            // 3-7 are not yet defined
            case 8:
                return Opcode.CLOSING;
            // 11-15 are not yet defined
            default:
                throw new InvalidFrameException("unknow optcode " + (short) opcode);
        }
    }

    @Override
    public List<Frame> translateFrame(ByteBuffer buffer) throws LimitExedeedException, InvalidDataException {
        List<Frame> frames = new LinkedList<Frame>();
        Frame cur;

        if (incompleteframe != null) {
            // complete an incomplete frame
            while (true) {
                try {
                    buffer.mark();
                    int available_next_byte_count = buffer.remaining();// The number of bytes received
                    int expected_next_byte_count = incompleteframe.remaining();// The number of bytes to complete the incomplete frame

                    if (expected_next_byte_count > available_next_byte_count) {
                        // did not receive enough bytes to complete the frame
                        incompleteframe.put(buffer.array(), buffer.position(), available_next_byte_count);
                        buffer.position(buffer.position() + available_next_byte_count);
                        return Collections.emptyList();
                    }
                    incompleteframe.put(buffer.array(), buffer.position(), expected_next_byte_count);
                    buffer.position(buffer.position() + expected_next_byte_count);

                    cur = translateSingleFrame((ByteBuffer) incompleteframe.duplicate().position(0));
                    frames.add(cur);
                    incompleteframe = null;
                    break; // go on with the normal frame receival
                } catch (IncompleteException e) {
                    // extending as much as suggested
                    int oldsize = incompleteframe.limit();
                    ByteBuffer extendedframe = ByteBuffer.allocate(checkAlloc(e.getPreferedSize()));
                    assert (extendedframe.limit() > incompleteframe.limit());
                    incompleteframe.rewind();
                    extendedframe.put(incompleteframe);
                    incompleteframe = extendedframe;

                    return translateFrame(buffer);
                }
            }
        }

        while (buffer.hasRemaining()) {// Read as much as possible full frames
            buffer.mark();
            try {
                cur = translateSingleFrame(buffer);
                frames.add(cur);
            } catch (IncompleteException e) {
                // remember the incomplete data
                buffer.reset();
                int pref = e.getPreferedSize();
                incompleteframe = ByteBuffer.allocate(checkAlloc(pref));
                incompleteframe.put(buffer);
                break;
            }
        }
        return frames;
    }

    public Frame translateSingleFrame(ByteBuffer buffer) throws IncompleteException, InvalidDataException {
        int maxpacketsize = buffer.remaining();
        int realpacketsize = 2;
        if (maxpacketsize < realpacketsize) {
            throw new IncompleteException(realpacketsize);
        }
        byte b1 = buffer.get( /*0*/);
        boolean FIN = b1 >> 8 != 0;
        byte rsv = (byte) ((b1 & ~(byte) 128) >> 4);
        if (rsv != 0) {
            throw new InvalidFrameException("bad rsv " + rsv);
        }
        byte b2 = buffer.get( /*1*/);
        boolean MASK = (b2 & -128) != 0;
        int payloadlength = (byte) (b2 & ~(byte) 128);
        Opcode optcode = toOpcode((byte) (b1 & 15));

        if (!FIN) {
            if (optcode == Opcode.CLOSING) {
                throw new InvalidFrameException("control frames may no be fragmented");
            }
        }

        if (payloadlength >= 0 && payloadlength <= 125) {
        } else {
            if (optcode == Opcode.CLOSING) {
                throw new InvalidFrameException("more than 125 octets");
            }
            if (payloadlength == 126) {
                realpacketsize += 2; // additional length bytes
                if (maxpacketsize < realpacketsize) {
                    throw new IncompleteException(realpacketsize);
                }
                byte[] sizebytes = new byte[3];
                sizebytes[ 1] = buffer.get( /*1 + 1*/);
                sizebytes[ 2] = buffer.get( /*1 + 2*/);
                payloadlength = new BigInteger(sizebytes).intValue();
            } else {
                realpacketsize += 8; // additional length bytes
                if (maxpacketsize < realpacketsize) {
                    throw new IncompleteException(realpacketsize);
                }
                byte[] bytes = new byte[8];
                for (int i = 0; i < 8; i++) {
                    bytes[ i] = buffer.get( /*1 + i*/);
                }
                long length = new BigInteger(bytes).longValue();
                if (length > Integer.MAX_VALUE) {
                    throw new LimitExedeedException("Payloadsize is to big...");
                } else {
                    payloadlength = (int) length;
                }
            }
        }

        // int maskskeystart = foff + realpacketsize;
        realpacketsize += (MASK ? 4 : 0);
        // int payloadstart = foff + realpacketsize;
        realpacketsize += payloadlength;

        if (maxpacketsize < realpacketsize) {
            throw new IncompleteException(realpacketsize);
        }

        ByteBuffer payload = ByteBuffer.allocate(checkAlloc(payloadlength));
        if (MASK) {
            byte[] maskskey = new byte[4];
            buffer.get(maskskey);
            for (int i = 0; i < payloadlength; i++) {
                payload.put((byte) ((byte) buffer.get( /*payloadstart + i*/) ^ (byte) maskskey[ i % 4]));
            }
        } else {
            payload.put(buffer.array(), buffer.position(), payload.limit());
            buffer.position(buffer.position() + payload.limit());
        }

        FrameData frame;
        if (optcode == Opcode.CLOSING) {
            frame = new CloseFrameImpl();
        } else {
            frame = new FrameDataImpl();
            frame.setFin(FIN);
            frame.setOptcode(optcode);
        }
        payload.flip();
        frame.setPayload(payload);
        return frame;
    }

    @Override
    public void reset() {
        incompleteframe = null;
    }
}
