package com.aoneconsultancy.zeromqpoc.annotation;

import com.aoneconsultancy.zeromqpoc.config.ZmqListenerConfigUtils;
import com.aoneconsultancy.zeromqpoc.listener.endpoint.ZmqListenerEndpointRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * An {@link ImportSelector} implementation that selects the configuration classes
 * for the ZeroMQ listener infrastructure.
 * Similar to Spring AMQP's RabbitListenerConfigurationSelector, this selects
 * the configuration classes to import based on the {@link EnableZmq} annotation.
 */
public class ZmqListenerConfigurationSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        List<String> imports = new ArrayList<>();

        imports.add(ZmqListenerConfiguration.class.getName());

        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableZmq.class.getName());
        if (attributes != null) {
            boolean createEndpointRegistry = (Boolean) attributes.get("createEndpointRegistry");
            if (createEndpointRegistry) {
                imports.add(ZmqListenerEndpointRegistrarConfiguration.class.getName());
            }
        }

        return imports.toArray(new String[0]);
    }

    /**
     * Configuration class for the ZeroMQ listener infrastructure.
     */
    @Configuration(proxyBeanMethods = false)
    public static class ZmqListenerConfiguration {

        @Bean(name = ZmqListenerConfigUtils.ZMQ_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
        public ZmqListenerBeanPostProcessor zmqListenerBeanPostProcessor(ZmqListenerEndpointRegistry zmqListenerEndpointRegistry) {
            ZmqListenerBeanPostProcessor zmqListenerBeanPostProcessor = new ZmqListenerBeanPostProcessor();
            zmqListenerBeanPostProcessor.setEndpointRegistry(zmqListenerEndpointRegistry);
            return zmqListenerBeanPostProcessor;
        }

    }

    /**
     * Configuration class for the ZeroMQ listener endpoint registry.
     */
    @Configuration(proxyBeanMethods = false)
    public static class ZmqListenerEndpointRegistrarConfiguration {

        @Bean(name = ZmqListenerConfigUtils.ZMQ_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)
        public ZmqListenerEndpointRegistry zmqListenerEndpointRegistry() {
            return new ZmqListenerEndpointRegistry();
        }
    }
}