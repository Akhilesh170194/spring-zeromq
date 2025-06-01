# Why ZmqPush is Needed for Sending Messages in ZmqTemplate

## Introduction

This document explains why `ZmqPush` is a necessary component for sending messages in `ZmqTemplate`, addressing the
architectural reasoning behind this design decision.

## Architectural Benefits

### 1. Separation of Concerns

The use of `ZmqPush` in `ZmqTemplate` follows the Single Responsibility Principle (SRP), a core principle of good
software design:

- **ZmqTemplate**: Handles high-level concerns like message conversion, post-processing, and adding standard headers
- **ZmqPush**: Focuses exclusively on socket operations for sending messages

This separation makes the code more maintainable and easier to understand, as each class has a clear, focused
responsibility.

### 2. Encapsulation of ZeroMQ Socket Operations

`ZmqPush` encapsulates all the low-level ZeroMQ socket operations:

- Socket creation and configuration
- Setting high water mark (buffer size)
- Binding to addresses
- Sending messages with appropriate flags
- Proper socket cleanup

This encapsulation shields `ZmqTemplate` from the complexities of ZeroMQ's API, allowing it to focus on its higher-level
responsibilities.

### 3. Improved Resource Management

`ZmqPush` provides proper resource management:

- Each `ZmqPush` instance manages a single socket
- The `close()` method ensures sockets are properly closed
- `ZmqTemplate` can manage multiple `ZmqPush` instances for different addresses
- Resources are cleaned up when `ZmqTemplate` is destroyed

### 4. Enhanced Testability

Using `ZmqPush` makes the code more testable:

- `ZmqPush` can be mocked in unit tests for `ZmqTemplate`
- Socket operations can be tested independently from message conversion logic
- Different socket configurations can be tested in isolation

### 5. Support for Multiple Destinations

`ZmqTemplate` maintains a map of `ZmqPush` instances for different addresses:

```java
private final Map<String, ZmqPush> pushSockets = new HashMap<>();

private synchronized ZmqPush getPushSocket(String address) {
    return pushSockets.computeIfAbsent(address, addr -> new ZmqPush(context, addr, bufferSize));
}
```

This allows sending messages to different destinations without creating new sockets for each send operation.

## Comparison with Previous Approach

The previous architecture used a monolithic `ZmqService` that combined both push and pull functionality:

- **ZmqService**: Managed both sending and receiving in a single class
- Created and managed both push and pull sockets
- Mixed concerns of message sending, receiving, and listener notification

The new architecture with separate `ZmqPush` and `ZmqPull` classes is:

- More modular and focused
- Easier to test and maintain
- More flexible for different use cases
- More aligned with Spring's design principles (similar to how RabbitTemplate works)

## Conclusion

Using `ZmqPush` for sending messages in `ZmqTemplate` is not just a design choice but a necessity for creating a
well-structured, maintainable, and testable ZeroMQ integration for Spring Boot. It follows established software design
principles and patterns, resulting in a more robust and flexible architecture.