package com.aoneconsultancy.zeromq.listener.adapter;

import com.aoneconsultancy.zeromq.core.error.ZmqListenerErrorHandler;
import com.aoneconsultancy.zeromq.core.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * A message listener adapter that invokes the appropriate method based on the message payload type.
 * Used for class-level {@link com.aoneconsultancy.zeromq.annotation.ZmqListener} annotations
 * with methods annotated with {@link com.aoneconsultancy.zeromq.annotation.ZmqHandler}.
 */
@Slf4j
public class MultiMethodMessageListenerAdapter extends MessagingMessageListenerAdapter {

    private final List<Method> handlerMethods;
    private final Method defaultMethod;

    /**
     * Create a new MultiMethodMessageListenerAdapter.
     *
     * @param bean             the bean instance
     * @param methods          the methods to invoke
     * @param defaultMethod    the default method to invoke if no method matches the payload type
     * @param returnExceptions whether to return exceptions to the caller
     * @param errorHandler     the error handler to use
     */
    public MultiMethodMessageListenerAdapter(Object bean, List<Method> methods, Method defaultMethod,
                                             boolean returnExceptions, ZmqListenerErrorHandler errorHandler) {
        super(bean, defaultMethod != null ? defaultMethod : methods.get(0), returnExceptions, errorHandler);
        Assert.notEmpty(methods, "Handler methods must not be empty");
        this.handlerMethods = new ArrayList<>(methods);
        this.defaultMethod = defaultMethod;
    }

    @Override
    public void onMessage(Message message) {
        Object payload = extractPayload(message);
        Method method = findHandlerMethod(payload);
        if (method != null) {
            invokeHandlerMethod(method, message);
        } else {
            log.warn("No handler method found for payload type: {}",
                    payload != null ? payload.getClass().getName() : "null");
        }
    }

    /**
     * Extract the payload from the message.
     *
     * @param message the message
     * @return the payload
     */
    private Object extractPayload(Message message) {
        try {
            return getMessageConverter().fromMessage(message);
        } catch (Exception e) {
            log.warn("Failed to extract payload from message", e);
            return message;
        }
    }

    /**
     * Find the appropriate handler method for the given payload.
     *
     * @param payload the payload
     * @return the handler method, or null if no method matches
     */
    private Method findHandlerMethod(Object payload) {
        if (payload == null) {
            return this.defaultMethod;
        }

        Class<?> payloadClass = payload.getClass();

        // First, try to find an exact match
        for (Method method : this.handlerMethods) {
            if (method.getParameterCount() > 0) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(payloadClass)) {
                    return method;
                }
            }
        }

        // If no exact match, try to find a compatible match
        for (Method method : this.handlerMethods) {
            if (method.getParameterCount() > 0) {
                Class<?> paramType = method.getParameterTypes()[0];

                // Handle special cases like byte[] to String conversion
                if (payload instanceof byte[] && paramType == String.class) {
                    return method;
                } else if (payload instanceof String && paramType == byte[].class) {
                    return method;
                }

                // Handle collection types
                if (payload instanceof List && paramType.isAssignableFrom(List.class)) {
                    Type genericType = method.getGenericParameterTypes()[0];
                    if (genericType instanceof ParameterizedType) {
                        Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                            Class<?> elementType = (Class<?>) typeArgs[0];
                            List<?> payloadList = (List<?>) payload;
                            if (!payloadList.isEmpty() && elementType.isAssignableFrom(payloadList.get(0).getClass())) {
                                return method;
                            }
                        }
                    }
                }
            }
        }

        // If no match found, return the default method
        return this.defaultMethod;
    }

    /**
     * Invoke the handler method with the message.
     *
     * @param method  the method to invoke
     * @param message the message
     */
    private void invokeHandlerMethod(Method method, Message message) {
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
                    Object rawPayload = getMessageConverter().fromMessage(message);

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
                result = method.invoke(getBean(), payload);
            } else {
                // Invoke the method with no parameters
                result = method.invoke(getBean());
            }

            // Handle result if needed
            if (result != null) {
                // Create an InvocationResult with null for sendTo and returnType
                handleResult(new InvocationResult(result, null, null, getBean(), method), message, null);
            }
        } catch (Exception ex) {
            handleInvocationException(ex, message);
        }
    }
}