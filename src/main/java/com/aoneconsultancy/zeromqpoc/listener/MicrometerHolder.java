package com.aoneconsultancy.zeromqpoc.listener;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;
import io.micrometer.core.instrument.Timer.Sample;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * Abstraction to avoid hard reference to Micrometer.
 *
 */
public final class MicrometerHolder {

    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    private final MeterRegistry registry;

    private final Map<String, String> tags;

    private final String listenerId;

    MicrometerHolder(@Nullable ApplicationContext context, String listenerId, Map<String, String> tags) {
        if (context == null) {
            throw new IllegalStateException("No micrometer registry present");
        }
        try {
            this.registry = context.getBeanProvider(MeterRegistry.class).getIfUnique();
        } catch (NoUniqueBeanDefinitionException ex) {
            throw new IllegalStateException(ex);
        }
        if (this.registry != null) {
            this.listenerId = listenerId;
            this.tags = tags;
        } else {
            throw new IllegalStateException("No micrometer registry present (or more than one and "
                    + "there is not exactly one marked with @Primary)");
        }
    }

    public Object start() {
        return Timer.start(this.registry);
    }

    public void success(Object sample, String queue) {
        Timer timer = this.timers.get(queue + "none");
        if (timer == null) {
            timer = buildTimer(this.listenerId, "success", queue, "none");
        }
        ((Sample) sample).stop(timer);
    }

    public void failure(Object sample, String queue, String exception) {
        Timer timer = this.timers.get(queue + exception);
        if (timer == null) {
            timer = buildTimer(this.listenerId, "failure", queue, exception);
        }
        ((Sample) sample).stop(timer);
    }

    private Timer buildTimer(String aListenerId, String result, String socket, String exception) {

        Builder builder = Timer.builder("spring.rabbitmq.listener")
                .description("Spring RabbitMQ Listener")
                .tag("listener.id", aListenerId)
                .tag("socket", socket)
                .tag("result", result)
                .tag("exception", exception);
        if (this.tags != null && !this.tags.isEmpty()) {
            this.tags.forEach(builder::tag);
        }
        Timer registeredTimer = builder.register(this.registry);
        this.timers.put(socket + exception, registeredTimer);
        return registeredTimer;
    }

    void destroy() {
        this.timers.values().forEach(this.registry::remove);
        this.timers.clear();
    }

}
