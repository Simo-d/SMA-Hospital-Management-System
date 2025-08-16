package ml;

import java.util.*;
import models.Patient;

/**
 * Machine Learning component for predicting patient wait times
 * Uses a simple linear regression model with feature engineering
 * Master IA - Systèmes Multi-Agents Project
 */
public class WaitTimePredictor {
    
    // Historical data for training
    private List<WaitTimeRecord> historicalData;
    
    // Model parameters (learned through training)
    private double baseWaitTime = 15.0; // Base wait time in minutes
    private double urgencyWeight = -3.0; // Higher urgency reduces wait
    private double queueSizeWeight = 5.0; // Each patient in queue adds time
    private double timeOfDayWeight = 0.5; // Peak hours adjustment
    private double resourceAvailabilityWeight = -10.0; // More resources reduce wait
    
    // Learning rate for online learning
    private final double LEARNING_RATE = 0.01;
    
    public WaitTimePredictor() {
        this.historicalData = new ArrayList<>();
        initializeModel();
    }
    
    /**
     * Initialize model with some synthetic training data
     */
    private void initializeModel() {
        // Generate synthetic training data based on domain knowledge
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int urgency = random.nextInt(5) + 1;
            int queueSize = random.nextInt(10);
            int hourOfDay = random.nextInt(24);
            double resourceAvailability = random.nextDouble();
            
            // Simulate realistic wait times
            double waitTime = calculateSyntheticWaitTime(urgency, queueSize, hourOfDay, resourceAvailability);
            
            historicalData.add(new WaitTimeRecord(urgency, queueSize, hourOfDay, 
                                                  resourceAvailability, waitTime));
        }
        
