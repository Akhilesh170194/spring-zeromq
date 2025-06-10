package com.aoneconsultancy.zeromq.support.converter;

import com.aoneconsultancy.zeromq.support.ZmqException;

public class MessageConversionException extends ZmqException {

    public MessageConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageConversionException(String message) {
        super(message);
    }

}
