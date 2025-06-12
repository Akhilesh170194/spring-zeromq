package com.aoneconsultancy.zeromq.core.monitoring;

import com.aoneconsultancy.zeromq.core.DefaultSocketEventListener;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot Actuator endpoint for exposing ZeroMQ socket statistics.
 */
@Endpoint(id = "zmq")
public class ZmqMonitoringEndpoint {

    private final DefaultSocketEventListener eventListener;

    /**
     * Create a new ZmqMonitoringEndpoint.
     *
     * @param eventListener the socket event listener
     */
    public ZmqMonitoringEndpoint(DefaultSocketEventListener eventListener) {
        this.eventListener = eventListener;
    }

    /**
     * Get statistics for all ZeroMQ sockets.
     *
     * @return a map of socket statistics
     */
    @ReadOperation
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();

        eventListener.getAllStats().forEach((socketName, stats) -> {
            Map<String, Object> socketStats = getStatsBySocketName(socketName);
            result.put(socketName, socketStats);
        });

        return result;
    }

    /**
     * Get statistics for a specific ZeroMQ socket.
     *
     * @param socketName the socket name
     * @return statistics for the socket, or null if not found
     */
    @ReadOperation
    public Map<String, Object> getStatsBySocketName(@Selector String socketName) {
        ZmqSocketStats stats = eventListener.getStats(socketName);
        if (stats == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("messagesReceived", stats.getMessagesReceived().get());
        result.put("messagesSent", stats.getMessagesSent().get());
        result.put("bytesReceived", stats.getBytesReceived().get());
        result.put("bytesSent", stats.getBytesSent().get());
        result.put("connectCount", stats.getConnectCount().get());
        result.put("disconnectCount", stats.getDisconnectCount().get());
        result.put("errorCount", stats.getErrorCount().get());
        result.put("connected", stats.isConnected());
        result.put("currentEndpoint", stats.getCurrentEndpoint());
        result.put("lastActivity", stats.getLastActivity() != null ? stats.getLastActivity().toString() : null);
        result.put("lastConnected", stats.getLastConnected() != null ? stats.getLastConnected().toString() : null);
        result.put("lastDisconnected", stats.getLastDisconnected() != null ? stats.getLastDisconnected().toString() : null);
        result.put("idleTimeMs", stats.getIdleTimeMs());

        return result;
    }
}
