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

import com.aoneconsultancy.zeromq.listener.PullZmqSocketListenerContainerFactory;
import org.springframework.boot.context.properties.PropertyMapper;
import org.zeromq.ZContext;

/**
 * Configure {@link PullZmqSocketListenerContainerFactory} with sensible defaults tuned
 * using configuration properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code PullZmqSocketListenerContainerFactory} whose configuration is based upon that
 * produced by auto-configuration.
 *
 * @author Akhilesh Singh
 * @since 1.0.0
 */
public final class PullZmqSocketListenerContainerFactoryConfigurer
        extends AbstractZmqListenerContainerFactoryConfigurer<PullZmqSocketListenerContainerFactory> {

    /**
     * Creates a new configurer that will use the given {@code rabbitProperties}.
     *
     * @param rabbitProperties properties to use
     * @since 2.6.0
     */
    public PullZmqSocketListenerContainerFactoryConfigurer(ZmqProperties rabbitProperties) {
        super(rabbitProperties);
    }

    @Override
    public void configure(PullZmqSocketListenerContainerFactory factory, ZContext context) {
        PropertyMapper map = PropertyMapper.get();
        ZmqProperties.Listener config = getZmqProperties().getListener();
        configure(factory, context, config);
        map.from(config.getPull().getAddresses()).whenNonNull().to(factory::setAddresses);
    }

}