package com.aoneconsultancy.zeromq.listener.endpoint;

import com.aoneconsultancy.zeromq.listener.ZmqListenerContainerFactory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper class for registering {@link ZmqListenerEndpoint} with a {@link ZmqListenerEndpointRegistry}.
 * Extends the generic EndpointRegistrar to provide ZeroMQ-specific functionality.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class ZmqListenerEndpointRegistrar implements BeanFactoryAware, InitializingBean {

    @Getter
    @Setter
    private ZmqListenerEndpointRegistry endpointRegistry;

    @Getter
    @Setter
    private ZmqListenerContainerFactory<?> containerFactory;

    @Setter
    private String containerFactoryBeanName;

    @Setter
    private BeanFactory beanFactory;

    private final List<ZmqListenerEndpointDescriptor> zmqListenerEndpointDescriptors = new ArrayList<>();

    private final Lock endpointDescriptorsLock = new ReentrantLock();

    // Custom method argument resolvers are no longer needed

    @Override
    public void afterPropertiesSet() {
        registerAllEndpoints();
    }

    /**
     * Register all endpoints with the registry.
     */
    public void registerAllEndpoints() {
        Assert.state(this.endpointRegistry != null, "No registry available");
        this.endpointDescriptorsLock.lock();
        try {
            for (ZmqListenerEndpointDescriptor descriptor : this.zmqListenerEndpointDescriptors) {
                this.endpointRegistry.registerListenerContainer(
                        descriptor.zmqListenerEndpoint, resolveContainerFactory(descriptor));
            }
            this.zmqListenerEndpointDescriptors.clear();
        } finally {
            this.endpointDescriptorsLock.unlock();
        }
    }

    /**
     * Register a new endpoint with the specified container factory.
     *
     * @param endpoint the endpoint to register
     * @param factory  the factory to use
     */
    public void registerEndpoint(ZmqListenerEndpoint endpoint, ZmqListenerContainerFactory<?> factory) {
        Assert.notNull(endpoint, "Endpoint must be set!");
        Assert.notNull(endpoint.getZmqConsumerProps(), "Endpoint Consumer props can not be null!");
        Assert.hasText(endpoint.getZmqConsumerProps().getName(), "Endpoint name must be set!");

        if (this.endpointRegistry != null) {
            this.endpointRegistry.registerListenerContainer(endpoint, factory, true);
        } else {
            this.zmqListenerEndpointDescriptors.add(new ZmqListenerEndpointDescriptor(endpoint, factory));
        }
    }

    /**
     * Register a new endpoint with the default container factory.
     *
     * @param endpoint the endpoint to register
     */
    public void registerEndpoint(ZmqListenerEndpoint endpoint) {
        Assert.notNull(endpoint, "Endpoint must not be null");

        ZmqListenerContainerFactory<?> factory = resolveContainerFactory(endpoint);
        registerEndpoint(endpoint, factory);
    }

    /**
     * Resolve the container factory to use for the given endpoint.
     *
     * @param descriptor the endpoint descriptor
     * @return the container factory to use
     */
    private ZmqListenerContainerFactory<?> resolveContainerFactory(ZmqListenerEndpointDescriptor descriptor) {
        if (descriptor.containerFactory != null) {
            return descriptor.containerFactory;
        }
        return resolveContainerFactory(descriptor.zmqListenerEndpoint);
    }

    /**
     * Resolve the container factory to use for the given endpoint.
     *
     * @param zmqListenerEndpoint the endpoint
     * @return the container factory to use
     */
    private ZmqListenerContainerFactory<?> resolveContainerFactory(ZmqListenerEndpoint zmqListenerEndpoint) {
        if (this.containerFactory != null) {
            return this.containerFactory;
        }
        if (this.containerFactoryBeanName != null) {
            Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
            return this.beanFactory.getBean(this.containerFactoryBeanName, ZmqListenerContainerFactory.class);
        }
        throw new IllegalStateException("Could not resolve the " +
                ZmqListenerContainerFactory.class.getSimpleName() + " to use for [" +
                zmqListenerEndpoint + "] no factory was given and no default is set.");
    }

    /**
     * A descriptor for an endpoint and its associated container factory.
     */
    private record ZmqListenerEndpointDescriptor(ZmqListenerEndpoint zmqListenerEndpoint,
                                                 ZmqListenerContainerFactory<?> containerFactory) {
        private ZmqListenerEndpointDescriptor(ZmqListenerEndpoint zmqListenerEndpoint,
                                              @Nullable ZmqListenerContainerFactory<?> containerFactory) {
            this.zmqListenerEndpoint = zmqListenerEndpoint;
            this.containerFactory = containerFactory;
        }
    }
}
