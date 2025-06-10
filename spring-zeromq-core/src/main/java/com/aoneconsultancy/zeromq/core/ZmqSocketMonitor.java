package com.aoneconsultancy.zeromq.core;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import zmq.ZError;

/**
 * A low-level monitor for ZeroMQ sockets that uses the native monitoring API.
 * This class creates a monitoring socket that receives events from a monitored socket.
 */
@Slf4j
public class ZmqSocketMonitor implements Closeable {

    // Socket event constants
    public static final int EVENT_CONNECTED = 1;
    public static final int EVENT_CONNECT_DELAYED = 2;
    public static final int EVENT_CONNECT_RETRIED = 4;
    public static final int EVENT_LISTENING = 8;
    public static final int EVENT_BIND_FAILED = 16;
    public static final int EVENT_ACCEPTED = 32;
    public static final int EVENT_ACCEPT_FAILED = 64;
    public static final int EVENT_CLOSED = 128;
    public static final int EVENT_CLOSE_FAILED = 256;
    public static final int EVENT_DISCONNECTED = 512;
    public static final int EVENT_MONITOR_STOPPED = 1024;

    // Default endpoint format for inproc monitor sockets
    private static final String MONITOR_SOCKET_ENDPOINT_FORMAT = "inproc://monitor-%s";

    private final String socketId;
    private final ZMQ.Socket monitoredSocket;
    private final ZMQ.Socket monitorSocket;
    private final String monitorEndpoint;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService monitorExecutor;
    private final long shutdownTimeout;
    private final int monitorEvents;
    private final SocketEventListener eventListener;

    /**
     * Create a new ZmqSocketMonitor to monitor all events on the given socket.
     *
     * @param context         the ZeroMQ context
     * @param monitoredSocket the socket to monitor
     * @param socketId        a unique identifier for the socket
     * @param eventListener   a listener for socket events
     */
    public ZmqSocketMonitor(ZContext context, ZMQ.Socket monitoredSocket, String socketId, SocketEventListener eventListener) {
        this(context, monitoredSocket, socketId, eventListener, 0xFFFF, 5000);
    }

    /**
     * Create a new ZmqSocketMonitor to monitor specific events on the given socket.
     *
     * @param context         the ZeroMQ context
     * @param monitoredSocket the socket to monitor
     * @param socketId        a unique identifier for the socket
     * @param eventListener   a listener for socket events
     * @param events          a bit mask of the events to monitor (EVENT_* constants)
     * @param shutdownTimeout timeout in milliseconds for shutting down the monitor
     */
    public ZmqSocketMonitor(ZContext context, ZMQ.Socket monitoredSocket, String socketId,
                            SocketEventListener eventListener, int events, long shutdownTimeout) {
        Assert.notNull(context, "ZContext cannot be null");
        Assert.notNull(monitoredSocket, "Monitored socket cannot be null");
        Assert.notNull(socketId, "Socket ID cannot be null");

        this.socketId = socketId;
        this.monitoredSocket = monitoredSocket;
        this.eventListener = eventListener;
        this.shutdownTimeout = shutdownTimeout;
        this.monitorEvents = events;

        // Generate a unique inproc endpoint for this monitor
        this.monitorEndpoint = String.format(MONITOR_SOCKET_ENDPOINT_FORMAT, socketId);

        // Create the monitor socket
        this.monitorSocket = context.createSocket(SocketType.PAIR);

        // Create a dedicated thread for monitoring
        this.monitorExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "zmq-monitor-" + socketId);
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Start monitoring the socket.
     *
     * @return true if monitoring started successfully, false otherwise
     */
    public boolean start() {
        if (running.compareAndSet(false, true)) {
            try {
                // Monitor the socket for events
                boolean monitoringStarted = monitoredSocket.monitor(monitorEndpoint, monitorEvents);
                if (!monitoringStarted) {
                    log.error("Failed to start monitoring for socket {}: {}", socketId,
                            ZError.toString(monitoredSocket.errno()));
                    running.set(false);
                    return false;
                }

                // Connect the monitor socket to the endpoint
                boolean connected = monitorSocket.connect(monitorEndpoint);
                if (!connected) {
                    log.error("Failed to connect monitor socket for {}: {}", socketId,
                            ZError.toString(monitorSocket.errno()));
                    monitoredSocket.monitor(null, 0); // Stop monitoring
                    running.set(false);
                    return false;
                }

                // Start the monitoring thread
                monitorExecutor.submit(this::monitorLoop);

                if (log.isInfoEnabled()) {
                    log.info("Started ZeroMQ socket monitor for socket {}", socketId);
                }

                return true;
            } catch (Exception e) {
                running.set(false);
                log.error("Failed to start ZeroMQ socket monitor for socket {}: {}", socketId,
                        e.getMessage(), e);
                return false;
            }
        } else {
            log.debug("ZeroMQ socket monitor for socket {} is already running", socketId);
            return true;
        }
    }

