package com.aoneconsultancy.zeromq.core.converter;

import com.aoneconsultancy.zeromq.core.message.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * A more advanced message converter that can convert messages based on headers
 * and target class. This is similar to Spring Messaging's SmartMessageConverter.
 */
public interface SmartMessageConverter extends MessageConverter {

    /**
     * Convert a ZmqMessage to an object, considering message headers.
     *
     * @param message the message to convert
     * @param headers additional headers to consider during conversion
     * @return the converted object
     */
    Object fromMessage(Message message, MessageHeaders headers);
}
