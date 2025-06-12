package com.aoneconsultancy.zeromq.core;

import com.aoneconsultancy.zeromq.config.ZmqConsumerProperties;
import com.aoneconsultancy.zeromq.config.ZmqProducerProperties;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.converter.SimpleMessageConverter;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import com.aoneconsultancy.zeromq.support.ActiveObjectCounter;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.zeromq.ZContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ZmqTemplateIntegrationTest {

    private ZContext context;
    private ZmqTemplate zmqTemplate;
    private BlockingQueueConsumer consumer;

    @BeforeEach
    public void setUp() {
        context = new ZContext();
        zmqTemplate = new ZmqTemplate(context, new ZmqProducerProperties(), 1000);
        ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter = new ActiveObjectCounter<>();

        ZmqConsumerProperties consumerConfig = ZmqConsumerProperties.builder().addresses(List.of("tcp://localhost:5555")).build();
        consumer = new BlockingQueueConsumer(context, activeObjectCounter, consumerConfig, 1000);
        consumer.start();
    }

    @AfterEach
    public void tearDown() {
        if (zmqTemplate != null) {
            zmqTemplate.destroy();
            zmqTemplate = null;
        }
        if (consumer != null) {
            consumer.stop();
            consumer = null;
        }
        if (context != null) {
            context.close();
            context = null;
        }
    }

    @Order(1)
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

    @Order(2)
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

    @Order(3)
    @Test
    public void testSendWithPostProcessor() throws InterruptedException {
        String testMessage = "Hello, ZeroMQ!";
        MessagePostProcessor postProcessor = message -> {
            message.setMessageProperty("processed", true);
            return message;
        };

        boolean sent = zmqTemplate.convertAndSend(zmqTemplate.getDefaultEndpointName(), testMessage, postProcessor);
        assertTrue(sent, "Message should be sent successfully");
    }

    @Order(5)
    @Test
    public void testSendWithRetry() {
        String testMessage = "Hello, ZeroMQ!";
        zmqTemplate.setPollRetry(5);
        zmqTemplate.setRetryDelay(100);

        boolean sent = zmqTemplate.sendBytes(testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully with retries");
    }

    @Order(6)
    @Test
    public void testSendWithBackpressure() {
        zmqTemplate.setBackpressureEnabled(true);
        String testMessage = "Hello, ZeroMQ!";

        boolean sent = zmqTemplate.sendBytes(testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully with backpressure");
    }

    @Order(7)
    @Test
    public void testSendWithCustomSendTimeout() {
        zmqTemplate.setSendTimeout(5000);
        String testMessage = "Hello, ZeroMQ!";

        boolean sent = zmqTemplate.sendBytes(testMessage.getBytes());
        assertTrue(sent, "Message should be sent successfully with custom send timeout");
    }

}
