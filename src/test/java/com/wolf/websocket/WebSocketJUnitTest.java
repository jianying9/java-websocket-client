package com.wolf.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
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
    //

    @Test
    public void test() throws InterruptedException, IOException {
        WebSocketImpl.DEBUG = true;
        String serverlocation = "ws://192.168.19.219:5880/search-server/socket.io";
        URI uri = URI.create(serverlocation);
        TestWebSocketClient client = new TestWebSocketClient(uri);
        client.send("{\"act\":\"INQUIRE_SEARCH_TASK\",\"pageSize\":\"10\"}");
    }
}
