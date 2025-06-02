package com.aoneconsultancy.zeromq.annotation;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * An {@link ImportSelector} implementation that selects the configuration classes
 * for the ZeroMQ listener infrastructure.
 * Similar to Spring AMQP's RabbitListenerConfigurationSelector, this selects
 * the configuration classes to import based on the {@link EnableZmq} annotation.
 */
public class ZmqListenerConfigurationSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{ZmqBootstrapConfiguration.class.getName()};
    }

}