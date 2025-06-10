package com.aoneconsultancy.zeromq.listener;

import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint;
import org.springframework.lang.Nullable;

/**
 * Factory for creating {@link MessageListenerContainer} instances.
 * Similar to Spring AMQP's AbstractMessageListenerContainerFactory,
 * this provides configuration options for creating listener containers.
 */
public interface ZmqListenerContainerFactory<T extends MessageListenerContainer> {


    /**
     * Create a {@link MessageListenerContainer} for the given
     * {@link ZmqListenerEndpoint}.
     *
     * @param endpoint the endpoint to configure.
     * @return the created container.
     */
    T createListenerContainer(@Nullable ZmqListenerEndpoint endpoint);

    /**
     * Create a {@link MessageListenerContainer}.
     *
     * @return the created container.
     * @since 2.1.
     */
    default T createListenerContainer() {
        return createListenerContainer(null);
    }
}
