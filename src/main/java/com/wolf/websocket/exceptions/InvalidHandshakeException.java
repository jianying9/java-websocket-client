package com.wolf.websocket.exceptions;

import com.wolf.websocket.frame.CloseFrame;

public class InvalidHandshakeException extends InvalidDataException {

    private static final long serialVersionUID = 2537000715728532675L;

    /**
     * Serializable
     */
    public InvalidHandshakeException() {
        super(CloseFrame.PROTOCOL_ERROR);
    }

    public InvalidHandshakeException(String arg0, Throwable arg1) {
        super(CloseFrame.PROTOCOL_ERROR, arg0, arg1);
    }

    public InvalidHandshakeException(String arg0) {
        super(CloseFrame.PROTOCOL_ERROR, arg0);
    }

    public InvalidHandshakeException(Throwable arg0) {
        super(CloseFrame.PROTOCOL_ERROR, arg0);
    }
}
