package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application configuration class.
 * 
 * The @ApplicationPath annotation establishes the versioned base URI
 * for all API resources. All resource paths are relative to "/api/v1".
 * 
 * In this implementation, resource discovery is handled by the ResourceConfig
 * in Main.java via package scanning, so we do not need to manually register
 * classes here. This class serves as the formal JAX-RS Application declaration.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Resource discovery is handled by ResourceConfig package scanning in Main.java.
    // This class provides the formal @ApplicationPath declaration required by JAX-RS.
}
