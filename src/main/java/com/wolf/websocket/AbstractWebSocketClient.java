package com.wolf.websocket;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author aladdin
 */
public abstract class AbstractWebSocketClient implements WebSocketClient {

    private final BlockingQueue<String> sendMessageQueue;
    private final String serverUrl;
    private Thread sendThread = null;

    public AbstractWebSocketClient(String serverUrl) {
        this.sendMessageQueue = new LinkedBlockingQueue<String>(50);
        this.serverUrl = serverUrl;
    }

    @Override
    public void send(String message) {
        synchronized (this) {
            if (sendThread == null || sendThread.isInterrupted()) {
                sendThread = new Thread(new WebSocketSendThread(this));
                sendThread.start();
            }
        }
        try {
            this.sendMessageQueue.put(message);
        } catch (InterruptedException ex) {
            this.onError(ex);
        }
    }

    private final class WebSocketSendThread implements Runnable {

        private final WebSocketClientListener listener;
        private WebSocket webSocket = null;

        public WebSocketSendThread(WebSocketClientListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            String message;
            try {
                while (Thread.interrupted() == false) {
                    message = sendMessageQueue.take();
                    System.out.println(Thread.currentThread().getName() + " ready send:" + message);
                    if (this.webSocket == null) {
                        System.out.println(Thread.currentThread().getName() + " one check web socket null......");
                    }
                    if (this.webSocket != null && this.webSocket.isOpen()) {
                        System.out.println(Thread.currentThread().getName() + " send:" + message);
                        this.webSocket.send(message);
                    } else {
                        System.out.println(Thread.currentThread().getName() + " connect......" + message);
                        this.webSocket = new AbstractWebSocket(serverUrl, message) {
                            @Override
                            public void onOpen() {
                                System.out.println(Thread.currentThread().getName() + " connect success!!!");
                            }

                            @Override
                            public void onMessage(String message) {
                                listener.onMessage(message);
                            }

                            @Override
                            public void onClose() {
                                System.out.println(Thread.currentThread().getName() + " closing");
                                webSocket = null;
                            }

                            @Override
                            public void onError(Exception ex) {
                                listener.onError(ex);
                            }
                        };
                        this.webSocket.connect();
                        if (this.webSocket == null) {
                            System.out.println(Thread.currentThread().getName() + " two check web socket null......");
                        }
                    }
                }
            } catch (InterruptedException e) {
                this.webSocket.onError(e);
            } finally {
                sendThread = null;
            }
        }
    }
}
