package com.wolf.websocket.frame;

import com.wolf.websocket.exceptions.InvalidDataException;
import java.nio.ByteBuffer;

public interface FrameData extends Frame {

    public void setFin(boolean fin);

    public void setOptcode(Opcode optcode);

    public void setPayload(ByteBuffer payload) throws InvalidDataException;
}