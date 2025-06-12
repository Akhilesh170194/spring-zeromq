package com.aoneconsultancy.zeromq.core.converter;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SimpleMessageConverter}.
 */
class SimpleMessageConverterTest {

    private SimpleMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new SimpleMessageConverter();
    }

    @Test
    void shouldConvertStringMessage() {
        // Given
        String payload = "Hello, World!";

        // When
        Message message = converter.toMessage(payload, null);
        String result = (String) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertEquals(payload, result);
        assertEquals(SimpleMessageConverter.CONTENT_TYPE_TEXT_PLAIN,
                message.getMessageProperty(ZmqHeaders.CONTENT_TYPE));
        assertEquals(StandardCharsets.UTF_8.name(),
                message.getMessageProperty(ZmqHeaders.CONTENT_ENCODING));
    }

    @Test
    void shouldConvertByteArrayMessage() {
        // Given
        byte[] payload = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        // When
        Message message = converter.toMessage(payload, null);
        byte[] result = (byte[]) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertArrayEquals(payload, result);
        assertEquals(SimpleMessageConverter.CONTENT_TYPE_BYTES,
                message.getMessageProperty(ZmqHeaders.CONTENT_TYPE));
    }

    @Test
    void shouldConvertSerializableMessage() {
        // Given
        TestSerializable payload = new TestSerializable("test", 123);

        // When
        Message message = converter.toMessage(payload, null);
        TestSerializable result = (TestSerializable) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertEquals(payload.getName(), result.getName());
        assertEquals(payload.getValue(), result.getValue());
        assertEquals(SimpleMessageConverter.CONTENT_TYPE_SERIALIZED_OBJECT,
                message.getMessageProperty(ZmqHeaders.CONTENT_TYPE));
    }

    @Test
    void shouldHandleNullPayload() {
        // Given
        Object payload = null;

        // When
        Message message = converter.toMessage(payload, null);
        Object result = converter.fromMessage(message);

        // Then
        assertNotNull(message);
        assertNull(result);
    }

    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // Given
        Object payload = new Object();

        // When/Then
        assertThrows(ZmqMessageConversionException.class, () -> converter.toMessage(payload, null));
    }

    @Test
    void shouldUseCustomCharset() {
        // Given
        String charset = "ISO-8859-1";
        converter.setDefaultCharset(charset);
        String payload = "Hello, World!";

        // When
        Message message = converter.toMessage(payload, null);
        String result = (String) converter.fromMessage(message);

        // Then
        assertNotNull(result);
        assertEquals(payload, result);
        assertEquals(charset, message.getMessageProperty(ZmqHeaders.CONTENT_ENCODING));
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
