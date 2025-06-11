package com.aoneconsultancy.zeromq.config;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.zeromq.SocketType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZmqConsumer {
    /**
     * Name of the consumer
     */
    private String name = "workerPullConsumer";

    /**
     * Type of socket (SUB, PULL, etc.)
     */
    private SocketType type = SocketType.PULL;

    /**
     * Whether to bind or connect the socket
     */
    private boolean bind = false;

    /**
     * List of endpoints for the socket
     */
    private List<String> addresses = List.of("tcp://localhost:5555");

    /**
     * Topics to subscribe to (for SUB sockets)
     */
    private List<String> topics = new ArrayList<>();

}
