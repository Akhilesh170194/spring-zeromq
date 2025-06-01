package com.aoneconsultancy.zeromqpoc.config;

import com.aoneconsultancy.zeromqpoc.listener.MessageListenerContainer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.Assert;

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
