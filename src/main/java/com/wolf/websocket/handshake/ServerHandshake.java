package com.wolf.websocket.handshake;

public interface ServerHandshake extends Handshakedata {

    public short getHttpStatus();

    public String getHttpStatusMessage();
    
    public void setHttpStatus(short status);

    public void setHttpStatusMessage(String message);
}
