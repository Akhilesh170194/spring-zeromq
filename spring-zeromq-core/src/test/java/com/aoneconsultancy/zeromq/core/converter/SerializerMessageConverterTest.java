package com.aoneconsultancy.zeromq.core.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoneconsultancy.zeromq.core.message.Message;
import com.aoneconsultancy.zeromq.core.message.ZmqHeaders;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SerializerMessageConverter}.
 */
class SerializerMessageConverterTest {

    private SerializerMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new SerializerMessageConverter();
    }

    @Test
    void shouldConvertSerializableMessage() {
        // Given
        TestSerializable payload = new TestSerializable("test", 123);
        Map<String, Object> properties = new HashMap<>();
        properties.put(ZmqHeaders.CONTENT_TYPE, SerializerMessageConverter.CONTENT_TYPE_SERIALIZED_OBJECT);

        // When
        Message message = converter.toMessage(payload, properties);
        TestSerializable result = (TestSerializable) converter.fromMessage(message, TestSerializable.class);

        // Then
        assertNotNull(result);
        assertEquals(payload.getName(), result.getName());
        assertEquals(payload.getValue(), result.getValue());
        assertEquals(SerializerMessageConverter.CONTENT_TYPE_SERIALIZED_OBJECT,
                message.getMessageProperty(ZmqHeaders.CONTENT_TYPE));
    }

    @Test
    void shouldHandleNullPayload() {
        // Given
        Object payload = null;

        // When
        Message message = converter.toMessage(payload, null);
        Object result = converter.fromMessage(message, Object.class);

        // Then
        assertNotNull(message);
        assertNull(result);
    }

    @Test
    void shouldThrowExceptionForNonSerializableType() {
        // Given
        Object nonSerializable = new Object();

        // When/Then
        assertThrows(ZmqMessageConversionException.class, () -> converter.toMessage(nonSerializable, null));
    }

    @Test
    void shouldHandleComplexSerializableObjects() {
        // Given
        ComplexSerializable payload = new ComplexSerializable();
        payload.setName("test");
        payload.setValue(123);
        payload.setChild(new TestSerializable("child", 456));

        // When
        Message message = converter.toMessage(payload, null);
        ComplexSerializable result = (ComplexSerializable) converter.fromMessage(message, ComplexSerializable.class);

        // Then
        assertNotNull(result);
        assertEquals(payload.getName(), result.getName());
        assertEquals(payload.getValue(), result.getValue());
        assertNotNull(result.getChild());
        assertEquals(payload.getChild().getName(), result.getChild().getName());
        assertEquals(payload.getChild().getValue(), result.getChild().getValue());
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

    /**
     * Complex serializable class for testing nested serialization.
     */
    static class ComplexSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int value;
        private TestSerializable child;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public TestSerializable getChild() {
            return child;
        }

        public void setChild(TestSerializable child) {
            this.child = child;
        }
    }
}
