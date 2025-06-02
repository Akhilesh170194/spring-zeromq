package com.aoneconsultancy.zeromqpoc.listener;

//import org.zeromq.

import com.aoneconsultancy.zeromqpoc.config.ContainerCustomizer;
import com.aoneconsultancy.zeromqpoc.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.listener.endpoint.ZmqListenerEndpoint;
import com.aoneconsultancy.zeromqpoc.support.micrometer.ZmqListenerObservationConvention;
import com.aoneconsultancy.zeromqpoc.support.postprocessor.MessagePostProcessor;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.JavaUtils;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * Factory for creating {@link MessageListenerContainer} instances.
 * Similar to Spring AMQP's AbstractMessageListenerContainerFactory,
 * this provides configuration options for creating listener containers.
 */
@Slf4j
public abstract class AbstractZmqListenerContainerFactory<T extends AbstractMessageListenerContainer>
        implements ZmqListenerContainerFactory<T>, ApplicationContextAware, ApplicationEventPublisherAware {

    protected final AtomicInteger counter = new AtomicInteger(); // NOSONAR

    @Setter
    protected ErrorHandler errorHandler;

    @Setter
    protected MessageConverter messageConverter;

    @Setter
    protected Executor taskExecutor;

    @Setter
    protected ApplicationEventPublisher applicationEventPublisher;

    @Getter
    @Setter
    protected ApplicationContext applicationContext;

    protected MessagePostProcessor[] afterReceivePostProcessors;

    @Setter
    protected Boolean consumerBatchEnabled;

    @Setter
    protected String address;

    @Setter
    protected Integer concurrency;

    @Getter
    @Setter
    private Boolean micrometerEnabled;

    @Getter
    @Setter
    private Boolean observationEnabled;

    @Setter
    protected ZmqListenerObservationConvention observationConvention;

    @Setter
    protected Integer batchSize = 1;

    @Setter
    protected Long batchTimeout = 1000L;

    @Setter
    protected TimeUnit batchTimeoutUnit = TimeUnit.MILLISECONDS;

    @Setter
    protected Integer bufferSize = 1000;

    @Setter
    protected ContainerCustomizer<T> containerCustomizer;

    @Setter
    protected ZmqSocketMonitor.SocketEventListener socketEventListener;

    /**
     * Set post-processors which will be applied after the Message is received.
     *
     * @param postProcessors the post-processors.
     * @since 2.0
     */
    public void setAfterReceivePostProcessors(MessagePostProcessor... postProcessors) {
        Assert.notNull(postProcessors, "'postProcessors' cannot be null");
        Assert.noNullElements(postProcessors, "'postProcessors' cannot have null elements");
        this.afterReceivePostProcessors = Arrays.copyOf(postProcessors, postProcessors.length);
    }

    @Override
    public T createListenerContainer(ZmqListenerEndpoint endpoint) {
        T instance = createContainerInstance();

        JavaUtils javaUtils =
                JavaUtils.INSTANCE
                        .acceptIfNotNull(this.errorHandler, instance::setErrorHandler);
        if (this.messageConverter != null && endpoint != null && endpoint.getMessageConverter() == null) {
            endpoint.setMessageConverter(this.messageConverter);
        }
        // First set endpoint properties (if available)
        if (endpoint != null) {
            javaUtils
                    .acceptIfNotNull(endpoint.getTaskExecutor(), instance::setTaskExecutor)
                    .acceptIfNotNull(endpoint.getSocketEventListener(), instance::setSocketEventListener);
        }

        // Then set factory properties (which will override endpoint properties if set)
        javaUtils
                .acceptIfNotNull(getApplicationContext(), instance::setApplicationContext)
                .acceptIfNotNull(this.taskExecutor, instance::setTaskExecutor)
                .acceptIfNotNull(this.applicationEventPublisher, instance::setApplicationEventPublisher)
                .acceptIfNotNull(this.afterReceivePostProcessors, instance::setAfterReceivePostProcessors)
                .acceptIfNotNull(getMicrometerEnabled(), instance::setMicrometerEnabled)
                .acceptIfNotNull(getObservationEnabled(), instance::setObservationEnabled)
                .acceptIfNotNull(this.observationConvention, instance::setObservationConvention)
                .acceptIfNotNull(this.concurrency, instance::setConcurrency)
                .acceptIfNotNull(this.address, instance::setAddress)
                .acceptIfNotNull(this.bufferSize, instance::setBufferSize)
                .acceptIfNotNull(this.socketEventListener, instance::setSocketEventListener)
                // TODO - Set below 3 properties in the endpoint
                .acceptIfNotNull(this.batchSize, instance::setBatchSize)
                .acceptIfNotNull(this.batchTimeout, instance::setBatchReceiveTimeout)
                .acceptIfNotNull(this.batchTimeoutUnit, instance::setBatchTimeOutUnit)
                .acceptIfNotNull(this.consumerBatchEnabled, instance::setConsumerBatchEnabled)
                .acceptIfNotNull(this.messageConverter, instance::setMessageConverter)
                .acceptIfNotNull(this.errorHandler, instance::setErrorHandler)
                .acceptIfNotNull(this.socketEventListener, instance::setSocketEventListener);

        if (endpoint != null) { // Set endpoint ID and batch listener
            instance.setListenerId(endpoint.getId());
            if (endpoint.getConsumerBatchEnabled() == null) {
                endpoint.setConsumerBatchEnabled(this.consumerBatchEnabled);
            }
            if (this.socketEventListener != null) {
                endpoint.setSocketEventListener(this.socketEventListener);
            }
        }
        initializeContainer(instance, endpoint);

        if (this.containerCustomizer != null) {
            this.containerCustomizer.configure(instance);
        }

        return instance;
    }

    /**
     * Create an empty container instance.
     *
     * @return the new container instance.
     */
    protected abstract T createContainerInstance();

    /**
     * Further initialize the specified container.
     * <p>Subclasses can inherit from this method to apply extra
     * configuration if necessary.
     *
     * @param instance the container instance to configure.
     * @param endpoint the endpoint.
     */
    protected void initializeContainer(T instance, ZmqListenerEndpoint endpoint) {
    }

}
