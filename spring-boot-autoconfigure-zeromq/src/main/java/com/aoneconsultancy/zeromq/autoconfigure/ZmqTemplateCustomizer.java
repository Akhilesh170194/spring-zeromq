package com.aoneconsultancy.zeromq.autoconfigure;

import com.aoneconsultancy.zeromq.core.ZmqTemplate;

/**
 * Callback interface that can be used to customize a {@link ZmqTemplate}.
 */
@FunctionalInterface
public interface ZmqTemplateCustomizer {

    /**
     * Callback to customize a {@link ZmqTemplate} instance.
     *
     * @param rabbitTemplate the zmqTemplate to customize
     */
    void customize(ZmqTemplate rabbitTemplate);
}
