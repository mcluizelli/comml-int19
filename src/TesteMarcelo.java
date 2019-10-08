import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

public class TesteMarcelo {


	public static void main(String[] args) throws IloException, IOException {
		
		//Parameters
		int networkSize = 50; //Number of routers
		int timeUnits = 50;  
		int numberOfFlows = 200;
		
		int capacityFlow = 5;    			    //available space in a given flow (e.g., # of bytes)
		
		int telemetryItemsRouter = 8;	        //number of telemetry items per router
		int maxSizeTelemetryItemsRouter = 30; 	//max size of a given telemetry item (in bytes)
		
		float[] normalOpStdItem = {(float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05};
		float[] normalOpAvgItem = {(float) 1.1, (float) 3.2, (float) 2.5, (float) 1.7, (float) 0.3, (float) 4.3, (float) 0.452, (float) 0.78, (float) 1.1, (float) 3.2, (float) 2.5, (float) 1.7, (float) 0.3, (float) 4.3, (float) 0.452, (float) 0.78};
		
		int numberMonitoringApp = 6;
		int numMaxSpatialDependencies = 4;
		int maxSizeSpatialDependency = 4;
		int maxFrequency = 5;
			
		long seed = System.currentTimeMillis();
		
		
		System.out.println("Starting");
		
		String pathInstance = "/Users/mcluizelli/eclipse-workspace/INTelemetry/instances/hs/hs1.txt";
		
 		
 		float percentDevicesAnomaly = (float) 0.5;
 		int lastingTimeAnomaly = 5;
 		int intervalTimeUnitAnomaly = 5;
 		int window = 10;
 		IloCplex cplex = new IloCplex();
 		
 		
 		SimulatorM sim = new SimulatorM(timeUnits, networkSize, numberOfFlows, capacityFlow, 
	 				telemetryItemsRouter, maxSizeTelemetryItemsRouter, numberMonitoringApp, normalOpAvgItem, normalOpStdItem, pathInstance);
	 		
 		
 		sim.runOptNaive(cplex, percentDevicesAnomaly, lastingTimeAnomaly, intervalTimeUnitAnomaly, window, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, seed);
 		
 		//sim.runLRU(numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, seed);
			
		//sim.runRB(maxSizeSpatialDependency, maxSizeSpatialDependency, maxFrequency,  seed);
			
		//sim.runRndFit(maxSizeSpatialDependency, maxSizeSpatialDependency, maxFrequency, seed);
			
 		
 		
 		
 		cplex.end();
 		
 		
	
	}
	
}

