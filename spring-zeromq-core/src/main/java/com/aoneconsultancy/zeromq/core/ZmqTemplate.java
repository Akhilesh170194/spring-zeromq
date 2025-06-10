package com.aoneconsultancy.zeromq.core;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.converter.SimpleMessageConverter;
import com.aoneconsultancy.zeromq.core.converter.ZmqMessageConversionException;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Helper similar to Spring's {@code RabbitTemplate} for sending
 * messages over ZeroMQ.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class ZmqTemplate implements DisposableBean {

    private ZContext context;
    private final Map<String, ZmqPush> pushSockets = new HashMap<>();
    private final int bufferSize;

    public void start() {
        ZMQ.Socket socket = context.createSocket(SocketType.PUSH);
        socket.setSendBufferSize(1000);
        socket.setSndHWM(1000);
        socket.setPlainUsername("abc");
    }

    /**
     * Get the message converter used by this template.
     */
    @Getter
    @Setter
    private MessageConverter messageConverter = new SimpleMessageConverter();

    /**
     * Set the post-processor to apply before sending messages.
     */
    @Setter
    private MessagePostProcessor beforeSendPostProcessor;

    /**
     * Set the default address to send messages to.
     */
    @Setter
    private String defaultAddress;

    /**
     * Create a new template with the given context, default buffer size, and a default JSON message converter.
     *
     * @param context the ZeroMQ context
     */
    public ZmqTemplate(ZContext context) {
        this(context, 1000);
    }

    /**
     * Create a new template with the given context, buffer size, and message converter.
     *
     * @param context    the ZeroMQ context
     * @param bufferSize the buffer size (high water mark) for sockets
     */
    public ZmqTemplate(ZContext context, int bufferSize) {
        this.context = context;
        this.bufferSize = bufferSize;
    }

    /**
     * Get or create a push socket for the specified address.
     *
     * @param address the address to bind to
     * @return the push socket
     */
    private synchronized ZmqPush getPushSocket(String address) {
        return pushSockets.computeIfAbsent(address, addr -> new ZmqPush(context, addr, bufferSize));
    }

    /**
     * Send a pre-serialized byte array.
     *
     * @param payload the bytes to send
     */
    public void sendBytes(byte[] payload) {
        if (defaultAddress == null) {
            throw new IllegalStateException("No default address configured");
        }
        sendBytes(defaultAddress, payload);
    }

    /**
     * Send a pre-serialized byte array to the specified address.
     *
     * @param address the address to send to
     * @param payload the bytes to send
     */
    public void sendBytes(String address, byte[] payload) {
        ZmqPush push = getPushSocket(address);
        push.send(payload);
    }

    @Override
    public void destroy() {
        // Close all push sockets
        for (ZmqPush push : pushSockets.values()) {
            push.close();
        }
        pushSockets.clear();
    }

    /**
     * Convert the payload to a message and send it.
     *
     * @param payload the object to send
     */
    public void convertAndSend(Object payload) {
        convertAndSend(defaultAddress, payload, null);
    }

    /**
     * Convert the payload to a message and send it to the specified address.
     *
     * @param address the address to send to
     * @param payload the object to send
     */
    public void convertAndSend(String address, Object payload) {
        convertAndSend(address, payload, null);
    }

    /**
     * Convert the payload to a message, post-process it, and send it.
     *
     * @param payload       the object to send
     * @param postProcessor the post-processor to apply
     */
    public void convertAndSend(Object payload, MessagePostProcessor postProcessor) {
        convertAndSend(defaultAddress, payload, postProcessor);
    }

    /**
     * Convert the payload to a message, post-process it, and send it to the specified address.
     *
     * @param address       the address to send to
     * @param payload       the object to send
     * @param postProcessor the post-processor to apply
     */
    public void convertAndSend(String address, Object payload, MessagePostProcessor postProcessor) {
        try {
            // Create a message with standard properties
            Map<String, Object> messageProperties = new HashMap<>();
            messageProperties.put(ZmqHeaders.MESSAGE_ID, UUID.randomUUID().toString());
            messageProperties.put(ZmqHeaders.TIMESTAMP, System.currentTimeMillis());

            Message message = messageConverter.toMessage(payload, messageProperties);

            // Apply before-send post-processor if configured
            if (beforeSendPostProcessor != null) {
                message = beforeSendPostProcessor.postProcessMessage(message);
            }

            // Apply method-specific post-processor if provided
            if (postProcessor != null) {
                message = postProcessor.postProcessMessage(message);
            }

            // Get the message body
            byte[] bytes = message.getBody();
            if (address != null) {
                sendBytes(address, bytes);
            } else {
                sendBytes(bytes);
            }
        } catch (Exception e) {
            throw new ZmqMessageConversionException("Failed to convert and send message", e);
        }
    }

    public void setContext(ZContext zContext) {
        Assert.notNull(zContext, "ZContext must not be null");
        this.context = zContext;
    }
}
