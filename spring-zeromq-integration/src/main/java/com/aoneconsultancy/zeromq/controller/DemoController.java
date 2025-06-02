package com.aoneconsultancy.zeromq.controller;

import com.aoneconsultancy.zeromq.model.payload.DemoPayload;
import com.aoneconsultancy.zeromq.service.ZmqTemplate;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
