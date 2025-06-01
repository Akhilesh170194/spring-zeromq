package com.aoneconsultancy.zeromqpoc.core;

import com.aoneconsultancy.zeromqpoc.core.message.Message;
import com.aoneconsultancy.zeromqpoc.support.ZmqException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import zmq.ZError;

/**
 * A class that handles consuming messages from a ZeroMQ socket.
 * This class encapsulates the ZeroMQ socket operations for receiving messages
 * and storing them in a blocking queue for consumption by a listener container.
 * Similar to Spring AMQP's BlockingQueueConsumer.
 */
@Slf4j
public class BlockingQueueConsumer {

    private final ZMQ.Socket socket;
    private final BlockingQueue<Message> queue;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Lock lifecycleLock = new ReentrantLock();
    @Getter
    private volatile Thread thread;
    private final ExecutorService pullerExecutor = Executors.newSingleThreadExecutor();
    @Getter
    private final String id;

    // For dynamic scaling
    @Getter
    private int consecutiveIdles = 0;
    @Getter
    private int consecutiveMessages = 0;

    @Setter
    private long shutdownTimeout;

    /**
     * Create a new ZmqMessageConsumer with the given parameters.
     *
     * @param context    the ZeroMQ context
     * @param address    the address to connect to
     * @param bufferSize the high-water mark for the socket
     */
    public BlockingQueueConsumer(ZContext context, String address, int bufferSize) {
        this(null, context, address, bufferSize, SocketType.PULL, false);
    }

    /**
     * Create a new ZmqMessageConsumer with the given parameters and ID.
     *
     * @param id         the consumer ID (can be null)
     * @param context    the ZeroMQ context
     * @param address    the address to connect to
     * @param bufferSize the high-water mark for the socket
     */
    public BlockingQueueConsumer(String id, ZContext context, String address, int bufferSize) {
        this(id, context, address, bufferSize, SocketType.PULL, false);
    }

    /**
     * Create a new ZmqMessageConsumer with the given parameters, ID, and socket type.
     *
     * @param id                  the consumer ID (can be null)
     * @param context             the ZeroMQ context
     * @param address             the address to connect to
     * @param bufferSize          the high-water mark for the socket
     * @param socketType          the type of socket to create
     * @param sendAcknowledgement whether to send an acknowledgement message back to the broker
     */
    public BlockingQueueConsumer(String id, ZContext context, String address, int bufferSize, SocketType socketType, boolean sendAcknowledgement) {
        this.id = id != null ? id : "zmq-consumer-" + System.currentTimeMillis();
        this.socket = context.createSocket(socketType);
        this.socket.setHWM(bufferSize);
        this.socket.connect(address);
        this.queue = new LinkedBlockingQueue<>(bufferSize > 0 ? bufferSize : 1000);

        if (log.isDebugEnabled()) {
            log.debug("Created consumer {} with address {}", this, address);
        }
    }

    /**
     * Record that a message was received.
     * Used for dynamic scaling of consumers.
     */
    public void messageReceived() {
        this.consecutiveIdles = 0;
        this.consecutiveMessages++;
    }

    /**
     * Record that no message was received.
     * Used for dynamic scaling of consumers.
     */
    public void idleDetected() {
        this.consecutiveMessages = 0;
        this.consecutiveIdles++;
    }

    /**
     * Check if this consumer is active.
     *
     * @return true if the consumer is active
     */
    public boolean isActive() {
        return this.active.get();
    }

    /**
     * Check if the consumer has been cancelled.
     *
     * @return true if the consumer has been cancelled
     */
    public boolean cancelled() {
        return this.cancelled.get() || !this.active.get();
    }

    /**
     * Start the consumer.
     */
    public void start() {
        this.lifecycleLock.lock();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Starting consumer {}", this);
            }
            this.thread = Thread.currentThread();
            CompletableFuture.runAsync(this::pullMessages, pullerExecutor);
            if (log.isDebugEnabled()) {
                log.debug("Started background puller thread for {}", this);
            }
            this.active.set(true);
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    /**
     * Background thread method that continuously pulls messages from the socket
     * and adds them to the queue.
     */
    private void pullMessages() {
        if (log.isDebugEnabled()) {
            log.debug("Background puller thread started for {}", this);
        }

        while (isActive() && !Thread.currentThread().isInterrupted()) {
            try {
                // Try to receive a message from the socket
                byte[] message = receiveWait();

                if (message != null) {
                    // Add the message to the queue
                    boolean added = this.queue.offer(new Message(message));
                    if (!added && log.isDebugEnabled()) {
                        log.debug("Queue is full, message discarded for {}", this);
                    }
                } else {
                    // No message available, sleep a bit to avoid busy waiting
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (log.isDebugEnabled()) {
                    log.debug("Background puller thread interrupted for {}", this);
                }
                break;
            } catch (Exception e) {
                if (isActive()) {
                    log.error("Error in background puller thread for {}: {}", this, e.getMessage(), e);
                }
                // Sleep a bit to avoid tight error loops
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Background puller thread stopped for {}", this);
        }
    }

    /**
     * Stop the consumer.
     */
    public void stop() {
        this.lifecycleLock.lock();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Stopping consumer {}", this);
            }

            this.active.set(false);
            this.cancelled.set(true);

            pullerExecutor.shutdownNow();
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    /**
     * Receive a message from the ZeroMQ socket.
     *
     * @return the message, or null if no message is available
     */
    public byte[] receiveWait() {
        try {
            byte[] data = this.socket.recv();

            if (data != null) {
                // Record that a message was received for dynamic scaling
                messageReceived();
            } else {
                // Record that no message was received for dynamic scaling
                log.warn("No message received for {}, error: {}", this, ZError.toString(socket.errno()));
                idleDetected();
            }

            return data;
        } catch (Exception e) {
            // Log error but continue processing
            log.error("Error in Pull Socket: {}", e.getMessage(), e);
            throw new ZmqException(e.getMessage(), e);
        }
    }

    /**
     * Main application-side API: wait for the next message delivery and return it.
     *
     * @return the next message
     * @throws InterruptedException if an interrupt is received while waiting
     */
    public Message nextMessage() throws InterruptedException {
        return nextMessage(-1);
    }

    /**
     * Main application-side API: wait for the next message delivery and return it.
     *
     * @param timeout timeout in millisecond
     * @return the next message or null if timed out
     * @throws InterruptedException if an interrupt is received while waiting
     */
    public Message nextMessage(long timeout) throws InterruptedException {
        if (log.isTraceEnabled()) {
            log.trace("Retrieving delivery for {}", this);
        }

        // Get a message from the queue
        Message message;
        if (timeout < 0) {
            // Wait indefinitely
            message = this.queue.take();
        } else {
            // Wait with timeout
            message = this.queue.poll(timeout, TimeUnit.MILLISECONDS);
        }

        if (message == null && this.cancelled.get()) {
            throw new RuntimeException("Consumer cancelled");
        }

        return message;
    }

    /**
     * Check if there is a delivery in the queue.
     *
     * @return true if there is at least one delivery in the queue
     */
    public boolean hasDelivery() {
        return !this.queue.isEmpty();
    }

    /**
     * Close the socket and release resources.
     */
    public void close() {
        // Stop the consumer and background thread
        stop();

        // Clear the queue
        this.queue.clear();

        // Close the socket
        this.socket.close();

        if (log.isDebugEnabled()) {
            log.debug("Closed resources for {}", this);
        }
    }

    @Override
    public String toString() {
        return "ZmqMessageConsumer[id=" + this.id + "]";
    }
}
