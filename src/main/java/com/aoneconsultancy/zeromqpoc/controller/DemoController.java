package com.aoneconsultancy.zeromqpoc.controller;

import com.aoneconsultancy.zeromqpoc.model.payload.DemoPayload;
import com.aoneconsultancy.zeromqpoc.service.ZmqTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class DemoController {

    private final ZmqTemplate zmqTemplate;

    public DemoController(ZmqTemplate zmqTemplate) {
        this.zmqTemplate = zmqTemplate;
    }

    @GetMapping("/demo")
    public DemoPayload sendDemo() throws Exception {
        DemoPayload payload = DemoPayload.builder()
                .id(1L)
                .name("Test")
                .createdAt(LocalDateTime.now())
                .build();
        zmqTemplate.convertAndSend(payload);
        return payload;
    }
}
