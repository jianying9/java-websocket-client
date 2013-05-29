package com.wolf.websocket.logger;

/**
 *
 * @author aladdin
 */
public class Log {

    public static final Log LOG = new Log();
    private boolean debug = false;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void debug(String text) {
        String tName = Thread.currentThread().getName();
        System.out.println(tName + ":" + text);
    }
}
