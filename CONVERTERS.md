# ZeroMQ Message Converters

This document provides an overview of the message converters available in the ZeroMQ Spring Boot integration.

## Available Converters

### Jackson2JsonZmqMessageConverter

This converter uses Jackson 2 for JSON conversion. It's the default converter used by `ZmqTemplate`.

**Features:**

- Converts objects to/from JSON using Jackson's ObjectMapper
- Preserves headers in the serialized message
- Handles different types of objects through Jackson's type system

**Usage Example:**

```java
// Create a converter with a default ObjectMapper
Jackson2JsonZmqMessageConverter converter = new Jackson2JsonZmqMessageConverter();

// Or with a custom ObjectMapper
ObjectMapper objectMapper = new ObjectMapper();
// Configure ObjectMapper as needed
Jackson2JsonZmqMessageConverter converter = new Jackson2JsonZmqMessageConverter(objectMapper);

// Use with ZmqTemplate
ZmqTemplate template = new ZmqTemplate(context, bufferSize, converter);
```

### SimpleStringZmqMessageConverter

A simple converter that handles String and byte[] payloads.

**Features:**

- Converts between String and byte[] payloads
- Uses a configurable charset for String conversion
- Adds appropriate content type and encoding headers

**Usage Example:**

```java
// Create a converter with the default UTF-8 charset
SimpleStringZmqMessageConverter converter = new SimpleStringZmqMessageConverter();

// Or with a custom charset
SimpleStringZmqMessageConverter converter = new SimpleStringZmqMessageConverter(StandardCharsets.ISO_8859_1);

// Use with ZmqTemplate
ZmqTemplate template = new ZmqTemplate(context, bufferSize, converter);
```

### SimpleZmqMessageConverter

A versatile converter that can handle Strings, Serializable objects, and byte arrays.

**Features:**

- Converts between different types of objects and byte arrays
- Uses content type to determine how to convert messages
- Similar to Spring AMQP's SimpleMessageConverter

**Usage Example:**

```java
// Create a converter with the default UTF-8 charset
SimpleZmqMessageConverter converter = new SimpleZmqMessageConverter();

// Configure the charset if needed
converter.

setDefaultCharset("ISO-8859-1");

// Use with ZmqTemplate
ZmqTemplate template = new ZmqTemplate(context, bufferSize, converter);
```

### SerializerZmqMessageConverter

A converter that uses Java serialization for all objects.

**Features:**

- Serializes and deserializes objects using Java's serialization mechanism
- Requires all objects to be Serializable
- Similar to Spring AMQP's SerializerMessageConverter

**Usage Example:**

```java
// Create a converter
SerializerZmqMessageConverter converter = new SerializerZmqMessageConverter();

// Use with ZmqTemplate
ZmqTemplate template = new ZmqTemplate(context, bufferSize, converter);
```

### ContentTypeZmqMessageConverter

A converter that delegates to different converters based on content type.

**Features:**

- Delegates to different converters based on the content type of the message
- Allows for flexible message conversion strategies
- Similar to Spring AMQP's ContentTypeDelegatingMessageConverter

**Usage Example:**

```java
// Create a converter with a default converter
SimpleZmqMessageConverter defaultConverter = new SimpleZmqMessageConverter();
ContentTypeZmqMessageConverter converter = new ContentTypeZmqMessageConverter(defaultConverter);

// Add converters for specific content types
converter.

addConverter("text/plain",new SimpleStringZmqMessageConverter());
        converter.

addConverter("application/json",new Jackson2JsonZmqMessageConverter());
        converter.

addConverter("application/x-java-serialized-object",new SerializerZmqMessageConverter());

// Use with ZmqTemplate
ZmqTemplate template = new ZmqTemplate(context, bufferSize, converter);
```

## Choosing a Converter

The choice of converter depends on your specific needs:

- **Jackson2JsonZmqMessageConverter**: Best for JSON-based messaging, especially when working with complex object graphs
  or when interoperability with other systems is important.

- **SimpleStringZmqMessageConverter**: Good for simple text-based messaging when you don't need to handle complex
  objects.

- **SimpleZmqMessageConverter**: A versatile option that can handle different types of payloads based on content type.
  Good for mixed messaging scenarios.

- **SerializerZmqMessageConverter**: Useful when you need to preserve the exact Java object state, but only works with
  Java-to-Java communication.

- **ContentTypeZmqMessageConverter**: Best when you need to handle different message formats in the same application and
  want to delegate to specialized converters based on content type.

## Configuration

You can configure the message converter used by `ZmqTemplate` in your Spring configuration:

```java

@Configuration
public class ZmqConfig {

    @Bean
    public ZmqMessageConverter zmqMessageConverter() {
        return new SimpleZmqMessageConverter();
    }

    @Bean
    public ZmqTemplate zmqTemplate(ZContext context, ZmqProperties properties, ZmqMessageConverter messageConverter) {
        return new ZmqTemplate(context, properties.getBufferSize(), messageConverter);
    }
}
```

Or you can set it programmatically:

```java
ZmqTemplate template = new ZmqTemplate(context);
template.

setMessageConverter(new SimpleZmqMessageConverter());
```