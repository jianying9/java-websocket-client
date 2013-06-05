package com.wolf.websocket;

public interface WebSocketListener {
    
    public void onMessage(String message);

    public void onClose();

    public void onError(Exception ex);
}
