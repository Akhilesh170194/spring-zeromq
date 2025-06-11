package com.aoneconsultancy.zeromq.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@ExtendWith(MockitoExtension.class)
public class ZmqPushTest {

    @Mock
    private ZContext mockContext;

    @Mock
    private ZMQ.Socket mockSocket;

    private ZmqPush zmqPush;

    private final String testAddress = "tcp://localhost:5555";
    private final byte[] testPayload = "test message".getBytes();
    private final int testHwm = 1000;
    private final int testSendBufferSize = 1024;

    @BeforeEach
    void setUp() {
        when(mockContext.createSocket(any(SocketType.class))).thenReturn(mockSocket);
    }

    @Test
    void testConstructorWithSingleAddress() {
        // Act
        zmqPush = new ZmqPush(mockContext, testAddress, testHwm);

        // Assert
        verify(mockContext).createSocket(SocketType.PUSH);
        verify(mockSocket).setHWM(testHwm);
        verify(mockSocket).setSendBufferSize(1024); // Default value
        verify(mockSocket).bind(testAddress);
    }

    @Test
    void testConstructorWithMultipleAddresses() {
        // Arrange
        List<String> addresses = List.of(testAddress, "tcp://localhost:5556");

        // Act
        zmqPush = new ZmqPush(mockContext, SocketType.PUSH, true, addresses, testHwm, testSendBufferSize, 0);

        // Assert
        verify(mockContext).createSocket(SocketType.PUSH);
        verify(mockSocket).setHWM(testHwm);
        verify(mockSocket).setSendBufferSize(testSendBufferSize);
        verify(mockSocket).bind(testAddress);
        verify(mockSocket).bind("tcp://localhost:5556");
    }

    @Test
    void testConstructorWithConnect() {
        // Arrange
        List<String> addresses = List.of(testAddress);

        // Act
        zmqPush = new ZmqPush(mockContext, SocketType.PUSH, false, addresses, testHwm, testSendBufferSize, 0);

        // Assert
        verify(mockContext).createSocket(SocketType.PUSH);
        verify(mockSocket).setHWM(testHwm);
        verify(mockSocket).setSendBufferSize(testSendBufferSize);
        verify(mockSocket).connect(testAddress);
        verify(mockSocket, never()).bind(anyString());
    }

    @Test
    void testSend() {
        // Arrange
        zmqPush = new ZmqPush(mockContext, testAddress, testHwm);
        when(mockSocket.send(any(byte[].class), eq(0))).thenReturn(true);

        // Act
        boolean result = zmqPush.send(testPayload);

        // Assert
        assertTrue(result);
        verify(mockSocket).send(eq(testPayload), eq(0));
    }

    @Test
    void testSendWithFlags() {
        // Arrange
        zmqPush = new ZmqPush(mockContext, testAddress, testHwm);
        when(mockSocket.send(any(byte[].class), eq(ZMQ.DONTWAIT))).thenReturn(true);

        // Act
        boolean result = zmqPush.send(testPayload, ZMQ.DONTWAIT);

        // Assert
        assertTrue(result);
        verify(mockSocket).send(eq(testPayload), eq(ZMQ.DONTWAIT));
    }

    @Test
    void testSendFailure() {
        // Arrange
        zmqPush = new ZmqPush(mockContext, testAddress, testHwm);
        when(mockSocket.send(any(byte[].class), anyInt())).thenReturn(false);

        // Act
        boolean result = zmqPush.send(testPayload);

        // Assert
        assertFalse(result);
        verify(mockSocket).send(eq(testPayload), eq(0));
    }

    @Test
    void testClose() {
        // Arrange
        zmqPush = new ZmqPush(mockContext, testAddress, testHwm);

        // Act
        zmqPush.close();

        // Assert
        verify(mockSocket).close();
    }
}