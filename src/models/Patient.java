package models;

import java.io.Serializable;
import java.util.UUID;

/**
 * Patient model class representing a patient in the hospital system
 * Master IA - Systèmes Multi-Agents Project
 */
public class Patient implements Serializable {
    private String id;
    private String name;
    private int urgencyLevel; // 1 (low) to 5 (critical)
    private String treatmentType;
    private long arrivalTime;
    private long waitingTime;
    private String status; // WAITING, IN_TREATMENT, COMPLETED
    private String assignedDoctor;
    private String assignedRoom;
    private String requiredEquipment;

    public Patient(String name, int urgencyLevel, String treatmentType) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.urgencyLevel = urgencyLevel;
        this.treatmentType = treatmentType;
        this.arrivalTime = System.currentTimeMillis();
        this.status = "WAITING";
        this.waitingTime = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(int urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public String getTreatmentType() { return treatmentType; }
    public void setTreatmentType(String treatmentType) { this.treatmentType = treatmentType; }

    public long getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(long arrivalTime) { this.arrivalTime = arrivalTime; }

    public long getWaitingTime() { return waitingTime; }
    public void setWaitingTime(long waitingTime) { this.waitingTime = waitingTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(String assignedDoctor) { this.assignedDoctor = assignedDoctor; }

    public String getAssignedRoom() { return assignedRoom; }
    public void setAssignedRoom(String assignedRoom) { this.assignedRoom = assignedRoom; }

    public String getRequiredEquipment() { return requiredEquipment; }
    public void setRequiredEquipment(String requiredEquipment) { this.requiredEquipment = requiredEquipment; }

    public void updateWaitingTime() {
        if (status.equals("WAITING")) {
            this.waitingTime = System.currentTimeMillis() - arrivalTime;
        }
    }

    @Override
    public String toString() {
        return String.format("Patient[%s]: %s, Urgency: %d, Treatment: %s, Status: %s", 
            id.substring(0, 8), name, urgencyLevel, treatmentType, status);
    }
}
