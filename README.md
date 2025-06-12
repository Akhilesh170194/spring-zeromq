# Spring ZeroMQ Integration for Spring Boot

## Overview
Spring ZeroMQ is a comprehensive integration library that brings the power of ZeroMQ messaging to Spring Boot applications. It provides a familiar Spring-style programming model for working with ZeroMQ sockets, making it easy to build high-performance, distributed messaging systems.

> **Current Limitations:** 
> - Only PUSH/PULL socket types are currently supported
> - Security features (authentication) are not yet implemented

## Features
- **Familiar Spring Programming Model**: Use annotations and templates similar to other Spring messaging integrations
- **Auto-configuration**: Automatic setup of ZeroMQ components based on application properties
- **Annotation-driven Listeners**: Declarative message handling with `@ZmqListener`
- **Template-based Sending**: Easy message sending with `ZmqTemplate`
- **Socket Types**: Currently supports PUSH/PULL socket types only
- **Batch Processing**: Process messages individually or in batches
- **Concurrency Control**: Configure multiple consumer threads per listener
- **Error Handling**: Comprehensive error handling and recovery mechanisms
- **Monitoring**: Socket event monitoring and metrics

## Modules
### 1. `spring-zeromq-core`
Core functionality including message listeners, templates, converters, and socket management.

### 2. `spring-boot-autoconfigure-zeromq`
Auto-configuration for ZeroMQ components based on application properties.

### 3. `spring-boot-starter-zeromq`
Starter package that includes all necessary dependencies for quick setup.

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Spring Boot 3.1 or higher

### Installation
Add the following dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>com.aoneconsultancy</groupId>
    <artifactId>spring-boot-starter-zeromq</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Basic Configuration
Add the following properties to your `application.yaml`:
```yaml
spring:
  zeromq:
    template:
      default-endpoint: "tcp://localhost:5555"
      producer:
        name: "defaultProducer"
        addresses: ["tcp://localhost:5555"]
        bind: false
        type: PUSH
    listener:
      concurrency: 3
      consumer:
        name: "defaultConsumer"
        addresses: ["tcp://localhost:5556"]
        bind: true
        type: PULL
```

## Usage Examples

### Sending Messages

#### Using ZmqTemplate
The `ZmqTemplate` provides methods for sending raw bytes or converting objects to messages:

```java
@Service
public class MessageSender {
    private final ZmqTemplate zmqTemplate;

    public MessageSender(ZmqTemplate zmqTemplate) {
        this.zmqTemplate = zmqTemplate;
    }

    // Send a string message
    public void sendMessage(String message) {
        zmqTemplate.convertAndSend(message);
    }

    // Send to a specific endpoint
    public void sendToEndpoint(String endpoint, String message) {
        zmqTemplate.convertAndSend(endpoint, message);
    }

    // Send with post-processing
    public void sendWithHeaders(String message) {
        zmqTemplate.convertAndSend(message, msg -> {
            msg.getHeaders().put("timestamp", System.currentTimeMillis());
            return msg;
        });
    }

    // Send raw bytes
    public void sendBytes(byte[] data) {
        zmqTemplate.sendBytes(data);
    }
}
```

### Receiving Messages

#### Using @ZmqListener Annotation
The `@ZmqListener` annotation provides a declarative way to define message listeners:

```java
@Component
public class MessageListener {

    // Basic listener
    @ZmqListener(endpoints = "tcp://localhost:5556")
    public void handleMessage(String message) {
        System.out.println("Received: " + message);
    }

    // Listener with explicit socket type (currently only PULL is supported)
    @ZmqListener(endpoints = "tcp://localhost:5557", socketType = SocketType.PULL)
    public void handleMessage2(String message) {
        System.out.println("Received from second endpoint: " + message);
    }

    // Note: Other socket types like PUB/SUB, REQ/REP will be supported in future releases

    // Batch listener
    @ZmqListener(endpoints = "tcp://localhost:5558", batch = true)
    public void handleBatch(List<Message> messages) {
        System.out.println("Received batch of " + messages.size() + " messages");
        for (Message msg : messages) {
            System.out.println("Batch message: " + msg.getBody());
        }
    }

    // Multi-threaded listener
    @ZmqListener(endpoints = "tcp://localhost:5559", concurrency = 5)
    public void handleWithConcurrency(String message) {
        System.out.println("Received by thread " + 
            Thread.currentThread().getName() + ": " + message);
    }
}
```

### Manual Container Configuration
For more advanced scenarios, you can configure listener containers programmatically:

