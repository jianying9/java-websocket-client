package com.wolf.websocket;

/**
 *
 * @author aladdin
 */
public interface WebSocket {
    
    public int DEFAULT_PORT = 80;
    
    public int RCV_BUF_SIZE = 16384;
    
    public void start();
    
    public void close();
    
    public void send(String message);
    
    public void connect();
    
    public boolean isConnecting();
    
    public boolean isOpen();
    
    public boolean isClosing();
    
    public boolean isClosed();
}
