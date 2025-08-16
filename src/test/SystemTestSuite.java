package test;

import ml.WaitTimePredictor;
import models.Patient;
import negotiation.NegotiationProtocol;
import fault.FaultToleranceManager;
import utils.SchedulingAlgorithm;

import java.util.*;

/**
 * Comprehensive Test Suite for Hospital System v2.0
 * Tests all major enhancements
 * Master IA - Systèmes Multi-Agents Project
 */
public class SystemTestSuite {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static List<String> failedTests = new ArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        Hospital System v2.0 - Comprehensive Test Suite     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // Run all test categories
        testMachineLearning();
        testNegotiationProtocol();
        testFaultTolerance();
        testLoadBalancing();
        testSchedulingAlgorithm();
        testIntegration();
        
        // Print results
        printTestResults();
    }
    
    /**
     * Test Machine Learning Components
     */
    private static void testMachineLearning() {
        printSection("MACHINE LEARNING TESTS");
        
        // Test 1: Predictor Initialization
        test("ML Predictor Initialization", () -> {
            WaitTimePredictor predictor = new WaitTimePredictor();
            return predictor != null;
        });
        
        // Test 2: Wait Time Prediction
        test("Wait Time Prediction", () -> {
            WaitTimePredictor predictor = new WaitTimePredictor();
            Patient patient = new Patient("TestPatient", 3, "CONSULTATION");
            double prediction = predictor.predictWaitTime(patient, 5, 0.7);
            return prediction > 0 && prediction < 100;
        });
        
        // Test 3: Online Learning
        test("Online Learning Update", () -> {
            WaitTimePredictor predictor = new WaitTimePredictor();
            Patient patient = new Patient("TestPatient", 3, "CONSULTATION");
            
            // Get initial prediction
            double initialPrediction = predictor.predictWaitTime(patient, 5, 0.7);
            
            // Update model with actual wait time
            predictor.updateModel(patient, 25.0, 5, 0.7);
            
            // Prediction should adapt
            double newPrediction = predictor.predictWaitTime(patient, 5, 0.7);
            return Math.abs(newPrediction - initialPrediction) > 0.01;
        });
        
        // Test 4: Confidence Intervals
        test("Confidence Interval Calculation", () -> {
            WaitTimePredictor predictor = new WaitTimePredictor();
            double prediction = 20.0;
            double[] interval = predictor.getConfidenceInterval(prediction);
            return interval[0] < prediction && interval[1] > prediction;
        });
        
        // Test 5: Model Metrics
        test("Model Metrics Calculation", () -> {
            WaitTimePredictor predictor = new WaitTimePredictor();
            Map<String, Double> metrics = predictor.getModelMetrics();
            return metrics.containsKey("mae") && 
                   metrics.containsKey("rmse") && 
                   metrics.containsKey("r2");
        });
    }
    
    /**
     * Test Negotiation Protocol
     */
    private static void testNegotiationProtocol() {
        printSection("NEGOTIATION PROTOCOL TESTS");
        
        // Test 6: Resource Request Creation
        test("Resource Request Creation", () -> {
            NegotiationProtocol.ResourceRequest request = 
                new NegotiationProtocol.ResourceRequest("DOCTOR", 4, 30000);
            return request.getResourceType().equals("DOCTOR") && 
                   request.getUrgencyLevel() == 4;
        });
        
        // Test 7: Bid Creation
        test("Bid Creation", () -> {
            NegotiationProtocol.ResourceRequest request = 
                new NegotiationProtocol.ResourceRequest("DOCTOR", 4, 30000);
            NegotiationProtocol.Bid bid = 
                new NegotiationProtocol.Bid(null, 85.5, request);
            return bid.getUtility() == 85.5;
        });
        
        // Test 8: Utility Function
        test("Doctor Utility Function", () -> {
            NegotiationProtocol.DoctorUtilityFunction utility = 
                new NegotiationProtocol.DoctorUtilityFunction("Surgery", true, 2);
            
            NegotiationProtocol.ResourceRequest request = 
                new NegotiationProtocol.ResourceRequest("DOCTOR", 5, 60000);
            request.addAttribute("specialization", "Surgery");
            
            double score = utility.calculateUtility(request);
            return score > 100; // Should be high due to specialization match and urgency
        });
        
        // Test 9: Bid Comparison
        test("Bid Comparison", () -> {
            NegotiationProtocol.ResourceRequest request = 
                new NegotiationProtocol.ResourceRequest("ROOM", 3, 30000);
            
            NegotiationProtocol.Bid bid1 = new NegotiationProtocol.Bid(null, 75.0, request);
            NegotiationProtocol.Bid bid2 = new NegotiationProtocol.Bid(null, 85.0, request);
            
            return bid2.compareTo(bid1) > 0;
        });
    }
    
    /**
     * Test Fault Tolerance
     */
    private static void testFaultTolerance() {
        printSection("FAULT TOLERANCE TESTS");
        
        // Test 10: Agent State Creation
        test("Agent State Persistence", () -> {
            FaultToleranceManager.AgentState state = 
                new FaultToleranceManager.AgentState(
                    "TestAgent", "DoctorAgent", 
                    new Object[]{"D1", "Dr. Test", "General"}
                );
            return state.getArguments().length == 3;
        });
        
        // Test 11: State Data Storage
        test("State Data Storage", () -> {
            FaultToleranceManager.AgentState state = 
                new FaultToleranceManager.AgentState(
                    "TestAgent", "DoctorAgent", new Object[]{}
                );
            state.addStateData("workload", 5);
            state.addStateData("available", true);
            
            return state.getStateData().size() == 2 && 
                   state.getStateData().get("workload").equals(5);
        });
    }
    
    /**
     * Test Load Balancing
     */
    private static void testLoadBalancing() {
        printSection("LOAD BALANCING TESTS");
        
        // Test 12: Load Variance Calculation
        test("Load Variance Calculation", () -> {
            List<Integer> loads = Arrays.asList(5, 5, 5, 5, 5);
            double variance = calculateVariance(loads);
            return variance == 0.0; // Perfect balance
        });
        
        // Test 13: Imbalanced Load Detection
        test("Imbalanced Load Detection", () -> {
            List<Integer> loads = Arrays.asList(10, 2, 8, 3, 12);
            double variance = calculateVariance(loads);
            return variance > 10.0; // High variance indicates imbalance
        });
        
        // Test 14: Load Balancing Decision
        test("Load Balancing Decision", () -> {
            int[] loads = {10, 3, 8, 2, 9};
            int minIndex = 0, maxIndex = 0;
            
            for (int i = 0; i < loads.length; i++) {
                if (loads[i] < loads[minIndex]) minIndex = i;
                if (loads[i] > loads[maxIndex]) maxIndex = i;
            }
            
            // Should migrate from maxIndex to minIndex
            return maxIndex == 0 && minIndex == 3;
        });
    }
    
    /**
     * Test Scheduling Algorithm
     */
    private static void testSchedulingAlgorithm() {
        printSection("SCHEDULING ALGORITHM TESTS");
        
        // Test 15: Priority Score Calculation
        test("Priority Score Calculation", () -> {
            Patient patient = new Patient("Test", 5, "EMERGENCY");
            double score = SchedulingAlgorithm.calculatePriorityScore(patient);
            return score > 40; // High urgency should give high score
        });
        
        // Test 16: Treatment Duration Estimation
        test("Treatment Duration Estimation", () -> {
            long consultDuration = SchedulingAlgorithm.estimateTreatmentDuration("CONSULTATION");
            long surgeryDuration = SchedulingAlgorithm.estimateTreatmentDuration("SURGERY");
            return surgeryDuration > consultDuration;
        });
        
        // Test 17: Equipment Requirement Detection
        test("Equipment Requirement Detection", () -> {
            String xrayEquipment = SchedulingAlgorithm.getRequiredEquipment("XRAY");
            String consultEquipment = SchedulingAlgorithm.getRequiredEquipment("CONSULTATION");
            return "XRAY".equals(xrayEquipment) && consultEquipment == null;
        });
        
        // Test 18: Doctor Suitability Check
        test("Doctor Suitability Check", () -> {
            boolean generalForConsult = SchedulingAlgorithm.isDoctorSuitable("GENERAL", "CONSULTATION");
            boolean surgeonForSurgery = SchedulingAlgorithm.isDoctorSuitable("SURGEON", "SURGERY");
            boolean generalForSurgery = SchedulingAlgorithm.isDoctorSuitable("GENERAL", "SURGERY");
            
            return generalForConsult && surgeonForSurgery && generalForSurgery;
        });
        
        // Test 19: Priority Queue Ordering
        test("Priority Queue Ordering", () -> {
            SchedulingAlgorithm scheduler = new SchedulingAlgorithm();
            
            Patient p1 = new Patient("Low", 1, "CHECKUP");
            Patient p2 = new Patient("High", 5, "EMERGENCY");
            Patient p3 = new Patient("Medium", 3, "CONSULTATION");
            
            scheduler.addPatient(p1);
            scheduler.addPatient(p2);
            scheduler.addPatient(p3);
            
            Patient next = scheduler.getNextPatient();
            return next.getName().equals("High"); // Highest urgency first
        });
    }
    
    /**
     * Integration Tests
     */
    private static void testIntegration() {
        printSection("INTEGRATION TESTS");
        
        // Test 20: ML + Scheduling Integration
        test("ML + Scheduling Integration", () -> {
            WaitTimePredictor predictor = new WaitTimePredictor();
            SchedulingAlgorithm scheduler = new SchedulingAlgorithm();
            
            Patient patient = new Patient("Integration", 4, "MRI");
            scheduler.addPatient(patient);
            
            double prediction = predictor.predictWaitTime(patient, scheduler.getQueueSize(), 0.6);
            return prediction > 0 && scheduler.hasWaitingPatients();
        });
        
        // Test 21: Negotiation + Priority Integration
        test("Negotiation + Priority Integration", () -> {
            NegotiationProtocol.ResourceRequest request = 
                new NegotiationProtocol.ResourceRequest("DOCTOR", 5, 30000);
            
            NegotiationProtocol.DoctorUtilityFunction utility1 = 
                new NegotiationProtocol.DoctorUtilityFunction("General", true, 1);
            NegotiationProtocol.DoctorUtilityFunction utility2 = 
                new NegotiationProtocol.DoctorUtilityFunction("General", true, 4);
            
            double score1 = utility1.calculateUtility(request);
            double score2 = utility2.calculateUtility(request);
            
            // Less busy doctor should have higher utility
            return score1 > score2;
        });
        
        // Test 22: System Stress Test
        test("System Stress Test", () -> {
            SchedulingAlgorithm scheduler = new SchedulingAlgorithm();
            WaitTimePredictor predictor = new WaitTimePredictor();
            
            // Add 100 patients
            for (int i = 0; i < 100; i++) {
                Patient p = new Patient("Patient" + i, (i % 5) + 1, "CONSULTATION");
                scheduler.addPatient(p);
            }
            
            // Process all patients
            int processed = 0;
            while (scheduler.hasWaitingPatients() && processed < 100) {
                Patient p = scheduler.getNextPatient();
                if (p != null) {
                    processed++;
                    double waitTime = 10 + Math.random() * 30;
                    predictor.updateModel(p, waitTime, processed, 0.5);
                }
            }
            
            return processed == 100;
        });
    }
    
    /**
     * Helper Methods
     */
    
    private static void test(String testName, TestCase testCase) {
        try {
            boolean result = testCase.run();
            if (result) {
                System.out.println("  ✅ " + testName);
                testsPassed++;
            } else {
                System.out.println("  ❌ " + testName);
                testsFailed++;
                failedTests.add(testName);
            }
        } catch (Exception e) {
            System.out.println("  ❌ " + testName + " (Exception: " + e.getMessage() + ")");
            testsFailed++;
            failedTests.add(testName + " - " + e.getMessage());
        }
    }
    
    private static void printSection(String section) {
        System.out.println("\n" + section);
        System.out.println("─".repeat(section.length()));
    }
    
    private static double calculateVariance(List<Integer> values) {
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        return values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
    }
    
    private static void printTestResults() {
        System.out.println("\n════════════════════════════════════════════════════════════");
        System.out.println("                     TEST RESULTS SUMMARY");
        System.out.println("════════════════════════════════════════════════════════════");
        
        int total = testsPassed + testsFailed;
        double percentage = total > 0 ? (testsPassed * 100.0 / total) : 0;
        
        System.out.println("  Total Tests: " + total);
        System.out.println("  Passed: " + testsPassed + " ✅");
        System.out.println("  Failed: " + testsFailed + " ❌");
        System.out.println("  Success Rate: " + String.format("%.1f%%", percentage));
        
        if (!failedTests.isEmpty()) {
            System.out.println("\n  Failed Tests:");
            for (String test : failedTests) {
                System.out.println("    • " + test);
            }
        }
        
        System.out.println("\n  Grade: " + getGrade(percentage));
        System.out.println("════════════════════════════════════════════════════════════\n");
    }
    
    private static String getGrade(double percentage) {
        if (percentage >= 95) return "A+ (Excellent)";
        if (percentage >= 90) return "A (Very Good)";
        if (percentage >= 85) return "B+ (Good)";
        if (percentage >= 80) return "B (Satisfactory)";
        if (percentage >= 75) return "C+ (Acceptable)";
        if (percentage >= 70) return "C (Pass)";
        return "F (Needs Improvement)";
    }
    
    @FunctionalInterface
    private interface TestCase {
        boolean run() throws Exception;
    }
}
