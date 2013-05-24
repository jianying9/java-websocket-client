package com.wolf.websocket;

import com.wolf.websocket.drafts.Draft;
import com.wolf.websocket.drafts.Draft.HandshakeState;
import com.wolf.websocket.drafts.Draft_17;
import com.wolf.websocket.exceptions.InvalidDataException;
import com.wolf.websocket.frame.CloseFrame;
import com.wolf.websocket.frame.Frame;
import com.wolf.websocket.frame.Frame.Opcode;
import com.wolf.websocket.handshake.ClientHandshake;
import com.wolf.websocket.handshake.ClientHandshakeImpl;
import com.wolf.websocket.handshake.Handshakedata;
import com.wolf.websocket.handshake.ServerHandshake;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents one end (client or server) of a single WebSocketImpl connection.
 * Takes care of the "handshake" phase, then allows for easy sending of text
 * frames, and receiving frames through an event-based model.
 *
 */
public class WebSocketImpl implements WebSocket {

    public static int RCVBUF = 16384;
    public static/*final*/ boolean DEBUG = false; // must be final in the future in order to take advantage of VM optimization
    /**
     * the possibly wrapped channel object whose selection is controlled by
     * {@link #key}
     */
    public final SocketChannel channel;
    /**
     * Queue of buffers that need to be sent to the client.
     */
    public final BlockingQueue<ByteBuffer> outQueue;
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
    private Thread writeThread;
    private Thread readThread;
    /**
     * stores the handshake sent by this websocket ( Role.CLIENT only )
     */
    private String closemessage = null;
    private Integer closecode = null;
    private Boolean closedremotely = null;

