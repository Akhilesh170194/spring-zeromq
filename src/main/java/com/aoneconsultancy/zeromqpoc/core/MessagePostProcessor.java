package com.aoneconsultancy.zeromqpoc.core;

import com.aoneconsultancy.zeromqpoc.core.message.Message;
import com.aoneconsultancy.zeromqpoc.support.ZmqException;

/**
 * Used in several places in the framework, such as
 * {@code AmqpTemplate#convertAndSend(Object, MessagePostProcessor)} where it can be used
 * to add/modify headers or properties after the message conversion has been performed. It
 * also can be used to modify inbound messages when receiving messages in listener
 * containers and {@code AmqpTemplate}s.
 */
@FunctionalInterface
public interface MessagePostProcessor {

    /**
     * Change (or replace) the message.
     *
     * @param message the message.
     * @return the message.
     * @throws ZmqException an exception.
     */
    Message postProcessMessage(Message message) throws ZmqException;

}
