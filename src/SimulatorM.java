import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Random;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

public class SimulatorM {

	public int timeUnits;
	public int numberFlows;
	public int numberMonitoringApp;
	public int sizeInfra;
	public int capacityFlow;
	public int telemetryItemsRouter;
	public int maxSizeTelemetryItemsRouter;
	public String path;
	
	public float[] normalOpAvgItem;
	public float[] normalOpStdItem;
	ArrayList<ArrayList<Integer>> listAnomalies; //router,item
	
	public SimulatorM(int timeUnits, int sizeInfra, int numberFlows, int capacityFlow
			,int telemetryItemsRouter, int maxSizeTelemetryItemsRouter, int numberMonitoringApp
			, float[] normalOpAvgItem, float[] normalOpStdItem, String path) {

		this.timeUnits = timeUnits;
		this.numberFlows = numberFlows;
		this.numberMonitoringApp = numberMonitoringApp;
		this.sizeInfra = sizeInfra;
		this.capacityFlow = capacityFlow;
		this.telemetryItemsRouter = telemetryItemsRouter;
		this.maxSizeTelemetryItemsRouter = maxSizeTelemetryItemsRouter;
		this.path = path;
		
		this.normalOpAvgItem = normalOpAvgItem;
		this.normalOpStdItem = normalOpStdItem;
		
		this.listAnomalies = new ArrayList<ArrayList<Integer>>();

	}

	public ArrayList<NetworkFlow> generateNetworkFlows(long seed) {

		int source, destination;

		Random rnd = new Random(seed);

		ArrayList<NetworkFlow> networkFlows = new ArrayList<NetworkFlow>();

		for (int i = 0; i < this.numberFlows; i++) {

			do {
				source = rnd.nextInt(this.sizeInfra);
				destination = rnd.nextInt(this.sizeInfra);
			} while (source == destination);

			
			NetworkFlow flow = new NetworkFlow(source, destination, rnd.nextInt(this.capacityFlow) + 1);

			networkFlows.add(flow);

		}

		return networkFlows;

	}
	
	public Hashtable<String, Float> generateValuesNormalOp(long seed) {
		
		Random rnd = new Random(seed);
		
		Hashtable<String, Float> valuesItems = new Hashtable<String, Float>();
		
		float value = 0;
		
		for(int i = 0; i < this.sizeInfra; i++) {
			
			for(int j = 0; j < this.telemetryItemsRouter; j++) {
				
				String id = new String(Integer.toString(i) + Integer.toString(j));
				value = (float) (rnd.nextGaussian() * this.normalOpStdItem[j] + this.normalOpAvgItem[j]);
				valuesItems.put(id, value);
				
			}
				
		}
		
		return valuesItems;
			
	}
	
	public Hashtable<String, Float> generateValuesAnamolyOp(long seed, float percent) {
		
		Random rnd = new Random(seed);
		
		Hashtable<String, Float> valuesItems = new Hashtable<String, Float>();
		
		float value = 0;
		
		if(!this.listAnomalies.isEmpty()) {
			
			for(ArrayList<Integer> it : listAnomalies) {
				
				double increaseFactor = (double)2 + rnd.nextDouble();
				String id = new String(Integer.toString(it.get(0)) + Integer.toString(it.get(1)));
				
				value = (float) (rnd.nextGaussian() * this.normalOpStdItem[it.get(1)] + increaseFactor * this.normalOpAvgItem[it.get(1)]);
				valuesItems.put(id, value);
				
			}
			
			for(int i = 0; i < this.sizeInfra; i++) {
				
				for(int j = 0; j < this.telemetryItemsRouter; j++) {
						
					String id = new String(Integer.toString(i) + Integer.toString(j));
					value = (float) (rnd.nextGaussian() * this.normalOpStdItem[j] + this.normalOpAvgItem[j]);
					valuesItems.put(id, value);
						
				}
					
			}
			
		}else {
			
			for(int i = 0; i < this.sizeInfra; i++) {
				double prob = rnd.nextDouble();
				if(prob < percent) {
					
					for(int j = 0; j < this.telemetryItemsRouter; j++) {
						
						double increaseFactor = (double)2 + rnd.nextDouble();
						
						String id = new String(Integer.toString(i) + Integer.toString(j));
						value = (float) (rnd.nextGaussian() * this.normalOpStdItem[j] + increaseFactor * this.normalOpAvgItem[j]);
						valuesItems.put(id, value);
						
						ArrayList<Integer> it = new ArrayList<Integer>();
						it.add(i);
						it.add(j);
						
						this.listAnomalies.add(it);
						
					}
					
				}else {
					
					for(int j = 0; j < this.telemetryItemsRouter; j++) {
						
						
						String id = new String(Integer.toString(i) + Integer.toString(j));
						value = (float) (rnd.nextGaussian() * this.normalOpStdItem[j] + this.normalOpAvgItem[j]);
						valuesItems.put(id, value);
						
					}
					
				}
						
			}
			
		}
		
		
		
		return valuesItems;
			
	}
	