    /**
     * crates a websocket with client role
     *
     * @param socket may be unbound
     */
    public WebSocketImpl(String ip, int port, String path, WebSocketListener listener) {
        this.outQueue = new LinkedBlockingQueue<ByteBuffer>();
        this.wsl = listener;
        this.ip = ip;
        this.port = port;
        this.path = path;
        try {
            this.channel = SelectorProvider.provider().openSocketChannel();
            this.channel.configureBlocking(true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("SocketChannel init error");
        }
    }

    /**
     *
     */
    public void decode(ByteBuffer socketBuffer) {
        if (socketBuffer.hasRemaining() == false) {
            return;
        }
        if (DEBUG) {
            System.out.println("process(" + socketBuffer.remaining() + "): {" + new String(socketBuffer.array(), socketBuffer.position(), socketBuffer.remaining()) + "}");
        }
        if (readystate == READYSTATE.OPEN) {
            decodeFrames(socketBuffer);
        }
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
                if (DEBUG) {
                    System.out.println("matched frame: " + f);
                }
                Opcode curop = f.getOpcode();
                boolean fin = f.isFin();

                if (curop == Opcode.CLOSING) {
                    int code = CloseFrame.NOCODE;
                    String reason = "";
                    if (f instanceof CloseFrame) {
                        CloseFrame cf = (CloseFrame) f;
                        code = cf.getCloseCode();
                        reason = cf.getMessage();
                    }
                    if (readystate == READYSTATE.CLOSING) {
                        // complete the close handshake by disconnecting
                        closeConnection(code, reason, true);
                    } else {
                        // echo close handshake
                        flushAndClose(code, reason, false);
                    }
                    continue;
                } else if (curop == Opcode.TEXT) {
                } else {
                    throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR, "non control or continious frame expected");
                }
            }
        } catch (InvalidDataException e1) {
            close(e1);
        }
    }

    private void close(int code, String message, boolean remote) {
        if (readystate != READYSTATE.CLOSING && readystate != READYSTATE.CLOSED) {
            if (readystate == READYSTATE.OPEN) {
                if (code == CloseFrame.ABNORMAL_CLOSE) {
                    assert (remote == false);
                    readystate = READYSTATE.CLOSING;
                    flushAndClose(code, message, false);
                    return;
                }
                flushAndClose(code, message, remote);
            } else {
                flushAndClose(CloseFrame.NEVER_CONNECTED, message, false);
            }
            if (code == CloseFrame.PROTOCOL_ERROR)// this endpoint found a PROTOCOL_ERROR
            {
                flushAndClose(code, message, remote);
            }
            readystate = READYSTATE.CLOSING;
        }
    }

    protected synchronized void flushAndClose(int code, String message, boolean remote) {
        closecode = code;
        closemessage = message;
        closedremotely = remote;
        try {
            wsl.onClose();
        } catch (RuntimeException e) {
            wsl.onError(e);
        }
        if (draft != null) {
            draft.reset();
        }
    }

    /**
     *
     * @param remote Indicates who "generated" <code>code</code>.<br>
     * <code>true</code> means that this endpoint received the <code>code</code>
     * from the other endpoint.<br> false means this endpoint decided to send
     * the given code,<br> <code>remote</code> may also be true if this endpoint
     * started the closing handshake since the other endpoint may not simply
     * echo the <code>code</code> but close the connection the same time this
     * endpoint does do but with an other <code>code</code>. <br>
     *
     */
    protected synchronized void closeConnection(int code, String message, boolean remote) {
        if (readystate == READYSTATE.CLOSED) {
            return;
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                wsl.onError(e);
            }
        }
        try {
            this.wsl.onClose();
        } catch (RuntimeException e) {
            wsl.onError(e);
        }
        if (draft != null) {
            draft.reset();
        }

        readystate = READYSTATE.CLOSED;
        this.outQueue.clear();
    }

    protected void closeConnection(int code, boolean remote) {
        closeConnection(code, "", remote);
    }

    public void closeConnection() {
        if (closedremotely == null) {
            throw new IllegalStateException("this method must be used in conjuction with flushAndClose");
        }
        closeConnection(closecode, closemessage, closedremotely);
    }

    public void closeConnection(int code, String message) {
        closeConnection(code, message, false);
    }

    public void close(InvalidDataException e) {
        close(e.getCloseCode(), e.getMessage(), false);
    }

    /**
     * Send Text data to the other end.
     *
     * @throws IllegalArgumentException
     * @throws NotYetConnectedException
     */
    @Override
    public void send(String text) {
        send(draft.createFrames(text));
    }

    /**
     * Send Binary data (plain bytes) to the other end.
     *
     * @throws IllegalArgumentException
     * @throws NotYetConnectedException
     */

    private void send(Collection<Frame> frames) {
        for (Frame f : frames) {
            sendFrame(f);
        }
    }

    @Override
    public void sendFrame(Frame framedata) {
        if (DEBUG) {
            System.out.println("send frame: " + framedata);
        }
        this.write(this.draft.createBinaryFrame(framedata));
    }

    @Override
    public boolean hasBufferedData() {
        return this.outQueue.isEmpty() == false;
    }

    private void write(ByteBuffer buf) {
        if (DEBUG) {
            System.out.println("write(" + buf.remaining() + "): {" + (buf.remaining() > 1000 ? "too big to display" : new String(buf.array())) + "}");
        }
        this.outQueue.add(buf);
    }

    private void write(List<ByteBuffer> bufs) {
        for (ByteBuffer b : bufs) {
            this.write(b);
        }
    }

    private void open(Handshakedata d) {
        if (DEBUG) {
            System.out.println("open using draft: " + draft.getClass().getSimpleName());
        }
        this.readystate = READYSTATE.OPEN;
        try {
            wsl.onOpen();
        } catch (RuntimeException e) {
            wsl.onError(e);
        }
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
    public String toString() {
        return super.toString(); // its nice to be able to set breakpoints here
    }

    @Override
    public void close() {
        close(CloseFrame.NORMAL, "", false);
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
            this.channel.connect(new InetSocketAddress(this.ip, this.port));
            this.readystate = READYSTATE.CONNECTING;
            this.channel.write(byteBuffer);
            while (this.channel.isOpen()) {
                if (SocketChannelIOHelper.read(buff, this)) {
                    boolean result = this.decodeHandshake(handshakerequest, buff);
                    if(result) {
                        this.open(handshake);
                        break;
                    } else {
                        this.close();
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("socket connect error");
        }
    }

    private final class WebsocketWriteThread implements Runnable {

        private final WebSocketImpl webSocket;

        public WebsocketWriteThread(WebSocketImpl webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void run() {
            try {
                while (Thread.interrupted() == false) {
                    SocketChannelIOHelper.writeBlocking(this.webSocket);
                }
            } catch (IOException e) {
                this.webSocket.close();
            } catch (InterruptedException e) {
            }
        }
    }

    private final class WebsocketReadThread implements Runnable {

        private final WebSocketImpl webSocket;

        public WebsocketReadThread(WebSocketImpl webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void run() {
            ByteBuffer buff = ByteBuffer.allocate(WebSocketImpl.RCVBUF);
            try {
                while (this.webSocket.channel.isOpen()) {
                    if (SocketChannelIOHelper.read(buff, this.webSocket)) {
                        this.webSocket.decode(buff);
                    } else {
                        this.webSocket.close();
                    }
                }
            } catch (IOException e) {
                this.webSocket.close();
            }
        }
    }
}
