package com.aoneconsultancy.zeromqpoc.core.converter;

import com.aoneconsultancy.zeromqpoc.core.message.Message;
import com.aoneconsultancy.zeromqpoc.support.converter.MessageConversionException;
import java.lang.reflect.Type;
import org.springframework.lang.Nullable;

/**
 * Strategy interface for converting between ZeroMQ byte arrays and {@link Message} objects.
 * Similar to Spring AMQP's MessageConverter, this allows pluggable message conversion strategies.
 */
public interface MessageConverter {

    /**
     * Convert an object to a ZmqMessage for sending over ZeroMQ.
     *
     * @param object            the object to convert
     * @param messageProperties additional message properties to include
     * @return the converted message
     */
    Message toMessage(Object object, java.util.Map<String, Object> messageProperties) throws MessageConversionException;

    /**
     * Convert a Java object to a Message.
     * The default implementation calls {@link #toMessage(Object, java.util.Map)}.
     *
     * @param object            the object to convert
     * @param messageProperties The message properties.
     * @param genericType       the type to use to populate type headers.
     * @return the Message
     * @throws MessageConversionException in case of conversion failure
     * @since 2.1
     */
    default Message toMessage(Object object, java.util.Map<String, Object> messageProperties, @Nullable Type genericType)
            throws MessageConversionException {
        return toMessage(object, messageProperties);
    }

    /**
     * Convert a ZmqMessage received from ZeroMQ to an object.
     *
     * @param message the message to convert
     * @return the converted object
     */
    Object fromMessage(Message message);
}
