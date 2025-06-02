package com.aoneconsultancy.zeromq.core;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring application event representing a ZeroMQ socket event.
 */
@Getter
public class ZmqSocketEvent extends ApplicationEvent {

    private final String socketId;
    private final int eventType;
    private final int eventValue;
    private final String address;

    /**
     * Create a new ZmqSocketEvent.
     *
     * @param socketId   the ID of the socket that generated the event
     * @param eventType  the event type as defined in ZmqSocketMonitor.EVENT_*
     * @param eventValue additional value associated with the event
     * @param address    the address associated with the event, or null if none
     */
    public ZmqSocketEvent(String socketId, int eventType, int eventValue, String address) {
        super(socketId);
        this.socketId = socketId;
        this.eventType = eventType;
        this.eventValue = eventValue;
        this.address = address;
    }

    @Override
    public String toString() {
        return String.format("ZmqSocketEvent[socketId=%s, eventType=%d, eventValue=%d, address=%s]",
                socketId, eventType, eventValue, address);
    }
}
