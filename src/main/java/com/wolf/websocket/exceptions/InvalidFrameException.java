package com.wolf.websocket.exceptions;

import com.wolf.websocket.frame.CloseFrame;

public class InvalidFrameException extends InvalidDataException {

    private static final long serialVersionUID = -5648511796732633365L;

    /**
     * Serializable
     */
    public InvalidFrameException() {
        super(CloseFrame.PROTOCOL_ERROR);
    }

    public InvalidFrameException(String arg0) {
        super(CloseFrame.PROTOCOL_ERROR, arg0);
    }

    public InvalidFrameException(Throwable arg0) {
        super(CloseFrame.PROTOCOL_ERROR, arg0);
    }

    public InvalidFrameException(String arg0, Throwable arg1) {
        super(CloseFrame.PROTOCOL_ERROR, arg0, arg1);
    }
}
