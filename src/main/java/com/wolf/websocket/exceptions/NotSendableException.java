package com.wolf.websocket.exceptions;

public class NotSendableException extends RuntimeException {

    private static final long serialVersionUID = 641494384099262913L;

    /**
     * Serializable
     */
    public NotSendableException() {
    }

    public NotSendableException(String message) {
        super(message);
    }

    public NotSendableException(Throwable cause) {
        super(cause);
    }

    public NotSendableException(String message, Throwable cause) {
        super(message, cause);
    }
}
