package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * JAX-RS resource class for managing the /api/v1/rooms collection.
 * 
 * Provides full CRUD operations for Room entities:
 * - GET /           → List all rooms
 * - POST /          → Create a new room
 * - GET /{roomId}   → Get a specific room by ID
 * - DELETE /{roomId} → Delete a room (with safety constraint)
 * 
 * Business Logic: A room cannot be deleted if it still has active sensors
 * assigned to it, to prevent orphaned sensor data.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore dataStore = DataStore.getInstance();

    /**
     * Retrieves a comprehensive list of all rooms on campus.
     * 
     * GET /api/v1/rooms
     *
     * @return 200 OK with JSON array of all Room objects
     */
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(dataStore.getRooms().values());
        return Response.ok(rooms).build();
    }

    /**
     * Creates a new room on campus.
     * 
     * POST /api/v1/rooms
     * 
     * The request body must contain a valid JSON representation of a Room.
     * On success, returns HTTP 201 Created with a Location header pointing
     * to the newly created resource.
     *
     * @param room the Room object from the JSON request body
     * @param uriInfo context for building the Location URI
     * @return 201 Created with Location header and the created Room in the body
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        dataStore.addRoom(room);

        // Build the URI for the newly created resource: /api/v1/rooms/{id}
        URI createdUri = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();

        return Response.created(createdUri).entity(room).build();
    }

    /**
     * Retrieves detailed metadata for a specific room.
     * 
     * GET /api/v1/rooms/{roomId}
     *
     * @param roomId the unique room identifier
     * @return 200 OK with the Room object, or 404 Not Found
     */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room with ID '" + roomId + "' not found.\"}")
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * Deletes (decommissions) a room from the campus system.
     * 
     * DELETE /api/v1/rooms/{roomId}
     * 
     * Business Logic Constraint: To prevent data orphans, a room cannot be
     * deleted if it still has active sensors assigned to it. If sensors exist,
     * a RoomNotEmptyException is thrown, which is mapped to HTTP 409 Conflict.
     * 
     * Idempotency: The first DELETE removes the room and returns 204 No Content.
     * Subsequent identical DELETE requests return 404 Not Found (the resource
     * is already gone). The operation is idempotent because the server-side
     * state after any number of identical calls is the same: the room does not exist.
     *
     * @param roomId the unique room identifier
     * @return 204 No Content on success, 404 if room doesn't exist
     * @throws RoomNotEmptyException if the room still has sensors (→ 409 Conflict)
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRoom(roomId);

        // If room doesn't exist, return 404 (idempotent behavior)
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room with ID '" + roomId + "' not found.\"}")
                    .build();
        }

        // Business logic: block deletion if room has sensors assigned
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Cannot delete room '" + roomId + "'. It currently has " +
                    room.getSensorIds().size() + " active sensor(s) assigned: " +
                    room.getSensorIds() + ". Please reassign or remove all sensors first.");
        }

        // Safe to delete — no sensors attached
        dataStore.removeRoom(roomId);
        return Response.noContent().build();
    }
}
