package com.wolf.websocket;

/**
 *
 * @author aladdin
 */
public class TestWebSocketClient {

    private final WebSocket webSocket;
    private final WebSocketListener listener = new WebSocketListener() {
        public void onMessage(String message) {
            System.out.println(message);
        }

        public void onClose() {
            System.out.println("socket close");
        }

        public void onError(Exception ex) {
        }
    };

    public TestWebSocketClient(String url) {
        this.webSocket = new WebSocketImpl(this.listener, url);
        this.webSocket.start();
    }
    
    public void send(String message) {
        this.webSocket.send(message);
    }
    
    public void login(String userName, String password) {
        this.webSocket.send("{\"act\":\"LOGIN\"}");
    }
}
