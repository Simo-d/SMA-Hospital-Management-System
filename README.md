# Hospital Resource Allocation System

A multi-agent system built with JADE (Java Agent DEvelopment Framework) that simulates a hospital resource allocation system. The system dynamically allocates hospital resources (doctors, rooms, equipment) to patients based on urgency levels and resource availability.

## Project Overview

This project implements a hospital resource allocation system using the JADE framework. The system consists of multiple agent types that interact to optimize resource allocation:

### Agents

1. **Patient Agent**
   - Represents patients with attributes like urgency level, required treatment, and arrival time
   - Requests treatment and waits for resource allocation

2. **Doctor Agent**
   - Represents doctors with specializations and availability status
   - Accepts patient assignments and performs treatments

3. **Room Agent**
   - Represents hospital rooms with different types (consultation, surgery, etc.)
   - Gets allocated to patients during treatment

4. **Equipment Agent**
   - Represents medical equipment (MRI, X-ray, etc.)
   - Gets allocated to patients when required for treatment

5. **Scheduler Agent**
   - Coordinates resource allocation
   - Matches patients with doctors, rooms, and equipment efficiently
   - Implements priority-based scheduling algorithms

6. **Monitoring Agent**
   - Manages the GUI and monitors system performance
   - Collects and displays statistics

### Key Features

- Priority-based patient scheduling based on urgency level and waiting time
- Dynamic resource discovery and allocation
- Real-time monitoring of system performance
- Graphical user interface for visualization
- Extensible architecture for adding more agents and behaviors

## Requirements

- Java JDK 8 or higher
- JADE Framework 4.5.0 or higher
- NetBeans IDE 12.0 or higher

## Project Structure

```
src/
├── agents/           # Agent implementations
│   ├── DoctorAgent.java
│   ├── EquipmentAgent.java
│   ├── MonitoringAgent.java
│   ├── PatientAgent.java
│   ├── RoomAgent.java
│   └── SchedulerAgent.java
├── behaviors/        # JADE behaviors
├── gui/              # GUI components
│   └── HospitalGUI.java
├── models/           # Data models
│   ├── Doctor.java
│   ├── Equipment.java
│   ├── Patient.java
│   └── Room.java
├── utils/            # Utility classes
│   ├── MessageProtocol.java
│   └── SchedulingAlgorithm.java
└── HospitalMain.java # Main class to start the system
```

## Running in NetBeans

### Setting up JADE Library

1. Download JADE from the official website: http://jade.tilab.com/download/jade/
   - Download the latest version of JADE (jade-4.5.0.zip or newer)

2. Extract the downloaded zip file

3. In NetBeans, go to **Tools > Libraries**

4. Click **New Library** and name it "JADE"

5. Click **Add JAR/Folder** and navigate to the extracted JADE folder

6. Add the following JAR files:
   - `jade/lib/jade.jar`
   - `jade/lib/commons-codec/commons-codec-1.3.jar` (if available)

7. Click **OK** to save the library

### Running the Project

1. Open the project in NetBeans

2. Right-click on the project in the Projects panel and select **Properties**

3. Go to **Libraries** and verify that the JADE library is included

4. Go to **Run** and verify that the Main Class is set to `HospitalMain`

5. Click **OK** to save the properties

6. Run the project by clicking the green **Run** button or pressing F6

### Alternative Run Configurations

The project includes two run configurations:

1. **HospitalMain** - Runs the system using the HospitalMain class
2. **JADE_GUI** - Runs the system using the JADE Boot class with predefined agents

To switch between configurations:

1. Click on the dropdown menu next to the Run button in NetBeans
2. Select the desired configuration
3. Click Run

You can also run the custom Ant target:

```
right-click project > Run Target > Custom > run-jade
```

## Evaluation Metrics

The system tracks several performance metrics:

- Average patient wait time
- Resource utilization rate (doctors, rooms, equipment)
- Number of patients treated successfully
- Resource allocation success rate

## License

This project is for educational purposes as part of the Master IA - Systèmes Multi-Agents course.