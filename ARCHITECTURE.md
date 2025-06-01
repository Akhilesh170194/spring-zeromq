# ZeroMQ Spring Boot Integration Architecture

This document describes the architecture of the ZeroMQ integration for Spring Boot, which is designed to behave
similarly to how RabbitMQ integrates with Spring Boot.

## Core Components

### ZmqPull

`ZmqPull` is a runnable class that handles pulling messages from a ZeroMQ socket. Key features:

- Encapsulates ZeroMQ socket operations for receiving messages
- Supports batch processing with configurable batch size and timeout
- Maintains a list of listeners that get notified when messages arrive
- Provides methods to register/unregister listeners and to poll for received messages
- Has proper lifecycle management (start, stop, close)

### ZmqPush

`ZmqPush` is a class that handles pushing messages to a ZeroMQ socket. Key features:

- Encapsulates ZeroMQ socket operations for sending messages
- Provides methods to send byte arrays with or without flags
- Has proper resource management (close method)
- Follows the Single Responsibility Principle by focusing only on message sending
- Enables better testability through mocking and dependency injection
- Allows for multiple socket configurations to different addresses

### ZmqTemplate

`ZmqTemplate` is a helper class similar to Spring's `RabbitTemplate` for sending messages over ZeroMQ. Key features:

- Uses `ZmqPush` for sending messages
- Supports converting and sending objects using a message converter
- Supports applying post-processors before sending
- Adds standard headers like MESSAGE_ID and TIMESTAMP

### Message Converters

The integration provides several message converters for different use cases:

#### Jackson2JsonZmqMessageConverter

- Converts objects to/from JSON using Jackson
- Preserves headers in the serialized message
- Handles different types of objects through Jackson's type system

#### SimpleStringZmqMessageConverter

- Simple converter that handles String and byte[] payloads
- Uses a configurable charset for String conversion
- Adds appropriate content type and encoding headers

#### SimpleZmqMessageConverter

- Handles Strings, Serializable objects, and byte arrays
- Uses content type to determine how to convert messages
- Similar to Spring AMQP's SimpleMessageConverter

#### SerializerZmqMessageConverter

- Uses Java serialization for all objects
- Requires all objects to be Serializable
- Similar to Spring AMQP's SerializerMessageConverter

#### ContentTypeZmqMessageConverter

- Delegates to different converters based on content type
- Allows for flexible message conversion strategies
- Similar to Spring AMQP's ContentTypeDelegatingMessageConverter

### SimpleZmqListenerContainer

`SimpleZmqListenerContainer` is the default implementation of `ZmqListenerContainer` that uses `ZmqPull`. Key features:

- Creates and manages a `ZmqPull` instance
- Supports configuring message conversion, post-processing, concurrency, etc.
- Properly utilizes all configured properties
- Supports batch processing and timeout

### ZmqListener

`ZmqListener` is an annotation for methods that should receive messages from ZeroMQ. Key features:

- Similar to Spring AMQP's `@RabbitListener`
- Supports configuring socket address, socket type, concurrency, etc.
- Supports flexible parameter binding:
    - Direct injection of a decoded object
    - Fallback to a raw ZeromqMessage
    - Support for multiple parameters

## Message Flow

### Sending Messages

1. Application code calls `ZmqTemplate.convertAndSend(payload)`
2. `ZmqTemplate` creates a `ZmqMessage` with the payload and standard headers
3. `ZmqTemplate` applies any configured post-processors
4. `ZmqTemplate` uses the configured `ZmqMessageConverter` to convert the message to a byte array
5. `ZmqTemplate` gets or creates a `ZmqPush` instance for the target address
6. `ZmqPush` sends the byte array to the ZeroMQ socket

### Receiving Messages

1. `ZmqPull` continuously polls for messages on the ZeroMQ socket
2. When a message is received, it's added to a batch
3. When the batch is full or the timeout expires, the batch is processed
4. For each message in the batch, all registered listeners are notified
5. `SimpleZmqListenerContainer` receives the message and applies any configured conversion or post-processing
6. The message is passed to the listener method, with appropriate parameter binding

## Configuration

The integration is configured using Spring Boot's auto-configuration mechanism:

- `ZmqAutoConfiguration` provides beans for `ZContext`, `ZmqMessageConverter`, `ZmqTemplate`, etc.
- `ZmqProperties` defines configuration properties with sensible defaults
- `SimpleZmqListenerContainerFactory` creates and configures `SimpleZmqListenerContainer` instances
- `ZmqListenerBeanPostProcessor` processes methods annotated with `@ZmqListener`

## Enhancements Over Previous Version

1. **Eliminated ZmqService**: Removed the redundant service layer, as `SimpleZmqListenerContainer` now handles message
   polling internally.

2. **Reusable and Testable Components**: Created `ZmqPull` and `ZmqPush` classes that encapsulate ZeroMQ socket
   operations and are plug-and-play.

3. **Proper Utilization of Container Fields**: Ensured all configurable fields in `SimpleZmqListenerContainer` are
   properly integrated into the message flow.

4. **Batch Processing and Timeout Support**: Implemented support for accumulating messages up to a batch size or
   timeout.

5. **Enhanced @ZmqListener Functionality**: Improved parameter binding to support direct injection of decoded objects,
   fallback to raw messages, and multiple parameters.

## Usage Examples

### Sending Messages

```java
@Autowired
private ZmqTemplate zmqTemplate;

public void sendMessage(MyPayload payload) {
    zmqTemplate.convertAndSend(payload);
}
```

### Receiving Messages

```java
@Component
public class MyListener {

    @ZmqListener
    public void handleMessage(MyPayload payload) {
        // Process the payload
    }

    @ZmqListener
    public void handleRawMessage(ZmqMessage<MyPayload> message) {
        // Process the message with headers
    }

    @ZmqListener
    public void handleMultipleParams(MyPayload payload, String rawString) {
        // Process with multiple parameters
    }
}
```

### Configuration

```yaml
zeromq:
  push-bind-address: tcp://*:5555
  pull-connect-address: tcp://localhost:5555
  buffer-size: 1000
  listener-concurrency: 5
  batch-size: 10
  batch-timeout: 1000
```
