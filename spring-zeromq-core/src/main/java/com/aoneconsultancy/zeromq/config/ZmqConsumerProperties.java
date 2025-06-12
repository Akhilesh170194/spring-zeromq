package com.aoneconsultancy.zeromq.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zeromq.SocketType;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZmqConsumerProperties {
    /**
     * Name of the consumer
     */
    @Builder.Default
    private String name = "workerPullConsumer";

    /**
     * Type of socket (SUB, PULL, etc.)
     */
    @Builder.Default
    private SocketType type = SocketType.PULL;

    /**
     * Whether to bind or connect the socket
     */
    @Builder.Default
    private boolean bind = false;

    /**
     * List of endpoints for the socket
     */
    @Builder.Default
    private List<String> addresses = List.of("tcp://localhost:5555");

    /**
     * Topics to subscribe to (for SUB sockets)
     */
    @Builder.Default
    private List<String> topics = new ArrayList<>();

}
