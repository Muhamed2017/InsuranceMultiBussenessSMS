package com.mic.smsmiddleware.exception;

public class InvalidMobileNumberException extends RuntimeException {

    public InvalidMobileNumberException(String rawPhone) {
        super("Invalid Egyptian mobile number: " + rawPhone);
    }
}
