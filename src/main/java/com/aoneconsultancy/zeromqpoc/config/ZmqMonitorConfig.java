package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.core.DefaultSocketEventListener;
import com.aoneconsultancy.zeromqpoc.core.ZmqSocketMonitor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ZeroMQ socket monitoring.
 */
@Configuration
public class ZmqMonitorConfig {

    /**
     * Create a default socket event listener if none is defined.
     *
     * @param eventPublisher the Spring application event publisher
     * @return a default socket event listener
     */
    @Bean
    @ConditionalOnMissingBean(ZmqSocketMonitor.SocketEventListener.class)
    public ZmqSocketMonitor.SocketEventListener socketEventListener(ApplicationEventPublisher eventPublisher) {
        return new DefaultSocketEventListener(eventPublisher);
    }
}
