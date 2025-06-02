package com.aoneconsultancy.zeromq.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation that aggregates several {@link ZmqListener} annotations.
 * <p>
 * Can be used natively, declaring several nested {@link ZmqListener} annotations.
 * Can also be used in conjunction with Java 8's support for repeatable annotations,
 * where {@link ZmqListener} can simply be declared several times on the same method
 * (or class), implicitly generating this container annotation.
 *
 * @see ZmqListener
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZmqListeners {

    ZmqListener[] value();

}
