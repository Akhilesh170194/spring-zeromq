# Consolidation of ZmqPull and ZmqConsumer

## Background

The ZeroMQ POC project originally had two classes with overlapping responsibilities:

1. `ZmqPull` - A class that encapsulated the ZeroMQ socket operations for receiving messages
2. `ZmqConsumer` - An inner class in `SimpleZmqListenerContainer` that managed a `ZmqPull` instance and handled dynamic
   scaling

This design created redundancy and unnecessary complexity:

- Both classes implemented `Runnable` and ran in their own threads
- Both classes had start/stop lifecycle methods
- Both classes managed message reception
- The layered architecture (Container → Consumer → Pull) added complexity without clear benefits

## Changes Made

The following changes were made to consolidate these classes:

1. Enhanced `ZmqPull` to include the dynamic scaling functionality from `ZmqConsumer`:
    - Added an ID field and constructor parameter
    - Added methods for tracking message activity (messageReceived, idleDetected)
    - Added getters for consecutive idle and message counts
    - Added a getThread method to identify the consumer thread
    - Updated the run method to track message activity and notify listeners of idle state

2. Modified `SimpleZmqListenerContainer` to use `ZmqPull` directly:
    - Changed the consumers field type from Set<ZmqConsumer> to Set<ZmqPull>
    - Updated all methods that used ZmqConsumer to use ZmqPull instead
    - Updated the internalListener to handle idle notifications
    - Removed the ZmqConsumer inner class

## Benefits

This consolidation provides several benefits:

1. **Simplified Architecture**: Removed a layer of indirection, making the code easier to understand and maintain
2. **Reduced Redundancy**: Eliminated duplicate code and overlapping responsibilities
3. **Improved Reusability**: ZmqPull is now a more complete and reusable component
4. **Better Separation of Concerns**: ZmqPull handles socket operations and message reception, while
   SimpleZmqListenerContainer handles container lifecycle and consumer management

## Conclusion

By consolidating ZmqPull and ZmqConsumer, we've created a more streamlined and maintainable codebase while preserving
all the original functionality, including dynamic scaling of consumers based on message activity.