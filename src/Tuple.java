import java.util.ArrayList;

public class Tuple<X, Y> {
	
	//It describes the router id
	public X x;    
	
	//It describes the item id
	public Y y;
	
	//It keeps track of network flows traversing router "x"
	public ArrayList<Integer> flows;
	
	//Aux variable used to control weather the item was collected or not
	public int aux;   
	
	public float value; 
	
	public Tuple(X x, Y y) {
		this.x = x;
		this.y = y;
		this.aux = 0;
		this.value = 0;
	}
	
}