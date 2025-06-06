package com.aoneconsultancy.zeromq.autoconfigure;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.service.ZmqTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.zeromq.ZContext;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ZmqTemplate}.
 * <p>
 * This configuration class is active only when the RabbitMQ and Spring AMQP client
 * libraries are on the classpath.
 * <p>
 * Registers the following beans:
 * <ul>
 * <li>{@link ZmqTemplate RabbitTemplate} if there
 * is no other bean of the same type in the context.</li>
 *
 * @author Greg Turnquist
 * @author Josh Long
 * @author Stephane Nicoll
 * @author Gary Russell
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ZmqTemplate.class, ZContext.class})
@EnableConfigurationProperties(ZmqProperties.class)
@Import({ZmqAnnotationDrivenConfiguration.class})
public class ZmqAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    protected static class RabbitConnectionFactoryCreator {

        private final ZmqProperties properties;

        protected RabbitConnectionFactoryCreator(ZmqProperties properties) {
            this.properties = properties;
        }

        @Configuration(proxyBeanMethods = false)
        @Import(RabbitConnectionFactoryCreator.class)
        protected static class RabbitTemplateConfiguration {

            @Bean
            @ConditionalOnMissingBean
            public ZmqTemplateConfigurer rabbitTemplateConfigurer(ZmqProperties properties,
                                                                  ObjectProvider<MessageConverter> messageConverter) {
                ZmqTemplateConfigurer configurer = new ZmqTemplateConfigurer(properties);
                configurer.setMessageConverter(messageConverter.getIfUnique());
                return configurer;
            }

            @Bean
            @ConditionalOnMissingBean
            public ZmqTemplate rabbitTemplate(ZmqTemplateConfigurer configurer,
                                              ObjectProvider<ZmqTemplateConfigurer> customizers, ZContext zContext) {
                ZmqTemplate template = new ZmqTemplate(zContext);
                configurer.configure(template, zContext);
                return template;
            }

        }
    }
}
