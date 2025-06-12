package com.aoneconsultancy.zeromq.listener.endpoint;

import com.aoneconsultancy.zeromq.core.MessageListener;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromq.listener.MessageListenerContainer;
import com.aoneconsultancy.zeromq.listener.adapter.MultiMethodMessageListenerAdapter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.List;

/**
 * An {@link ZmqListenerEndpoint} implementation for invoking multiple methods
 * when a message is received, based on the message payload type.
 * Used for class-level {@link com.aoneconsultancy.zeromq.annotation.ZmqListener} annotations
 * with methods annotated with {@link com.aoneconsultancy.zeromq.annotation.ZmqHandler}.
 */
@Getter
@Setter
public class MultiMethodZmqListenerEndpoint extends AbstractZmqListenerEndpoint {

    private final List<Method> methods;
    private final Method defaultMethod;
    private final Object bean;

    @Setter
    private boolean returnExceptions;

    @Setter
    private ZmqListenerErrorHandler errorHandler;

    /**
     * Create a new {@link MultiMethodZmqListenerEndpoint} for the given bean and methods.
     *
     * @param methods       the methods to invoke
     * @param defaultMethod the default method to invoke if no method matches the payload type
     * @param bean          the bean instance
     */
    public MultiMethodZmqListenerEndpoint(List<Method> methods, Method defaultMethod, Object bean) {
        Assert.notNull(bean, "Bean must not be null");
        Assert.notEmpty(methods, "Methods must not be empty");
        this.methods = methods;
        this.defaultMethod = defaultMethod;
        this.bean = bean;

        // Make all methods accessible
        for (Method method : methods) {
            method.setAccessible(true);
        }
        if (defaultMethod != null) {
            defaultMethod.setAccessible(true);
        }
    }

    @Override
    protected MessageListener createMessageListener(MessageListenerContainer container) {
        Boolean batch = getConsumerBatchEnabled();
        if (batch != null && batch) {
            throw new UnsupportedOperationException("Batch processing is not supported for multi-method endpoints");
        }

        MultiMethodMessageListenerAdapter messageListener =
                new MultiMethodMessageListenerAdapter(this.bean, this.methods, this.defaultMethod,
                        this.returnExceptions, this.errorHandler);

        MessageConverter messageConverter = getMessageConverter();
        if (messageConverter != null) {
            messageListener.setMessageConverter(messageConverter);
        }

        if (getBeanResolver() != null) {
            messageListener.setBeanResolver(getBeanResolver());
        }

        return messageListener;
    }
}
