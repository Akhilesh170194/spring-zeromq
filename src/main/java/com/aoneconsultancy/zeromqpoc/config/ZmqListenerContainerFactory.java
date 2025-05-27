package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.listener.ZmqListenerContainer;
import java.util.function.Consumer;

/**
 * Factory for creating {@link ZmqListenerContainer} instances.
 */
public interface ZmqListenerContainerFactory<T extends ZmqListenerContainer> {

    /**
     * Create a listener container configured with the given message listener.
     * @param listener consumer for received bytes
     * @return new container instance
     */
    T createContainer(Consumer<byte[]> listener);
}
