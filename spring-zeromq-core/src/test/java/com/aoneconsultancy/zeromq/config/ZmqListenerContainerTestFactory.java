package com.aoneconsultancy.zeromq.config;

import com.aoneconsultancy.zeromq.listener.ZmqListenerContainerFactory;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ZmqListenerContainerFactory} implementation that returns
 * {@link MessageListenerTestContainer} instances.
 * <p>
 * This is a test implementation that allows us to verify that the
 * {@link com.aoneconsultancy.zeromq.annotation.ZmqListenerAnnotationBeanPostProcessor}
 * correctly processes {@link com.aoneconsultancy.zeromq.annotation.ZmqListener} annotations.
 */
public class ZmqListenerContainerTestFactory implements ZmqListenerContainerFactory<MessageListenerTestContainer>, DisposableBean {

    private final List<MessageListenerTestContainer> listenerContainers = new ArrayList<>();

    @Override
    public MessageListenerTestContainer createListenerContainer(ZmqListenerEndpoint endpoint) {
        MessageListenerTestContainer container = new MessageListenerTestContainer(endpoint);
        this.listenerContainers.add(container);
        return container;
    }

    /**
     * Return the list of listener containers created by this factory.
     *
     * @return the list of listener containers
     */
    public List<MessageListenerTestContainer> getListenerContainers() {
        return this.listenerContainers;
    }

    @Override
    public void destroy() {
        for (MessageListenerTestContainer container : this.listenerContainers) {
            container.stop();
        }
    }
}