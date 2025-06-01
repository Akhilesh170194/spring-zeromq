# Why ZmqHandler is Not Needed

## Background

In Spring AMQP, there are two main annotations for message listeners:

1. `@RabbitListener` - Can be applied at the method level or class level
2. `@RabbitHandler` - Used with class-level `@RabbitListener` to mark methods that handle different types of messages

When `@RabbitListener` is applied at the class level, methods annotated with `@RabbitHandler` within that class become
message handlers. This allows a single listener container to route messages to different methods based on the payload
type.

## Current Architecture

In our ZeroMQ integration, we have:

1. `@ZmqListener` - Defined to be applicable at both method and class levels
2. `ZmqListenerBeanPostProcessor` - Processes methods annotated with `@ZmqListener`

However, our current implementation only supports method-level `@ZmqListener` annotations. The
`ZmqListenerBeanPostProcessor` does not process class-level annotations or look for methods annotated with a potential
`@ZmqHandler` annotation.

## Why ZmqHandler is Not Needed

1. **No Current Usage**: There are no classes in the project that use class-level `@ZmqListener` annotations.

2. **Simplified Architecture**: The current architecture is simpler and more straightforward, with each listener method
   directly annotated with `@ZmqListener`.

3. **Sufficient Functionality**: The method-level `@ZmqListener` annotation provides all the necessary functionality for
   our use cases, as demonstrated in the examples in ARCHITECTURE.md.

4. **Reduced Complexity**: Without the need to route messages to different methods based on payload type within a single
   listener container, we avoid the additional complexity that `@ZmqHandler` would introduce.

5. **Future Extensibility**: If needed in the future, we can add `@ZmqHandler` support by enhancing the
   `ZmqListenerBeanPostProcessor` to handle class-level `@ZmqListener` annotations and methods annotated with
   `@ZmqHandler`.

## Conclusion

While the `@ZmqListener` annotation is defined to be applicable at both method and class levels (similar to
`@RabbitListener`), the current implementation only supports method-level usage. This is sufficient for our current
needs, and there is no evidence that `@ZmqHandler` functionality is required.

If more complex message routing based on payload type is needed in the future, we can implement `@ZmqHandler` and
enhance the `ZmqListenerBeanPostProcessor` to support it.