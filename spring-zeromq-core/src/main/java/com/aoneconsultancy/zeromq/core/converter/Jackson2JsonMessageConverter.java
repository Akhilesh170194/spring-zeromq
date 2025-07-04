package com.aoneconsultancy.zeromq.core.converter;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link MessageConverter} that uses Jackson 2 for JSON conversion.
 * Similar to Spring AMQP's Jackson2JsonMessageConverter.
 */
@Getter
public class Jackson2JsonMessageConverter implements MessageConverter {

    /**
     * Get the ObjectMapper used by this converter.
     */
    private final ObjectMapper objectMapper;
    private static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();

    /**
     * Create a new converter with a default ObjectMapper.
     */
    public Jackson2JsonMessageConverter() {
        this(new ObjectMapper());
    }

    /**
     * Create a new converter with the given ObjectMapper.
     *
     * @param objectMapper the ObjectMapper to use
     */
    public Jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message toMessage(Object object, Map<String, Object> messageProperties) {
        try {
            // Convert the object to JSON
            byte[] jsonBytes = objectMapper.writeValueAsBytes(object);

            // Create a new message with the JSON bytes and properties
            Map<String, Object> properties = new HashMap<>();
            if (messageProperties != null) {
                properties.putAll(messageProperties);
            }

            // Add content type and encoding if not present
            if (!properties.containsKey(ZmqHeaders.CONTENT_TYPE)) {
                properties.put(ZmqHeaders.CONTENT_TYPE, "application/json");
            }
            if (!properties.containsKey(ZmqHeaders.CONTENT_ENCODING)) {
                properties.put(ZmqHeaders.CONTENT_ENCODING, DEFAULT_CHARSET);
            }

            return new Message(jsonBytes, properties);
        } catch (JsonProcessingException e) {
            throw new ZmqMessageConversionException("Failed to convert object to JSON", e);
        }
    }

    @Override
    public Object fromMessage(Message message) {
        try {
            // Get the message body
            byte[] body = message.getBody();

            // By default, convert JSON to a Map
            return objectMapper.readValue(body, Object.class);
        } catch (IOException e) {
            throw new ZmqMessageConversionException("Failed to convert JSON to object", e);
        }
    }
}
