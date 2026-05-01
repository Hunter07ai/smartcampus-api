package com.smartcampus.model;

/**
 * Represents a sensor device deployed in a SmartCampus room.
 * Each sensor has a type, status, current reading value, and is linked to a room.
 */
public class Sensor {

    /** Unique identifier, e.g., "TEMP-001" */
    private String id;

    /** Category, e.g., "Temperature", "Occupancy", "CO2" */
    private String type;

    /** Current state: "ACTIVE", "MAINTENANCE", or "OFFLINE" */
    private String status;

    /** The most recent measurement recorded */
    private double currentValue;

    /** Foreign key linking to the Room where the sensor is located */
    private String roomId;

    /** Default constructor required for JSON deserialization. */
    public Sensor() {
    }

    /**
     * Parameterized constructor for convenient sensor creation.
     *
     * @param id           unique sensor identifier
     * @param type         sensor category
     * @param status       current operational state
     * @param currentValue most recent reading
     * @param roomId       ID of the room this sensor is deployed in
     */
    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    // ==================== Getters & Setters ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
