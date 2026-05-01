package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "catch-all" ExceptionMapper that intercepts any unexpected runtime
 * errors (e.g., NullPointerException, IndexOutOfBoundsException, etc.)
 * and returns a generic HTTP 500 Internal Server Error.
 * 
 * Security Purpose: This mapper ensures that raw Java stack traces are NEVER
 * exposed to external API consumers. Exposing stack traces is a significant
 * cybersecurity risk because:
 * - It reveals internal class names, package structure, and code organisation
 * - It discloses library/framework versions (enabling targeted CVE exploits)
 * - It can expose file system paths and server configuration details
 * - It provides attackers with a roadmap for crafting more sophisticated attacks
 * 
 * Instead, a sanitized, generic error message is returned to the client,
 * while the full stack trace is logged server-side for debugging purposes.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the full exception server-side for debugging and monitoring
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global safety net: "
                + exception.getClass().getSimpleName() + " - " + exception.getMessage(), exception);

        // Return a sanitized, generic error to the client — no stack trace leak
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("status", 500);
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message",
                "An unexpected error occurred while processing your request. " +
                "Please try again later or contact the system administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
