package com.aoneconsultancy.zeromqpoc.listener;

import com.aoneconsultancy.zeromqpoc.annotation.ZmqListener;
import com.aoneconsultancy.zeromqpoc.config.ZmqProperties;
import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.listener.endpoint.ZmqListenerEndpoint;
import com.aoneconsultancy.zeromqpoc.support.postprocessor.MessagePostProcessor;
import java.util.Collection;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.ZContext;

/**
 * Default factory that creates {@link SimpleMessageListenerContainer} instances.
 */
@Slf4j
@ToString
public class SimpleZmqListenerContainerFactory extends AbstractZmqListenerContainerFactory<SimpleMessageListenerContainer> {

    private final ZContext context;
    private final ZmqProperties properties;

    @Setter
    private MessageConverter messageConverter;

    @Setter
    private Collection<MessagePostProcessor> afterReceivePostProcessor;

    @Setter
    private String defaultAddress;

    /**
     * Set the default socket type. For SimpleZmqListenerContainer, this must be PULL.
     *
     * @param defaultSocketType the default socket type
     */
    public void setSocketType(ZmqListener.SocketType defaultSocketType) {
        if (defaultSocketType != ZmqListener.SocketType.PULL) {
            throw new IllegalArgumentException("SimpleZmqListenerContainer only supports PULL sockets. " +
                    "For other socket types, extend AbstractZmqListenerContainer and implement the socket-specific logic.");
        }
    }

    public SimpleZmqListenerContainerFactory(ZContext context, ZmqProperties properties) {
        super();
        this.context = context;
        this.properties = properties;
        setConcurrency(properties.getListenerConcurrency());
        setBufferSize(properties.getBufferSize());
    }

    @Override
    protected SimpleMessageListenerContainer createContainerInstance() {
        return new SimpleMessageListenerContainer(context);
    }


    @Override
    public void initializeContainer(SimpleMessageListenerContainer instance, ZmqListenerEndpoint endpoint) {

        super.initializeContainer(instance, endpoint);

        instance.setSocketType(ZmqListener.SocketType.PULL);
        instance.setBufferSize(bufferSize);
        instance.setBatchSize(batchSize);
        if (Boolean.TRUE.equals(this.consumerBatchEnabled)) {
            instance.setConsumerBatchEnabled(true);
        }

    }

}
