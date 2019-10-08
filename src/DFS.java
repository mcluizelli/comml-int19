import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;

public class DFS {

	ArrayList< ArrayList<Integer> > paths;
	NetworkFlow flow;
	ArrayList<Integer> path;
	int[][] graph;
	
	public DFS() {
		
	}
	
	public DFS(NetworkInfrastructure infra){
	
		paths = new ArrayList< ArrayList<Integer> >();

		Random r = new Random();
		int v = Math.abs(r.nextInt() % infra.size);
		Search(v,infra);
		
	}
	
	//run through the whole matrix
	public DFS(int[][] matriz,int size,int v){
		// paths - contains a list of sequences
		paths = new ArrayList< ArrayList<Integer> >();
		
		// graph - contains the adjacency matrix of paths - is a matrix size x size
		this.graph = new int[size][size];
		for(int i=0;i<size;i++) {
			for(int j=0;j<size;j++) {
				this.graph[i][j]=0;
			}
		}
		Search(v,matriz,size);
	}
	
	public DFS(int v,int[][] matriz,int size){
		
		path = new ArrayList<Integer>();
		this.graph = new int[size][size];
		for(int i=0;i<size;i++)
			for(int j=0;j<size;j++)
				this.graph[i][j]=0;
		
		int i=0;
		path.add(v);
		i=0;
		while(i!=size){
			for(i=0;i<size;i++){
				if(matriz[v][i]!=0){
					matriz[v][i]=0;
					matriz[i][v]=0;
					this.graph[i][v]=1;
					this.graph[v][i]=1;
					v=i;
					path.add(v);
					break;
				}
			}
		}
	}
	
	public DFS(int v,int[][] matriz,int size,int max){
		
		// flow - flow
		flow = new NetworkFlow(v,-1,max);
		
		// c - capacity of flow
		int c = max;
		
		// path - the path of flow
		path = new ArrayList<Integer>();
		path.add(v);
		
		int i=0;
		while(c>0 && i!=size){
			for(i=0;i<size;i++){
				if(matriz[v][i]!=0){
					matriz[v][i]=0;
					matriz[i][v]=0;
					v=i;
					path.add(v);
					c= c-1;
					break;
				}
			}
		}flow.setPath(path);
	}
	
	public void Search(int v,NetworkInfrastructure infra){
		
		ArrayList<Integer> path = new ArrayList<Integer>();
		
		int i=0;
		System.out.println(infra.size);
		path.add(v);
		while(i!=infra.size){
			for(i=0;i<infra.size;i++){
				if(infra.graph[v][i]!=0){
					infra.graph[v][i]=0;
					infra.graph[i][v]=0;
					v=i;
					path.add(v);
					break;
				}
			}
		}if(path.size()>1){
			paths.add(path);
			for(i=path.size()-2;i>=0;i--) {
				Search(path.get(i),infra);
			}
		}
		
	}

	public void Search(int v,int[][] matriz,int size){
		ArrayList<Integer> path = new ArrayList<Integer>();
		
		int i=0;
		path.add(v);
		while(i!=size){
			for(i=0;i<size;i++){
				if(matriz[v][i]!=0){
					matriz[v][i]=0;
					matriz[i][v]=0;
					this.graph[i][v]=1;
					this.graph[v][i]=1;
					v=i;
					path.add(v);
					break;
				}
			}
		}paths.add(path);
		if(path.size()>1){
			paths.add(path);
			for(i=path.size()-2;i>=0;i--) {
				Search(path.get(i),matriz,size);
			}
		}
	}
	
	public ArrayList<Integer>getPath(){
		return path;
	}
	public ArrayList< ArrayList<Integer> > getPaths(){
		return paths;
	}
	
	public int[][] getgraph(){
		return graph;
	}
	
	public int getgraph(int i,int j){
		return graph[i][j];
	}
	
	
	
	
	
	
	
}
