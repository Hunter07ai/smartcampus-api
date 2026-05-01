package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JAX-RS filter implementing API observability through request and response logging.
 * 
 * Implements both ContainerRequestFilter and ContainerResponseFilter to provide
 * comprehensive logging of all API traffic as a cross-cutting concern.
 * 
 * Advantages of using JAX-RS filters over manual Logger.info() statements:
 * - DRY Principle: Logging logic is defined once, not scattered across every method
 * - Consistency: All endpoints are logged uniformly without risk of omission
 * - Separation of Concerns: Business logic remains clean and focused
 * - Maintainability: Logging format or detail level can be changed in one place
 * - Extensibility: Easy to add metrics, tracing, or audit logging without touching resources
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Intercepts every incoming HTTP request and logs the HTTP method and URI.
     * 
     * This filter runs before the resource method is invoked.
     *
     * @param requestContext the context of the incoming request
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();

        LOGGER.info(">> Incoming Request: " + method + " " + uri);
    }

    /**
     * Intercepts every outgoing HTTP response and logs the final HTTP status code.
     * 
     * This filter runs after the resource method has completed execution.
     *
     * @param requestContext  the context of the original request
     * @param responseContext the context of the outgoing response
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        int status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();

        LOGGER.info("<< Outgoing Response: " + method + " " + uri + " -> HTTP " + status);
    }
}
