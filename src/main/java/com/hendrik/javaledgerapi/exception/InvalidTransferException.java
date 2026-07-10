package com.hendrik.javaledgerapi.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransferException extends ApiException {
    public InvalidTransferException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
