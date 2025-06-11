package com.aoneconsultancy.zeromq.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.converter.SimpleMessageConverter;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import com.aoneconsultancy.zeromq.support.ActiveObjectCounter;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

public class ZmqTemplateIntegrationTest {

    private ZContext context;
    private ZmqTemplate zmqTemplate;
    private BlockingQueueConsumer consumer;

    @BeforeEach
    public void setUp() {
        context = new ZContext();
        zmqTemplate = new ZmqTemplate(context, 1000);
        ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter = new ActiveObjectCounter<>();
        consumer = new BlockingQueueConsumer(context, activeObjectCounter, "tcp://localhost:5555", 1000);
        consumer.start();
    }

    @Test
    public void testSendAndReceiveMessage() throws Exception {
        String testMessage = "Hello, ZeroMQ!";
        byte[] payload = testMessage.getBytes();

        boolean sent = zmqTemplate.sendBytes(payload);
        assertTrue(sent, "Message should be sent successfully");

        Message receivedMessage = consumer.nextMessage(1000);
        assertNotNull(receivedMessage, "Message should be received");
        assertArrayEquals(payload, receivedMessage.getBody(), "Received message body should match sent message body");
    }

    @Test
    public void testConvertAndSendMessage() {
        String testMessage = "Hello, ZeroMQ!";
        MessageConverter messageConverter = new SimpleMessageConverter();

        Map<String, Object> messageProperties = new HashMap<>();
        messageProperties.put(ZmqHeaders.MESSAGE_ID, UUID.randomUUID().toString());
        messageProperties.put(ZmqHeaders.TIMESTAMP, System.currentTimeMillis());

        Message message = messageConverter.toMessage(testMessage, messageProperties);

        boolean sent = zmqTemplate.convertAndSend(message);
        assertTrue(sent, "Message should be sent successfully");
    }

    @Test
    public void testSendWithPostProcessor() {
        String testMessage = "Hello, ZeroMQ!";
        MessagePostProcessor postProcessor = message -> {
            message.getMessageProperties().put("processed", true);
            return message;
        };

        boolean sent = zmqTemplate.convertAndSend(zmqTemplate.getDefaultId(), testMessage, postProcessor);
        assertTrue(sent, "Message should be sent successfully");
    }

    @Test
    public void testSendToSpecificEndpoint() {
        String testMessage = "Hello, ZeroMQ!";
        String endpoint = "tcp://localhost:5556";

        boolean sent = zmqTemplate.sendBytes(endpoint, testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully to the specified endpoint");
    }

    @Test
    public void testSendWithRetry() {
        String testMessage = "Hello, ZeroMQ!";
        zmqTemplate.setPollRetry(5);
        zmqTemplate.setRetryDelay(100);

        boolean sent = zmqTemplate.sendBytes(testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully with retries");
    }

    @Test
    public void testSendWithBackpressure() {
        zmqTemplate.setBackpressureEnabled(true);
        String testMessage = "Hello, ZeroMQ!";

        boolean sent = zmqTemplate.sendBytes(testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully with backpressure");
    }

    @Test
    public void testSendWithCustomSocketType() {
        zmqTemplate.setSocketType(SocketType.PUB);
        String testMessage = "Hello, ZeroMQ!";

        boolean sent = zmqTemplate.sendBytes(testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully with custom socket type");
    }

    @Test
    public void testSendWithCustomSendTimeout() {
        zmqTemplate.setSendTimeout(5000);
        String testMessage = "Hello, ZeroMQ!";

        boolean sent = zmqTemplate.sendBytes(testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully with custom send timeout");
    }

    @Test
    public void testSendWithCustomEndpoints() {
        List<String> endpoints = List.of("tcp://localhost:5557", "tcp://localhost:5558");
        zmqTemplate.setEndpoints(endpoints);
        String testMessage = "Hello, ZeroMQ!";

        boolean sent = zmqTemplate.sendBytes(testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully to custom endpoints");
    }
}