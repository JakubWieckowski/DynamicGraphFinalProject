package y2022_2023.uksw;

import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.WattsStrogatzGenerator;
import org.graphstream.graph.Node;
import org.graphstream.graph.Edge;
import java.util.ArrayList;
import java.util.Random;

public class Generator {
	
	public final static int VON_NEUMANN = 4;
	public final static int MOORE = 8;
	public final static boolean NO_DISPLAY = false;
	
	public final static int GRID = 10;
	public final static int TORUS = 11;
	public final static int RING = 12;
	public final static int FULL_CONNECTED = 13;
	public final static int TREE = 14;
	public final static int RANDOM_FROM_TREE = 30;
	public final static int RANDOM_FROM_RING = 31;
	public final static int ERDOS_RENYI = 32;
	public final static int RGG = 40;
	public final static int BARABASI_ALBERT = 50;
	public final static int WATTS_STROGATZ = 51;

	
	public static SingleGraph randomGeometricGraphW(int numberOfNodes, double distanceThrehold, int environmentSize) {
		SingleGraph g = new SingleGraph("random generator");
		int n = numberOfNodes;
		int size = environmentSize;
		double d = distanceThrehold;
		Random alea = new Random(System.currentTimeMillis());
		// Nodes creation with their coordinates
		for(int i=0;i<n;i++) {
			Node u = g.addNode("u_"+i);
			// random position of the node within the environment
			double x = alea.nextDouble()*size;
			double y = alea.nextDouble()*size;
			u.setAttribute("x",x);
			u.setAttribute("y",y);
		}
		// add edges
		for(Node u:g.getNodeSet()) {
			for(Node v:g.getNodeSet()) {
				if(u.getId() != v.getId()) {
					if((distance(u,v) < d) && (!u.hasEdgeBetween(v))) {
						g.addEdge(u.getId()+"--"+v.getId(),u.getId(),v.getId());
					}
				}
			}
		}
		return g;
	}
	
	/**
	 * a generator for a random geometric graph based on the 
	 * euclidean distance between nodes.
	 * 
	 * @param n the number of nodes
	 * @param d the maximum euclidean distance for 2 nodes to be connected
	 * @param envSize the size of the environment in which nodes are positioned
	 * @return rgg an instance of such a random euclidean graph
	 */
	public static SingleGraph randomEuclideanGraph(int n, double d, int envSize) {
		SingleGraph rgg = new SingleGraph("RGG: ("+n+","+d+","+envSize+")");
		Random alea = aleaGenerator();
		// creation of nodes
		for(int u=0;u<n;u++) {
			Node v = rgg.addNode("v_"+u);
			v.addAttribute("x",alea.nextDouble()*envSize);
			v.addAttribute("y",alea.nextDouble()*envSize);
		}
		// creation of edges
		for(Node u:rgg.getNodeSet()) {
			for(Node v:rgg.getNodeSet()) {
				if(((u != v) && (distance(u,v) < d)) && (!u.hasEdgeBetween(v)))
					rgg.addEdge(u.getId()+"-"+v.getId(),u.getId(),v.getId());
			}
		}
		return rgg;
	}
	
	
	
	/**
	 * random graph generator, "à la" Erdos-Renyi
	 * @param n
	 * @param proba
	 * @return
	 */
	public static SingleGraph almostErdosRenyi(int n, double proba) {
		SingleGraph myRandomGraph = fullconnected(n);
		myRandomGraph.display();
		//Tools.hitakey("Remove edges");
		ArrayList<Edge> edgesToBeRemoved = new ArrayList<>();
		Random alea = aleaGenerator();
		for(Edge e: myRandomGraph.getEdgeSet()) {
			if(alea.nextDouble() < 1-proba) edgesToBeRemoved.add(e);
		}
		for(Edge e: edgesToBeRemoved) {
			myRandomGraph.removeEdge(e);
		}
		return myRandomGraph;
	}
	

