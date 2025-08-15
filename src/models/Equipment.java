package models;

import java.io.Serializable;

/**
 * Equipment model class
 * Master IA - Systèmes Multi-Agents Project
 */
public class Equipment implements Serializable {
    private String id;
    private String type; // MRI, CT_SCAN, XRAY, VENTILATOR, ECG
    private boolean available;
    private String currentPatientId;
    private int usageCount;

    public Equipment(String id, String type) {
        this.id = id;
        this.type = type;
        this.available = true;
        this.usageCount = 0;
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

    public int getUsageCount() { return usageCount; }
    public void incrementUsageCount() { this.usageCount++; }

    public void allocate(String patientId) {
        this.available = false;
        this.currentPatientId = patientId;
        this.usageCount++;
    }

    public void release() {
        this.available = true;
        this.currentPatientId = null;
    }

    @Override
    public String toString() {
        return String.format("Equipment[%s]: Type: %s, Available: %s, Usage: %d", 
            id, type, available, usageCount);
    }
}
