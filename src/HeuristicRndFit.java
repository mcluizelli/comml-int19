import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

public class HeuristicRndFit {
	NetworkInfrastructure infra;
	ArrayList <NetworkFlow> networkFlows;
	ArrayList<Tuple<Integer, Integer>> priorityTelItems = new ArrayList<Tuple<Integer, Integer>>();
	int[][] CollectedItems; //tells wheter an item has been collected or not;
	Random rnd;
	
	StatisticsOut mngStatistics = null;
	
	ArrayList<MonitoringApp> monitoringApps = null;
	
	//maping of router+item to list of flows
	Hashtable<String, ArrayList<Integer>> itemToNetFlowMapping;
	
	
	
	public HeuristicRndFit(NetworkInfrastructure infra, ArrayList<NetworkFlow> networkFlows, long seed){ 
		this.rnd = new Random(seed);
		this.infra = infra;
		this.networkFlows = networkFlows;
		this.CollectedItems = new int[this.infra.size][this.infra.telemetryItemsRouter];

		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				this.CollectedItems[i][j] = 0;	
			}	
		}
		
		this.itemToNetFlowMapping = new Hashtable<String, ArrayList<Integer>>();
		countNetworkFlowPerRouter();
		
	}
	
	//this constructor considers StatisticsOut
	public HeuristicRndFit(NetworkInfrastructure infra, ArrayList<NetworkFlow> networkFlows
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
		
		this.itemToNetFlowMapping = new Hashtable<String, ArrayList<Integer>>();
		countNetworkFlowPerRouter();
		
	}
	
	public void resetCap() {
		for(int i=0;i<networkFlows.size();i++) {
			networkFlows.get(i).capacityVariable = networkFlows.get(i).capacityFlow;
			networkFlows.get(i).itens.clear();
			networkFlows.get(i).routers.clear();
		}
	}
	
	private void countNetworkFlowPerRouter() {
		
		for(int i = 0; i < this.networkFlows.size(); i++) {
			
			for(int j = 0; j < this.networkFlows.get(i).path.size(); j++) {
				
				int router = this.networkFlows.get(i).path.get(j);
				
				for(int k = 0; k < this.infra.telemetryItemsRouter; k++) {
					
					if(this.infra.items[router][k] == 1) {
						
						String key = Integer.toString(router) + Integer.toString(k);
						
						if(this.itemToNetFlowMapping.get(key) == null) {
							
							ArrayList<Integer> networkFlowsList = new ArrayList<Integer>();
							networkFlowsList.add(i);
							this.itemToNetFlowMapping.put(key, networkFlowsList);
							
						}else {
							ArrayList<Integer> networkFlowsList = this.itemToNetFlowMapping.get(key);
							networkFlowsList.add(i);
							this.itemToNetFlowMapping.put(key, networkFlowsList);
						}
						
						
					}
					
				}
				
				
			}
			
		}
		
	
		
	}
	
		
	//This approach tries to fulfill the first flow before moving onto the next one.
	public void runRndFit(int[][] oldCollectedItems) {
		
		int contItens = 0;
		int contMonApp = 0;
		
		int[] flowCapacity = new int[networkFlows.size()];
		
		
			
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			flowCapacity[i] = this.networkFlows.get(i).capacityFlow;
		}
		
		resetCap();
		
		int router;
		int item;
		int attempFailure = 0;
		
		while(attempFailure < 500) {
			//System.out.println(attempFailure);
			String key; 
			do {
				
				router = this.rnd.nextInt(this.infra.size);
				item   = this.rnd.nextInt(this.infra.telemetryItemsRouter);
				key = Integer.toString(router) + Integer.toString(item);
				
			}while(this.itemToNetFlowMapping.get(key) == null);
			
			ArrayList<Integer> potentialNetFlows = this.itemToNetFlowMapping.get(key);
			
			if(!potentialNetFlows.isEmpty()) {
				
				int chosenNetFlow = potentialNetFlows.get( rnd.nextInt(potentialNetFlows.size()) );
				
				if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 0 &&
						this.infra.sizeTelemetryItems[item] <= flowCapacity[chosenNetFlow]) {
					
					attempFailure = 0;
					
					contItens++;
					flowCapacity[chosenNetFlow] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
					CollectedItems[router][item] = 1; //trace item to be collected;
					
					//keep track of statistics.
					if(this.mngStatistics != null) {
						String keyStat = Integer.toString(router) + Integer.toString(item);
						if(mngStatistics.qtdCollectedItem.get(keyStat) == null) {
							mngStatistics.qtdCollectedItem.put(keyStat, (float) 1);
						}else {
							mngStatistics.qtdCollectedItem.put(keyStat, mngStatistics.qtdCollectedItem.get(keyStat) + (float) 1);
						}
					}
					
					this.networkFlows.get(chosenNetFlow).routers.add(router);
					this.networkFlows.get(chosenNetFlow).itens.add(item);
					
					//System.out.println(chosenNetFlow + " " + router + " " + item);
					
				}else {
					attempFailure++;
				}
					
			}else {
				attempFailure++;
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
				
				float netFlowCapacityUsed = this.networkFlows.get(i).capacityFlow - this.networkFlows.get(i).capacityVariable; 
				
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
	
	public void concentra_old(int[][] oldCollectedItems) {
		
		int contItens = 0;
		int contMonApp = 0;
		
		int[] flowCapacity = new int[networkFlows.size()];
		
			
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			flowCapacity[i] = this.networkFlows.get(i).capacityFlow;
		}
		
		resetCap();
		
		int router;
		
		
		for(int i = 0; i < this.networkFlows.size(); i++) {
			
			for(int j = 0; j < this.networkFlows.get(i).path.size(); j++) {
			
				router = this.networkFlows.get(i).path.get(j);
				
				for(int item = 0; item < this.infra.telemetryItemsRouter; item++) {
					
					if(this.infra.items[router][item] == 1) {
						
						if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 0 &&
								this.infra.sizeTelemetryItems[item] <= flowCapacity[i]) {
						
						
							contItens++;
							flowCapacity[i] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
							CollectedItems[router][item] = 1; //trace item to be collected;
							
							//keep track of statistics.
							if(this.mngStatistics != null) {
								String keyStat = Integer.toString(router) + Integer.toString(item);
								if(mngStatistics.qtdCollectedItem.get(keyStat) == null) {
									mngStatistics.qtdCollectedItem.put(keyStat, (float) 1);
								}else {
									mngStatistics.qtdCollectedItem.put(keyStat, mngStatistics.qtdCollectedItem.get(keyStat) + (float) 1);
								}
							}
							
							this.networkFlows.get(i).routers.add(router);
							this.networkFlows.get(i).itens.add(item);
							
						
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
	
	
	public void distribui(int[][] oldCollectedItems) {
		
		int contItens = 0;
		int contMonApp = 0;
		
		int[] flowCapacity = new int[networkFlows.size()];
		
			
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			flowCapacity[i] = this.networkFlows.get(i).capacityFlow;
		}
		
		resetCap();
		
		int router;
		
		
		for(router = 0; router < this.infra.size; router++) {
			
			for(int item = 0; item < this.infra.telemetryItemsRouter; item++) {
				
				if(this.infra.items[router][item] == 1) {
					
					if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 0 ){
						
						String key = Integer.toString(router) + Integer.toString(item);
						ArrayList<Integer> potentialNetFlows = this.itemToNetFlowMapping.get(key);
						if(potentialNetFlows != null) {
							
							int currentCapacity = Integer.MIN_VALUE;
							int bestFlow = -1;
							
							
							for(int i = 0; i < potentialNetFlows.size(); i++) {
								
								if (this.infra.sizeTelemetryItems[item] <= flowCapacity[potentialNetFlows.get(i)] && flowCapacity[potentialNetFlows.get(i)] > currentCapacity) {
									
									bestFlow = potentialNetFlows.get(i);
									currentCapacity = flowCapacity[potentialNetFlows.get(i)];
									
									
								}
								
							}
							
							if(bestFlow != -1) {
								
								contItens++;
								flowCapacity[bestFlow] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
								CollectedItems[router][item] = 1; //trace item to be collected;
								
								//keep track of statistics.
								if(this.mngStatistics != null) {
									String keyStat = Integer.toString(router) + Integer.toString(item);
									if(mngStatistics.qtdCollectedItem.get(keyStat) == null) {
										mngStatistics.qtdCollectedItem.put(keyStat, (float) 1);
									}else {
										mngStatistics.qtdCollectedItem.put(keyStat, mngStatistics.qtdCollectedItem.get(keyStat) + (float) 1);
									}
								}
								
								this.networkFlows.get(bestFlow).routers.add(router);
								this.networkFlows.get(bestFlow).itens.add(item);
								
							
								
							}
							
						}
							
						
					}
					
				}
				
			}
			
		}
		
		
		for(router = 0; router < this.infra.size; router++) {
			
			for(int item = 0; item < this.infra.telemetryItemsRouter; item++) {
				
				if(this.infra.items[router][item] == 1) {
					
					if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 1 ){
						
						
						String key = Integer.toString(router) + Integer.toString(item);
						ArrayList<Integer> potentialNetFlows = this.itemToNetFlowMapping.get(key);
						if(potentialNetFlows != null) {
							
							int currentCapacity = Integer.MIN_VALUE;
							int bestFlow = -1;
							
							for(int i = 0; i < potentialNetFlows.size(); i++) {
								
								if (this.infra.sizeTelemetryItems[item] <= flowCapacity[potentialNetFlows.get(i)] && flowCapacity[potentialNetFlows.get(i)] > currentCapacity) {
									
									bestFlow = potentialNetFlows.get(i);
									currentCapacity = flowCapacity[potentialNetFlows.get(i)];
									
								}
								
							}
							
							if(bestFlow != -1) {
								
								contItens++;
								flowCapacity[bestFlow] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
								CollectedItems[router][item] = 1; //trace item to be collected;
								
								//keep track of statistics.
								if(this.mngStatistics != null) {
									String keyStat = Integer.toString(router) + Integer.toString(item);
									if(mngStatistics.qtdCollectedItem.get(keyStat) == null) {
										mngStatistics.qtdCollectedItem.put(keyStat, (float) 1);
									}else {
										mngStatistics.qtdCollectedItem.put(keyStat, mngStatistics.qtdCollectedItem.get(keyStat) + (float) 1);
									}
								}
								
								this.networkFlows.get(bestFlow).routers.add(router);
								this.networkFlows.get(bestFlow).itens.add(item);
								
							
								
							}
							
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
	
	
	public void concentra(int[][] oldCollectedItems) {
		
		int contItens = 0;
		int contMonApp = 0;
		
		int[] flowCapacity = new int[networkFlows.size()];
		
			
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			flowCapacity[i] = this.networkFlows.get(i).capacityFlow;
		}
		
		resetCap();
		
		int router;
		
		
		for(router = 0; router < this.infra.size; router++) {
			
			for(int item = 0; item < this.infra.telemetryItemsRouter; item++) {
				
				if(this.infra.items[router][item] == 1) {
					
					if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 0 ){
						
						String key = Integer.toString(router) + Integer.toString(item);
						ArrayList<Integer> potentialNetFlows = this.itemToNetFlowMapping.get(key);
						if(potentialNetFlows != null) {
							
							int currentCapacity = Integer.MAX_VALUE;
							int bestFlow = -1;
							
							
							for(int i = 0; i < potentialNetFlows.size(); i++) {
								
								if (this.infra.sizeTelemetryItems[item] <= flowCapacity[potentialNetFlows.get(i)] && flowCapacity[potentialNetFlows.get(i)] < currentCapacity) {
									
									bestFlow = potentialNetFlows.get(i);
									currentCapacity = flowCapacity[potentialNetFlows.get(i)];
									
									
								}
								
							}
							
							if(bestFlow != -1) {
								
								contItens++;
								flowCapacity[bestFlow] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
								CollectedItems[router][item] = 1; //trace item to be collected;
								
								//keep track of statistics.
								if(this.mngStatistics != null) {
									String keyStat = Integer.toString(router) + Integer.toString(item);
									if(mngStatistics.qtdCollectedItem.get(keyStat) == null) {
										mngStatistics.qtdCollectedItem.put(keyStat, (float) 1);
									}else {
										mngStatistics.qtdCollectedItem.put(keyStat, mngStatistics.qtdCollectedItem.get(keyStat) + (float) 1);
									}
								}
								
								this.networkFlows.get(bestFlow).routers.add(router);
								this.networkFlows.get(bestFlow).itens.add(item);
								
							
								
							}
							
						}
							
						
					}
					
				}
				
			}
			
		}
		
		
		for(router = 0; router < this.infra.size; router++) {
			
			for(int item = 0; item < this.infra.telemetryItemsRouter; item++) {
				
				if(this.infra.items[router][item] == 1) {
					
					if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 1 ){
						
						
						String key = Integer.toString(router) + Integer.toString(item);
						ArrayList<Integer> potentialNetFlows = this.itemToNetFlowMapping.get(key);
						if(potentialNetFlows != null) {
							
							int currentCapacity = Integer.MAX_VALUE;
							int bestFlow = -1;
							
							for(int i = 0; i < potentialNetFlows.size(); i++) {
								
								if (this.infra.sizeTelemetryItems[item] <= flowCapacity[potentialNetFlows.get(i)] && flowCapacity[potentialNetFlows.get(i)] < currentCapacity) {
									
									bestFlow = potentialNetFlows.get(i);
									currentCapacity = flowCapacity[potentialNetFlows.get(i)];
									
								}
								
							}
							
							if(bestFlow != -1) {
								
								contItens++;
								flowCapacity[bestFlow] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
								CollectedItems[router][item] = 1; //trace item to be collected;
								
								//keep track of statistics.
								if(this.mngStatistics != null) {
									String keyStat = Integer.toString(router) + Integer.toString(item);
									if(mngStatistics.qtdCollectedItem.get(keyStat) == null) {
										mngStatistics.qtdCollectedItem.put(keyStat, (float) 1);
									}else {
										mngStatistics.qtdCollectedItem.put(keyStat, mngStatistics.qtdCollectedItem.get(keyStat) + (float) 1);
									}
								}
								
								this.networkFlows.get(bestFlow).routers.add(router);
								this.networkFlows.get(bestFlow).itens.add(item);
								
							
								
							}
							
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
	
	
	public void runFirstFit(int[][] oldCollectedItems) {
		
		int contItens = 0;
		int contMonApp = 0;
		
		int[] flowCapacity = new int[networkFlows.size()];
		
			
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			flowCapacity[i] = this.networkFlows.get(i).capacityFlow;
		}
		
		resetCap();
		
		int router;
		int item;
		int attempFailure = 0;
		
		while(attempFailure < 2000) {
			//System.out.println(attempFailure);
			String key; 
			do {
				
				router = this.rnd.nextInt(this.infra.size);
				item   = this.rnd.nextInt(this.infra.telemetryItemsRouter);
				key = Integer.toString(router) + Integer.toString(item);
				
			}while(this.itemToNetFlowMapping.get(key) == null);
			
			ArrayList<Integer> potentialNetFlows = this.itemToNetFlowMapping.get(key);
			
			if(!potentialNetFlows.isEmpty()) {
				
				for(int i = 0; i < potentialNetFlows.size(); i++) {
					
					int chosenNetFlow = potentialNetFlows.get( i );
					
					if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 0 &&
							this.infra.sizeTelemetryItems[item] <= flowCapacity[chosenNetFlow]) {
						
						attempFailure = 0;
						
						contItens++;
						flowCapacity[chosenNetFlow] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
						CollectedItems[router][item] = 1; //trace item to be collected;
						
						//keep track of statistics.
						if(this.mngStatistics != null) {
							String keyStat = Integer.toString(router) + Integer.toString(item);
							if(mngStatistics.qtdCollectedItem.get(keyStat) == null) {
								mngStatistics.qtdCollectedItem.put(keyStat, (float) 1);
							}else {
								mngStatistics.qtdCollectedItem.put(key, mngStatistics.qtdCollectedItem.get(keyStat) + (float) 1);
							}
						}
						
						this.networkFlows.get(chosenNetFlow).routers.add(router);
						this.networkFlows.get(chosenNetFlow).itens.add(item);
						
						//System.out.println(chosenNetFlow + " " + router + " " + item);
						
						break;
						
					}else {
						attempFailure++;
					}
				
				
				}
				
				
					
			}else {
				attempFailure++;
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
	
	public void runBestFit(int[][] oldCollectedItems) {
		
		int contItens = 0;
		int contMonApp = 0;
		
		int[] flowCapacity = new int[networkFlows.size()];
		
			
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			flowCapacity[i] = this.networkFlows.get(i).capacityFlow;
			
		}
		
		resetCap();
		
		int router;
		int item;
		int attempFailure = 0;
		
		while(attempFailure < 2000) {
			//System.out.println(attempFailure);
			String key; 
			do {
				
				router = this.rnd.nextInt(this.infra.size);
				item   = this.rnd.nextInt(this.infra.telemetryItemsRouter);
				key = Integer.toString(router) + Integer.toString(item);
				
			}while(this.itemToNetFlowMapping.get(key) == null);
			
			ArrayList<Integer> potentialNetFlows = this.itemToNetFlowMapping.get(key);
			
			if(!potentialNetFlows.isEmpty()) {
				
				//Try first those network with higher available capacity
				for(int i = 0; i < potentialNetFlows.size(); i++) {
					
					int chosenNetFlow = potentialNetFlows.get( i );
					
					if(CollectedItems[router][item] == 0 && oldCollectedItems[router][item] == 0 &&
							this.infra.sizeTelemetryItems[item] <= flowCapacity[chosenNetFlow]) {
						
						attempFailure = 0;
						
						contItens++;
						flowCapacity[chosenNetFlow] -= this.infra.sizeTelemetryItems[item]; //update flow's capacity
						CollectedItems[router][item] = 1; //trace item to be collected;
						
						
						
						//keep track of statistics.
						if(this.mngStatistics != null) {
							String keyStat = Integer.toString(router) + Integer.toString(item);
							if(mngStatistics.qtdCollectedItem.get(keyStat) == null) {
								mngStatistics.qtdCollectedItem.put(keyStat, (float) 1);
							}else {
								mngStatistics.qtdCollectedItem.put(key, mngStatistics.qtdCollectedItem.get(keyStat) + (float) 1);
							}
						}
						
						this.networkFlows.get(chosenNetFlow).routers.add(router);
						this.networkFlows.get(chosenNetFlow).itens.add(item);
						
						//System.out.println(chosenNetFlow + " " + router + " " + item);
						
						break;
						
					}else {
						attempFailure++;
					}
				
				
				}
				
				
					
			}else {
				attempFailure++;
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
		Random rnd = new Random();
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				//System.out.println("collected[" + i + "][" + j + "] = " + CollectedItems[i][j]);
				if(CollectedItems[i][j] == 1) {
					oldCollectedItems[i][j] = 1; //range = [1,6], randomize items that may be collected again. If == 1 (true), Else (false)
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