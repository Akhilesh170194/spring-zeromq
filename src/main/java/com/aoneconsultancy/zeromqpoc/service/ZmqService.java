package com.aoneconsultancy.zeromqpoc.service;

import com.aoneconsultancy.zeromqpoc.config.ZmqProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.DisposableBean;
import org.zeromq.SocketType;
import org.springframework.integration.zeromq.ZeroMqProxy;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service that manages ZeroMQ push/pull sockets.
 */
public class ZmqService implements DisposableBean {

    private final ZmqProperties properties;
    private final ZeroMqProxy zeroMqProxy;
    private final ZContext context;
    private final ZMQ.Socket pushSocket;
    private final ZMQ.Socket pullSocket;
    private final ObjectMapper mapper = new ObjectMapper();
    private final BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();
    private final List<Consumer<byte[]>> listeners = new CopyOnWriteArrayList<>();

    public ZmqService(ZmqProperties properties, ZeroMqProxy zeroMqProxy) {
        this.properties = properties;
        this.zeroMqProxy = zeroMqProxy;
        this.context = zeroMqProxy.getContext();

        pushSocket = context.createSocket(SocketType.PUSH);
        pushSocket.setHWM(properties.getBufferSize());
        pushSocket.connect("tcp://localhost:" + zeroMqProxy.getFrontendPort());

        pullSocket = context.createSocket(SocketType.PULL);
        pullSocket.setHWM(properties.getBufferSize());
        pullSocket.connect("tcp://localhost:" + zeroMqProxy.getBackendPort());
    }

    @PostConstruct
    public void startListener() {
        listenerExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] data = pullSocket.recv(0);
                if (data != null) {
                    String json = new String(data);
                    System.out.println("Received: " + json);
                    receivedMessages.offer(json);
                    for (Consumer<byte[]> listener : listeners) {
                        listener.accept(data);
                    }
                }
            }
        });
    }

    public void send(Object payload) throws JsonProcessingException {
        pushSocket.send(mapper.writeValueAsBytes(payload), 0);
    }

    public void registerListener(Consumer<byte[]> listener) {
        listeners.add(listener);
    }

    public String pollReceived(long timeout, TimeUnit unit) throws InterruptedException {
        return receivedMessages.poll(timeout, unit);
    }

    @Override
    public void destroy() {
        listenerExecutor.shutdownNow();
        pushSocket.close();
        pullSocket.close();
    }
}
