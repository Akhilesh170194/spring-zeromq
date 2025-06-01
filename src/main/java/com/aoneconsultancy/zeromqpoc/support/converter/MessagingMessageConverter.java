package com.aoneconsultancy.zeromqpoc.support.converter;

import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.core.converter.SimpleMessageConverter;
import com.aoneconsultancy.zeromqpoc.core.message.Message;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Convert a {@link org.springframework.messaging.Message} from the messaging abstraction to and from a
 * {@link Message} using an underlying
 * {@link MessageConverter} for the payload.
 *
 * @author Akhilesh Singh
 * @since 1.4
 */
public class MessagingMessageConverter implements MessageConverter, InitializingBean {

    private MessageConverter payloadConverter;

    /**
     * Create an instance with a default payload converter for an inbound
     * handler.
     *
     * @see SimpleMessageConverter
     */
    public MessagingMessageConverter() {
        this(new SimpleMessageConverter());
    }

    /**
     * Create an instance with the specified payload converter and
     * header mapper.
     *
     * @param payloadConverter the target {@link MessageConverter} for {@code payload}.
     */
    public MessagingMessageConverter(MessageConverter payloadConverter) {
        Assert.notNull(payloadConverter, "PayloadConverter must not be null");
        this.payloadConverter = payloadConverter;
    }


    /**
     * Set the {@link MessageConverter} to use to convert the payload.
     *
     * @param payloadConverter the target {@link MessageConverter} for {@code payload}.
     */
    public void setPayloadConverter(MessageConverter payloadConverter) {
        this.payloadConverter = payloadConverter;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.payloadConverter, "Property 'payloadConverter' is required");
    }

    @Override
    public Message toMessage(Object object, Map<String, Object> messageProperties)
            throws MessageConversionException {

        if (!(object instanceof org.springframework.messaging.Message<?> input)) {
            throw new IllegalArgumentException("Could not convert [" + object + "] - only [" +
                    org.springframework.messaging.Message.class.getName() + "] is handled by this converter");
        }
        return this.payloadConverter.toMessage(
                input.getPayload(), messageProperties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        if (message == null) {
            return null;
        }
        Map<String, Object> mappedHeaders = message.getMessageProperties();
        Object convertedObject = extractPayload(message);
        if (convertedObject == null) {
            throw new MessageConversionException("Message converter returned null");
        }
        MessageBuilder<Object> builder = (convertedObject instanceof org.springframework.messaging.Message) ?
                MessageBuilder.fromMessage((org.springframework.messaging.Message<Object>) convertedObject) : // NOSONAR
                MessageBuilder.withPayload(convertedObject);
        return builder.copyHeadersIfAbsent(mappedHeaders).build();
    }

    /**
     * Extract the payload of the specified {@link Message}.
     *
     * @param message the AMQP Message to extract {@code payload}.
     * @return the extracted {@code payload}.
     */
    protected Object extractPayload(Message message) {
        return this.payloadConverter.fromMessage(message);
    }

}
