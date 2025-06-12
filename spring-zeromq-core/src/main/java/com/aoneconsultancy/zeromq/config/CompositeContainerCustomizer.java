package com.aoneconsultancy.zeromq.config;

import com.aoneconsultancy.zeromq.listener.MessageListenerContainer;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class CompositeContainerCustomizer<C extends MessageListenerContainer> implements ContainerCustomizer<C> {

    private final List<ContainerCustomizer<C>> customizers = new ArrayList<>();

    /**
     * Create an instance with the provided delegate customizers.
     *
     * @param customizers the customizers.
     */
    public CompositeContainerCustomizer(List<ContainerCustomizer<C>> customizers) {
        Assert.notNull(customizers, "At least one customizer must be present");
        this.customizers.addAll(customizers);
    }

    @Override
    public void configure(C container) {
        this.customizers.forEach(c -> c.configure(container));
    }

}
