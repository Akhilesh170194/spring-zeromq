package com.aoneconsultancy.zeromq.core.converter;

/**
 * Exception thrown when a message conversion fails.
 * Similar to Spring AMQP's MessageConversionException.
 */
public class ZmqMessageConversionException extends RuntimeException {

    /**
     * Create a new ZmqMessageConversionException.
     *
     * @param message the detail message
     */
    public ZmqMessageConversionException(String message) {
        super(message);
    }

    /**
     * Create a new ZmqMessageConversionException.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ZmqMessageConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}