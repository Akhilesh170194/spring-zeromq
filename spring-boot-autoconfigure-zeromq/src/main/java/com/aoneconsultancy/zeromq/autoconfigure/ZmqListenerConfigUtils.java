package com.aoneconsultancy.zeromq.autoconfigure;

/**
 * Configuration constants for the ZeroMQ listener infrastructure.
 * Similar to Spring AMQP's RabbitListenerConfigUtils, this provides
 * constants for bean names and other configuration values.
 */
public abstract class ZmqListenerConfigUtils {

    /**
     * The bean name of the internally managed ZeroMQ listener annotation processor.
     */
    public static final String ZMQ_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME =
            "com.aoneconsultancy.zeromq.annotation.internalZmqListenerBeanPostProcessor";

    /**
     * The bean name of the internally managed ZeroMQ listener endpoint registry.
     */
    public static final String ZMQ_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME =
            "com.aoneconsultancy.zeromq.listener.endpoint.internalZmqListenerEndpointRegistry";

}