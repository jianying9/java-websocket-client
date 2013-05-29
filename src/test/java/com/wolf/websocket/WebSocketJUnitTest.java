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

//    @Test
    public void test() throws InterruptedException, IOException {
        int time = 100;
        String serverlocation = "ws://192.168.64.50:8080/test-server/socket.io";
        URI uri = URI.create(serverlocation);
        TestWebSocketClient client = new TestWebSocketClient(uri);
        System.out.println("1--------------------");
        client.send("{\"act\":\"GET_TIME\"}");
        Thread.sleep(time);
        System.out.println("2--------------------");
        client.send("{\"act\":\"GET_TIME\"}");
        Thread.sleep(time);
        System.out.println("3----------------------");
        client.send("{\"act\":\"LOGIN\"}");
        Thread.sleep(time);
        System.out.println("4------------------------");
        client.send("{\"act\":\"GET_TIME\"}");
        Thread.sleep(time);
        System.out.println("5--------------------");
        client.send("{\"act\":\"GET_TIME\"}");
        Thread.sleep(time);
        System.out.println("6--------------------");
        client.close();
        Thread.sleep(5000);
    }
    
    @Test
    public void test123() throws InterruptedException, IOException {
        Thread.currentThread().setName("mainThread");
        String serverlocation = "ws://192.168.64.50:8080/test-server/socket.io";
        URI uri = URI.create(serverlocation);
        TestWebSocketClient client = new TestWebSocketClient(uri);
        client.send("{\"act\":\"LOGIN\"}");
//        client.send("{\"act\":\"GET_TIME\"}");
        int i = 0;
        while(i < 10) {
            System.out.println(i + "---------------------------");
            client.send("{\"act\":\"GET_TIME\",\"num\":\"" + i + "\"}");
            i++;
        }
        Thread.sleep(5000);
        client.close();
    }
}
