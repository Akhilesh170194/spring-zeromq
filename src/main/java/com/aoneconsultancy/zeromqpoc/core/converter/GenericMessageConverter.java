package com.aoneconsultancy.zeromqpoc.core.converter;

import com.aoneconsultancy.zeromqpoc.core.message.Message;
import java.util.Map;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

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
    public <T> T fromMessage(Message message, Class<T> targetClass) {
        return fromMessage(message, targetClass, null);
    }

    @Override
    public <T> T fromMessage(Message message, Class<T> targetClass, MessageHeaders headers) {
        if (message == null) {
            return null;
        }

        Object payload = message.getBody();

        // If the target class is byte[], return the raw bytes
        if (targetClass == byte[].class) {
            return targetClass.cast(payload);
        }

        // If the payload is already of the target type, return it directly
        if (targetClass.isInstance(payload)) {
            return targetClass.cast(payload);
        }

        // Try to convert the payload using the conversion service
        if (this.conversionService.canConvert(payload.getClass(), targetClass)) {
            return this.conversionService.convert(payload, targetClass);
        }

        // If we can't convert directly, try to convert from byte[] to String first
        if (payload instanceof byte[] && this.conversionService.canConvert(String.class, targetClass)) {
            String payloadAsString = new String((byte[]) payload);
            return this.conversionService.convert(payloadAsString, targetClass);
        }

        // Fallback to SimpleZmqMessageConverter
        return new SimpleMessageConverter().fromMessage(message, targetClass);
    }
}