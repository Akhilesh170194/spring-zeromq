package com.aoneconsultancy.zeromq.integration;

import com.aoneconsultancy.zeromq.annotation.EnableZmq;
import com.aoneconsultancy.zeromq.annotation.ZmqListener;
import com.aoneconsultancy.zeromq.config.ZmqProducerProperties;
import com.aoneconsultancy.zeromq.core.ZmqTemplate;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.listener.PullZmqSocketListenerContainerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.zeromq.ZContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig
public class ZmqIntegrationTest {

    private static final String TEST_ADDRESS = "tcp://localhost:5555";
    private static final String TEST_MESSAGE = "Hello ZeroMQ!";
    private static final int TIMEOUT_SECONDS = 5;

    @Configuration
    @EnableZmq
    static class TestConfig {

        @Bean
        public ZContext zContext() {
            return new ZContext();
        }

        @Bean
        public ZmqTemplate zmqTemplate(ZContext zContext) {
            ZmqTemplate template = new ZmqTemplate(zContext, new ZmqProducerProperties());
            template.setDefaultEndpointName(TEST_ADDRESS);
            return template;
        }

        @Bean
        public PullZmqSocketListenerContainerFactory pullZmqSocketListenerContainerFactory(ZContext zContext) {
            PullZmqSocketListenerContainerFactory factory = new PullZmqSocketListenerContainerFactory(zContext);
            return factory;
        }

        @Bean
        public MessageListener messageListener() {
            return new MessageListener();
        }
    }

    static class MessageListener {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<String> receivedMessage = new AtomicReference<>();

        @ZmqListener(endpoints = TEST_ADDRESS)
        public void handleMessage(Message message) {
            byte[] body = message.getBody();
            if (body != null) {
                receivedMessage.set(new String(body));
                latch.countDown();
            }
        }

        public boolean awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public String getReceivedMessage() {
            return receivedMessage.get();
        }
    }

    @Autowired
    private ZmqTemplate zmqTemplate;

    @Autowired
    private MessageListener messageListener;

    @Test
    void testSendAndReceiveMessage() throws Exception {
        // Create a message
        Message message = Message.builder(TEST_MESSAGE.getBytes()).build();

        // Send the message
        boolean sent = zmqTemplate.convertAndSend(message);
        assertTrue(sent, "Message should be sent successfully");

        // Wait for the message to be received
        boolean received = messageListener.awaitMessage(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(received, "Message should be received within timeout");

        // Verify the message content
        assertEquals(TEST_MESSAGE, messageListener.getReceivedMessage(), "Received message should match sent message");
    }

    @Test
    void testSendAndReceivePayload() throws Exception {
        // Send a string payload directly
        boolean sent = zmqTemplate.convertAndSend(TEST_MESSAGE);
        assertTrue(sent, "Message should be sent successfully");

        // Wait for the message to be received
        boolean received = messageListener.awaitMessage(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(received, "Message should be received within timeout");

        // Verify the message content
        assertEquals(TEST_MESSAGE, messageListener.getReceivedMessage(), "Received message should match sent message");
    }
}