	ArrayList<Item> generateNewTelemetryItems(int currentTimeUnit, ArrayList<NetworkFlow> networkFlows
												, Hashtable<String, Float> valuesNormalOp){
		
		ArrayList<Item> newTelemetryItems = new ArrayList<Item>();
		
		for(int i = 0; i < networkFlows.size(); i++) {
			
			for(int j = 0; j < networkFlows.get(i).routers.size(); j++) {
				
				Item newItem = new Item(currentTimeUnit, networkFlows.get(i).routers.get(j));
				
				String key1 = Integer.toString(networkFlows.get(i).routers.get(j));
				String key2 = Integer.toString(networkFlows.get(i).itens.get(j));
				
				float value = valuesNormalOp.get(key1 + key2);
				
				newItem.featureList.add((double) value);
				newItem.itemId = networkFlows.get(i).itens.get(j);
				
				newTelemetryItems.add(newItem);
				
			}
			
		}
		
		return newTelemetryItems;
		
	}
	
	private boolean hasItemList(MonitoringApp monitoring, ArrayList<Integer> spatialItems) {
		
		boolean hasList = false;
		ArrayList<ArrayList<Integer>> listItems = monitoring.spatialRequirements;
		
		for(int i = 0; i < listItems.size(); i++) {
			
			if (listItems.get(i).containsAll(spatialItems))
				return true;
			
		}
		
		return hasList;
		
	}
	
	void printInfrastructure(NetworkInfrastructure infra, ArrayList<NetworkFlow> flows) {
		
		for(int i = 0; i < flows.size(); i++) {
			System.out.println("Flow " + i);
			System.out.println("    Capacity " + flows.get(i).capacityFlow);
		}
		
		for(int i = 0; i < this.sizeInfra; i++) {
			
			System.out.println("Node " + i);
			System.out.println("    Items ");
			for(int j = 0; j < this.telemetryItemsRouter; j++) {
				if (infra.items[i][j] == 1) {
					System.out.printf("%d ", infra.sizeTelemetryItems[j]);
				}
			}
			System.out.println("");
		}
		
	}
	
	/*
	 * This method creates a set of monitoring applications with its spatial and temporal dependencies.
	 */
	ArrayList<MonitoringApp> generateMonitoringApps(long seed, int numMonitoring, int numMaxSpatialDependencies
													, int maxSizeSpatialDependency, int maxFrequency){
		
		Random rnd = new Random(seed);
		int telemetryItems;
		int maxTelemetryItems = this.telemetryItemsRouter; //max telemetry in a router.
		int telemetryCandidate;
		boolean hasItem = false;
		
		int contFail = 0;
		
		
		ArrayList<MonitoringApp> monitoringApps = new ArrayList<MonitoringApp>();
		
		for(int i = 0; i < numMonitoring; i++) {
			
			if(numMaxSpatialDependencies < 3) {
				telemetryItems = 3;
			}else {
				do {
					
					telemetryItems = rnd.nextInt(numMaxSpatialDependencies);
					
				}while(telemetryItems < 3);
			}
			
			
			
			MonitoringApp mon = new MonitoringApp();
			
			for(int j = 0; j < telemetryItems; j++) {
				
				
				int size;
				
				do {
					size = rnd.nextInt(maxSizeSpatialDependency+1);

				}while(size <= 1);
				
				ArrayList<Integer> spatialItems = null;
				
				contFail = 0;
				do {
					
					if (contFail > 20) break; //not possible to create subset
					
					spatialItems = new ArrayList<Integer>();
					
					while(spatialItems.size() < size) {
						
						do{
							hasItem = false;
							telemetryCandidate = rnd.nextInt(this.telemetryItemsRouter);
							for(int l = 0; l < spatialItems.size(); l++) {
								if(spatialItems.get(l) == telemetryCandidate) {
									hasItem = true;
									break;
								}
							}
							
						}while(hasItem);
						
						spatialItems.add(telemetryCandidate);
						
						
					}
				
					Collections.sort(spatialItems);
					contFail++;
				}while(hasItemList(mon, spatialItems));
				
				if(spatialItems != null) {
					
					mon.spatialRequirements.add(spatialItems);
					
					int freq;
					
					do {
						freq = rnd.nextInt(maxFrequency+1);  //max frequency
					}while(freq == 0);
						
					mon.temporalRequirements.add(freq);
				
				}
				
				
			}
			
			//initialize history
			for(int j = 0; j < mon.spatialRequirements.size(); j++) {
				
				mon.lastTimeCollected.add(0); //MAX value means what not collected yet
			}
			
			monitoringApps.add(mon);
			
			
		}
		//
		return monitoringApps;
		
				
	}
		
