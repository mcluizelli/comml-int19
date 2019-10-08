
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

public class HeuristicLRU {

	NetworkInfrastructure infra;
	ArrayList<NetworkFlow> networkFlows;
	ArrayList<ArrayList<Tuple<Integer, Integer>>> prioritys = new ArrayList<ArrayList<Tuple<Integer, Integer>>>();// lista
	ArrayList<Tuple<Integer, Integer>> lastUsed = new ArrayList<Tuple<Integer, Integer>>();		//new																									// de
	int aux;
	int capacity;

	Random rnd;
	
	StatisticsOut mngStatistics = null;
	
	ArrayList<MonitoringApp> monitoringApps = null;

	public HeuristicLRU(NetworkInfrastructure infra, ArrayList<NetworkFlow> networkFlows, StatisticsOut mngStatistics, ArrayList<MonitoringApp> monitoringApps, long seed) {

		this.infra = infra;
		this.networkFlows = networkFlows;
		this.rnd = new Random(seed);
		this.mngStatistics = mngStatistics;
		this.monitoringApps = monitoringApps;

		// Initialize priority per flow
		for (int i = 0; i < this.networkFlows.size(); i++) {

			ArrayList<Tuple<Integer, Integer>> itemsPerRouter = new ArrayList<Tuple<Integer, Integer>>();
			ArrayList<Integer> path = networkFlows.get(i).path;

			for (int j = 0; j < path.size(); j++) {

				for (int k = 0; k < this.infra.telemetryItemsRouter; k++) {

					if (this.infra.items[path.get(j)][k] == 1) {

						Tuple<Integer, Integer> pair = new Tuple<Integer, Integer>(path.get(j), k);
						itemsPerRouter.add(pair);
					}

				}

			}

			prioritys.add(itemsPerRouter);

		}
		//new
		for(int i=0; i<infra.size;i++) { // inicia a fila com todos os itens presentes na infra
			for(int j=0; j<infra.telemetryItemsRouter;j++) {
				if(infra.items[i][j]==1) {
					Tuple<Integer, Integer> dp = new Tuple<Integer, Integer>(i, j);
					lastUsed.add(dp);
					//System.out.println("router: "+i+" item: "+j);
				}
			}
		}

	
	}
	
	
	public void resetCap() {
		for(int i=0;i<networkFlows.size();i++) {
			networkFlows.get(i).capacityVariable = networkFlows.get(i).capacityFlow;
			networkFlows.get(i).itens.clear();
			networkFlows.get(i).routers.clear();
		}
	}
	
