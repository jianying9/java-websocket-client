package com.wolf.websocket;

import java.util.logging.Level;
import java.util.logging.Logger;
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
        TestWebSocketClient client = new TestWebSocketClient("ws://192.168.64.50:8080/test-server/socket.io");
        client.login("", "");
        int i = 0;
        while (i < 1000) {
            System.out.println(i + "---------------------------");
            client.send("{\"act\":\"GET_TIME\",\"num\":\"" + i + "\"}");
            i++;
        }
        try {
            Thread.sleep(500000);
        } catch (InterruptedException ex) {
        }
    }
}
