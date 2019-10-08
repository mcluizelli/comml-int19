import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;

public class StatisticsOut {

	public int timeUnits;
	public int numberFlows;
	public int numberMonitoringApp;
	public int sizeInfra;
	public int capacityFlow;
	public int telemetryItemsRouter;
	public int maxSizeTelemetryItemsRouter;
	public int numSpatialDepdencies;
	

	
	public int numMaxSpatialDependencies;
	
	public String method;
	
	public int totalItensInfra;
	
	public NetworkInfrastructure infra;

	// Statistics
	public int countTotalItemsCollected;
	public int[][][] countItemsPerFlowTime;
	
	
	float percentDevicesAnomaly;
	int lastingTimeAnomaly;
	int intervalTimeUnitAnomaly;
	int window;
	
	
	long seed;
	
	//cplex 
	int contSB, contTB,  contMonApp, contFlows, contItems;
	
	
	//data structures to keep up 
	//Hashtable<String, Float> freshness; //  
	Hashtable<String, Float> qtdCollectedItem; //stores how many times an item was collected
	
	Hashtable<String, Float> qtdSatisfiedSd; //stores the amount of spatial dependencies satisfied.
	
	Hashtable<Integer, Integer> qtdFlowsUsed; //stores the number of times network flows have been used.
	
	Hashtable<Integer, Float> averageCapacityFlowsUsed;
	
	PrintWriter out;

	public StatisticsOut(int timeUnits, int sizeInfra, int numberFlows, int capacityFlow, int telemetryItemsRouter,
			int maxSizeTelemetryItemsRouter, NetworkInfrastructure infra, String method, float percentDevicesAnomaly,
			int lastingTimeAnomaly, int intervalTimeUnitAnomaly, int window, int numMaxSpatialDependencies, long seed) throws FileNotFoundException {

		this.timeUnits = timeUnits;
		this.numberFlows = numberFlows;
		this.numberMonitoringApp = numberMonitoringApp;
		this.numMaxSpatialDependencies = numMaxSpatialDependencies;
		this.sizeInfra = sizeInfra;
		this.capacityFlow = capacityFlow;
		this.telemetryItemsRouter = telemetryItemsRouter;
		this.maxSizeTelemetryItemsRouter = maxSizeTelemetryItemsRouter;
		this.method = method;
		
		this.percentDevicesAnomaly = percentDevicesAnomaly;
		this.lastingTimeAnomaly = lastingTimeAnomaly;
		this.intervalTimeUnitAnomaly = intervalTimeUnitAnomaly;
		this.window = window;
		

		this.seed = seed;
		
		
		this.qtdCollectedItem = new Hashtable<String, Float>();
		this.qtdSatisfiedSd   = new Hashtable<String, Float>();
		
		this.qtdFlowsUsed 	  = new Hashtable<Integer, Integer>();
		this.averageCapacityFlowsUsed = new Hashtable<Integer, Float>();
		
		
		this.infra = infra;

		this.totalItensInfra = 0;
		for(int i = 0; i < this.infra.size; i++) this.totalItensInfra += this.infra.numTelemetryItems[i];
		
		// Statistics
		countItemsPerFlowTime = new int[sizeInfra][this.telemetryItemsRouter][this.timeUnits];
		
		this.out = new PrintWriter(sizeInfra + "." + numberFlows + "." + capacityFlow + "." + telemetryItemsRouter 
				+ "." + maxSizeTelemetryItemsRouter + "." + this.method + "." + this.percentDevicesAnomaly
				+ "." + this.lastingTimeAnomaly + "." + this.intervalTimeUnitAnomaly + "." + this.window + "." + this.seed + ".dat");
		
	}
	
