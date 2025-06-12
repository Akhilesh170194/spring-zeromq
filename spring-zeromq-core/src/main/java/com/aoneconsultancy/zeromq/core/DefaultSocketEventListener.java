package com.aoneconsultancy.zeromq.core;

import com.aoneconsultancy.zeromq.core.monitoring.ZmqSocketStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
     * @param socketName the socket name
     * @return the socket statistics
     */
    public ZmqSocketStats getStats(String socketName) {
        return socketStats.computeIfAbsent(socketName, ZmqSocketStats::new);
    }

    /**
     * Get statistics for all sockets.
     *
     * @return a map of socket name to socket statistics
     */
    public ConcurrentMap<String, ZmqSocketStats> getAllStats() {
        return socketStats;
    }

    @Override
    public void onEvent(String socketName, int eventType, int eventValue, String address) {
        // Get or create stats for this socket
        ZmqSocketStats stats = getStats(socketName);

        // Update statistics based on event type
        switch (eventType) {
            case ZmqSocketMonitor.EVENT_CONNECTED:
                log.info("ZeroMQ socket {} connected to {}", socketName, address);
                stats.recordConnected(address);
                break;
            case ZmqSocketMonitor.EVENT_CONNECT_DELAYED:
                log.info("ZeroMQ socket {} connection to {} delayed", socketName, address);
                stats.recordError();
                break;
            case ZmqSocketMonitor.EVENT_CONNECT_RETRIED:
                log.info("ZeroMQ socket {} retrying connection to {}, retry interval: {} ms",
                        socketName, address, eventValue);
                break;
            case ZmqSocketMonitor.EVENT_LISTENING:
                log.info("ZeroMQ socket {} listening on {}", socketName, address);
                break;
            case ZmqSocketMonitor.EVENT_BIND_FAILED:
                log.error("ZeroMQ socket {} failed to bind to {}", socketName, address);
                stats.recordError();
                break;
            case ZmqSocketMonitor.EVENT_ACCEPTED:
                log.debug("ZeroMQ socket {} accepted connection from {}", socketName, address);
                break;
            case ZmqSocketMonitor.EVENT_ACCEPT_FAILED:
                log.error("ZeroMQ socket {} failed to accept connection", socketName);
                stats.recordError();
                break;
            case ZmqSocketMonitor.EVENT_CLOSED:
                log.info("ZeroMQ socket {} closed connection to {}", socketName, address);
                break;
            case ZmqSocketMonitor.EVENT_CLOSE_FAILED:
                log.error("ZeroMQ socket {} failed to close connection to {}", socketName, address);
                stats.recordError();
                break;
            case ZmqSocketMonitor.EVENT_DISCONNECTED:
                log.info("ZeroMQ socket {} disconnected from {}", socketName, address);
                stats.recordDisconnected();
                break;
            case ZmqSocketMonitor.EVENT_MONITOR_STOPPED:
                log.info("ZeroMQ socket {} monitor stopped", socketName);
                break;
            default:
                log.debug("ZeroMQ socket {} unknown event: {}, value: {}, address: {}",
                        socketName, eventType, eventValue, address);
        }

        // Publish a Spring application event if possible
        if (eventPublisher != null) {
            try {
                eventPublisher.publishEvent(new ZmqSocketEvent(socketName, eventType, eventValue, address));
            } catch (Exception e) {
                log.warn("Failed to publish ZeroMQ socket event: {}", e.getMessage());
            }
        }
    }
}
