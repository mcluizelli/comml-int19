import java.util.ArrayList;

public class Item {

	int timeUnit;
	int routerId;
	
	public ArrayList<Double> featureList; 
	
	public int timeUnits;
	
	int itemId; 
	
	public Item(int timeUnit, int routerId) {
		
		this.timeUnit = timeUnit;
		this.routerId = routerId;
		featureList = new ArrayList<Double>();
		
	}
	
	public void addFeature(Double feature) {
		featureList.add(feature);
	}
	
	
	public int getSizeFeatures() {
		return featureList.size();
	}
	
	public ArrayList<Double> getFeatures(){
		return featureList;
	}

}
