import java.util.ArrayList;

public class NetworkFlow {

	public ArrayList<Integer> path;  //all routers which belong to a given network flow
	public int source;
	public int destination;
	public int capacityFlow; //max flow capacity
	public int capacityVariable;
	
	//items captured by a given flow
	public ArrayList<Integer> itens= new ArrayList<Integer>();
	
	//flow path routers in which items were collected
	public ArrayList<Integer> routers = new ArrayList<Integer>();
	
	
	public NetworkFlow(int source, int destination, int capacityFlow) {
		this.source = source;
		this.destination = destination;
		this.capacityFlow  = capacityFlow;
		this.capacityVariable = capacityFlow;
		this.path = new ArrayList<Integer>();
	}
	
	public void setPath(ArrayList<Integer> path) {
		this.path = path;
	}
	

}
