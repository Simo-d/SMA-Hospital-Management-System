package models;

import java.io.Serializable;

/**
 * Doctor model class
 * Master IA - Systèmes Multi-Agents Project
 */
public class Doctor implements Serializable {
    private String id;
    private String name;
    private String specialization;
    private boolean available;
    private String currentPatientId;
    private int patientsServed;
    private long totalServiceTime;

    public Doctor(String id, String name, String specialization) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.available = true;
        this.patientsServed = 0;
        this.totalServiceTime = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getCurrentPatientId() { return currentPatientId; }
    public void setCurrentPatientId(String currentPatientId) { this.currentPatientId = currentPatientId; }

    public int getPatientsServed() { return patientsServed; }
    public void incrementPatientsServed() { this.patientsServed++; }

    public long getTotalServiceTime() { return totalServiceTime; }
    public void addServiceTime(long time) { this.totalServiceTime += time; }

    public double getAverageServiceTime() {
        return patientsServed > 0 ? (double) totalServiceTime / patientsServed : 0;
    }

    @Override
    public String toString() {
        return String.format("Doctor[%s]: %s, Spec: %s, Available: %s, Served: %d", 
            id, name, specialization, available, patientsServed);
    }
}
