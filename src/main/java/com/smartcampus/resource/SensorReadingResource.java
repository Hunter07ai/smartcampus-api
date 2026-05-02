package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Sub-resource class for managing sensor readings.
 * 
 * This class is NOT annotated with @Path — it is instantiated by the
 * sub-resource locator method in SensorResource. Each instance is scoped
 * to a specific sensor via the sensorId passed in the constructor.
 * 
 * Endpoints (relative to /api/v1/sensors/{sensorId}/readings):
 * - GET /   → Fetch the historical reading log for this sensor
 * - POST /  → Append a new reading (with side effect on parent sensor)
 * 
 * Business Logic:
 * - POST is blocked if the sensor's status is "MAINTENANCE" (→ 403 Forbidden)
 * - A successful POST updates the parent Sensor's currentValue for data consistency
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore dataStore = DataStore.getInstance();

    /**
     * Constructor — receives the sensor ID from the sub-resource locator.
     *
     * @param sensorId the ID of the sensor whose readings are being managed
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * Retrieves the historical reading log for this sensor.
     * 
     * GET /api/v1/sensors/{sensorId}/readings
     *
     * @return 200 OK with JSON array of SensorReading objects, or 404 if sensor doesn't exist
     */
    @GET
    public Response getReadings() {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Sensor with ID '" + sensorId + "' not found.\"}")
                    .build();
        }

        List<SensorReading> readings = dataStore.getReadings(sensorId);
        return Response.ok(readings).build();
    }

    /**
     * Appends a new reading for this sensor.
     * 
     * POST /api/v1/sensors/{sensorId}/readings
     * Content-Type: application/json
     * 
     * State Constraint: If the sensor is currently in "MAINTENANCE" status, it is
     * physically disconnected and cannot accept new readings. In this case, a
     * SensorUnavailableException is thrown (→ 403 Forbidden).
     * 
     * Side Effect: A successful POST triggers an update to the parent Sensor's
     * currentValue field to ensure data consistency across the API.
     *
     * @param reading the SensorReading from the JSON request body
     * @return 201 Created with the reading in the body
     * @throws SensorUnavailableException if sensor is in MAINTENANCE mode (→ 403)
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        // Validation: Ensure the request body is not empty
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Request body is missing. Please provide a valid JSON reading object (e.g., {'value': 23.5}).\"}")
                    .build();
        }

        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Sensor with ID '" + sensorId + "' not found.\"}")
                    .build();
        }

        // State constraint: block readings for sensors in MAINTENANCE mode
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently in MAINTENANCE mode and " +
                    "cannot accept new readings. The sensor is physically disconnected.");
        }

        // Auto-generate ID and timestamp if not provided by the client
        if (reading.getId() == null || reading.getId().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Store the reading
        dataStore.addReading(sensorId, reading);

        // Side effect: update the parent sensor's currentValue for data consistency
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
