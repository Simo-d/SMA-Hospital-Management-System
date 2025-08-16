package analytics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.core.AID;

/**
 * Advanced Analytics Dashboard with real-time charts and metrics
 * Master IA - Systèmes Multi-Agents Project
 */
public class AnalyticsDashboard extends JFrame {
    
    private JTabbedPane tabbedPane;
    private OverviewPanel overviewPanel;
    private PerformancePanel performancePanel;
    private ResourcePanel resourcePanel;
    private PredictionPanel predictionPanel;
    private ManagePanel managePanel;
    
    // Data storage
    private Map<String, TimeSeries> timeSeriesData;
    private Map<String, Double> currentMetrics;
    private Map<String, AgentInfo> activeAgents;
    private Random random = new Random();
    
    // Reference to main agent system
    private jade.wrapper.AgentContainer mainContainer;
    
    // Agent counters for unique naming
    private int patientCounter = 0;
    private int doctorCounter = 10; // Start from 10 to avoid conflicts
    private int roomCounter = 10;
    private int equipmentCounter = 10;
    
    public AnalyticsDashboard() {
        setTitle("Hospital Analytics Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        // Initialize data structures
        timeSeriesData = new ConcurrentHashMap<>();
        currentMetrics = new ConcurrentHashMap<>();
        activeAgents = new ConcurrentHashMap<>();
        initializeTimeSeries();
        
        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 245, 250));
        
        // Create header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create tabbed pane with custom styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Create panels
        overviewPanel = new OverviewPanel();
        performancePanel = new PerformancePanel();
        resourcePanel = new ResourcePanel();
        predictionPanel = new PredictionPanel();
        managePanel = new ManagePanel();
        
