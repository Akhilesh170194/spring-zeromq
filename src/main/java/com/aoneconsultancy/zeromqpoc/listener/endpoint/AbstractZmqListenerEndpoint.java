package com.aoneconsultancy.zeromqpoc.listener.endpoint;

import com.aoneconsultancy.zeromqpoc.annotation.ZmqListener;
import com.aoneconsultancy.zeromqpoc.core.MessageListener;
import com.aoneconsultancy.zeromqpoc.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromqpoc.listener.MessageListenerContainer;
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

/**
 * Base class for {@link ZmqListenerEndpoint} implementations.
 * Provides common properties and methods for endpoints.
 */
@Slf4j
@Getter
@Setter
public abstract class AbstractZmqListenerEndpoint implements ZmqListenerEndpoint, BeanFactoryAware {

    private String id;

    @Getter
    @Setter
    private Boolean consumerBatchEnabled;

    @Nullable
    private Integer concurrency;

    @Nullable
    private String address;

    @Getter
    private ZmqListener.SocketType socketType;

    @Nullable
    private ZmqListenerErrorHandler errorHandler;

    @Nullable
    private MessageConverter messageConverter;

    @Nullable
    @Getter
    @Setter
    private ZmqSocketMonitor.SocketEventListener socketEventListener;

    @Setter
    private TaskExecutor taskExecutor;

    @Getter
    private BeanFactory beanFactory;

    @Getter
    private BeanExpressionResolver resolver;

    @Getter
    private BeanExpressionContext expressionContext;

    @Getter
    private BeanResolver beanResolver;

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
        if (this.address != null) {
            container.setAddress(this.address);
        }

        if (this.socketType != null) {
            container.setSocketType(this.socketType);
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
        return result.append(getClass().getSimpleName()).append("[").append(this.id).
                append("] address='").append(this.address).
                append("' | socketType='").append(this.socketType).
                append("' | concurrency='").append(this.concurrency).
                append("' | batchListener='").append(this.consumerBatchEnabled).append("'");
    }

    @Override
    public String toString() {
        return getEndpointDescription().toString();
    }

}
