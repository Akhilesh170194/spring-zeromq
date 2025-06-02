package com.aoneconsultancy.zeromq.service;

import com.aoneconsultancy.zeromq.annotation.ZmqListener;
import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.model.payload.DemoPayload;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple component that receives {@link DemoPayload} messages using
 * {@link ZmqListener} and prints them.
 */
@Component
@Slf4j
public class DemoListener {

    private final BlockingQueue<DemoPayload> receivedMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> receivedRawMessages = new LinkedBlockingQueue<>();

    @ZmqListener
    public void handle(DemoPayload payload) {
        log.info("@ZmqListener received payload: {}", payload);
        receivedMessages.offer(payload);
    }

    /**
     * Demonstrates receiving the raw ZmqMessage without conversion.
     * This is similar to how Spring AMQP allows receiving the raw Message.
     */
    public void handleRawMessage(Message message) {
        log.info("@ZmqListener received raw message with {} bytes", message.getBody().length);
        receivedRawMessages.offer(message);
    }

    /**
     * Poll for a received payload message.
     *
     * @param timeout the timeout to wait
     * @param unit    the time unit for the timeout
     * @return the received payload, or null if the timeout expires
     * @throws InterruptedException if interrupted while waiting
     */
    public DemoPayload poll(long timeout, TimeUnit unit) throws InterruptedException {
        return receivedMessages.poll(timeout, unit);
    }

    /**
     * Poll for a received raw message.
     *
     * @param timeout the timeout to wait
     * @param unit    the time unit for the timeout
     * @return the received raw message, or null if the timeout expires
     * @throws InterruptedException if interrupted while waiting
     */
    public Message pollRawMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return receivedRawMessages.poll(timeout, unit);
    }

}
