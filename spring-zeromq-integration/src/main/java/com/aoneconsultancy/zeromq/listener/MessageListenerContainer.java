package com.aoneconsultancy.zeromq.listener;

import com.aoneconsultancy.zeromq.annotation.ZmqListener;
import com.aoneconsultancy.zeromq.core.MessageListener;
import com.aoneconsultancy.zeromq.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

/**
 * Abstraction representing a ZeroMQ message listener container.
 * Similar to Spring AMQP's MessageListenerContainer, this provides
 * lifecycle methods and configuration options for message listeners.
 */
public interface MessageListenerContainer extends SmartLifecycle, InitializingBean {

    /**
     * Setup the message listener to use. Throws an {@link IllegalArgumentException}
     * if that message listener type is not supported.
     *
     * @param messageListener the {@code object} to wrapped to the {@code MessageListener}.
     */
    void setupMessageListener(MessageListener messageListener);

    /**
     * Set the message converter to use for converting received messages.
     *
     * @param messageConverter the message converter
     */
    void setMessageConverter(MessageConverter messageConverter);

    /**
     * Set the socket addresses to connect to.
     *
     * @param addresses the socket address
     */
    void setAddresses(List<String> addresses);

    /**
     * Set the socket type to use.
     *
     * @param socketType the socket type
     */
    void setSocketType(ZmqListener.SocketType socketType);

    /**
     * Set the concurrency for this listener (number of threads).
     *
     * @param concurrency the concurrency
     */
    void setConcurrency(int concurrency);

    /**
     * Get the message listener.
     *
     * @return The message listener object.
     * @since 2.4
     */
    @Nullable
    Object getMessageListener();

    /**
     * Start listening for messages.
     */
    void start();

    /**
     * Stop listening for messages.
     */
    void stop();

    /**
     * @return whether the container is currently running
     */
    boolean isRunning();

    void setListenerId(String id);

    @Override
    default void afterPropertiesSet() {
    }

    void setErrorHandler(ErrorHandler errorHandler);

    void setSocketEventListener(ZmqSocketMonitor.SocketEventListener socketEventListener);
}
