package com.aoneconsultancy.zeromq.listener.adapter;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.converter.SmartMessageConverter;
import com.aoneconsultancy.zeromq.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromq.core.message.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * A simplified adapter that invokes a handler method for a single ZmqMessage.
 * This adapter directly invokes the method with the ZmqMessage without
 * any Spring Messaging integration or complex type inference.
 */
@Slf4j
public class MessagingMessageListenerAdapter extends AbstractAdaptableMessageListener {

    private final boolean returnExceptions;

    private final ZmqListenerErrorHandler errorHandler;

    @Getter
    protected final Object bean;

    @Getter
    protected final Method method;

    public MessagingMessageListenerAdapter(Object bean, Method method) {
        this(bean, method, false, null);
    }

    public MessagingMessageListenerAdapter(Object bean, Method method, boolean returnExceptions,
                                           ZmqListenerErrorHandler errorHandler) {
        this(bean, method, returnExceptions, errorHandler, false);
    }

    protected MessagingMessageListenerAdapter(Object bean, Method method, boolean returnExceptions,
                                              ZmqListenerErrorHandler errorHandler, boolean batch) {
        this.bean = bean;
        this.method = method;
        this.returnExceptions = returnExceptions;
        this.errorHandler = errorHandler;
    }

    /**
     * Handle the message by invoking the handler method directly.
     *
     * @param message the message to handle
     */
    @Override
    public void onMessage(Message message) {
        invokeHandlerMethod(message);
    }

    /**
     * Directly invoke the handler method with the ZmqMessage.
     *
     * @param message the ZmqMessage to pass to the handler method
     */
    protected void invokeHandlerMethod(Message message) {
        try {
            Object result;

            // Check if the method has parameters
            if (method.getParameterCount() > 0) {
                // Extract payload based on method parameter type
                Class<?> paramType = method.getParameterTypes()[0];
                Object payload;

                if (Message.class.isAssignableFrom(paramType)) {
                    payload = message; // Pass the raw message
                } else {
                    MessageConverter converter = getMessageConverter();
                    // Get the payload using the converter
                    Object rawPayload;
                    if (converter instanceof SmartMessageConverter smartConverter) {
                        rawPayload = smartConverter.fromMessage(message, null);
                    } else {
                        rawPayload = converter.fromMessage(message);
                    }

                    // Try to cast or convert the payload to the parameter type
                    if (paramType.isInstance(rawPayload)) {
                        payload = rawPayload;
                    } else if (rawPayload instanceof byte[] && paramType == String.class) {
                        payload = new String((byte[]) rawPayload);
                    } else if (rawPayload instanceof String && paramType == byte[].class) {
                        payload = ((String) rawPayload).getBytes();
                    } else {
                        log.warn("Could not convert payload of type {} to parameter type {}",
                                rawPayload.getClass().getName(), paramType.getName());
                        payload = rawPayload;
                    }
                }

                // Invoke the method with the payload
                result = method.invoke(bean, payload);
            } else {
                // Invoke the method with no parameters
                result = method.invoke(bean);
            }

            // Handle result if needed
            if (result != null) {
                // Create an InvocationResult with null for sendTo and returnType
                handleResult(new InvocationResult(result, null, null, bean, method), message, null);
            }
        } catch (Exception ex) {
            handleInvocationException(ex, message);
        }
    }

    /**
     * Handle an exception thrown during method invocation.
     *
     * @param throwable the exception
     * @param message   the message being processed
     */
    protected void handleInvocationException(Throwable throwable, Message message) {
        Exception exception = throwable instanceof Exception ? (Exception) throwable : new RuntimeException(throwable);

        if (this.returnExceptions) {
            // Return exceptions to caller if requested
            throw new RuntimeException("Failed to invoke handler method", exception);
        } else if (this.errorHandler != null) {
            // Use the error handler if available
            this.errorHandler.handleError(message, exception);
        } else {
            // Otherwise, rethrow the exception
            throw new RuntimeException("Failed to invoke handler method", exception);
        }
    }
}
