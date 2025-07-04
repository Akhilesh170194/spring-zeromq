package com.aoneconsultancy.zeromq.support;

import com.aoneconsultancy.zeromq.core.message.Message;

import java.util.*;

public class ListenerExecutionFailedException extends ZmqException {
    private final List<Message> failedMessages = new ArrayList<>();

    /**
     * Constructor for ListenerExecutionFailedException.
     *
     * @param msg           the detail message
     * @param cause         the exception thrown by the listener method
     * @param failedMessage the message(s) that failed
     */
    public ListenerExecutionFailedException(String msg, Throwable cause, Message... failedMessage) {
        super(msg, cause);
        this.failedMessages.addAll(Arrays.asList(failedMessage));
    }

    public Message getFailedMessage() {
        return this.failedMessages.get(0);
    }

    public Collection<Message> getFailedMessages() {
        return Collections.unmodifiableList(this.failedMessages);
    }
}
