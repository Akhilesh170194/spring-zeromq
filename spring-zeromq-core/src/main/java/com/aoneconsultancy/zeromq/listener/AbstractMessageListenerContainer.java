package com.aoneconsultancy.zeromq.listener;

import com.aoneconsultancy.zeromq.config.ZmqConsumerProperties;
import com.aoneconsultancy.zeromq.core.BlockingQueueConsumer;
import com.aoneconsultancy.zeromq.core.DefaultSocketEventListener;
import com.aoneconsultancy.zeromq.core.MessageListener;
import com.aoneconsultancy.zeromq.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.listener.exception.MessageRejectedWhileStoppingException;
import com.aoneconsultancy.zeromq.support.ListenerExecutionFailedException;
import com.aoneconsultancy.zeromq.support.ZmqException;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractMessageListenerContainer extends ObservableListenerContainer
        implements ApplicationEventPublisherAware {

    protected static final long DEFAULT_RECEIVE_TIMEOUT = 1000;

    public static final long DEFAULT_SHUTDOWN_TIMEOUT = 5000;

    protected final ZContext context;
    protected final Lock lifecycleLock = new ReentrantLock();

    @Getter
    @Setter
    private ZmqConsumerProperties zmqConsumerProps = new ZmqConsumerProperties();

    @Getter
    private volatile boolean active = false;

    @Getter
    private volatile boolean running = false;

    @Getter
    private boolean initialized = false;

    @Getter
    private final ContainerDelegate delegate = this::actualInvokeListener;

    @Setter
    @Getter
    protected MessageConverter messageConverter;

    @Setter
    @Getter
    private MessageListener messageListener;

    private Collection<MessagePostProcessor> afterReceivePostProcessors;

    @Setter
    protected ErrorHandler errorHandler;

    @Setter
    protected long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;

    // Tracking for dynamic scaling
    protected volatile long lastReceive = System.currentTimeMillis();

    @Setter
    protected int recvHwm = 1000;

    @Setter
    protected int socketLinger = 0;

    @Setter
    protected int socketRecvBuffer = 1024;

    @Setter
    protected int socketReconnectInterval = 5000;

    @Setter
    protected long socketBackoff = 100;

    @Getter
    // Thread pool for message processing
    protected Executor taskExecutor = new SimpleAsyncTaskExecutor();
    protected boolean taskExecutorSet;

    @Setter
    protected int concurrency = 1;

    @Setter
    @Getter
    private ApplicationEventPublisher applicationEventPublisher;

    @Setter
    private boolean consumerBatchEnabled = false;

    protected int batchSize = 1;

    @Setter
    private Long batchReceiveTimeout = 0L;

    @Setter
    protected ZmqSocketMonitor.SocketEventListener socketEventListener = new DefaultSocketEventListener(applicationEventPublisher);

    @Getter
    private long shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

    @Getter
    private long consumerStartTimeout = 10000;

    /**
     * Set the task executor.
     *
     * @param taskExecutor the executor
     */
    public void setTaskExecutor(Executor taskExecutor) {
        Assert.isTrue(taskExecutor != null, "'taskExecutor' must not be null");
        this.taskExecutor = taskExecutor;
        this.taskExecutorSet = true;
    }

    /**
     * Set the batch size for this container.
     * <p>This property has several functions:
     * <p>It determines how many messages to process in a single batch.
     * <p>When {@link #setConsumerBatchEnabled(boolean)} is true, it determines how
     * many records to include in the batch.
     * <p>Default is 1.
     *
     * @param batchSize the batch size
     */
    public void setBatchSize(int batchSize) {
        Assert.isTrue(batchSize > 0, "'batchSize' must be > 0");
        this.batchSize = batchSize;
    }

    /**
     * Create a new AbstractZmqListenerContainer with the given ZContext.
     *
     * @param context the ZContext to use
     */
    public AbstractMessageListenerContainer(ZContext context) {
        this.context = context;
    }

    /**
     * Update the last receiver timestamp.
     */
    protected void updateLastReceive() {
        this.lastReceive = System.currentTimeMillis();
    }

    @Override
    public void setupMessageListener(MessageListener messageListener) {
        setMessageListener(messageListener);
    }

    /**
     * Publish a consumer failed event.
     *
     * @param reason the reason for the failure
     * @param fatal  whether the failure is fatal
     * @param t      the throwable that caused the failure, if any
     */
    protected void publishConsumerFailedEvent(String reason, boolean fatal, Throwable t) {
        log.error("Consumer failed: {}", reason, t);
    }

    /**
     * Set {@link MessagePostProcessor}s that will be applied after message reception, before
     * invoking the {@link MessageListener}. Often used to decompress data.  Processors are invoked in order,
     * depending on {@code PriorityOrder}, {@code Order} and finally unordered.
     *
     * @param afterReceivePostProcessors the post processor.
     * @see #addAfterReceivePostProcessors(MessagePostProcessor...)
     * @since 1.4.2
     */
    public void setAfterReceivePostProcessors(MessagePostProcessor... afterReceivePostProcessors) {
        org.springframework.util.Assert.notNull(afterReceivePostProcessors, "'afterReceivePostProcessors' cannot be null");
        org.springframework.util.Assert.noNullElements(afterReceivePostProcessors, "'afterReceivePostProcessors' cannot have null elements");
        this.afterReceivePostProcessors = MessagePostProcessorUtils.sort(Arrays.asList(afterReceivePostProcessors));
    }

    /**
     * Add {@link MessagePostProcessor}s that will be applied after message reception, before
     * invoking the {@link MessageListener}. Often used to decompress data.  Processors are invoked in order,
     * depending on {@code PriorityOrder}, {@code Order} and finally unordered.
     * <p>
     * In contrast to {@link #setAfterReceivePostProcessors(MessagePostProcessor...)}, this
     * method does not override the previously added afterReceivePostProcessors.
     *
     * @param postprocessors the post processor.
     * @since 2.1.4
     */
    public void addAfterReceivePostProcessors(MessagePostProcessor... postprocessors) {
        org.springframework.util.Assert.notNull(postprocessors, "'afterReceivePostProcessors' cannot be null");
        if (this.afterReceivePostProcessors == null) {
            this.afterReceivePostProcessors = new ArrayList<>();
        }
        this.afterReceivePostProcessors.addAll(Arrays.asList(postprocessors));
        this.afterReceivePostProcessors = MessagePostProcessorUtils.sort(this.afterReceivePostProcessors);
    }

    /**
     * Remove the provided {@link MessagePostProcessor} from the {@link #afterReceivePostProcessors} list.
     *
     * @param afterReceivePostProcessor the MessagePostProcessor to remove.
     * @return the boolean if the provided post processor has been removed.
     * @see #addAfterReceivePostProcessors(MessagePostProcessor...)
     * @since 2.1.4
     */
    public boolean removeAfterReceivePostProcessor(MessagePostProcessor afterReceivePostProcessor) {
        org.springframework.util.Assert.notNull(afterReceivePostProcessor, "'afterReceivePostProcessor' cannot be null");
        if (this.afterReceivePostProcessors != null) {
            return this.afterReceivePostProcessors.remove(afterReceivePostProcessor);
        }
        return false;
    }

    /**
     * Start this container.
     * <p>This method implements the basic lifecycle start method.
     * The {@link #doStart()} method contains the actual implementation.
     */
    @Override
    public void start() {
        if (isRunning()) {
            return;
        }

        if (!this.initialized) {
            this.lifecycleLock.lock();
            try {
                afterPropertiesSet();
                doStart();
            } finally {
                this.lifecycleLock.unlock();
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Started ZmqListenerContainer");
        }

    }

    @Override
    public void afterPropertiesSet() {

        super.afterPropertiesSet();

        // Initialize the task executor if not provided
        if (this.taskExecutor == null && StringUtils.hasText(getListenerName())) {
            this.taskExecutor = new SimpleAsyncTaskExecutor(getListenerName() + "-");
            this.taskExecutorSet = true;
        }

        this.initialized = true;
    }

    /**
     * Start this container, and notify all invoker tasks.
     */
    protected void doStart() {
        // Reschedule paused tasks, if any.
        this.lifecycleLock.lock();
        try {
            this.active = true;
            this.running = true;
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    /**
     * Stop this container.
     * <p>This method implements the basic lifecycle stop method.
     * The {@link #doStop()} method contains the actual implementation.
     */
    @Override
    public void stop() {
        try {
            shutdown(null);
            this.initialized = false;
        } finally {
            setNotRunning();
        }
    }

    /**
     * Stop the shared Connection, call {@link #shutdownAndWaitOrCallback(Runnable)}, and
     * close this container.
     *
     * @param callback an optional {@link Runnable} to call when the stop is complete.
     */
    public void shutdown(@Nullable Runnable callback) {
        this.lifecycleLock.lock();
        try {
            if (!isActive()) {
                log.debug("Shutdown ignored - container is not active already");
                if (callback != null) {
                    callback.run();
                }
                return;
            }
            this.active = false;
        } finally {
            this.lifecycleLock.unlock();
        }

        log.debug("Shutting down ZMQ listener container");

        // Shut down the invokers.
        try {
            shutdownAndWaitOrCallback(callback);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            setNotRunning();
        }
    }

    /**
     * Determine whether this container is currently running, that is, whether it has been started and not stopped yet.
     *
     * @see #start()
     * @see #stop()
     */
    @Override
    public final boolean isRunning() {
        return this.running;
    }

    protected void setNotRunning() {
        this.lifecycleLock.lock();
        try {
            this.running = false;
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    /**
     * Subclasses must implement this method to perform the actual shutdown logic.
     *
     * @param callback callback to run after shutdown
     */
    protected abstract void shutdownAndWaitOrCallback(@Nullable Runnable callback);

    /**
     * Template method that gets called when the container is stopped.
     * <p>Subclasses must implement this method to perform the actual stop logic.
     */
    protected abstract void doStop();

    /**
     * Simple utility class for assertions.
     */
    protected static class Assert {
        /**
         * Assert that an expression is true.
         *
         * @param expression the expression to check
         * @param message    the message to throw if the assertion fails
         */
        public static void isTrue(boolean expression, String message) {
            if (!expression) {
                throw new IllegalArgumentException(message);
            }
        }
    }

    protected void actualInvokeListener(Object data) {
        Object listener = getMessageListener();
        if (listener instanceof MessageListener msgListener) {
            doInvokeListener(msgListener, data);
        }
    }

    /**
     * Invoke the specified listener as Spring Rabbit MessageListener.
     * <p>
     * Default implementation performs a plain invocation of the <code>onMessage</code> method.
     * <p>
     * Exception thrown from listener will be wrapped to {@link ListenerExecutionFailedException}.
     *
     * @param listener the Rabbit MessageListener to invoke
     * @param data     the received Rabbit Message or List of Message.
     * @see MessageListener#onMessage
     */
    @SuppressWarnings("unchecked")
    protected void doInvokeListener(MessageListener listener, Object data) {
        Message message = null;
        try {
            if (data instanceof List) {
                listener.onMessageBatch((List<Message>) data);
            } else {
                message = (Message) data;
                listener.onMessage(message);
            }
        } catch (Exception e) {
            throw wrapToListenerExecutionFailedExceptionIfNeeded(e, data);
        }
    }

    /**
     * Handle the given exception that arose during listener execution.
     * <p>
     * The default implementation logs the exception at error level, not propagating it to the Rabbit provider -
     * assuming that all handling of acknowledgment and/or transactions is done by this listener container. This can be
     * overridden in subclasses.
     *
     * @param ex the exception to handle
     */
    protected void handleListenerException(Throwable ex) {
        if (isActive()) {
            // Regular case: failed while active.
            // Invoke ErrorHandler if available.
            invokeErrorHandler(ex);
        } else {
            // Rare case: listener thread failed after container shutdown.
            // Log at debug level, to avoid spamming the shutdown log.
            log.debug("Listener exception after container shutdown", ex);
        }
    }

    /**
     * Invoke the registered ErrorHandler, if any. Log at error level otherwise.
     *
     * @param ex the uncaught error that arose during Rabbit processing.
     * @see #setErrorHandler
     */
    protected void invokeErrorHandler(Throwable ex) {
        if (this.errorHandler != null) {
            try {
                this.errorHandler.handleError(ex);
            } catch (Exception e) {
                log.error(
                        "Execution of Rabbit message listener failed, and the error handler threw an exception", e);
                throw e;
            }
        } else {
            log.warn("Execution of Rabbit message listener failed, and no ErrorHandler has been set.", ex);
        }
    }

    /**
     * @param e    The Exception.
     * @param data The failed message.
     * @return If 'e' is of type {@link ListenerExecutionFailedException} - return 'e' as it is, otherwise wrap it to
     * {@link ListenerExecutionFailedException} and return.
     */
    @SuppressWarnings("unchecked")
    protected ListenerExecutionFailedException wrapToListenerExecutionFailedExceptionIfNeeded(Exception e,
                                                                                              Object data) {

        if (!(e instanceof ListenerExecutionFailedException)) {
            // Wrap exception to ListenerExecutionFailedException.
            if (data instanceof List) {
                return new ListenerExecutionFailedException("Listener threw exception", e,
                        ((List<Message>) data).toArray(new Message[0]));
            } else {
                return new ListenerExecutionFailedException("Listener threw exception", e, (Message) data);
            }
        }
        return (ListenerExecutionFailedException) e;
    }

    @FunctionalInterface
    private interface ContainerDelegate {
        void invokeListener(Object data);
    }

    public boolean receiveAndExecute(final BlockingQueueConsumer consumer) throws Exception { // NOSONAR
        return doReceiveAndExecute(consumer);
    }

    private boolean doReceiveAndExecute(BlockingQueueConsumer consumer) throws Exception { //NOSONAR

        List<Message> messages = null;
        boolean isBatchReceiveTimeoutEnabled = this.batchReceiveTimeout > 0;
        long startTime = isBatchReceiveTimeoutEnabled ? System.currentTimeMillis() : 0;
        for (int i = 0; i < this.batchSize; i++) {
            boolean batchTimedOut = isBatchReceiveTimeoutEnabled &&
                    (System.currentTimeMillis() - startTime) > this.batchReceiveTimeout;
            if (batchTimedOut) {
                if (log.isTraceEnabled()) {
                    long gathered = messages != null ? messages.size() : 0;
                    log.trace("Timed out for gathering batch messages. gathered size is {}", gathered);
                }
                break;
            }

            log.trace("Waiting for message from consumer.");
            Message message = consumer.nextMessage(this.receiveTimeout);
            if (message == null) {
                break;
            }
            if (this.consumerBatchEnabled) {
                Collection<MessagePostProcessor> afterReceivePostProcessors = this.afterReceivePostProcessors;
                if (afterReceivePostProcessors != null) {
                    Message original = message;
                    for (MessagePostProcessor processor : afterReceivePostProcessors) {
                        message = processor.postProcessMessage(message);
                        if (message == null) {
                            if (log.isDebugEnabled()) {
                                log.debug(
                                        "Message Post Processor returned 'null', discarding message " + original);
                            }
                            break;
                        }
                    }
                }
                if (message != null) {
                    if (messages == null) {
                        messages = new ArrayList<>(this.batchSize);
                    }
                    messages.add(message);
                }
            } else {
                if (messages != null) {
                    break;
                }
                try {
                    executeListener(message);
                } catch (Exception ex) {
                    log.error("");
                }
            }
        }
        if (messages != null) {
            executeWithList(messages, consumer);
        }

        return true;
    }

    /**
     * Execute the specified listener, committing or rolling back the transaction afterwards (if necessary).
     *
     * @param data the received Rabbit Message
     */
    protected void executeListener(Object data) {
        if (data instanceof Message message) {
            executeListenerAndHandleException(message);
        } else if (data instanceof List<?> messages) {
            executeListenerAndHandleException(messages);
        } else {
            executeListenerAndHandleException(data);
        }
    }

    protected void executeListenerAndHandleException(Object data) {
        if (!isRunning()) {
            if (log.isWarnEnabled()) {
                log.warn("Rejecting received message(s) because the listener container has been stopped: {}", data);
            }
            throw new MessageRejectedWhileStoppingException();
        }
        try {
            doExecuteListener(data);
        } catch (RuntimeException ex) {
            handleListenerException(ex);
            throw ex;
        }
    }

    private void doExecuteListener(Object data) {
        if (data instanceof Message message) {
            if (this.afterReceivePostProcessors != null) {
                for (MessagePostProcessor processor : this.afterReceivePostProcessors) {
                    message = processor.postProcessMessage(message);
                    if (message == null) {
                        throw new ZmqException(
                                "Message Post Processor returned 'null', discarding message");
                    }
                }
            } else {
                invokeListener(message);
            }
        } else {
            invokeListener(data);
        }
    }

    protected void invokeListener(Object data) {
        this.delegate.invokeListener(data);
    }

    private void executeWithList(List<Message> messages, BlockingQueueConsumer consumer) {

        try {
            executeListener(messages);
        } catch (Exception ex) {
            log.error("Error while executing batch listener", ex);
            throw new ZmqException(ex);
        }
    }

}
