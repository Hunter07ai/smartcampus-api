package com.smartcampus.exception;

/**
 * Custom exception thrown when a request references a linked resource
 * (e.g., a roomId in a Sensor POST) that does not exist in the system.
 * 
 * This exception is mapped to HTTP 422 Unprocessable Entity by
 * LinkedResourceNotFoundExceptionMapper, because the JSON payload itself
 * is syntactically valid but contains a semantically invalid reference.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
