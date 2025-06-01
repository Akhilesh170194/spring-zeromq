package com.aoneconsultancy.zeromqpoc.listener.endpoint;

import com.aoneconsultancy.zeromqpoc.listener.MessageListenerContainer;
import com.aoneconsultancy.zeromqpoc.listener.ZmqListenerContainerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Registry for ZeroMQ listener endpoints.
 */
public class ZmqListenerEndpointRegistry implements DisposableBean, SmartLifecycle, ApplicationContextAware,
        ApplicationListener<ContextRefreshedEvent> {

    private final Map<String, MessageListenerContainer> listenerContainers = new ConcurrentHashMap<>();

    private final Lock listenerContainersLock = new ReentrantLock();

    private final Lock lifecycleLock = new ReentrantLock();

    @Setter
    private int phase = Integer.MAX_VALUE;

    private ConfigurableApplicationContext applicationContext;

    private boolean contextRefreshed;

    /**
     * Return the managed {@link MessageListenerContainer} instance(s).
     *
     * @return the managed {@link MessageListenerContainer} instance(s)
     */
    public Collection<MessageListenerContainer> getListenerContainers() {
        return Collections.unmodifiableCollection(this.listenerContainers.values());
    }

    /**
     * Return the managed {@link MessageListenerContainer} instance for the given endpoint id,
     * if any.
     *
     * @param id the endpoint id
     * @return the managed {@link MessageListenerContainer} instance, or {@code null} if none
     */
    @Nullable
    public MessageListenerContainer getListenerContainer(String id) {
        Assert.hasText(id, "Endpoint id must not be empty");
        return this.listenerContainers.get(id);
    }

    /**
     * Register a new {@link ZmqListenerEndpoint} with the given {@link ZmqListenerContainerFactory}.
     *
     * @param endpoint the endpoint to register
     * @param factory  the factory to use
     */
    public void registerListenerContainer(
            ZmqListenerEndpoint endpoint, ZmqListenerContainerFactory<?> factory) {
        Assert.notNull(endpoint, "Endpoint must not be null");
        Assert.notNull(factory, "Factory must not be null");
        registerListenerContainer(endpoint, factory, false);
    }

    /**
     * Register a new {@link ZmqListenerEndpoint} with the given {@link ZmqListenerContainerFactory}.
     *
     * @param endpoint         the endpoint to register
     * @param factory          the factory to use
     * @param startImmediately start the container immediately if necessary
     */
    @SuppressWarnings("unchecked")
    public void registerListenerContainer(
            ZmqListenerEndpoint endpoint, ZmqListenerContainerFactory<?> factory, boolean startImmediately) {

        Assert.notNull(endpoint, "Endpoint must not be null");
        Assert.notNull(factory, "Factory must not be null");

        String id = endpoint.getId();
        Assert.hasText(id, "Endpoint id must not be empty");
        this.listenerContainersLock.lock();
        try {
            Assert.state(!this.listenerContainers.containsKey(id),
                    "Another endpoint is already registered with id '" + id + "'");
            MessageListenerContainer container = createListenerContainer(endpoint, factory);
            this.listenerContainers.put(id, container);
            if (startImmediately) {
                startIfNecessary(container);
            }
        } finally {
            this.listenerContainersLock.unlock();
        }
    }

    /**
     * Create a message listener container for the given {@link ZmqListenerEndpoint}.
     *
     * @param endpoint the endpoint to create a container for
     * @param factory  the factory to use
     * @return the created container
     */
    protected MessageListenerContainer createListenerContainer(
            ZmqListenerEndpoint endpoint, ZmqListenerContainerFactory<?> factory) {

        // Create a dummy listener that will be replaced by the endpoint setup
        Consumer<byte[]> dummyListener = (byte[] bytes) -> {
        };
        MessageListenerContainer container = factory.createListenerContainer(endpoint);
        // TODO - Call container.afterPropertiesSet();
        endpoint.setupListenerContainer(container);
        return container;
    }

    @Override
    public void destroy() {
        for (MessageListenerContainer container : getListenerContainers()) {
            if (container instanceof DisposableBean disposableBean) {
                try {
                    disposableBean.destroy();
                } catch (Exception ex) {
                    // Continue with other containers
                }
            }
        }
    }

    @Override
    public void start() {
        this.lifecycleLock.lock();
        try {
            for (MessageListenerContainer container : getListenerContainers()) {
                startIfNecessary(container);
            }
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    @Override
    public void stop() {
        this.lifecycleLock.lock();
        try {
            for (MessageListenerContainer container : getListenerContainers()) {
                container.stop();
            }
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    @Override
    public boolean isRunning() {
        for (MessageListenerContainer listenerContainer : getListenerContainers()) {
            if (listenerContainer.isRunning()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getPhase() {
        return this.phase;
    }

    private void startIfNecessary(MessageListenerContainer container) {
        if (this.contextRefreshed || container.isAutoStartup()) {
            container.start();
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().equals(this.applicationContext)) {
            this.contextRefreshed = true;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext configurable) {
            this.applicationContext = configurable;
        }
    }
}
