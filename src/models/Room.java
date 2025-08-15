package models;

import java.io.Serializable;

/**
 * Room model class
 * Master IA - Systèmes Multi-Agents Project
 */
public class Room implements Serializable {
    private String id;
    private String type; // CONSULTATION, SURGERY, EMERGENCY, ICU
    private boolean available;
    private String currentPatientId;
    private String currentDoctorId;
    private int capacity;

    public Room(String id, String type, int capacity) {
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.available = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getCurrentPatientId() { return currentPatientId; }
    public void setCurrentPatientId(String currentPatientId) { this.currentPatientId = currentPatientId; }

    public String getCurrentDoctorId() { return currentDoctorId; }
    public void setCurrentDoctorId(String currentDoctorId) { this.currentDoctorId = currentDoctorId; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public void occupy(String patientId, String doctorId) {
        this.available = false;
        this.currentPatientId = patientId;
        this.currentDoctorId = doctorId;
    }

    public void release() {
        this.available = true;
        this.currentPatientId = null;
        this.currentDoctorId = null;
    }

    @Override
    public String toString() {
        return String.format("Room[%s]: Type: %s, Available: %s, Capacity: %d", 
            id, type, available, capacity);
    }
}
