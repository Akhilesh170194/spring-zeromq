package com.aoneconsultancy.zeromq.core.event;

import com.aoneconsultancy.zeromq.listener.MessageListenerContainer;
import lombok.Getter;

/**
 * Event published when a consumer fails.
 */

@Getter
public class ZmqConsumerFailedEvent extends ZmqEvent {

    private final String reason;

    private final boolean fatal;

    private final Throwable throwable;

    /**
     * Create a new ZmqListenerContainerConsumerFailedEvent.
     *
     * @param source    the source container
     * @param reason    the reason for the failure
     * @param throwable the throwable that caused the failure, if any
     * @param fatal     whether the failure is fatal
     */
    public ZmqConsumerFailedEvent(MessageListenerContainer source, String reason, Throwable throwable, boolean fatal) {
        super(source);
        this.reason = reason;
        this.throwable = throwable;
        this.fatal = fatal;
    }

    public String toString() {
        return "ZmqConsumerFailedEvent [reason=" + this.reason + ", fatal=" + this.fatal + ", throwable="
                + this.throwable + ", container=" + this.source + "]";
    }

}