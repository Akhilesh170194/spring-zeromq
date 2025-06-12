package com.aoneconsultancy.zeromq.core.converter;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link MessageConverter} that delegates to other converters
 * based on the content type of the message. Similar to Spring AMQP's
 * ContentTypeDelegatingMessageConverter.
 */
public class ContentTypeMessageConverter implements MessageConverter {

    private final Map<String, MessageConverter> converters = new HashMap<>();

    /**
     * Set the default converter to use when no specific converter is found.
     */
    @Setter
    private MessageConverter defaultConverter;

    /**
     * Create a new converter with a SimpleZmqMessageConverter as the default.
     */
    public ContentTypeMessageConverter() {
        this(new SimpleMessageConverter());
    }

    /**
     * Create a new converter with the given default converter.
     *
     * @param defaultConverter the default converter to use when no specific converter is found
     */
    public ContentTypeMessageConverter(MessageConverter defaultConverter) {
        this.defaultConverter = defaultConverter;
    }

    /**
     * Add a converter for a specific content type.
     *
     * @param contentType the content type to associate with the converter
     * @param converter   the converter to use for the content type
     */
    public void addConverter(String contentType, MessageConverter converter) {
        converters.put(contentType, converter);
    }

    @Override
    public Message toMessage(Object object, Map<String, Object> messageProperties) {
        // Use the default converter to create a message
        Message message = defaultConverter.toMessage(object, messageProperties);

        // Get the content type from the message properties
        String contentType = (String) message.getMessageProperty(ZmqHeaders.CONTENT_TYPE);

        // If there's a specific converter for this content type, use it
        MessageConverter converter = getConverter(contentType);
        if (converter != defaultConverter) {
            message = converter.toMessage(object, messageProperties);
        }

        return message;
    }

    @Override
    public Object fromMessage(Message message) {
        // Get the content type from the message properties
        String contentType = (String) message.getMessageProperty(ZmqHeaders.CONTENT_TYPE);

        // Get the appropriate converter for the content type
        MessageConverter converter = getConverter(contentType);

        // Convert the message using the appropriate converter
        return converter.fromMessage(message);
    }

    /**
     * Get the converter for the given content type.
     *
     * @param contentType the content type
     * @return the converter for the content type, or the default converter if none is found
     */
    protected MessageConverter getConverter(String contentType) {
        if (contentType != null && converters.containsKey(contentType)) {
            return converters.get(contentType);
        }
        return defaultConverter;
    }
}
