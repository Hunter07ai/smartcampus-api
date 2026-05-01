package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root "Discovery" resource for the SmartCampus API.
 * 
 * Provides essential metadata about the API including version information,
 * administrative contact details, and a navigable map of primary resource
 * collections. This follows the HATEOAS principle by allowing clients to
 * discover available resources programmatically.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource {

    /**
     * Discovery endpoint — returns API metadata and resource navigation links.
     * 
     * GET /api/v1
     *
     * @return JSON object with version, description, contact, and resource links
     */
    @GET
    public Response getApiInfo() {
        Map<String, Object> apiInfo = new LinkedHashMap<>();
        apiInfo.put("version", "1.0");
        apiInfo.put("title", "SmartCampus Sensor & Room Management API");
        apiInfo.put("description",
                "A RESTful API for managing campus rooms, IoT sensors, and sensor readings " +
                "for the university's SmartCampus initiative.");
        apiInfo.put("contact", "admin@smartcampus.university.ac.uk");

        // HATEOAS-style resource navigation map
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        apiInfo.put("resources", resources);

        return Response.ok(apiInfo).build();
    }
}
