/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aoneconsultancy.zeromq.autoconfigure;

import com.aoneconsultancy.zeromq.core.ZmqTemplate;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.support.ZmqException;
import lombok.Setter;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.Assert;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

/**
 * Configure {@link ZmqTemplate} with sensible defaults tuned using configuration
 * properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code ZmqTemplateConfigurer} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Akhilesh Singh
 * @since 1.0.0
 */
public class ZmqTemplateConfigurer {

    @Setter
    private MessageConverter messageConverter;

    private final ZmqProperties zmqProperties;

    /**
     * Creates a new configurer that will use the given {@code zmqProperties}.
     *
     * @param zmqProperties properties to use
     * @since 1.0.0
     */
    public ZmqTemplateConfigurer(ZmqProperties zmqProperties) {
        Assert.notNull(zmqProperties, "'zmqProperties' must not be null");
        this.zmqProperties = zmqProperties;
    }


    protected final ZmqProperties getZmqProperties() {
        return this.zmqProperties;
    }

    /**
     * Configure the specified {@link ZmqTemplate}. The template can be further tuned
     * and default settings can be overridden.
     *
     * @param template the {@link ZmqTemplate} instance to configure
     * @param context  the {@link ZContext} to use
     */
    public void configure(ZmqTemplate template, ZContext context) {
        PropertyMapper map = PropertyMapper.get();
        if (this.messageConverter != null) {
            template.setMessageConverter(this.messageConverter);
        }
        map.from(context).whenNonNull().to(template::setContext);

        // Configure a template with properties from the new structure
        ZmqProperties.Template templateConfig = this.zmqProperties.getTemplate();
        ZmqProperties.Template.Producer producer = templateConfig.getProducer();
        ZmqProperties.Listener.Consumer consumer = this.zmqProperties.getListener().getConsumer();

        // Validate socket type compatibility
        if (producer != null && consumer != null) {
            if (consumer.getType() == SocketType.PULL && !producer.getType().equals(SocketType.PUSH)) {
                throw new ZmqException("Invalid producer type for PULL consumer. Must be PUSH. Found: " + producer.getType());
            }
            map.from(producer.getType()).whenNonNull().to(template::setSocketType);
        }

        // Apply template configuration
        map.from(templateConfig.getSendTimeout()).to(template::setSendTimeout);
        map.from(templateConfig.getPollRetry()).to(template::setPollRetry);
        map.from(templateConfig.getRetryDelay()).to(template::setRetryDelay);
        map.from(templateConfig.isBackpressureEnabled()).to(template::setBackpressureEnabled);

        // Set default address if configured
        if (templateConfig.getDefaultSocket() != null) {
            template.setDefaultId(templateConfig.getDefaultSocket());
        }

        // If producer has addresses, use the first one as default if not already set
        if (producer != null && !producer.getAddresses().isEmpty() && template.getDefaultId() == null) {
            template.setDefaultId(producer.getAddresses().get(0));
        }
    }
}
