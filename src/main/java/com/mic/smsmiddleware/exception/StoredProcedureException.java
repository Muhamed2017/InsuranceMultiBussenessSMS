package com.mic.smsmiddleware.exception;

public class StoredProcedureException extends RuntimeException {

    public StoredProcedureException(String procedureName, Throwable cause) {
        super("Failed to execute stored procedure: " + procedureName, cause);
    }
}
