package com.aoneconsultancy.zeromq.core.converter;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link ContentTypeMessageConverter}.
 */
class ContentTypeMessageConverterTest {

    private ContentTypeMessageConverter converter;
    private SimpleMessageConverter simpleConverter;
    private SerializerMessageConverter serializerConverter;

    @BeforeEach
    void setUp() {
        simpleConverter = new SimpleMessageConverter();
        serializerConverter = new SerializerMessageConverter();

        converter = new ContentTypeMessageConverter(simpleConverter);
        converter.addConverter(SimpleMessageConverter.CONTENT_TYPE_TEXT_PLAIN, simpleConverter);
        converter.addConverter(SerializerMessageConverter.CONTENT_TYPE_SERIALIZED_OBJECT, serializerConverter);
    }

    @Test
    void shouldDelegateToSimpleConverterForTextPlain() {
        // Given
        String payload = "Hello, World!";
        Map<String, Object> messageProperties = new HashMap<>();
        messageProperties.put(ZmqHeaders.CONTENT_TYPE, SimpleMessageConverter.CONTENT_TYPE_TEXT_PLAIN);

        // When
        Message message = converter.toMessage(payload, messageProperties);
        String result = (String) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertEquals(payload, result);
        assertEquals(SimpleMessageConverter.CONTENT_TYPE_TEXT_PLAIN,
                message.getMessageProperty(ZmqHeaders.CONTENT_TYPE));
    }

    @Test
    void shouldDelegateToSerializerConverterForSerializedObject() {
        // Given
        TestSerializable payload = new TestSerializable("test", 123);
        Map<String, Object> messageProperties = new HashMap<>();
        messageProperties.put(ZmqHeaders.CONTENT_TYPE, SerializerMessageConverter.CONTENT_TYPE_SERIALIZED_OBJECT);

        // When
        Message message = converter.toMessage(payload, messageProperties);
        TestSerializable result = (TestSerializable) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertEquals(payload.getName(), result.getName());
        assertEquals(payload.getValue(), result.getValue());
        assertEquals(SerializerMessageConverter.CONTENT_TYPE_SERIALIZED_OBJECT,
                message.getMessageProperty(ZmqHeaders.CONTENT_TYPE));
    }

    @Test
    void shouldUseDefaultConverterWhenNoContentTypeSpecified() {
        // Given
        String payload = "Hello, World!";

        // When
        Message message = converter.toMessage(payload, null);
        String result = (String) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertEquals(payload, result);
    }

    @Test
    void shouldUseDefaultConverterWhenUnknownContentType() {
        // Given
        String payload = "Hello, World!";
        Map<String, Object> messageProperties = new HashMap<>();
        messageProperties.put(ZmqHeaders.CONTENT_TYPE, "unknown/content-type");

        // When
        Message message = converter.toMessage(payload, messageProperties);
        String result = (String) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertEquals(payload, result);
    }

    @Test
    void shouldChangeDefaultConverter() {
        // Given
        converter.setDefaultConverter(serializerConverter);
        TestSerializable payload = new TestSerializable("test", 123);

        // When
        Message message = converter.toMessage(payload, null);
        TestSerializable result = (TestSerializable) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertEquals(payload.getName(), result.getName());
        assertEquals(payload.getValue(), result.getValue());
        assertEquals(SerializerMessageConverter.CONTENT_TYPE_SERIALIZED_OBJECT,
                message.getMessageProperty(ZmqHeaders.CONTENT_TYPE));
    }

    /**
     * Test serializable class for testing serialization.
     */
    static class TestSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;
        private final int value;

        public TestSerializable(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
