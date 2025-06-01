package com.aoneconsultancy.zeromqpoc.listener.endpoint;

import com.aoneconsultancy.zeromqpoc.annotation.ZmqListener;
import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromqpoc.listener.MessageListenerContainer;
import java.util.concurrent.Executor;
import org.springframework.lang.Nullable;

/**
 * Model for a message listener endpoint.
 * This is a simplified version of ZmqListenerEndpoint.
 */
public interface ZmqListenerEndpoint {

    /**
     * Return the id of this endpoint.
     *
     * @return the id of this endpoint
     */
    String getId();

    /**
     * Return the concurrency of this endpoint, if any.
     *
     * @return the concurrency of this endpoint, or null
     */
    @Nullable
    Integer getConcurrency();

    /**
     * Return the address of this endpoint, if any.
     *
     * @return the address of this endpoint, or null
     */
    @Nullable
    String getAddress();

    /**
     * Return the socket type of this endpoint, if any.
     *
     * @return the socket type of this endpoint, or null
     */
    @Nullable
    ZmqListener.SocketType getSocketType();

    /**
     * Return the error handler of this endpoint, if any.
     *
     * @return the error handler of this endpoint, or null
     */
    @Nullable
    ZmqListenerErrorHandler getErrorHandler();

    /**
     * The preferred way for a container factory to pass a message converter
     * to the endpoint's adapter.
     *
     * @param converter the converter.
     * @since 2.0.8
     */
    default void setMessageConverter(MessageConverter converter) {
        // NOSONAR
    }

    /**
     * Return the message converter of this endpoint, if any.
     *
     * @return the message converter of this endpoint, or null
     */
    @Nullable
    MessageConverter getMessageConverter();

    /**
     * Return the auto startup flag of this endpoint.
     *
     * @return the auto startup flag of this endpoint
     */
    boolean isAutoStartup();

    /**
     * Setup the specified message listener container with the model
     * defined by this endpoint.
     *
     * @param container the container to configure
     */
    void setupListenerContainer(MessageListenerContainer container);

    Executor getTaskExecutor();

    Boolean getBatchListener();

    void setBatchListener(Boolean batchListener);

}