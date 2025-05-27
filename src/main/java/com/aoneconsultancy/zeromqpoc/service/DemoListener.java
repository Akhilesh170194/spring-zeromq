package com.aoneconsultancy.zeromqpoc.service;

import com.aoneconsultancy.zeromqpoc.listener.ZmqListener;
import com.aoneconsultancy.zeromqpoc.model.payload.DemoPayload;
import org.springframework.stereotype.Component;

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