	public void newLRU() {
		
		int contItens = 0;
		
		int[][] CollectedItems = new int[this.infra.size+1][this.infra.telemetryItemsRouter+1];

		
		int tag=0;
	
		resetCap();
		
		for(int i=0, cont = 0;cont<lastUsed.size();cont++, i++) {//percorre a fila de recentemente pegos
			
			for(int j=0; j<networkFlows.size();j++) {	//percorre os fluxos para tentar achar algum que possa pegar o primeiro
				
				//System.out.println("capacidade "+networkFlows.get(j).capacityVariable);
				for(int k=0; k<networkFlows.get(j).path.size(); k++) {
				
					if(infra.items[networkFlows.get(j).path.get(k)][lastUsed.get(i).y]==1 
							&& networkFlows.get(j).capacityVariable >= infra.sizeTelemetryItems[lastUsed.get(i).y]
							&& CollectedItems[networkFlows.get(j).path.get(k)][lastUsed.get(i).y] == 0) { 
						
						//testa se o dispositivo possui o item e se há capacidade para pegalo
						networkFlows.get(j).capacityVariable -= infra.sizeTelemetryItems[lastUsed.get(i).y];
						networkFlows.get(j).itens.add(lastUsed.get(i).y); //o que é lastUsed.get?
						networkFlows.get(j).routers.add(networkFlows.get(j).path.get(k));
						lastUsed.add(lastUsed.get(i));
						
						CollectedItems[networkFlows.get(j).path.get(k)][lastUsed.get(i).y] = 1;
						
						//keep track of statistics.
						if(this.mngStatistics != null) {
							int router = networkFlows.get(j).path.get(k);
							int item = lastUsed.get(i).y;
							
							String key = Integer.toString(router) + Integer.toString(item);
							if(mngStatistics.qtdCollectedItem.get(key) == null) {
								mngStatistics.qtdCollectedItem.put(key, (float) 1);
							}else {
								mngStatistics.qtdCollectedItem.put(key, mngStatistics.qtdCollectedItem.get(key) + (float) 1);
							}
						}
						
						lastUsed.remove(i);
						i--;
						tag=1;
						contItens++;
						
						
						break;
					}
				}
				if(tag==1) {
					tag=0;
					break;
				}
			}
		}
		
			
		
		int contSB = 0;
		int contMonApp = 0;
		
		if(this.monitoringApps != null) {
			
			//cont the number of spatial/temporal dependencies that were satisfied
			for(int i = 0; i < this.monitoringApps.size(); i++) {
				int contMonAppAux  = 0;
				ArrayList<ArrayList<Integer>> spatialReq = this.monitoringApps.get(i).spatialRequirements;
				
				for(int j = 0; j < spatialReq.size(); j++) {
					
					contMonAppAux += countSpatialReqSatisfied(spatialReq.get(j), i, j);
					
				}
				contSB += contMonAppAux;
				if (contMonAppAux == spatialReq.size()) contMonApp++;
				
			}
			
		}
		
		int contFlows = 0;
		//get flows' capacity
		for(int i = 0; i < this.networkFlows.size(); i++) {
			
			if (this.networkFlows.get(i).capacityVariable < this.networkFlows.get(i).capacityFlow) {
				contFlows++;
				
				float netFlowCapacityUsed = this.networkFlows.get(i).capacityFlow - this.networkFlows.get(i).capacityVariable; 
				
				if (mngStatistics.averageCapacityFlowsUsed.get(i) == null){
					mngStatistics.averageCapacityFlowsUsed.put(i, netFlowCapacityUsed);
				}else {
					float newValue = mngStatistics.averageCapacityFlowsUsed.get(i) + (float) netFlowCapacityUsed;
					mngStatistics.averageCapacityFlowsUsed.put(i, newValue);
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
	
	
	int countSpatialReqSatisfied(ArrayList<Integer> spatialReq, int m, int idSpatial) {
		
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
				//System.out.println( m + "," + k + "," + idSpatial);
			}
				
		}
			
		return countSpatialReq;
		
	}

	public void updateValues(Hashtable<String, Float> valuesNormalOp) {
		
		for(int i = 0; i < this.prioritys.size(); i++) {
			
			ArrayList<Tuple<Integer, Integer>> priorityFlow = this.prioritys.get(i);

			for (int j = 0; j < priorityFlow.size(); j++) {// percorre a prioridade do fluxo
				
				Tuple<Integer,Integer> item = priorityFlow.get(j);
				item.value = valuesNormalOp.get(Integer.toString(item.x) + Integer.toString(item.y));
			}
			
		}
		
		
	}
	
	public void runRnd() {

		int k = 0;
		int i = 0;

		int[] auxFlow = new int[networkFlows.size()];
		for (i = 0; i < networkFlows.size(); i++)
			auxFlow[i] = 0;

		while (k < networkFlows.size()) {

			do {

				i = rnd.nextInt(networkFlows.size());

			} while (auxFlow[i] == 1);
			k++;
			auxFlow[i] = 1;

			networkFlows.get(i).itens.clear();
			networkFlows.get(i).routers.clear();

			capacity = networkFlows.get(i).capacityFlow; // pega a capacity do fluxo para modificar
			
			ArrayList<Tuple<Integer, Integer>> priorityFlow = this.prioritys.get(i);

			for (int j = 0; j < priorityFlow.size(); j++) {// percorre a prioridade do fluxo
				
				Tuple<Integer,Integer> potencialItem = priorityFlow.get(j);
				
				if( this.infra.sizeTelemetryItems[potencialItem.y] <= capacity ) {
					
					//add item to the current flow and update capacity
					networkFlows.get(i).itens.add(potencialItem.y);
					networkFlows.get(i).routers.add(potencialItem.x);
					capacity -= this.infra.sizeTelemetryItems[potencialItem.y];
					
					for(int l = 0; l < this.prioritys.size(); l++) {
						
						ArrayList<Tuple<Integer, Integer>> auxPriority = this.prioritys.get(l);
						
						for(int m = 0; m < auxPriority.size(); m++) {
							
							if(auxPriority.get(m).x == potencialItem.x && auxPriority.get(m).y == potencialItem.y) {
								
								Tuple<Integer,Integer> auxItem = auxPriority.get(m);
								auxPriority.remove(m);
								auxPriority.add(auxItem);
								
							}
							
						}
				
					}
					
					//place tuple at the end of priority list
					//priorityFlow.remove(j);
					//priorityFlow.add(potencialItem);
					j--;
					
					
					
				}
				
			}
			
			
		}
		
		

	}
	



}
