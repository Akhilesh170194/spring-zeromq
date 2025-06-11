package com.aoneconsultancy.zeromq.core;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * A class that handles pushing messages to a ZeroMQ socket.
 * This class encapsulates the ZeroMQ socket operations for sending messages.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class ZmqPush {

    @Setter
    private String name;

    private final ZContext context;
    private final ZMQ.Socket socket;
    private final SocketType socketType;
    private final boolean bind;
    private final List<String> addresses;
    private final int sendBufferSize;
    private final int linger;

    /**
     * Create a new ZmqPush with the given parameters.
     *
     * @param context    the ZeroMQ context
     * @param endpoint    the endpoint to bind to
     * @param sendHwm the high water mark for the socket
     */
    public ZmqPush(ZContext context, String endpoint, int sendHwm) {
        this(context, SocketType.PUSH, true, List.of(endpoint), sendHwm, 1024, 0);
    }

    /**
     * Create a new ZmqPush with the given parameters.
     *
     * @param context        the ZeroMQ context
     * @param socketType     the type of socket to create
     * @param bind           whether to bind or connect the socket
     * @param endpoints      the endpoints to bind/connect to
     * @param hwm            the high water mark for the socket
     * @param sendBufferSize the send buffer size for the socket
     */
    public ZmqPush(ZContext context, SocketType socketType, boolean bind, List<String> endpoints,
                   int hwm, int sendBufferSize, int linger) {
        this.context = context;
        this.socketType = socketType;
        this.bind = bind;
        this.addresses = endpoints;
        this.sendBufferSize = sendBufferSize;
        this.linger = linger;

        this.socket = context.createSocket(socketType);
        this.socket.setHWM(hwm);

        for (String endpoint : endpoints) {
            if (bind) {
                log.debug("Binding to endpoint: {}", endpoint);
                this.socket.bind(endpoint);
            } else {
                this.socket.setSendBufferSize(sendBufferSize);
                log.debug("Connecting to endpoint: {}", endpoint);
                this.socket.connect(endpoint);
            }
        }
    }

    /**
     * Send a byte array message.
     *
     * @param data the message to send
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean send(byte[] data) {
        return socket.send(data, 0);
    }

    /**
     * Send a byte array message with the specified flags.
     *
     * @param data  the message to send
     * @param flags the send flags
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean send(byte[] data, int flags) {
        return socket.send(data, flags);
    }

    /**
     * Close the socket and release resources.
     */
    public void close() {
        socket.close();
    }
}
