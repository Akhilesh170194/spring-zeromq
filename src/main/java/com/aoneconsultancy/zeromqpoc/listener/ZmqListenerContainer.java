package com.aoneconsultancy.zeromqpoc.listener;

import java.util.function.Consumer;

/**
 * Simple abstraction representing a ZeroMQ message listener container.
 */
public interface ZmqListenerContainer {

    /**
     * Set the listener that will receive raw message bytes.
     * @param listener consumer of the received bytes
     */
    void setMessageListener(Consumer<byte[]> listener);

    /** Start listening for messages. */
    void start();

    /** Stop listening for messages. */
    void stop();

    /**
     * @return whether the container is currently running
     */
    boolean isRunning();

}
