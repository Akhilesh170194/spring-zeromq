package com.aoneconsultancy.zeromq.listener;

import com.aoneconsultancy.zeromq.core.BlockingQueueConsumer;
import com.aoneconsultancy.zeromq.core.event.AsyncConsumerStoppedEvent;
import com.aoneconsultancy.zeromq.core.event.ZmqConsumerFailedEvent;
import com.aoneconsultancy.zeromq.support.ActiveObjectCounter;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

@Slf4j
public class PullZmqMessageListenerContainer extends AbstractMessageListenerContainer {

    private final Lock consumersLock = new ReentrantLock();
    private final AtomicReference<Thread> containerStoppingForAbort = new AtomicReference<>();
    private final BlockingQueue<ZmqConsumerFailedEvent> abortEvents = new LinkedBlockingQueue<>();
    private final AtomicBoolean stopNow = new AtomicBoolean();
    private final ActiveObjectCounter<BlockingQueueConsumer> cancellationLock = new ActiveObjectCounter<>();

    private Set<BlockingQueueConsumer> consumers;

    public PullZmqMessageListenerContainer(ZContext context) {
        super(context);
    }

    @Override
    public void setSocketType(SocketType socketType) {
        if (socketType != SocketType.PULL) {
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

                // Calculate the total number of consumers based on concurrency and number of addresses
                int totalConsumers = this.concurrency * this.endpoints.size();
                this.consumers = new HashSet<>(totalConsumers);
                Set<AsyncMessageProcessingConsumer> processors = new HashSet<>();

                // Create consumers for each address
                for (String endpoint : this.endpoints) {
                    for (int i = 0; i < this.concurrency; i++) {
                        String id = "consumer-" + getListenerId() + "-" + i;
                        AsyncMessageProcessingConsumer consumer = createAsyncZmqConsumer(id, endpoint);
                        processors.add(consumer);
                        this.taskExecutor.execute(consumer);
                        count++;
                    }
                }
                waitForConsumersToStart(processors);
            }
        } finally {
            this.consumersLock.unlock();
        }
        return count;
    }

    private AsyncMessageProcessingConsumer createAsyncZmqConsumer(String id, String address) {

        BlockingQueueConsumer zmqMsgPuller = new BlockingQueueConsumer(id, this.context, this.cancellationLock, address,
                this.recvHwm, this.socketType, taskExecutorSet);
        zmqMsgPuller.setShutdownTimeout(getShutdownTimeout());
        zmqMsgPuller.setBind(this.bind);
        zmqMsgPuller.setConsumeDelay(1000);
        zmqMsgPuller.setSocketLinger(this.socketLinger);
        zmqMsgPuller.setSocketRecvBuffer(this.socketRecvBuffer);
        zmqMsgPuller.setSocketBackoff(this.socketBackoff);
        zmqMsgPuller.setSocketReconnectInterval(this.socketReconnectInterval);

        AsyncMessageProcessingConsumer consumer = new AsyncMessageProcessingConsumer(zmqMsgPuller);
        this.consumers.add(zmqMsgPuller);
        this.cancellationLock.add(zmqMsgPuller);

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
                    // Handle the failed event, e.g., notify an external system or perform cleanup
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

        String endpoint = oldConsumer.getEndpoint();
        String id = oldConsumer.getId();
        this.consumersLock.lock();
        try {
            if (this.consumers != null) {
                oldConsumer.close();
                cancellationLock.release(oldConsumer);
                // Remove this consumer from the set
                this.consumers.remove(oldConsumer);
                if (!isActive()) {
                    // Do not restart - container is stopping
                    return;
                }
                // Create a new AsyncZmqConsumer with the new consumer
                AsyncMessageProcessingConsumer newAsyncConsumer = createAsyncZmqConsumer(id, endpoint);
                // Start the new consumer
                getTaskExecutor().execute(newAsyncConsumer);

                if (log.isDebugEnabled()) {
                    log.debug("Restarted consumer: {} -> {}", oldConsumer, oldConsumer);
                }

                // Wait for the new consumer to start or fail
                try {
                    Exception startupException = newAsyncConsumer.getStartupException();
                    if (startupException != null) {
                        consumers.remove(oldConsumer);
                        cancellationLock.release(oldConsumer);
                        throw new RuntimeException("Failed to start consumer", startupException);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    consumers.remove(oldConsumer);
                    cancellationLock.release(oldConsumer);
                    throw new RuntimeException("Interrupted while waiting for consumer to start", e);
                } catch (Exception e) {
                    consumers.remove(oldConsumer);
                    cancellationLock.release(oldConsumer);
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
                                new AsyncConsumerStoppedEvent(PullZmqMessageListenerContainer.this, this.consumer));
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
