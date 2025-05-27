package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.service.ZmqService;
import com.aoneconsultancy.zeromqpoc.service.listener.ZmqListenerBeanPostProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.integration.zeromq.ZeroMqProxy;
import org.zeromq.ZContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ZmqProperties.class)
public class ZmqConfig {

    @Bean
    public ZeroMqProxy zeroMqProxy(ZmqProperties properties) {
        ZContext context = new ZContext();
        ZeroMqProxy proxy = new ZeroMqProxy(context, ZeroMqProxy.Type.PULL_PUSH);
        proxy.setFrontendPort(extractPort(properties.getPushBindAddress()));
        proxy.setBackendPort(extractPort(properties.getPullConnectAddress()));
        return proxy;
    }

    @Bean
    public ZmqService zmqService(ZmqProperties properties, ZeroMqProxy zeroMqProxy) {
        return new ZmqService(properties, zeroMqProxy);
    }

    @Bean
    public ZmqListenerBeanPostProcessor zmqListenerBeanPostProcessor(ZmqService zmqService, ObjectMapper mapper) {
        return new ZmqListenerBeanPostProcessor(zmqService, mapper);
    }

    private int extractPort(String address) {
        int index = address.lastIndexOf(':');
        return index > 0 ? Integer.parseInt(address.substring(index + 1)) : 0;
    }
}
