package com.wolf.websocket;

import com.wolf.websocket.client.WebSocketClient;
import com.wolf.websocket.logger.Log;
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
        Log.LOG.debug("TestWebSocketClient: on open");
    }

    @Override
    public void onMessage(String message) {
    }

    @Override
    public void onError(Exception ex) {
    }

    public void onClose() {
        Log.LOG.debug("TestWebSocketClient: on close");
    }
}