    /**
     * The main monitoring loop that receives and processes socket events.
     */
    private void monitorLoop() {
        log.debug("ZeroMQ monitor loop started for socket {}", socketId);

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Check for a new event with timeout to allow checking the running flag
                byte[] eventData = monitorSocket.recv(ZMQ.DONTWAIT);
                if (eventData != null && eventData.length >= 6) { // At least 6 bytes (event type and value)
                    processEvent(eventData);
                } else {
                    // Sleep a bit to avoid busy waiting
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.error("Error in ZeroMQ monitor for socket {}: {}", socketId, e.getMessage(), e);
                }
                try {
                    // Sleep a bit to avoid tight error loops
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.debug("ZeroMQ monitor loop stopped for socket {}", socketId);
    }

    /**
     * Process a socket event.
     *
     * @param eventData the raw event data
     */
    private void processEvent(byte[] eventData) {
        // Event data format: [event(2bytes)][value(4bytes)][endpoint(variable)]
        ByteBuffer buffer = ByteBuffer.wrap(eventData);

        // First 16-bit integer is the event type
        int eventType = buffer.getShort() & 0xFFFF;

        // Next 32-bit integer is the event value
        int eventValue = buffer.getInt();

        // Remaining bytes are the endpoint address (if any)
        String address = null;
        if (buffer.remaining() > 0) {
            byte[] addressBytes = new byte[buffer.remaining()];
            buffer.get(addressBytes);
            address = new String(addressBytes, StandardCharsets.UTF_8);
        }

        // Log the event
        if (log.isDebugEnabled()) {
            log.debug("ZeroMQ socket {} event: {} ({}), value: {}, address: {}",
                    socketId, getEventName(eventType), eventType, eventValue, address);
        }

        // Notify listeners
        if (eventListener != null) {
            try {
                eventListener.onEvent(socketId, eventType, eventValue, address);
            } catch (Exception e) {
                log.warn("Error in event listener for socket {}: {}", socketId, e.getMessage(), e);
            }
        }

        // Special handling for monitor stopped event
        if (eventType == EVENT_MONITOR_STOPPED) {
            running.set(false);
        }
    }

    /**
     * Get a human-readable name for a ZeroMQ event type.
     *
     * @param eventType the event type
     * @return a human-readable name for the event type
     */
    private String getEventName(int eventType) {
        return switch (eventType) {
            case EVENT_CONNECTED -> "CONNECTED";
            case EVENT_CONNECT_DELAYED -> "CONNECT_DELAYED";
            case EVENT_CONNECT_RETRIED -> "CONNECT_RETRIED";
            case EVENT_LISTENING -> "LISTENING";
            case EVENT_BIND_FAILED -> "BIND_FAILED";
            case EVENT_ACCEPTED -> "ACCEPTED";
            case EVENT_ACCEPT_FAILED -> "ACCEPT_FAILED";
            case EVENT_CLOSED -> "CLOSED";
            case EVENT_CLOSE_FAILED -> "CLOSE_FAILED";
            case EVENT_DISCONNECTED -> "DISCONNECTED";
            case EVENT_MONITOR_STOPPED -> "MONITOR_STOPPED";
            default -> "UNKNOWN(" + eventType + ")";
        };
    }

    /**
     * Stop monitoring the socket.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                // Stop the monitor
                monitoredSocket.monitor(null, 0);

                // Shutdown the executor
                monitorExecutor.shutdownNow();
                if (!monitorExecutor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                    log.warn("ZeroMQ monitor thread for socket {} did not terminate in time", socketId);
                }

                // Close the monitor socket
                monitorSocket.close();

                if (log.isInfoEnabled()) {
                    log.info("Stopped ZeroMQ socket monitor for socket {}", socketId);
                }
            } catch (Exception e) {
                log.warn("Error stopping ZeroMQ monitor for socket {}: {}", socketId, e.getMessage(), e);
            }
        } else {
            log.debug("ZeroMQ socket monitor for socket {} is already stopped", socketId);
        }
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Interface for receiving socket events.
     */
    public interface SocketEventListener {
        /**
         * Called when a socket event occurs.
         *
         * @param socketId   the ID of the socket that generated the event
         * @param eventType  the type of event (one of the EVENT_* constants)
         * @param eventValue additional value associated with the event
         * @param address    the endpoint address associated with the event, may be null
         */
        void onEvent(String socketId, int eventType, int eventValue, String address);
    }
}
