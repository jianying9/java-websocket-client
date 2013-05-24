package com.wolf.websocket.handshake;

public class ServerHandshakeImpl extends HandshakedataImpl implements ServerHandshake {

    private short httpstatus;
    private String httpstatusmessage;

    public ServerHandshakeImpl() {
    }

    @Override
    public String getHttpStatusMessage() {
        return httpstatusmessage;
    }

    @Override
    public short getHttpStatus() {
        return httpstatus;
    }

    public void setHttpStatusMessage(String message) {
        this.httpstatusmessage = message;
    }

    public void setHttpStatus(short status) {
        httpstatus = status;
    }
}
