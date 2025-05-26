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
        DemoPayload payload = new DemoPayload(1L, "Test", LocalDateTime.now());
        zmqService.send(payload);
        return payload;
    }
}
