package com.aoneconsultancy.zeromqpoc.core;

import com.aoneconsultancy.zeromqpoc.core.monitoring.ZmqSocketStats;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Default implementation of a socket event listener that logs events and publishes them as Spring events.
 */
@Slf4j
public class DefaultSocketEventListener implements ZmqSocketMonitor.SocketEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final ConcurrentMap<String, ZmqSocketStats> socketStats = new ConcurrentHashMap<>();

    /**
     * Create a new DefaultSocketEventListener.
     *
     * @param eventPublisher the Spring ApplicationEventPublisher to publish events to
     */
    public DefaultSocketEventListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get statistics for the specified socket.
     * If statistics for the socket don't exist yet, they will be created.
     *
     * @param socketId the socket ID
     * @return the socket statistics
     */
    public ZmqSocketStats getStats(String socketId) {
        return socketStats.computeIfAbsent(socketId, ZmqSocketStats::new);
    }

    /**
     * Get statistics for all sockets.
     *
     * @return a map of socket ID to socket statistics
     */
    public ConcurrentMap<String, ZmqSocketStats> getAllStats() {
        return socketStats;
    }

    @Override
    public void onEvent(String socketId, int eventType, int eventValue, String address) {
        // Get or create stats for this socket
        ZmqSocketStats stats = getStats(socketId);

        // Update statistics based on event type
        switch (eventType) {
            case ZmqSocketMonitor.EVENT_CONNECTED:
                log.info("ZeroMQ socket {} connected to {}", socketId, address);
                stats.recordConnected(address);
                break;
            case ZmqSocketMonitor.EVENT_CONNECT_DELAYED:
                log.info("ZeroMQ socket {} connection to {} delayed", socketId, address);
                stats.recordError();
                break;
            case ZmqSocketMonitor.EVENT_CONNECT_RETRIED:
                log.info("ZeroMQ socket {} retrying connection to {}, retry interval: {} ms",
                        socketId, address, eventValue);
                break;
            case ZmqSocketMonitor.EVENT_LISTENING:
                log.info("ZeroMQ socket {} listening on {}", socketId, address);
                break;
            case ZmqSocketMonitor.EVENT_BIND_FAILED:
                log.error("ZeroMQ socket {} failed to bind to {}", socketId, address);
                stats.recordError();
                break;
            case ZmqSocketMonitor.EVENT_ACCEPTED:
                log.debug("ZeroMQ socket {} accepted connection from {}", socketId, address);
                break;
            case ZmqSocketMonitor.EVENT_ACCEPT_FAILED:
                log.error("ZeroMQ socket {} failed to accept connection", socketId);
                stats.recordError();
                break;
            case ZmqSocketMonitor.EVENT_CLOSED:
                log.info("ZeroMQ socket {} closed connection to {}", socketId, address);
                break;
            case ZmqSocketMonitor.EVENT_CLOSE_FAILED:
                log.error("ZeroMQ socket {} failed to close connection to {}", socketId, address);
                stats.recordError();
                break;
            case ZmqSocketMonitor.EVENT_DISCONNECTED:
                log.info("ZeroMQ socket {} disconnected from {}", socketId, address);
                stats.recordDisconnected();
                break;
            case ZmqSocketMonitor.EVENT_MONITOR_STOPPED:
                log.info("ZeroMQ socket {} monitor stopped", socketId);
                break;
            default:
                log.debug("ZeroMQ socket {} unknown event: {}, value: {}, address: {}",
                        socketId, eventType, eventValue, address);
        }

        // Publish a Spring application event if possible
        if (eventPublisher != null) {
            try {
                eventPublisher.publishEvent(new ZmqSocketEvent(socketId, eventType, eventValue, address));
            } catch (Exception e) {
                log.warn("Failed to publish ZeroMQ socket event: {}", e.getMessage());
            }
        }
    }
}