	/**
	 * grid/torus generator with either von Neumann or Moore neighborhood
	 * @param n
	 * @param neighborhood
	 * @param isAtorus
	 * @return
	 */
	public static SingleGraph grid(int n, int neighborhood, boolean isAtorus, boolean display) {
		SingleGraph theGrid = grid(n,isAtorus,display);
		if(neighborhood == MOORE) {
			// add missing edges
			int val = 1;
			if(isAtorus) val = 0;
			for(int line=0;line<n-val;line++) {
				for(int col=0;col<n-val;col++) {
					int colplusone = (col+1)%n;
					int lineplusone = (line+1)%n;
					Node v = theGrid.getNode(line+","+col);
					Node w = theGrid.getNode(lineplusone+","+colplusone);
					Edge e = theGrid.addEdge(v.getId()+"-"+w.getId(),v.getId(),w.getId());
				}
			}
			for(int line=val;line<n;line++) {
				for(int col=0;col<n-val;col++) {
					int colplusone = (col+1)%n;
					int lineminusone = (n+line-1)%n;
					Node v = theGrid.getNode(line+","+col);
					Node w = theGrid.getNode(lineminusone+","+colplusone);
					Edge e = theGrid.addEdge(v.getId()+"-"+w.getId(),v.getId(),w.getId());
				}
			}
		}
		return theGrid;
	}
	
	/**
	 * grid/torus generator with either von Neumann or Moore neighborhood
	 * @param n
	 * @param neighborhood
	 * @param isAtorus
	 * @return
	 */
	public static SingleGraph grid(int n, int neighborhood, boolean isAtorus) {
		return grid(n,neighborhood,isAtorus,true);
	}
	
	/**
	 * grid/torus generator with a von Neumann neighborhood
	 * @param n
	 * @param isAtorus
	 * @return
	 */
	public static SingleGraph grid(int n, boolean isAtorus, boolean display) {
		SingleGraph myGrid = new SingleGraph("grid of size: "+n+"x"+n);
		if(display != NO_DISPLAY) myGrid.display(isAtorus);
		// creation of all nodes with their coordinates
		for(int line=0 ; line<n ; line++) {
			for(int col=0 ; col<n ; col++) {
				Node v = myGrid.addNode(line+","+col);
				v.addAttribute("x",line);
				v.addAttribute("y",col);
			}
		}
		//Tools.hitakey("add links");
		// add links between nodes
		int val = 1;
		if(isAtorus) val = 0;
		for(int line=0;line<n;line++) {
			for(int col=0 ; col<n-val ; col++) {
				Node u = myGrid.getNode(line+","+col);
				int colval = (col+1)%n;
				Node vright = myGrid.getNode(line+","+colval);
				myGrid.addEdge(u.getId()+"-"+vright.getId(),u.getId(),vright.getId());
			}
		}
		for(int col=0;col<n;col++) {
			for(int line=0 ; line<n-val ; line++) {
				Node u = myGrid.getNode(line+","+col);
				int lineval = (line+1)%n;
				Node vbelow = myGrid.getNode(lineval+","+col);
				myGrid.addEdge(u.getId()+"-"+vbelow.getId(),u.getId(),vbelow.getId());
			}
		}
		return myGrid;
	}
	

