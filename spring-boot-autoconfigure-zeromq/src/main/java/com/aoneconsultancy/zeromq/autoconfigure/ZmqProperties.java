package com.aoneconsultancy.zeromq.autoconfigure;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.zeromq.SocketType;

/**
 * Configuration properties for ZeroMQ integration.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "spring.zeromq")
public class ZmqProperties {

    private Integer contextThread = 1;
    private Listener listener = new Listener();

    @Data
    public static class Listener {

        private Boolean consumerBatchEnabled;

        /**
         * High watermark / buffer size for sockets
         */
        private int bufferSize = 1000;

        private Boolean acknowledge;

        /**
         * Number of threads for message listener containers.
         */
        private int concurrency = 1;

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
        private SocketType type = SocketType.PULL;

        /**
         * Configuration for PUSH sockets
         */
        @Getter
        private final Push push = new Push();

        /**
         * Configuration for PULL sockets
         */
        @Getter
        private final Pull pull = new Pull();

        /**
         * Configuration for PUB sockets
         */
        @Getter
        private final Pub pub = new Pub();

        /**
         * Configuration for SUB sockets
         */
        @Getter
        private final Sub sub = new Sub();

        /**
         * Configuration for REQ sockets
         */
        @Getter
        private final Req req = new Req();

        /**
         * Configuration for REP sockets
         */
        @Getter
        private final Rep rep = new Rep();

    }

    /**
     * Configuration for PUSH sockets
     */
    public static class Push {
        /**
         * List of addresses for push sockets
         */
        @Setter
        @Getter
        private List<String> addresses = List.of("tcp://localhost:5555");
    }

    /**
     * Configuration for PULL sockets
     */
    public static class Pull {
        /**
         * List of addresses for pull sockets
         */
        @Setter
        @Getter
        private List<String> addresses = List.of("tcp://localhost:5556");
    }

    /**
     * Configuration for PUB sockets
     */
    public static class Pub {
        /**
         * List of addresses for pub sockets
         */
        @Setter
        @Getter
        private List<String> addresses = List.of("tcp://localhost:5557");

        /**
         * Topics to publish
         */
        @Setter
        @Getter
        private List<String> topics = List.of();
    }

    /**
     * Configuration for SUB sockets
     */
    public static class Sub {
        /**
         * List of addresses for sub sockets
         */
        @Setter
        @Getter
        private List<String> addresses = List.of("tcp://localhost:5557");

        /**
         * Topics to subscribe to
         */
        @Setter
        @Getter
        private List<String> topics = List.of();
    }

    /**
     * Configuration for REQ sockets
     */
    public static class Req {
        /**
         * List of addresses for req sockets
         */
        @Setter
        @Getter
        private List<String> addresses = List.of("tcp://localhost:5558");
    }

    /**
     * Configuration for REP sockets
     */
    public static class Rep {
        /**
         * List of addresses for rep sockets
         */
        @Setter
        @Getter
        private List<String> addresses = List.of("tcp://localhost:5558");
    }

}
