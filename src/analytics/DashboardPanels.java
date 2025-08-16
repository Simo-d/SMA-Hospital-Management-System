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
