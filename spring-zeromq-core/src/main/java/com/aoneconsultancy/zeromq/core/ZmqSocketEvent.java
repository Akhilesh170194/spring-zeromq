package com.aoneconsultancy.zeromq.core;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring application event representing a ZeroMQ socket event.
 */
@Getter
public class ZmqSocketEvent extends ApplicationEvent {

    private final String socketName;
    private final int eventType;
    private final int eventValue;
    private final String address;

    /**
     * Create a new ZmqSocketEvent.
     *
     * @param socketName the ID of the socket that generated the event
     * @param eventType  the event type as defined in ZmqSocketMonitor.EVENT_*
     * @param eventValue additional value associated with the event
     * @param address    the address associated with the event, or null if none
     */
    public ZmqSocketEvent(String socketName, int eventType, int eventValue, String address) {
        super(socketName);
        this.socketName = socketName;
        this.eventType = eventType;
        this.eventValue = eventValue;
        this.address = address;
    }

    @Override
    public String toString() {
        return String.format("ZmqSocketEvent[socketName=%s, eventType=%d, eventValue=%d, address=%s]",
                socketName, eventType, eventValue, address);
    }
}
