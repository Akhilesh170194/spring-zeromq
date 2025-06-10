package com.aoneconsultancy.zeromq.listener;

import com.aoneconsultancy.zeromq.annotation.ZmqListener;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import java.util.Collection;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

/**
 * Default factory that creates {@link SimpleMessageListenerContainer} instances.
 */
@Slf4j
@ToString
public class PullZmqSocketListenerContainerFactory extends AbstractZmqListenerContainerFactory<SimpleMessageListenerContainer> {

    @Setter
    private MessageConverter messageConverter;

    @Setter
    private Collection<MessagePostProcessor> afterReceivePostProcessor;

    /**
     * Set the default socket type. For SimpleZmqListenerContainer, this must be PULL.
     *
     * @param defaultSocketType the default socket type
     */
    public void setSocketType(SocketType defaultSocketType) {
        if (defaultSocketType != SocketType.PULL) {
            throw new IllegalArgumentException("SimpleZmqListenerContainer only supports PULL sockets. " +
                    "For other socket types, extend AbstractZmqListenerContainer and implement the socket-specific logic.");
        }
    }

    public PullZmqSocketListenerContainerFactory(ZContext context) {
        super();
        this.context = context;
    }

    @Override
    protected SimpleMessageListenerContainer createContainerInstance() {
        return new SimpleMessageListenerContainer(context);
    }


    @Override
    public void initializeContainer(SimpleMessageListenerContainer instance, ZmqListenerEndpoint endpoint) {

        super.initializeContainer(instance, endpoint);

        instance.setSocketType(ZmqListener.SocketType.PULL);
        if (Boolean.TRUE.equals(this.consumerBatchEnabled)) {
            instance.setConsumerBatchEnabled(true);
        }

        // Set the addresses list if available
        if (this.addresses != null && !this.addresses.isEmpty()) {
            instance.setAddresses(this.addresses);
        }

    }

}
