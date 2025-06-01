//package com.aoneconsultancy.zeromqpoc.listener.adapter;
//
//import com.aoneconsultancy.zeromqpoc.core.MessageListener;
//import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
//import com.aoneconsultancy.zeromqpoc.core.error.ZmqListenerErrorHandler;
//import com.aoneconsultancy.zeromqpoc.core.message.Message;
//import com.aoneconsultancy.zeromqpoc.support.converter.MessagingMessageConverter;
//import java.lang.reflect.ParameterizedType;
//import java.lang.reflect.Type;
//import java.lang.reflect.WildcardType;
//import java.util.Collection;
//import java.util.List;
//import java.util.Optional;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.MethodParameter;
//import org.springframework.lang.Nullable;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import org.springframework.messaging.MessageHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Headers;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.util.TypeUtils;
//
/// **
// * An adapter that invokes a handler method for a single ZmqMessage.
// * This adapter directly invokes the method with the ZmqMessage without
// * any Spring Messaging integration.
// */
//public class MessagingMessageListenerAdapter extends AbstractAdaptableMessageListener {
//
//    private final MessagingMessageConverterAdapter messagingMessageConverter;
//
//    private final boolean returnExceptions;
//
//    private final ZmqListenerErrorHandler errorHandler;
//
//    public MessagingMessageListenerAdapter(Object bean, Method method) {
//        this(bean, method, false, null);
//    }
//
//    public MessagingMessageListenerAdapter(Object bean, Method method, boolean returnExceptions,
//                                           ZmqListenerErrorHandler errorHandler) {
//        this(bean, method, returnExceptions, errorHandler, false);
//    }
//
//    protected MessagingMessageListenerAdapter(Object bean, Method method, boolean returnExceptions,
//                                              ZmqListenerErrorHandler errorHandler, boolean batch) {
//
//        this.messagingMessageConverter = new MessagingMessageConverterAdapter(bean, method, batch);
//        this.returnExceptions = returnExceptions;
//        this.errorHandler = errorHandler;
//    }
//
//    /**
//     * @return the {@link MessagingMessageConverter} for this listener,
//     * being able to convert {@link org.springframework.messaging.Message}.
//     */
//    protected final MessagingMessageConverter getMessagingMessageConverter() {
//        return this.messagingMessageConverter;
//    }
//
//    /**
//     * Handle the message by invoking the handler method directly.
//     * @param message the message to handle
//     */
//    public void onMessage(Message message) {
//        invokeHandlerMethod( message );
//    }
//
//    /**
//     * Directly invoke the handler method with the ZmqMessage.
//     * @param message the ZmqMessage to pass to the handler method
//     */
//    protected void invokeHandlerMethod(Message message) {
//        try {
//            // Extract payload based on method parameter type
//            Class<?> paramType = method.getParameterTypes()[0];
//            Object payload;
//
//            if (ZmqMessage.class.isAssignableFrom(paramType)) {
//                payload = message; // Pass the raw message
//            } else {
//                payload = converter.fromMessage(message, paramType);
//            }
//
//            // Invoke the method
//            Object result = method.invoke(bean, payload);
//
//            // Handle result if needed
//            if (result != null) {
//                handleResult(result, message);
//            }
//        }
//        catch (Exception ex) {
//            handleException(ex, message);
//        }
//
//    }
//
//    /**
//     * Handle an exception thrown during method invocation.
//     * @param throwable the exception
//     * @param message the message being processed
//     */
//    protected void handleInvocationException(Throwable throwable, Message message) {
//        Exception exception = throwable instanceof Exception ? (Exception) throwable : new RuntimeException(throwable);
//
//        if (this.returnExceptions) {
//            // Return exceptions to caller if requested
//            throw new RuntimeException("Failed to invoke handler method", exception);
//        }
//        else if (this.errorHandler != null) {
//            // Use the error handler if available
//            this.errorHandler.handleError(message, exception);
//        }
//        else {
//            // Otherwise, rethrow the exception
//            throw new RuntimeException("Failed to invoke handler method", exception);
//        }
//    }
//
//    /**
//     * Delegates payload extraction to
//     * {@link #extractMessage(Message message)}
//     * to enforce backward compatibility. Uses this listener adapter's converter instead of
//     * the one configured in the converter adapter.
//     * If the inbound message has no type information and the configured message converter
//     * supports it, we attempt to infer the conversion type from the method signature.
//     */
//    @Slf4j
//    protected final class MessagingMessageConverterAdapter extends MessagingMessageConverter {
//
//        private final Object bean;
//
//        final Method method; // NOSONAR visibility
//
//        private final Type inferredArgumentType;
//
//        private final boolean isBatch;
//
//        private boolean isMessageList;
//
//        private boolean isAmqpMessageList;
//
//        private boolean isCollection;
//
//        MessagingMessageConverterAdapter(Object bean, Method method, boolean batch) {
//            this.bean = bean;
//            this.method = method;
//            this.isBatch = batch;
//            this.inferredArgumentType = determineInferredType();
//            if (log.isDebugEnabled() && this.inferredArgumentType != null) {
//                log.debug("Inferred argument type for " + method.toString() + " is " + this.inferredArgumentType);
//            }
//        }
//
//        protected boolean isMessageList() {
//            return this.isMessageList;
//        }
//
//        protected boolean isAmqpMessageList() {
//            return this.isAmqpMessageList;
//        }
//
//        protected Method getMethod() {
//            return this.method;
//        }
//
//        @Override
//        protected Object extractPayload(Message message) {
//            MessageProperties messageProperties = message.getMessageProperties();
//            if (this.bean != null) {
//                messageProperties.setTargetBean(this.bean);
//            }
//            if (this.method != null) {
//                messageProperties.setTargetMethod(this.method);
//                if (this.inferredArgumentType != null) {
//                    messageProperties.setInferredArgumentType(this.inferredArgumentType);
//                }
//            }
//            return extractMessage(message);
//        }
//
//        private Type determineInferredType() { // NOSONAR - complexity
//            if (this.method == null) {
//                return null;
//            }
//
//            Type genericParameterType = null;
//
//            for (int i = 0; i < this.method.getParameterCount(); i++) {
//                MethodParameter methodParameter = new MethodParameter(this.method, i);
//                /*
//                 * We're looking for a single parameter, or one annotated with @Payload.
//                 * We ignore parameters with type Message because they are not involved with conversion.
//                 */
//                boolean isHeaderOrHeaders = methodParameter.hasParameterAnnotation(Header.class)
//                        || methodParameter.hasParameterAnnotation(Headers.class)
//                        || methodParameter.getParameterType().equals(MessageHeaders.class);
//                boolean isPayload = methodParameter.hasParameterAnnotation(Payload.class);
//                if (isHeaderOrHeaders && isPayload && MessagingMessageListenerAdapter.this.logger.isWarnEnabled()) {
//                    MessagingMessageListenerAdapter.this.logger.warn(this.method.getName()
//                            + ": Cannot annotate a parameter with both @Header and @Payload; "
//                            + "ignored for payload conversion");
//                }
//                if (isEligibleParameter(methodParameter) // NOSONAR
//                        && (!isHeaderOrHeaders || isPayload) && !(isHeaderOrHeaders && isPayload)) {
//
//                    if (genericParameterType == null) {
//                        genericParameterType = extractGenericParameterTypFromMethodParameter(methodParameter);
//                        if (this.isBatch && !this.isCollection) {
//                            throw new IllegalStateException(
//                                    "Mis-configuration; a batch listener must consume a List<?> or "
//                                            + "Collection<?> for method: " + this.method);
//                        }
//
//                    }
//                    else {
//                        if (MessagingMessageListenerAdapter.this.logger.isDebugEnabled()) {
//                            MessagingMessageListenerAdapter.this.logger
//                                    .debug("Ambiguous parameters for target payload for method " + this.method
//                                            + "; no inferred type header added");
//                        }
//                        return null;
//                    }
//                }
//            }
//            return checkOptional(genericParameterType);
//        }
//
//        protected Type checkOptional(Type genericParameterType) {
//            if (genericParameterType instanceof ParameterizedType pType && pType.getRawType().equals(Optional.class)) {
//                return pType.getActualTypeArguments()[0];
//            }
//            return genericParameterType;
//        }
//
//        /*
//         * Don't consider parameter types that are available after conversion.
//         * Message, Message<?> and Channel.
//         */
//        private boolean isEligibleParameter(MethodParameter methodParameter) {
//            Type parameterType = methodParameter.getGenericParameterType();
//            if (parameterType.equals(Channel.class)
//                    || parameterType.equals(MessageProperties.class)
//                    || parameterType.equals(org.springframework.amqp.core.Message.class)
//                    || parameterType.getTypeName().startsWith("kotlin.coroutines.Continuation")) {
//                return false;
//            }
//            if (parameterType instanceof ParameterizedType parameterizedType &&
//                    (parameterizedType.getRawType().equals(org.springframework.messaging.Message.class))) {
//                return !(parameterizedType.getActualTypeArguments()[0] instanceof WildcardType);
//            }
//            return !parameterType.equals(org.springframework.messaging.Message.class); // could be Message without a generic type
//        }
//
//        private Type extractGenericParameterTypFromMethodParameter(MethodParameter methodParameter) {
//            Type genericParameterType = methodParameter.getGenericParameterType();
//            if (genericParameterType instanceof ParameterizedType parameterizedType) {
//                if (parameterizedType.getRawType().equals(org.springframework.messaging.Message.class)) {
//                    genericParameterType = ((ParameterizedType) genericParameterType).getActualTypeArguments()[0];
//                }
//                else if (this.isBatch
//                        && ((parameterizedType.getRawType().equals(List.class)
//                        || parameterizedType.getRawType().equals(Collection.class))
//                        && parameterizedType.getActualTypeArguments().length == 1)) {
//
//                    this.isCollection = true;
//                    Type paramType = parameterizedType.getActualTypeArguments()[0];
//                    boolean messageHasGeneric = paramType instanceof ParameterizedType pType
//                            && pType.getRawType().equals(org.springframework.messaging.Message.class);
//                    this.isMessageList = TypeUtils.isAssignable(paramType, org.springframework.messaging.Message.class) || messageHasGeneric;
//                    this.isAmqpMessageList =
//                            TypeUtils.isAssignable(paramType, org.springframework.amqp.core.Message.class);
//                    if (messageHasGeneric) {
//                        genericParameterType = ((ParameterizedType) paramType).getActualTypeArguments()[0];
//                    }
//                    else {
//                        // when decoding batch messages we convert to the List's generic type
//                        genericParameterType = paramType;
//                    }
//                }
//            }
//            return genericParameterType;
//        }
//
//    }
//}
