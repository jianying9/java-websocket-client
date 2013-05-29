package com.wolf.websocket;

import com.wolf.websocket.drafts.Draft;
import com.wolf.websocket.drafts.Draft.HandshakeState;
import com.wolf.websocket.drafts.Draft_17;
import com.wolf.websocket.exceptions.InvalidDataException;
import com.wolf.websocket.frame.Frame;
import com.wolf.websocket.frame.Frame.Opcode;
import com.wolf.websocket.handshake.ClientHandshake;
import com.wolf.websocket.handshake.ClientHandshakeImpl;
import com.wolf.websocket.handshake.ServerHandshake;
import com.wolf.websocket.logger.Log;
import com.wolf.websocket.util.Charsetfunctions;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents one end (client or server) of a single WebSocketImpl connection.
 * Takes care of the "handshake" phase, then allows for easy sending of text
 * frames, and receiving frames through an event-based model.
 *
 */
public class WebSocketImpl implements WebSocket {

    public static int RCVBUF = 16384;
    /**
     * the possibly wrapped channel object whose selection is controlled by
     * {@link #key}
     */
    public SocketChannel channel;
    /**
     * Queue of buffers that need to be sent to the client.
     */
    private final BlockingQueue<String> sendMessageQueue;
    /**
     * When true no further frames may be submitted to be sent
     */
    private volatile READYSTATE readystate = READYSTATE.NOT_YET_CONNECTED;
    /**
     * The listener to notify of WebSocket events.
     */
    private final WebSocketListener wsl;
    private final Draft draft = new Draft_17();
    private final String ip;
    private final int port;
    private final String path;
    private volatile Thread writeThread;

    /**
     * crates a websocket with client role
     *
     * @param socket may be unbound
     */
    public WebSocketImpl(String ip, int port, String path, WebSocketListener listener) {
        this.sendMessageQueue = new LinkedBlockingQueue<String>(50);
        this.wsl = listener;
        this.ip = ip;
        this.port = port;
        this.path = path;
        //
        this.writeThread = new Thread(new WebsocketWriteThread(this));
        this.writeThread.start();
    }

    /**
     *
     */
    public void decode(ByteBuffer socketBuffer) {
        if (socketBuffer.hasRemaining() == false) {
            return;
        }
        String text = "process(" + socketBuffer.remaining() + "): {" + new String(socketBuffer.array(), socketBuffer.position(), socketBuffer.remaining()) + "}";
        Log.LOG.debug(text);
        this.decodeFrames(socketBuffer);
    }

    /**
     * Returns whether the handshake phase has is completed. In case of a broken
     * handshake this will be never the case.
     *
     */
    private boolean decodeHandshake(ClientHandshake request, ByteBuffer socketBufferNew) {
        ServerHandshake response = draft.translateHandshake(socketBufferNew);
        HandshakeState handshakestate = draft.acceptHandshakeAsClient(request, response);
        return handshakestate == HandshakeState.MATCHED;
    }

    private void decodeFrames(ByteBuffer socketBuffer) {
        List<Frame> frames;
        try {
            frames = draft.translateFrame(socketBuffer);
            for (Frame f : frames) {
                Opcode curop = f.getOpcode();
                switch (curop) {
                    case CLOSING:
                        this.close();
                        break;
                    case TEXT:
                        this.message(Charsetfunctions.stringUtf8(f.getPayloadData()));
                        break;
                }
            }
        } catch (InvalidDataException e1) {
            throw new RuntimeException(e1);
        }
    }

    /**
     * Send Text data to the other end.
     *
     * @throws IllegalArgumentException
     * @throws NotYetConnectedException
     */
    @Override
    public void send(String text) {
        if (this.sendMessageQueue.size() < 20) {
            this.sendMessageQueue.add(text);
        } else {
            this.wsl.onMessage("{\"flag\":\"BUSY\"}");
        }
    }

    private void open() {
        this.readystate = READYSTATE.OPEN;
        this.wsl.onOpen();
    }

    private void message(String message) {
        Log.LOG.debug("on message:" + message);
        this.wsl.onMessage(message);
    }

