# Running the Hospital Resource Allocation System in NetBeans

## Setting up JADE Library in NetBeans

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

## Running the Project

1. Open the project in NetBeans

2. Right-click on the project in the Projects panel and select **Properties**

3. Go to **Libraries** and verify that the JADE library is included

4. Go to **Run** and verify that the Main Class is set to `HospitalMain`

5. Click **OK** to save the properties

6. Run the project by clicking the green **Run** button or pressing F6

## Troubleshooting

If you encounter any issues:

1. Make sure the JADE library is correctly configured

2. Check that the Main Class is set to `HospitalMain`

3. Verify that all source files are in the correct packages

4. If you get "ClassNotFoundException" for JADE classes, double-check your library configuration

## Alternative Run Configuration

You can also run the JADE platform with specific parameters:

1. Right-click on the project and select **Properties**

2. Go to **Run**

3. In the **VM Options** field, add:
   ```
   -gui -agents "Scheduler:agents.SchedulerAgent;Monitor:agents.MonitoringAgent"
   ```

4. Set the Main Class to `jade.Boot`

5. Click **OK** to save the properties

This alternative configuration starts JADE with the GUI and creates the Scheduler and Monitor agents automatically.