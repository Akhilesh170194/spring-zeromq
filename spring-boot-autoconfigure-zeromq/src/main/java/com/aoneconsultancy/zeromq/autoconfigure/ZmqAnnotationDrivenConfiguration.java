package com.aoneconsultancy.zeromq.autoconfigure;

import com.aoneconsultancy.zeromq.annotation.EnableZmq;
import com.aoneconsultancy.zeromq.config.ContainerCustomizer;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.listener.PullZmqSocketListenerContainerFactory;
import com.aoneconsultancy.zeromq.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zeromq.ZContext;

/**
 * Configuration for Spring AMQP annotation driven endpoints.
 *
 * @author Stephane Nicoll
 * @author Josh Thornhill
 * @author Moritz Halbritter
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableZmq.class)
class ZmqAnnotationDrivenConfiguration {

    private final ObjectProvider<MessageConverter> messageConverter;

    private final ZmqProperties properties;

    ZmqAnnotationDrivenConfiguration(ObjectProvider<MessageConverter> messageConverter,
                                     ZmqProperties properties) {
        this.messageConverter = messageConverter;
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    ZContext zContext() {
        // Create ZContext with the number of IO threads
        ZContext context = new ZContext(this.properties.getContextThread());

        // Note: ZContext doesn't have a setMaxSockets method in the current version
        // The maxSockets property is stored in ZmqProperties for future use

        // Set linger period if configured
        if (this.properties.getLinger() != null) {
            context.setLinger(this.properties.getLinger());
        }

        return context;
    }

    @Bean
    @ConditionalOnMissingBean
    PullZmqSocketListenerContainerFactoryConfigurer simpleZmqListenerContainerFactoryConfigurer() {
        PullZmqSocketListenerContainerFactoryConfigurer configurer = new PullZmqSocketListenerContainerFactoryConfigurer(
                this.properties);
        configurer.setMessageConverter(this.messageConverter.getIfUnique());
        return configurer;
    }

    @Bean(name = "zmqListenerContainerFactory")
    @ConditionalOnMissingBean(name = "zmqListenerContainerFactory")
    @ConditionalOnProperty(prefix = "spring.zmq.listener", name = "type", havingValue = "PULL",
            matchIfMissing = true)
    PullZmqSocketListenerContainerFactory simpleRabbitListenerContainerFactory(
            PullZmqSocketListenerContainerFactoryConfigurer configurer,
            ObjectProvider<ContainerCustomizer<SimpleMessageListenerContainer>> simpleContainerCustomizer,
            ZContext zContext) {
        PullZmqSocketListenerContainerFactory factory = new PullZmqSocketListenerContainerFactory(zContext);
        configurer.configure(factory, zContext);
        simpleContainerCustomizer.ifUnique(factory::setContainerCustomizer);
        return factory;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableZmq
    @ConditionalOnMissingBean(name = ZmqListenerConfigUtils.ZMQ_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
    static class EnableRabbitConfiguration {

    }

}
