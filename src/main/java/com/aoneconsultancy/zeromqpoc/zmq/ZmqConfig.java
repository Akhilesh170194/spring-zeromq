package com.aoneconsultancy.zeromqpoc.zmq;

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
    public ZmqListenerBeanPostProcessor zmqListenerBeanPostProcessor(ZmqService zmqService, ObjectMapper mapper) {
        return new ZmqListenerBeanPostProcessor(zmqService, mapper);
    }
}
