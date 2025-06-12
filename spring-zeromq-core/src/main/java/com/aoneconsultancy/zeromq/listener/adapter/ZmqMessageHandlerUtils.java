package com.aoneconsultancy.zeromq.listener.adapter;

import com.aoneconsultancy.zeromq.core.message.Message;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Utility class for handling ZeroMQ listener method invocations.
 * This replaces the Spring Messaging dependency with direct method invocation.
 */
public class ZmqMessageHandlerUtils {

    /**
     * Check if a method can accept a ZmqMessage parameter.
     *
     * @param method the method to check
     * @return true if the method can accept a ZmqMessage
     */
    public static boolean acceptsZmqMessage(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        return paramTypes.length == 1 && paramTypes[0].isAssignableFrom(Message.class);
    }

    /**
     * Check if a method can accept a List<ZmqMessage> parameter.
     *
     * @param method the method to check
     * @return true if the method can accept a List<ZmqMessage>
     */
    public static boolean acceptsZmqMessageList(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1 || !List.class.isAssignableFrom(paramTypes[0])) {
            return false;
        }

        Type paramType = method.getGenericParameterTypes()[0];
        return paramType instanceof ParameterizedType &&
                isList((ParameterizedType) paramType, Message.class);
    }

    /**
     * Check if a method can accept a byte[] parameter.
     *
     * @param method the method to check
     * @return true if the method can accept a byte[]
     */
    public static boolean acceptsByteArray(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        return paramTypes.length == 1 && paramTypes[0] == byte[].class;
    }

    /**
     * Check if a method takes no parameters.
     *
     * @param method the method to check
     * @return true if the method takes no parameters
     */
    public static boolean acceptsNoParameters(Method method) {
        return method.getParameterCount() == 0;
    }

    /**
     * Check if a type is a List of a specific element type.
     *
     * @param type        the parameterized type to check
     * @param elementType the expected element type
     * @return true if the type is a List of the specified element type
     */
    private static boolean isList(ParameterizedType type, Class<?> elementType) {
        if (!List.class.isAssignableFrom((Class<?>) type.getRawType())) {
            return false;
        }

        Type[] typeArguments = type.getActualTypeArguments();
        return typeArguments.length == 1 &&
                typeArguments[0].equals(elementType);
    }
}