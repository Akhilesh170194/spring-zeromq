package com.aoneconsultancy.zeromq.core;

import com.aoneconsultancy.zeromq.config.ZmqConsumerProperties;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.support.ActiveObjectCounter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BlockingQueueConsumerIntegrationTest {

    private ZContext context;
    private BlockingQueueConsumer consumer;
    private ZMQ.Socket socket;
    private static int portCounter = 5555;
    private int currentPort;

    @BeforeEach
    public void setUp() {
        // Use a different port for each test to avoid conflicts
        currentPort = portCounter++;
        String address = "tcp://localhost:" + currentPort;

        context = new ZContext();
        ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter = new ActiveObjectCounter<>();
        consumer = new BlockingQueueConsumer(context, activeObjectCounter, ZmqConsumerProperties.builder().name("testConsumer").addresses(List.of(address)).type(SocketType.PULL).build(), 1000);

        // Simulate sending a message to the consumer
        socket = context.createSocket(SocketType.PUSH);
        socket.setLinger(0);
        socket.bind(address);
    }

    @AfterEach
    public void tearDown() {
        if (socket != null) {
            socket.setLinger(0); // Ensure socket doesn't linger
            socket.close();
            socket = null;
        }
        if (consumer != null) {
            try {
                if (consumer.isActive()) {
                    consumer.stop();
                }
                consumer.stop();
            } catch (Exception e) {
                // Log exception but continue cleanup
                System.err.println("Error closing consumer: " + e.getMessage());
            }
            consumer = null;
        }
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                // Log exception but continue cleanup
                System.err.println("Error closing context: " + e.getMessage());
            }
            context = null;
        }
    }

    @Order(1)
    @Test
    public void testStartAndStopConsumer() {
        consumer.start();
        assertTrue(consumer.isActive(), "Consumer should be active after start");
        consumer.stop();
        assertFalse(consumer.isActive(), "Consumer should be inactive after stop");
    }

    @Order(2)
    @Test
    public void testReceiveMessage() throws Exception {
        consumer.start();
        String testMessage = "Hello, ZeroMQ!";
        byte[] payload = testMessage.getBytes();
        socket.send(payload, 0);

        Message receivedMessage = consumer.nextMessage(1000);
        assertNotNull(receivedMessage, "Message should be received");
        assertArrayEquals(payload, receivedMessage.getBody(), "Received message body should match sent message body");
    }

    @Order(3)
    @Test
    public void testReceiveMultipleMessages() throws Exception {
        consumer.start();
        String testMessage1 = "Hello, ZeroMQ!";
        String testMessage2 = "Another message";
        byte[] payload1 = testMessage1.getBytes();
        byte[] payload2 = testMessage2.getBytes();

        socket.send(payload1, 0);
        // Add a small delay between sending messages to ensure they're processed correctly
        Thread.sleep(100);
        socket.send(payload2, 0);

        Message receivedMessage1 = consumer.nextMessage(1000);
        assertNotNull(receivedMessage1, "First message should be received");
        assertArrayEquals(payload1, receivedMessage1.getBody(), "First received message body should match sent message body");

        Message receivedMessage2 = consumer.nextMessage(1000);
        assertNotNull(receivedMessage2, "Second message should be received");
        assertArrayEquals(payload2, receivedMessage2.getBody(), "Second received message body should match sent message body");
    }

    @Order(4)
    @Test
    public void testConsumerCancellation() throws Exception {
        consumer.start();
        consumer.stop();
        assertTrue(consumer.cancelled(), "Consumer should be cancelled");
    }

    @Order(5)
    @Test
    public void testConsumerQueueUtilization() throws Exception {
        consumer.start();
        String testMessage = "Hello, ZeroMQ!";
        byte[] payload = testMessage.getBytes();

        // Use the existing socket instead of creating a new one
        socket.send(payload, 0);

        // Wait for the message to be processed by the consumer
        Message receivedMessage = consumer.nextMessage(1000);
        assertNotNull(receivedMessage, "Message should be received");

        // Now check the queue utilization
        int utilization = consumer.getQueueUtilizationPercentage();
        assertTrue(utilization >= 0, "Queue utilization should be greater than or equal to 0");
    }

}
