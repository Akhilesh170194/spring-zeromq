package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.annotation.ZmqListenerBeanPostProcessor;
import com.aoneconsultancy.zeromqpoc.core.converter.Jackson2JsonMessageConverter;
import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.listener.SimpleZmqListenerContainerFactory;
import com.aoneconsultancy.zeromqpoc.listener.ZmqListenerContainerFactory;
import com.aoneconsultancy.zeromqpoc.service.ZmqTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@EnableConfigurationProperties(ZmqProperties.class)
public class ZmqAutoConfiguration {

    /**
     * Configuration for ZeroMQ core components.
     */
    @Configuration
    @ConditionalOnClass({ZContext.class})
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
            ZmqTemplate template = new ZmqTemplate(context, properties.getBufferSize(), messageConverter);
            template.setDefaultAddress(properties.getPushBindAddress());
            return template;
        }
    }

    /**
     * Configuration for ZeroMQ listener components.
     */
    @Configuration
    @ConditionalOnClass({ZContext.class})
    protected static class ZmqListenerConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "zmqListenerContainerFactory")
        public ZmqListenerContainerFactory<?> zmqListenerContainerFactory(
                ZContext context,
                ZmqProperties properties,
                MessageConverter messageConverter) {
            SimpleZmqListenerContainerFactory factory = new SimpleZmqListenerContainerFactory(context, properties);
            factory.setMessageConverter(messageConverter);
            factory.setDefaultConcurrency(properties.getListenerConcurrency());
            factory.setDefaultAddress(properties.getPullConnectAddress());
            factory.setBatchSize(properties.getBatchSize());
            factory.setBatchTimeout(properties.getBatchTimeout());
            factory.setBatchTimeoutUnit(TimeUnit.MILLISECONDS);
            factory.setBufferSize(properties.getBufferSize());
            return factory;
        }

        @Bean
        @ConditionalOnMissingBean
        public ZmqListenerBeanPostProcessor zmqListenerBeanPostProcessor() {
            return new ZmqListenerBeanPostProcessor();
        }
    }
}
