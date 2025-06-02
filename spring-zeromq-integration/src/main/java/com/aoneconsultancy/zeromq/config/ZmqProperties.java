package com.aoneconsultancy.zeromq.config;

import com.aoneconsultancy.zeromq.listener.MessageListenerContainer;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.zeromq.SocketType;

@Data
@ConfigurationProperties(prefix = "spring.zeromq.listener")
public class ZmqProperties {

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

    /**
     * Default socket type
     */
    private SocketType socketType = SocketType.PUSH;

    /**
     * Configuration for PUSH sockets
     */
    private Push push = new Push();

    /**
     * Configuration for PULL sockets
     */
    private Pull pull = new Pull();

    /**
     * Configuration for PUB sockets
     */
    private Pub pub = new Pub();

    /**
     * Configuration for SUB sockets
     */
    private Sub sub = new Sub();

    /**
     * Configuration for REQ sockets
     */
    private Req req = new Req();

    /**
     * Configuration for REP sockets
     */
    private Rep rep = new Rep();

    /**
     * Configuration for PUSH sockets
     */
    @Data
    public static class Push {
        /**
         * List of addresses for push sockets
         */
        private List<String> addresses;
    }

    /**
     * Configuration for PULL sockets
     */
    @Data
    public static class Pull {
        /**
         * List of addresses for pull sockets
         */
        private List<String> addresses;
    }

    /**
     * Configuration for PUB sockets
     */
    @Data
    public static class Pub {
        /**
         * List of addresses for pub sockets
         */
        private List<String> addresses;

        /**
         * Topics to publish
         */
        private List<String> topics;
    }

    /**
     * Configuration for SUB sockets
     */
    @Data
    public static class Sub {
        /**
         * List of addresses for sub sockets
         */
        private List<String> addresses;

        /**
         * Topics to subscribe to
         */
        private List<String> topics;
    }

    /**
     * Configuration for REQ sockets
     */
    @Data
    public static class Req {
        /**
         * List of addresses for req sockets
         */
        private List<String> addresses;
    }

    /**
     * Configuration for REP sockets
     */
    @Data
    public static class Rep {
        /**
         * List of addresses for rep sockets
         */
        private List<String> addresses;
    }

}