    @Override
    public boolean isConnecting() {
        return readystate == READYSTATE.CONNECTING; // ifflushandclosestate
    }

    @Override
    public boolean isOpen() {
        return readystate == READYSTATE.OPEN;
    }

    @Override
    public boolean isClosing() {
        return readystate == READYSTATE.CLOSING;
    }

    @Override
    public boolean isClosed() {
        return readystate == READYSTATE.CLOSED;
    }

    @Override
    public synchronized void close() {
        if (readystate != READYSTATE.CLOSING || readystate != READYSTATE.CLOSED) {
            this.readystate = READYSTATE.CLOSING;
            try {
                this.channel.close();
            } catch (IOException ex) {
            }
            this.readystate = READYSTATE.CLOSED;
        }
    }

    @Override
    public void connect() {
        String host = this.ip + (port != WebSocket.DEFAULT_PORT ? ":" + port : "");
        ClientHandshakeImpl handshake = new ClientHandshakeImpl();
        handshake.setResourceDescriptor(path);
        handshake.put("Host", host);
        ClientHandshake handshakerequest = draft.postProcessHandshakeRequestAsClient(handshake);
        ByteBuffer byteBuffer = this.draft.createHandshake(handshakerequest);
        ByteBuffer buff = ByteBuffer.allocate(WebSocketImpl.RCVBUF);
        try {
            this.channel = SelectorProvider.provider().openSocketChannel();
            this.channel.configureBlocking(true);
            this.channel.connect(new InetSocketAddress(this.ip, this.port));
            this.readystate = READYSTATE.CONNECTING;
            this.channel.write(byteBuffer);
            int read;
            boolean result;
            while (this.channel.isOpen()) {
                buff.clear();
                read = this.channel.read(buff);
                buff.flip();
                if (read > 0) {
                    result = this.decodeHandshake(handshakerequest, buff);
                    if (result) {
                        this.open();
                        WebsocketReadThread websocketReadThread = new WebsocketReadThread(this);
                        Thread readThread = new Thread(websocketReadThread);
                        readThread.start();
                        websocketReadThread.connectLatch.await();
                        break;
                    } else {
                        throw new RuntimeException("connect handshake error");
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final class WebsocketWriteThread implements Runnable {

        private final WebSocketImpl webSocket;

        public WebsocketWriteThread(WebSocketImpl webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("writeThread");
            Log.LOG.debug("start....");
            String text;
            ByteBuffer buf;
            Frame frame;
            try {
                while (Thread.interrupted() == false) {
                    text = this.webSocket.sendMessageQueue.take();
                    Log.LOG.debug("send message：" + text);
                    frame = this.webSocket.draft.createFrames(text);
                    buf = this.webSocket.draft.createBinaryFrame(frame);
                    synchronized (this.webSocket) {
                        Log.LOG.debug("assert connect...");
                        if (this.webSocket.readystate != READYSTATE.OPEN) {
                            Log.LOG.debug("connect...");
                            this.webSocket.connect();
                        }
                    }
                    Log.LOG.debug("write..........");
                    while (buf.hasRemaining()) {
                        this.webSocket.channel.write(buf);
                    }
                }
            } catch (IOException e) {
                this.webSocket.close();
            } catch (InterruptedException e) {
            }
        }
    }

    private final class WebsocketReadThread implements Runnable {

        private final WebSocketImpl webSocket;
        private final CountDownLatch connectLatch = new CountDownLatch(1);

        public WebsocketReadThread(WebSocketImpl webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("readThread");
            Log.LOG.debug("start....");
            this.connectLatch.countDown();
            ByteBuffer buff = ByteBuffer.allocate(WebSocketImpl.RCVBUF);
            int read;
            try {
                while (this.webSocket.readystate == READYSTATE.OPEN) {
                    buff.clear();
                    read = this.webSocket.channel.read(buff);
                    buff.flip();
                    Log.LOG.debug("read message...." + read);
                    if (read > 0) {
                        this.webSocket.decode(buff);
                    }
                }
            } catch (IOException e) {
            } finally {
                this.webSocket.draft.reset();
            }
        }
    }
}
