package com.aoneconsultancy.zeromq.autoconfigure;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.listener.AbstractZmqListenerContainerFactory;
import lombok.Setter;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.Assert;
import org.zeromq.ZContext;

import java.util.concurrent.Executor;

/**
 * Base class for configurers of sub-classes of
 * {@link AbstractZmqListenerContainerFactory}.
 *
 * @param <T> the container factory type.
 */
public abstract class AbstractZmqListenerContainerFactoryConfigurer<T extends AbstractZmqListenerContainerFactory<?>> {

    @Setter
    private MessageConverter messageConverter;

    private final ZmqProperties zmqProperties;

    @Setter
    private Executor taskExecutor;

    /**
     * Creates a new configurer that will use the given {@code rabbitProperties}.
     *
     * @param zmqProperties properties to use
     * @since 2.6.0
     */
    protected AbstractZmqListenerContainerFactoryConfigurer(ZmqProperties zmqProperties) {
        this.zmqProperties = zmqProperties;
    }

    protected final ZmqProperties getZmqProperties() {
        return this.zmqProperties;
    }

    /**
     * Configure the specified rabbit listener container factory. The factory can be
     * further tuned and default settings can be overridden.
     *
     * @param factory the {@link AbstractZmqListenerContainerFactory} instance to
     *                configure
     * @param context the {@link ZContext} to use
     */
    public abstract void configure(T factory, ZContext context);

    protected void configure(T factory, ZContext context, ZmqProperties.Listener config) {

        Assert.notNull(factory, "'factory' must not be null");
        Assert.notNull(config, "'config' must not be null");
        Assert.notNull(context, "'context' must not be null");
        PropertyMapper map = PropertyMapper.get();
        if (this.messageConverter != null) {
            factory.setMessageConverter(this.messageConverter);
        }
        if (config.getAcknowledge() != null) {
            factory.setAcknowledge(config.getAcknowledge());
        }
        if (this.taskExecutor != null) {
            factory.setTaskExecutor(this.taskExecutor);
        }
        factory.setContext(context);
        map.from(config::getConsumerBatchEnabled).whenNonNull().to(factory::setConsumerBatchEnabled);
        map.from(config::getConcurrency).whenNonNull().to(factory::setConcurrency);
        map.from(config::getBatchSize).whenNonNull().to(factory::setBatchSize);
        map.from(config::getAcknowledge).whenNonNull().to(factory::setAcknowledge);
        map.from(config::getBatchTimeout).whenNonNull().to(factory::setBatchTimeout);
        map.from(config::getSocketRecvBuffer).whenNonNull().to(factory::setSocketRecvBuffer);
        map.from(config::getSocketHwm).whenNonNull().to(factory::setRecvHwm);
        map.from(config::getSocketReconnectInterval).whenNonNull().to(factory::setSocketReconnectInterval);
        map.from(config::getSocketBackoff).whenNonNull().to(factory::setSocketBackoff);
    }

}
