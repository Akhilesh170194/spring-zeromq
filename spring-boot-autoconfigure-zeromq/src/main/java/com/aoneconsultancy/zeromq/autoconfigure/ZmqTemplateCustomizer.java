package com.aoneconsultancy.zeromq.autoconfigure;

import com.aoneconsultancy.zeromq.service.ZmqTemplate;

/**
 * Callback interface that can be used to customize a {@link RabbitTemplate}.
 */
@FunctionalInterface
public interface ZmqTemplateCustomizer {

    /**
     * Callback to customize a {@link ZmqTemplate} instance.
     *
     * @param rabbitTemplate the rabbitTemplate to customize
     */
    void customize(ZmqTemplate rabbitTemplate);
}
