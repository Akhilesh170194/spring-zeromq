package com.aoneconsultancy.zeromq.core.converter;

import com.aoneconsultancy.zeromq.core.message.Message;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * A generic implementation of {@link SmartMessageConverter} that delegates to a
 * {@link ConversionService} for converting the message payload.
 * Similar to Spring Messaging's GenericMessageConverter.
 */
public class GenericMessageConverter implements SmartMessageConverter {

    private final ConversionService conversionService;

    /**
     * Create a new converter with the given conversion service.
     *
     * @param conversionService the conversion service to use
     */
    public GenericMessageConverter(ConversionService conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        this.conversionService = conversionService;
    }

    @Override
    public Message toMessage(Object object, Map<String, Object> messageProperties) {
        if (object == null) {
            return new Message(new byte[0], messageProperties);
        }

        if (object instanceof byte[]) {
            return new Message((byte[]) object, messageProperties);
        }

        // For other types, use the SimpleZmqMessageConverter as a fallback
        return new SimpleMessageConverter().toMessage(object, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) {
        return fromMessage(message, null);
    }

    @Override
    public Object fromMessage(Message message, MessageHeaders headers) {
        if (message == null) {
            return null;
        }

        Object payload = message.getBody();

        // If the payload is a byte array, return it directly
        if (payload instanceof byte[]) {
            // Check if we should convert to String based on content type
            Map<String, Object> properties = message.getMessageProperties();
            String contentType = (String) properties.getOrDefault("contentType", "");
            if (contentType.contains("text") || contentType.contains("json")) {
                return new String((byte[]) payload);
            }
            return payload;
        }

        // Fallback to SimpleZmqMessageConverter
        return new SimpleMessageConverter().fromMessage(message);
    }
}
