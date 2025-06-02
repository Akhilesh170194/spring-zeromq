# ZeroMQ Spring Boot Integration

This project provides Spring Boot integration for ZeroMQ, a high-performance asynchronous messaging library.

## Project Structure

The project is organized into the following modules:

- **zeromq-poc**: The main application that demonstrates ZeroMQ integration with Spring Boot
- **spring-boot-starter-zeromq**: A Spring Boot starter for ZeroMQ integration
- **spring-boot-autoconfigure-zeromq**: Auto-configuration for ZeroMQ integration

## Getting Started

### Using the Starter

To use the ZeroMQ Spring Boot Starter in your project, add the following dependency:

```xml

<dependency>
  <groupId>com.aoneconsultancy</groupId>
  <artifactId>spring-boot-starter-zeromq</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Configuration

Configure ZeroMQ in your `application.properties` or `application.yml`:

```properties
# Enable/disable ZeroMQ integration (default: true)
zeromq.enabled=true
# Socket configurations
zeromq.push.addresses[0]=tcp://*:5557
zeromq.pull.addresses[0]=tcp://localhost:5557
```

See the [starter documentation](spring-boot-starter-zeromq/README.md) for more configuration options.

### Example Usage

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

## Building from Source

To build the project from source:

```bash
git clone https://github.com/yourusername/zeromq-spring-boot.git
cd zeromq-spring-boot
mvn clean install
```

## Documentation

- [ZeroMQ Spring Boot Starter](spring-boot-starter-zeromq/README.md)
- [ZeroMQ Spring Boot Autoconfigure](spring-boot-autoconfigure-zeromq/README.md)

## License

This project is licensed under the MIT License.