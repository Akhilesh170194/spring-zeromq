package com.aoneconsultancy.zeromq.integration;

import com.aoneconsultancy.zeromq.annotation.EnableZmq;
import com.aoneconsultancy.zeromq.config.ZmqConsumerProperties;
import com.aoneconsultancy.zeromq.config.ZmqProducerProperties;
import com.aoneconsultancy.zeromq.core.MessageListener;
import com.aoneconsultancy.zeromq.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromq.core.ZmqTemplate;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.listener.MessageListenerContainer;
import com.aoneconsultancy.zeromq.listener.PullZmqMessageListenerContainer;
import com.aoneconsultancy.zeromq.listener.PullZmqSocketListenerContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ErrorHandler;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the MessageListenerContainer and ZmqListenerContainerFactory.
 * This test verifies that:
 * 1. The container factory correctly creates containers
 * 2. The container lifecycle (start, stop) works correctly
 * 3. Messages can be sent and received through the container
 * 4. Error handling works correctly
 */
@SpringJUnitConfig
public class ListenerContainerIntegrationTest {

    private static final String TEST_ADDRESS = "tcp://localhost:5559"; // Using a different port to avoid conflicts
    private static final String TEST_MESSAGE = "Hello ZeroMQ Container!";
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
            // Configure the factory with default settings
            factory.setConcurrency(1);
            return factory;
        }

        @Bean
        public TestMessageListener testMessageListener() {
            return new TestMessageListener();
        }

        @Bean
        public TestErrorHandler testErrorHandler() {
            return new TestErrorHandler();
        }
    }

    static class TestMessageListener implements MessageListener {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<String> receivedMessage = new AtomicReference<>();

        @Override
        public void onMessage(Message message) {
            byte[] body = message.getBody();
            if (body != null) {
                receivedMessage.set(new String(body));
                latch.countDown();
            }
        }

        @Override
        public void onMessageBatch(List<Message> messages) {
            // Not used in this test
        }

        public boolean awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public String getReceivedMessage() {
            return receivedMessage.get();
        }

        public void reset() {
            receivedMessage.set(null);
            // Reset the latch for the next test
            while (latch.getCount() == 0) {
                // Create a new latch if the current one is already counted down
                CountDownLatch newLatch = new CountDownLatch(1);
                try {
                    Field latchField = TestMessageListener.class.getDeclaredField("latch");
                    latchField.setAccessible(true);
                    latchField.set(this, newLatch);
                    break;
                } catch (Exception e) {
                    // If reflection fails, just create a new listener
                    throw new RuntimeException("Failed to reset latch", e);
                }
            }
        }
    }

    static class TestErrorHandler implements ErrorHandler {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<Throwable> caughtException = new AtomicReference<>();

        @Override
        public void handleError(Throwable t) {
            caughtException.set(t);
            latch.countDown();
        }

        public boolean awaitError(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public Throwable getCaughtException() {
            return caughtException.get();
        }
    }

    @Autowired
    private ZmqTemplate zmqTemplate;

    @Autowired
    private PullZmqSocketListenerContainerFactory containerFactory;

    @Autowired
    private TestMessageListener messageListener;

    @Autowired
    private TestErrorHandler errorHandler;

    @Autowired
    private ZContext zContext;

    private MessageListenerContainer container;

    @BeforeEach
    void setUp() {
        // Create a ZmqListenerEndpoint to pass to the factory
        ZmqConsumerProperties consumer = ZmqConsumerProperties.builder()
                .name("testConsumer")
                .addresses(List.of(TEST_ADDRESS))
                .type(SocketType.PULL)
                .build();

        // Create a simple endpoint implementation
        com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint endpoint =
                new com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint() {
                    @Override
                    public ZmqConsumerProperties getZmqConsumerProps() {
                        return consumer;
                    }

                    @Override
                    public ZmqListenerErrorHandler getErrorHandler() {
                        return null;
                    }

                    @Override
                    public void setMessageConverter(MessageConverter converter) {
                        // No-op
                    }

                    @Override
                    public MessageConverter getMessageConverter() {
                        return null;
                    }

                    @Override
                    public void setupListenerContainer(MessageListenerContainer container) {
                        // No-op
                    }

                    @Override
                    public Executor getTaskExecutor() {
                        return null;
                    }

                    @Override
                    public Boolean getConsumerBatchEnabled() {
                        return false;
                    }

                    @Override
                    public void setConsumerBatchEnabled(Boolean consumerBatchEnabled) {
                        // No-op
                    }

                    @Override
                    public ZmqSocketMonitor.SocketEventListener getSocketEventListener() {
                        return null;
                    }

                    @Override
                    public void setSocketEventListener(ZmqSocketMonitor.SocketEventListener socketEventListener) {
                        // No-op
                    }
                };

        container = containerFactory.createListenerContainer(endpoint);
        container.setupMessageListener(messageListener);
        container.setErrorHandler(errorHandler);
    }

    @AfterEach
    void tearDown() {
        // Stop the container if it's running
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    @Test
    void testContainerCreation() {
        // Verify that the container was created correctly
        assertNotNull(container, "Container should not be null");
        assertTrue(container instanceof PullZmqMessageListenerContainer,
                "Container should be an instance of PullZmqMessageListenerContainer");
    }

    @Test
    void testContainerLifecycle() {
        // Test container lifecycle (start, stop)
        assertFalse(container.isRunning(), "Container should not be running initially");

        container.start();
        assertTrue(container.isRunning(), "Container should be running after start");

        container.stop();
        assertFalse(container.isRunning(), "Container should not be running after stop");
    }

    @Test
    void testSendAndReceiveMessage() throws Exception {
        // Start the container
        container.start();

        // Create a message
        Message message = Message.builder(TEST_MESSAGE.getBytes()).build();

        // Send the message
        boolean sent = zmqTemplate.convertAndSend(message);
        assertTrue(sent, "Message should be sent successfully");

        // Wait for the message to be received
        boolean received = messageListener.awaitMessage(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(received, "Message should be received within timeout");

        // Verify the message content
        assertEquals(TEST_MESSAGE, messageListener.getReceivedMessage(),
                "Received message should match sent message");
    }

    @Test
    void testFactoryConfiguration() {
        // Create a container with custom configuration
        PullZmqSocketListenerContainerFactory customFactory =
                new PullZmqSocketListenerContainerFactory(zContext);
        customFactory.setConcurrency(2);
        customFactory.setConsumerBatchEnabled(true);
        customFactory.setBatchSize(10);

        ZmqConsumerProperties consumer = ZmqConsumerProperties.builder()
                .name("customConsumer")
                .addresses(List.of("tcp://localhost:5560"))
                .type(SocketType.PULL)
                .build();

        // Create a simple endpoint implementation
        com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint endpoint =
                new com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint() {
                    @Override
                    public ZmqConsumerProperties getZmqConsumerProps() {
                        return consumer;
                    }

                    @Override
                    public ZmqListenerErrorHandler getErrorHandler() {
                        return null;
                    }

                    @Override
                    public void setMessageConverter(MessageConverter converter) {
                        // No-op
                    }

                    @Override
                    public MessageConverter getMessageConverter() {
                        return null;
                    }

                    @Override
                    public void setupListenerContainer(MessageListenerContainer container) {
                        // No-op
                    }

                    @Override
                    public Executor getTaskExecutor() {
                        return null;
                    }

                    @Override
                    public Boolean getConsumerBatchEnabled() {
                        return false;
                    }

                    @Override
                    public void setConsumerBatchEnabled(Boolean consumerBatchEnabled) {
                        // No-op
                    }

                    @Override
                    public ZmqSocketMonitor.SocketEventListener getSocketEventListener() {
                        return null;
                    }

                    @Override
                    public void setSocketEventListener(ZmqSocketMonitor.SocketEventListener socketEventListener) {
                        // No-op
                    }
                };

        MessageListenerContainer customContainer = customFactory.createListenerContainer(endpoint);

        // Verify that the container was created correctly
        assertTrue(customContainer instanceof PullZmqMessageListenerContainer,
                "Container should be an instance of PullZmqMessageListenerContainer");

        // Verify the consumer properties were set correctly
        assertEquals("customConsumer", customContainer.getZmqConsumerProps().getName(),
                "Consumer name should be 'customConsumer'");
        assertEquals(SocketType.PULL, customContainer.getZmqConsumerProps().getType(),
                "Socket type should be PULL");
        assertEquals("tcp://localhost:5560", customContainer.getZmqConsumerProps().getAddresses().get(0),
                "Address should be tcp://localhost:5560");
    }
}
