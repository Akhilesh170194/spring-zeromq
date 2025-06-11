package com.aoneconsultancy.zeromq.core;

import java.util.List;
import lombok.EqualsAndHashCode;
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

    private final ZContext context;
    private final ZMQ.Socket socket;
    private final SocketType socketType;
    private final boolean bind;
    private final List<String> addresses;
    private final int sendBufferSize;

    /**
     * Create a new ZmqPush with the given parameters.
     *
     * @param context    the ZeroMQ context
     * @param address    the address to bind to
     * @param bufferSize the high water mark for the socket
     */
    public ZmqPush(ZContext context, String address, int bufferSize) {
        this(context, SocketType.PUSH, true, List.of(address), bufferSize, 1024);
    }

    /**
     * Create a new ZmqPush with the given parameters.
     *
     * @param context        the ZeroMQ context
     * @param socketType     the type of socket to create
     * @param bind           whether to bind or connect the socket
     * @param addresses      the addresses to bind/connect to
     * @param hwm            the high water mark for the socket
     * @param sendBufferSize the send buffer size for the socket
     */
    public ZmqPush(ZContext context, SocketType socketType, boolean bind, List<String> addresses,
                   int hwm, int sendBufferSize) {
        this.context = context;
        this.socketType = socketType;
        this.bind = bind;
        this.addresses = addresses;
        this.sendBufferSize = sendBufferSize;

        this.socket = context.createSocket(socketType);
        this.socket.setHWM(hwm);
        this.socket.setSendBufferSize(sendBufferSize);

        for (String address : addresses) {
            if (bind) {
                log.debug("Binding to address: {}", address);
                this.socket.bind(address);
            } else {
                log.debug("Connecting to address: {}", address);
                this.socket.connect(address);
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
