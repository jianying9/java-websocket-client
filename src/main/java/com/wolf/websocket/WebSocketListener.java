package com.wolf.websocket;

/**
 * Implemented by <tt>WebSocketClient</tt> and <tt>WebSocketServer</tt>. The
 * methods within are called by <tt>WebSocket</tt>. Almost every method takes a
 * first parameter conn which represents the source of the respective event.
 */
public interface WebSocketListener {

    /**
     * Called when an entire text frame has been received. Do whatever you want
     * here...
     *
     * @param conn The <tt>WebSocket</tt> instance this event is occurring on.
     * @param message The UTF-8 decoded message that was received.
     */
    public void onMessage(String message);



    /**
     * Called after <var>onHandshakeReceived</var> returns <var>true</var>.
     * Indicates that a complete WebSocket connection has been established, and
     * we are ready to send/receive data.
     *
     * @param conn The <tt>WebSocket</tt> instance this event is occuring on.
     */
    public void onOpen();
    
    

    /**
     * Called after <tt>WebSocket#close</tt> is explicity called, or when the
     * other end of the WebSocket connection is closed.
     *
     * @param conn The <tt>WebSocket</tt> instance this event is occuring on.
     */
    public void onClose();


    /**
     * Called if an exception worth noting occurred. If an error causes the
     * connection to fail onClose will be called additionally afterwards.
     *
     * @param ex The exception that occurred. <br> Might be null if the
     * exception is not related to any specific connection. For example if the
     * server port could not be bound.
     */
    public void onError(Exception ex);
}
