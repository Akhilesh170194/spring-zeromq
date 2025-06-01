package com.aoneconsultancy.zeromqpoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation for methods that should receive messages from ZeroMQ.
 * Similar to Spring AMQP's @RabbitListener, this provides configuration
 * options for the listener.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ZmqListener {

    /**
     * The unique identifier of the container managing for this endpoint.
     * <p>If none is specified an auto-generated one is provided.
     *
     * @return the {@code id} for the container managing for this endpoint.
     */
    String id() default "";

    /**
     * The socket address to connect to, e.g. tcp://localhost:5555.
     * If not specified, the default address from properties will be used.
     */
    @AliasFor("address")
    String value() default "";

    /**
     * The socket address to connect to, e.g. tcp://localhost:5555.
     * If not specified, the default address from properties will be used.
     */
    @AliasFor("value")
    String address() default "";

    /**
     * The socket type to use.
     * Defaults to PULL.
     */
    SocketType socketType() default SocketType.PULL;

    /**
     * The concurrency for this listener (number of threads).
     * Defaults to 1.
     */
    int concurrency() default 1;

    /**
     * The name of the container factory to use.
     * Defaults to "zmqListenerContainerFactory".
     */
    String containerFactory() default "zmqListenerContainerFactory";

    /**
     * Override the container factory's message converter used for this listener.
     *
     * @return the message converter bean name. If a SpEL expression is provided
     * ({@code #{...}}), the expression can either evaluate to a converter instance
     * or a bean name.
     * @since 2.3
     */
    String messageConverter() default "";

    /**
     * Override the container factory's {@code batchListener} property. The listener
     * method signature should receive a {@code List<?>}; refer to the reference
     * documentation. This allows a single container factory to be used for both record
     * and batch listeners; previously separate container factories were required.
     *
     * @return "true" for the annotated method to be a batch listener or "false" for a
     * single message listener. If not set, the container factory setting is used. SpEL and
     * property place holders are not supported because the listener type cannot be
     * variable.
     * @see Boolean#parseBoolean(String)
     * @since 3.0
     */
    boolean batch() default false;

    /**
     * Supported socket types for ZeroMQ listeners.
     */
    enum SocketType {
        /**
         * PULL socket - receives messages from a PUSH socket.
         */
        PULL,

        /**
         * SUB socket - receives messages from a PUB socket.
         */
        SUB,

        /**
         * REP socket - receives requests and sends replies.
         */
        REP
    }
}