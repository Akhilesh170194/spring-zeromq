package com.aoneconsultancy.zeromq.listener.exception;

import com.aoneconsultancy.zeromq.support.ZmqException;

/**
 * Exception class that indicates a rejected message on shutdown. Used to trigger a rollback for an
 * external transaction manager in that case.
 */
@SuppressWarnings("serial")
public class MessageRejectedWhileStoppingException extends ZmqException {

    public MessageRejectedWhileStoppingException() {
        super("Message listener container was stopping when a message was received");
    }

}
