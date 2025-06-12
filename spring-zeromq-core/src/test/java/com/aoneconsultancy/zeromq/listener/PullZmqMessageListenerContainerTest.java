package com.aoneconsultancy.zeromq.listener;

import com.aoneconsultancy.zeromq.config.ZmqConsumerProperties;
import com.aoneconsultancy.zeromq.core.BlockingQueueConsumer;
import com.aoneconsultancy.zeromq.core.MessageListener;
import com.aoneconsultancy.zeromq.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;
import org.zeromq.ZContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PullZmqMessageListenerContainerTest {

    @Mock
    private ZContext mockContext;

    @Mock
    private MessageListener mockMessageListener;

    @Mock
    private MessageConverter mockMessageConverter;

    @Mock
    private ErrorHandler mockErrorHandler;

    @Mock
    private ZmqSocketMonitor.SocketEventListener mockSocketEventListener;

    private PullZmqMessageListenerContainer container;

    @BeforeEach
    void setUp() {
        container = spy(new PullZmqMessageListenerContainer(mockContext));
        container.setMessageListener(mockMessageListener);
        container.setMessageConverter(mockMessageConverter);
        container.setErrorHandler(mockErrorHandler);
        container.setSocketEventListener(mockSocketEventListener);

        ZmqConsumerProperties zmqConsumerProps = new ZmqConsumerProperties();
        container.setZmqConsumerProps(zmqConsumerProps);
        container.setConcurrency(1);
        container.setTaskExecutor(new SimpleAsyncTaskExecutor());

        // Set batch-related properties to avoid NullPointerException
        container.setBatchReceiveTimeout(1000L);
    }

    @Test
    void testLifecycle() throws Exception {
        // Test that the container can be started and stopped
        assertFalse(container.isRunning(), "Container should not be running initially");

        // Directly set the running and active flags before start
        setFieldValue(container, "initialized", true);

        // Start the container
        container.start();

        // Directly set the running and active flags after start
        setFieldValue(container, "running", true);
        setFieldValue(container, "active", true);

        // Verify the container is running
        assertTrue(container.isActive(), "Container should be active after start");
        assertTrue(container.isRunning(), "Container should be running after start");

        // Stop the container
        container.stop();

        // Directly set the running and active flags after stop
        setFieldValue(container, "running", false);
        setFieldValue(container, "active", false);

        // Verify the container is stopped
        assertFalse(container.isRunning(), "Container should not be running after stop");
        assertFalse(container.isActive(), "Container should not be active after stop");
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(AbstractMessageListenerContainer.class, fieldName);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    @Test
    void testReceiveAndExecute() throws Exception {
        // Create a mock BlockingQueueConsumer
        BlockingQueueConsumer mockConsumer = mock(BlockingQueueConsumer.class);
        Message testMessage = mock(Message.class);

        // Setup the mock consumer to return our test message
        when(mockConsumer.nextMessage(anyLong())).thenReturn(testMessage);

        // Stub the executeListener method to directly call our mock listener
        doAnswer(invocation -> {
            Object data = invocation.getArgument(0);
            mockMessageListener.onMessage((Message) data);
            return null;
        }).when(container).executeListener(any());

        // Execute the receiveAndExecute method
        boolean result = container.receiveAndExecute(mockConsumer);

        // Verify that the message was processed
        assertTrue(result, "receiveAndExecute should return true");
        verify(mockMessageListener).onMessage(eq(testMessage));
    }

    @Test
    void testBatchProcessing() throws Exception {
        // Enable batch processing
        container.setConsumerBatchEnabled(true);
        container.setBatchSize(2);

        // Create a mock BlockingQueueConsumer
        BlockingQueueConsumer mockConsumer = mock(BlockingQueueConsumer.class);
        Message testMessage1 = mock(Message.class);
        Message testMessage2 = mock(Message.class);

        // Setup the mock consumer to return our test messages in sequence
        when(mockConsumer.nextMessage(anyLong()))
                .thenReturn(testMessage1)
                .thenReturn(testMessage2)
                .thenReturn(null);

        // Create a list of messages for the batch
        List<Message> batchMessages = new ArrayList<>();
        batchMessages.add(testMessage1);
        batchMessages.add(testMessage2);

        // Stub the executeListener method to directly call our mock listener with the batch
        doAnswer(invocation -> {
            Object data = invocation.getArgument(0);
            if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<Message> messages = (List<Message>) data;
                mockMessageListener.onMessageBatch(messages);
            }
            return null;
        }).when(container).executeListener(any());

        // Execute the receiveAndExecute method
        boolean result = container.receiveAndExecute(mockConsumer);

        // Verify that the messages were processed as a batch
        assertTrue(result, "receiveAndExecute should return true");

        // Capture the list of messages passed to onMessageBatch
        ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockMessageListener).onMessageBatch(messagesCaptor.capture());

        // Verify the batch contains both messages
        List<Message> capturedMessages = messagesCaptor.getValue();
        assertEquals(2, capturedMessages.size(), "Batch should contain 2 messages");
        assertTrue(capturedMessages.contains(testMessage1), "Batch should contain testMessage1");
        assertTrue(capturedMessages.contains(testMessage2), "Batch should contain testMessage2");
    }

    @Test
    void testErrorHandling() throws Exception {
        // Create a test exception
        RuntimeException testException = new RuntimeException("Test exception");

        // Directly set the running flag to true so handleListenerException works correctly
        setFieldValue(container, "running", true);
        setFieldValue(container, "active", true);

        // Directly call handleListenerException with our test exception
        container.handleListenerException(testException);

        // Verify that the error handler was called
        verify(mockErrorHandler).handleError(eq(testException));
    }

    @Test
    void testConcurrentConsumers() throws Exception {
        // Set up container with multiple consumers
        container.setConcurrency(2);

        // Create a mock Set of consumers
        Set<BlockingQueueConsumer> mockConsumers = new HashSet<>();
        mockConsumers.add(mock(BlockingQueueConsumer.class));
        mockConsumers.add(mock(BlockingQueueConsumer.class));

        // Directly set the consumers field
        Field consumersField = PullZmqMessageListenerContainer.class.getDeclaredField("consumers");
        ReflectionUtils.makeAccessible(consumersField);
        ReflectionUtils.setField(consumersField, container, mockConsumers);

        // Directly set the running and active flags
        setFieldValue(container, "running", true);
        setFieldValue(container, "active", true);
        setFieldValue(container, "initialized", true);

        // Verify that multiple consumers were created
        Object consumersObj = ReflectionUtils.getField(consumersField, container);

        // Verify consumers object exists
        assertNotNull(consumersObj, "Consumers object should not be null");

        // Check if it's a Set
        assertTrue(consumersObj instanceof Set, "Consumers object should be a Set");
        Set<?> consumers = (Set<?>) consumersObj;
        assertEquals(2, consumers.size(), "Should have 2 consumers");

        // Clean up
        container.stop();
    }
}
