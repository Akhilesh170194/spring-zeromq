package com.aoneconsultancy.zeromq.config;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zeromq.SocketType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZmqProducer {

    /**
     * Name of the producer
     */
    private String name = "workerPushProducer";

    /**
     * Type of socket (PUB, PUSH, etc.)
     */
    private SocketType type = SocketType.PUSH;

    /**
     * Whether to bind or connect the socket
     */
    private boolean bind = true;

    /**
     * List of endpoints for the socket
     */
    private List<String> addresses = List.of("tcp://localhost:5555");

}
