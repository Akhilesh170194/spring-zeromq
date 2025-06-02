# ZeroMQ Spring Boot Autoconfigure

This module provides auto-configuration for ZeroMQ integration in Spring Boot applications.

## Purpose

The autoconfigure module is responsible for:

1. Providing configuration properties for ZeroMQ integration
2. Auto-configuring ZeroMQ beans when ZeroMQ is on the classpath
3. Conditionally enabling/disabling features based on configuration

## Implementation

The module includes:

- `ZmqProperties`: Configuration properties for ZeroMQ integration
- `ZmqAutoConfiguration`: Auto-configuration for ZeroMQ beans
- `ZmqListenerConfigUtils`: Constants for ZeroMQ listener infrastructure

## Usage

This module is not meant to be used directly. Instead, include the `spring-boot-starter-zeromq` dependency in your
project:

```xml

<dependency>
  <groupId>com.aoneconsultancy</groupId>
  <artifactId>spring-boot-starter-zeromq</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Development

To extend or modify the auto-configuration:

1. Add or modify configuration properties in `ZmqProperties`
2. Update the auto-configuration in `ZmqAutoConfiguration`
3. Add or modify conditional beans as needed

## Testing

To test the auto-configuration:

1. Create a test application with the starter dependency
2. Configure ZeroMQ properties in `application.properties` or `application.yml`
3. Verify that the expected beans are created and configured correctly

## License

This project is licensed under the MIT License.