	public void generateStatisticsNetworkFlows() throws FileNotFoundException {
			
		PrintWriter outFlows = new PrintWriter(sizeInfra + "." + numberFlows + "." + capacityFlow + "." + telemetryItemsRouter 
				+ "." + maxSizeTelemetryItemsRouter + "." + this.method + "." + this.percentDevicesAnomaly
				+ "." + this.lastingTimeAnomaly + "." + this.intervalTimeUnitAnomaly + "." + this.window + "." + this.seed + ".networkFlows");
		
		
		for(Integer key : this.qtdFlowsUsed.keySet()) {
			float avgUtilization = this.averageCapacityFlowsUsed.get(key) / (float) this.timeUnits;
			
			outFlows.println(sizeInfra + ";" + numberFlows + ";" + capacityFlow + ";" + telemetryItemsRouter 
				+ ";" + maxSizeTelemetryItemsRouter + ";" + this.method + ";" + this.seed + ";" + key + ";" + this.qtdFlowsUsed.get(key) + ";" + avgUtilization );
			
		}
		
		outFlows.close();
		
	}
	
	
	public void generateStatisticsItems() throws FileNotFoundException {
		
		PrintWriter outFlows = new PrintWriter(sizeInfra + "." + numberFlows + "." + capacityFlow + "." + telemetryItemsRouter 
				+ "." + maxSizeTelemetryItemsRouter + "." + this.method + "." + this.percentDevicesAnomaly
				+ "." + this.lastingTimeAnomaly + "." + this.intervalTimeUnitAnomaly + "." + this.window + "." + this.seed + ".items");
		
		
		for(int i = 0; i < this.infra.size; i++) {
			
			for (int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				
				if(this.infra.items[i][j] == 1) {
					
					String key = Integer.toString(i) + Integer.toString(j);
					if(qtdCollectedItem.get(key) == null) {
						
						outFlows.println(sizeInfra + ";" + numberFlows + ";" + capacityFlow + ";" + telemetryItemsRouter 
								+ ";" + maxSizeTelemetryItemsRouter + ";" + this.method + ";" + this.seed + ";" + key + ";" + "0" );
						
					}else {
						outFlows.println(sizeInfra + ";" + numberFlows + ";" + capacityFlow + ";" + telemetryItemsRouter 
								+ ";" + maxSizeTelemetryItemsRouter + ";" + this.method + ";" + this.seed + ";" + key + ";" + qtdCollectedItem.get(key) );
					}
					
				}
				
			}
			
		}
		
		outFlows.close();
		
		PrintWriter outFlows2 = new PrintWriter(sizeInfra + "." + numberFlows + "." + capacityFlow + "." + telemetryItemsRouter 
				+ "." + maxSizeTelemetryItemsRouter + "." + this.method + "." + this.percentDevicesAnomaly
				+ "." + this.lastingTimeAnomaly + "." + this.intervalTimeUnitAnomaly + "." + this.window + "." + this.seed + ".itemsAg");
		
		
		for(int k = 1; k <= timeUnits; k++) {
			int cont = 0;
			for(int i = 0; i < this.infra.size; i++) {
				
				for (int j = 0; j < this.infra.telemetryItemsRouter; j++) {
					
					if(this.infra.items[i][j] == 1) {
						
						String key = Integer.toString(i) + Integer.toString(j);
						if(qtdCollectedItem.get(key) != null && qtdCollectedItem.get(key) >= k) {
							cont++;
						}
						
					}
					
				}
				
			}
			
			outFlows2.println(sizeInfra + ";" + numberFlows + ";" + capacityFlow + ";" + telemetryItemsRouter 
					+ ";" + maxSizeTelemetryItemsRouter + ";" + this.method + ";" + this.seed + ";" + k + ";" + (float)cont/(float)this.totalItensInfra );
			
		}
		
		outFlows2.close();
		
		
	}

	public void generateStatistics(ArrayList<NetworkFlow> networkFlows, ArrayList<ArrayList<Integer>> sol, int iTimeUnit) throws FileNotFoundException {

		int itensCollectedTimeUnit = countCollectedItems(networkFlows, iTimeUnit);
		
		float infraCover = (float)itensCollectedTimeUnit / (float)totalItensInfra;
		
		out.println(iTimeUnit + ";" + itensCollectedTimeUnit + ";" + this.countTotalItemsCollected + ";" + infraCover + ";" + countUseFlows(networkFlows)  + ";" + getAverage(iTimeUnit) + ";" + this.method 
				+ ";" + this.percentDevicesAnomaly + ";" + this.lastingTimeAnomaly + ";" + this.intervalTimeUnitAnomaly + ";" + this.window + ";");
		
		
		

	}
	
	public void setCplexData(int contSB, int contTB, int contMonApp, int contFlows, int contItems, int numSpatialDepdencies) {
		
		this.contSB = contSB;
		this.contTB = contTB;
		this.contMonApp = contMonApp;
		this.contFlows = contFlows;
		this.contItems = contItems;
		this.numSpatialDepdencies = numSpatialDepdencies;
		
	}
	
	
	
	public void generateStatisticsRB(int iTimeUnit) throws FileNotFoundException {

		float infraCover = (float)contItems / (float)totalItensInfra;
		
		int soma = 0;
		int cont = 0;
		float average = (float) 0.0;
		float max = Integer.MIN_VALUE;
		float min = Integer.MAX_VALUE;
		
		for(String key : this.qtdCollectedItem.keySet()) {
			soma += this.qtdCollectedItem.get(key);
			cont++;
			if (this.qtdCollectedItem.get(key) > max) max = this.qtdCollectedItem.get(key);
			if (this.qtdCollectedItem.get(key) < min) min = this.qtdCollectedItem.get(key);
		}
		
		average = (float)soma/(float)cont;	
		
		out.print(iTimeUnit +  ";" + totalItensInfra + ";" + this.numberFlows + ";" + contItems + ";" + infraCover + ";" + this.contSB + ";" + this.contTB + ";" + this.contMonApp + ";" + contFlows + ";" + this.method 
				+ ";" + this.percentDevicesAnomaly + ";" + this.lastingTimeAnomaly + ";" + this.intervalTimeUnitAnomaly + ";" + this.window + ";" + this.numberFlows +  ";" + this.capacityFlow + ";" + this.telemetryItemsRouter +  ";" + this.maxSizeTelemetryItemsRouter  +  ";" + average + ";" + max + ";" + min + ";" + this.numMaxSpatialDependencies + ";" + this.numberMonitoringApp + "\n");
		
		

	}
	
