package com.aoneconsultancy.zeromqpoc.core;

import com.aoneconsultancy.zeromqpoc.core.message.Message;
import java.util.List;

public interface MessageListener {

    /**
     * Delivers a single message.
     *
     * @param message the message.
     */
    void onMessage(Message message);

    /**
     * Delivers a batch of messages.
     *
     * @param messages the messages.
     * @since 2.2
     */
    default void onMessageBatch(List<Message> messages) {
        throw new UnsupportedOperationException("This listener does not support message batches");
    }

}
