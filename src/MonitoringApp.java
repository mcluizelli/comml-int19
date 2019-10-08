import java.util.ArrayList;
import java.util.Hashtable;

public class MonitoringApp {

	//Need to define monitoring requirements
	
	
	//{ {0,2,3}, {4,5} } 
	ArrayList<ArrayList<Integer>> spatialRequirements;
	
	//<0, 2>
	//<1, 1>
	//Hash works for sets of spatial requirements. The hash key refers to the spataialRequirement's index
	ArrayList<Integer> temporalRequirements;
	ArrayList<Integer> lastTimeCollected;  //it stores 
	
	public MonitoringApp() {
		
		this.spatialRequirements  = new ArrayList<ArrayList<Integer>>();
		this.temporalRequirements = new ArrayList<Integer>();
		this.lastTimeCollected    = new ArrayList<Integer>();
	}

}
