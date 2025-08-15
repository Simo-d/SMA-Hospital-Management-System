package utils;

/**
 * Message types for FIPA ACL communication
 * Master IA - Systèmes Multi-Agents Project
 */
public class MessageProtocol {
    // Conversation IDs
    public static final String TREATMENT_REQUEST = "treatment-request";
    public static final String RESOURCE_ALLOCATION = "resource-allocation";
    public static final String STATUS_UPDATE = "status-update";
    public static final String RESOURCE_QUERY = "resource-query";
    
    // Ontologies
    public static final String HOSPITAL_ONTOLOGY = "hospital-management";
    
    // Content keywords
    public static final String REQUEST_TREATMENT = "REQUEST_TREATMENT";
    public static final String ASSIGN_PATIENT = "ASSIGN_PATIENT";
    public static final String TREATMENT_COMPLETE = "TREATMENT_COMPLETE";
    public static final String RESOURCE_AVAILABLE = "RESOURCE_AVAILABLE";
    public static final String RESOURCE_BUSY = "RESOURCE_BUSY";
    public static final String CHECK_AVAILABILITY = "CHECK_AVAILABILITY";
    public static final String ALLOCATE_RESOURCE = "ALLOCATE_RESOURCE";
    public static final String RELEASE_RESOURCE = "RELEASE_RESOURCE";
    
    // Service types in Yellow Pages
    public static final String DOCTOR_SERVICE = "doctor-service";
    public static final String ROOM_SERVICE = "room-service";
    public static final String EQUIPMENT_SERVICE = "equipment-service";
    public static final String SCHEDULER_SERVICE = "scheduler-service";
    public static final String PATIENT_SERVICE = "patient-service";
}
