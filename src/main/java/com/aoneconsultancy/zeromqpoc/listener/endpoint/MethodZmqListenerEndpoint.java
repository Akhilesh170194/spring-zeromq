package com.aoneconsultancy.zeromqpoc.listener.endpoint;

import com.aoneconsultancy.zeromqpoc.core.MessageListener;
import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromqpoc.listener.MessageListenerContainer;
import com.aoneconsultancy.zeromqpoc.listener.adapter.BatchMessagingMessageListenerAdapter;
import com.aoneconsultancy.zeromqpoc.listener.adapter.MessagingMessageListenerAdapter;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.util.Assert;

/**
 * An {@link ZmqListenerEndpoint} implementation for invoking a method
 * when a message is received.
 */
@Getter
@Setter
public class MethodZmqListenerEndpoint extends AbstractZmqListenerEndpoint {

    private Object bean;
    private Method method;

    // AdapterProvider has been removed as it's no longer needed

    @Setter
    private boolean returnExceptions;

    @Setter
    private ZmqListenerErrorHandler errorHandler;

    /**
     * Create a new {@link MethodZmqListenerEndpoint} for the given bean and method.
     *
     * @param bean   the bean instance
     * @param method the method to invoke
     */
    public MethodZmqListenerEndpoint(Object bean, Method method) {
        Assert.notNull(bean, "Bean must not be null");
        Assert.notNull(method, "Method must not be null");
        this.bean = bean;
        this.method = method;
        method.setAccessible(true);
    }

    @Override
    protected MessageListener createMessageListener(MessageListenerContainer container) {
        Boolean batch = getConsumerBatchEnabled();
        MessagingMessageListenerAdapter messageListener;

        if (batch != null && batch) {
            messageListener = new BatchMessagingMessageListenerAdapter(this.bean, this.method, this.returnExceptions, this.errorHandler);
        } else {
            messageListener = new MessagingMessageListenerAdapter(this.bean, this.method, this.returnExceptions, this.errorHandler);
        }

        MessageConverter messageConverter = getMessageConverter();
        if (messageConverter != null) {
            messageListener.setMessageConverter(messageConverter);
        }

        if (getBeanResolver() != null) {
            messageListener.setBeanResolver(getBeanResolver());
        }

        return messageListener;
    }

    @Override
    public void setupListenerContainer(MessageListenerContainer container) {
        super.setupListenerContainer(container);

        // Create the appropriate message listener adapter based on the method parameters
        MessageListener messageListener = createMessageListener(container);

        // Set up the message listener in the container
        container.setupMessageListener(messageListener);
    }

    // These methods have been removed as they're no longer needed since we're directly invoking the method

    /**
     * Check if a type is a List of a specific element type.
     *
     * @param type        the type to check
     * @param elementType the expected element type
     * @return true if the type is a List of the specified element type
     */
    private static boolean isList(Type type, Class<?> elementType) {
        if (!(type instanceof ParameterizedType parameterizedType)) {
            return false;
        }

        if (!List.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
            return false;
        }

        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        return typeArguments.length == 1 && typeArguments[0].equals(elementType);
    }

    // This method has been removed as it's no longer needed

    @Nullable
    private String getDefaultReplyToAddress() {
        Method listenerMethod = getMethod();
        if (listenerMethod != null) {
            SendTo ann = AnnotationUtils.getAnnotation(listenerMethod, SendTo.class);
            if (ann != null) {
                String[] destinations = ann.value();
                if (destinations.length > 1) {
                    throw new IllegalStateException("Invalid @" + SendTo.class.getSimpleName() + " annotation on '"
                            + listenerMethod + "' one destination must be set (got " + Arrays.toString(destinations) + ")");
                }
                return destinations.length == 1 ? resolveSendTo(destinations[0]) : "";
            }
        }
        return null;
    }

    private String resolveSendTo(String value) {
        if (getBeanFactory() != null && getResolver() != null && getExpressionContext() != null) {
            String resolvedValue = value;
            if (getBeanFactory() instanceof org.springframework.beans.factory.config.ConfigurableListableBeanFactory clbf) {
                resolvedValue = clbf.resolveEmbeddedValue(value);
            }
            Object newValue = getResolver().evaluate(resolvedValue, getExpressionContext());
            Assert.isInstanceOf(String.class, newValue, "Invalid @SendTo expression");
            return (String) newValue;
        } else {
            return value;
        }
    }

    // AdapterProvider interface and implementation have been removed as they're no longer needed
}
