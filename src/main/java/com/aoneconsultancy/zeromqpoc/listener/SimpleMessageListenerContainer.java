package com.aoneconsultancy.zeromqpoc.listener;

import com.aoneconsultancy.zeromqpoc.annotation.ZmqListener;
import com.aoneconsultancy.zeromqpoc.core.BlockingQueueConsumer;
import com.aoneconsultancy.zeromqpoc.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromqpoc.core.event.AsyncConsumerStoppedEvent;
import com.aoneconsultancy.zeromqpoc.core.event.ZmqConsumerFailedEvent;
import com.aoneconsultancy.zeromqpoc.support.ActiveObjectCounter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.zeromq.ZContext;

/**
 * Default {@link MessageListenerContainer} implementation for PULL sockets using {@link BlockingQueueConsumer}.
 * This implementation is inspired by Spring AMQP's SimpleMessageListenerContainer
 * and provides similar features like dynamic scaling of consumers, error recovery,
 * and proper lifecycle management.
 *
 * <p>Main features include:
 * <ul>
 *   <li>Configurable number of concurrent consumers</li>
 *   <li>Dynamic scaling of consumers based on load</li>
 *   <li>Error recovery with backoff</li>
 *   <li>Proper lifecycle management</li>
 *   <li>Event publishing for monitoring</li>
 * </ul>
 *
 * <p>This container is designed to be similar to Spring AMQP's SimpleMessageListenerContainer,
 * making it easy to use for developers familiar with Spring AMQP.
 *
 * <p>This container is specifically designed for PULL sockets. For other socket types,
 * extend {@link AbstractMessageListenerContainer} and implement the socket-specific logic.
 */
