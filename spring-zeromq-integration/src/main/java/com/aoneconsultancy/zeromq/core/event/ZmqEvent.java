package com.aoneconsultancy.zeromq.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for all ZMQ events.
 */
public class ZmqEvent extends ApplicationEvent {

    /**
     * Create a new ZmqListenerContainerEvent.
     *
     * @param source the source container
     */
    public ZmqEvent(Object source) {
        super(source);
    }

}