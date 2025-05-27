package com.aoneconsultancy.zeromqpoc.service.listener;

import com.aoneconsultancy.zeromqpoc.service.ZmqService;

import java.util.function.Consumer;

/**
 * Default factory that creates {@link SimpleZmqListenerContainer} instances.
 */
public class SimpleZmqListenerContainerFactory implements ZmqListenerContainerFactory<SimpleZmqListenerContainer> {

    private final ZmqService zmqService;

    public SimpleZmqListenerContainerFactory(ZmqService zmqService) {
        this.zmqService = zmqService;
    }

    @Override
    public SimpleZmqListenerContainer createContainer(Consumer<byte[]> listener) {
        SimpleZmqListenerContainer container = new SimpleZmqListenerContainer(zmqService);
        container.setMessageListener(listener);
        return container;
    }
}
