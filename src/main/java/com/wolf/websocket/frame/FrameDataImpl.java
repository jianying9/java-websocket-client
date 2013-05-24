package com.wolf.websocket.frame;

import com.wolf.websocket.exceptions.InvalidDataException;
import com.wolf.websocket.exceptions.InvalidFrameException;
import com.wolf.websocket.util.Charsetfunctions;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FrameDataImpl implements FrameData {

    protected static byte[] emptyarray = {};
    protected boolean fin;
    protected Opcode optcode;
    private ByteBuffer unmaskedpayload;

    public FrameDataImpl() {
    }

    public FrameDataImpl(Opcode op) {
        this.optcode = op;
        unmaskedpayload = ByteBuffer.wrap(emptyarray);
    }

    /**
     * Helper constructor which helps to create "echo" frames. The new object
     * will use the same underlying payload data.
	 *
     */
    public FrameDataImpl(Frame f) {
        fin = f.isFin();
        optcode = f.getOpcode();
        unmaskedpayload = f.getPayloadData();
    }

    @Override
    public boolean isFin() {
        return fin;
    }

    @Override
    public Opcode getOpcode() {
        return optcode;
    }

    @Override
    public ByteBuffer getPayloadData() {
        return unmaskedpayload;
    }

    @Override
    public void setFin(boolean fin) {
        this.fin = fin;
    }

    @Override
    public void setOptcode(Opcode optcode) {
        this.optcode = optcode;
    }

    @Override
    public void setPayload(ByteBuffer payload) throws InvalidDataException {
        unmaskedpayload = payload;
    }

    @Override
    public void append(Frame nextframe) throws InvalidFrameException {
        ByteBuffer b = nextframe.getPayloadData();
        if (unmaskedpayload == null) {
            unmaskedpayload = ByteBuffer.allocate(b.remaining());
            b.mark();
            unmaskedpayload.put(b);
            b.reset();
        } else {
            b.mark();
            unmaskedpayload.position(unmaskedpayload.limit());
            unmaskedpayload.limit(unmaskedpayload.capacity());

            if (b.remaining() > unmaskedpayload.remaining()) {
                ByteBuffer tmp = ByteBuffer.allocate(b.remaining() + unmaskedpayload.capacity());
                unmaskedpayload.flip();
                tmp.put(unmaskedpayload);
                tmp.put(b);
                unmaskedpayload = tmp;

            } else {
                unmaskedpayload.put(b);
            }
            unmaskedpayload.rewind();
            b.reset();
        }
        fin = nextframe.isFin();
    }

    @Override
    public String toString() {
        return "Framedata{ optcode:" + getOpcode() + ", fin:" + isFin() + ", payloadlength:" + unmaskedpayload.limit() + ", payload:" + Arrays.toString(Charsetfunctions.utf8Bytes(new String(unmaskedpayload.array()))) + "}";
    }
}
