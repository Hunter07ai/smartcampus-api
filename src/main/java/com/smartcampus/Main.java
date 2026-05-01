package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the SmartCampus API server.
 * Bootstraps a Grizzly HTTP server with the JAX-RS application configuration.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /** Base URI where the Grizzly HTTP server will listen. */
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    /**
     * Creates and configures the Grizzly HTTP server with all JAX-RS resources,
     * exception mappers, and filters registered via package scanning.
     *
     * @return a configured HttpServer instance (not yet started)
     */
    public static HttpServer createServer() {
        // ResourceConfig scans the entire com.smartcampus package tree
        // to auto-discover @Path resources, @Provider exception mappers, and filters.
        final ResourceConfig config = new ResourceConfig()
                .packages("com.smartcampus");

        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    /**
     * Main method — starts the server and waits for user input to shut down.
     */
    public static void main(String[] args) {
        try {
            final HttpServer server = createServer();
            LOGGER.log(Level.INFO, "==============================================");
            LOGGER.log(Level.INFO, " SmartCampus API is RUNNING");
            LOGGER.log(Level.INFO, " Base URI : {0}", BASE_URI);
            LOGGER.log(Level.INFO, " API Root : {0}", BASE_URI);
            LOGGER.log(Level.INFO, "==============================================");
            LOGGER.log(Level.INFO, "Press ENTER to stop the server...");

            // Keep the server alive until the user presses Enter
            System.in.read();
            server.shutdownNow();
            LOGGER.log(Level.INFO, "Server stopped.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start server", e);
        }
    }
}
