#!/bin/bash

# Run the Hospital Management System with JADE
java -cp bin:lib/jade.jar jade.Boot -gui -agents "monitoring:agents.MonitoringAgent;scheduler:agents.SchedulerAgent;doctor1:agents.DoctorAgent(1,Dr.Smith,General);doctor2:agents.DoctorAgent(2,Dr.Johnson,Specialist);room1:agents.RoomAgent(1,Examination,2);room2:agents.RoomAgent(2,Surgery,1);equipment1:agents.EquipmentAgent(1,XRay);equipment2:agents.EquipmentAgent(2,MRI)" 2>&1