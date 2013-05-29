package com.wolf.websocket;

import com.wolf.websocket.frame.Frame;
import java.nio.channels.NotYetConnectedException;

public interface WebSocket {

    public enum READYSTATE {

        NOT_YET_CONNECTED, CONNECTING, OPEN, CLOSING, CLOSED;
    }
    /**
     * The default port of WebSockets, as defined in the spec. If the nullary
     * constructor is used, DEFAULT_PORT will be the port the WebSocketServer is
     * binded to. Note that ports under 1024 usually require root permissions.
     */
    public static final int DEFAULT_PORT = 80;
    

    /**
     * Convenience function which behaves like close(CloseFrame.NORMAL)
     */
    public void close();

    /**
     * Send Text data to the other end.
     *
     * @throws IllegalArgumentException
     * @throws NotYetConnectedException
     */
    public void send(String text);

    /**
     * Send Binary data (plain bytes) to the other end.
     *
     * @throws IllegalArgumentException
     * @throws NotYetConnectedException
     */
    public void connect();

    public boolean isConnecting();

    public boolean isOpen();

    public boolean isClosing();

    /**
     * Returns whether the close handshake has been completed and the socket is
     * closed.
     */
    public boolean isClosed();
}