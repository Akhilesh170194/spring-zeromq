package com.aoneconsultancy.zeromqpoc.zmq;

import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simple component that receives {@link DemoPayload} messages using
 * {@link ZmqListener} and prints them.
 */
@Component
public class DemoListener {
    private final BlockingQueue<DemoPayload> received = new LinkedBlockingQueue<>();

    @ZmqListener
    public void handle(DemoPayload payload) {
        received.offer(payload);
        System.out.println("@ZmqListener received: " + payload);
    }

    public DemoPayload poll(long timeout, TimeUnit unit) throws InterruptedException {
        return received.poll(timeout, unit);
    }
}
