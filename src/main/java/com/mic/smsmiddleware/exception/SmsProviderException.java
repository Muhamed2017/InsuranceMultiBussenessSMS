package com.mic.smsmiddleware.exception;

public class SmsProviderException extends RuntimeException {

    public SmsProviderException(String message) {
        super(message);
    }

    public SmsProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
