package utils;

import models.Patient;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Scheduling algorithm for patient prioritization
 * Master IA - Systèmes Multi-Agents Project
 */
public class SchedulingAlgorithm {
    
    // Priority queue for patients based on urgency and waiting time
    private PriorityQueue<Patient> patientQueue;
    
    public SchedulingAlgorithm() {
        // Custom comparator: Higher urgency first, then longer waiting time
        this.patientQueue = new PriorityQueue<>(new Comparator<Patient>() {
            @Override
            public int compare(Patient p1, Patient p2) {
                // First compare by urgency (higher urgency = higher priority)
                int urgencyCompare = Integer.compare(p2.getUrgencyLevel(), p1.getUrgencyLevel());
                if (urgencyCompare != 0) {
                    return urgencyCompare;
                }
                // If same urgency, prioritize by waiting time (longer wait = higher priority)
                return Long.compare(p1.getArrivalTime(), p2.getArrivalTime());
            }
        });
    }
    
    public void addPatient(Patient patient) {
        patientQueue.offer(patient);
    }
    
    public Patient getNextPatient() {
        return patientQueue.poll();
    }
    
    public boolean hasWaitingPatients() {
        return !patientQueue.isEmpty();
    }
    
    public int getQueueSize() {
        return patientQueue.size();
    }
    
    /**
     * Calculate priority score for resource allocation
     */
    public static double calculatePriorityScore(Patient patient) {
        long waitingTime = System.currentTimeMillis() - patient.getArrivalTime();
        double waitingMinutes = waitingTime / 60000.0;
        
        // Priority score formula: urgency * 10 + waiting_minutes
        return patient.getUrgencyLevel() * 10 + waitingMinutes;
    }
    
    /**
     * Estimate treatment duration based on treatment type
     */
    public static long estimateTreatmentDuration(String treatmentType) {
        switch (treatmentType.toUpperCase()) {
            case "CONSULTATION":
                return 15 * 60 * 1000; // 15 minutes
            case "EMERGENCY":
                return 30 * 60 * 1000; // 30 minutes
            case "SURGERY":
                return 120 * 60 * 1000; // 2 hours
            case "CHECKUP":
                return 20 * 60 * 1000; // 20 minutes
            case "XRAY":
                return 10 * 60 * 1000; // 10 minutes
            case "MRI":
                return 45 * 60 * 1000; // 45 minutes
            default:
                return 30 * 60 * 1000; // Default 30 minutes
        }
    }
    
    /**
     * Determine required equipment based on treatment type
     */
    public static String getRequiredEquipment(String treatmentType) {
        switch (treatmentType.toUpperCase()) {
            case "XRAY":
                return "XRAY";
            case "MRI":
                return "MRI";
            case "CT_SCAN":
                return "CT_SCAN";
            case "SURGERY":
                return "VENTILATOR";
            case "EMERGENCY":
                return "ECG";
            default:
                return null;
        }
    }
    
    /**
     * Match doctor specialization with treatment type
     */
    public static boolean isDoctorSuitable(String doctorSpecialization, String treatmentType) {
        if (doctorSpecialization.equals("GENERAL")) {
            return true; // General doctors can handle any non-specialized treatment
        }
        
        switch (treatmentType.toUpperCase()) {
            case "SURGERY":
                return doctorSpecialization.equals("SURGEON");
            case "EMERGENCY":
                return doctorSpecialization.equals("EMERGENCY");
            case "CARDIOLOGY":
                return doctorSpecialization.equals("CARDIOLOGIST");
            case "NEUROLOGY":
                return doctorSpecialization.equals("NEUROLOGIST");
            default:
                return true; // Accept any doctor for unspecified treatments
        }
    }
}
