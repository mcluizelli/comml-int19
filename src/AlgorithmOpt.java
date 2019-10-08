import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import ilog.concert.*;
import ilog.cplex.*;

import java.io.*;

public class AlgorithmOpt {

	NetworkInfrastructure infra;
	ArrayList<NetworkFlow> networkFlows;
	ArrayList<MonitoringApp> monitoringApps;
	int numMaxSpatialDependencies;
	//Hashtable<ArrayList<Integer>, KMeans> clusteringList;
	
	IloCplex cplex = null;

	int prevCollectedItems[][][];
	
	ArrayList<ArrayList<Integer>> priorityList = new ArrayList<ArrayList<Integer>>();
	ArrayList<ArrayList<Integer>> priorityListCollected = new ArrayList<ArrayList<Integer>>();
	
	ArrayList<Tuple<Integer, Integer>> prioritys = new ArrayList<Tuple<Integer, Integer>>();// lista

	public AlgorithmOpt(IloCplex cplex, NetworkInfrastructure infra, ArrayList<NetworkFlow> networkFlows, 
						ArrayList<MonitoringApp> monitoringApps, int numMaxSpatialDependencies) {
		
		this.cplex = cplex;
		this.infra = infra;
		this.networkFlows = networkFlows;
		this.monitoringApps = monitoringApps;
		this.prevCollectedItems = new int[monitoringApps.size()][infra.size][numMaxSpatialDependencies];
		this.numMaxSpatialDependencies = numMaxSpatialDependencies;
		//this.clusteringList = clusteringList;
		
		resetPrevCollectedItems();
		
		priorityListInit();

	}
	
	//m d p
	private void moveToCollected(int i, int j, int k) {
		
		for(int cont = 0; cont < this.priorityList.size(); cont++) {
			
			if(this.priorityList.get(cont).get(0) == i 
					&& this.priorityList.get(cont).get(1) == j
					&& this.priorityList.get(cont).get(2) == k) {
				
				this.priorityListCollected.add(this.priorityList.get(cont));
				this.priorityList.remove(cont);
				break;
				
			}
		}
		
		if(this.priorityList.size() == 0) {
			this.priorityList.addAll(this.priorityListCollected);
			this.priorityListCollected.clear();
		}
		
	}
	
	private boolean routerSatifySpatialRequirement(int iRouter, ArrayList<Integer> spatialRequirement) {
		
		boolean satisfy = true;
		
		for(int i = 0; i < spatialRequirement.size();i++) {
			
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				
				if(this.infra.items[iRouter][spatialRequirement.get(i)] == 0) {
					return false;
				}
		
			}
			
		}
		
