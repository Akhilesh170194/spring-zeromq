package com.aoneconsultancy.zeromq.listener.endpoint;

import com.aoneconsultancy.zeromq.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromq.listener.MessageListenerContainer;
import java.util.List;
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
     * Return the addresses of this endpoint, if any.
     *
     * @return the address of this endpoint, or null
     */
    @Nullable
    List<String> getAddresses();

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
     * Setup the specified message listener container with the model
     * defined by this endpoint.
     *
     * @param container the container to configure
     */
    void setupListenerContainer(MessageListenerContainer container);

    Executor getTaskExecutor();

    Boolean getConsumerBatchEnabled();

    void setConsumerBatchEnabled(Boolean consumerBatchEnabled);

    /**
     * Return the socket event listener of this endpoint, if any.
     *
     * @return the socket event listener of this endpoint, or null
     */
    @Nullable
    ZmqSocketMonitor.SocketEventListener getSocketEventListener();

    /**
     * Set the socket event listener for this endpoint.
     *
     * @param socketEventListener the socket event listener
     */
    default void setSocketEventListener(ZmqSocketMonitor.SocketEventListener socketEventListener) {
        // NOSONAR
    }
}
