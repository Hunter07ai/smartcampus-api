package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JAX-RS ExceptionMapper for LinkedResourceNotFoundException.
 * 
 * Maps the exception to HTTP 422 Unprocessable Entity. This status code
 * is more semantically accurate than 404 because the JSON payload itself
 * is well-formed and syntactically valid — the issue is that a referenced
 * resource (e.g., a roomId) within the payload does not exist. The server
 * understands the content type and syntax but cannot process the request
 * due to semantic errors in the data.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("status", 422);
        errorResponse.put("error", "Unprocessable Entity");
        errorResponse.put("message", exception.getMessage());
        errorResponse.put("detail",
                "The request body contains a reference to a resource that does not exist. " +
                "Please verify that all linked resource IDs are valid before submitting.");

        return Response.status(422)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
