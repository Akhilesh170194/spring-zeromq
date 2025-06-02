package com.aoneconsultancy.zeromq.core.message;

/**
 * Constants for standard ZeroMQ message headers.
 * Similar to Spring AMQP's AmqpHeaders, this provides
 * standard header names for common metadata.
 */
public final class ZmqHeaders {

    private ZmqHeaders() {
        // Prevent instantiation
    }

    /**
     * The timestamp when the message was created.
     */
    public static final String TIMESTAMP = "zmq_timestamp";

    /**
     * A unique identifier for the message.
     */
    public static final String MESSAGE_ID = "zmq_message_id";

    /**
     * The content type of the message payload.
     */
    public static final String CONTENT_TYPE = "zmq_content_type";

    /**
     * The content encoding of the message payload.
     */
    public static final String CONTENT_ENCODING = "zmq_content_encoding";

    /**
     * The reply address for request-reply patterns.
     */
    public static final String REPLY_TO = "zmq_reply_to";

    /**
     * The correlation ID for request-reply patterns.
     */
    public static final String CORRELATION_ID = "zmq_correlation_id";

    /**
     * The socket type used for the message.
     */
    public static final String SOCKET_TYPE = "zmq_socket_type";
}