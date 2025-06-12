package com.aoneconsultancy.zeromq.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zeromq.SocketType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZmqProducerProperties {

    /**
     * Name of the producer
     */
    @Builder.Default
    private String name = "workerPushProducer";

    /**
     * Type of socket (PUB, PUSH, etc.)
     */
    @Builder.Default
    private SocketType type = SocketType.PUSH;

    /**
     * Whether to bind or connect the socket
     */
    @Builder.Default
    private boolean bind = true;

    /**
     * List of endpoints for the socket
     */
    @Builder.Default
    private List<String> addresses = List.of("tcp://localhost:5555");

}
