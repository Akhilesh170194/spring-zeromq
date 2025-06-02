package com.aoneconsultancy.zeromq.config;

import com.aoneconsultancy.zeromq.annotation.ZmqListenerBeanPostProcessor;
import com.aoneconsultancy.zeromq.core.converter.Jackson2JsonMessageConverter;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.listener.PullZmqSocketListenerContainerFactory;
import com.aoneconsultancy.zeromq.listener.ZmqListenerContainerFactory;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpointRegistry;
import com.aoneconsultancy.zeromq.service.ZmqTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zeromq.ZContext;

/**
 * Autoconfiguration for ZeroMQ integration.
 * Similar to Spring AMQP's RabbitAutoConfiguration, this provides
 * autoconfiguration for ZeroMQ when it's present on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass({ZContext.class})
@ConditionalOnProperty(name = "zeromq.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ZmqProperties.class)
public class ZmqAutoConfiguration {

    /**
     * Configuration for ZeroMQ core components.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ZContext.class})
    @ConditionalOnProperty(name = {"zeromq.enabled", "zeromq.core.enabled"}, matchIfMissing = true)
    protected static class ZmqCoreConfiguration {

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean
        public ZContext zmqContext() {
            return new ZContext();
        }

        @Bean
        @ConditionalOnMissingBean
        public MessageConverter zmqMessageConverter(ObjectMapper objectMapper) {
            return new Jackson2JsonMessageConverter(objectMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        public ZmqTemplate zmqTemplate(ZContext context, ZmqProperties properties, MessageConverter messageConverter) {
            ZmqTemplate template = new ZmqTemplate(context, properties.getBufferSize());
            template.setMessageConverter(messageConverter);
            return template;
        }
    }

    /**
     * Configuration for ZeroMQ listener components.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ZContext.class})
    @ConditionalOnProperty(name = {"zeromq.enabled", "zeromq.listener.enabled"}, matchIfMissing = true)
    protected static class ZmqListenerConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "zmqListenerContainerFactory")
        @ConditionalOnBean({ZContext.class, MessageConverter.class})
        public ZmqListenerContainerFactory<?> zmqListenerContainerFactory(
                ZContext context,
                ZmqProperties properties,
                MessageConverter messageConverter) {
            PullZmqSocketListenerContainerFactory factory = new PullZmqSocketListenerContainerFactory(context);
            factory.setMessageConverter(messageConverter);
            factory.setConcurrency(properties.getListenerConcurrency());

            factory.setBatchSize(properties.getBatchSize());
            factory.setBatchTimeout(properties.getBatchTimeout());
            factory.setBatchTimeoutUnit(TimeUnit.MILLISECONDS);
            factory.setBufferSize(properties.getBufferSize());
            return factory;
        }

        @Bean(name = ZmqListenerConfigUtils.ZMQ_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)
        @ConditionalOnMissingBean(name = ZmqListenerConfigUtils.ZMQ_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)
        public ZmqListenerEndpointRegistry zmqListenerEndpointRegistry() {
            return new ZmqListenerEndpointRegistry();
        }

        @Bean(name = ZmqListenerConfigUtils.ZMQ_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
        @ConditionalOnMissingBean(name = ZmqListenerConfigUtils.ZMQ_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
        public ZmqListenerBeanPostProcessor zmqListenerBeanPostProcessor(ZmqListenerEndpointRegistry endpointRegistry) {
            ZmqListenerBeanPostProcessor processor = new ZmqListenerBeanPostProcessor();
            processor.setEndpointRegistry(endpointRegistry);
            return processor;
        }
    }
}
