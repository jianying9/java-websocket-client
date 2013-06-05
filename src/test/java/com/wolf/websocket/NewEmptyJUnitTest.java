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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author aladdin
 */
public class NewEmptyJUnitTest {

    public NewEmptyJUnitTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //

    @Test
    public void hello() {
        Draft draft = new Draft17Impl();
        String serverlocation = "ws://192.168.64.50:8080/test-server/socket.io";
        URI uri = URI.create(serverlocation);
        ClientHandshake clientHandshake = draft.createClientHandshake(uri);
        ByteBuffer clientHandshakeByteBuffer = draft.createClientHandshakeByteBuffer(clientHandshake);
        ByteBuffer buff = ByteBuffer.allocate(WebSocket.RCV_BUF_SIZE);
        SocketChannel channel;
        try {
            channel = SelectorProvider.provider().openSocketChannel();
            channel.configureBlocking(true);
            int port = uri.getPort();
            if (port == -1) {
                port = WebSocket.DEFAULT_PORT;
            }
            channel.connect(new InetSocketAddress(uri.getHost(), port));
            channel.write(clientHandshakeByteBuffer);
            int read;
            boolean result;
            ServerHandshake serverHandshake;
            while (channel.isOpen()) {
                buff.clear();
                read = channel.read(buff);
                buff.flip();
                if (read > 0) {
                    serverHandshake = draft.parseServerHandshake(buff);
                    result = draft.validate(clientHandshake, serverHandshake);
                    if (result) {
                        break;
                    } else {
                        throw new RuntimeException("connect handshake error");
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //
        String message = "{\"act\":\"LOGIN\"}";
        Message clentMessage = new MessageImpl(OpCode.TEXT, message);
        ByteBuffer clientBuff = draft.createFrame(clentMessage);
        List<Message> serverMessageList = null;
        try {
            channel.write(clientBuff);
            int read;
            while (channel.isOpen()) {
                buff.clear();
                read = channel.read(buff);
                buff.flip();
                if (read > 0) {
                    serverMessageList = draft.parseFrame(buff);
                    break;
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if(serverMessageList != null) {
            for (Message message1 : serverMessageList) {
                System.out.println(message1.getOpCode().name());
                System.out.println(message1.getUTF8Data());
            }
        }
    }
}
