package com.aoneconsultancy.zeromq.core.converter;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import lombok.Getter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link MessageConverter} that can work with Strings, Serializable
 * instances, or byte arrays. Similar to Spring AMQP's SimpleMessageConverter.
 */
@Getter
public class SimpleMessageConverter implements MessageConverter {

    public static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();
    public static final String CONTENT_TYPE_BYTES = "application/octet-stream";
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";

    /**
     * Get the default charset used by this converter.
     */
    private String defaultCharset = DEFAULT_CHARSET;

    /**
     * Specify the default charset to use when converting to or from text-based
     * Message body content. If not specified, the charset will be "UTF-8".
     *
     * @param defaultCharset The default charset.
     */
    public void setDefaultCharset(String defaultCharset) {
        this.defaultCharset = (defaultCharset != null) ? defaultCharset : DEFAULT_CHARSET;
    }

    @Override
    public Message toMessage(Object object, Map<String, Object> messageProperties) {
        byte[] body;
        Map<String, Object> properties = new HashMap<>();
        if (messageProperties != null) {
            properties.putAll(messageProperties);
        }

        if (object == null) {
            body = new byte[0];
            properties.put(ZmqHeaders.CONTENT_TYPE, CONTENT_TYPE_BYTES);
        } else if (object instanceof byte[]) {
            body = (byte[]) object;
            properties.put(ZmqHeaders.CONTENT_TYPE, CONTENT_TYPE_BYTES);
        } else if (object instanceof String) {
            try {
                body = ((String) object).getBytes(this.defaultCharset);
                properties.put(ZmqHeaders.CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN);
                properties.put(ZmqHeaders.CONTENT_ENCODING, this.defaultCharset);
            } catch (Exception e) {
                throw new ZmqMessageConversionException("Failed to convert String to bytes", e);
            }
        } else if (object instanceof Serializable) {
            try {
                body = serialize((Serializable) object);
                properties.put(ZmqHeaders.CONTENT_TYPE, CONTENT_TYPE_SERIALIZED_OBJECT);
            } catch (Exception e) {
                throw new ZmqMessageConversionException("Failed to serialize object", e);
            }
        } else {
            throw new ZmqMessageConversionException(
                    "SimpleZmqMessageConverter only supports String, byte[] and Serializable objects, not " +
                            object.getClass().getName());
        }

        return new Message(body, properties);
    }

    @Override
    public Object fromMessage(Message message) {
        byte[] body = message.getBody();
        Map<String, Object> properties = message.getMessageProperties();

        if (body == null || body.length == 0) {
            return null;
        }

        String contentType = (String) properties.getOrDefault(ZmqHeaders.CONTENT_TYPE, CONTENT_TYPE_BYTES);

        // Default to returning the byte array
        if (CONTENT_TYPE_BYTES.equals(contentType)) {
            return body;
        }
        // If it's a text message, convert to String
        else if (CONTENT_TYPE_TEXT_PLAIN.equals(contentType)) {
            String encoding = (String) properties.getOrDefault(ZmqHeaders.CONTENT_ENCODING, this.defaultCharset);
            try {
                return new String(body, encoding);
            } catch (Exception e) {
                throw new ZmqMessageConversionException("Failed to convert bytes to String", e);
            }
        }
        // If it's a serialized object, deserialize it
        else if (CONTENT_TYPE_SERIALIZED_OBJECT.equals(contentType)) {
            try {
                return deserialize(body);
            } catch (Exception e) {
                throw new ZmqMessageConversionException("Failed to deserialize object", e);
            }
        }

        // If we get here, we couldn't determine the content type, so return the raw bytes
        return body;
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
