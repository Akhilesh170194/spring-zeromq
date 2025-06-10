package com.aoneconsultancy.zeromq.core;

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

    /**
     * Create a new ZmqPush with the given parameters.
     *
     * @param context    the ZeroMQ context
     * @param address    the address to bind to
     * @param bufferSize the high water mark for the socket
     */
    public ZmqPush(ZContext context, String address, int bufferSize) {
        this.context = context;
        this.socket = context.createSocket(SocketType.PUSH);
        this.socket.setHWM(bufferSize);
        this.socket.bind(address);
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
