package com.aoneconsultancy.zeromqpoc.annotation;

import com.aoneconsultancy.zeromqpoc.listener.ZmqListenerContainerFactory;
import com.aoneconsultancy.zeromqpoc.listener.endpoint.ZmqListenerEndpointRegistry;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Enable ZeroMQ listener annotated endpoints.
 * Similar to Spring AMQP's @EnableRabbit, this enables detection of
 * {@link ZmqListener} annotations on any Spring-managed bean in the container.
 * <p>
 * To be used on @{@link org.springframework.context.annotation.Configuration Configuration}
 * classes as follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableZmq
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public ZmqListenerContainerFactory&lt;?&gt; zmqListenerContainerFactory() {
 *         SimpleZmqListenerContainerFactory factory = new SimpleZmqListenerContainerFactory();
 *         factory.setConnectionFactory(connectionFactory());
 *         factory.setMessageConverter(messageConverter());
 *         return factory;
 *     }
 *
 *     &#064;Bean
 *     public ZContext zmqContext() {
 *         return new ZContext();
 *     }
 * }
 * </pre>
 * <p>
 * The {@code zmqListenerContainerFactory} is required, unless an explicit default
 * has been provided through configuration.
 *
 * <p>Note that {@code ZmqAutoConfiguration} enables ZMQ listener support by default.
 *
 * @see ZmqListener
 * @see ZmqListenerBeanPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ZmqListenerConfigurationSelector.class)
public @interface EnableZmq {

    /**
     * Alias for {@link #containerFactory()}.
     *
     * @return the container factory bean name
     */
    @AliasFor("containerFactory")
    String value() default "";

    /**
     * The bean name of the {@link ZmqListenerContainerFactory}
     * to use by default.
     * <p>If none is specified, "zmqListenerContainerFactory" is assumed to be defined.
     *
     * @return the container factory bean name
     */
    @AliasFor("value")
    String containerFactory() default "";

    /**
     * Specify whether to create a default {@link ZmqListenerEndpointRegistry} bean
     * in the context.
     *
     * @return whether to create the registry as a bean
     */
    boolean createEndpointRegistry() default true;

}