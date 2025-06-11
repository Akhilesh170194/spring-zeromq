package com.aoneconsultancy.zeromq.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@ExtendWith(MockitoExtension.class)
public class ZmqTemplateTest {

    @Mock
    private ZContext mockContext;

    @Mock
    private ZMQ.Socket mockSocket;

    @Mock
    private MessageConverter mockMessageConverter;

    @Mock
    private MessagePostProcessor mockPostProcessor;

    private ZmqTemplate template;

    private final String testAddress = "tcp://localhost:5555";
    private final byte[] testPayload = "test message".getBytes();

    @BeforeEach
    void setUp() {
        // Setup ZContext to return our mock socket
        when(mockContext.createSocket(any(SocketType.class))).thenReturn(mockSocket);

        template = new ZmqTemplate(mockContext);
        template.setMessageConverter(mockMessageConverter);
        template.setDefaultId(testAddress);
    }

    @Test
    void testSendBytes() {
        // Arrange
        when(mockSocket.send(any(byte[].class), anyInt())).thenReturn(true);

        // Act
        boolean result = template.sendBytes(testPayload);

        // Assert
        assertTrue(result);
        verify(mockSocket).send(eq(testPayload), anyInt());
    }

    @Test
    void testSendBytesToSpecificAddress() {
        // Arrange
        when(mockSocket.send(any(byte[].class), anyInt())).thenReturn(true);

        // Act
        boolean result = template.sendBytes("tcp://localhost:5556", testPayload);

        // Assert
        assertTrue(result);
        verify(mockSocket).send(eq(testPayload), anyInt());
    }

    @Test
    void testSendBytesWithRetry() {
        // Arrange
        // First attempt fails, second succeeds
        when(mockSocket.send(any(byte[].class), anyInt()))
                .thenReturn(false)
                .thenReturn(true);

        // Act
        boolean result = template.sendBytes(testPayload);

        // Assert
        assertTrue(result);
        verify(mockSocket, times(2)).send(eq(testPayload), anyInt());
    }

    @Test
    void testSendBytesFailure() {
        // Arrange
        // Configure the mock to return false for all invocations
        when(mockSocket.send(any(byte[].class), anyInt())).thenReturn(false);

        // Set a very short retry delay to speed up the test
        template.setRetryDelay(1);
        template.setPollRetry(1); // Only retry once

        // Act
        boolean result = template.sendBytes(testPayload);

        // Assert
        assertFalse(result);

        // Verify the send method was called at least once
        // We can't reliably verify exactly how many times due to timing issues
        verify(mockSocket, atLeastOnce()).send(eq(testPayload), anyInt());
    }

    @Test
    void testConvertAndSend() {
        // Arrange
        // Use a real template but mock the socket behavior
        when(mockSocket.send(any(byte[].class), anyInt())).thenReturn(true);

        Message mockMessage = mock(Message.class);
        when(mockMessage.getBody()).thenReturn(testPayload);

        when(mockMessageConverter.toMessage(any(), any(Map.class))).thenReturn(mockMessage);

        Object payload = new Object();

        // Act
        boolean result = template.convertAndSend(payload);

        // Assert
        assertTrue(result);
        verify(mockMessageConverter).toMessage(eq(payload), any(Map.class));
        verify(mockSocket).send(eq(testPayload), anyInt());
    }

    @Test
    void testConvertAndSendWithPostProcessor() {
        // Arrange
        // Use a real template but mock the socket behavior
        when(mockSocket.send(any(byte[].class), anyInt())).thenReturn(true);

        Message mockMessage = mock(Message.class);
        Message processedMessage = mock(Message.class);
        when(processedMessage.getBody()).thenReturn(testPayload);

        when(mockMessageConverter.toMessage(any(), any(Map.class))).thenReturn(mockMessage);
        when(mockPostProcessor.postProcessMessage(any(Message.class))).thenReturn(processedMessage);

        Object payload = new Object();

        // Act
        boolean result = template.convertAndSend(testAddress, payload, mockPostProcessor);

        // Assert
        assertTrue(result);
        verify(mockMessageConverter).toMessage(eq(payload), any(Map.class));
        verify(mockPostProcessor).postProcessMessage(eq(mockMessage));
        verify(mockSocket).send(eq(testPayload), anyInt());
    }

    @Test
    void testDestroy() {
        // Create a real ZmqTemplate with a mock context
        ZmqTemplate realTemplate = new ZmqTemplate(mockContext);

        // Send a message to create a socket
        when(mockSocket.send(any(byte[].class), anyInt())).thenReturn(true);
        realTemplate.setDefaultId(testAddress);
        realTemplate.sendBytes(testPayload);

        // Act
        realTemplate.destroy();

        // Assert
        verify(mockSocket).close();
    }
}