	public void generateStatisticsOpt(int iTimeUnit) throws FileNotFoundException {

		float infraCover = (float)contItems / (float)totalItensInfra;
		
		int soma = 0;
		int cont = 0;
		float average = (float) 0.0;
		float max = Integer.MIN_VALUE;
		float min = Integer.MAX_VALUE;
		
		for(String key : this.qtdCollectedItem.keySet()) {
			soma += this.qtdCollectedItem.get(key);
			cont++;
			if (this.qtdCollectedItem.get(key) > max) max = this.qtdCollectedItem.get(key);
			if (this.qtdCollectedItem.get(key) < min) min = this.qtdCollectedItem.get(key);
		}
		
		average = (float)soma/(float)cont;
		
		out.print(iTimeUnit +  ";" + totalItensInfra + ";" + this.numberFlows + ";" +  contItems + ";" + infraCover + ";" + this.contSB + ";" + this.contTB + ";" + this.contMonApp + ";" + contFlows + ";" + this.method 
				+ ";" + this.percentDevicesAnomaly + ";" + this.lastingTimeAnomaly + ";" + this.intervalTimeUnitAnomaly + ";" + this.window + ";" + this.numberFlows +  ";" + this.capacityFlow 
				+ ";" + this.telemetryItemsRouter +  ";" + this.maxSizeTelemetryItemsRouter +  ";" + average + ";" + max + ";" + min + ";" + this.numMaxSpatialDependencies + ";" + this.numberMonitoringApp + "\n");
		

	}
	
	public void generateStatisticsLRU(int iTimeUnit) throws FileNotFoundException {

		float infraCover = (float)contItems / (float)totalItensInfra;
		
		int soma = 0;
		int cont = 0;
		float average = (float) 0.0;
		float max = Integer.MIN_VALUE;
		float min = Integer.MAX_VALUE;
		
		float averageCover = 0;
		
		for(String key : this.qtdCollectedItem.keySet()) {
			soma += this.qtdCollectedItem.get(key);
			cont++;
			if (this.qtdCollectedItem.get(key) > max) max = this.qtdCollectedItem.get(key);
			if (this.qtdCollectedItem.get(key) < min) min = this.qtdCollectedItem.get(key);
		}
		
		
		averageCover = soma / ( (float)(iTimeUnit+1) * (float) totalItensInfra );
		average = (float)soma/(float)cont;
		
		out.print(iTimeUnit +  ";" + totalItensInfra + ";" + this.numberFlows + ";" + contItems + ";" + infraCover + ";" + this.contSB + ";" + this.contTB + ";" + this.contMonApp + ";" + contFlows + ";" + this.method 
				+ ";" + this.percentDevicesAnomaly + ";" + this.lastingTimeAnomaly + ";" + this.intervalTimeUnitAnomaly + ";" + this.window + ";" + this.numberFlows +  ";" + this.capacityFlow + ";" + this.telemetryItemsRouter +  ";" + this.maxSizeTelemetryItemsRouter +  ";" + average + ";" + max + ";" + min + ";" + this.numMaxSpatialDependencies + ";" + this.numberMonitoringApp + ";" + averageCover + "\n");
		
		

	}
	
