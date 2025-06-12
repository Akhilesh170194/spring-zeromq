package com.aoneconsultancy.zeromq.core.converter;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of {@link MessageConverter} that converts String payloads.
 * Similar to Spring AMQP's SimpleMessageConverter, but only for String payloads.
 */
public class SimpleStringMessageConverter implements MessageConverter {

    private final Charset defaultCharset;

    /**
     * Create a new converter with UTF-8 as the default charset.
     */
    public SimpleStringMessageConverter() {
        this(StandardCharsets.UTF_8);
    }

    /**
     * Create a new converter with the given default charset.
     *
     * @param defaultCharset the default charset to use
     */
    public SimpleStringMessageConverter(Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
    }

    @Override
    public Message toMessage(Object object, Map<String, Object> messageProperties) {
        if (object == null) {
            return new Message(new byte[0], messageProperties);
        }

        byte[] body;
        Map<String, Object> properties = new HashMap<>();
        if (messageProperties != null) {
            properties.putAll(messageProperties);
        }

        if (object instanceof byte[]) {
            body = (byte[]) object;
            properties.put(ZmqHeaders.CONTENT_TYPE, "application/octet-stream");
        } else if (object instanceof String) {
            body = ((String) object).getBytes(defaultCharset);
            properties.put(ZmqHeaders.CONTENT_TYPE, "text/plain");
            properties.put(ZmqHeaders.CONTENT_ENCODING, defaultCharset.name());
        } else {
            throw new ZmqMessageConversionException(
                    "SimpleStringZmqMessageConverter only supports String and byte[] objects, not " +
                            object.getClass().getName());
        }

        return new Message(body, properties);
    }

    @Override
    public Object fromMessage(Message message) {
        byte[] body = message.getBody();

        if (body == null || body.length == 0) {
            return null;
        }

        // Check the content type to determine how to convert the message
        String contentType = (String) message.getMessageProperty(ZmqHeaders.CONTENT_TYPE);

        // If it's a text message, convert to String
        if ("text/plain".equals(contentType)) {
            String encoding = (String) message.getMessageProperty(ZmqHeaders.CONTENT_ENCODING);
            Charset charset = encoding != null ? Charset.forName(encoding) : defaultCharset;
            return new String(body, charset);
        }

        // Otherwise, return the raw bytes
        return body;
    }

    /**
     * Get the default charset used by this converter.
     *
     * @return the default charset
     */
    public Charset getDefaultCharset() {
        return defaultCharset;
    }
}
