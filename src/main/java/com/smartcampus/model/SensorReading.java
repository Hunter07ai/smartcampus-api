package com.smartcampus.model;

/**
 * Represents a single reading event captured by a sensor.
 * Each reading records the value and timestamp of a measurement.
 */
public class SensorReading {

    /** Unique reading event ID (UUID recommended) */
    private String id;

    /** Epoch time (milliseconds) when the reading was captured */
    private long timestamp;

    /** The actual metric value recorded by the hardware */
    private double value;

    /** Default constructor required for JSON deserialization. */
    public SensorReading() {
    }

    /**
     * Parameterized constructor for convenient reading creation.
     *
     * @param id        unique reading identifier
     * @param timestamp epoch time in milliseconds
     * @param value     the measured value
     */
    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // ==================== Getters & Setters ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
