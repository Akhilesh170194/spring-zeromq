package com.aoneconsultancy.zeromq.autoconfigure;

import java.util.ArrayList;
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

    /**
     * Total number of I/O threads used internally by ZeroMQ
     */
    private Integer ioThreads = 2;

    /**
     * How long to wait before closing socket (in ms)
     */
    private Integer linger = 0;

    /**
     * Maximum number of sockets allowed in context
     */
    private Integer maxSockets = 1024;

    /**
     * Authentication configuration
     */
    private final Auth auth = new Auth();

    /**
     * Listener configuration
     */
    private final Listener listener = new Listener();

    /**
     * Template configuration
     */
    private final Template template = new Template();

    /**
     * Authentication configuration
     */
    @Data
    public static class Auth {
        /**
         * Whether authentication is enabled
         */
        private boolean enabled = false;

        /**
         * Authentication mechanism (PLAIN/CURVE)
         */
        private String mechanism;

        /**
         * Username for PLAIN authentication
         */
        private String username;

        /**
         * Password for PLAIN authentication
         */
        private String password;

        /**
         * Authentication domain
         */
        private String domain = "global";

        /**
         * CURVE authentication configuration
         */
        private final Curve curve = new Curve();

        /**
         * CURVE authentication configuration
         */
        @Data
        public static class Curve {
            /**
             * Whether this is a CURVE server
             */
            private boolean server = false;

            /**
             * Server public key (base64 or hex)
             */
            private String publicKey;

            /**
             * Server secret key (base64 or hex)
             */
            private String secretKey;

            /**
             * Client keys for authentication
             */
            private List<ClientKey> clientKeys = new ArrayList<>();

            /**
             * Client key configuration
             */
            @Data
            public static class ClientKey {
                /**
                 * Name of the client
                 */
                private String name;

                /**
                 * Client public key (base64 or hex)
                 */
                private String publicKey;
            }
        }
    }

    /**
     * Listener configuration
     */
    @Data
    public static class Listener {

        /**
         * Whether to enable consumer batching
         */
        private Boolean consumerBatchEnabled;
        /**
         * Number of threads for message listener containers
         */
        private int concurrency = 3;

        /**
         * Maximum number of messages to process in a batch
         */
        private int batchSize = 10;

        /**
         * Timeout in milliseconds for processing a batch
         */
        private long batchTimeout = 1000;

        /**
         * Connection retry timeout in milliseconds
         */
        private long retryTimeout = 30000;

        /**
         * How long to poll socket in ms before retry loop
         */
        private long socketPollTimeout = 500;

        /**
         * Low-level ZMQ receive buffer size
         */
        private int socketRecvBuffer = 1024;

        /**
         * High watermark to control flow
         */
        private int socketHwm = 1000;

        /**
         * Interval in ms for reconnection attempts
         */
        private long socketReconnectInterval = 5000;

        /**
         * Backoff time between failed polls
         */
        private long socketBackoff = 100;

        /**
         * Whether to acknowledge messages
         */
        private Boolean acknowledge;

        /**
         * Consumer configurations
         */
        private Consumer consumer = new Consumer();

        /**
         * Consumer configuration
         */
        @Data
        public static class Consumer {
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
             * List of addresses for the socket
             */
            private List<String> addresses = List.of("tcp://localhost:5555");

            /**
             * Topics to subscribe to (for SUB sockets)
             */
            private List<String> topics = new ArrayList<>();
        }
    }

    /**
     * Template configuration
     */
    @Data
    public static class Template {
        /**
         * Default socket to use
         */
        private String defaultSocket;

        /**
         * Timeout for send in ms
         */
        private long sendTimeout = 2000;

        /**
         * Low-level ZMQ send buffer size
         */
        private int socketSendBuffer = 1024;

        /**
         * High watermark for producer sockets
         */
        private int socketHwm = 1000;

        /**
         * Number of times to retry poll/send
         */
        private int pollRetry = 3;

        /**
         * Delay between retries
         */
        private long retryDelay = 100;

        /**
         * Whether to block or drop when HWM reached
         */
        private boolean backpressureEnabled = true;

        /**
         * Producer configurations
         */
        private Producer producer = new Producer();

        /**
         * Producer configuration
         */
        @Data
        public static class Producer {
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
            private boolean bind = false;

            /**
             * List of addresses for the socket
             */
            private List<String> addresses = List.of("tcp://localhost:5555");
        }
    }

    /**
     * Get the context thread count (for backward compatibility)
     */
    public Integer getContextThread() {
        return this.ioThreads;
    }
}