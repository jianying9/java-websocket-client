package com.wolf.websocket;

import com.wolf.websocket.drafts.Draft;
import com.wolf.websocket.drafts.Draft17Impl;
import com.wolf.websocket.handshake.ClientHandshake;
import com.wolf.websocket.handshake.ServerHandshake;
import com.wolf.websocket.message.Message;
import com.wolf.websocket.message.MessageImpl;
import com.wolf.websocket.message.OpCode;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author aladdin
 */
public class WebSocketImpl implements WebSocket {

    public SocketChannel channel;
    private final BlockingQueue<String> sendMessageQueue;
    private final BlockingQueue<Message> receiveMessageQueue;
    private volatile ReadyStateEnum readystate = ReadyStateEnum.NOT_YET_CONNECTED;
    private final WebSocketListener webSocketListener;
    private final Draft draft = new Draft17Impl();
    private final URI uri;
    private volatile Thread sendThread = null;
    private volatile Thread receiveThread = null;
    private volatile Thread channelThread = null;

    public WebSocketImpl(WebSocketListener webSocketListener, String serverUrl) {
        this.webSocketListener = webSocketListener;
        this.uri = URI.create(serverUrl);
        this.sendMessageQueue = new LinkedBlockingQueue<String>(50);
        this.receiveMessageQueue = new LinkedBlockingQueue<Message>(50);
    }

    @Override
    public synchronized void start() {
        if (this.sendThread == null) {
            this.sendThread = new Thread(new WebSocketSendThread(this), "webSocketSendThread");
            this.sendThread.start();
        }
        if (this.receiveThread == null) {
            this.receiveThread = new Thread(new WebSocketReceiveThread(this), "webSocketReceiveThread");
            this.receiveThread.start();
        }
    }

    @Override
    public synchronized void close() {
        if (readystate != ReadyStateEnum.CLOSING || readystate != ReadyStateEnum.CLOSED) {
            this.readystate = ReadyStateEnum.CLOSING;
            try {
                this.channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            this.readystate = ReadyStateEnum.CLOSED;
        }
    }

    @Override
    public void send(String message) {
        try {
            this.sendMessageQueue.put(message);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void connect() {
        ClientHandshake clientHandshake = this.draft.createClientHandshake(this.uri);
        ByteBuffer clientHandshakeByteBuffer = this.draft.createClientHandshakeByteBuffer(clientHandshake);
        ByteBuffer buff = ByteBuffer.allocate(WebSocket.RCV_BUF_SIZE);
        try {
            this.channel = SelectorProvider.provider().openSocketChannel();
            this.channel.configureBlocking(true);
            int port = this.uri.getPort();
            if (port == -1) {
                port = WebSocket.DEFAULT_PORT;
            }
            this.channel.connect(new InetSocketAddress(this.uri.getHost(), port));
            this.readystate = ReadyStateEnum.CONNECTING;
            this.channel.write(clientHandshakeByteBuffer);
            int read;
            boolean result;
            ServerHandshake serverHandshake;
            while (this.channel.isOpen()) {
                buff.clear();
                read = this.channel.read(buff);
                buff.flip();
                if (read > 0) {
                    serverHandshake = this.draft.parseServerHandshake(buff);
                    result = this.draft.validate(clientHandshake, serverHandshake);
                    if (result) {
                        this.readystate = ReadyStateEnum.OPEN;
                        if (this.channelThread == null) {
                            this.channelThread = new Thread(new WebSocketIOThread(this), "webSocketIOThread");
                            this.channelThread.start();
                        }
                        break;
                    } else {
                        throw new RuntimeException("connect handshake error");
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean isConnecting() {
        return this.readystate == ReadyStateEnum.CONNECTING;
    }

    @Override
    public boolean isOpen() {
        return this.readystate == ReadyStateEnum.OPEN;
    }

    @Override
    public boolean isClosing() {
        return this.readystate == ReadyStateEnum.CLOSING;
    }

    @Override
    public boolean isClosed() {
        return this.readystate == ReadyStateEnum.CLOSED;
    }

    private final class WebSocketSendThread implements Runnable {

        private final WebSocketImpl webSocket;

        public WebSocketSendThread(WebSocketImpl webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void run() {
            String message;
            ByteBuffer buf;
            Message mes;
            try {
                while (Thread.interrupted() == false) {
                    message = this.webSocket.sendMessageQueue.take();
                    mes = new MessageImpl(OpCode.TEXT, message);
                    buf = this.webSocket.draft.createFrame(mes);
                    synchronized (this.webSocket) {
                        if (this.webSocket.readystate != ReadyStateEnum.OPEN) {
                            this.webSocket.connect();
                        }
                    }
                    while (buf.hasRemaining()) {
                        this.webSocket.channel.write(buf);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                this.webSocket.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
                this.webSocket.sendThread = null;
            }
        }
    }

    private final class WebSocketReceiveThread implements Runnable {

        private final WebSocketImpl webSocket;

        public WebSocketReceiveThread(WebSocketImpl webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void run() {
            Message message;
            try {
                while (Thread.interrupted() == false) {
                    message = this.webSocket.receiveMessageQueue.take();
                    if (message.getOpCode() == OpCode.CLOSING) {
                        this.webSocket.close();
                    } else {
                        this.webSocket.webSocketListener.onMessage(message.getUTF8Data());
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                this.webSocket.receiveThread = null;
            }
        }
    }

    private final class WebSocketIOThread implements Runnable {

        private final WebSocketImpl webSocket;

        public WebSocketIOThread(WebSocketImpl webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void run() {
            ByteBuffer buff = ByteBuffer.allocate(WebSocket.RCV_BUF_SIZE);
            int read;
            List<Message> serverMessageList;
            try {
                while (Thread.interrupted() == false && this.webSocket.channel.isOpen()) {
                    buff.clear();
                    read = this.webSocket.channel.read(buff);
                    buff.flip();
                    if (read > 0) {
                        serverMessageList = this.webSocket.draft.parseFrame(buff);
                        if (serverMessageList.isEmpty() == false) {
                            for (Message message : serverMessageList) {
                                this.webSocket.receiveMessageQueue.put(message);
                                
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                this.webSocket.close();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
