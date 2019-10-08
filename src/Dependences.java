
import java.util.ArrayList;
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author francisco
 */
public class Dependences {

	NetworkInfrastructure infra;
	ArrayList<NetworkFlow> networkFlows;
	ArrayList<Tuple<Integer, Integer>> prioritys = new ArrayList<Tuple<Integer, Integer>>();// lista
	int timeUnits = 0;
	ArrayList<Tuple<Tuple<Integer, Integer>, Integer>> itensTimeUnits;
	ArrayList<Tuple<Integer, Integer>> itemsPriority = new ArrayList<Tuple<Integer, Integer>>();
	ArrayList<Tuple<Integer, Integer>> itemsSobra = new ArrayList<Tuple<Integer, Integer>>();
	ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>>> depend;
	int aux;
	int capacity;

	Random rnd;

	public Dependences(NetworkInfrastructure infra, ArrayList<NetworkFlow> networkFlows, long seed,
			ArrayList<Tuple<Tuple<Integer, Integer>, Integer>> itensTimeUnits,
			ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>>> Depend) {

		this.infra = infra;
		this.itensTimeUnits = itensTimeUnits;
		this.networkFlows = networkFlows;
		this.rnd = new Random(seed);
		this.depend = Depend;

		boolean auxFind = false;
		// Initialize priority per flow

		for (int i = 0; i < this.networkFlows.size(); i++) {

			ArrayList<Integer> path = networkFlows.get(i).path;

			for (int j = 0; j < path.size(); j++) {

				for (int k = 0; k < this.infra.telemetryItemsRouter; k++) {

					if (this.infra.items[path.get(j)][k] == 1) {

						// Verify weather item is or not in the prioririty
						auxFind = false;

						for (int l = 0; l < prioritys.size(); l++) {

							if (prioritys.get(l).x == path.get(j) && prioritys.get(l).y == k) {

								// Add flow info to the item
								prioritys.get(l).flows.add(i);
								auxFind = true;

							}

						}

						if (!auxFind) {

							// Add item to the priority
							Tuple<Integer, Integer> pair = new Tuple<Integer, Integer>(path.get(j), k);
							pair.flows = new ArrayList<Integer>();
							pair.flows.add(i);

							prioritys.add(pair);

						}

					}

				}

			}

		}

	}

	void updateQueue() {
		for (int i = 0; i < itensTimeUnits.size(); i++) {
			if (itensTimeUnits.get(i).y % timeUnits == 0) {
				for (int j = 0; j < prioritys.size(); j++) {
					if (prioritys.get(j).x == itensTimeUnits.get(i).x.x
							&& prioritys.get(j).y == itensTimeUnits.get(i).x.y) {
						prioritys.add(0, prioritys.get(j));
						prioritys.remove(j + 1);
						itemsPriority.add(prioritys.get(j));
						break;
					}
				}
			}
		}
		for (int i = 0; i < itemsSobra.size(); i++) {
			for (int j = 0; j < prioritys.size(); j++) {
				if (prioritys.get(j).x == itemsSobra.get(i).x && prioritys.get(j).y == itemsSobra.get(i).y) {
					prioritys.add(0, prioritys.get(j));
					prioritys.remove(j + 1);
					break;
				}
			}
		}
		itemsSobra.clear();

	}

	public void runRnd() {

		updateQueue();
		timeUnits++;

		int i = 0;

		int flow = 0;
		int[] flowCapacity = new int[networkFlows.size()];
		int[] auxFlow = new int[networkFlows.size()];

		for (int j = 0; j < this.prioritys.size(); j++) {
			this.prioritys.get(j).aux = 0;
		}

		for (i = 0; i < networkFlows.size(); i++) {
			auxFlow[i] = 0;
			flowCapacity[i] = networkFlows.get(i).capacityFlow;
			networkFlows.get(i).itens.clear();
			networkFlows.get(i).routers.clear();
		}

		for (int j = 0; j < this.prioritys.size(); j++) {// percorre a prioridade de items

			Tuple<Integer, Integer> potencialItem = this.prioritys.get(j);

			// If items was not collected at this run
			if (potencialItem.aux == 0) {

				// Try to place this in one avaliable flow
				for (int l = 0; l < potencialItem.flows.size(); l++) {

					flow = potencialItem.flows.get(l);

					if (this.infra.sizeTelemetryItems[potencialItem.y] <= flowCapacity[flow]) {
						for (int u = 0; u < depend.size(); u++) {
							if (depend.get(u).x.x == potencialItem.x && depend.get(u).x.y == potencialItem.y) {
								if (flowCapacity[flow] >= this.infra.sizeTelemetryItems[potencialItem.y]
										+ this.infra.sizeTelemetryItems[depend.get(u).y.y]) {
									// Add item
									flowCapacity[flow] -= this.infra.sizeTelemetryItems[potencialItem.y]
											+ this.infra.sizeTelemetryItems[depend.get(u).y.y];
									// Rotate flows
									potencialItem.flows.remove(l);
									potencialItem.flows.add(flow);

									// Mark it as collected
									potencialItem.aux = 1; // control which item was collected

									// Place that item to the end
									this.prioritys.remove(j);
									this.prioritys.add(potencialItem);

									networkFlows.get(flow).itens.add(potencialItem.y);
									networkFlows.get(flow).itens.add(depend.get(u).y.y);
									networkFlows.get(flow).routers.add(potencialItem.x);
									networkFlows.get(flow).routers.add(depend.get(u).y.y);
									for (int y = 0; y < itemsPriority.size(); y++) {
										if (potencialItem.x == itemsPriority.get(y).x
												&& potencialItem.y == itemsPriority.get(y).y) {
											itemsPriority.remove(y);
											break;
										}
									}
									j--;
									break;

								}
							}
						}
					}

				}

			}

		}
		for (int t = 0; t < itemsPriority.size(); t++) {
			itemsSobra.add(itemsPriority.get(t));
		}
		itemsPriority.clear();

	}
}
