package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.listener.SimpleZmqListenerContainerFactory;
import com.aoneconsultancy.zeromqpoc.listener.ZmqListenerContainerFactory;
import com.aoneconsultancy.zeromqpoc.service.ZmqTemplate;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.zeromq.ZContext;

/**
 * Test configuration for ZeroMQ integration tests.
 * Provides beans needed for testing.
 */
@TestConfiguration
public class ZmqTestConfiguration {

    /**
     * Create a ZmqProperties bean for testing.
     *
     * @return a ZmqProperties instance
     */
    @Bean
    @Primary
    public ZmqProperties zmqTestProperties() {
        ZmqProperties properties = new ZmqProperties();
        properties.setPushBindAddress("tcp://*:5557");
        properties.setPullConnectAddress("tcp://localhost:5557");
        properties.setBufferSize(1000);
        properties.setListenerConcurrency(1);
        properties.setBatchSize(10);
        properties.setBatchTimeout(1000);
        return properties;
    }

    /**
     * Create a ZmqTemplate bean that uses the same port as ZmqService.
     * This ensures that the DemoController uses the same port as the test.
     *
     * @param context          the ZeroMQ context
     * @param properties       the ZeroMQ properties
     * @param messageConverter the message converter
     * @return a ZmqTemplate instance
     */
    @Bean
    @Primary
    public ZmqTemplate zmqTemplate(ZContext context, ZmqProperties properties, MessageConverter messageConverter) {
        ZmqTemplate template = new ZmqTemplate(context, properties.getBufferSize(), messageConverter);
        template.setDefaultAddress(properties.getPushBindAddress());
        return template;
    }

    /**
     * Create a ZmqListenerContainerFactory bean that uses the same port as ZmqService.
     * This ensures that the DemoListener uses the same port as the test.
     *
     * @param context          the ZeroMQ context
     * @param properties       the ZeroMQ properties
     * @param messageConverter the message converter
     * @return a ZmqListenerContainerFactory instance
     */
    @Bean
    @Primary
    public ZmqListenerContainerFactory<?> zmqListenerContainerFactory(
            ZContext context,
            ZmqProperties properties,
            MessageConverter messageConverter) {
        SimpleZmqListenerContainerFactory factory = new SimpleZmqListenerContainerFactory(context, properties);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrency(properties.getListenerConcurrency());
        factory.setDefaultAddress(properties.getPullConnectAddress());
        factory.setBatchSize(properties.getBatchSize());
        factory.setBatchTimeout(properties.getBatchTimeout());
        factory.setBatchTimeoutUnit(TimeUnit.MILLISECONDS);
        factory.setBufferSize(properties.getBufferSize());
        return factory;
    }
}
