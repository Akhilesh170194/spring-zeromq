# ZeroMQ Spring Boot Starter

This is a Spring Boot starter for ZeroMQ, providing auto-configuration for ZeroMQ integration in Spring Boot
applications.

## Usage

Add the following dependency to your project:

```xml
<dependency>
    <groupId>com.aoneconsultancy</groupId>
    <artifactId>spring-boot-starter-zeromq</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Configuration

The starter provides the following configuration properties:

```properties
# Enable/disable ZeroMQ integration (default: true)
zeromq.enabled=true

# Enable/disable ZeroMQ core components (default: true)
zeromq.core.enabled=true

# Enable/disable ZeroMQ listener components (default: true)
zeromq.listener.enabled=true

# High watermark / buffer size for sockets (default: 1000)
zeromq.buffer-size=1000

# Number of threads for message listener containers (default: 1)
zeromq.listener-concurrency=1

# Maximum number of messages to process in a batch (default: 10)
zeromq.batch-size=10

# Timeout in milliseconds for processing a batch (default: 1000)
zeromq.batch-timeout=1000

# Connection retry timeout in milliseconds (default: 30000)
zeromq.connection-retry-timeout=30000

# Default socket type (default: PUSH)
zeromq.socket-type=PUSH

# PUSH socket configuration
zeromq.push.addresses[0]=tcp://*:5557

# PULL socket configuration
zeromq.pull.addresses[0]=tcp://localhost:5557

# PUB socket configuration
zeromq.pub.addresses[0]=tcp://*:5558
zeromq.pub.topics[0]=topic1

# SUB socket configuration
zeromq.sub.addresses[0]=tcp://localhost:5558
zeromq.sub.topics[0]=topic1

# REQ socket configuration
zeromq.req.addresses[0]=tcp://localhost:5559

# REP socket configuration
zeromq.rep.addresses[0]=tcp://*:5559
```

## Features

- Auto-configuration for ZeroMQ context
- Support for PUSH/PULL, PUB/SUB, and REQ/REP socket types
- Integration with Spring's messaging infrastructure
- Annotation-based message listeners with `@ZmqListener`
- Template-based message sending with `ZmqTemplate`

## Example

```java
@Service
public class MyService {

    private final ZmqTemplate zmqTemplate;

    public MyService(ZmqTemplate zmqTemplate) {
        this.zmqTemplate = zmqTemplate;
    }

    public void sendMessage(String message) {
        zmqTemplate.convertAndSend(message);
    }

    @ZmqListener(addresses = "tcp://localhost:5557")
    public void handleMessage(String message) {
        System.out.println("Received message: " + message);
    }
}
```

## License

This project is licensed under the MIT License.