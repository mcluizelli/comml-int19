import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class NetworkInfrastructure {

	public int[][]graph; //network structure (nodes)
	public int size;	 //size of the network
	public String filePath;
	
	public int[] numTelemetryItems; //number of telemetry item per router (vertex)
	public int[] sizeTelemetryItems;//size of each telemetry item	
	public int[][] items; //row = routers, col = items
 	
	public int telemetryItemsRouter;	//total number of possible existing items in any router
	public int maxSizeTelemetryItemsRouter; //max possible item size
	
	public NetworkInfrastructure(int size, String filePath, int telemetryItemsRouter, int maxSizeTelemetryItemsRouter) {
		
		this.filePath = filePath;
		this.size = size;
		this.telemetryItemsRouter = telemetryItemsRouter;
		this.maxSizeTelemetryItemsRouter = maxSizeTelemetryItemsRouter;
		this.graph = new int[size][size];
		this.numTelemetryItems = new int[size];
		this.items = new int[size][telemetryItemsRouter];
		
	}
	
	
	/**
	 * This method loads network topology without any randomness
	 * @throws FileNotFoundException
	 */
	//read .dat
	void loadTopologyDat() throws FileNotFoundException {
		
		File file = new File(filePath); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		
		int i, j;
		
		try {
			String st;
			while( (st = br.readLine())!=null) {
				
				String[] split = st.split("\t");
				i = Integer.parseInt(split[1]);
				j = Integer.parseInt(split[2]);
				this.graph[i][j] = 1;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		for(i = 0; i < this.size; i++){
			
			numTelemetryItems[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItems[i]) {
				
				items[i][k] = 1;
				k++;
 				
			}
				
		}
		
		for(j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = maxSizeTelemetryItemsRouter;
			
		}
		
		
	}
	

	void loadTopologyTxt() throws FileNotFoundException {
		
		File file = new File(filePath); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		
		int i, j;
		
		try {
			String st;
			while( (st = br.readLine())!=null) {
				
				String[] split = st.split(" ");
				i = Integer.parseInt(split[0]);
				j = Integer.parseInt(split[1]);
				this.graph[i][j] = 1;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		for(i = 0; i < this.size; i++){
			
			numTelemetryItems[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItems[i]) {
				
				items[i][k] = 1;
				k++;
 				
			}
				
		}
		
		for(j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = maxSizeTelemetryItemsRouter;
			
		}
		
		
	}
	
	void loadTopologyTxt(long seed) throws FileNotFoundException {
		
		File file = new File(filePath); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		Random rnd = new Random(seed);
		
		int i, j;
		
		try {
			String st;
			while( (st = br.readLine())!=null) {
				
				String[] split = st.split(" ");
				i = Integer.parseInt(split[0]);
				j = Integer.parseInt(split[1]);
				this.graph[i][j] = 1;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		for(i = 0; i < this.size; i++){
			
			//numTelemetryItems[i] = rnd.nextInt(telemetryItemsRouter) + 1;
			numTelemetryItems[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItems[i]) {
				
				items[i][k] = 1;
				k++;
 				
			}
				
		}
		
		int aux = 0;
		
		for(j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = rnd.nextInt(maxSizeTelemetryItemsRouter) + 1;
			
		}
		
		
	}
	
	
	void loadAbilene(long seed) throws FileNotFoundException {
		
		this.graph[0][1] = 1;
		this.graph[1][0] = 1;
		
		this.graph[0][10] = 1;
		this.graph[10][0] = 1;
		
		this.graph[1][2] = 1;
		this.graph[2][1] = 1;
		
		this.graph[1][10] = 1;
		this.graph[10][1] = 1;
		
		this.graph[1][9] = 1;
		this.graph[9][1] = 1;
		
		this.graph[10][9] = 1;
		this.graph[9][10] = 1;
		
		this.graph[2][3] = 1;
		this.graph[3][2] = 1;
		
		this.graph[9][3] = 1;
		this.graph[3][9] = 1;

		this.graph[9][8] = 1;
		this.graph[8][9] = 1;
		
		this.graph[3][4] = 1;
		this.graph[4][3] = 1;
		
		this.graph[8][4] = 1;
		this.graph[4][8] = 1;
		
		this.graph[5][4] = 1;
		this.graph[4][5] = 1;
		
		this.graph[5][8] = 1;
		this.graph[8][5] = 1;
		
		this.graph[7][8] = 1;
		this.graph[8][7] = 1;
		
		this.graph[7][6] = 1;
		this.graph[6][7] = 1;
		
		this.graph[5][6] = 1;
		this.graph[6][5] = 1;
		
		
		
		
		
	}
	
	void loadTopologyDat(long seed) throws FileNotFoundException {
		
		File file = new File(filePath); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		Random rnd = new Random(seed);
		
		int i, j;
		
		try {
			String st;
			while( (st = br.readLine())!=null) {
				
				String[] split = st.split("\t");
				i = Integer.parseInt(split[1]);
				j = Integer.parseInt(split[2]);
				this.graph[i][j] = 1;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		//generete randomly telemetry items
		for(i = 0; i < this.size; i++){
			
			//numTelemetryItems[i] = rnd.nextInt(telemetryItemsRouter) + 1;
			numTelemetryItems[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItems[i]) {
				
				//if (rnd.nextDouble() > 0.5 && items[i][l] == 0) {
					items[i][l] = 1;
					k++;
 				//}
				l++;
				l = l % this.telemetryItemsRouter;
				
			}
			
				
		}
		
		for(j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = rnd.nextInt(maxSizeTelemetryItemsRouter) + 1;
			
		}
		
		
	}
	
	void generateRndTopology(double linkProbability, long seed) {
		
		Random rnd = new Random(seed);
		
		for(int i = 0; i < size; i++) {
			
			for(int j = 0; j < size; j++) {
				
				if (i != j && rnd.nextDouble() < linkProbability) {
					this.graph[i][j] = 1;
					this.graph[j][i] = 1;
				}
				
			}
			
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		//generete randomly telemetry items
		for(int i = 0; i < this.size; i++){
			
			do {
				numTelemetryItems[i] = rnd.nextInt(telemetryItemsRouter+1);
				//numTelemetryItems[i] = 4;
				
			}while(numTelemetryItems[i] == 0);
			
			
			int k = 0;
			int l = 0;
			
			
			
			
			while(k < numTelemetryItems[i]) {
				
				//if (rnd.nextDouble() > 0.5 && items[i][l] == 0) {
					items[i][l] = 1;
					k++;
 				//}
				l++;
				l = l % this.telemetryItemsRouter;
				
			}
			
				
		}
		
		for(int j = 0; j < this.telemetryItemsRouter; j++) {
			do {
				//sizeTelemetryItems[j] = 2;rnd.nextInt(maxSizeTelemetryItemsRouter);
				sizeTelemetryItems[j] = rnd.nextInt(maxSizeTelemetryItemsRouter);
			}while(sizeTelemetryItems[j] == 0);
		}
		
		
		
	}
	
	ArrayList<Integer> getShortestPath(int nodeA, int nodeB) {
		
		ArrayList<Integer> shortPath = new ArrayList<Integer>();
		ArrayList<Integer> availableNodes = new ArrayList<Integer>();
		
		int currentNode = -1;
		int currentAdj = -1;
		
		int dist[] = new int[size];
		int prev[]    = new int[size];
		
		int visited[]  = new int[size];
		
		//initialize;
		for(int i = 0; i < this.size; i++) {
			dist[i] = Integer.MAX_VALUE;
			prev[i] = -1;
			visited[i] = 0;
			availableNodes.add(i);
			
		}
		
		visited[nodeA] = 1;
		dist[nodeA] = 0;
		
		
		while(!availableNodes.isEmpty()) {
			
			currentNode = getMin(availableNodes, dist, visited);
			visited[currentNode] = 1;
			
			int k;
			for(k = 0; k < availableNodes.size(); k++) {
				if (availableNodes.get(k) == currentNode) break;
			}
			availableNodes.remove(k);
			
			ArrayList<Integer> adj = getAdj(currentNode);
			
			for(int i = 0; i < adj.size(); i++) {
				
				currentAdj = adj.get(i);
				
				if(visited[currentAdj] == 0) {
					if(dist[currentAdj] > dist[currentNode] + this.graph[currentNode][currentAdj]) {
						dist[currentAdj] = dist[currentNode] + this.graph[currentNode][currentAdj];
						prev[currentAdj] = currentNode;
					}
				}
				
			}
			
		}
		
		shortPath = buildPath(nodeA, nodeB, prev);
		
		return shortPath;
	
	}
	
	ArrayList<Integer> buildPath(int nodeA, int nodeB, int[] prev){
		
		ArrayList<Integer> filePath = new ArrayList<Integer>();
		int currentNode = nodeB;
		
		while(prev[currentNode] != -1) {
			filePath.add(0,currentNode);
			currentNode = prev[currentNode];
		}
		
		filePath.add(0,nodeA);
		
		return filePath;
		
		
	}
	
	int getMin(ArrayList<Integer> availableNodes, int[] dist, int[] visited) {
		
		int min = Integer.MAX_VALUE;
		int index = -1;
		
		for(int i = 0; i < availableNodes.size(); i++) {
			if(dist[availableNodes.get(i)] < min) {
				min = dist[availableNodes.get(i)];
				index = availableNodes.get(i);
			}
		}
		
		return index;
		
	}
	
	ArrayList<Integer> getAdj(int i){
		
		ArrayList<Integer> adj = new ArrayList<Integer>();
		
		for(int j = 0; j < this.size; j++) {
			if(this.graph[i][j] == 1){
				adj.add(j);
			}
		}
		
		return adj;
		
	}
	
	public void generateDirectedTopology() {
		for(int i = 0; i < graph.length; i++) {
			for(int j = 0; j < graph[i].length; j++) {
				graph[i][j] = 0;
			}
		}
		
		graph[0][1] = 1;
		graph[1][2] = 1;
		graph[1][6] = 1;
		graph[1][7] = 1;
		graph[1][8] = 1;
		graph[2][3] = 1;
		graph[2][6] = 1;
		graph[3][4] = 1;
		graph[3][6] = 1;
		graph[4][5] = 1;
		graph[4][6] = 1;
		graph[7][8] = 1;
		graph[8][7] = 1;
		graph[5][6] = 1;
		graph[2][8] = 1;
	}
	
	
	
	

}
