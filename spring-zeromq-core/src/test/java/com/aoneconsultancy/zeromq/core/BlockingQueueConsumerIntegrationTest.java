package com.aoneconsultancy.zeromq.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.support.ActiveObjectCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class BlockingQueueConsumerIntegrationTest {

    private ZContext context;
    private BlockingQueueConsumer consumer;

    @BeforeEach
    public void setUp() {
        context = new ZContext();
        ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter = new ActiveObjectCounter<>();
        consumer = new BlockingQueueConsumer(context, activeObjectCounter, "tcp://localhost:5555", 1000);
    }

    @Test
    public void testStartAndStopConsumer() {
        consumer.start();
        assertTrue(consumer.isActive(), "Consumer should be active after start");
        consumer.stop();
        assertFalse(consumer.isActive(), "Consumer should be inactive after stop");
    }

    @Test
    public void testReceiveMessage() throws Exception {
        consumer.start();
        String testMessage = "Hello, ZeroMQ!";
        byte[] payload = testMessage.getBytes();

        // Simulate sending a message to the consumer
        ZMQ.Socket socket = context.createSocket(SocketType.PUSH);
        socket.bind("tcp://localhost:5555");
        socket.send(payload, 0);

        Message receivedMessage = consumer.nextMessage(1000);
        assertNotNull(receivedMessage, "Message should be received");
        assertArrayEquals(payload, receivedMessage.getBody(), "Received message body should match sent message body");

        consumer.stop();
        socket.setLinger(0);
        socket.close();
    }

    @Test
    public void testReceiveMultipleMessages() throws Exception {
        consumer.start();
        String testMessage1 = "Hello, ZeroMQ!";
        String testMessage2 = "Another message";
        byte[] payload1 = testMessage1.getBytes();
        byte[] payload2 = testMessage2.getBytes();

        // Simulate sending multiple messages to the consumer
        ZMQ.Socket socket = context.createSocket(SocketType.PUSH);
        socket.bind("tcp://localhost:5555");
        socket.send(payload1, 0);
        socket.send(payload2, 0);

        Message receivedMessage1 = consumer.nextMessage(1000);
        assertNotNull(receivedMessage1, "First message should be received");
        assertArrayEquals(payload1, receivedMessage1.getBody(), "First received message body should match sent message body");

        Message receivedMessage2 = consumer.nextMessage(1000);
        assertNotNull(receivedMessage2, "Second message should be received");
        assertArrayEquals(payload2, receivedMessage2.getBody(), "Second received message body should match sent message body");

        consumer.stop();
        socket.setLinger(0);
        socket.close();
    }

    @Test
    public void testConsumerCancellation() throws Exception {
        consumer.start();
        consumer.cancelled();
        assertTrue(consumer.cancelled(), "Consumer should be cancelled");

        consumer.stop();
    }

    @Test
    public void testConsumerQueueUtilization() throws Exception {
        consumer.start();
        String testMessage = "Hello, ZeroMQ!";
        byte[] payload = testMessage.getBytes();

        // Simulate sending a message to the consumer
        ZMQ.Socket socket = context.createSocket(SocketType.PUSH);
        socket.bind("tcp://localhost:5555");
        socket.send(payload, 0);

        int utilization = consumer.getQueueUtilizationPercentage();
        assertTrue(utilization > 0, "Queue utilization should be greater than 0");

        consumer.stop();
        socket.setLinger(0);
        socket.close();
    }

    @Test
    public void testConsumerClose() throws Exception {
        consumer.start();
        consumer.close();
        assertFalse(consumer.isActive(), "Consumer should be inactive after close");
    }
}