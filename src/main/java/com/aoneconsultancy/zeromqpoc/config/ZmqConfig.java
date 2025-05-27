package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.service.ZmqService;
import com.aoneconsultancy.zeromqpoc.service.ZmqTemplate;
import com.aoneconsultancy.zeromqpoc.service.listener.SimpleZmqListenerContainerFactory;
import com.aoneconsultancy.zeromqpoc.service.listener.ZmqListenerBeanPostProcessor;
import com.aoneconsultancy.zeromqpoc.service.listener.ZmqListenerContainerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ZmqProperties.class)
public class ZmqConfig {

    @Bean
    public ZmqService zmqService(ZmqProperties properties) {
        return new ZmqService(properties);
    }

    @Bean
    public ZmqTemplate zmqTemplate(ZmqService zmqService, ObjectMapper mapper) {
        return new ZmqTemplate(zmqService, mapper);
    }

    @Bean
    public ZmqListenerContainerFactory<?> zmqListenerContainerFactory(ZmqService zmqService,
                                                                      ZmqProperties properties) {
        return new SimpleZmqListenerContainerFactory(zmqService, properties);
    }

    @Bean
    public ZmqListenerBeanPostProcessor zmqListenerBeanPostProcessor(ZmqListenerContainerFactory<?> factory,
                                                                    ObjectMapper mapper) {
        return new ZmqListenerBeanPostProcessor(factory, mapper);
    }

}
