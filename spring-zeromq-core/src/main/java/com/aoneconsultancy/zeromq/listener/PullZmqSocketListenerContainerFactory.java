package com.aoneconsultancy.zeromq.listener;

import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint;
import com.aoneconsultancy.zeromq.support.postprocessor.MessagePostProcessor;
import java.util.Collection;
import java.util.List;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

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

    @Setter
    private String id;

    @Setter
    private boolean bind;

    @Setter
    private List<String> endpoints;

    private SocketType socketType = SocketType.PULL;

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
        this.socketType = defaultSocketType;
    }

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

        instance.setSocketType(SocketType.PULL);
        if (Boolean.TRUE.equals(this.consumerBatchEnabled)) {
            instance.setConsumerBatchEnabled(true);
        }
        // Set the addresses list if available
        if (this.endpoints != null && !this.endpoints.isEmpty()) {
            instance.setEndpoints(this.endpoints);
        }
        instance.setBind(this.bind);
        instance.setSocketType(this.socketType);
        instance.setListenerId(this.id);
    }

}
