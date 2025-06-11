package com.aoneconsultancy.zeromq.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ZmqSocketMonitorIntegrationTest {

    private ZContext context;
    private ZMQ.Socket socket;
    private ZmqSocketMonitor monitor;

    @BeforeEach
    public void setUp() {
        context = new ZContext();
        socket = context.createSocket(SocketType.PULL);
        socket.bind("tcp://localhost:5555");
        monitor = new ZmqSocketMonitor(context, socket, "test-socket", new DefaultSocketEventListener(null));
    }

    @AfterEach
    public void tearDown() {
        if (socket != null) {
            socket.close();
        }
        if (context != null) {
            context.close();
        }
        if (monitor != null) {
            monitor.close();
        }
    }

    @Test
    public void testStartAndStopMonitor() {
        boolean started = monitor.start();
        assertTrue(started, "Monitor should start successfully");
        monitor.stop();
        assertFalse(monitor.getRunning().get(), "Monitor should stop successfully");
    }

    @Test
    public void testMonitorEvents() {
        boolean started = monitor.start();
        assertTrue(started, "Monitor should start successfully");

        // Simulate a socket event
        byte[] eventData = new byte[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 0}; // Example event data
        monitor.processEvent(eventData);

        monitor.stop();
    }

    @Test
    public void testMonitorEventNames() {
        String eventName = monitor.getEventName(ZmqSocketMonitor.EVENT_CONNECTED);
        assertEquals("CONNECTED", eventName, "Event name should be CONNECTED");

        eventName = monitor.getEventName(ZmqSocketMonitor.EVENT_DISCONNECTED);
        assertEquals("DISCONNECTED", eventName, "Event name should be DISCONNECTED");

        eventName = monitor.getEventName(9999);
        assertEquals("UNKNOWN(9999)", eventName, "Event name should be UNKNOWN for invalid event type");
    }

    @Test
    public void testMonitorLoop() {
        boolean started = monitor.start();
        assertTrue(started, "Monitor should start successfully");

        // Simulate a socket event
        byte[] eventData = new byte[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 0}; // Example event data
        monitor.processEvent(eventData);

        monitor.stop();
    }

    @Test
    public void testMonitorClose() throws Exception {
        boolean started = monitor.start();
        assertTrue(started, "Monitor should start successfully");
        monitor.close();
        assertFalse(monitor.getRunning().get(), "Monitor should stop after close");
    }
}