	public void runRB(int numMaxSpatialDependencies, int maxSizeSpatialDependency, int maxFrequency, long seed) throws IloException, IOException {
		
		// Seed
		//long seed = System.currentTimeMillis();
		
		//Instantiate the graph
		NetworkInfrastructure infra = new NetworkInfrastructure(this.sizeInfra, this.path, this.telemetryItemsRouter,
				this.maxSizeTelemetryItemsRouter);
	
		//Load network topology from text file | To create random topologies, use loadTopology(long seed) prototype
		//infra.generateRndTopology(0.8, seed); //(dynamic maxSizeTelemetryItemsRouter)
		infra.loadTopologyTxt(seed); //(fixed maxSizeTelemetryItemsRouter) 
		
		//Create Monitorign Apps
		ArrayList<MonitoringApp> monitoringApps = generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency);
				
		
		//Create a set of networkflows.
		ArrayList<NetworkFlow> networkFlows = generateNetworkFlows(seed);
		for (int i = 0; i < networkFlows.size(); i++)
			networkFlows.get(i)
					.setPath(infra.getShortestPath(networkFlows.get(i).source, networkFlows.get(i).destination));
		
		
		StatisticsOut mngStatistics = new StatisticsOut(timeUnits, sizeInfra, numberFlows,
				capacityFlow, telemetryItemsRouter,
				maxSizeTelemetryItemsRouter, infra, "RR", 0,
				0,  0,  0, numMaxSpatialDependencies, seed);
		
		HeuristicRB algorithmStrategy = new HeuristicRB(infra, networkFlows, monitoringApps, mngStatistics , seed);
		
		mngStatistics.numberMonitoringApp = numberMonitoringApp;
		
		int[][] oldCollectedItems = new int[sizeInfra][telemetryItemsRouter]; //all zeros
		
		for (int i = 0; i < timeUnits; i++) {
			
			//run heuristic;
			algorithmStrategy.runRB_NoDependencies(oldCollectedItems);
			
			
			//Imprime a solucao//
			//algorithmStrategy.printSolution();
			
			
			//copy telemetry items to old matrix;
			algorithmStrategy.copyToOldCollectedItems(oldCollectedItems);
			
			//reset current matrix;
			algorithmStrategy.resetMatrix(algorithmStrategy.CollectedItems);
			
	
			//clean collected items at every iteration
			algorithmStrategy.resetFlowItems();
			
			mngStatistics.generateStatisticsRB(i);
			
		}
		
		mngStatistics.generateStatisticsItems();
		mngStatistics.generateStatisticsNetworkFlows();
		
