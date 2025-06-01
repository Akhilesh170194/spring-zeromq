package com.aoneconsultancy.zeromqpoc.core.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import zmq.io.net.Address;

/**
 * Represents a ZeroMQ message, consisting of a payload and headers.
 * Similar to Spring's Message interface, this provides a consistent
 * message representation across the ZeroMQ integration.
 */
@ToString
@EqualsAndHashCode
public class Message {

    /**
     * Get the message payload.
     */
    @Getter
    private final byte[] body;
    private final Map<String, Object> messageProperties;

    /**
     * Create a new message with the given body and no message properties.
     *
     * @param body the message body
     */
    public Message(byte[] body) {
        this(body, new HashMap<>());
    }

    /**
     * Create a new message with the given body and message properties.
     *
     * @param body              the message body
     * @param messageProperties the message properties
     */
    public Message(byte[] body, Map<String, Object> messageProperties) {
        this.body = body != null ? body : new byte[0];
        this.messageProperties = messageProperties != null ? new HashMap<>(messageProperties) : new HashMap<>();
    }

    /**
     * Get the message properties.
     *
     * @return an unmodifiable view of the message properties
     */
    public Map<String, Object> getMessageProperties() {
        return Collections.unmodifiableMap(messageProperties);
    }

    /**
     * Get a message property value.
     *
     * @param key the property key
     * @return the property value, or null if not present
     */
    public Object getMessageProperty(String key) {
        return messageProperties.get(key);
    }

    /**
     * Create a builder for constructing messages.
     *
     * @param body the message body
     * @return a new builder
     */
    public static Builder builder(byte[] body) {
        return new Builder(body);
    }

    /**
     * Builder for constructing {@link Message} instances.
     */
    public static class Builder {
        private final byte[] body;
        private final Map<String, Object> messageProperties = new HashMap<>();

        private Builder(byte[] body) {
            this.body = body;
        }

        /**
         * Add a message property.
         *
         * @param key   the property key
         * @param value the property value
         * @return this builder
         */
        public Builder setMessageProperty(String key, Object value) {
            messageProperties.put(key, value);
            return this;
        }

        /**
         * Add all message properties from the given map.
         *
         * @param messageProperties the message properties to add
         * @return this builder
         */
        public Builder setMessageProperties(Map<String, Object> messageProperties) {
            if (messageProperties != null) {
                this.messageProperties.putAll(messageProperties);
            }
            return this;
        }

        /**
         * Build the message.
         *
         * @return a new message
         */
        public Message build() {
            return new Message(body, messageProperties);
        }
    }

    public static class MessageProperties {
        Address remoteAddress;
    }
}
