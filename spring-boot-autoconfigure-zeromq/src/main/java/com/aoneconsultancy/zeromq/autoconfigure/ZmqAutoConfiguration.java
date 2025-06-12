package com.aoneconsultancy.zeromq.autoconfigure;

import com.aoneconsultancy.zeromq.config.ZmqProducerProperties;
import com.aoneconsultancy.zeromq.core.ZmqTemplate;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
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
    protected static class ZmqConnectionFactoryCreator {

        protected ZmqConnectionFactoryCreator() {
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ZmqConnectionFactoryCreator.class)
    protected static class ZmqTemplateConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ZmqTemplateConfigurer zmqTemplateConfigurer(ZmqProperties properties,
                                                           ObjectProvider<MessageConverter> messageConverter) {
            ZmqTemplateConfigurer configurer = new ZmqTemplateConfigurer(properties);
            configurer.setMessageConverter(messageConverter.getIfUnique());
            return configurer;
        }

        @Bean
        @ConditionalOnMissingBean
        public ZmqTemplate zmqTemplate(ZmqTemplateConfigurer configurer,
                                       ObjectProvider<ZmqTemplateCustomizer> customizers,
                                       ZContext zContext,
                                       ZmqProperties properties) {
            ZmqProperties.Template templateConfig = properties.getTemplate();
            ZmqProducerProperties producer = templateConfig.getProducer();

            // Create a template with all configuration parameters
            ZmqTemplate template = new ZmqTemplate(
                    zContext,
                    producer,
                    templateConfig.getSocketHwm(),
                    templateConfig.getSocketSendBuffer(),
                    properties.getLinger()
            );

            // Set socket endpoints
            String defaultSocket = templateConfig.getDefaultEndpoint();
            template.setDefaultEndpointName(defaultSocket);

            // Apply additional configuration
            configurer.configure(template, zContext);
            customizers.orderedStream().forEach((customizer) -> customizer.customize(template));
            return template;
        }

    }
}
