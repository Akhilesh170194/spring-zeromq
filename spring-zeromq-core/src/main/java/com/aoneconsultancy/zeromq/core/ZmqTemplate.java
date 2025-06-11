package com.aoneconsultancy.zeromq.core;

import com.aoneconsultancy.zeromq.config.ZmqProducer;
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
import org.springframework.util.CollectionUtils;
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

    private static final String DEFAULT_ID = "workerZmqProducer";
    private static final String DEFAULT_ZMQ_PUSH_ID = "p-workerZmqProducer-0";
    private ZContext context;

    private final Map<String, ZmqPush> pushSockets = new ConcurrentHashMap<>();

    ZmqProducer zmqProducer;

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

    @Setter
    private int linger;

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
    private String defaultEndpointName;

    /**
     * Create a new template with the given context, default buffer size, and a default JSON message converter.
     *
     * @param context the ZeroMQ context
     */
    public ZmqTemplate(ZContext context, ZmqProducer producer) {
        this(context, producer,1000);
    }

    /**
     * Create a new template with the given context, buffer size, and message converter.
     *
     * @param context    the ZeroMQ context
     * @param producer  the producer to connect/bind to
     * @param sendHwm   the high water mark for sockets
     */
    public ZmqTemplate(ZContext context, ZmqProducer producer, int sendHwm) {
        this(context, producer, sendHwm, 1024, 0);
    }

    /**
     * Create a new template with the given context and configuration.
     *
     * @param context          the ZeroMQ context
     * @param producer  the producer to connect/bind to
     * @param socketHwm        the high water mark for sockets
     * @param socketSendBuffer the send buffer size for sockets
     */
    public ZmqTemplate(ZContext context, ZmqProducer producer, int socketHwm, int socketSendBuffer,
                       int linger) {
        this.context = context;
        this.socketHwm = socketHwm;
        this.socketSendBuffer = socketSendBuffer;
        this.linger = linger;
        this.zmqProducer = producer;
        addEndpoints(producer);
    }

    /**
     * Set endpoints and create configured sockets.
     *
     * @param producer the producer details
     */
    private void addEndpoints(ZmqProducer producer) {

        if(CollectionUtils.isEmpty(producer.getAddresses())) {
            throw new IllegalArgumentException("Endpoints must not be empty");
        }
        ZmqPush pushSocket = new ZmqPush(context, producer.getType(), producer.isBind(), producer.getAddresses(), socketHwm, socketSendBuffer, linger);
        pushSockets.put(producer.getName(), pushSocket);
    }

    /**
     * Get or create a push socket for the specified name.
     *
     * @param name the address to bind to
     * @return the push socket
     */
    private ZmqPush getPushSocket(String name) {
        ZmqPush pushSocket = pushSockets.get(name);
        if(pushSocket == null) {
            throw new IllegalStateException("No push socket configured for " + name);
        }
        return pushSocket;
    }

    /**
     * Send a pre-serialized byte array.
     *
     * @param payload the bytes to send
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean sendBytes(byte[] payload) {
        if (defaultEndpointName == null) {
            throw new IllegalStateException("No default Id configured");
        }
        return sendBytes(defaultEndpointName, payload);
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

    public void close(String id) {
        ZmqPush push = pushSockets.remove(id);
        if (push != null) {
            push.close();
        }
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
        return convertAndSend(defaultEndpointName, payload, null);
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
        return convertAndSend(defaultEndpointName, payload, postProcessor);
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

    private String getProducerId(String id, int i) {
        if(id == null) {
            return DEFAULT_ZMQ_PUSH_ID;
        }

        return "p" + id + "-" + i;
    }
}