	/**
	 * tree generator by the method of adding a new vertex at each time step
	 * @param n
	 * @return
	 */
	public static SingleGraph tree(int n, boolean display) {
		SingleGraph myTree = new SingleGraph("tree of size: "+n);
		if(display) myTree.display();
		int index = 0;
		Node v = myTree.addNode("v_"+index);
		v.addAttribute("timestamp",index);
		index++;
		while(index < n) {
			// randomly choose one node within the graph
			Node u = Toolkit.randomNode(myTree);	
			Node nn = myTree.addNode("v_"+index);
			nn.addAttribute("timestamp",index);
			myTree.addEdge(u.getId()+"--"+nn.getId(),
					u.getId(),nn.getId());
			index++;
		}
		return myTree;
	}
	
	
	/**
	 * tree generator by the method of adding a new vertex at each time step
	 * @param n
	 * @return
	 */
	public static SingleGraph tree(int n) {
		return tree(n,true);
	}
	
	
	/**
	 * full connected graph generator
	 * @param n
	 * @return
	 */
	public static SingleGraph fullconnected(int n) {
		SingleGraph myGraph = new SingleGraph("full connected "+n);
		// creation of all vertices
		for(int i=0;i<n;i++) {
			myGraph.addNode("v_"+i);
		}
		// creation of all edges between vertices
		for(Node u: myGraph.getNodeSet()) {
			for(Node v: myGraph.getNodeSet()) {
				// if no edge between vertices and u is not v add an edge
				if((u.getId() != v.getId()) && 
						!(u.hasEdgeBetween(v))) {
					myGraph.addEdge(u.getId()+"--"+v.getId(),u.getId(),v.getId());
				}
			}
		}
		return myGraph;
	}

	
	/**
	 * a simple call to the Watts Strogatz generator provided 
	 * by GraphStream
	 * @param n
	 * @param k
	 * @param rewired
	 * @return
	 */
	public static SingleGraph wattsStrogatzForMultipleRandomWalk(int n, int k, double rewired) {
		SingleGraph graph = new SingleGraph("This is a small world!");
		WattsStrogatzGenerator gen = new WattsStrogatzGenerator(400, 8, 0.2);

		gen.addSink(graph);
		gen.begin();
		while(gen.nextEvents()) {}
		gen.end();
		return graph;
		/*
		System.out.println("G=("+graph.getNodeCount()+
				","+graph.getEdgeCount()+")");
		int[] degreeDistrib = new int[40];
		for(Node u:graph.getNodeSet()) {
			degreeDistrib[u.getDegree()]++;
		}
		System.out.println();
		for(int d=0;d<40;d++) {
			System.out.println(d+":"+degreeDistrib[d]);
		}
		
		Tools.hitakey("the end");
		System.exit(0);
		 */
	}
	
	/**
	 * a simple call to the Barabasi Albert generator provided
	 * by GraphStream
	 * @param n
	 * @param k
	 * @return
	 */
	public static SingleGraph barabasiAlbertForMultipleRandomWalk(int n, int k) {
		SingleGraph graph = new SingleGraph("Barabàsi-Albert");
		BarabasiAlbertGenerator gen = new BarabasiAlbertGenerator(4);
		gen.addSink(graph); 
		gen.setExactlyMaxLinksPerStep(true);
		gen.begin();
		for(int i=0; i<400; i++) gen.nextEvents();
		gen.end();
		return graph;
		/*
		graph.display();
		System.out.println("G=("+graph.getNodeCount()+
				","+graph.getEdgeCount()+")");
		int[] degreeDistrib = new int[40];
		for(Node u:graph.getNodeSet()) {
			degreeDistrib[(int)(u.getDegree()/10)]++;
		}
		System.out.println();
		for(int d=0;d<40;d++) {
			System.out.println(d+":"+degreeDistrib[d]);
		}
		
		Tools.hitakey("the end");
		System.exit(0);
		*/
	}
	
	
	// ================= UTILS ======================
	
	
	public static Node getRandomNeighbor(Random alea, Node u) {
		Node neighbor = null;
		if(u.getDegree() > 0) {
			ArrayList<Edge> edges = new ArrayList<>(u.getEdgeSet());
			neighbor = edges.get(alea.nextInt(u.getDegree())).getOpposite(u);
		}
		return neighbor;
	}
	
	/**
	 * add some edges to the graph
	 * !!!! a verification should be done that the total number of 
	 * added edges do not exceed the one of a full-connected graph 
	 * @param graph
	 * @param nbEdgesToAdd
	 */
	public static void densify(SingleGraph graph, int nbEdgesToAdd) {
		while(nbEdgesToAdd > 0) {
			Node u = Toolkit.randomNode(graph);
			Node v = Toolkit.randomNode(graph);
			if((u.getId() != v.getId()) && !u.hasEdgeBetween(v)) {
				graph.addEdge(u.getId()+"-"+v.getId(),u.getId(),v.getId());
				nbEdgesToAdd--;
			}
		}
	}
	
	public static Random aleaGenerator() {
		return new Random(System.currentTimeMillis());
	}
	
	/**
	 * computes the euclidean distance between two nodes 
	 * @param u
	 * @param v
	 * @return
	 */
	public static double distance(Node u, Node v) {
		double xu = u.getAttribute("x");
		double yu = u.getAttribute("y");
		double xv = v.getAttribute("x");
		double yv = v.getAttribute("y");
		return Math.sqrt((xu-xv)*(xu-xv)+(yu-yv)*(yu-yv));
	}
	
}
