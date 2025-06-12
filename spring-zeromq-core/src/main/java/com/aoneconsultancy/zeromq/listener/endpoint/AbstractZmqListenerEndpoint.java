package com.aoneconsultancy.zeromq.listener.endpoint;

import com.aoneconsultancy.zeromq.config.ZmqConsumerProperties;
import com.aoneconsultancy.zeromq.core.MessageListener;
import com.aoneconsultancy.zeromq.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromq.listener.MessageListenerContainer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.expression.BeanResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.zeromq.SocketType;

import java.util.List;

/**
 * Base class for {@link ZmqListenerEndpoint} implementations.
 * Provides common properties and methods for endpoints.
 */
@Slf4j
public abstract class AbstractZmqListenerEndpoint implements ZmqListenerEndpoint, BeanFactoryAware {

    @Getter
    @Setter
    private Boolean consumerBatchEnabled;

    @Nullable
    @Setter
    @Getter
    private Integer concurrency;

    @Nullable
    private ZmqListenerErrorHandler errorHandler;

    @Nullable
    @Setter
    @Getter
    private MessageConverter messageConverter;

    @Nullable
    @Getter
    @Setter
    private ZmqSocketMonitor.SocketEventListener socketEventListener;

    @Setter
    @Getter
    private TaskExecutor taskExecutor;

    @Getter
    private BeanFactory beanFactory;

    @Getter
    private BeanExpressionResolver resolver;

    @Getter
    private BeanExpressionContext expressionContext;

    @Getter
    private BeanResolver beanResolver;

    @Getter
    private ZmqConsumerProperties zmqConsumerProps;

    public void setZmqConsumerProps(String name, List<String> endpoints, boolean bind, SocketType type) {
        this.zmqConsumerProps = ZmqConsumerProperties.builder().name(name).addresses(endpoints).bind(bind).type(type).build();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        if (beanFactory instanceof ConfigurableListableBeanFactory clbf) {
            this.resolver = clbf.getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext(clbf, null);
        }
        this.beanResolver = new BeanFactoryResolver(beanFactory);
    }

    @Override
    public void setupListenerContainer(MessageListenerContainer container) {

        if (this.zmqConsumerProps != null) {
            container.setZmqConsumerProps(this.zmqConsumerProps);
        }

        if (this.concurrency != null) {
            container.setConcurrency(this.concurrency);
        }

        if (this.errorHandler != null) {
            container.setErrorHandler(throwable -> {
                log.error("Error occurred in ZMQ listener for endpoint [{}]", this, throwable);
            });
        }

        if (this.socketEventListener != null) {
            container.setSocketEventListener(this.socketEventListener);
        }

        if (this.messageConverter != null) {
            container.setMessageConverter(this.messageConverter);
        }
        setupMessageListener(container);
    }

    /**
     * Create a {@link MessageListener} that is able to serve this endpoint for the
     * specified container.
     *
     * @param container the {@link MessageListenerContainer} to create a {@link MessageListener}.
     * @return a a {@link MessageListener} instance.
     */
    protected abstract MessageListener createMessageListener(MessageListenerContainer container);

    private void setupMessageListener(MessageListenerContainer container) {
        MessageListener messageListener = createMessageListener(container);
        Assert.state(messageListener != null, () -> "Endpoint [" + this + "] must provide a non null message listener");
        container.setupMessageListener(messageListener);
    }

    /**
     * @return a description for this endpoint.
     * <p>Available to subclasses, for inclusion in their {@code toString()} result.
     */
    protected StringBuilder getEndpointDescription() {
        StringBuilder result = new StringBuilder();
        return result.append(getClass().getSimpleName()).append("[").append(this.zmqConsumerProps.getName()).
                append("] endpoints='").append(this.zmqConsumerProps.getAddresses()).
                append("' | socketType='").append(this.zmqConsumerProps.getType()).
                append("' | concurrency='").append(this.concurrency).
                append("' | batchListener='").append(this.consumerBatchEnabled).append("'");
    }

    @Override
    public String toString() {
        return getEndpointDescription().toString();
    }

}
