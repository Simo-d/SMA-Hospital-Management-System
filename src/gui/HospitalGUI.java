package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import models.Patient;
import utils.MessageProtocol;

/**
 * Hospital GUI - Visualizes the hospital resource allocation system
 * Master IA - Systèmes Multi-Agents Project
 */
public class HospitalGUI extends JFrame {
    private JTabbedPane tabbedPane;
    private JPanel patientPanel;
    private JPanel resourcePanel;
    private JPanel statsPanel;
    
    // Patient queue table
    private DefaultTableModel patientTableModel;
    private JTable patientTable;
    
    // Resource tables
    private DefaultTableModel doctorTableModel;
    private JTable doctorTable;
    private DefaultTableModel roomTableModel;
    private JTable roomTable;
    private DefaultTableModel equipmentTableModel;
    private JTable equipmentTable;
    
    // Statistics components
    private JLabel totalPatientsLabel;
    private JLabel avgWaitTimeLabel;
    private JLabel successRateLabel;
    private JLabel resourceUtilizationLabel;
    
    // Control panel
    private JPanel controlPanel;
    private JButton addPatientButton;
    private JComboBox<String> treatmentTypeCombo;
    private JComboBox<Integer> urgencyLevelCombo;
    
    // Reference to the monitoring agent
    private Agent monitoringAgent;
    
