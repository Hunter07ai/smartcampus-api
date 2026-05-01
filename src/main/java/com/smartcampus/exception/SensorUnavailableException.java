package com.smartcampus.exception;

/**
 * Custom exception thrown when a client attempts to interact with a sensor
 * that is currently in "MAINTENANCE" mode and physically disconnected.
 * 
 * This exception is mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper,
 * because the server understands the request but refuses to process it due
 * to the sensor's current operational state.
 */
public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}
