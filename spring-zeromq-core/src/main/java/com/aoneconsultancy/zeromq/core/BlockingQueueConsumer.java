package com.aoneconsultancy.zeromq.core;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.monitoring.ZmqSocketStats;
import com.aoneconsultancy.zeromq.support.ActiveObjectCounter;
import com.aoneconsultancy.zeromq.support.ConsumerCancelledException;
import com.aoneconsultancy.zeromq.support.ZmqException;
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
import org.springframework.util.Assert;
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

    // Configuration properties
    @Getter
    private final String id;
    private final SocketType socketType;
    @Getter
    private final String address;
    private final int highWaterMark;
    @Setter
    private long shutdownTimeout;
    @Setter
    private long consumeDelay;

    // ZeroMQ related fields
    private final ZContext context;
    private ZMQ.Socket socket;
    private ZmqSocketMonitor socketMonitor;
    private ZmqSocketMonitor.SocketEventListener eventListener;

    // Message queue and state management
    private final BlockingQueue<Message> queue;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile long abortStarted;

    // Thread management
    private final Lock lifecycleLock = new ReentrantLock();
    @Getter
    private volatile Thread thread;
    private final ExecutorService pullerExecutor;
    private final ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter;

    // Backoff mechanism for polling
    private long currentBackoff = 1;
    private final long maxBackoff = 100; // Maximum backoff in ms

    /**
     * Create a new ZmqMessageConsumer with the given parameters.
     *
     * @param context             the ZeroMQ context
     * @param address             the address to connect to
     * @param bufferSize          the high-water mark for the socket
     * @param activeObjectCounter the counter to track active objects
     */
    public BlockingQueueConsumer(ZContext context,
                                 ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter,
                                 String address,
                                 int bufferSize) {
        this(null, context, activeObjectCounter, address, bufferSize, SocketType.PULL, false);
    }

    /**
     * Create a new ZmqMessageConsumer with the given parameters and ID.
     *
     * @param id                  the consumer ID (can be null)
     * @param context             the ZeroMQ context
     * @param activeObjectCounter the counter to track active objects
     * @param address             the address to connect to
     * @param bufferSize          the high-water mark for the socket
     */
    public BlockingQueueConsumer(String id,
                                 ZContext context,
                                 ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter,
                                 String address,
                                 int bufferSize) {
        this(id, context, activeObjectCounter, address, bufferSize, SocketType.PULL, false);
    }

    /**
     * Create a new ZmqMessageConsumer with the given parameters, ID, and socket type.
     *
     * @param id                  the consumer ID (can be null)
     * @param context             the ZeroMQ context
     * @param activeObjectCounter the counter to track active objects
     * @param address             the address to connect to
     * @param bufferSize          the high-water mark for the socket
     * @param socketType          the type of socket to create
     * @param sendAcknowledgement whether to send an acknowledgement message back to the broker
     */
    public BlockingQueueConsumer(String id,
                                 ZContext context,
                                 ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter,
                                 String address,
                                 int bufferSize,
                                 SocketType socketType,
                                 boolean sendAcknowledgement) {

        Assert.notNull(context, "ZContext cannot be null");
        Assert.notNull(activeObjectCounter, "ActiveObjectCounter cannot be null");
        Assert.notNull(address, "Address cannot be null");

        this.id = id != null ? id : "zmq-consumer-" + System.currentTimeMillis();
        this.queue = new LinkedBlockingQueue<>(bufferSize > 0 ? bufferSize : 1000);
        this.shutdownTimeout = 5000; // Default 5 seconds

        this.pullerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "zmq-puller-" + this.id);
            thread.setDaemon(true);
            return thread;
        });

        this.context = context;
        this.address = address;
        this.socketType = socketType;
        this.highWaterMark = bufferSize;
        this.activeObjectCounter = activeObjectCounter;

        if (log.isDebugEnabled()) {
            log.debug("Created consumer {} with address {}", this, address);
        }
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
        return this.cancelled.get() ||
                (this.abortStarted > 0 && this.abortStarted + this.shutdownTimeout > System.currentTimeMillis()) ||
                !this.activeObjectCounter.isActive();
    }

    /**
     * Start the consumer.
     */
    public void start() {
        this.lifecycleLock.lock();
        try {
            if (isActive()) {
                log.debug("Consumer {} is already running", this);
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("Starting consumer {}", this);
            }

            // First connect the socket before starting the thread
            initializedAndConnectSocket();
            setQosAndCreateConsumers();

            // Now we can start processing
            this.thread = Thread.currentThread();
            this.active.set(true);
            this.activeObjectCounter.add(this);

            // Start the background processing thread
            CompletableFuture.runAsync(this::pullMessages, pullerExecutor);

            if (log.isDebugEnabled()) {
                log.debug("Started background puller thread for {}", this);
            }
        } catch (Exception e) {
            this.active.set(false);
            log.error("Failed to start consumer {}: {}", this, e.getMessage(), e);
            throw e;
        } finally {
            this.lifecycleLock.unlock();
        }

        if (log.isDebugEnabled()) {
            log.debug("Started consumer {}", this);
        }
    }

    /**
     * Initialize and connect the ZeroMQ socket.
     * This method is called during consumer start.
     *
     * @throws ZmqException if there is an error connecting the socket
     */
    public void initializedAndConnectSocket() {
        this.lifecycleLock.lock();
        try {
            if (this.socket != null) {
                log.debug("Socket already initialized for {}", this);
                return;
            }

            log.debug("Initializing socket for {} with address {}", this, this.address);
            this.socket = context.createSocket(this.socketType);

            // Set socket options
            this.socket.setHWM(this.highWaterMark);
            // TODO - Check is we need to set the below or not.
            // this.socket.setLinger(0);

            // Setup socket monitoring if an event listener is configured
            if (this.eventListener != null) {
                try {
                    // Create the monitor with all events enabled
                    this.socketMonitor = new ZmqSocketMonitor(
                            this.context,
                            this.socket,
                            this.id,
                            this.eventListener
                    );

                    // Start the monitor
                    boolean started = this.socketMonitor.start();
                    if (started) {
                        log.info("Started socket monitor for {}", this);
                    } else {
                        log.warn("Failed to start socket monitor for {}", this);
                    }
                } catch (Exception e) {
                    log.warn("Failed to create socket monitor for {}: {}", this, e.getMessage(), e);
                    // Continue without monitoring
                }
            }

            // Connect the socket
            boolean connected = this.socket.connect(this.address);
            if (!connected) {
                int errorCode = this.socket.errno();
                String errorMsg = ZError.toString(errorCode);
                log.error("Failed to connect socket for {} to {}: {}", this, this.address, errorMsg);
                throw new ZmqException("Failed to connect socket: " + errorMsg);
            }

            log.debug("Successfully connected socket for {} to {}", this, this.address);
        } catch (Exception e) {
            log.error("Error connecting ZeroMQ socket: {}", e.getMessage(), e);
            throw new ZmqException("Error connecting ZeroMQ socket: " + e.getMessage(), e);
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    /**
     * Apply QoS settings and delay if configured.
     * This method is called during consumer start.
     */
    private void setQosAndCreateConsumers() {
        if (this.consumeDelay > 0) {
            try {
                log.debug("Applying consume delay of {}ms for {}", this.consumeDelay, this);
                Thread.sleep(this.consumeDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while applying consume delay");
            }
        }
    }

    /**
     * Get the current queue size.
     *
     * @return the current queue size
     */
    public int getQueueSize() {
        return this.queue.size();
    }

    /**
     * Get the queue capacity.
     *
     * @return the queue capacity
     */
    public int getQueueCapacity() {
        return this.queue.size() + this.queue.remainingCapacity();
    }

    /**
     * Get the queue utilization as a percentage.
     *
     * @return the queue utilization as a percentage (0-100)
     */
    public int getQueueUtilizationPercentage() {
        int capacity = getQueueCapacity();
        if (capacity == 0) {
            return 0;
        }
        return (int) (getQueueSize() * 100.0 / capacity);
    }

    /**
     * Background thread method that continuously pulls messages from the socket
     * and adds them to the queue.
     */
    private void pullMessages() {
        if (log.isDebugEnabled()) {
            log.debug("Background puller thread started for {}", this);
        }

        try {
            while (isActive() && !Thread.currentThread().isInterrupted()) {
                processNextMessage();
            }
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Background puller thread stopped for {}", this);
            }
        }
    }

    /**
     * Process a single message from the socket
     */
    private void processNextMessage() {
        try {
            // Try to receive a message from the socket
            byte[] message = receiveWait();

            if (message != null) {
                // Add the message to the queue
                this.queue.put(new Message(message));
                // Reset backoff when messages are flowing
                currentBackoff = 1;
            } else {
                // Adaptive backoff to reduce CPU usage during idle periods
                Thread.sleep(currentBackoff);
                currentBackoff = Math.min(currentBackoff * 2, maxBackoff);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (log.isDebugEnabled()) {
                log.debug("Background consumer thread interrupted for {}", this);
            }
        } catch (Exception e) {
            if (isActive()) {
                log.error("Error in background puller thread for {}: {}", this, e.getMessage(), e);
            }
            // Sleep a bit to avoid tight error loops
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Stop the consumer.
     */
    public void stop() {
        this.lifecycleLock.lock();
        try {
            if (!isActive()) {
                log.debug("Consumer {} is already stopped", this);
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("Stopping consumer {}", this);
            }

            if (this.abortStarted == 0) { // signal handle delivery to use offer
                this.abortStarted = System.currentTimeMillis();
            }

            // First mark as inactive to stop processing
            this.active.set(false);
            this.cancelled.set(true);

            // Then shutdown the executor service
            shutdownPullerExecutor();

        } finally {
            this.lifecycleLock.unlock();
        }
    }

    /**
     * Shutdown the puller executor service with proper timeout handling
     */
    private void shutdownPullerExecutor() {
        try {
            // First try a graceful shutdown
            pullerExecutor.shutdown();
            if (!pullerExecutor.awaitTermination(shutdownTimeout / 2, TimeUnit.MILLISECONDS)) {
                // If graceful shutdown doesn't complete in time, force it
                pullerExecutor.shutdownNow();
                if (!pullerExecutor.awaitTermination(shutdownTimeout / 2, TimeUnit.MILLISECONDS)) {
                    log.warn("Puller thread for {} did not terminate in time", this);
                }
            }
        } catch (InterruptedException e) {
            // Re-interrupt the thread and force shutdown
            Thread.currentThread().interrupt();
            pullerExecutor.shutdownNow();
            log.warn("Interrupted while waiting for puller thread termination", e);
        } finally {
            // Always release from active object counter
            this.activeObjectCounter.release(this);
        }
    }

    /**
     * Receive a message from the ZeroMQ socket.
     *
     * @return the message, or null if no message is available
     */
    public byte[] receiveWait() {
        if (this.socket == null) {
            log.error("Socket is null, cannot receive message");
            return null;
        }

        try {
            byte[] data = this.socket.recv();

            if (data != null) {
                // Record message statistics if event listener is available
                if (this.eventListener instanceof DefaultSocketEventListener) {
                    ZmqSocketStats stats = ((DefaultSocketEventListener) this.eventListener).getStats(this.id);
                    stats.recordMessageReceived(data.length);
                }

                if (log.isDebugEnabled()) {
                    log.debug("Received message for {}: {} bytes", this, data.length);
                }
            } else {
                int errorCode = socket.errno();
                if (errorCode != 0) {
                    log.warn("Error receiving message for {}: {}", this, ZError.toString(errorCode));

                    // Record error in statistics
                    if (this.eventListener instanceof DefaultSocketEventListener) {
                        ZmqSocketStats stats = ((DefaultSocketEventListener) this.eventListener).getStats(this.id);
                        stats.recordError();
                    }

                    // Handle specific error codes if needed
                    if (errorCode == ZError.ETERM) {
                        log.error("ZeroMQ context was terminated");
                        // Signal termination
                        this.active.set(false);
                    }
                } else if (log.isTraceEnabled()) {
                    log.trace("No message available for {}", this);
                }
            }

            return data;
        } catch (Exception e) {
            // Record error in statistics
            if (this.eventListener instanceof DefaultSocketEventListener) {
                ZmqSocketStats stats = ((DefaultSocketEventListener) this.eventListener).getStats(this.id);
                stats.recordError();
            }

            // Categorize exceptions for better handling
            if (!isActive()) {
                log.debug("Exception in receiveWait while consumer is inactive: {}", e.getMessage());
            } else {
                log.error("Error in Pull Socket: {}", e.getMessage(), e);
            }

            // Only throw exceptions that should be propagated
            throw new ZmqException("Error receiving message", e);
        }
    }

    /**
     * Set a listener for socket events.
     * This must be called before starting the consumer.
     *
     * @param eventListener the event listener
     */
    public void setSocketEventListener(ZmqSocketMonitor.SocketEventListener eventListener) {
        this.eventListener = eventListener;
    }

    /**
     * Main application-side API: wait for the next message delivery and return it.
     *
     * @return the next message
     * @throws InterruptedException       if an interrupt is received while waiting
     * @throws ConsumerCancelledException if the consumer was cancelled
     */
    public Message nextMessage() throws InterruptedException, ConsumerCancelledException {
        return nextMessage(-1);
    }

    /**
     * Main application-side API: wait for the next message delivery and return it.
     *
     * @param timeout timeout in millisecond
     * @return the next message or null if timed out
     * @throws InterruptedException       if an interrupt is received while waiting
     * @throws ConsumerCancelledException if the consumer was cancelled
     */
    public Message nextMessage(long timeout) throws InterruptedException, ConsumerCancelledException {
        // Check for cancellation before waiting
        checkCancelled();

        if (log.isTraceEnabled()) {
            log.trace("Retrieving message for {}", this);
        }

        // Get a message from the queue
        Message message;
        try {
            if (timeout < 0) {
                // Wait indefinitely
                message = this.queue.take();
            } else {
                // Wait with timeout
                message = this.queue.poll(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Check cancellation on interrupt
            checkCancelled();
            throw e;
        }

        // Check again after waiting
        if (message == null) {
            checkCancelled();
        }

        return message;
    }

    /**
     * Check if the consumer has been cancelled and throw an exception if it has.
     *
     * @throws ConsumerCancelledException if the consumer has been cancelled
     */
    private void checkCancelled() throws ConsumerCancelledException {
        if (this.cancelled.get()) {
            this.activeObjectCounter.release(this);
            throw new ConsumerCancelledException();
        }
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
        this.lifecycleLock.lock();
        try {
            // First stop all processing
            stop();

            // Clear any pending messages
            int remainingMessages = this.queue.size();
            if (remainingMessages > 0 && log.isInfoEnabled()) {
                log.info("Discarding {} messages from queue on close for {}", remainingMessages, this);
            }
            this.queue.clear();

            // Stop and close the socket monitor if it exists
            if (this.socketMonitor != null) {
                try {
                    this.socketMonitor.close();
                    log.debug("Closed socket monitor for {}", this);
                } catch (Exception e) {
                    log.warn("Error closing socket monitor for {}: {}", this, e.getMessage());
                } finally {
                    this.socketMonitor = null;
                }
            }

            // Safely close ZeroMQ resources
            if (this.socket != null) {
                try {
                    this.socket.close();
                } catch (Exception e) {
                    log.warn("Error closing ZeroMQ socket: {}", e.getMessage(), e);
                } finally {
                    this.socket = null;
                }
            }

            // We don't close the context here as it might be shared with other components
            // The context should be managed by the container that created this consumer

            if (log.isDebugEnabled()) {
                log.debug("Closed resources for {}", this);
            }
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    @Override
    public String toString() {
        return String.format("ZmqMessageConsumer[id=%s, address=%s, socketType=%s, active=%s, queueSize=%d/%d]",
                this.id,
                this.address,
                this.socketType,
                this.active.get(),
                this.queue.size(),
                this.queue.remainingCapacity() + this.queue.size());
    }
}
