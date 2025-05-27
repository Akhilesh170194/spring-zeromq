package com.aoneconsultancy.zeromqpoc.service.listener;

import com.aoneconsultancy.zeromqpoc.model.payload.DemoPayload;
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

    @ZmqListener
    public void handle(DemoPayload payload) {
        System.out.println("@ZmqListener received: " + payload);
    }

}
