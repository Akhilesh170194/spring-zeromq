package com.aoneconsultancy.zeromq.support.micrometer;

import com.aoneconsultancy.zeromq.core.message.Message;
import io.micrometer.observation.transport.ReceiverContext;
import lombok.Getter;

@Getter
public class ZmqMessageReceiverContext extends ReceiverContext<Message> {

    private final String listenerId;

    private final Message message;

    public ZmqMessageReceiverContext(Message message, String listenerId) {
        super((carrier, key) -> String.valueOf(carrier.getMessageProperty(key)));
        setCarrier(message);
        this.message = message;
        this.listenerId = listenerId;
        setRemoteServiceName("RabbitMQ");
    }

    /**
     * Return the source (queue) for this message.
     *
     * @return the source.
     */
    public String getSource() {
        return String.valueOf(this.message.getMessageProperties().get("socket"));
    }

}
