package com.wolf.websocket.exceptions;

import com.wolf.websocket.frame.CloseFrame;

public class LimitExedeedException extends InvalidDataException {

    private static final long serialVersionUID = 2672539593901783790L;

    /**
     * Serializable
     */
    public LimitExedeedException() {
        super(CloseFrame.TOOBIG);
    }

    public LimitExedeedException(String s) {
        super(CloseFrame.TOOBIG, s);
    }
}
