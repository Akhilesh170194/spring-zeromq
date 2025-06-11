package com.aoneconsultancy.zeromq.core;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.core.converter.SimpleMessageConverter;
import com.aoneconsultancy.zeromq.core.converter.ZmqMessageConversionException;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, ZmqPush> pushSockets = new ConcurrentHashMap<>();

    /**
     * High water mark for sockets
     */
    @Getter
    private final int socketHwm;

    /**
     * Send buffer size for sockets
     */
    @Getter
    private final int socketSendBuffer;

    /**
     * Socket type for producer
     */
    @Getter
    @Setter
    private SocketType socketType;

    /**
     * Whether to bind or connect sockets
     */
    @Getter
    private final boolean bind;

    /**
     * Send timeout in milliseconds
     */
    @Getter
    @Setter
    private long sendTimeout = 2000;

    /**
     * Number of times to retry poll/send
     */
    @Getter
    @Setter
    private int pollRetry = 3;

    /**
     * Delay between retries in milliseconds
     */
    @Getter
    @Setter
    private long retryDelay = 100;

    /**
     * Whether to enable backpressure
     */
    @Getter
    @Setter
    private boolean backpressureEnabled = true;

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
     * Default address to send messages to.
     */
    @Getter
    @Setter
    private String defaultId;

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
        this(context, bufferSize, 1024, SocketType.PUSH, true);
    }

    /**
     * Create a new template with the given context and configuration.
     *
     * @param context          the ZeroMQ context
     * @param socketHwm        the high water mark for sockets
     * @param socketSendBuffer the send buffer size for sockets
     * @param socketType       the socket type for producer
     * @param bind             whether to bind or connect sockets
     */
    public ZmqTemplate(ZContext context, int socketHwm, int socketSendBuffer,
                       SocketType socketType, boolean bind) {
        this.context = context;
        this.socketHwm = socketHwm;
        this.socketSendBuffer = socketSendBuffer;
        this.socketType = socketType;
        this.bind = bind;
    }

    /**
     * Set endpoints and create configured sockets.
     *
     * @param endpoints the producer socket endpoints
     */
    public void setEndpoints(List<String> endpoints) {
        for (String endpoint : endpoints) {
            getPushSocket(endpoint);
        }
    }

    /**
     * Get or create a push socket for the specified id.
     *
     * @param id the address to bind to
     * @return the push socket
     */
    private ZmqPush getPushSocket(String id) {
        return pushSockets.computeIfAbsent(id, addr ->
                new ZmqPush(context, socketType, bind, List.of(addr), socketHwm, socketSendBuffer));
    }

    /**
     * Send a pre-serialized byte array.
     *
     * @param payload the bytes to send
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean sendBytes(byte[] payload) {
        if (defaultId == null) {
            throw new IllegalStateException("No default Id configured");
        }
        return sendBytes(defaultId, payload);
    }

    /**
     * Send a pre-serialized byte array to the specified name.
     *
     * @param name    the name to send to
     * @param payload the bytes to send
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean sendBytes(String name, byte[] payload) {
        ZmqPush push = getPushSocket(name);

        // Apply send timeout, retry logic, and backpressure
        long startTime = System.currentTimeMillis();
        boolean sent = false;
        int retries = 0;

        while (!sent && retries < pollRetry && (System.currentTimeMillis() - startTime < sendTimeout)) {
            // If backpressure is enabled, use blocking send (0 flags)
            // Otherwise use non-blocking send (ZMQ.DONTWAIT)
            int flags = backpressureEnabled ? 0 : ZMQ.DONTWAIT;

            sent = push.send(payload, flags);

            if (!sent) {
                retries++;
                if (retries < pollRetry) {
                    try {
                        log.debug("Send failed, retrying in {} ms (attempt {}/{})", retryDelay, retries, pollRetry);
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while waiting to retry Zmq send", e);
                        break;
                    }
                }
            }
        }

        if (!sent) {
            log.warn("Failed to send message to {} after {} retries within {} ms",
                    name, retries, System.currentTimeMillis() - startTime);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Sent message to {} after {} retries within {} ms",
                        name, retries, System.currentTimeMillis() - startTime);
            }
        }

        return sent;
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
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean convertAndSend(Object payload) {
        return convertAndSend(defaultId, payload, null);
    }

    /**
     * Convert the payload to a message and send it to the specified name.
     *
     * @param name    the name to send to
     * @param payload the object to send
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean convertAndSend(String name, Object payload) {
        return convertAndSend(name, payload, null);
    }

    /**
     * Convert the payload to a message, post-process it, and send it.
     *
     * @param payload       the object to send
     * @param postProcessor the post-processor to apply
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean convertAndSend(Object payload, MessagePostProcessor postProcessor) {
        return convertAndSend(defaultId, payload, postProcessor);
    }

    /**
     * Convert the payload to a message, post-process it, and send it to the specified name.
     *
     * @param name          the name to send to
     * @param payload       the object to send
     * @param postProcessor the post-processor to apply
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean convertAndSend(String name, Object payload, MessagePostProcessor postProcessor) {
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
            boolean sent;
            if (name != null) {
                sent = sendBytes(name, bytes);
            } else {
                sent = sendBytes(bytes);
            }

            if (!sent) {
                log.warn("Failed to send message with ID: {}", messageProperties.get(ZmqHeaders.MESSAGE_ID));
            }

            return sent;
        } catch (Exception e) {
            throw new ZmqMessageConversionException("Failed to convert and send message", e);
        }
    }

    public void setContext(ZContext zContext) {
        Assert.notNull(zContext, "ZContext must not be null");
        this.context = zContext;
    }
}
