package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
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

import agents.MonitoringAgent;

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
        
        // Set up the frame with improved styling
        setTitle("Hospital Resource Allocation System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        
        // Set look and feel to system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));
        
        // Create tabbed pane with custom styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabbedPane.setBackground(Color.WHITE);
        
        // Initialize panels
        createPatientPanel();
        createResourcePanel();
        createStatsPanel();
        createControlPanel();
        
        // Add panels to tabbed pane with tooltips
        tabbedPane.addTab("Patients", new ImageIcon(), patientPanel, "View and manage patients");
        tabbedPane.addTab("Resources", new ImageIcon(), resourcePanel, "View hospital resources");
        tabbedPane.addTab("Statistics", new ImageIcon(), statsPanel, "View hospital statistics");
        
        // Add components to main panel
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // Add main panel to frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        
        // Add a title panel at the top
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(41, 128, 185)); // Blue header
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("Hospital Resource Allocation System");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        // Add current time to the right side of the title panel
        JLabel timeLabel = new JLabel(new java.util.Date().toString());
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        timeLabel.setForeground(Color.WHITE);
        titlePanel.add(timeLabel, BorderLayout.EAST);
        
        getContentPane().add(titlePanel, BorderLayout.NORTH);
        
        // Set visible
        setVisible(true);
    }
    
    private void createPatientPanel() {
        patientPanel = new JPanel(new BorderLayout(0, 10));
        patientPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        patientPanel.setBackground(new Color(240, 240, 240));
        
        // Create patient table model with columns
        patientTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        patientTableModel.addColumn("ID");
        patientTableModel.addColumn("Name");
        patientTableModel.addColumn("Urgency");
        patientTableModel.addColumn("Treatment");
        patientTableModel.addColumn("Status");
        patientTableModel.addColumn("Wait Time (s)");
        patientTableModel.addColumn("Doctor");
        patientTableModel.addColumn("Room");
        
        // Create table with improved styling
        patientTable = new JTable(patientTableModel);
        
        // Use TableStyler to style the patient table
        TableStyler styleTable = (JTable table) -> {
            table.setRowHeight(30); // Taller rows for better readability
            table.setIntercellSpacing(new Dimension(10, 5)); // More space between cells
            table.setShowGrid(true); // Show grid lines
            table.setGridColor(new Color(220, 220, 220)); // Light gray grid
            table.setSelectionBackground(new Color(232, 242, 254)); // Light blue selection
            table.setSelectionForeground(Color.BLACK);
            table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
            table.getTableHeader().setBackground(new Color(66, 139, 202)); // Blue header
            table.getTableHeader().setForeground(Color.WHITE);
            table.getTableHeader().setPreferredSize(new Dimension(0, 35));
        };
        
        // Apply the styling
        styleTable.style(patientTable);
        
        // Custom renderer for urgency level (color-coded)
        patientTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    int urgency = (Integer) value;
                    if (urgency >= 4) { // Critical or Emergency
                        c.setBackground(new Color(255, 102, 102)); // Red
                        c.setForeground(Color.WHITE);
                    } else if (urgency == 3) { // Urgent
                        c.setBackground(new Color(255, 204, 102)); // Orange
                        c.setForeground(Color.BLACK);
                    } else { // Normal
                        c.setBackground(new Color(204, 255, 204)); // Light green
                        c.setForeground(Color.BLACK);
                    }
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                        c.setForeground(table.getSelectionForeground());
                    }
                }
                return c;
            }
        });
        
        // Custom renderer for status (color-coded)
        patientTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String status = (String) value;
                    if (status.equals("WAITING")) {
                        c.setBackground(new Color(255, 255, 204)); // Light yellow
                    } else if (status.equals("IN_TREATMENT")) {
                        c.setBackground(new Color(204, 229, 255)); // Light blue
                    } else if (status.equals("COMPLETED")) {
                        c.setBackground(new Color(204, 255, 204)); // Light green
                    }
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                    }
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(patientTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Create header panel with title
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Patient Queue", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.setBackground(new Color(240, 240, 240));
        
        // Add to panel
        patientPanel.add(headerPanel, BorderLayout.NORTH);
        patientPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Interface for styling tables
     */
    private interface TableStyler {
        void style(JTable table);
    }
    
    private void createResourcePanel() {
        resourcePanel = new JPanel(new GridLayout(3, 1, 0, 10)); // Add vertical gap between components
        resourcePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        TableStyler styleTable = (JTable table) -> {
            table.setRowHeight(30); // Increased row height for better readability
            table.setIntercellSpacing(new Dimension(10, 5));
            table.setShowGrid(true);
            table.setGridColor(new Color(220, 220, 220));
            table.setSelectionBackground(new Color(184, 207, 229));
            table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
            table.getTableHeader().setBackground(new Color(66, 139, 202));
            table.getTableHeader().setForeground(Color.WHITE);
            table.getTableHeader().setPreferredSize(new Dimension(0, 35)); // Taller header
        };
        
        // Enhanced availability cell renderer with improved colors
        DefaultTableCellRenderer availabilityRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    boolean available = (Boolean) value;
                    if (available) {
                        c.setBackground(new Color(220, 255, 220)); // Brighter green for available
                        c.setForeground(new Color(0, 100, 0)); // Dark green text
                        setText("Available");
                    } else {
                        c.setBackground(new Color(255, 220, 220)); // Brighter red for busy
                        c.setForeground(new Color(150, 0, 0)); // Dark red text
                        setText("Busy");
                    }
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                        c.setForeground(table.getSelectionForeground());
                    }
                }
                return c;
            }
        };
        
        // Doctor panel with improved styling
        JPanel doctorPanel = new JPanel(new BorderLayout());
        doctorPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        doctorPanel.setBackground(Color.WHITE);
        
        doctorTableModel = new DefaultTableModel();
        doctorTableModel.addColumn("ID");
        doctorTableModel.addColumn("Name");
        doctorTableModel.addColumn("Specialization");
        doctorTableModel.addColumn("Available");
        doctorTableModel.addColumn("Current Patient");
        doctorTableModel.addColumn("Patients Served");
        
        doctorTable = new JTable(doctorTableModel);
        styleTable.style(doctorTable);
        doctorTable.getColumnModel().getColumn(3).setCellRenderer(availabilityRenderer);
        
        JScrollPane doctorScrollPane = new JScrollPane(doctorTable);
        doctorScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel doctorHeaderPanel = new JPanel(new BorderLayout());
        JLabel doctorLabel = new JLabel("Doctors", JLabel.CENTER);
        doctorLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        doctorLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        doctorHeaderPanel.add(doctorLabel, BorderLayout.CENTER);
        doctorHeaderPanel.setBackground(new Color(66, 139, 202)); // Blue header
        doctorLabel.setForeground(Color.WHITE);
        
        doctorPanel.add(doctorHeaderPanel, BorderLayout.NORTH);
        doctorPanel.add(doctorScrollPane, BorderLayout.CENTER);
        
        // Room panel with improved styling
        JPanel roomPanel = new JPanel(new BorderLayout());
        roomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        roomPanel.setBackground(Color.WHITE);
        
        roomTableModel = new DefaultTableModel();
        roomTableModel.addColumn("ID");
        roomTableModel.addColumn("Type");
        roomTableModel.addColumn("Available");
        roomTableModel.addColumn("Current Patient");
        roomTableModel.addColumn("Current Doctor");
        
        roomTable = new JTable(roomTableModel);
        styleTable.style(roomTable);
        roomTable.getColumnModel().getColumn(2).setCellRenderer(availabilityRenderer);
        
        JScrollPane roomScrollPane = new JScrollPane(roomTable);
        roomScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel roomHeaderPanel = new JPanel(new BorderLayout());
        JLabel roomLabel = new JLabel("Rooms", JLabel.CENTER);
        roomLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        roomLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        roomHeaderPanel.add(roomLabel, BorderLayout.CENTER);
        roomHeaderPanel.setBackground(new Color(92, 184, 92)); // Green header
        roomLabel.setForeground(Color.WHITE);
        
        roomPanel.add(roomHeaderPanel, BorderLayout.NORTH);
        roomPanel.add(roomScrollPane, BorderLayout.CENTER);
        
        // Equipment panel with improved styling
        JPanel equipmentPanel = new JPanel(new BorderLayout());
        equipmentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        equipmentPanel.setBackground(Color.WHITE);
        
        equipmentTableModel = new DefaultTableModel();
        equipmentTableModel.addColumn("ID");
        equipmentTableModel.addColumn("Type");
        equipmentTableModel.addColumn("Available");
        equipmentTableModel.addColumn("Current Patient");
        equipmentTableModel.addColumn("Usage Count");
        
        equipmentTable = new JTable(equipmentTableModel);
        styleTable.style(equipmentTable);
        equipmentTable.getColumnModel().getColumn(2).setCellRenderer(availabilityRenderer);
        
        JScrollPane equipmentScrollPane = new JScrollPane(equipmentTable);
        equipmentScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel equipmentHeaderPanel = new JPanel(new BorderLayout());
        JLabel equipmentLabel = new JLabel("Equipment", JLabel.CENTER);
        equipmentLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        equipmentLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        equipmentHeaderPanel.add(equipmentLabel, BorderLayout.CENTER);
        equipmentHeaderPanel.setBackground(new Color(240, 173, 78)); // Orange header
        equipmentLabel.setForeground(Color.WHITE);
        
        equipmentPanel.add(equipmentHeaderPanel, BorderLayout.NORTH);
        equipmentPanel.add(equipmentScrollPane, BorderLayout.CENTER);
        
        // Add all to resource panel
        resourcePanel.add(doctorPanel);
        resourcePanel.add(roomPanel);
        resourcePanel.add(equipmentPanel);
    }
    
    private void createStatsPanel() {
        statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Hospital Statistics Dashboard", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.setBackground(new Color(240, 240, 240));
        
        // Create statistics cards panel
        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        
        // Create styled statistic cards
        JPanel totalPatientsCard = createStatCard("Total Patients", "0", new Color(66, 139, 202)); // Blue
        JPanel avgWaitTimeCard = createStatCard("Avg Wait Time", "0.00 sec", new Color(92, 184, 92)); // Green
        JPanel successRateCard = createStatCard("Success Rate", "0.00%", new Color(240, 173, 78)); // Orange
        JPanel utilizationCard = createStatCard("Resource Utilization", "0.00%", new Color(217, 83, 79)); // Red
        
        // Store references to the value labels
        totalPatientsLabel = (JLabel) ((JPanel)((JPanel)totalPatientsCard.getComponent(0)).getComponent(1)).getComponent(0);
        avgWaitTimeLabel = (JLabel) ((JPanel)((JPanel)avgWaitTimeCard.getComponent(0)).getComponent(1)).getComponent(0);
        successRateLabel = (JLabel) ((JPanel)((JPanel)successRateCard.getComponent(0)).getComponent(1)).getComponent(0);
        resourceUtilizationLabel = (JLabel) ((JPanel)((JPanel)utilizationCard.getComponent(0)).getComponent(1)).getComponent(0);
        
        // Add cards to panel
        cardsPanel.add(totalPatientsCard);
        cardsPanel.add(avgWaitTimeCard);
        cardsPanel.add(successRateCard);
        cardsPanel.add(utilizationCard);
        
        // Create chart panel with placeholder
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        JPanel chartPanel = new JPanel();
        chartPanel.setPreferredSize(new Dimension(500, 300));
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        chartPanel.setBackground(Color.WHITE);
        
        // Add a placeholder chart title
        JLabel chartTitle = new JLabel("Performance Metrics Over Time", JLabel.CENTER);
        chartTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        chartTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Add a placeholder chart image
        JPanel chartImagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                
                // Set rendering hints for better quality
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // Draw coordinate axes
                g2d.setColor(Color.BLACK);
                g2d.drawLine(50, height - 50, width - 50, height - 50); // x-axis
                g2d.drawLine(50, 50, 50, height - 50); // y-axis
                
                // Draw x-axis labels
                g2d.drawString("Time", width / 2, height - 20);
                
                // Draw y-axis label
                Graphics2D g2 = (Graphics2D) g2d.create();
                g2.rotate(-Math.PI / 2, 20, height / 2);
                g2.drawString("Value", 20, height / 2);
                g2.dispose();
                
                // Draw sample data lines
                int numPoints = 10;
                int xStep = (width - 100) / numPoints;
                
                // Wait time line (green)
                int[] waitTimeY = generateRandomPoints(numPoints, 50, height - 150);
                g2d.setColor(new Color(92, 184, 92));
                g2d.setStroke(new BasicStroke(2));
                drawLine(g2d, waitTimeY, 50, xStep, height - 50);
                
                // Success rate line (orange)
                int[] successRateY = generateRandomPoints(numPoints, 50, height - 150);
                g2d.setColor(new Color(240, 173, 78));
                g2d.setStroke(new BasicStroke(2));
                drawLine(g2d, successRateY, 50, xStep, height - 50);
                
                // Utilization line (red)
                int[] utilizationY = generateRandomPoints(numPoints, 50, height - 150);
                g2d.setColor(new Color(217, 83, 79));
                g2d.setStroke(new BasicStroke(2));
                drawLine(g2d, utilizationY, 50, xStep, height - 50);
                
                // Draw legend
                int legendX = width - 150;
                int legendY = 70;
                int legendItemHeight = 20;
                
                g2d.setColor(new Color(92, 184, 92));
                g2d.fillRect(legendX, legendY, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawString("Wait Time", legendX + 20, legendY + 12);
                
                g2d.setColor(new Color(240, 173, 78));
                g2d.fillRect(legendX, legendY + legendItemHeight, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawString("Success Rate", legendX + 20, legendY + legendItemHeight + 12);
                
                g2d.setColor(new Color(217, 83, 79));
                g2d.fillRect(legendX, legendY + 2 * legendItemHeight, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawString("Utilization", legendX + 20, legendY + 2 * legendItemHeight + 12);
                
                g2d.dispose();
            }
            
            private int[] generateRandomPoints(int numPoints, int min, int max) {
                int[] points = new int[numPoints];
                for (int i = 0; i < numPoints; i++) {
                    points[i] = min + (int)(Math.random() * max);
                }
                return points;
            }
            
            private void drawLine(Graphics2D g2d, int[] points, int startX, int xStep, int baseY) {
                for (int i = 0; i < points.length - 1; i++) {
                    int x1 = startX + i * xStep;
                    int y1 = baseY - points[i];
                    int x2 = startX + (i + 1) * xStep;
                    int y2 = baseY - points[i + 1];
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        };
        chartImagePanel.setPreferredSize(new Dimension(500, 250));
        
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(chartTitle, BorderLayout.NORTH);
        chartPanel.add(chartImagePanel, BorderLayout.CENTER);
        
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        
        // Add all components to stats panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(cardsPanel, BorderLayout.NORTH);
        contentPanel.add(chartContainer, BorderLayout.CENTER);
        
        statsPanel.add(headerPanel, BorderLayout.NORTH);
        statsPanel.add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Helper method to create a statistic card
     */
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setBackground(Color.WHITE);
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        
        // Create colored indicator bar at top
        JPanel colorBar = new JPanel();
        colorBar.setBackground(color);
        colorBar.setPreferredSize(new Dimension(card.getWidth(), 5));
        
        // Create title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(new Color(100, 100, 100));
        
        // Create value label
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        valueLabel.setForeground(color);
        
        // Add components to content panel
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBackground(Color.WHITE);
        labelPanel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        valuePanel.setBackground(Color.WHITE);
        valuePanel.add(valueLabel);
        
        labelPanel.add(valuePanel, BorderLayout.CENTER);
        
        contentPanel.add(colorBar, BorderLayout.NORTH);
        contentPanel.add(labelPanel, BorderLayout.CENTER);
        
        card.add(contentPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        controlPanel.setBackground(new Color(245, 245, 245));
        
        // Add patient button with improved styling
        addPatientButton = new JButton("Add Patient");
        addPatientButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        addPatientButton.setForeground(Color.WHITE);
        addPatientButton.setBackground(new Color(41, 128, 185)); // Blue button
        addPatientButton.setFocusPainted(false);
        addPatientButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Add hover effect
        addPatientButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                addPatientButton.setBackground(new Color(52, 152, 219)); // Lighter blue on hover
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                addPatientButton.setBackground(new Color(41, 128, 185)); // Back to original blue
            }
        });
        
        addPatientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create a custom dialog for patient input
                final JDialog dialog = new JDialog(HospitalGUI.this, "Add New Patient", true);
                dialog.setLayout(new BorderLayout());
                dialog.setSize(400, 250);
                dialog.setLocationRelativeTo(HospitalGUI.this);
                
                // Create form panel
                JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 15));
                formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
                formPanel.setBackground(Color.WHITE);
                
                // Name field
                JLabel nameLabel = new JLabel("Patient Name:");
                nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                final JTextField nameField = new JTextField(20);
                nameField.setPreferredSize(new Dimension(150, 28));
                
                // Treatment type combo
                JLabel treatmentLabel = new JLabel("Treatment Type:");
                treatmentLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                final JComboBox<String> treatmentCombo = new JComboBox<>(new String[] {
                    "CONSULTATION", "EMERGENCY", "SURGERY", "CHECKUP", "XRAY", "MRI", "CT_SCAN"
                });
                
                // Urgency level combo with color renderer
                JLabel urgencyLabel = new JLabel("Urgency Level:");
                urgencyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                final JComboBox<Integer> urgencyCombo = new JComboBox<>(new Integer[] {1, 2, 3, 4, 5});
                urgencyCombo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (value != null) {
                            int urgency = (Integer) value;
                            if (urgency >= 4) { // Critical or Emergency
                                c.setBackground(new Color(255, 102, 102)); // Red
                                if (!isSelected) c.setForeground(Color.WHITE);
                            } else if (urgency == 3) { // Urgent
                                c.setBackground(new Color(255, 204, 102)); // Orange
                            } else { // Normal
                                c.setBackground(new Color(204, 255, 204)); // Light green
                            }
                        }
                        return c;
                    }
                });
                
                // Add components to form panel
                formPanel.add(nameLabel);
                formPanel.add(nameField);
                formPanel.add(treatmentLabel);
                formPanel.add(treatmentCombo);
                formPanel.add(urgencyLabel);
                formPanel.add(urgencyCombo);
                
                // Create button panel
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.setBackground(Color.WHITE);
                buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
                
                // Cancel button
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                });
                
                // Add patient button
                JButton addButton = new JButton("Add Patient");
                addButton.setBackground(new Color(41, 128, 185));
                addButton.setForeground(Color.WHITE);
                addButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String name = nameField.getText().trim();
                        if (!name.isEmpty()) {
                            String treatment = (String) treatmentCombo.getSelectedItem();
                            int urgency = (Integer) urgencyCombo.getSelectedItem();
                            
                            // Notify the monitoring agent to create a new patient
                            ((MonitoringAgent) monitoringAgent).createPatientAgent(name, urgency, treatment);
                            dialog.dispose();
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Please enter a patient name", "Input Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                
                buttonPanel.add(cancelButton);
                buttonPanel.add(Box.createHorizontalStrut(10));
                buttonPanel.add(addButton);
                
                // Add panels to dialog
                dialog.add(formPanel, BorderLayout.CENTER);
                dialog.add(buttonPanel, BorderLayout.SOUTH);
                
                // Show dialog
                dialog.setVisible(true);
            }
        });
        
        // Create a refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Request updated data from the monitoring agent
                // This would typically trigger a refresh of all tables
                JOptionPane.showMessageDialog(HospitalGUI.this, "Refreshing data...", "Refresh", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        // Add components to panel with some spacing
        controlPanel.add(refreshButton);
        controlPanel.add(Box.createHorizontalStrut(20));
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
    public void updateStatistics(int totalPatients, double avgWaitTime, double successRate, double resourceUtilization) {
        totalPatientsLabel.setText(String.valueOf(totalPatients));
        avgWaitTimeLabel.setText(String.format("%.2f", avgWaitTime) + " sec");
        successRateLabel.setText(String.format("%.2f%%", successRate));
        resourceUtilizationLabel.setText(String.format("%.2f%%", resourceUtilization));
    }
}