package com.aoneconsultancy.zeromqpoc.core.converter;

import com.aoneconsultancy.zeromqpoc.core.message.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * A more advanced message converter that can convert messages based on headers
 * and target class. This is similar to Spring Messaging's SmartMessageConverter.
 */
public interface SmartMessageConverter extends MessageConverter {

    /**
     * Convert a ZmqMessage to an object of the specified type, considering message headers.
     *
     * @param <T>         the expected type of the object
     * @param message     the message to convert
     * @param targetClass the expected class of the object
     * @param headers     additional headers to consider during conversion
     * @return the converted object
     */
    <T> T fromMessage(Message message, Class<T> targetClass, MessageHeaders headers);
}