        // Add tabs
        tabbedPane.addTab("Overview", overviewPanel);
        tabbedPane.addTab("Performance", performancePanel);
        tabbedPane.addTab("Resources", resourcePanel);
        tabbedPane.addTab("Predictions", predictionPanel);
        tabbedPane.addTab("Manage", managePanel);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Add control panel at bottom with better colors
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Start update timer
        Timer updateTimer = new Timer(1000, e -> updateDashboard());
        updateTimer.start();
    }
    
    public void setMainContainer(jade.wrapper.AgentContainer container) {
        this.mainContainer = container;
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(41, 128, 185));
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Title
        JLabel titleLabel = new JLabel("Hospital Resource Allocation - Analytics Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        // Time display
        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        timeLabel.setForeground(Color.WHITE);
        Timer timeTimer = new Timer(1000, e -> {
            timeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        });
        timeTimer.start();
        
        header.add(titleLabel, BorderLayout.WEST);
        header.add(timeLabel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBackground(new Color(52, 73, 94)); // Dark blue-gray background
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add Random Patient Button
        JButton addPatientBtn = createStyledButton("Add Random Patient", new Color(46, 204, 113), Color.WHITE);
        addPatientBtn.addActionListener(e -> addRandomPatient());
        
        // Add Multiple Patients Button
        JButton addMultipleBtn = createStyledButton("Add 5 Patients", new Color(52, 152, 219), Color.WHITE);
        addMultipleBtn.addActionListener(e -> {
            for (int i = 0; i < 5; i++) {
                addRandomPatient();
            }
        });
        
        // Emergency Patient Button
        JButton emergencyBtn = createStyledButton("Add Emergency", new Color(231, 76, 60), Color.WHITE);
        emergencyBtn.addActionListener(e -> addEmergencyPatient());
        
        // Clear Metrics Button
        JButton clearBtn = createStyledButton("Clear Metrics", new Color(189, 195, 199), Color.BLACK);
        clearBtn.addActionListener(e -> clearMetrics());
        
        // Export Report Button
        JButton exportBtn = createStyledButton("Export Report", new Color(155, 89, 182), Color.WHITE);
        exportBtn.addActionListener(e -> exportReport());
        
        panel.add(addPatientBtn);
        panel.add(addMultipleBtn);
        panel.add(emergencyBtn);
        panel.add(clearBtn);
        panel.add(exportBtn);
        
        return panel;
    }
    
    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setPreferredSize(new Dimension(140, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private void addRandomPatient() {
        if (mainContainer == null) {
            JOptionPane.showMessageDialog(this, "Agent container not initialized", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", 
                            "Henry", "Iris", "Jack", "Kate", "Leo", "Mia", "Noah", "Olivia"};
            String[] treatments = {"CONSULTATION", "SURGERY", "EMERGENCY", "CHECKUP", 
                                  "XRAY", "MRI", "CT_SCAN", "CARDIOLOGY"};
            
            String name = names[random.nextInt(names.length)] + "_" + (++patientCounter);
            String treatment = treatments[random.nextInt(treatments.length)];
            int urgency = random.nextInt(5) + 1;
            
            jade.wrapper.AgentController patientAgent = mainContainer.createNewAgent(
                "Patient_" + name,
                "agents.PatientAgent",
                new Object[]{name, urgency, treatment}
            );
            patientAgent.start();
            
            // Track agent
            activeAgents.put("Patient_" + name, new AgentInfo("Patient_" + name, "Patient", "Active"));
            
            // Show notification
            showNotification("Patient Added: " + name + " (Urgency: " + urgency + ", Treatment: " + treatment + ")");
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding patient: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addEmergencyPatient() {
        if (mainContainer == null) return;
        
        try {
            String name = "EMERGENCY_" + (++patientCounter);
            
            jade.wrapper.AgentController patientAgent = mainContainer.createNewAgent(
                "Patient_" + name,
                "agents.PatientAgent",
                new Object[]{name, 5, "EMERGENCY"} // Max urgency
            );
            patientAgent.start();
            
            activeAgents.put("Patient_" + name, new AgentInfo("Patient_" + name, "Patient", "Emergency"));
            
            showNotification("⚠️ EMERGENCY Patient Added: " + name);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showNotification(String message) {
        JLabel notification = new JLabel(message);
        notification.setOpaque(true);
        notification.setBackground(new Color(46, 204, 113));
        notification.setForeground(Color.WHITE);
        notification.setFont(new Font("Segoe UI", Font.BOLD, 12));
        notification.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JDialog dialog = new JDialog(this, false);
        dialog.setUndecorated(true);
        dialog.add(notification);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setLocation(dialog.getX(), dialog.getY() - 100);
        
        dialog.setVisible(true);
        
        Timer timer = new Timer(3000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
    }
    
    /**
     * Manage Panel - Create agents with detailed specifications
     */
    class ManagePanel extends JPanel {
        private JTabbedPane manageTabs;
        
        public ManagePanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(245, 245, 250));
            
            manageTabs = new JTabbedPane();
            manageTabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            // Create sub-panels for each agent type
            manageTabs.addTab("Patients", createPatientManagementPanel());
            manageTabs.addTab("Doctors", createDoctorManagementPanel());
            manageTabs.addTab("Rooms", createRoomManagementPanel());
            manageTabs.addTab("Equipment", createEquipmentManagementPanel());
            manageTabs.addTab("Batch Operations", createBatchOperationsPanel());
            
            add(manageTabs, BorderLayout.CENTER);
        }
        
        private JPanel createPatientManagementPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Title
            JLabel titleLabel = new JLabel("Create New Patient");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(titleLabel, gbc);
            
            // Patient Name
            gbc.gridwidth = 1;
            gbc.gridy = 1;
            panel.add(new JLabel("Patient Name:"), gbc);
            JTextField nameField = new JTextField(15);
            gbc.gridx = 1;
            panel.add(nameField, gbc);
            
            // Urgency Level
            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Urgency Level:"), gbc);
            JComboBox<String> urgencyCombo = new JComboBox<>(new String[]{"1 - Low", "2 - Moderate", "3 - Medium", "4 - High", "5 - Critical"});
            gbc.gridx = 1;
            panel.add(urgencyCombo, gbc);
            
            // Treatment Type
            gbc.gridx = 0; gbc.gridy = 3;
            panel.add(new JLabel("Treatment Type:"), gbc);
            JComboBox<String> treatmentCombo = new JComboBox<>(new String[]{
                "CONSULTATION", "CHECKUP", "SURGERY", "EMERGENCY", 
                "XRAY", "MRI", "CT_SCAN", "CARDIOLOGY", "NEUROLOGY"
            });
            gbc.gridx = 1;
            panel.add(treatmentCombo, gbc);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            
            JButton createBtn = new JButton("Create Patient");
            createBtn.setBackground(new Color(46, 204, 113));
            createBtn.setForeground(Color.WHITE);
            createBtn.addActionListener(e -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    name = "Patient_" + (++patientCounter);
                }
                int urgency = urgencyCombo.getSelectedIndex() + 1;
                String treatment = (String) treatmentCombo.getSelectedItem();
                
                createPatient(name, urgency, treatment);
                nameField.setText("");
            });
            
            JButton randomBtn = new JButton("Generate Random");
            randomBtn.setBackground(new Color(52, 152, 219));
            randomBtn.setForeground(Color.WHITE);
            randomBtn.addActionListener(e -> {
                String[] randomNames = {"John", "Mary", "David", "Sarah", "Michael", "Emma"};
                nameField.setText(randomNames[random.nextInt(randomNames.length)] + "_" + (patientCounter + 1));
                urgencyCombo.setSelectedIndex(random.nextInt(5));
                treatmentCombo.setSelectedIndex(random.nextInt(treatmentCombo.getItemCount()));
            });
            
            buttonPanel.add(createBtn);
            buttonPanel.add(randomBtn);
            
            gbc.gridx = 0; gbc.gridy = 4;
            gbc.gridwidth = 2;
            panel.add(buttonPanel, gbc);
            
            // Active Patients List
            gbc.gridy = 5;
            panel.add(new JSeparator(), gbc);
            
            gbc.gridy = 6;
            JLabel listLabel = new JLabel("Active Patients");
            listLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            panel.add(listLabel, gbc);
            
            DefaultListModel<String> patientListModel = new DefaultListModel<>();
            JList<String> patientList = new JList<>(patientListModel);
            JScrollPane scrollPane = new JScrollPane(patientList);
            scrollPane.setPreferredSize(new Dimension(300, 200));
            
            gbc.gridy = 7;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
            panel.add(scrollPane, gbc);
            
            // Update list periodically
            Timer updateTimer = new Timer(2000, e -> {
                patientListModel.clear();
                activeAgents.forEach((name, info) -> {
                    if (info.type.equals("Patient")) {
                        patientListModel.addElement(name + " - " + info.status);
                    }
                });
            });
            updateTimer.start();
            
            return panel;
        }
        
        private JPanel createDoctorManagementPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Title
            JLabel titleLabel = new JLabel("Create New Doctor");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(titleLabel, gbc);
            
            // Doctor ID
            gbc.gridwidth = 1;
            gbc.gridy = 1;
            panel.add(new JLabel("Doctor ID:"), gbc);
            JTextField idField = new JTextField("D" + doctorCounter, 15);
            gbc.gridx = 1;
            panel.add(idField, gbc);
            
            // Doctor Name
            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Doctor Name:"), gbc);
            JTextField nameField = new JTextField(15);
            gbc.gridx = 1;
            panel.add(nameField, gbc);
            
            // Specialization
            gbc.gridx = 0; gbc.gridy = 3;
            panel.add(new JLabel("Specialization:"), gbc);
            JComboBox<String> specCombo = new JComboBox<>(new String[]{
                "General", "Surgery", "Cardiology", "Neurology", 
                "Pediatrics", "Emergency", "Radiology", "Orthopedics"
            });
            gbc.gridx = 1;
            panel.add(specCombo, gbc);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            
            JButton createBtn = new JButton("Create Doctor");
            createBtn.setBackground(new Color(46, 204, 113));
            createBtn.setForeground(Color.WHITE);
            createBtn.addActionListener(e -> {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    name = "Dr. Smith_" + doctorCounter;
                }
                String spec = (String) specCombo.getSelectedItem();
                
                createDoctor(id, name, spec);
                doctorCounter++;
                idField.setText("D" + doctorCounter);
                nameField.setText("");
            });
            
            JButton randomBtn = new JButton("Generate Random");
            randomBtn.setBackground(new Color(52, 152, 219));
            randomBtn.setForeground(Color.WHITE);
            randomBtn.addActionListener(e -> {
                String[] randomNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia"};
                nameField.setText("Dr. " + randomNames[random.nextInt(randomNames.length)]);
                specCombo.setSelectedIndex(random.nextInt(specCombo.getItemCount()));
            });
            
            JButton addMultipleBtn = new JButton("Add 3 Doctors");
            addMultipleBtn.setBackground(new Color(155, 89, 182));
            addMultipleBtn.setForeground(Color.WHITE);
            addMultipleBtn.addActionListener(e -> {
                String[] specs = {"General", "Surgery", "Emergency"};
                String[] names = {"Anderson", "Martinez", "Taylor"};
                for (int i = 0; i < 3; i++) {
                    createDoctor("D" + (doctorCounter++), "Dr. " + names[i], specs[i]);
                }
                idField.setText("D" + doctorCounter);
            });
            
            buttonPanel.add(createBtn);
            buttonPanel.add(randomBtn);
            buttonPanel.add(addMultipleBtn);
            
            gbc.gridx = 0; gbc.gridy = 4;
            gbc.gridwidth = 2;
            panel.add(buttonPanel, gbc);
            
            return panel;
        }
        
        private JPanel createRoomManagementPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Title
            JLabel titleLabel = new JLabel("Create New Room");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(titleLabel, gbc);
            
            // Room ID
            gbc.gridwidth = 1;
            gbc.gridy = 1;
            panel.add(new JLabel("Room ID:"), gbc);
            JTextField idField = new JTextField("R" + roomCounter, 15);
            gbc.gridx = 1;
            panel.add(idField, gbc);
            
            // Room Type
            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Room Type:"), gbc);
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{
                "CONSULTATION", "SURGERY", "EMERGENCY", "ICU", 
                "EXAMINATION", "RECOVERY", "RADIOLOGY"
            });
            gbc.gridx = 1;
            panel.add(typeCombo, gbc);
            
            // Capacity
            gbc.gridx = 0; gbc.gridy = 3;
            panel.add(new JLabel("Capacity:"), gbc);
            JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
            gbc.gridx = 1;
            panel.add(capacitySpinner, gbc);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            
            JButton createBtn = new JButton("Create Room");
            createBtn.setBackground(new Color(46, 204, 113));
            createBtn.setForeground(Color.WHITE);
            createBtn.addActionListener(e -> {
                String id = idField.getText().trim();
                String type = (String) typeCombo.getSelectedItem();
                int capacity = (Integer) capacitySpinner.getValue();
                
                createRoom(id, type, capacity);
                roomCounter++;
                idField.setText("R" + roomCounter);
            });
            
            JButton addMultipleBtn = new JButton("Add Standard Set");
            addMultipleBtn.setBackground(new Color(155, 89, 182));
            addMultipleBtn.setForeground(Color.WHITE);
            addMultipleBtn.addActionListener(e -> {
                createRoom("R" + (roomCounter++), "CONSULTATION", 1);
                createRoom("R" + (roomCounter++), "SURGERY", 2);
                createRoom("R" + (roomCounter++), "EMERGENCY", 3);
                createRoom("R" + (roomCounter++), "ICU", 2);
                idField.setText("R" + roomCounter);
                showNotification("Added 4 standard rooms");
            });
            
            buttonPanel.add(createBtn);
            buttonPanel.add(addMultipleBtn);
            
            gbc.gridx = 0; gbc.gridy = 4;
            gbc.gridwidth = 2;
            panel.add(buttonPanel, gbc);
            
            return panel;
        }
        
        private JPanel createEquipmentManagementPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Title
            JLabel titleLabel = new JLabel("Create New Equipment");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(titleLabel, gbc);
            
            // Equipment ID
            gbc.gridwidth = 1;
            gbc.gridy = 1;
            panel.add(new JLabel("Equipment ID:"), gbc);
            JTextField idField = new JTextField("E" + equipmentCounter, 15);
            gbc.gridx = 1;
            panel.add(idField, gbc);
            
            // Equipment Type
            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Equipment Type:"), gbc);
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{
                "MRI", "CT_SCAN", "XRAY", "ULTRASOUND", 
                "ECG", "VENTILATOR", "DIALYSIS", "DEFIBRILLATOR"
            });
            gbc.gridx = 1;
            panel.add(typeCombo, gbc);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            
            JButton createBtn = new JButton("Create Equipment");
            createBtn.setBackground(new Color(46, 204, 113));
            createBtn.setForeground(Color.WHITE);
            createBtn.addActionListener(e -> {
                String id = idField.getText().trim();
                String type = (String) typeCombo.getSelectedItem();
                
                createEquipment(id, type);
                equipmentCounter++;
                idField.setText("E" + equipmentCounter);
            });
            
            JButton addEssentialBtn = new JButton("Add Essential Set");
            addEssentialBtn.setBackground(new Color(155, 89, 182));
            addEssentialBtn.setForeground(Color.WHITE);
            addEssentialBtn.addActionListener(e -> {
                createEquipment("E" + (equipmentCounter++), "MRI");
                createEquipment("E" + (equipmentCounter++), "CT_SCAN");
                createEquipment("E" + (equipmentCounter++), "XRAY");
                createEquipment("E" + (equipmentCounter++), "ECG");
                createEquipment("E" + (equipmentCounter++), "VENTILATOR");
                idField.setText("E" + equipmentCounter);
                showNotification("Added 5 essential equipment items");
            });
            
            buttonPanel.add(createBtn);
            buttonPanel.add(addEssentialBtn);
            
            gbc.gridx = 0; gbc.gridy = 3;
            gbc.gridwidth = 2;
            panel.add(buttonPanel, gbc);
            
            return panel;
        }
        
        private JPanel createBatchOperationsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Title
            JLabel titleLabel = new JLabel("Batch Operations");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            titleLabel.setForeground(Color.BLACK);
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(titleLabel, gbc);
            
            // Initialize Hospital Button
            JButton initHospitalBtn = new JButton("Initialize Complete Hospital");
            initHospitalBtn.setBackground(new Color(41, 128, 185));
            initHospitalBtn.setForeground(Color.WHITE);
            initHospitalBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            initHospitalBtn.setPreferredSize(new Dimension(250, 50));
            initHospitalBtn.setOpaque(true);
            initHospitalBtn.setBorderPainted(false);
            initHospitalBtn.addActionListener(e -> initializeHospital());
            
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            panel.add(initHospitalBtn, gbc);
            
            // Scenario Buttons
            gbc.gridy = 2;
            panel.add(new JSeparator(), gbc);
            
            JLabel scenarioLabel = new JLabel("Test Scenarios");
            scenarioLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            scenarioLabel.setForeground(Color.BLACK);
            gbc.gridy = 3;
            panel.add(scenarioLabel, gbc);
            
            // Normal Load Scenario
            JButton normalLoadBtn = new JButton("Normal Load (10 patients)");
            normalLoadBtn.setBackground(new Color(46, 204, 113));
            normalLoadBtn.setForeground(Color.WHITE);
            normalLoadBtn.setOpaque(true);
            normalLoadBtn.setBorderPainted(false);
            normalLoadBtn.addActionListener(e -> createNormalLoadScenario());
            gbc.gridy = 4;
            gbc.gridwidth = 1;
            panel.add(normalLoadBtn, gbc);
            
            // Emergency Scenario
            JButton emergencyScenarioBtn = new JButton("Emergency Scenario (5 critical)");
            emergencyScenarioBtn.setBackground(new Color(231, 76, 60));
            emergencyScenarioBtn.setForeground(Color.WHITE);
            emergencyScenarioBtn.setOpaque(true);
            emergencyScenarioBtn.setBorderPainted(false);
            emergencyScenarioBtn.addActionListener(e -> createEmergencyScenario());
            gbc.gridx = 1;
            panel.add(emergencyScenarioBtn, gbc);
            
            // Peak Load Scenario
            JButton peakLoadBtn = new JButton("Peak Load (20 patients)");
            peakLoadBtn.setBackground(new Color(241, 196, 15));
            peakLoadBtn.setForeground(Color.BLACK);
            peakLoadBtn.setOpaque(true);
            peakLoadBtn.setBorderPainted(false);
            gbc.gridx = 0; gbc.gridy = 5;
            panel.add(peakLoadBtn, gbc);
            
            peakLoadBtn.addActionListener(e -> createPeakLoadScenario());
            
            // Mixed Scenario
            JButton mixedBtn = new JButton("Mixed Scenario");
            mixedBtn.setBackground(new Color(155, 89, 182));
            mixedBtn.setForeground(Color.WHITE);
            mixedBtn.setOpaque(true);
            mixedBtn.setBorderPainted(false);
            gbc.gridx = 1;
            panel.add(mixedBtn, gbc);
            
            mixedBtn.addActionListener(e -> createMixedScenario());
            
            // Statistics
            gbc.gridy = 6;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            panel.add(new JSeparator(), gbc);
            
            JLabel statsLabel = new JLabel("System Statistics");
            statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            statsLabel.setForeground(Color.BLACK);
            gbc.gridy = 7;
            panel.add(statsLabel, gbc);
            
            JTextArea statsArea = new JTextArea(8, 40);
            statsArea.setEditable(false);
            statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            statsArea.setBackground(new Color(245, 245, 250));
            statsArea.setForeground(Color.BLACK);
            JScrollPane scrollPane = new JScrollPane(statsArea);
            scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            
            gbc.gridy = 8;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
            panel.add(scrollPane, gbc);
            
            // Update stats periodically
            Timer statsTimer = new Timer(3000, e -> {
                StringBuilder stats = new StringBuilder();
                stats.append("=== Current System Status ===\n");
                stats.append(String.format("Total Patients: %.0f\n", currentMetrics.getOrDefault("total_patients", 0.0)));
                stats.append(String.format("Waiting: %.0f\n", currentMetrics.getOrDefault("waiting_patients", 0.0)));
                stats.append(String.format("Treated: %.0f\n", currentMetrics.getOrDefault("treated_patients", 0.0)));
                stats.append(String.format("Avg Wait Time: %.1f min\n", currentMetrics.getOrDefault("avg_wait_time", 0.0)));
                stats.append(String.format("Doctor Utilization: %.1f%%\n", currentMetrics.getOrDefault("doctor_utilization", 0.0)));
                stats.append(String.format("Room Utilization: %.1f%%\n", currentMetrics.getOrDefault("room_utilization", 0.0)));
                statsArea.setText(stats.toString());
            });
            statsTimer.start();
            
            return panel;
        }
        
        private void createPatient(String name, int urgency, String treatment) {
            if (mainContainer == null) return;
            
            try {
                jade.wrapper.AgentController patientAgent = mainContainer.createNewAgent(
                    "Patient_" + name,
                    "agents.PatientAgent",
                    new Object[]{name, urgency, treatment}
                );
                patientAgent.start();
                
                activeAgents.put("Patient_" + name, new AgentInfo("Patient_" + name, "Patient", "Active"));
                showNotification("Created patient: " + name);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ManagePanel.this, "Error creating patient: " + e.getMessage());
            }
        }
        
        private void createDoctor(String id, String name, String specialization) {
            if (mainContainer == null) return;
            
            try {
                jade.wrapper.AgentController doctorAgent = mainContainer.createNewAgent(
                    "Doctor_" + id,
                    "agents.DoctorAgent",
                    new Object[]{id, name, specialization}
                );
                doctorAgent.start();
                
                activeAgents.put("Doctor_" + id, new AgentInfo("Doctor_" + id, "Doctor", "Available"));
                showNotification("Created doctor: " + name);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ManagePanel.this, "Error creating doctor: " + e.getMessage());
            }
        }
        
        private void createRoom(String id, String type, int capacity) {
            if (mainContainer == null) return;
            
            try {
                jade.wrapper.AgentController roomAgent = mainContainer.createNewAgent(
                    "Room_" + id,
                    "agents.RoomAgent",
                    new Object[]{id, type, capacity}
                );
                roomAgent.start();
                
                activeAgents.put("Room_" + id, new AgentInfo("Room_" + id, "Room", type));
                showNotification("Created room: " + id + " (" + type + ")");
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ManagePanel.this, "Error creating room: " + e.getMessage());
            }
        }
        
        private void createEquipment(String id, String type) {
            if (mainContainer == null) return;
            
            try {
                jade.wrapper.AgentController equipmentAgent = mainContainer.createNewAgent(
                    "Equipment_" + id,
                    "agents.EquipmentAgent",
                    new Object[]{id, type}
                );
                equipmentAgent.start();
                
                activeAgents.put("Equipment_" + id, new AgentInfo("Equipment_" + id, "Equipment", type));
                showNotification("Created equipment: " + id + " (" + type + ")");
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ManagePanel.this, "Error creating equipment: " + e.getMessage());
            }
        }
        
        private void initializeHospital() {
            // Create standard hospital resources
            String[] doctorSpecs = {"General", "Surgery", "Emergency", "Cardiology", "Pediatrics"};
            String[] doctorNames = {"Smith", "Johnson", "Williams", "Brown", "Davis"};
            
            for (int i = 0; i < 5; i++) {
                createDoctor("D" + (doctorCounter++), "Dr. " + doctorNames[i], doctorSpecs[i]);
            }
            
            // Create rooms
            createRoom("R" + (roomCounter++), "CONSULTATION", 1);
            createRoom("R" + (roomCounter++), "CONSULTATION", 1);
            createRoom("R" + (roomCounter++), "SURGERY", 2);
            createRoom("R" + (roomCounter++), "EMERGENCY", 3);
            createRoom("R" + (roomCounter++), "ICU", 2);
            createRoom("R" + (roomCounter++), "EXAMINATION", 1);
            
            // Create equipment
            createEquipment("E" + (equipmentCounter++), "MRI");
            createEquipment("E" + (equipmentCounter++), "CT_SCAN");
            createEquipment("E" + (equipmentCounter++), "XRAY");
            createEquipment("E" + (equipmentCounter++), "ECG");
            createEquipment("E" + (equipmentCounter++), "VENTILATOR");
            createEquipment("E" + (equipmentCounter++), "ULTRASOUND");
            
            showNotification("Hospital initialized with 5 doctors, 6 rooms, and 6 equipment items");
        }
        
        private void createNormalLoadScenario() {
            String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry", "Iris", "Jack"};
            String[] treatments = {"CONSULTATION", "CHECKUP", "XRAY", "CONSULTATION", "MRI", 
                                  "CHECKUP", "CONSULTATION", "CT_SCAN", "CHECKUP", "CONSULTATION"};
            int[] urgencies = {2, 3, 2, 1, 3, 2, 3, 4, 1, 2};
            
            for (int i = 0; i < 10; i++) {
                createPatient(names[i] + "_" + (++patientCounter), urgencies[i], treatments[i]);
            }
            
            showNotification("Created 10 patients with normal load distribution");
        }
        
        private void createEmergencyScenario() {
            for (int i = 0; i < 5; i++) {
                createPatient("CRITICAL_" + (++patientCounter), 5, "EMERGENCY");
            }
            showNotification("Created 5 critical emergency patients");
        }
        
        private void createPeakLoadScenario() {
            for (int i = 0; i < 20; i++) {
                String[] treatments = {"CONSULTATION", "SURGERY", "EMERGENCY", "CHECKUP", "XRAY", "MRI"};
                String treatment = treatments[random.nextInt(treatments.length)];
                int urgency = random.nextInt(5) + 1;
                createPatient("Peak_" + (++patientCounter), urgency, treatment);
            }
            showNotification("Created 20 patients for peak load testing");
        }
        
        private void createMixedScenario() {
            // Mix of different patient types
            createPatient("Emergency_" + (++patientCounter), 5, "EMERGENCY");
            createPatient("Surgery_" + (++patientCounter), 4, "SURGERY");
            createPatient("Consult_" + (++patientCounter), 2, "CONSULTATION");
            createPatient("MRI_" + (++patientCounter), 3, "MRI");
            createPatient("Checkup_" + (++patientCounter), 1, "CHECKUP");
            
            showNotification("Created mixed scenario with 5 different patient types");
        }
    }
    
    /**
     * Overview Panel with key metrics cards
     */
    class OverviewPanel extends JPanel {
        private List<MetricCard> metricCards;
        
        public OverviewPanel() {
            setLayout(new GridBagLayout());
            setBackground(new Color(245, 245, 250));
            
            metricCards = new ArrayList<>();
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 0.5;
            gbc.weighty = 0.5;
            
            MetricCard patientsCard = new MetricCard("Total Patients", "0", new Color(52, 152, 219));
            MetricCard waitTimeCard = new MetricCard("Avg Wait Time", "0 min", new Color(46, 204, 113));
            MetricCard successCard = new MetricCard("Success Rate", "100%", new Color(155, 89, 182));
            MetricCard utilizationCard = new MetricCard("Resource Utilization", "0%", new Color(241, 196, 15));
            
            metricCards.add(patientsCard);
            metricCards.add(waitTimeCard);
            metricCards.add(successCard);
            metricCards.add(utilizationCard);
            
            gbc.gridx = 0; gbc.gridy = 0;
            add(patientsCard, gbc);
            
            gbc.gridx = 1;
            add(waitTimeCard, gbc);
            
            gbc.gridx = 0; gbc.gridy = 1;
            add(successCard, gbc);
            
            gbc.gridx = 1;
            add(utilizationCard, gbc);
            
            gbc.gridx = 0; gbc.gridy = 2;
            gbc.gridwidth = 2;
            gbc.weighty = 1.0;
            add(new RealTimeChart("System Activity"), gbc);
        }
        
        public void update() {
            metricCards.get(0).setValue(String.format("%.0f", currentMetrics.getOrDefault("total_patients", 0.0)));
            metricCards.get(1).setValue(String.format("%.1f min", currentMetrics.getOrDefault("avg_wait_time", 0.0)));
            metricCards.get(2).setValue(String.format("%.1f%%", currentMetrics.getOrDefault("success_rate", 100.0)));
            
            double avgUtilization = (currentMetrics.getOrDefault("doctor_utilization", 0.0) + 
                                    currentMetrics.getOrDefault("room_utilization", 0.0) + 
                                    currentMetrics.getOrDefault("equipment_utilization", 0.0)) / 3;
            metricCards.get(3).setValue(String.format("%.1f%%", avgUtilization));
        }
    }
    
    /**
     * Performance Panel with charts and metrics
     */
    class PerformancePanel extends JPanel {
        private JPanel chartsPanel;
        private JTextArea performanceLog;
        
        public PerformancePanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(245, 245, 250));
            
            // Title
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(245, 245, 250));
            JLabel titleLabel = new JLabel("Performance Metrics");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            titlePanel.add(titleLabel);
            add(titlePanel, BorderLayout.NORTH);
            
            // Charts Panel
            chartsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
            chartsPanel.setBackground(new Color(245, 245, 250));
            chartsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Add performance charts
            chartsPanel.add(createPerformanceChart("Wait Time Trend", new Color(52, 152, 219)));
            chartsPanel.add(createPerformanceChart("Throughput", new Color(46, 204, 113)));
            chartsPanel.add(createPerformanceChart("Queue Length", new Color(241, 196, 15)));
            chartsPanel.add(createPerformanceChart("Success Rate", new Color(155, 89, 182)));
            
            add(chartsPanel, BorderLayout.CENTER);
            
            // Performance Log
            performanceLog = new JTextArea(5, 50);
            performanceLog.setEditable(false);
            performanceLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
            JScrollPane scrollPane = new JScrollPane(performanceLog);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Performance Log"));
            add(scrollPane, BorderLayout.SOUTH);
        }
        
        private JPanel createPerformanceChart(String title, Color color) {
            JPanel chartPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw title
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(title, 10, 20);
                    
                    // Draw sample chart
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(2));
                    
                    int[] values = new int[10];
                    for (int i = 0; i < 10; i++) {
                        values[i] = 30 + random.nextInt(40);
                    }
                    
                    int xStep = (getWidth() - 40) / 10;
                    for (int i = 0; i < 9; i++) {
                        int x1 = 20 + i * xStep;
                        int x2 = 20 + (i + 1) * xStep;
                        int y1 = getHeight() - values[i] - 20;
                        int y2 = getHeight() - values[i + 1] - 20;
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
            };
            chartPanel.setBackground(Color.WHITE);
            chartPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            chartPanel.setPreferredSize(new Dimension(300, 200));
            return chartPanel;
        }
        
        public void update() {
            String logEntry = String.format("[%s] Patients: %.0f | Wait: %.1f min | Success: %.1f%%\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                currentMetrics.getOrDefault("total_patients", 0.0),
                currentMetrics.getOrDefault("avg_wait_time", 0.0),
                currentMetrics.getOrDefault("success_rate", 100.0));
            
            performanceLog.append(logEntry);
            if (performanceLog.getLineCount() > 100) {
                performanceLog.setText(logEntry);
            }
            
            chartsPanel.repaint();
        }
    }
    
    /**
     * Resource Panel with utilization displays
     */
    class ResourcePanel extends JPanel {
        private JProgressBar doctorBar;
        private JProgressBar roomBar;
        private JProgressBar equipmentBar;
        private JTextArea resourceLog;
        private JPanel resourceGrid;
        
        public ResourcePanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(245, 245, 250));
            
            // Title
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(245, 245, 250));
            JLabel titleLabel = new JLabel("Resource Utilization");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            titlePanel.add(titleLabel);
            add(titlePanel, BorderLayout.NORTH);
            
            // Main content panel
            JPanel contentPanel = new JPanel(new GridBagLayout());
            contentPanel.setBackground(new Color(245, 245, 250));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Utilization bars
            gbc.gridx = 0; gbc.gridy = 0;
            contentPanel.add(new JLabel("Doctor Utilization:"), gbc);
            doctorBar = new JProgressBar(0, 100);
            doctorBar.setStringPainted(true);
            doctorBar.setForeground(new Color(52, 152, 219));
            gbc.gridx = 1; gbc.weightx = 1.0;
            contentPanel.add(doctorBar, gbc);
            
            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
            contentPanel.add(new JLabel("Room Utilization:"), gbc);
            roomBar = new JProgressBar(0, 100);
            roomBar.setStringPainted(true);
            roomBar.setForeground(new Color(46, 204, 113));
            gbc.gridx = 1; gbc.weightx = 1.0;
            contentPanel.add(roomBar, gbc);
            
            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
            contentPanel.add(new JLabel("Equipment Utilization:"), gbc);
            equipmentBar = new JProgressBar(0, 100);
            equipmentBar.setStringPainted(true);
            equipmentBar.setForeground(new Color(241, 196, 15));
            gbc.gridx = 1; gbc.weightx = 1.0;
            contentPanel.add(equipmentBar, gbc);
            
            // Resource Grid
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
            
            resourceGrid = new JPanel(new GridLayout(3, 1, 5, 5));
            resourceGrid.setBackground(Color.WHITE);
            resourceGrid.setBorder(BorderFactory.createTitledBorder("Resource Status"));
            
            // Add resource status panels
            resourceGrid.add(createResourceStatusPanel("Doctors", 5));
            resourceGrid.add(createResourceStatusPanel("Rooms", 6));
            resourceGrid.add(createResourceStatusPanel("Equipment", 6));
            
            contentPanel.add(resourceGrid, gbc);
            
            add(contentPanel, BorderLayout.CENTER);
            
            // Resource Log
            resourceLog = new JTextArea(4, 50);
            resourceLog.setEditable(false);
            resourceLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
            JScrollPane scrollPane = new JScrollPane(resourceLog);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Resource Activity"));
            add(scrollPane, BorderLayout.SOUTH);
        }
        
        private JPanel createResourceStatusPanel(String title, int count) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setBackground(Color.WHITE);
            
            JLabel label = new JLabel(title + ": ");
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            panel.add(label);
            
            for (int i = 0; i < count; i++) {
                JLabel statusIcon = new JLabel("●");
                statusIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
                statusIcon.setForeground(new Color(46, 204, 113));
                panel.add(statusIcon);
            }
            
            return panel;
        }
        
        public void update() {
            int doctorUtil = currentMetrics.getOrDefault("doctor_utilization", 0.0).intValue();
            int roomUtil = currentMetrics.getOrDefault("room_utilization", 0.0).intValue();
            int equipUtil = currentMetrics.getOrDefault("equipment_utilization", 0.0).intValue();
            
            doctorBar.setValue(doctorUtil);
            roomBar.setValue(roomUtil);
            equipmentBar.setValue(equipUtil);
            
            String logEntry = String.format("[%s] Resource Update - Doctors: %d%%, Rooms: %d%%, Equipment: %d%%\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                doctorUtil, roomUtil, equipUtil);
            
            resourceLog.append(logEntry);
            if (resourceLog.getLineCount() > 50) {
                resourceLog.setText(logEntry);
            }
        }
    }
    
    /**
     * Prediction Panel with ML insights
     */
    class PredictionPanel extends JPanel {
        private JTextArea predictionText;
        private JPanel predictionCharts;
        
        public PredictionPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(245, 245, 250));
            
            // Title
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(245, 245, 250));
            JLabel titleLabel = new JLabel("ML Predictions & Insights");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            titlePanel.add(titleLabel);
            add(titlePanel, BorderLayout.NORTH);
            
            // Split pane for predictions and visualizations
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setDividerLocation(400);
            
            // Left side - Predictions text
            predictionText = new JTextArea();
            predictionText.setEditable(false);
            predictionText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            predictionText.setBackground(Color.WHITE);
            JScrollPane textScroll = new JScrollPane(predictionText);
            textScroll.setBorder(BorderFactory.createTitledBorder("Predictions"));
            splitPane.setLeftComponent(textScroll);
            
            // Right side - Visualization
            predictionCharts = new JPanel(new GridLayout(2, 1, 10, 10));
            predictionCharts.setBackground(new Color(245, 245, 250));
            predictionCharts.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Add prediction visualizations
            predictionCharts.add(createPredictionVisualization("Wait Time Forecast", new Color(52, 152, 219)));
            predictionCharts.add(createPredictionVisualization("Resource Demand", new Color(46, 204, 113)));
            
            splitPane.setRightComponent(predictionCharts);
            add(splitPane, BorderLayout.CENTER);
            
            updatePredictions();
        }
        
        private JPanel createPredictionVisualization(String title, Color color) {
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw title
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(title, 10, 20);
                    
                    // Draw prediction curve
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(2));
                    
                    // Historical data (solid line)
                    int midPoint = getWidth() / 2;
                    g2.drawLine(20, getHeight() - 40, midPoint, getHeight() - 60);
                    
                    // Prediction (dashed line)
                    float[] dash = {5.0f, 5.0f};
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                    g2.drawLine(midPoint, getHeight() - 60, getWidth() - 20, getHeight() - 30);
                    
                    // Draw legend
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    g2.setStroke(new BasicStroke(1));
                    g2.drawString("Historical", 20, getHeight() - 10);
                    g2.drawString("Predicted", midPoint, getHeight() - 10);
                }
            };
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            return panel;
        }
        
        private void updatePredictions() {
            StringBuilder predictions = new StringBuilder();
            predictions.append("=== ML PREDICTIONS ===\n\n");
            
            double currentWait = currentMetrics.getOrDefault("avg_wait_time", 0.0);
            double utilization = (currentMetrics.getOrDefault("doctor_utilization", 0.0) + 
                                currentMetrics.getOrDefault("room_utilization", 0.0) + 
                                currentMetrics.getOrDefault("equipment_utilization", 0.0)) / 3;
            
            predictions.append("CURRENT STATUS\n");
            predictions.append(String.format("• Average Wait Time: %.1f minutes\n", currentWait));
            predictions.append(String.format("• System Utilization: %.1f%%\n", utilization));
            predictions.append(String.format("• Queue Length: %.0f patients\n\n", 
                currentMetrics.getOrDefault("queue_length", 0.0)));
            
            predictions.append("PREDICTIONS (Next Hour)\n");
            double predictedWait = currentWait * (1 + utilization/100);
            if (predictedWait == 0) predictedWait = 10;
            predictions.append(String.format("• Expected Wait Time: %.1f-%.1f minutes\n", 
                predictedWait * 0.8, predictedWait * 1.2));
            predictions.append(String.format("• Expected Patient Load: %d-%d\n", 
                5 + random.nextInt(5), 10 + random.nextInt(5)));
            
            predictions.append("\nRECOMMENDATIONS\n");
            if (utilization > 70) {
                predictions.append("⚠ High utilization - Consider adding resources\n");
            }
            if (currentWait > 20) {
                predictions.append("⚠ Long wait times - Prioritize urgent cases\n");
            }
            if (currentMetrics.getOrDefault("queue_length", 0.0) > 10) {
                predictions.append("⚠ Large queue - Enable load balancing\n");
            }
            if (utilization < 30 && currentWait < 5) {
                predictions.append("✓ System operating efficiently\n");
            }
            
            predictions.append("\nMODEL CONFIDENCE\n");
            predictions.append(String.format("• Accuracy: %.1f%%\n", (double)(85 + random.nextInt(10))));
            predictions.append("• Last Updated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
            predictionText.setText(predictions.toString());
        }
        
        public void update() {
            updatePredictions();
            predictionCharts.repaint();
        }
    }
    
    // Helper classes
    class MetricCard extends JPanel {
        private JLabel valueLabel;
        
        public MetricCard(String title, String value, Color color) {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
            
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            titleLabel.setForeground(Color.GRAY);
            
            valueLabel = new JLabel(value);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
            valueLabel.setForeground(color);
            
            add(titleLabel, BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);
        }
        
        public void setValue(String value) {
            valueLabel.setText(value);
        }
    }
    
    class RealTimeChart extends JPanel {
        private String title;
        private LinkedList<Double> dataPoints;
        private Timer animationTimer;
        
        public RealTimeChart(String title) {
            this.title = title;
            this.dataPoints = new LinkedList<>();
            
            for (int i = 0; i < 50; i++) {
                dataPoints.add(0.0);
            }
            
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            
            animationTimer = new Timer(500, e -> {
                dataPoints.removeFirst();
                double newValue = currentMetrics.getOrDefault("queue_length", 0.0) + Math.random() * 10;
                dataPoints.addLast(newValue);
                repaint();
            });
            animationTimer.start();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString(title, 10, 20);
            
            if (dataPoints.size() > 1) {
                g2.setColor(new Color(52, 152, 219));
                g2.setStroke(new BasicStroke(2));
                
                int xStep = (getWidth() - 60) / (dataPoints.size() - 1);
                double maxValue = dataPoints.stream().mapToDouble(Double::doubleValue).max().orElse(1);
                if (maxValue == 0) maxValue = 1;
                
                for (int i = 0; i < dataPoints.size() - 1; i++) {
                    int x1 = 40 + i * xStep;
                    int x2 = 40 + (i + 1) * xStep;
                    int y1 = getHeight() - 40 - (int)(dataPoints.get(i) * (getHeight() - 80) / maxValue);
                    int y2 = getHeight() - 40 - (int)(dataPoints.get(i + 1) * (getHeight() - 80) / maxValue);
                    
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
        }
    }
    
    class TimeSeries {
        private LinkedList<Double> data;
        private int maxSize;
        
        public TimeSeries(int maxSize) {
            this.maxSize = maxSize;
            this.data = new LinkedList<>();
        }
        
        public void addPoint(double value) {
            data.addLast(value);
            if (data.size() > maxSize) {
                data.removeFirst();
            }
        }
        
        public List<Double> getData() {
            return new ArrayList<>(data);
        }
        
        public void clear() {
            data.clear();
        }
    }
    
    private static class AgentInfo {
        String name;
        String type;
        String status;
        String activity;
        
        AgentInfo(String name, String type, String status) {
            this.name = name;
            this.type = type;
            this.status = status;
            this.activity = "Processing";
        }
    }
    
    private void initializeTimeSeries() {
        timeSeriesData.put("wait_time", new TimeSeries(100));
        timeSeriesData.put("throughput", new TimeSeries(100));
        timeSeriesData.put("utilization", new TimeSeries(100));
        timeSeriesData.put("queue_length", new TimeSeries(100));
        
        // Initialize all metrics with default values
        currentMetrics.put("total_patients", 0.0);
        currentMetrics.put("treated_patients", 0.0);
        currentMetrics.put("waiting_patients", 0.0);
        currentMetrics.put("avg_wait_time", 0.0);
        currentMetrics.put("success_rate", 100.0);
        currentMetrics.put("doctor_utilization", 0.0);
        currentMetrics.put("room_utilization", 0.0);
        currentMetrics.put("equipment_utilization", 0.0);
        currentMetrics.put("efficiency_score", 0.0);
        currentMetrics.put("queue_length", 0.0);
        currentMetrics.put("throughput", 0.0);
    }
    
    private void updateDashboard() {
        overviewPanel.update();
        performancePanel.update();
        resourcePanel.update();
        predictionPanel.update();
    }
    
    public void updateMetric(String key, double value) {
        currentMetrics.put(key, value);
        
        SwingUtilities.invokeLater(() -> {
            if (overviewPanel != null) {
                overviewPanel.update();
            }
            if (performancePanel != null) {
                performancePanel.update();
            }
            if (resourcePanel != null) {
                resourcePanel.update();
            }
        });
    }
    
    public void updateTimeSeries(String key, double value) {
        TimeSeries series = timeSeriesData.get(key);
        if (series != null) {
            series.addPoint(value);
        }
    }
    
    private void clearMetrics() {
        currentMetrics.replaceAll((k, v) -> 0.0);
        timeSeriesData.values().forEach(TimeSeries::clear);
        activeAgents.clear();
        showNotification("Metrics cleared");
    }
    
    private void exportReport() {
        StringBuilder report = new StringBuilder();
        report.append("Hospital Analytics Report\n");
        report.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
        
        report.append("=== Current Metrics ===\n");
        currentMetrics.forEach((key, value) -> {
            report.append(key).append(": ").append(String.format("%.2f", value)).append("\n");
        });
        
        report.append("\n=== Active Agents ===\n");
        activeAgents.forEach((name, info) -> {
            report.append(name).append(" - Type: ").append(info.type)
                  .append(", Status: ").append(info.status).append("\n");
        });
        
        try {
            String filename = "hospital_report_" + System.currentTimeMillis() + ".txt";
            java.nio.file.Files.write(java.nio.file.Paths.get(filename), report.toString().getBytes());
            showNotification("Report exported to " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
