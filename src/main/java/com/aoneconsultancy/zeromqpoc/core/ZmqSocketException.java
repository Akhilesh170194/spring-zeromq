package com.aoneconsultancy.zeromqpoc.core;

/**
 * Exception thrown when a ZeroMQ socket operation fails.
 * This exception is used to represent errors that occur when interacting with ZeroMQ sockets.
 */
public class ZmqSocketException extends RuntimeException {

    private final int errorCode;

    /**
     * Create a new ZmqSocketException with the specified error code and message.
     *
     * @param errorCode the ZeroMQ error code
     * @param message   the error message
     */
    public ZmqSocketException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Create a new ZmqSocketException with the specified error code, message, and cause.
     *
     * @param errorCode the ZeroMQ error code
     * @param message   the error message
     * @param cause     the cause of the exception
     */
    public ZmqSocketException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Get the ZeroMQ error code associated with this exception.
     *
     * @return the error code
     */
    public int getErrorCode() {
        return errorCode;
    }
}