		mngStatistics.out.close();
	}
	
	public void runRndFit(int numMaxSpatialDependencies, int maxSizeSpatialDependency, int maxFrequency, long seed) throws IloException, IOException {
	
		// Seed
		//long seed = System.currentTimeMillis();
		
		//Instantiate the graph
		NetworkInfrastructure infra = new NetworkInfrastructure(this.sizeInfra, this.path, this.telemetryItemsRouter,
				this.maxSizeTelemetryItemsRouter);
	
		//Load network topology from text file | To create random topologies, use loadTopology(long seed) prototype
		//infra.generateRndTopology(0.8, seed); //(dynamic maxSizeTelemetryItemsRouter)
		infra.loadTopologyTxt(seed); //(fixed maxSizeTelemetryItemsRouter) 
		
		//Create Monitorign Apps
		ArrayList<MonitoringApp> monitoringApps = generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency);
				
		
		//Create a set of networkflows.
		ArrayList<NetworkFlow> networkFlows = generateNetworkFlows(seed);
		for (int i = 0; i < networkFlows.size(); i++)
			networkFlows.get(i)
					.setPath(infra.getShortestPath(networkFlows.get(i).source, networkFlows.get(i).destination));
		
		
		StatisticsOut mngStatistics = new StatisticsOut(timeUnits, sizeInfra, numberFlows,
				capacityFlow, telemetryItemsRouter,
				maxSizeTelemetryItemsRouter, infra, "RndFit", 0,
				0,  0,  0, numMaxSpatialDependencies, seed);
		
		HeuristicRndFit algorithmStrategy = new HeuristicRndFit(infra, networkFlows, monitoringApps, mngStatistics , seed);
		
		mngStatistics.numberMonitoringApp = numberMonitoringApp;
		
		int[][] oldCollectedItems = new int[sizeInfra][telemetryItemsRouter]; //all zeros
		
		for (int i = 0; i < timeUnits; i++) {
			//System.out.print("ITERATION " + i + "----------------\n");
			//run heuristic;
			algorithmStrategy.runRndFit(oldCollectedItems);
			
			
			//Imprime a solucao//
			//algorithmStrategy.printSolution();
			
			
			//copy telemetry items to old matrix;
			algorithmStrategy.copyToOldCollectedItems(oldCollectedItems);
			
			//reset current matrix;
			algorithmStrategy.resetMatrix(algorithmStrategy.CollectedItems);
			
	
			//clean collected items at every iteration
			algorithmStrategy.resetFlowItems();
			
			mngStatistics.generateStatisticsRB(i);
			
		}
		
		mngStatistics.generateStatisticsItems();
		mngStatistics.generateStatisticsNetworkFlows();
		
		mngStatistics.out.close();
	}
	
	public void runDistribui(int numMaxSpatialDependencies, int maxSizeSpatialDependency, int maxFrequency, long seed) throws IloException, IOException {
		
		// Seed
		//long seed = System.currentTimeMillis();
		
		//Instantiate the graph
		NetworkInfrastructure infra = new NetworkInfrastructure(this.sizeInfra, this.path, this.telemetryItemsRouter,
				this.maxSizeTelemetryItemsRouter);
	
		//Load network topology from text file | To create random topologies, use loadTopology(long seed) prototype
		//infra.generateRndTopology(0.8, seed); //(dynamic maxSizeTelemetryItemsRouter)
		infra.loadTopologyTxt(seed); //(fixed maxSizeTelemetryItemsRouter) 
		
		//Create Monitorign Apps
		ArrayList<MonitoringApp> monitoringApps = generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency);
				
		
		//Create a set of networkflows.
		ArrayList<NetworkFlow> networkFlows = generateNetworkFlows(seed);
		for (int i = 0; i < networkFlows.size(); i++)
			networkFlows.get(i)
					.setPath(infra.getShortestPath(networkFlows.get(i).source, networkFlows.get(i).destination));
		
		
		StatisticsOut mngStatistics = new StatisticsOut(timeUnits, sizeInfra, numberFlows,
				capacityFlow, telemetryItemsRouter,
				maxSizeTelemetryItemsRouter, infra, "Distribute", 0,
				0,  0,  0, numMaxSpatialDependencies, seed);
		
		HeuristicRndFit algorithmStrategy = new HeuristicRndFit(infra, networkFlows, monitoringApps, mngStatistics , seed);
		
		mngStatistics.numberMonitoringApp = numberMonitoringApp;
		
		int[][] oldCollectedItems = new int[sizeInfra][telemetryItemsRouter]; //all zeros
		
		for (int i = 0; i < timeUnits; i++) {
			//System.out.print("ITERATION " + i + "----------------\n");
			//run heuristic;
			algorithmStrategy.distribui(oldCollectedItems);
			
			
			//Imprime a solucao//
			//algorithmStrategy.printSolution();
			
			
			//copy telemetry items to old matrix;
			algorithmStrategy.copyToOldCollectedItems(oldCollectedItems);
			
			//reset current matrix;
			algorithmStrategy.resetMatrix(algorithmStrategy.CollectedItems);
			
	
			//clean collected items at every iteration
			algorithmStrategy.resetFlowItems();
			
			mngStatistics.generateStatisticsRB(i);
			
		}
		
		mngStatistics.generateStatisticsItems();
		mngStatistics.generateStatisticsNetworkFlows();
		
		mngStatistics.out.close();
	}
	
	public void runConcentra(int numMaxSpatialDependencies, int maxSizeSpatialDependency, int maxFrequency,  long seed) throws IloException, IOException {
		
		// Seed
		//long seed = System.currentTimeMillis();
		
		//Instantiate the graph
		NetworkInfrastructure infra = new NetworkInfrastructure(this.sizeInfra, this.path, this.telemetryItemsRouter,
				this.maxSizeTelemetryItemsRouter);
	
		//Load network topology from text file | To create random topologies, use loadTopology(long seed) prototype
		//infra.generateRndTopology(0.8, seed); //(dynamic maxSizeTelemetryItemsRouter)
		infra.loadTopologyTxt(seed); //(fixed maxSizeTelemetryItemsRouter) 
		
		//Create Monitorign Apps
		ArrayList<MonitoringApp> monitoringApps = generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency);
				
		
		//Create a set of networkflows.
		ArrayList<NetworkFlow> networkFlows = generateNetworkFlows(seed);
		for (int i = 0; i < networkFlows.size(); i++)
			networkFlows.get(i)
					.setPath(infra.getShortestPath(networkFlows.get(i).source, networkFlows.get(i).destination));
		
		
		StatisticsOut mngStatistics = new StatisticsOut(timeUnits, sizeInfra, numberFlows,
				capacityFlow, telemetryItemsRouter,
				maxSizeTelemetryItemsRouter, infra, "Gather", 0,
				0,  0,  0, numMaxSpatialDependencies, seed);
		
		HeuristicRndFit algorithmStrategy = new HeuristicRndFit(infra, networkFlows, monitoringApps, mngStatistics , seed);
		
		mngStatistics.numberMonitoringApp = numberMonitoringApp;
		
		int[][] oldCollectedItems = new int[sizeInfra][telemetryItemsRouter]; //all zeros
		
		for (int i = 0; i < timeUnits; i++) {
			//System.out.print("ITERATION " + i + "----------------\n");
			//run heuristic;
			algorithmStrategy.concentra(oldCollectedItems);
			
			
			//Imprime a solucao//
			//algorithmStrategy.printSolution();
			
			
			//copy telemetry items to old matrix;
			algorithmStrategy.copyToOldCollectedItems(oldCollectedItems);
			
			//reset current matrix;
			algorithmStrategy.resetMatrix(algorithmStrategy.CollectedItems);
			
	
			//clean collected items at every iteration
			algorithmStrategy.resetFlowItems();
			
			mngStatistics.generateStatisticsRB(i);
			
		}
		
		mngStatistics.generateStatisticsItems();
		mngStatistics.generateStatisticsNetworkFlows();
		
		mngStatistics.out.close();
	}
	
	public void runFirstFit(int numMaxSpatialDependencies, int maxSizeSpatialDependency, int maxFrequency, long seed) throws IloException, IOException {
		
		// Seed
		//long seed = System.currentTimeMillis();
		
		//Instantiate the graph
		NetworkInfrastructure infra = new NetworkInfrastructure(this.sizeInfra, this.path, this.telemetryItemsRouter,
				this.maxSizeTelemetryItemsRouter);
	
		//Load network topology from text file | To create random topologies, use loadTopology(long seed) prototype
		//infra.generateRndTopology(0.8, seed); //(dynamic maxSizeTelemetryItemsRouter)
		infra.loadTopologyTxt(seed); //(fixed maxSizeTelemetryItemsRouter) 
		
		//Create Monitorign Apps
		ArrayList<MonitoringApp> monitoringApps = generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency);
				
		
		//Create a set of networkflows.
		ArrayList<NetworkFlow> networkFlows = generateNetworkFlows(seed);
		for (int i = 0; i < networkFlows.size(); i++)
			networkFlows.get(i)
					.setPath(infra.getShortestPath(networkFlows.get(i).source, networkFlows.get(i).destination));
		
		
		StatisticsOut mngStatistics = new StatisticsOut(timeUnits, sizeInfra, numberFlows,
				capacityFlow, telemetryItemsRouter,
				maxSizeTelemetryItemsRouter, infra, "FirstFit", 0,
				0,  0,  0, numMaxSpatialDependencies, seed);
		
		HeuristicRndFit algorithmStrategy = new HeuristicRndFit(infra, networkFlows, monitoringApps, mngStatistics , seed);
		
		
		int[][] oldCollectedItems = new int[sizeInfra][telemetryItemsRouter]; //all zeros
		
		for (int i = 0; i < timeUnits; i++) {
			//System.out.print("ITERATION " + i + "----------------\n");
			//run heuristic;
			algorithmStrategy.runFirstFit(oldCollectedItems);
			
			//Imprime a solucao//
			//algorithmStrategy.printSolution();
			
			
			//copy telemetry items to old matrix;
			algorithmStrategy.copyToOldCollectedItems(oldCollectedItems);
			
			//reset current matrix;
			algorithmStrategy.resetMatrix(algorithmStrategy.CollectedItems);
			
	
			//clean collected items at every iteration
			algorithmStrategy.resetFlowItems();
			
			mngStatistics.generateStatisticsRB(i);
			
		}
		
		mngStatistics.generateStatisticsItems();
		mngStatistics.generateStatisticsNetworkFlows();
		
		mngStatistics.out.close();
	}
	
	public void printMonitoringApps(ArrayList<MonitoringApp> monitoringApps) {
		
		
		for(int i = 0; i < monitoringApps.size(); i++) {
			System.out.println("Monitoring App " + i);
			int numSpatialDependencies = monitoringApps.get(i).spatialRequirements.size();
			for(int k = 0; k < numSpatialDependencies; k++) {
				
				System.out.println("   Spatial Req: " + k);
				
				for(int l = 0; l < monitoringApps.get(i).spatialRequirements.get(k).size(); l++) {
					
					System.out.printf("%d ", monitoringApps.get(i).spatialRequirements.get(k).get(l));
					
				}
				System.out.println(" ");
					
			}
				
		}
		
		
	}
	
	public void runOptNaive(IloCplex cplex, float percentDevicesAnomaly, int lastingTimeAnomaly, int intervalTimeUnitAnomaly, int window, int numMaxSpatialDependencies
			, int maxSizeSpatialDependency, int maxFrequency, long seed) throws IloException, IOException {

		//Instantiate the graph
		NetworkInfrastructure infra = new NetworkInfrastructure(this.sizeInfra, this.path, this.telemetryItemsRouter,
				this.maxSizeTelemetryItemsRouter);

		
		//Load network topology from text file | To create random topologies, use loadTopology(long seed) prototype
		//infra.generateRndTopology(0.8, seed); //(dynamic maxSizeTelemetryItemsRouter)
		infra.loadTopologyTxt(seed); //(fixed maxSizeTelemetryItemsRouter) 
		
		//Create a set of networkflows.
		ArrayList<NetworkFlow> networkFlows = generateNetworkFlows(seed);
		for (int i = 0; i < networkFlows.size(); i++)
			networkFlows.get(i)
					.setPath(infra.getShortestPath(networkFlows.get(i).source, networkFlows.get(i).destination));
		
		//Create Monitorign Apps
		ArrayList<MonitoringApp> monitoringApps = generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency);
		
		
		StatisticsOut mngStatistics = new StatisticsOut(timeUnits, sizeInfra, numberFlows,
														capacityFlow, telemetryItemsRouter,
														maxSizeTelemetryItemsRouter, infra, "DYNAMIC", percentDevicesAnomaly,
														lastingTimeAnomaly,  intervalTimeUnitAnomaly,  window, numMaxSpatialDependencies, seed);
		
		
		mngStatistics.numberMonitoringApp = numberMonitoringApp;
		
		AlgorithmOpt algorithmStrategy = new AlgorithmOpt(cplex, infra, networkFlows, monitoringApps, numMaxSpatialDependencies);
		
		boolean generateAnomaly = false;
		int contTimeUnitAnomaly = 0;
		
		
		for (int i = 0; i < timeUnits; i++) {
			
			Hashtable<String, Float> valuesNormalOp;
			
			//Generate normal values according to predefined mean/std
			if (contTimeUnitAnomaly == lastingTimeAnomaly) {
				generateAnomaly = false;
				contTimeUnitAnomaly = 0;
				this.listAnomalies.clear();
			}
			
			if(!generateAnomaly) {
				valuesNormalOp = generateValuesNormalOp(seed);
				
			}else{
				//System.out.println("Injected anomaly");
				valuesNormalOp = generateValuesAnamolyOp(seed, percentDevicesAnomaly); //number of devices being affected
				contTimeUnitAnomaly++;
				
			}
			
			if(!generateAnomaly && i > 0 && (i % intervalTimeUnitAnomaly == 0)) {
				
				generateAnomaly = true;
				contTimeUnitAnomaly = 0;
				
			}
			
			//Run algorithm strategy to collect network telemetry.
			//algorithmStrategy.runRnd();
			algorithmStrategy.run(valuesNormalOp, i, mngStatistics);
			
			ArrayList<Float> entropy = new ArrayList<Float>();
			
			
			
			mngStatistics.generateStatisticsOpt(i); //, entropy
			
			
		}
		
		mngStatistics.generateStatisticsItems();
		mngStatistics.generateStatisticsNetworkFlows();
		
		mngStatistics.out.close();
		
		
		
	}

	public void runLRU(int numMaxSpatialDependencies, int maxSizeSpatialDependency, int maxFrequency, long seed) throws FileNotFoundException {
		// Seed
		//long seed = System.currentTimeMillis();
				
		//Instantiate the graph
		NetworkInfrastructure infra = new NetworkInfrastructure(this.sizeInfra, this.path, this.telemetryItemsRouter,
					this.maxSizeTelemetryItemsRouter);
		//Load network topology from text file | To create random topologies, use loadTopology(long seed) prototype
		//infra.generateRndTopology(0.8, seed); //(dynamic maxSizeTelemetryItemsRouter)
		infra.loadTopologyTxt(seed); //(fixed maxSizeTelemetryItemsRouter) 
		
		//Create Monitorign Apps
		ArrayList<MonitoringApp> monitoringApps = generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency);
		
		//Create a set of networkflows.
		ArrayList<NetworkFlow> networkFlows = generateNetworkFlows(seed);
		for (int i = 0; i < networkFlows.size(); i++)
				networkFlows.get(i)
						.setPath(infra.getShortestPath(networkFlows.get(i).source, networkFlows.get(i).destination));
	
		StatisticsOut mngStatistics = new StatisticsOut(timeUnits, sizeInfra, numberFlows,
						capacityFlow, telemetryItemsRouter,
						maxSizeTelemetryItemsRouter, infra, "LRU", 0,
						0,  0,  0, numMaxSpatialDependencies, seed);
		
		mngStatistics.numberMonitoringApp = numberMonitoringApp;
		
		HeuristicLRU algorithmStrategy = new HeuristicLRU(infra, networkFlows, mngStatistics, monitoringApps, seed);
		
		//printInfrastructure(infra, networkFlows);
		
		for (int i = 0; i < timeUnits; i++) {
		
			algorithmStrategy.newLRU();
					
			mngStatistics.generateStatisticsLRU(i);
			
		
			
		}
		
		mngStatistics.generateStatisticsNetworkFlows();
		mngStatistics.generateStatisticsItems();
		mngStatistics.out.close();
		
		
		
		
	}
	
	
	public void runTeste(int numMaxSpatialDependencies, int maxSizeSpatialDependency, int maxFrequency, long seed) throws FileNotFoundException {
	
		NetworkInfrastructure infra = new NetworkInfrastructure(11, this.path, this.telemetryItemsRouter,
					this.maxSizeTelemetryItemsRouter);
		
		infra.loadAbilene(seed); //(fixed maxSizeTelemetryItemsRouter) 
		
		int[][]contLink = new int[12][12];
		
		for(int i = 0; i < 11; i++) {
			
			for(int j = 0; j < 11; j++) {
				
				if( i != j) {
					
					ArrayList<Integer> path = infra.getShortestPath(i, j);
					
					for(int k = 0; k < path.size()-1; k++) {
						contLink[path.get(k)][path.get(k+1)] += 1;
					}
					
					
				}
				
			}
			
		}
		
		for(int i = 0; i < 11; i++) {
			
			for(int j = 0; j < 11; j++) {
				
				if(infra.graph[i][j] == 1) {
					System.out.println(i + "," + j + " -- " + contLink[i][j]);
				}
				
			}
		}
		
		
		System.out.println("oi");
		
		
		
		
	}
	
}