@Slf4j
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {

    private final Lock consumersLock = new ReentrantLock();
    private final AtomicReference<Thread> containerStoppingForAbort = new AtomicReference<>();
    private final BlockingQueue<ZmqConsumerFailedEvent> abortEvents = new LinkedBlockingQueue<>();
    private final AtomicBoolean stopNow = new AtomicBoolean();
    private final ActiveObjectCounter<BlockingQueueConsumer> cancellationLock = new ActiveObjectCounter<>();

    private Set<BlockingQueueConsumer> consumers;
    @Setter
    private ZmqSocketMonitor.SocketEventListener socketEventListener;

    public SimpleMessageListenerContainer(ZContext context) {
        super(context);
        // Default to PULL socket type
        setSocketType(ZmqListener.SocketType.PULL);
    }

    @Override
    public void setSocketType(ZmqListener.SocketType socketType) {
        if (socketType != ZmqListener.SocketType.PULL) {
            throw new IllegalArgumentException("SimpleZmqListenerContainer only supports PULL sockets. " +
                    "For other socket types, extend AbstractZmqListenerContainer and implement the socket-specific logic.");
        }
        super.setSocketType(socketType);
    }

    /**
     * Publish a consumer failed event.
     *
     * @param reason the reason for the failure
     * @param fatal  whether the failure is fatal
     * @param t      the throwable that caused the failure, if any
     */
    @Override
    protected void publishConsumerFailedEvent(String reason, boolean fatal, Throwable t) {
        try {
            if (fatal && isActive()) {
                this.abortEvents.put(new ZmqConsumerFailedEvent(this, reason, t, fatal));
            } else {
                super.publishConsumerFailedEvent(reason, fatal, t);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Implementation of the template method from AbstractZmqListenerContainer.
     * This method contains the ZMQ-specific logic for starting the container.
     */
    @Override
    protected void doStart() {
        // Initialize consumers
        super.doStart();
        this.consumersLock.lock();
        try {
            if (this.consumers != null) {
                throw new IllegalStateException("A stopped container should not have consumers");
            }
            int newConsumers = initializeConsumers();
            if (this.consumers == null) {
                log.info("Consumers were initialized and then cleared (presumably the container was stopped concurrently)");
                return;
            }
            if (newConsumers <= 0) {
                log.info("Consumers are already running");
                return;
            }

            if (log.isInfoEnabled()) {
                log.info("Started SimpleZmqListenerContainer with {} consumers", this.concurrency);
            }
        } finally {
            this.consumersLock.unlock();
        }
    }

    /**
     * Initialize the consumers.
     *
     * @return the number of consumers that were created
     */
    protected int initializeConsumers() {
        int count = 0;
        this.consumersLock.lock();
        try {
            if (this.consumers == null) {
                this.cancellationLock.reset();
                this.consumers = new HashSet<>(this.concurrency);
                Set<AsyncMessageProcessingConsumer> processors = new HashSet<>();
                for (int i = 0; i < this.concurrency; i++) {
                    AsyncMessageProcessingConsumer consumer = createAsyncZmqConsumer();
                    processors.add(consumer);
                    this.taskExecutor.execute(consumer);
                    count++;
                }
                waitForConsumersToStart(processors);
            }
        } finally {
            this.consumersLock.unlock();
        }
        return count;
    }

    private AsyncMessageProcessingConsumer createAsyncZmqConsumer() {

        String consumerId = "consumer-" + generateConsumerId();
        BlockingQueueConsumer zmqPull = new BlockingQueueConsumer(consumerId, this.context, this.cancellationLock, this.address,
                this.bufferSize,
                convertSocketType(this.socketType), taskExecutorSet);
        zmqPull.setShutdownTimeout(getShutdownTimeout());

        // Configure socket monitoring if event listener is available
        if (this.socketEventListener != null) {
            zmqPull.setSocketEventListener(this.socketEventListener);
        }

        AsyncMessageProcessingConsumer consumer = new AsyncMessageProcessingConsumer(zmqPull);
        this.consumers.add(zmqPull);
        this.cancellationLock.add(zmqPull);

        return consumer;
    }

    /**
     * Wait for the consumers to start.
     *
     * @param consumers the consumers to wait for
     */
    private void waitForConsumersToStart(Set<AsyncMessageProcessingConsumer> consumers) {
        for (AsyncMessageProcessingConsumer consumer : consumers) {
            Exception startupException = null;
            try {
                startupException = consumer.getStartupException();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted waiting for consumer to start", e);
            }
            if (startupException != null) {
                throw new RuntimeException("Fatal exception on listener startup", startupException);
            }
        }
    }

    /**
     * Shutdown and wait for consumers to finish processing.
     *
     * @param callback callback to run after shutdown
     */
    @Override
    protected void shutdownAndWaitOrCallback(@Nullable Runnable callback) {
        List<BlockingQueueConsumer> canceledConsumers = new ArrayList<>();
        this.consumersLock.lock();
        try {
            if (this.consumers != null) {
                if (isForceStop()) {
                    this.stopNow.set(true);
                }
                for (BlockingQueueConsumer consumer : this.consumers) {
                    consumer.close();
                    canceledConsumers.add(consumer);
                }
                this.consumers.clear();
                this.consumers = null;
            } else {
                log.info("Shutdown ignored - container is already stopped");
                if (callback != null) {
                    callback.run();
                }
                return;
            }
        } finally {
            this.consumersLock.unlock();
        }

        Runnable awaitShutdown = () -> {
            log.info("Waiting for workers to finish.");
            try {
                // Wait for consumers to finish
                boolean finished = this.cancellationLock.await(getShutdownTimeout(), TimeUnit.MILLISECONDS);
                if (finished) {
                    log.info("Successfully waited for workers to finish.");
                } else {
                    log.info("Workers not finished.");
                    if (this.stopNow.get()) {
                        for (BlockingQueueConsumer consumer : canceledConsumers) {
                            if (log.isWarnEnabled()) {
                                log.warn("Closing channel for unresponsive consumer: {}", consumer);
                            }
                            consumer.close();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted waiting for workers. Continuing with shutdown.");
            }

            this.cancellationLock.deactivate();
            this.stopNow.set(false);

            if (callback != null) {
                callback.run();
            }
        };

        if (callback == null) {
            awaitShutdown.run();
        } else {
            this.taskExecutor.execute(awaitShutdown);
        }
    }

    /**
     * Implementation of the template method from AbstractZmqListenerContainer.
     * This method contains the ZMQ-specific logic for stopping the container.
     */
    @Override
    protected void doStop() {
        Thread thread = this.containerStoppingForAbort.get();
        if (thread != null && !thread.equals(Thread.currentThread())) {
            log.info("Shutdown ignored - container is stopping due to an aborted consumer");
            return;
        }

        // Stop all consumers and wait for them to finish
        shutdownAndWaitOrCallback(() -> {
            // Clear the task executor reference
            // Note: We don't explicitly shut down the executor as it might be shared
            // and the Executor interface doesn't provide shutdown methods
            this.taskExecutor = null;

            // Process any pending abort events
            if (thread != null) {
                processAbortEvents();
            }
        });
    }

    /**
     * Process any pending abort events.
     */
    private void processAbortEvents() {
        ZmqConsumerFailedEvent event = null;
        do {
            try {
                event = this.abortEvents.poll(5, TimeUnit.SECONDS);
                if (event != null) {
                    log.error("Consumer failed: {}", event.getReason(), event.getThrowable());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        while (event != null);
    }

    /**
     * Check if the container should force stop.
     *
     * @return true if the container should force stop
     */
    protected boolean isForceStop() {
        return false;
    }

    /**
     * Check if the consumer is active.
     *
     * @param consumer the consumer to check
     * @return true if the consumer is active
     */
    private boolean isActive(BlockingQueueConsumer consumer) {
        boolean consumerActive;
        this.consumersLock.lock();
        try {
            consumerActive = this.consumers != null && this.consumers.contains(consumer);
        } finally {
            this.consumersLock.unlock();
        }
        return consumerActive && this.isActive();
    }

    /**
     * Generate a unique consumer ID.
     *
     * @return a unique consumer ID
     */
    private String generateConsumerId() {
        return String.valueOf(System.currentTimeMillis() % 10000) + "-" +
                String.valueOf(System.nanoTime() % 10000);
    }

    /**
     * Restart this consumer by stopping the old one, creating a new one, and starting it.
     * Similar to Spring AMQP's SimpleMessageListenerContainer.restart() method.
     */
    public void restart(BlockingQueueConsumer oldConsumer) {

        BlockingQueueConsumer consumer = oldConsumer;
        this.consumersLock.lock();
        try {
            if (this.consumers != null) {
                consumer.close();
                cancellationLock.release(consumer);
                // Remove this consumer from the set
                this.consumers.remove(consumer);
                if (!isActive()) {
                    // Do not restart - container is stopping
                    return;
                }
                // Create a new consumer with the same address
                String consumerId = "consumer-" + generateConsumerId();
                consumer = new BlockingQueueConsumer(
                        consumerId, context, this.cancellationLock, address, bufferSize, convertSocketType(socketType)
                        , taskExecutorSet);

                // Create a new AsyncZmqConsumer with the new consumer
                AsyncMessageProcessingConsumer newAsyncConsumer = new AsyncMessageProcessingConsumer(consumer);

                // Add the new consumer to the set
                consumers.add(consumer);
                cancellationLock.add(consumer);

                // Start the new consumer
                getTaskExecutor().execute(newAsyncConsumer);

                if (log.isDebugEnabled()) {
                    log.debug("Restarted consumer: {} -> {}", oldConsumer, consumer);
                }

                // Wait for the new consumer to start or fail
                try {
                    Exception startupException = newAsyncConsumer.getStartupException();
                    if (startupException != null) {
                        consumers.remove(consumer);
                        cancellationLock.release(consumer);
                        throw new RuntimeException("Failed to start consumer", startupException);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    consumers.remove(consumer);
                    cancellationLock.release(consumer);
                    throw new RuntimeException("Interrupted while waiting for consumer to start", e);
                } catch (Exception e) {
                    consumers.remove(consumer);
                    cancellationLock.release(consumer);
                    throw new RuntimeException("Error starting consumer", e);
                }
            }
        } finally {
            consumersLock.unlock();
        }
    }

    /**
     * Async wrapper for ZmqMessageConsumer that handles message processing in a separate thread.
     * Similar to Spring AMQP's AsyncMessageProcessingConsumer.
     */
    private final class AsyncMessageProcessingConsumer implements Runnable {

        private static final int ABORT_EVENT_WAIT_SECONDS = 5;
        @Getter
        private final BlockingQueueConsumer consumer;
        private final CountDownLatch start = new CountDownLatch(1);
        private volatile Exception startupException;

        /**
         * Create a new AsyncZmqConsumer with the given consumer and message listener.
         *
         * @param consumer the ZmqMessageConsumer consumer
         */
        public AsyncMessageProcessingConsumer(BlockingQueueConsumer consumer) {
            this.consumer = consumer;
        }

        /**
         * Get the startup exception if one occurred.
         *
         * @return the startup exception, or null if none occurred
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        public Exception getStartupException() throws InterruptedException {
            if (!this.start.await(
                    getConsumerStartTimeout(), TimeUnit.MILLISECONDS)) {
                log.error("Consumer failed to start in {} milliseconds; does the task executor have enough threads to support the container concurrency?", getConsumerStartTimeout());
            }
            return this.startupException;
        }

        /**
         * Stop the consumer.
         */
        public void stop() {
            if (this.consumer != null) {
                this.consumer.close();
            }
        }

        @Override
        public void run() {

            if (!isActive()) {
                this.start.countDown();
                return;
            }

            boolean aborted = false;

            try {
                initialize();
                // Main processing loop
                while (isActive(this.consumer) || this.consumer.hasDelivery() || !this.consumer.cancelled()) {
                    aborted = mainLoop();
                }
            } catch (Error e) { //NOSONAR
                log.error("Consumer thread error, thread abort.", e);
                publishConsumerFailedEvent("Consumer threw an Error", true, e);
                aborted = true;
            } catch (Exception e) {
                this.startupException = e;
                publishConsumerFailedEvent("Error starting consumer: " + e.getMessage(), true, e);
                aborted = true;
            } finally {
                this.start.countDown();
                cancellationLock.release(this.consumer);
            }

            killOrRestart(aborted);
        }

        private boolean mainLoop() {

            boolean aborted = false;
            try {
                boolean receivedOk = receiveAndExecute(this.consumer); // At least one message received
                if (receivedOk) {
                    updateLastReceive();
                }
            } catch (RuntimeException e) {
                Thread.currentThread().interrupt();
                aborted = true;
            } catch (Exception e) {
                // Log the error
                log.error("Error processing message: {}", e.getMessage(), e);
                // Publish a consumer failed event
                publishConsumerFailedEvent("Consumer raised exception, attempting restart", false, e);
            }
            return aborted;
        }

        private void killOrRestart(boolean aborted) {
            if (!isActive(this.consumer) || aborted) {
                log.debug("Cancelling {}", this.consumer);
                try {
                    stop();
                    cancellationLock.release(this.consumer);
                    ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
                    if (applicationEventPublisher != null && !isApplicationContextClosed()) {
                        applicationEventPublisher.publishEvent(
                                new AsyncConsumerStoppedEvent(SimpleMessageListenerContainer.this, this.consumer));
                    }
                } catch (Exception e) {
                    log.info("Could not cancel message consumer", e);
                }
                // TODO - add aborted logic
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Restarting {}", this.consumer);
                }
                restart(this.consumer);
            }
        }

        /**
         * Initialize the consumer.
         */
        private void initialize() {
            try {
                // Start the consumer
                this.consumer.start();
                this.start.countDown();
            } catch (Exception e) {
                this.startupException = e;
                this.start.countDown();
                throw e;
            }
        }
    }

}
