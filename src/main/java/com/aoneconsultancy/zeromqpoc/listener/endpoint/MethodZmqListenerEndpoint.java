package com.aoneconsultancy.zeromqpoc.listener.endpoint;

import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromqpoc.core.message.Message;
import com.aoneconsultancy.zeromqpoc.listener.MessageListenerContainer;
import com.aoneconsultancy.zeromqpoc.listener.adapter.BatchMessagingMessageListenerAdapter;
import com.aoneconsultancy.zeromqpoc.listener.adapter.MessagingMessageListenerAdapter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
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

    @Setter
    private AdapterProvider adapterProvider = new DefaultAdapterProvider();

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
    protected MessagingMessageListenerAdapter createMessageListener(MessageListenerContainer container) {
        MessagingMessageListenerAdapter messageListener = createMessageListenerInstance(getConsumerBatchEnabled());
        messageListener.setHandlerAdapter(configureListenerAdapter(messageListener));
        String replyToAddress = getDefaultReplyToAddress();
        if (replyToAddress != null) {
            messageListener.setResponseAddress(replyToAddress);
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

        // Determine the type of listener based on the method parameters
        Parameter[] parameters = this.method.getParameters();
        if (parameters.length == 1) {
            if (parameters[0].getType() == Message.class) {
                // Special case: if there's only one parameter, and it's a ZmqMessage,
                // use the MessagingMessageListenerAdapter for direct message passing
                MessagingMessageListenerAdapter adapter = new MessagingMessageListenerAdapter(
                        this.bean, this.method, false, getErrorHandler());

                Consumer<Message> messageListener = adapter::onMessage;
                container.setZmqMessageListener(messageListener);
                return;
            } else if (isList(parameters[0].getParameterizedType(), Message.class)) {
                // Special case for List<ZmqMessage>: use batch adapter
                BatchMessagingMessageListenerAdapter adapter = new BatchMessagingMessageListenerAdapter(
                        this.bean, this.method, false, getErrorHandler());

                Consumer<List<Message>> batchListener = adapter::onMessages;
                container.setZmqBatchMessageListener(batchListener);
                return;
            } else if (parameters[0].getType() == byte[].class) {
                // Direct byte array handling
                Consumer<byte[]> listener = bytes -> {
                    try {
                        method.invoke(bean, bytes);
                    } catch (Exception e) {
                        if (getErrorHandler() != null) {
                            getErrorHandler().handleError(new Message(bytes), e instanceof Exception ? (Exception) e : new RuntimeException(e));
                        } else {
                            throw new RuntimeException("Failed to invoke handler method", e);
                        }
                    }
                };
                container.setMessageListener(listener);
                return;
            }
        } else if (parameters.length == 0) {
            // Method takes no parameters
            Consumer<byte[]> listener = bytes -> {
                try {
                    method.invoke(bean);
                } catch (Exception e) {
                    if (getErrorHandler() != null) {
                        getErrorHandler().handleError(new Message(bytes), e instanceof Exception ? (Exception) e : new RuntimeException(e));
                    } else {
                        throw new RuntimeException("Failed to invoke handler method", e);
                    }
                }
            };
            container.setMessageListener(listener);
            return;
        }

        // Standard case: use message adapter with ZmqMessage
        MessagingMessageListenerAdapter adapter = new MessagingMessageListenerAdapter(
                this.bean, this.method, false, getErrorHandler());

        Consumer<byte[]> listener = bytes -> {
            // Create a ZmqMessage directly from the byte array
            Message rawMessage = new Message(bytes);

            // Invoke the handler method with the message
            adapter.onMessage(rawMessage);
        };

        container.setMessageListener(listener);
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

    /**
     * Create an empty {@link MessagingMessageListenerAdapter} instance.
     *
     * @param batch whether this endpoint is for a batch listener.
     * @return the {@link MessagingMessageListenerAdapter} instance.
     */
    protected MessagingMessageListenerAdapter createMessageListenerInstance(@Nullable Boolean batch) {
        return this.adapterProvider.getAdapter(batch == null ? getConsumerBatchEnabled() : batch, this.bean, this.method,
                this.returnExceptions, this.errorHandler);
    }

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
        if (getBeanFactory() != null) {
            String resolvedValue = getBeanExpressionContext().getBeanFactory().resolveEmbeddedValue(value);
            Object newValue = getResolver().evaluate(resolvedValue, getBeanExpressionContext());
            Assert.isInstanceOf(String.class, newValue, "Invalid @SendTo expression");
            return (String) newValue;
        } else {
            return value;
        }
    }

    /**
     * Provider of listener adapters.
     *
     * @since 2.4
     */
    public interface AdapterProvider {

        /**
         * Get an adapter instance.
         *
         * @param batch            true for a batch listener.
         * @param bean             the bean.
         * @param method           the method.
         * @param returnExceptions true to return exceptions.
         * @param errorHandler     the error handler.
         * @return the adapter.
         */
        MessagingMessageListenerAdapter getAdapter(boolean batch, Object bean, Method method, boolean returnExceptions,
                                                   ZmqListenerErrorHandler errorHandler);
    }

    private static final class DefaultAdapterProvider implements AdapterProvider {

        @Override
        public MessagingMessageListenerAdapter getAdapter(boolean batch, Object bean, Method method,
                                                          boolean returnExceptions, ZmqListenerErrorHandler errorHandler) {
            if (batch) {
                return new BatchMessagingMessageListenerAdapter(bean, method, returnExceptions, errorHandler);
            } else {
                return new MessagingMessageListenerAdapter(bean, method, returnExceptions, errorHandler);
            }
        }

    }
}
