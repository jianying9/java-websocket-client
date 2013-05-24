package com.wolf.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;

public class SocketChannelIOHelper {

    public static boolean read(final ByteBuffer buf, WebSocketImpl ws) throws IOException {
        buf.clear();
        int read = ws.channel.read(buf);
        buf.flip();
        return read > 0;
    }

    public static void writeBlocking(WebSocketImpl ws) throws InterruptedException, IOException {
        assert (ws.channel instanceof AbstractSelectableChannel == true ? ((AbstractSelectableChannel) ws.channel).isBlocking() : true);
        ByteBuffer buf = ws.outQueue.take();
        while (buf.hasRemaining()) {
            ws.channel.write(buf);
        }
    }
}
