package com.aoneconsultancy.zeromq.core.event;

import lombok.Getter;

/**
 * An event that is published whenever a consumer is stopped (and not restarted).
 *
 * @author Gary Russell
 * @since 1.7
 */
@Getter
@SuppressWarnings("serial")
public class AsyncConsumerStoppedEvent extends ZmqEvent {

    private final Object consumer;

    /**
     * @param source   the listener container.
     * @param consumer the old consumer.
     */
    public AsyncConsumerStoppedEvent(Object source, Object consumer) {
        super(source);
        this.consumer = consumer;
    }

    @Override
    public String toString() {
        return "AsyncConsumerStoppedEvent [consumer=" + this.consumer
                + ", container=" + this.getSource() + "]";
    }

}
