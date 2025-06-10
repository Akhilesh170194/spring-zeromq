package com.aoneconsultancy.zeromq.config;

import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpointRegistrar;

/**
 * Interface to be implemented by objects that configure the ZeroMQ listener
 * endpoint registrar.
 * Similar to Spring AMQP's RabbitListenerConfigurer, this provides
 * a way to customize the registrar programmatically.
 */
public interface ZmqListenerConfigurer {

    /**
     * Configure the given registrar.
     *
     * @param registrar the registrar to configure
     */
    void configureZmqListeners(ZmqListenerEndpointRegistrar registrar);

}