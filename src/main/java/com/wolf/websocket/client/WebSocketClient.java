package com.wolf.websocket.client;

import com.wolf.websocket.WebSocket;
import com.wolf.websocket.WebSocketImpl;
import com.wolf.websocket.WebSocketListener;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;

/**
 * The <tt>WebSocketClient</tt> is an abstract class that expects a valid
 * "ws://" URI to connect to. When connected, an instance recieves important
 * events related to the life of the connection. A subclass must implement
 * <var>onOpen</var>, <var>onClose</var>, and <var>onMessage</var> to be useful.
 * An instance can send messages to it's connected server via the
 * <var>send</var> method.
 *
 * @author Nathan Rajlich
 */
public abstract class WebSocketClient implements WebSocketListener {

    /**
     * The URI this channel is supposed to connect to.
     */
    private WebSocket webSocket = null;

    public WebSocketClient(URI serverUri) {
        if (serverUri == null) {
            throw new IllegalArgumentException();
        }
        String path;
        String part1 = serverUri.getPath();
        String part2 = serverUri.getQuery();
        if (part1 == null || part1.length() == 0) {
            path = "/";
        } else {
            path = part1;
        }
        if (part2 != null) {
            path += "?" + part2;
        }
        int port = serverUri.getPort();
        if (port == -1) {
            port = WebSocket.DEFAULT_PORT;
        }
        webSocket = new WebSocketImpl(serverUri.getHost(), port, path, this);
    }

    /**
     * Same as connect but blocks until the websocket connected or failed to do
     * so.<br> Returns whether it succeeded or not.
     *
     */
    /**
     * Sends <var>text</var> to the connected WebSocket server.
     *
     * @param text The String to send to the WebSocket server.
     */
    public void send(String text) throws NotYetConnectedException {
        synchronized (this) {
            if (this.webSocket.isOpen() == false) {
                this.webSocket.connect();
            }
        }
        this.webSocket.send(text);
    }
}
