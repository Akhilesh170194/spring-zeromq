package com.aoneconsultancy.zeromqpoc.core.converter;

import com.aoneconsultancy.zeromqpoc.core.message.Message;
import com.aoneconsultancy.zeromqpoc.core.message.ZmqHeaders;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link MessageConverter} that uses Java serialization.
 * Similar to Spring AMQP's SerializerMessageConverter, this converter
 * requires all objects to be {@link Serializable}.
 */
public class SerializerMessageConverter implements MessageConverter {

    public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";

    @Override
    public Message toMessage(Object object, Map<String, Object> messageProperties) {
        if (object == null) {
            return new Message(new byte[0], messageProperties);
        }

        if (!(object instanceof Serializable)) {
            throw new ZmqMessageConversionException(
                    "SerializerZmqMessageConverter requires a Serializable object but received: " +
                            object.getClass().getName());
        }

        try {
            byte[] serializedObject = serialize((Serializable) object);

            Map<String, Object> properties = new HashMap<>();
            if (messageProperties != null) {
                properties.putAll(messageProperties);
            }
            properties.put(ZmqHeaders.CONTENT_TYPE, CONTENT_TYPE_SERIALIZED_OBJECT);

            return new Message(serializedObject, properties);
        } catch (IOException e) {
            throw new ZmqMessageConversionException("Failed to serialize object", e);
        }
    }

    @Override
    public Object fromMessage(Message message) {
        byte[] body = message.getBody();

        if (body == null || body.length == 0) {
            return null;
        }

        try {
            return deserialize(body);
        } catch (Exception e) {
            throw new ZmqMessageConversionException("Failed to deserialize object", e);
        }
    }

    /**
     * Serialize an object to a byte array.
     *
     * @param object the object to serialize
     * @return the serialized object
     * @throws IOException if serialization fails
     */
    protected byte[] serialize(Serializable object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);
        oos.close();
        return bos.toByteArray();
    }

    /**
     * Deserialize a byte array to an object.
     *
     * @param bytes the bytes to deserialize
     * @return the deserialized object
     * @throws IOException            if deserialization fails
     * @throws ClassNotFoundException if the class of the serialized object cannot be found
     */
    protected Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object object = ois.readObject();
        ois.close();
        return object;
    }
}
