package com.aoneconsultancy.zeromq.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZmqHandler {

    /**
     * When true, designate that this is the default fallback method if the payload type
     * matches no other {@link ZmqHandler} method. Only one method can be so designated.
     *
     * @return true if this is the default method.
     * @since 2.0.3
     */
    boolean isDefault() default false;

}
