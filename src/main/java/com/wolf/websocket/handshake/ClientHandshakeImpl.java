package com.wolf.websocket.handshake;

public class ClientHandshakeImpl extends HandshakedataImpl implements ClientHandshake {

    private String resourcedescriptor;

    public ClientHandshakeImpl() {
    }

    public void setResourceDescriptor(String resourcedescriptor) throws IllegalArgumentException {
        this.resourcedescriptor = resourcedescriptor;
    }

    public String getResourceDescriptor() {
        return resourcedescriptor;
    }
}
