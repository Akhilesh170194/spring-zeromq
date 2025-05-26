package com.aoneconsultancy.zeromqpoc;

import com.aoneconsultancy.zeromqpoc.service.listener.DemoListener;
import com.aoneconsultancy.zeromqpoc.model.payload.DemoPayload;
import com.aoneconsultancy.zeromqpoc.service.ZmqService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ZmqIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ZmqService zmqService;

    @Autowired
    DemoListener demoListener;

    @Test
    void sendEndpointShouldSendAndReceiveMessage() throws Exception {
        DemoPayload response = restTemplate.getForObject("http://localhost:" + port + "/demo", DemoPayload.class);
        assertThat(response.getId()).isEqualTo(1L);
        String receivedJson = zmqService.pollReceived(5, TimeUnit.SECONDS);
        assertThat(receivedJson).isNotNull();
        assertThat(receivedJson).contains("\"id\":1");
        assertThat(receivedJson).contains("\"name\":\"Test\"");

        DemoPayload received = demoListener.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.getName()).isEqualTo("Test");
    }
}
