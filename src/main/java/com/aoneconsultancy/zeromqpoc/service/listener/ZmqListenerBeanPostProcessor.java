package com.aoneconsultancy.zeromqpoc.service.listener;

import com.aoneconsultancy.zeromqpoc.service.ZmqService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Registers methods annotated with {@link ZmqListener} to receive messages
 * from {@link ZmqService}.
 */
public class ZmqListenerBeanPostProcessor implements BeanPostProcessor {

    private final ZmqService zmqService;
    private final ObjectMapper mapper;

    public ZmqListenerBeanPostProcessor(ZmqService zmqService, ObjectMapper mapper) {
        this.zmqService = zmqService;
        this.mapper = mapper;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        ReflectionUtils.doWithMethods(targetClass, method -> registerMethod(bean, method),
                method -> method.isAnnotationPresent(ZmqListener.class));
        return bean;
    }

    private void registerMethod(Object bean, Method method) {
        Class<?> paramType = method.getParameterCount() == 1 ? method.getParameterTypes()[0] : byte[].class;
        method.setAccessible(true);
        Consumer<byte[]> listener = bytes -> {
            try {
                Object arg;
                if (paramType == byte[].class) {
                    arg = bytes;
                } else if (paramType == String.class) {
                    arg = new String(bytes);
                } else {
                    arg = mapper.readValue(bytes, paramType);
                }
                method.invoke(bean, arg);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke ZmqListener method", e);
            }
        };
        zmqService.registerListener(listener);
    }
}
