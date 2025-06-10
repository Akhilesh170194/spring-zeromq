package com.aoneconsultancy.zeromq.core.error;

import com.aoneconsultancy.zeromq.core.message.Message;

/**
 * Strategy interface for handling errors that arise from a {@link ZmqListener}.
 * Similar to Spring AMQP's RabbitListenerErrorHandler, this provides
 * a way to handle exceptions thrown by listener methods.
 */
public interface ZmqListenerErrorHandler {

    /**
     * Handle the error.
     *
     * @param message   the message that was being processed when the error occurred
     * @param exception the exception that was thrown
     * @return a result which will be returned to the caller
     */
    Object handleError(Message message, Exception exception);
}