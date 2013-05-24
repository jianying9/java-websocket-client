package com.wolf.websocket;

import com.wolf.websocket.client.WebSocketClient;
import java.net.URI;

/**
 *
 * @author aladdin
 */
public class TestWebSocketClient extends WebSocketClient {

    public TestWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen() {
        System.out.println("TestWebSocketClient: on open");
    }

    @Override
    public void onMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void onError(Exception ex) {
    }

    public void onClose() {
        System.out.println("TestWebSocketClient: on close");
    }
}
