package com.aoneconsultancy.zeromq.config;

import com.aoneconsultancy.zeromq.core.MessageListener;
import com.aoneconsultancy.zeromq.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.listener.MessageListenerContainer;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

/**
 * A simple {@link MessageListenerContainer} implementation for testing.
 * <p>
 * This implementation doesn't actually connect to ZeroMQ, but it does track
 * whether it's been started and stopped, and it provides access to the endpoint
 * that was used to create it.
 */
public class MessageListenerTestContainer implements MessageListenerContainer {

    @Getter
    private final ZmqListenerEndpoint endpoint;
    private boolean startInvoked;
    private boolean stopInvoked;
    private MessageListener messageListener;
    private MessageConverter messageConverter;
    private int concurrency = 1;
    private ErrorHandler errorHandler;
    private ZmqSocketMonitor.SocketEventListener socketEventListener;

    @Setter
    @Getter
    private ZmqConsumerProperties zmqConsumerProps;

    public MessageListenerTestContainer(ZmqListenerEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Start the container.
     */
    @Override
    public void start() {
        this.startInvoked = true;
    }

    /**
     * Stop the container.
     */
    @Override
    public void stop() {
        this.stopInvoked = true;
    }

    /**
     * Return whether the container has been started.
     *
     * @return true if the container has been started
     */
    public boolean isStarted() {
        return this.startInvoked;
    }

    /**
     * Return whether the container has been stopped.
     *
     * @return true if the container has been stopped
     */
    public boolean isStopped() {
        return this.stopInvoked;
    }

    /**
     * Set the message listener for this container.
     *
     * @param messageListener the message listener
     */
    @Override
    public void setupMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    /**
     * Return the message listener for this container.
     *
     * @return the message listener
     */
    @Override
    @Nullable
    public Object getMessageListener() {
        return this.messageListener;
    }

    @Override
    public void setMessageConverter(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Override
    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void setSocketEventListener(ZmqSocketMonitor.SocketEventListener socketEventListener) {
        this.socketEventListener = socketEventListener;
    }

    @Override
    public boolean isRunning() {
        return this.startInvoked && !this.stopInvoked;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
