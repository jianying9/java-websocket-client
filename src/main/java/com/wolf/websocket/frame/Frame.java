package com.wolf.websocket.frame;

import com.wolf.websocket.exceptions.InvalidFrameException;
import java.nio.ByteBuffer;

public interface Frame {

    public enum Opcode {

        TEXT, CLOSING
        // more to come
    }

    public boolean isFin();

    public Opcode getOpcode();

    public ByteBuffer getPayloadData();// TODO the separation of the application data and the extension data is yet to be done

    public abstract void append(Frame nextframe) throws InvalidFrameException;
}
