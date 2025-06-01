package com.aoneconsultancy.zeromqpoc.support.converter;

import com.aoneconsultancy.zeromqpoc.support.ZmqException;

public class MessageConversionException extends ZmqException {

    public MessageConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageConversionException(String message) {
        super(message);
    }

}