	public void generateStatistics(int iTimeUnit, ArrayList<Float> entropy) throws FileNotFoundException {

		//int itensCollectedTimeUnit = countCollectedItems(networkFlows, iTimeUnit);
		
		float infraCover = (float)contItems / (float)totalItensInfra;
		
		out.print(iTimeUnit +  ";" + totalItensInfra + ";" + this.numberFlows + ";" +  contItems + ";" + infraCover + ";" + this.contSB + ";" + this.contTB + ";" + this.contMonApp + ";" + contFlows + ";" + this.method 
				+ ";" + this.percentDevicesAnomaly + ";" + this.lastingTimeAnomaly + ";" + this.intervalTimeUnitAnomaly + ";" + this.window + ";" + entropy.size() );
		
		for(int i = 0; i < entropy.size(); i++) {
			
			out.print(";"  + entropy.get(i));
			
		}
		
		out.print("\n");
		int soma = 0;
		int cont = 0;
		float max = Integer.MIN_VALUE;
		float min = Integer.MAX_VALUE;
		
		for(String key : this.qtdCollectedItem.keySet()) {
			//System.out.println(key + " " + this.qtdCollected.get(key));
			soma += this.qtdCollectedItem.get(key);
			cont++;
			if (this.qtdCollectedItem.get(key) > max) max = this.qtdCollectedItem.get(key);
			if (this.qtdCollectedItem.get(key) < min) min = this.qtdCollectedItem.get(key);
		}
		
		int min10 = 0;
		
		for(String key : this.qtdCollectedItem.keySet()) {
			
			if (this.qtdCollectedItem.get(key) <= 10) min10++;
		
		}
		
		int sum = 0;
		
		System.out.println(" ");
		
		for(String key : this.qtdSatisfiedSd.keySet()) {
			
			System.out.println(this.qtdSatisfiedSd.get(key)); 
		
		}
		
		System.out.println(" ");
		
		//System.out.println((float)soma/(float)cont + " "  + min + " " + max + " " + min10);
		

	}
	
	public void teste() {
		
		int soma = 0;
		int cont = 0;
		float max = Integer.MIN_VALUE;
		float min = Integer.MAX_VALUE;
		
		for(String key : this.qtdCollectedItem.keySet()) {
			//System.out.println(key + " " + this.qtdCollected.get(key));
			soma += this.qtdCollectedItem.get(key);
			cont++;
			if (this.qtdCollectedItem.get(key) > max) max = this.qtdCollectedItem.get(key);
			if (this.qtdCollectedItem.get(key) < min) min = this.qtdCollectedItem.get(key);
		}
		
		int min10 = 0;
		
		for(String key : this.qtdCollectedItem.keySet()) {
			
			if (this.qtdCollectedItem.get(key) <= 10) min10++;
		
		}
		
		System.out.println((float)soma/(float)cont + " "  + min + " " + max + " " + min10);
		
	}
	
	
	public int countUseFlows(ArrayList<NetworkFlow> networkFlows) {
		
		int count = 0;
		for (int j = 0; j < networkFlows.size(); j++) {
			
			if (networkFlows.get(j).itens.size() > 0) {
				count++;
			}
			
		}

		return count;
		
	}

	public int countCollectedItems(ArrayList<NetworkFlow> networkFlows, int iTimeUnit) {
		
		// Keep track of how many times each telemetry item was collected
		int countTimeUnit = 0;
		for (int j = 0; j < networkFlows.size(); j++) {
			for (int k = 0; k < networkFlows.get(j).itens.size(); k++) {
				countItemsPerFlowTime[networkFlows.get(j).routers.get(k)][networkFlows.get(j).itens.get(k)][iTimeUnit] += 1;
				countTimeUnit++;
			}
		}
		this.countTotalItemsCollected += countTimeUnit;
		
		return countTimeUnit;
		
	}
	
	public void countCollectedItemsAll(int iTimeUnitInitial, int iTimeUnitFinal) throws FileNotFoundException {
		
		 	PrintWriter out1 = new PrintWriter(this.sizeInfra + "." + this.numberFlows + "." + this.capacityFlow + "." + this.telemetryItemsRouter + "." + this.maxSizeTelemetryItemsRouter + ".itens.dat");
		
			for (int j = 0; j < infra.size; j++) {
				
				for (int k = 0; k < this.telemetryItemsRouter; k++) {
					int countTimeUnit = 0;
					
					for (int i = iTimeUnitInitial; i < iTimeUnitFinal; i++) {
					
						countTimeUnit += countItemsPerFlowTime[j][k][i];
						
					}
					
					out1.println(j + ";" + k + ";" + countTimeUnit);
					
					if (countTimeUnit > 0) {
						System.out.println("Router " + j + " Item " + k + " " + countTimeUnit);
					}
					
				}
				
			}
			out1.close();
			
	}

	public float getAverage(int iTimeUnit) {
		
		float avgGlobal = 0;
		float contItens = 0;
		
		for (int j = 0; j < sizeInfra; j++) {

			for (int k = 0; k < infra.telemetryItemsRouter; k++) {
				int last = -1;
				float avg = 0;
				int cont = 0;

				for (int i = 0; i <= iTimeUnit; i++) {

					if (infra.items[j][k] == 1 && countItemsPerFlowTime[j][k][i] == 1) {

						last += Math.abs(i - last);
						cont++;
						
					}

				}
				
				if (cont > 0 && last != -1) {
					avg = (float) last / (float) cont;
					avgGlobal += avg;
					
					contItens++;
				}
				
			}

		}
		
		if (contItens == 0) return 0;
		else return avgGlobal / (float) contItens; 
		
	}



}
