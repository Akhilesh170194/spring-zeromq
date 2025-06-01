package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.listener.MessageListenerContainer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "zeromq")
public class ZmqProperties {

    /**
     * Address where push socket will bind, e.g. tcp://*:5555
     */
    private String pushBindAddress = "tcp://*:5555";

    /**
     * Address where the pull socket connects to, e.g. tcp://localhost:5555
     */
    private String pullConnectAddress = "tcp://localhost:5555";

    /**
     * High watermark / buffer size for sockets
     */
    private int bufferSize = 1000;

    /**
     * Number of threads for {@link MessageListenerContainer}.
     */
    private int listenerConcurrency = 1;

    /**
     * Maximum number of messages to process in a batch
     */
    private int batchSize = 10;

    /**
     * Timeout in milliseconds for processing a batch
     */
    private long batchTimeout = 1000;

    /**
     * Connection retry timeout in milliseconds.
     * When a socket disconnects, the system will try to reconnect for this duration.
     * Default is 30000 (30 seconds).
     */
    private long connectionRetryTimeout = 30000;

}
