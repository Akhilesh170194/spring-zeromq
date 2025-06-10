package com.aoneconsultancy.zeromq.support.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

public interface ZmqListenerObservationConvention extends ObservationConvention<ZmqMessageReceiverContext> {

    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof ZmqMessageReceiverContext;
    }

    @Override
    default String getName() {
        return "spring.zmq.listener";
    }
}
