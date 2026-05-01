package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JAX-RS ExceptionMapper for RoomNotEmptyException.
 * 
 * Maps the exception to HTTP 409 Conflict with a JSON body explaining
 * that the room is currently occupied by active hardware and cannot be
 * decommissioned until all sensors are reassigned or removed.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("status", 409);
        errorResponse.put("error", "Conflict");
        errorResponse.put("message", exception.getMessage());
        errorResponse.put("detail",
                "The room is currently occupied by active hardware. " +
                "Please reassign or remove all sensors before attempting to delete this room.");

        return Response.status(Response.Status.CONFLICT)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
