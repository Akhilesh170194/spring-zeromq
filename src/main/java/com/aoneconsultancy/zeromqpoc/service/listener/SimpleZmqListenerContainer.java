package com.aoneconsultancy.zeromqpoc.service.listener;

import com.aoneconsultancy.zeromqpoc.service.ZmqService;

import java.util.function.Consumer;

/**
 * Default {@link ZmqListenerContainer} implementation backed by {@link ZmqService}.
 */
public class SimpleZmqListenerContainer implements ZmqListenerContainer {

    private final ZmqService zmqService;
    private Consumer<byte[]> listener;
    private boolean running;

    public SimpleZmqListenerContainer(ZmqService zmqService) {
        this.zmqService = zmqService;
    }

    @Override
    public void setMessageListener(Consumer<byte[]> listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        if (!running && listener != null) {
            zmqService.registerListener(listener);
            running = true;
        }
    }

    @Override
    public void stop() {
        if (running && listener != null) {
            zmqService.unregisterListener(listener);
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
