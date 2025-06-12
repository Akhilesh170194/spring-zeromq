package com.aoneconsultancy.zeromq.listener;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.ZContext;

import java.util.Collection;

/**
 * Default factory that creates {@link PullZmqMessageListenerContainer} instances.
 */
@Slf4j
@ToString
public class PullZmqSocketListenerContainerFactory extends AbstractZmqListenerContainerFactory<PullZmqMessageListenerContainer> {

    @Setter
    private MessageConverter messageConverter;

    @Setter
    private Collection<MessagePostProcessor> afterReceivePostProcessor;

    public PullZmqSocketListenerContainerFactory(ZContext context) {
        super();
        this.context = context;
    }

    @Override
    protected PullZmqMessageListenerContainer createContainerInstance() {
        return new PullZmqMessageListenerContainer(context);
    }


    @Override
    public void initializeContainer(PullZmqMessageListenerContainer instance, ZmqListenerEndpoint endpoint) {

        super.initializeContainer(instance, endpoint);

        if (Boolean.TRUE.equals(this.consumerBatchEnabled)) {
            instance.setConsumerBatchEnabled(true);
        }
        // Set the listener consumer props
        if (endpoint.getZmqConsumerProps() != null) {
            instance.setZmqConsumerProps(endpoint.getZmqConsumerProps());
        }
    }

}
