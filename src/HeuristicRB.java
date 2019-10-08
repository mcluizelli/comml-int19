import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

public class HeuristicRB {
	NetworkInfrastructure infra;
	ArrayList <NetworkFlow> networkFlows;
	ArrayList<Tuple<Integer, Integer>> priorityTelItems = new ArrayList<Tuple<Integer, Integer>>();
	int[][] CollectedItems; //tells wheter an item has been collected or not;
	Random rnd;
	
	StatisticsOut mngStatistics = null;
	ArrayList<MonitoringApp> monitoringApps = null;
	
	public HeuristicRB(NetworkInfrastructure infra, ArrayList<NetworkFlow> networkFlows, long seed){ 
		this.rnd = new Random(seed);
		this.infra = infra;
		this.networkFlows = networkFlows;
		this.CollectedItems = new int[this.infra.size][this.infra.telemetryItemsRouter];

		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				this.CollectedItems[i][j] = 0;	
			}	
		}
		
	}
	
	//this constructor considers StatisticsOut
	public HeuristicRB(NetworkInfrastructure infra, ArrayList<NetworkFlow> networkFlows
					, ArrayList<MonitoringApp> monitoringApps
					, StatisticsOut mngStatistics, long seed){ 
		this.rnd = new Random(seed);
		this.infra = infra;
		this.networkFlows = networkFlows;
		this.CollectedItems = new int[this.infra.size][this.infra.telemetryItemsRouter];
		this.mngStatistics = mngStatistics;
		this.monitoringApps = monitoringApps;
		
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				this.CollectedItems[i][j] = 0;	
			}	
		}
		
	}
	
	//reset flows' capacity
	public void resetCap() {
		for(int i=0;i<networkFlows.size();i++) {
			networkFlows.get(i).capacityVariable = networkFlows.get(i).capacityFlow;
			networkFlows.get(i).itens.clear();
			networkFlows.get(i).routers.clear();
		}
	}
	
	public void runRB_NoDependencies(int[][] oldCollectedItems) {
		
		int contItens = 0;
		int contMonApp = 0;	
		int[] flowCapacity = new int[networkFlows.size()];
			
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			flowCapacity[i] = this.networkFlows.get(i).capacityFlow;
			
		}
		
		resetCap();
		
		for(int flow = 0; flow < this.networkFlows.size(); flow++) {
			for(int iRouter = 0; iRouter < this.networkFlows.get(flow).path.size(); iRouter++) {
				for(int item = 0; item < this.infra.telemetryItemsRouter; item++) { //items
					
					int router = this.networkFlows.get(flow).path.get(iRouter); 
					
					if(this.infra.items[router][item] == 1) { //checks wheter the router has the item or not
						if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 0 &&
								this.infra.sizeTelemetryItems[item] <= flowCapacity[flow]) {
						
							contItens++;
							flowCapacity[flow] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
							CollectedItems[router][item] = 1; //trace item to be collected;
							
							//keep track of statistics.
							if(this.mngStatistics != null) {
								String key = Integer.toString(router) + Integer.toString(item);
								if(mngStatistics.qtdCollectedItem.get(key) == null) {
									mngStatistics.qtdCollectedItem.put(key, (float) 1);
								}else {
									mngStatistics.qtdCollectedItem.put(key, mngStatistics.qtdCollectedItem.get(key) + (float) 1);
								}
							}
							
							this.networkFlows.get(flow).routers.add(router);
							this.networkFlows.get(flow).itens.add(item);
						}
					}
				}
			}
		}
		
		for(int flow = 0; flow < this.networkFlows.size(); flow++) {
			for(int iRouter = 0; iRouter < this.networkFlows.get(flow).path.size(); iRouter++) {
				for(int item = 0; item < this.infra.telemetryItemsRouter; item++) { //items
					int router = this.networkFlows.get(flow).path.get(iRouter); 

					if(this.infra.items[router][item] == 1) { //checks wheter the router has the item or not						
						if (CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 1 &&
								this.infra.sizeTelemetryItems[item] <= flowCapacity[flow]) {
							
							contItens++;
							CollectedItems[router][item] = 1;
							flowCapacity[flow] -= this.infra.sizeTelemetryItems[item];
							
							//keep track of statistics.
							if(this.mngStatistics != null) {
								String key = Integer.toString(router) + Integer.toString(item);
								if(mngStatistics.qtdCollectedItem.get(key) == null) {
									mngStatistics.qtdCollectedItem.put(key, (float) 1);
								}else {
									mngStatistics.qtdCollectedItem.put(key, mngStatistics.qtdCollectedItem.get(key) + (float) 1);
								}
							}
							this.networkFlows.get(flow).routers.add(router);
							this.networkFlows.get(flow).itens.add(item);	
						}
					}
				}
			}
		}
		
		
		
		int contSB = 0;
		if(this.monitoringApps != null) {
			
			//cont the number of spatial/temporal dependencies that were satisfied
			for(int i = 0; i < this.monitoringApps.size(); i++) {
				int contMonAppAux  = 0;
				ArrayList<ArrayList<Integer>> spatialReq = this.monitoringApps.get(i).spatialRequirements;
				
				for(int j = 0; j < spatialReq.size(); j++) {
					
					contMonAppAux += countSpatialReqSatisfied(spatialReq.get(j));
					
				}
				contSB += contMonAppAux;
				if (contMonAppAux == spatialReq.size()) contMonApp++;
				
			}
			
		}
		int contFlows = 0;
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			if (flowCapacity[i] < this.networkFlows.get(i).capacityFlow) {
				contFlows++;
				float netFlowCapacityUsed = (float) this.networkFlows.get(i).capacityFlow - (float)flowCapacity[i];
				if (mngStatistics.averageCapacityFlowsUsed.get(i) == null){
					mngStatistics.averageCapacityFlowsUsed.put(i, netFlowCapacityUsed);
				}else {
					mngStatistics.averageCapacityFlowsUsed.put(i, mngStatistics.averageCapacityFlowsUsed.get(i) + (float) netFlowCapacityUsed);
				}
						
				if (mngStatistics.qtdFlowsUsed.get(i) == null){
					mngStatistics.qtdFlowsUsed.put(i, 1);
				}else {
					mngStatistics.qtdFlowsUsed.put(i, mngStatistics.qtdFlowsUsed.get(i) + 1);
				}
					
			}
		}
				
		
		mngStatistics.contItems = contItens;
		mngStatistics.contSB = contSB;
		mngStatistics.contFlows = contFlows;
		mngStatistics.contMonApp = contMonApp;
	
	}
	
	

	Hashtable<Integer, Integer> getItemsRouter(int iRouter){
		
		Hashtable<Integer, Integer> listItems = new Hashtable<Integer, Integer>();
		
		for(int iFlow = 0; iFlow < this.networkFlows.size(); iFlow++) {
			
			for(int i = 0; i < this.networkFlows.get(iFlow).routers.size(); i++) {
				
				if(this.networkFlows.get(iFlow).routers.get(i) == iRouter) {
					listItems.put(this.networkFlows.get(iFlow).itens.get(i), iRouter);
				}
				
			}
			
		}
		
		return listItems; 
		
	}
	
	
	int countSpatialReqSatisfied(ArrayList<Integer> spatialReq) {
		
		int countSpatialReq = 0;
		
		for(int k = 0; k < this.infra.size; k++) {
		
			Hashtable<Integer, Integer> listItems = getItemsRouter(k);
			int contItemSpatial = 0;
			
			for(int i = 0; i < spatialReq.size(); i++) {
			
				if (listItems.get(spatialReq.get(i)) != null) {
					contItemSpatial++;		
				}
			}
				
			if ( contItemSpatial == spatialReq.size() ) {
				countSpatialReq++;
				
			}
				
		}
			
		return countSpatialReq;
		
	}
	
	
	//teste para verificar como estao os items em this.infra.items[i][j]
	public void testeInfra() {
		for(int p = 0; p < this.infra.size; p++) {
			for(int q = 0; q < this.infra.telemetryItemsRouter; q++) {
				//System.out.println("infra["+p+"]["+q+"]= " + this.infra.items[p][q]);
			}
		}	
	}
	
	
	//reset matrix;
	public void resetMatrix(int[][] matrix) {
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				matrix[i][j] = 0;
			}
		}
	}
	
	
	//copy telemetry items to old matrix;
	public void copyToOldCollectedItems(int[][] oldCollectedItems) {
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				//System.out.println("collected[" + i + "][" + j + "] = " + CollectedItems[i][j]);
				if(CollectedItems[i][j] == 1) {
					oldCollectedItems[i][j] = 1;
				}
				else{
					oldCollectedItems[i][j] = CollectedItems[i][j];
				}
				//System.out.println("old[" + i + "][" + j + "] = " + oldCollectedItems[i][j]);
			}
		}
	}


	//clean collected items at every iteration;
	public void resetFlowItems() {
		for(int j = 0; j < networkFlows.size(); j++) {
			if(networkFlows.get(j).itens.size() != 0) {
				networkFlows.get(j).itens.clear();
			}
		}
	}
	
	
	//print the results of an iteration;
	public void printSolution() {
		for(int j = 0; j < networkFlows.size(); j++) {
			for(int k = 0; k < networkFlows.get(j).itens.size(); k++) {
				if (networkFlows.get(j).itens.get(k) != 0) {
					System.out.println("Flow " + j + " router: " + networkFlows.get(j).routers.get(k) + " item: " + networkFlows.get(j).itens.get(k));
				}
			}
		}	
	}
	
}
