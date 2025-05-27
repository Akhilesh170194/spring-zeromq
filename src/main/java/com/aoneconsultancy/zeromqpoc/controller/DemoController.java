package com.aoneconsultancy.zeromqpoc.controller;

import com.aoneconsultancy.zeromqpoc.model.payload.DemoPayload;
import com.aoneconsultancy.zeromqpoc.service.ZmqService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class DemoController {

    private final ZmqService zmqService;

    public DemoController(ZmqService zmqService) {
        this.zmqService = zmqService;
    }

    @GetMapping("/demo")
    public DemoPayload sendDemo() throws Exception {
        DemoPayload payload = DemoPayload.builder()
                .id(1L)
                .name("Test")
                .createdAt(LocalDateTime.now())
                .build();
        zmqService.send(payload);
        return payload;
    }
}
