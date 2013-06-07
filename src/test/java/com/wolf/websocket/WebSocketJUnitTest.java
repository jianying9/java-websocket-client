package com.wolf.websocket;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author aladdin
 */
public class WebSocketJUnitTest {

    public WebSocketJUnitTest() {
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

    @Test
    public void hello() {
        WebSocketClient client = new AbstractWebSocketClient("ws://192.168.64.50:8080/test-server/socket.io") {
            public void onMessage(String message) {
                System.out.println(Thread.currentThread().getName() + " " + message);
            }

            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
        client.send("{\"act\":\"LOGIN\"}");
        int i = 0;
        while (i < 1000) {
            client.send("{\"act\":\"GET_TIME\",\"num\":\"" + i + "\"}");
            i++;
        }
//        client.close();
        try {
            Thread.sleep(500000);
        } catch (InterruptedException ex) {
        }
    }
}
