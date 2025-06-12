package com.aoneconsultancy.zeromq.core.monitoring;

import lombok.Getter;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics collected for a ZeroMQ socket.
 */
@Getter
public class ZmqSocketStats {

    private final String socketName;
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong connectCount = new AtomicLong(0);
    private final AtomicLong disconnectCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile Instant lastActivity = Instant.now();
    private volatile Instant lastConnected = null;
    private volatile Instant lastDisconnected = null;
    private volatile boolean connected = false;
    private volatile String currentEndpoint = null;

    /**
     * Create a new ZmqSocketStats for the given socket name.
     *
     * @param socketName the socket name
     */
    public ZmqSocketStats(String socketName) {
        this.socketName = socketName;
    }

    /**
     * Record a message received event.
     *
     * @param bytes the number of bytes received
     */
    public void recordMessageReceived(int bytes) {
        messagesReceived.incrementAndGet();
        bytesReceived.addAndGet(bytes);
        lastActivity = Instant.now();
    }

    /**
     * Record a message sent event.
     *
     * @param bytes the number of bytes sent
     */
    public void recordMessageSent(int bytes) {
        messagesSent.incrementAndGet();
        bytesSent.addAndGet(bytes);
        lastActivity = Instant.now();
    }

    /**
     * Record a connection event.
     *
     * @param endpoint the endpoint address
     */
    public void recordConnected(String endpoint) {
        connectCount.incrementAndGet();
        lastConnected = Instant.now();
        lastActivity = lastConnected;
        connected = true;
        currentEndpoint = endpoint;
    }

    /**
     * Record a disconnection event.
     */
    public void recordDisconnected() {
        disconnectCount.incrementAndGet();
        lastDisconnected = Instant.now();
        lastActivity = lastDisconnected;
        connected = false;
    }

    /**
     * Record an error event.
     */
    public void recordError() {
        errorCount.incrementAndGet();
        lastActivity = Instant.now();
    }

    /**
     * Reset all statistics.
     */
    public void reset() {
        messagesReceived.set(0);
        messagesSent.set(0);
        bytesReceived.set(0);
        bytesSent.set(0);
        connectCount.set(0);
        disconnectCount.set(0);
        errorCount.set(0);
        lastActivity = Instant.now();
        lastConnected = null;
        lastDisconnected = null;
        connected = false;
        currentEndpoint = null;
    }

    /**
     * Get the time since the last activity in milliseconds.
     *
     * @return the time since the last activity in milliseconds
     */
    public long getIdleTimeMs() {
        return lastActivity != null ? Instant.now().toEpochMilli() - lastActivity.toEpochMilli() : 0;
    }
}