		return satisfy;
	}
	
	public boolean existItem(int i, int j, int k, ArrayList<ArrayList<Integer>> priorityList) {
		
		for(ArrayList<Integer> p : priorityList) {
			if(p.get(0) == i && p.get(1) == j && p.get(2) == k) {
				return true;
			}
		}
		
		return false;
		
	}
	
	public boolean flowSatisfyDemandSpatialRequirement(int idNetFlow, ArrayList<Integer> spatialRequirement) {
		
		int demand = 0;
		int flowCapacity = this.networkFlows.get(idNetFlow).capacityFlow;
		for(int i = 0; i < spatialRequirement.size(); i++) {
			
			demand += this.infra.sizeTelemetryItems[spatialRequirement.get(i)];
			
		}
		
		if (demand > flowCapacity) return false;
		else return true;
		
	}
	
	public void priorityListInit() {
		
		for(int i = 0; i < this.monitoringApps.size(); i++) {
			
			int numSpatialDependencies = this.monitoringApps.get(i).spatialRequirements.size();
			for(int k = 0; k < numSpatialDependencies; k++) {
			
				for(int l = 0; l < this.networkFlows.size(); l++) {
					for(int j = 0; j < this.networkFlows.get(l).path.size(); j++) {
						
						if(routerSatifySpatialRequirement(this.networkFlows.get(l).path.get(j) ,this.monitoringApps.get(i).spatialRequirements.get(k))
							&& flowSatisfyDemandSpatialRequirement(l, this.monitoringApps.get(i).spatialRequirements.get(k))) {
							
							//if(!existItem(i, this.networkFlows.get(l).path.get(j), k, this.priorityList)) {
								ArrayList<Integer> priorityItem = new ArrayList<Integer>();
								priorityItem.add(i);
								priorityItem.add(this.networkFlows.get(l).path.get(j));
								priorityItem.add(k);
								this.priorityList.add(priorityItem);
							//}
							
						}
					}
					
				}
				
			}
			
		}
		
		Collections.shuffle(this.priorityList);
		
	}
	
	void resetPrevCollectedItems() {
		
		for(int i = 0; i < this.monitoringApps.size(); i++) {
			
			for(int j = 0; j < this.infra.size; j++) {
				
				for(int k = 0; k < this.numMaxSpatialDependencies; k++) {
					
					this.prevCollectedItems[i][j][k] = 1; 
					
				}
			}
		}
	}
	
	public void updateValues(Hashtable<String, Float> valuesNormalOp) {
		
		for(int i = 0; i < this.prioritys.size(); i++) {
			
			Tuple<Integer,Integer> item = this.prioritys.get(i);

			item.value = valuesNormalOp.get(Integer.toString(item.x) + Integer.toString(item.y));
			
		}
		
	}

	void run(Hashtable<String, Float> valuesNormalOp, int currentTimeUnit, StatisticsOut mngStatistics) throws IloException {

		buildAndRunOnline(valuesNormalOp, currentTimeUnit, mngStatistics);
		//buildAndRunOnlineRelaxed();
	}

	
	void buildAndRunOnline(Hashtable<String, Float> valuesNormalOp, int currentTimeUnit, StatisticsOut mngStatistics) throws IloException {

		this.cplex.setParam(IloCplex.IntParam.Threads, 4);
		//this.cplex.setParam(IloCplex.DoubleParam.TiLim, 500);
		cplex.setOut(null);
		this.cplex.clearModel();

		//Variables
		IloNumVar[][][] y   = new IloNumVar[infra.size][infra.telemetryItemsRouter][networkFlows.size()];
		IloNumVar[][][] s   = new IloNumVar[this.monitoringApps.size()][infra.size][this.numMaxSpatialDependencies];
		IloNumVar[][][] sb  = new IloNumVar[this.monitoringApps.size()][infra.size][this.numMaxSpatialDependencies]; 
		
		IloNumVar[][] sbA  = new IloNumVar[this.monitoringApps.size()][this.numMaxSpatialDependencies]; 
		
		IloNumVar[][] t  = new IloNumVar[this.monitoringApps.size()][this.numMaxSpatialDependencies];
		IloNumVar[][] tb = new IloNumVar[this.monitoringApps.size()][this.numMaxSpatialDependencies];
		
		//Allocating memory to variables
		for (int i = 0; i < infra.size; i++) {
			for (int j = 0; j < infra.telemetryItemsRouter; j++) {
				y[i][j] = cplex.boolVarArray(networkFlows.size());
			}
		}
		
		for(int i = 0; i < this.monitoringApps.size(); i++) {
			
			t[i]  = cplex.numVarArray(this.numMaxSpatialDependencies, 0, Double.MAX_VALUE);
			tb[i] = cplex.boolVarArray(this.numMaxSpatialDependencies);
			sbA[i]   = cplex.numVarArray(this.numMaxSpatialDependencies, 0 , Double.MAX_VALUE);
			
			for(int j = 0; j < this.infra.size; j++) {
				
				s[i][j] = cplex.numVarArray(this.numMaxSpatialDependencies, 0, Double.MAX_VALUE);
				sb[i][j] = cplex.boolVarArray(this.numMaxSpatialDependencies);
			}
			
		}

		// Constraint set (2)
		int iRouter = 0;
		for (int k = 0; k < this.networkFlows.size(); k++) {

			IloLinearNumExpr expr = cplex.linearNumExpr();
			for (int i = 0; i < this.networkFlows.get(k).path.size(); i++) {
				for (int j = 0; j < infra.telemetryItemsRouter; j++) {
					iRouter = this.networkFlows.get(k).path.get(i);
					if (this.infra.items[iRouter][j] == 1) {
						expr.addTerm(infra.sizeTelemetryItems[j], y[iRouter][j][k]);
					}

				}
			}

			cplex.addLe(expr, networkFlows.get(k).capacityFlow);
		}

		// Constraint set (3)
		// Ensures that a single telemetry item is not collected by more than a single
		// network flow f
		for (int i = 0; i < this.infra.size; i++) {
			for (int j = 0; j < infra.telemetryItemsRouter; j++) {

				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < this.networkFlows.size(); k++) {

					expr.addTerm(1.0, y[i][j][k]);

				}
				cplex.addLe(expr, 1.0);
			}

		}
		
		// Constraint set (4)
		for (int m = 0; m < this.monitoringApps.size(); m++) {
			
			for(int d = 0; d < this.infra.size; d++) {
				
				int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
				
				for(int p = 0; p < numSpatialDependencies; p++) {
				
					IloLinearNumExpr expr = cplex.linearNumExpr();
					
					ArrayList<Integer> spatialRequirement = this.monitoringApps.get(m).spatialRequirements.get(p);
					
					for(int v = 0; v < spatialRequirement.size(); v++) {
						
						for(int f = 0; f < this.networkFlows.size(); f++) {
							
							//ensure d is in path(f)
							for (int iPath = 0; iPath < this.networkFlows.get(f).path.size(); iPath++) {
								
								if(this.networkFlows.get(f).path.get(iPath) == d) {
									
									expr.addTerm(1.0, y[d][spatialRequirement.get(v)][f]);
								
								}
								
							}
							
							
						}
						
					}
					
					cplex.addEq(s[m][d][p], expr);
				
				}
				
			}
			
		}
		
		//constraint set (6)
		for(int m = 0; m < this.monitoringApps.size(); m++) {
					
			for(int d = 0; d < this.infra.size; d++) {
						
				int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
						
				for(int p = 0; p < numSpatialDependencies; p++) {
					int tamSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.get(p).size();
					IloLinearNumExpr expr = cplex.linearNumExpr();
				
					expr.addTerm(1.0/tamSpatialDependencies, s[m][d][p]);
					
					cplex.addLe(sb[m][d][p], expr);
				}
			}
		}
		
		//constraint set (5)
		for(int m = 0; m < this.monitoringApps.size(); m++) {
			
			int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
			
			for(int p = 0; p < numSpatialDependencies; p++) {
				
				if(this.monitoringApps.get(m).lastTimeCollected.get(p) > this.monitoringApps.get(m).temporalRequirements.get(p)) {
				
					IloLinearNumExpr expr = cplex.linearNumExpr();
					ArrayList<Integer> spatialRequirement = this.monitoringApps.get(m).spatialRequirements.get(p);
					
					for(int v = 0; v < spatialRequirement.size(); v++) {
						
						for(int d = 0; d < this.infra.size; d++) {
							
							for(int f = 0; f < this.networkFlows.size(); f++) {
								
								//ensure d is in path(f)
								for (int iPath = 0; iPath < this.networkFlows.get(f).path.size(); iPath++) {
									
									if(this.networkFlows.get(f).path.get(iPath) == d) {
										
										expr.addTerm(1.0, y[d][v][f]);
									
									}
									
								}
									
							}
							
						}
						
					}
					
					cplex.addEq(t[m][p], expr);
					
				}
				
			}
			
		}
		
	
		//costraint set (7)
		for(int m = 0; m < this.monitoringApps.size(); m++) {
							
			int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
								
			for(int p = 0; p < numSpatialDependencies; p++) {
				int tamSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.get(p).size();				
				IloLinearNumExpr expr = cplex.linearNumExpr();
				expr.addTerm(1.0/tamSpatialDependencies, t[m][p]);
				cplex.addLe(tb[m][p], expr);
			}
		}
		
		
		//costraint set (8) AAUX
		
		for(int m = 0; m < this.monitoringApps.size(); m++) {
			
			int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
			
			for(int p = 0; p < numSpatialDependencies; p++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				
				for(int d = 0; d < this.infra.size; d++) {
					expr.addTerm(1, sb[m][d][p]);
					
				}
				cplex.addLe(sbA[m][p], expr);
			
			}
			
		}
			
		
		// Objective (max num items not collected in the last window)
		IloLinearNumExpr obj = cplex.linearNumExpr();
		
		
		for(int m = 0; m < this.monitoringApps.size(); m++) {
			
			int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
			
			for(int p = 0; p < numSpatialDependencies; p++) {
				
				obj.addTerm(1.0, tb[m][p]);
				
				for(int d = 0; d < this.infra.size; d++) {
					obj.addTerm(1.0, sb[m][d][p]);
				}	
			}
		}
	

		IloObjective objectiveFunctionRef = cplex.addMaximize(obj);

		try {
			
			if (cplex.solve()) {
				
				int[][] CollectedItems = new int[this.infra.size][this.infra.telemetryItemsRouter];
				System.out.println("Cplex: " + cplex.getObjValue());
				
				int contSB = 0;
				int contTB  = 0;
				int contMonApp = 0;
				int contFlows = 0;
				int contItems = 0;
				
				int numSpatialDepdencies = 0;
				
				boolean hasMonAppSatisfied = false; 
				
				for(int m = 0; m < this.monitoringApps.size(); m++) {
					
					hasMonAppSatisfied = false;
					int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
					numSpatialDepdencies += numSpatialDependencies;
					
					for(int p = 0; p < numSpatialDependencies; p++) {
						
						for(int d = 0; d < this.infra.size; d++) {
							
							if(cplex.getValue(sb[m][d][p]) > 0) {
								
								System.out.println( m + "," + d + "," + p);
								
								hasMonAppSatisfied = true;
								contSB++;
								
								Item item = new Item(currentTimeUnit, d);
								ArrayList<Integer> telemetry = this.monitoringApps.get(m).spatialRequirements.get(p);
								
								
								String key = Integer.toString(m) + Integer.toString(d) + Integer.toString(p);
								if(mngStatistics.qtdSatisfiedSd.get(key) == null) {
									mngStatistics.qtdSatisfiedSd.put(key, (float) 1);
								}else {
									mngStatistics.qtdSatisfiedSd.put(key, mngStatistics.qtdSatisfiedSd.get(key) + (float) 1);
								}
								
							}
							
							
						}
						
					}
					
					if(hasMonAppSatisfied) contMonApp++;
					
				}
				
				
				System.out.println("SB " + contSB);
				
				
				//System.out.println("Temporal");
				for(int m = 0; m < this.monitoringApps.size(); m++) {
					int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
					for(int p = 0; p < numSpatialDependencies; p++) {
						
						if(cplex.getValue(tb[m][p]) > 0) {
							//	System.out.println(m + " " + " " + p);
							contTB++;
						}
						
					}
					
				}
							
				for (int k = 0; k < this.networkFlows.size(); k++) {
					
					float flowUtilization = 0;
					
					boolean hasFlowUsed = false;
					for (int i = 0; i < this.networkFlows.get(k).path.size(); i++) {
						for (int j = 0; j < infra.telemetryItemsRouter; j++) {
							iRouter = this.networkFlows.get(k).path.get(i);
							if (this.infra.items[iRouter][j] != 0) {

								if (cplex.getValue(y[iRouter][j][k]) > 0.0) {
									
									contItems++;
									
									System.out.println("Items: " + iRouter + " " + j + " " + k);
									
									CollectedItems[iRouter][j] = 1;
									
									flowUtilization += this.infra.sizeTelemetryItems[j];
									
									hasFlowUsed = true;
									String key = Integer.toString(iRouter) + Integer.toString(j);
									
									if(mngStatistics.qtdCollectedItem.get(key) == null) {
										mngStatistics.qtdCollectedItem.put(key, (float) 1);
									}else {
										mngStatistics.qtdCollectedItem.put(key, mngStatistics.qtdCollectedItem.get(key) + (float) 1);
									}
									
								}

							}
						}
						
					}
					
					if(hasFlowUsed) {
					
						contFlows++;
					
						if (mngStatistics.averageCapacityFlowsUsed.get(k) == null){
							mngStatistics.averageCapacityFlowsUsed.put(k, (float) flowUtilization);
						}else {
							mngStatistics.averageCapacityFlowsUsed.put(k, mngStatistics.averageCapacityFlowsUsed.get(k) + (float) flowUtilization);
						}
						
						if (mngStatistics.qtdFlowsUsed.get(k) == null){
							mngStatistics.qtdFlowsUsed.put(k, 1);
						}else {
							mngStatistics.qtdFlowsUsed.put(k, mngStatistics.qtdFlowsUsed.get(k) + 1);
						}
					
					}
					
					
						
				}
				
				
				int contSB2 = 0;
				for(int i = 0; i < this.monitoringApps.size(); i++) {
					int contMonAppAux  = 0;
					ArrayList<ArrayList<Integer>> spatialReq = this.monitoringApps.get(i).spatialRequirements;
					
					for(int j = 0; j < spatialReq.size(); j++) {
						
						contMonAppAux += countSpatialReqSatisfied(spatialReq.get(j), CollectedItems);
						
					}
					contSB2 += contMonAppAux;
					if (contMonAppAux == spatialReq.size()) contMonApp++;
					
				}
				
				
			
				mngStatistics.setCplexData(contSB, contTB, contMonApp, contFlows, contItems, numSpatialDepdencies);
				
			}
		}catch (IloException e) {
			System.out.println("Exeption " + e);
		}
		
		

		

	}
	
	int countSpatialReqSatisfied(ArrayList<Integer> spatialReq, int[][] CollectedItems) {
		
		int countSpatialReq = 0;
		for(int j = 0; j < this.infra.size; j++) {
			int count = 0;
			for(int i = 0; i < spatialReq.size(); i++) {
				
				if (CollectedItems[j][spatialReq.get(i)] != 0) {
					count++;
				}
				if (count == spatialReq.size()) {
					countSpatialReq++;
				}
			}
		
		}
		
		return countSpatialReq;
		
	}
	
	
	void buildAndRunOnlineRelaxed() throws IloException {

		IloCplex cplex = new IloCplex();
		cplex.setParam(IloCplex.IntParam.Threads, 4);
		
		cplex.setParam(IloCplex.BooleanParam.PreInd, false);
		cplex.clearModel();

		//Variables
		IloNumVar[][][] y   = new IloNumVar[infra.size][infra.telemetryItemsRouter][networkFlows.size()];
		IloNumVar[][][] s   = new IloNumVar[this.monitoringApps.size()][infra.size][this.numMaxSpatialDependencies];
		IloNumVar[][][] sb  = new IloNumVar[this.monitoringApps.size()][infra.size][this.numMaxSpatialDependencies]; 
		
		IloNumVar[][] t  = new IloNumVar[this.monitoringApps.size()][this.numMaxSpatialDependencies];
		IloNumVar[][] tb = new IloNumVar[this.monitoringApps.size()][this.numMaxSpatialDependencies];
		
		//Allocating memory to variables
		for (int i = 0; i < infra.size; i++) {
			for (int j = 0; j < infra.telemetryItemsRouter; j++) {
				y[i][j] = cplex.numVarArray(networkFlows.size(), 0, Double.MAX_VALUE);
			}
		}
		
		for(int i = 0; i < this.monitoringApps.size(); i++) {
			
			t[i]  = cplex.numVarArray(this.numMaxSpatialDependencies, 0, Double.MAX_VALUE);
			tb[i] = cplex.numVarArray(this.numMaxSpatialDependencies, 0, Double.MAX_VALUE);
			
			for(int j = 0; j < this.infra.size; j++) {
				
				s[i][j] = cplex.numVarArray(this.numMaxSpatialDependencies, 0, Double.MAX_VALUE);
				sb[i][j] = cplex.numVarArray(this.numMaxSpatialDependencies, 0, Double.MAX_VALUE);
			}
			
		}

		// Constraint set (2)
		int iRouter = 0;
		for (int k = 0; k < this.networkFlows.size(); k++) {

			IloLinearNumExpr expr = cplex.linearNumExpr();
			for (int i = 0; i < this.networkFlows.get(k).path.size(); i++) {
				for (int j = 0; j < infra.telemetryItemsRouter; j++) {
					iRouter = this.networkFlows.get(k).path.get(i);
					if (this.infra.items[iRouter][j] == 1) {
						expr.addTerm(infra.sizeTelemetryItems[j], y[iRouter][j][k]);
					}

				}
			}

			cplex.addLe(expr, networkFlows.get(k).capacityFlow);
		}

		// Constraint set (3)
		// Ensures that a single telemetry item is not collected by more than a single
		// network flow f
		for (int i = 0; i < this.infra.size; i++) {
			for (int j = 0; j < infra.telemetryItemsRouter; j++) {

				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < this.networkFlows.size(); k++) {

					expr.addTerm(1.0, y[i][j][k]);

				}
				cplex.addLe(expr, 1.0);
			}

		}
		
		// Constraint set (4)
		for (int m = 0; m < this.monitoringApps.size(); m++) {
			
			for(int d = 0; d < this.infra.size; d++) {
				
				int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
				
				for(int p = 0; p < numSpatialDependencies; p++) {
				
					IloLinearNumExpr expr = cplex.linearNumExpr();
					
					ArrayList<Integer> spatialRequirement = this.monitoringApps.get(m).spatialRequirements.get(p);
					
					for(int v = 0; v < spatialRequirement.size(); v++) {
						
						for(int f = 0; f < this.networkFlows.size(); f++) {
							
							//ensure d is in path(f)
							for (int iPath = 0; iPath < this.networkFlows.get(f).path.size(); iPath++) {
								
								if(this.networkFlows.get(f).path.get(iPath) == d) {
									
									expr.addTerm(1.0, y[d][spatialRequirement.get(v)][f]);
								
								}
								
							}
							
							
						}
						
					}
					
					cplex.addEq(s[m][d][p], expr);
				
				}
				
			}
			
		}
		
		//constraint set (6)
		for(int m = 0; m < this.monitoringApps.size(); m++) {
					
			for(int d = 0; d < this.infra.size; d++) {
						
				int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
						
				for(int p = 0; p < numSpatialDependencies; p++) {
					int tamSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.get(p).size();
					IloLinearNumExpr expr = cplex.linearNumExpr();
				
					expr.addTerm(1.0/tamSpatialDependencies, s[m][d][p]);
					
					cplex.addLe(sb[m][d][p], expr);
				}
			}
		}
		
		
		//constraint set (5)
		
		for(int m = 0; m < this.monitoringApps.size(); m++) {
			
			int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
			
			for(int p = 0; p < numSpatialDependencies; p++) {
				
				if(this.monitoringApps.get(m).lastTimeCollected.get(p) > this.monitoringApps.get(m).temporalRequirements.get(p)) {
				
					IloLinearNumExpr expr = cplex.linearNumExpr();
					ArrayList<Integer> spatialRequirement = this.monitoringApps.get(m).spatialRequirements.get(p);
					
					for(int v = 0; v < spatialRequirement.size(); v++) {
						
						for(int d = 0; d < this.infra.size; d++) {
							
							for(int f = 0; f < this.networkFlows.size(); f++) {
								
								//ensure d is in path(f)
								for (int iPath = 0; iPath < this.networkFlows.get(f).path.size(); iPath++) {
									
									if(this.networkFlows.get(f).path.get(iPath) == d) {
										
										expr.addTerm(1.0, y[d][v][f]);
									
									}
									
								}
									
							}
							
						}
						
					}
					
					cplex.addEq(t[m][p], expr);
					
				}
				
			}
			
		}
		
	
		//costraint set (7)
		for(int m = 0; m < this.monitoringApps.size(); m++) {
							
			int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
								
			for(int p = 0; p < numSpatialDependencies; p++) {
				int tamSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.get(p).size();				
				IloLinearNumExpr expr = cplex.linearNumExpr();
				expr.addTerm(1.0/tamSpatialDependencies, t[m][p]);
				cplex.addLe(tb[m][p], expr);
			}
		}
		
		
		
		// Objective (max num items not collected in the last window)
		IloLinearNumExpr obj = cplex.linearNumExpr();

		for(int m = 0; m < this.monitoringApps.size(); m++) {
			
			int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
			
			for(int p = 0; p < numSpatialDependencies; p++) {
				
				obj.addTerm(1.0, tb[m][p]);
				
				for(int d = 0; d < this.infra.size; d++) {
					obj.addTerm(1.0, sb[m][d][p]);
				}
				
				
			}
			
		}
		
		

		IloObjective objectiveFunctionRef = cplex.addMaximize(obj);

		try {
			if (cplex.solve()) {
				
				System.out.println("Cplex: " + cplex.getObjValue());
				
				for(int m = 0; m < this.monitoringApps.size(); m++) {
					int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
					for(int p = 0; p < numSpatialDependencies; p++) {
						
						for(int d = 0; d < this.infra.size; d++) {
							
							if(cplex.getValue(sb[m][d][p]) > 0) {
								System.out.println(m + " " + d + " " + p);
							}
							
							
						}
						
					}
					
				}
				System.out.println("Temporal");
				for(int m = 0; m < this.monitoringApps.size(); m++) {
					int numSpatialDependencies = this.monitoringApps.get(m).spatialRequirements.size(); 
					for(int p = 0; p < numSpatialDependencies; p++) {
						
						if(cplex.getValue(tb[m][p]) > 0) {
								System.out.println(m + " " + " " + p);
						}
						
					}
					
				}
							
				for (int k = 0; k < this.networkFlows.size(); k++) {
					for (int i = 0; i < this.networkFlows.get(k).path.size(); i++) {
						for (int j = 0; j < infra.telemetryItemsRouter; j++) {
							iRouter = this.networkFlows.get(k).path.get(i);
							if (this.infra.items[iRouter][j] != 0) {

								if (cplex.getValue(y[iRouter][j][k]) > 0.0) {
									System.out.println("y[" + iRouter + "]" + "[" + j + "]" + "[" + k + "]");
								
								}

							}
						}
					}
				}
				
				System.out.println( "oi");
				
				
				/*
				for (int k = 0; k < this.networkFlows.size(); k++) {
					for (int i = 0; i < this.networkFlows.get(k).path.size(); i++) {
						for (int j = 0; j < infra.telemetryItemsRouter; j++) {
							iRouter = this.networkFlows.get(k).path.get(i);
							if (this.infra.items[iRouter][j] != 0) {

								if (cplex.getValue(y[iRouter][j][k]) > 0.0) {
									System.out.println("y[" + iRouter + "]" + "[" + j + "]" + "[" + k + "]");
									this.networkFlows.get(k).itens.add(j);
									this.networkFlows.get(k).routers.add(iRouter);
									this.prevCollectedItems[iRouter][j] -= 5;
								}else {
									this.prevCollectedItems[iRouter][j] += 1;
								}

							}

						}
					}
				}*/

			
				
			}
		}catch (IloException e) {
			System.out.println("Exeption " + e);
		}
		
		

		cplex.end();

	}
	

	

}