        // Train initial model
        trainModel();
    }
    
    /**
     * Calculate synthetic wait time for training data
     */
    private double calculateSyntheticWaitTime(int urgency, int queueSize, int hour, double resourceAvail) {
        double wait = 20.0; // Base wait
        wait -= urgency * 3.5; // Urgent patients wait less
        wait += queueSize * 4.8; // Queue size increases wait
        wait += isPeakHour(hour) ? 10 : 0; // Peak hours add delay
        wait *= (1.0 - resourceAvail * 0.3); // More resources reduce wait
        wait += (Math.random() - 0.5) * 5; // Add noise
        return Math.max(5, wait); // Minimum 5 minutes
    }
    
    /**
     * Predict wait time for a patient given current system state
     */
    public double predictWaitTime(Patient patient, int queueSize, double resourceAvailability) {
        Calendar cal = Calendar.getInstance();
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        
        // Feature extraction
        double[] features = extractFeatures(patient.getUrgencyLevel(), queueSize, 
                                           hourOfDay, resourceAvailability);
        
        // Linear prediction
        double predictedWait = baseWaitTime;
        predictedWait += urgencyWeight * features[0];
        predictedWait += queueSizeWeight * features[1];
        predictedWait += timeOfDayWeight * features[2];
        predictedWait += resourceAvailabilityWeight * features[3];
        
        // Apply non-linearity for realism
        predictedWait = applyNonLinearity(predictedWait, patient.getUrgencyLevel());
        
        return Math.max(1, predictedWait); // Minimum 1 minute wait
    }
    
    /**
     * Extract features for ML model
     */
    private double[] extractFeatures(int urgency, int queueSize, int hour, double resourceAvail) {
        return new double[] {
            normalizeUrgency(urgency),
            normalizeQueueSize(queueSize),
            isPeakHour(hour) ? 1.0 : 0.0,
            resourceAvail
        };
    }
    
    /**
     * Apply non-linear transformations for more realistic predictions
     */
    private double applyNonLinearity(double baseWait, int urgency) {
        // Critical patients (urgency 5) get expedited
        if (urgency == 5) {
            return baseWait * 0.3;
        }
        
        // Apply sigmoid-like transformation
        double factor = 1.0 / (1.0 + Math.exp(-(urgency - 3) * 0.5));
        return baseWait * (0.5 + factor);
    }
    
    /**
     * Update model with actual wait time (online learning)
     */
    public void updateModel(Patient patient, double actualWaitTime, int queueSize, 
                           double resourceAvailability) {
        Calendar cal = Calendar.getInstance();
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        
        // Store new data point
        historicalData.add(new WaitTimeRecord(patient.getUrgencyLevel(), queueSize, 
                                             hourOfDay, resourceAvailability, actualWaitTime));
        
        // Predict with current model
        double predicted = predictWaitTime(patient, queueSize, resourceAvailability);
        double error = actualWaitTime - predicted;
        
        // Gradient descent update
        double[] features = extractFeatures(patient.getUrgencyLevel(), queueSize, 
                                           hourOfDay, resourceAvailability);
        
        baseWaitTime += LEARNING_RATE * error;
        urgencyWeight += LEARNING_RATE * error * features[0];
        queueSizeWeight += LEARNING_RATE * error * features[1];
        timeOfDayWeight += LEARNING_RATE * error * features[2];
        resourceAvailabilityWeight += LEARNING_RATE * error * features[3];
        
        // Keep only recent data (sliding window)
        if (historicalData.size() > 500) {
            historicalData.remove(0);
        }
    }
    
    /**
     * Train model on historical data using gradient descent
     */
    private void trainModel() {
        int epochs = 50;
        
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0;
            
            for (WaitTimeRecord record : historicalData) {
                double[] features = extractFeatures(record.urgency, record.queueSize, 
                                                   record.hourOfDay, record.resourceAvailability);
                
                // Predict
                double predicted = baseWaitTime;
                predicted += urgencyWeight * features[0];
                predicted += queueSizeWeight * features[1];
                predicted += timeOfDayWeight * features[2];
                predicted += resourceAvailabilityWeight * features[3];
                
                // Calculate error
                double error = record.actualWaitTime - predicted;
                totalError += error * error;
                
                // Update weights
                baseWaitTime += LEARNING_RATE * error;
                urgencyWeight += LEARNING_RATE * error * features[0];
                queueSizeWeight += LEARNING_RATE * error * features[1];
                timeOfDayWeight += LEARNING_RATE * error * features[2];
                resourceAvailabilityWeight += LEARNING_RATE * error * features[3];
            }
            
            // Early stopping if converged
            if (totalError / historicalData.size() < 0.1) {
                break;
            }
        }
    }
    
    /**
     * Get confidence interval for prediction
     */
    public double[] getConfidenceInterval(double prediction) {
        // Simple confidence interval based on historical variance
        double variance = calculateHistoricalVariance();
        double stdDev = Math.sqrt(variance);
        
        return new double[] {
            prediction - 1.96 * stdDev, // 95% lower bound
            prediction + 1.96 * stdDev  // 95% upper bound
        };
    }
    
    private double calculateHistoricalVariance() {
        if (historicalData.isEmpty()) return 10.0;
        
        double mean = historicalData.stream()
            .mapToDouble(r -> r.actualWaitTime)
            .average()
            .orElse(15.0);
        
        return historicalData.stream()
            .mapToDouble(r -> Math.pow(r.actualWaitTime - mean, 2))
            .average()
            .orElse(10.0);
    }
    
    private double normalizeUrgency(int urgency) {
        return (urgency - 3.0) / 2.0; // Normalize around mean urgency
    }
    
    private double normalizeQueueSize(int queueSize) {
        return queueSize / 10.0; // Normalize to [0, 1] range
    }
    
    private boolean isPeakHour(int hour) {
        return (hour >= 9 && hour <= 11) || (hour >= 14 && hour <= 16);
    }
    
    /**
     * Get model accuracy metrics
     */
    public Map<String, Double> getModelMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        
        if (historicalData.size() < 10) {
            metrics.put("mae", 0.0);
            metrics.put("rmse", 0.0);
            metrics.put("r2", 0.0);
            return metrics;
        }
        
        double mae = 0, mse = 0, totalVariance = 0;
        double meanActual = historicalData.stream()
            .mapToDouble(r -> r.actualWaitTime)
            .average()
            .orElse(0);
        
        for (WaitTimeRecord record : historicalData) {
            double predicted = baseWaitTime;
            double[] features = extractFeatures(record.urgency, record.queueSize, 
                                               record.hourOfDay, record.resourceAvailability);
            predicted += urgencyWeight * features[0];
            predicted += queueSizeWeight * features[1];
            predicted += timeOfDayWeight * features[2];
            predicted += resourceAvailabilityWeight * features[3];
            
            double error = Math.abs(record.actualWaitTime - predicted);
            mae += error;
            mse += error * error;
            totalVariance += Math.pow(record.actualWaitTime - meanActual, 2);
        }
        
        mae /= historicalData.size();
        mse /= historicalData.size();
        double rmse = Math.sqrt(mse);
        double r2 = 1.0 - (mse / (totalVariance / historicalData.size()));
        
        metrics.put("mae", mae);
        metrics.put("rmse", rmse);
        metrics.put("r2", r2);
        
        return metrics;
    }
    
    /**
     * Inner class to store historical wait time records
     */
    private static class WaitTimeRecord {
        int urgency;
        int queueSize;
        int hourOfDay;
        double resourceAvailability;
        double actualWaitTime;
        
        WaitTimeRecord(int urgency, int queueSize, int hourOfDay, 
                      double resourceAvailability, double actualWaitTime) {
            this.urgency = urgency;
            this.queueSize = queueSize;
            this.hourOfDay = hourOfDay;
            this.resourceAvailability = resourceAvailability;
            this.actualWaitTime = actualWaitTime;
        }
    }
}
