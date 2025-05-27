package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.service.ZmqService;

import com.aoneconsultancy.zeromqpoc.listener.SimpleZmqListenerContainer;
import java.util.function.Consumer;

/**
 * Default factory that creates {@link SimpleZmqListenerContainer} instances.
 */
public class SimpleZmqListenerContainerFactory implements ZmqListenerContainerFactory<SimpleZmqListenerContainer> {

    private final ZmqService zmqService;
    private final ZmqProperties properties;

    public SimpleZmqListenerContainerFactory(ZmqService zmqService, ZmqProperties properties) {
        this.zmqService = zmqService;
        this.properties = properties;
    }

    @Override
    public SimpleZmqListenerContainer createContainer(Consumer<byte[]> listener) {
        SimpleZmqListenerContainer container = new SimpleZmqListenerContainer(zmqService);
        container.setMessageListener(listener);
        container.setConcurrency(properties.getListenerConcurrency());
        return container;
    }
}