```java
@Configuration
@EnableZmq
public class ZmqConfig {

    @Bean
    public PullZmqSocketListenerContainerFactory listenerContainerFactory(ZContext zContext) {
        PullZmqSocketListenerContainerFactory factory = new PullZmqSocketListenerContainerFactory();
        factory.setZContext(zContext);
        factory.setConcurrency(10);
        factory.setSocketHwm(1000);
        factory.setSocketRecvBuffer(2048);
        factory.setSocketReconnectInterval(5000);
        return factory;
    }

    @Bean
    public MessageListenerContainer customContainer(
            ZContext zContext, 
            MessageListener listener) {

        ZmqConsumerProperties consumerProps = new ZmqConsumerProperties();
        consumerProps.setName("customConsumer");
        consumerProps.setAddresses(Arrays.asList("tcp://localhost:5560"));
        consumerProps.setBind(true);
        consumerProps.setType(SocketType.PULL);

        PullZmqMessageListenerContainer container = new PullZmqMessageListenerContainer(zContext);
        container.setZmqConsumerProps(consumerProps);
        container.setConcurrency(3);
        container.setupMessageListener(listener);
        return container;
    }
}
```

## Configuration Properties

### Context Properties
```yaml
spring:
  zeromq:
    io-threads: 2              # Number of I/O threads for ZeroMQ
    linger: 0                  # Socket linger time in ms
    max-sockets: 1024          # Maximum number of sockets
```

### Template Properties
```yaml
spring:
  zeromq:
    template:
      default-endpoint: "tcp://localhost:5555"  # Default endpoint
      send-timeout: 2000                        # Send timeout in ms
      socket-send-buffer: 1024                  # Socket send buffer size
      socket-hwm: 1000                          # High water mark
      poll-retry: 3                             # Number of send retries
      retry-delay: 100                          # Delay between retries in ms
      backpressure-enabled: true                # Enable/disable backpressure
      producer:
        name: "defaultProducer"                 # Producer name
        addresses: ["tcp://localhost:5555"]     # Socket addresses
        bind: false                             # Whether to bind or connect
        type: PUSH                              # Socket type
```

### Listener Properties
```yaml
spring:
  zeromq:
    listener:
      consumer-batch-enabled: false             # Enable batch consumption
      concurrency: 3                            # Number of threads
      batch-size: 10                            # Batch size
      batch-timeout: 1000                       # Batch timeout in ms
      retry-timeout: 30000                      # Retry timeout in ms
      socket-poll-timeout: 500                  # Socket poll timeout in ms
      socket-recv-buffer: 1024                  # Socket receive buffer size
      socket-hwm: 1000                          # High water mark
      socket-reconnect-interval: 5000           # Reconnect interval in ms
      socket-backoff: 100                       # Backoff time in ms
      consumer:
        name: "defaultConsumer"                 # Consumer name
        addresses: ["tcp://localhost:5556"]     # Socket addresses
        bind: true                              # Whether to bind or connect
        type: PULL                              # Socket type
```

## Advanced Features

### Authentication (Future Feature)
ZeroMQ supports PLAIN and CURVE authentication mechanisms, which will be implemented in future releases:

```yaml
spring:
  zeromq:
    auth:
      enabled: true
      mechanism: "PLAIN"       # PLAIN or CURVE
      username: "admin"        # For PLAIN auth
      password: "secret"       # For PLAIN auth
      domain: "global"
      curve:
        server: true           # Is this a CURVE server?
        public-key: "..."      # Server public key
        secret-key: "..."      # Server secret key
        client-keys:           # Authorized clients
          - name: "client1"
            public-key: "..."
```

> **Note:** Authentication is not yet implemented in the current version.

### Error Handling
You can configure custom error handlers for your listeners:

```java
@Bean
public ZmqListenerErrorHandler customErrorHandler() {
    return (exception, data) -> {
        log.error("Error processing message: {}", exception.getMessage());
        // Custom recovery logic
    };
}

@ZmqListener(endpoints = "tcp://localhost:5556", errorHandler = "customErrorHandler")
public void handleWithCustomErrorHandler(String message) {
    // Process message
}
```

### Message Post-Processing
You can apply post-processors to messages before sending or after receiving:

```java
@Bean
public MessagePostProcessor compressionPostProcessor() {
    return message -> {
        // Compress message body
        byte[] compressed = compressData(message.getBody());
        return new Message(compressed, message.getHeaders());
    };
}
```

And then use it in your service:

```java
// In your service
public void sendCompressed(String message) {
    zmqTemplate.convertAndSend(message, compressionPostProcessor());
}
```
