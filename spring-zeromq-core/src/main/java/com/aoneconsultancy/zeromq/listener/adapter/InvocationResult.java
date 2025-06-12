package com.aoneconsultancy.zeromq.listener.adapter;

import lombok.Getter;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * The result of a listener method invocation.
 *
 * @author Gary Russell
 * @since 2.1
 */
public final class InvocationResult {

    @Getter
    private final Object returnValue;

    @Getter
    private final Expression sendTo;

    @Nullable
    private final Type returnType;

    @Nullable
    private final Object bean;

    @Nullable
    private final Method method;

    /**
     * Construct an instance with the provided properties.
     *
     * @param result     the result.
     * @param sendTo     the sendTo expression.
     * @param returnType the return type.
     * @param bean       the bean.
     * @param method     the method.
     */
    public InvocationResult(Object result, @Nullable Expression sendTo, @Nullable Type returnType,
                            @Nullable Object bean, @Nullable Method method) {

        this.returnValue = result;
        this.sendTo = sendTo;
        this.returnType = returnType;
        this.bean = bean;
        this.method = method;
    }

    @Nullable
    public Type getReturnType() {
        return this.returnType;
    }

    @Nullable
    public Object getBean() {
        return this.bean;
    }

    @Nullable
    public Method getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return "InvocationResult [returnValue=" + this.returnValue
                + (this.sendTo != null ? ", sendTo=" + this.sendTo : "")
                + ", returnType=" + this.returnType
                + ", bean=" + this.bean
                + ", method=" + this.method
                + "]";
    }

}
