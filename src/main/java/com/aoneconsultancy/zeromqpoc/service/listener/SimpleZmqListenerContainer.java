package com.aoneconsultancy.zeromqpoc.service.listener;

import com.aoneconsultancy.zeromqpoc.service.ZmqService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Default {@link ZmqListenerContainer} implementation backed by {@link ZmqService}.
 */
public class SimpleZmqListenerContainer implements ZmqListenerContainer {

    private final ZmqService zmqService;
    private Consumer<byte[]> listener;
    private boolean running;
    private Consumer<byte[]> internalListener;
    private boolean running;
    private int concurrency = 1;
    private ExecutorService executor;

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
            this.executor = Executors.newFixedThreadPool(concurrency);
            this.internalListener = bytes -> executor.execute(() -> listener.accept(bytes));
            zmqService.registerListener(internalListener);
            running = true;
        }
    }

    @Override
    public void stop() {
        if (running && internalListener != null) {
            zmqService.unregisterListener(internalListener);
            executor.shutdown();
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
