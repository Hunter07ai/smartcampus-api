package com.smartcampus.repository;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, singleton in-memory data store for the SmartCampus API.
 * 
 * Uses ConcurrentHashMap to ensure thread safety since JAX-RS resource classes
 * are instantiated per-request by default. All resource instances share this
 * single DataStore via the static getInstance() method.
 * 
 * No database is used — data is stored purely in Java collections as required
 * by the coursework specification.
 */
public class DataStore {

    /** Singleton instance — eagerly initialized for thread safety. */
    private static final DataStore INSTANCE = new DataStore();

    /** All rooms indexed by their unique ID. */
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    /** All sensors indexed by their unique ID. */
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    /** Sensor readings grouped by sensor ID. Each sensor has a list of historical readings. */
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    /**
     * Private constructor — seeds the data store with sample data for demonstration.
     */
    private DataStore() {
        seedSampleData();
    }

    /**
     * Returns the singleton DataStore instance.
     *
     * @return the shared DataStore
     */
    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ==================== Room Operations ====================

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public Room removeRoom(String id) {
        return rooms.remove(id);
    }

    // ==================== Sensor Operations ====================

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Initialize an empty readings list for the new sensor
        sensorReadings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public Sensor removeSensor(String id) {
        sensorReadings.remove(id);
        return sensors.remove(id);
    }

    // ==================== Sensor Reading Operations ====================

    public List<SensorReading> getReadings(String sensorId) {
        return sensorReadings.getOrDefault(sensorId, new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }

    // ==================== Sample Data Seeding ====================

    /**
     * Pre-populates the data store with sample rooms, sensors, and readings
     * so that the API can be demonstrated immediately upon startup.
     */
    private void seedSampleData() {
        // --- Rooms ---
        Room room1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room room2 = new Room("ENG-102", "Engineering Lab A", 30);
        Room room3 = new Room("SCI-201", "Science Lecture Hall", 120);

        // --- Sensors ---
        Sensor sensor1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor sensor2 = new Sensor("CO2-001", "CO2", "ACTIVE", 415.0, "LIB-301");
        Sensor sensor3 = new Sensor("OCC-001", "Occupancy", "ACTIVE", 28.0, "ENG-102");
        Sensor sensor4 = new Sensor("TEMP-002", "Temperature", "MAINTENANCE", 0.0, "SCI-201");

        // Link sensors to rooms
        room1.getSensorIds().add(sensor1.getId());
        room1.getSensorIds().add(sensor2.getId());
        room2.getSensorIds().add(sensor3.getId());
        room3.getSensorIds().add(sensor4.getId());

        // Store rooms
        addRoom(room1);
        addRoom(room2);
        addRoom(room3);

        // Store sensors
        addSensor(sensor1);
        addSensor(sensor2);
        addSensor(sensor3);
        addSensor(sensor4);

        // --- Sample Readings for TEMP-001 ---
        addReading("TEMP-001", new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis() - 3600000, 21.8));
        addReading("TEMP-001", new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis() - 1800000, 22.1));
        addReading("TEMP-001", new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis(), 22.5));

        // --- Sample Readings for CO2-001 ---
        addReading("CO2-001", new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis() - 900000, 410.0));
        addReading("CO2-001", new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis(), 415.0));
    }
}