    public HospitalGUI(Agent agent) {
        this.monitoringAgent = agent;
        
        // Set up the frame
        setTitle("Hospital Resource Allocation System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Initialize panels
        createPatientPanel();
        createResourcePanel();
        createStatsPanel();
        createControlPanel();
        
        // Add panels to tabbed pane
        tabbedPane.addTab("Patients", patientPanel);
        tabbedPane.addTab("Resources", resourcePanel);
        tabbedPane.addTab("Statistics", statsPanel);
        
        // Add tabbed pane to frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(controlPanel, BorderLayout.SOUTH);
        
        // Set visible
        setVisible(true);
    }
    
    private void createPatientPanel() {
        patientPanel = new JPanel(new BorderLayout());
        
        // Create patient table model with columns
        patientTableModel = new DefaultTableModel();
        patientTableModel.addColumn("ID");
        patientTableModel.addColumn("Name");
        patientTableModel.addColumn("Urgency");
        patientTableModel.addColumn("Treatment");
        patientTableModel.addColumn("Status");
        patientTableModel.addColumn("Wait Time (s)");
        patientTableModel.addColumn("Doctor");
        patientTableModel.addColumn("Room");
        
        // Create table
        patientTable = new JTable(patientTableModel);
        JScrollPane scrollPane = new JScrollPane(patientTable);
        
        // Add to panel
        patientPanel.add(new JLabel("Patient Queue"), BorderLayout.NORTH);
        patientPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    private void createResourcePanel() {
        resourcePanel = new JPanel(new GridLayout(3, 1));
        
        // Doctor panel
        JPanel doctorPanel = new JPanel(new BorderLayout());
        doctorTableModel = new DefaultTableModel();
        doctorTableModel.addColumn("ID");
        doctorTableModel.addColumn("Name");
        doctorTableModel.addColumn("Specialization");
        doctorTableModel.addColumn("Available");
        doctorTableModel.addColumn("Current Patient");
        doctorTableModel.addColumn("Patients Served");
        
        doctorTable = new JTable(doctorTableModel);
        JScrollPane doctorScrollPane = new JScrollPane(doctorTable);
        doctorPanel.add(new JLabel("Doctors"), BorderLayout.NORTH);
        doctorPanel.add(doctorScrollPane, BorderLayout.CENTER);
        
        // Room panel
        JPanel roomPanel = new JPanel(new BorderLayout());
        roomTableModel = new DefaultTableModel();
        roomTableModel.addColumn("ID");
        roomTableModel.addColumn("Type");
        roomTableModel.addColumn("Available");
        roomTableModel.addColumn("Current Patient");
        roomTableModel.addColumn("Current Doctor");
        
        roomTable = new JTable(roomTableModel);
        JScrollPane roomScrollPane = new JScrollPane(roomTable);
        roomPanel.add(new JLabel("Rooms"), BorderLayout.NORTH);
        roomPanel.add(roomScrollPane, BorderLayout.CENTER);
        
        // Equipment panel
        JPanel equipmentPanel = new JPanel(new BorderLayout());
        equipmentTableModel = new DefaultTableModel();
        equipmentTableModel.addColumn("ID");
        equipmentTableModel.addColumn("Type");
        equipmentTableModel.addColumn("Available");
        equipmentTableModel.addColumn("Current Patient");
        equipmentTableModel.addColumn("Usage Count");
        
        equipmentTable = new JTable(equipmentTableModel);
        JScrollPane equipmentScrollPane = new JScrollPane(equipmentTable);
        equipmentPanel.add(new JLabel("Equipment"), BorderLayout.NORTH);
        equipmentPanel.add(equipmentScrollPane, BorderLayout.CENTER);
        
        // Add all to resource panel
        resourcePanel.add(doctorPanel);
        resourcePanel.add(roomPanel);
        resourcePanel.add(equipmentPanel);
    }
    
    private void createStatsPanel() {
        statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        
        // Create statistics labels
        totalPatientsLabel = new JLabel("Total patients processed: 0");
        avgWaitTimeLabel = new JLabel("Average wait time: 0.00 seconds");
        successRateLabel = new JLabel("Resource allocation success rate: 0.00%");
        resourceUtilizationLabel = new JLabel("Resource utilization: 0.00%");
        
        // Add to panel with some padding
        statsPanel.add(Box.createVerticalStrut(20));
        statsPanel.add(totalPatientsLabel);
        statsPanel.add(Box.createVerticalStrut(10));
        statsPanel.add(avgWaitTimeLabel);
        statsPanel.add(Box.createVerticalStrut(10));
        statsPanel.add(successRateLabel);
        statsPanel.add(Box.createVerticalStrut(10));
        statsPanel.add(resourceUtilizationLabel);
        
        // Add a chart panel (placeholder for now)
        JPanel chartPanel = new JPanel();
        chartPanel.setPreferredSize(new Dimension(500, 300));
        chartPanel.setBorder(BorderFactory.createTitledBorder("Performance Metrics"));
        chartPanel.setBackground(Color.WHITE);
        
        statsPanel.add(Box.createVerticalStrut(20));
        statsPanel.add(chartPanel);
    }
    
    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout());
        
        // Treatment type combo
        treatmentTypeCombo = new JComboBox<>(new String[] {
            "CONSULTATION", "EMERGENCY", "SURGERY", "CHECKUP", "XRAY", "MRI", "CT_SCAN"
        });
        
        // Urgency level combo
        urgencyLevelCombo = new JComboBox<>(new Integer[] {1, 2, 3, 4, 5});
        
        // Add patient button
        addPatientButton = new JButton("Add Patient");
        addPatientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(HospitalGUI.this, "Enter patient name:");
                if (name != null && !name.trim().isEmpty()) {
                    String treatment = (String) treatmentTypeCombo.getSelectedItem();
                    int urgency = (Integer) urgencyLevelCombo.getSelectedItem();
                    
                    // Notify the monitoring agent to create a new patient
                    ((MonitoringAgent) monitoringAgent).createPatientAgent(name, urgency, treatment);
                }
            }
        });
        
        // Add components to panel
        controlPanel.add(new JLabel("Treatment Type:"));
        controlPanel.add(treatmentTypeCombo);
        controlPanel.add(new JLabel("Urgency Level:"));
        controlPanel.add(urgencyLevelCombo);
        controlPanel.add(addPatientButton);
    }
    
    /**
     * Update the patient table with current data
     */
    public void updatePatientTable(List<Patient> patients) {
        // Clear existing rows
        while (patientTableModel.getRowCount() > 0) {
            patientTableModel.removeRow(0);
        }
        
        // Add patients to table
        for (Patient patient : patients) {
            patientTableModel.addRow(new Object[] {
                patient.getId().substring(0, 8),
                patient.getName(),
                patient.getUrgencyLevel(),
                patient.getTreatmentType(),
                patient.getStatus(),
                patient.getWaitingTime() / 1000,
                patient.getAssignedDoctor() != null ? patient.getAssignedDoctor() : "-",
                patient.getAssignedRoom() != null ? patient.getAssignedRoom() : "-"
            });
        }
    }
    
    /**
     * Update the doctor table with current data
     */
    public void updateDoctorTable(List<Map<String, Object>> doctors) {
        // Clear existing rows
        while (doctorTableModel.getRowCount() > 0) {
            doctorTableModel.removeRow(0);
        }
        
        // Add doctors to table
        for (Map<String, Object> doctor : doctors) {
            doctorTableModel.addRow(new Object[] {
                doctor.get("id"),
                doctor.get("name"),
                doctor.get("specialization"),
                doctor.get("available"),
                doctor.get("currentPatient") != null ? doctor.get("currentPatient") : "-",
                doctor.get("patientsServed")
            });
        }
    }
    
    /**
     * Update the room table with current data
     */
    public void updateRoomTable(List<Map<String, Object>> rooms) {
        // Clear existing rows
        while (roomTableModel.getRowCount() > 0) {
            roomTableModel.removeRow(0);
        }
        
        // Add rooms to table
        for (Map<String, Object> room : rooms) {
            roomTableModel.addRow(new Object[] {
                room.get("id"),
                room.get("type"),
                room.get("available"),
                room.get("currentPatient") != null ? room.get("currentPatient") : "-",
                room.get("currentDoctor") != null ? room.get("currentDoctor") : "-"
            });
        }
    }
    
    /**
     * Update the equipment table with current data
     */
    public void updateEquipmentTable(List<Map<String, Object>> equipment) {
        // Clear existing rows
        while (equipmentTableModel.getRowCount() > 0) {
            equipmentTableModel.removeRow(0);
        }
        
        // Add equipment to table
        for (Map<String, Object> item : equipment) {
            equipmentTableModel.addRow(new Object[] {
                item.get("id"),
                item.get("type"),
                item.get("available"),
                item.get("currentPatient") != null ? item.get("currentPatient") : "-",
                item.get("usageCount")
            });
        }
    }
    
    /**
     * Update statistics display
     */
    public void updateStatistics(int totalPatients, double avgWaitTime, 
                               double successRate, double resourceUtilization) {
        totalPatientsLabel.setText("Total patients processed: " + totalPatients);
        avgWaitTimeLabel.setText("Average wait time: " + String.format("%.2f", avgWaitTime) + " seconds");
        successRateLabel.setText("Resource allocation success rate: " + String.format("%.2f", successRate) + "%");
        resourceUtilizationLabel.setText("Resource utilization: " + String.format("%.2f", resourceUtilization) + "%");
    }
}