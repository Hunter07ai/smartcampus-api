package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JAX-RS resource class for managing the /api/v1/sensors collection.
 * 
 * Provides CRUD operations for Sensor entities with integrity validation:
 * - GET /                      → List all sensors (with optional type filter)
 * - POST /                     → Register a new sensor (validates roomId exists)
 * - GET /{sensorId}            → Get a specific sensor
 * - {sensorId}/readings        → Sub-resource locator delegating to SensorReadingResource
 * 
 * Data Integrity: When creating a sensor, the specified roomId must exist.
 * If it doesn't, a LinkedResourceNotFoundException is thrown (→ 422).
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore dataStore = DataStore.getInstance();

    /**
     * Retrieves all sensors, with optional filtering by sensor type.
     * 
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     * 
     * If the 'type' query parameter is provided, only sensors matching that
     * type are returned. The comparison is case-insensitive for flexibility.
     *
     * @param type optional query parameter to filter sensors by type
     * @return 200 OK with JSON array of matching Sensor objects
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(dataStore.getSensors().values());

        // Apply optional type filter if provided
        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    /**
     * Registers a new sensor on the SmartCampus.
     * 
     * POST /api/v1/sensors
     * Content-Type: application/json
     * 
     * Validation: The roomId specified in the request body must correspond to
     * an existing room. If the room does not exist, a LinkedResourceNotFoundException
     * is thrown, which is mapped to HTTP 422 Unprocessable Entity.
     * 
     * Side Effect: On successful creation, the sensor's ID is added to the
     * parent Room's sensorIds list to maintain bidirectional linkage.
     *
     * @param sensor the Sensor object from the JSON request body
     * @param uriInfo context for building the Location URI
     * @return 201 Created with the sensor in the body
     * @throws LinkedResourceNotFoundException if roomId doesn't exist (→ 422)
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        // Validate that the referenced room exists
        Room room = dataStore.getRoom(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor. The specified roomId '" + sensor.getRoomId() +
                    "' does not exist in the system. Please provide a valid room ID.");
        }

        // Store the sensor
        dataStore.addSensor(sensor);

        // Link the sensor to its parent room
        room.getSensorIds().add(sensor.getId());

        // Build the Location URI for the new resource
        URI createdUri = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();

        return Response.created(createdUri).entity(sensor).build();
    }

    /**
     * Retrieves a specific sensor by its ID.
     * 
     * GET /api/v1/sensors/{sensorId}
     *
     * @param sensorId the unique sensor identifier
     * @return 200 OK with the Sensor object, or 404 Not Found
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Sensor with ID '" + sensorId + "' not found.\"}")
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * Sub-Resource Locator — delegates requests for a sensor's readings to
     * the dedicated SensorReadingResource class.
     * 
     * This pattern is used to manage complexity: rather than handling all nested
     * paths (sensors/{id}/readings, sensors/{id}/readings/{rid}) in this one class,
     * we delegate to a focused sub-resource class that handles reading operations.
     * 
     * Path: /api/v1/sensors/{sensorId}/readings → SensorReadingResource
     *
     * @param sensorId the sensor whose readings are being accessed
     * @return a new SensorReadingResource instance scoped to this sensor
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
