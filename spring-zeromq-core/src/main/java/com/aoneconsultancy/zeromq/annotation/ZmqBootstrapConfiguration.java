package com.aoneconsultancy.zeromq.annotation;

import com.aoneconsultancy.zeromq.config.ZmqListenerConfigUtils;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpointRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

public class ZmqBootstrapConfiguration implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(@Nullable AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {

        if (!registry.containsBeanDefinition(
                ZmqListenerConfigUtils.ZMQ_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)) {

            registry.registerBeanDefinition(ZmqListenerConfigUtils.ZMQ_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME,
                    new RootBeanDefinition(ZmqListenerAnnotationBeanPostProcessor.class));
        }

        if (!registry.containsBeanDefinition(ZmqListenerConfigUtils.ZMQ_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)) {
            registry.registerBeanDefinition(ZmqListenerConfigUtils.ZMQ_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME,
                    new RootBeanDefinition(ZmqListenerEndpointRegistry.class));
        }
    }
}
