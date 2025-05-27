package com.aoneconsultancy.zeromqpoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simplified helper similar to Spring's {@code RabbitTemplate} for sending
 * messages over ZeroMQ.
 */
public class ZmqTemplate {

    private final ZmqService zmqService;
    private final ObjectMapper mapper;

    public ZmqTemplate(ZmqService zmqService, ObjectMapper mapper) {
        this.zmqService = zmqService;
        this.mapper = mapper;
    }

    /**
     * Send a pre-serialized byte array.
     * @param payload the bytes to send
     */
    public void sendBytes(byte[] payload) {
        zmqService.sendBytes(payload);
    }

    /**
     * Convert the payload to JSON and send it.
     * @param payload the object to serialize
     */
    public void convertAndSend(Object payload) {
        try {
            zmqService.sendBytes(mapper.writeValueAsBytes(payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }
}
