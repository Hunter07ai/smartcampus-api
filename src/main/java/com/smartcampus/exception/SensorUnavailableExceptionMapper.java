package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JAX-RS ExceptionMapper for SensorUnavailableException.
 * 
 * Maps the exception to HTTP 403 Forbidden. This status code indicates
 * that the server understood the request but refuses to authorize it.
 * In this context, a sensor in "MAINTENANCE" status is physically
 * disconnected and the server refuses to accept new readings for it.
 */
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("status", 403);
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", exception.getMessage());
        errorResponse.put("detail",
                "The sensor is currently in MAINTENANCE mode and is physically disconnected. " +
                "It cannot accept new readings until it is restored to ACTIVE status.");

        return Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
