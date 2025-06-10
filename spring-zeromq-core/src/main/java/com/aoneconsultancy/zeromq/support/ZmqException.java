package com.aoneconsultancy.zeromq.support;

public class ZmqException extends RuntimeException {

    public ZmqException(String message) {
        super(message);
    }

    public ZmqException(Throwable cause) {
        super(cause);
    }

    public ZmqException(String message, Throwable cause) {
        super(message, cause);
    }


}
