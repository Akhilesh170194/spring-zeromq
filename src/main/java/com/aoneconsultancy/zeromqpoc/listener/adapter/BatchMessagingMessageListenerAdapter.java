//package com.aoneconsultancy.zeromqpoc.listener.adapter;
//
//import com.aoneconsultancy.zeromqpoc.core.error.ZmqListenerErrorHandler;
//import com.aoneconsultancy.zeromqpoc.core.message.Message;
//import org.springframework.lang.Nullable;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.List;
//
/// **
// * An adapter that invokes a handler method for a batch of ZmqMessages.
// * This adapter directly invokes the method with the List<ZmqMessage>.
// */
//public class BatchMessagingMessageListenerAdapter extends MessagingMessageListenerAdapter {
//
//    /**
//     * Create a new BatchMessagingMessageListenerAdapter.
//     * @param bean the bean instance
//     * @param method the method to invoke
//     * @param returnExceptions whether to return exceptions
//     * @param errorHandler the error handler
//     */
//    public BatchMessagingMessageListenerAdapter(Object bean, Method method, boolean returnExceptions,
//                                               @Nullable ZmqListenerErrorHandler errorHandler) {
//        super(bean, method, returnExceptions, errorHandler);
//    }
//
//    /**
//     * Handle a batch of messages by invoking the handler method directly.
//     * @param messages the list of messages to handle
//     */
//    @Override
//    public void onBatchMessage(List<Message> messages) {
//        try {
//            Method method = getMethod();
//            Object bean = getBean();
//
//            // Invoke the method with the list of messages
//            method.invoke(bean, messages);
//        }
//        catch (InvocationTargetException e) {
//            // Get the first message for error handling, or null if the list is empty
//            Message firstMessage = messages.isEmpty() ? null : messages.get(0);
//            handleInvocationException(e.getTargetException(), firstMessage);
//        }
//        catch (Exception e) {
//            // Get the first message for error handling, or null if the list is empty
//            Message firstMessage = messages.isEmpty() ? null : messages.get(0);
//            handleInvocationException(e, firstMessage);
//        }
//    }
//}