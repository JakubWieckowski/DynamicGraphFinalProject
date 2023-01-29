package proj;
/* -----------------------------------------------------------------
 * This class is a simple demonstrator for broadcasting algorithms
 * In the graph, there are 4 nodes that are not stations but define 
 * the corners of the environment, thus neither the mobility model
 * nor the broadcasting algorithm are applied on them. 
 * All the other nodes are gathered into the stations ArrayList
 * -----------------------------------------------------------------
 * Simulations are done such that each node/station executes the 
 * same algorithm, both for moving and for broadcasting. 
 * The simulation stops as soon as the broadcasting is finished. 
 * -----------------------------------------------------------------
 * version: listopad 2022 
 * author: of the code (including bugs ;-) F. Guinand
 * -----------------------------------------------------------------
 */


import org.graphstream.graph.implementations.SingleGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.graphstream.algorithm.ConnectedComponents;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;


public class Broadcasting {
	
	public final static int RWP = 1;
	public final static int MANHATTAN = 2;
	public final static int MARKOVIAN = 3;

	public final static int NO_BROADCASTING = 10;
	public final static int SIMPLE_FLOODING = 11;
	public final static int SIMPLE_FLOODING_WITH_DELAY = 12;

	public final static String sourceStyle = "fill-color: red;shape:cross;";
	public final static String senderStyle = "fill-color:blue;";
	public final static String completedTaskStyle = "fill-color:green;";
	public final static String TTLexpired = "fill-color:red;";
	public final static String edgeONstyle = "fill-color:red;size:1px;";
	public final static String edgeOFFstyle = "fill-color:white;size:1px;";


	// execution parameters 
	int delay = 50;
	Random alea = new Random(System.currentTimeMillis());
	SingleGraph g;
	ArrayList<Node> stations;
	boolean stepByStep = false;
	boolean labelOnNodes = false;
	int maxIterations = 10000; // if no broadcasting algo is running
	
	// mobility and graph parameters
	int nbNodes = 300;
	int indexNodes = nbNodes;
	double maxSpeed = 5.0;
	double proximityThreshold = 4;
	int d = 70;
	int envSize = 1000;
	int mobilityModel = RWP; //RWP // MANHATTAN; // MARKOVIAN //
	int nbParallelStreets = 10;
	int distanceInterStreets = (int)(envSize/nbParallelStreets);
	//Nowe parametry
	double p = 0.9; //probabilities of Edge-Markovian model
	double q = 0.9;
	int Scenario = 1; //Scenario 1//Scenario 2
	int TTL = 3; //For Scenario 1 - the lifetime of a message on a vertex
	double r = 0.2; //For Scenario 2 - the ratio of renewing nodes within the graph
	
	// broadcast parameters
	Node source = null;
	ArrayList<Node> readyToSend;
	int broadcastingStrategy = SIMPLE_FLOODING; // NO_BROADCASTING; // 
	int maxRAD = 10; // stands for max Random Assessment Delay
	
	

	// ---- constructor
	public Broadcasting() {
		initGraph();
		if(broadcastingStrategy != NO_BROADCASTING) {
			initBroadcast();
			System.out.println("source is: "+source.getId());
		}
		initMobility();
		Tools.hitakey("Go!!");
		moveAndBroadcast();
	}
	
	/**
	 * main method for launching the simulation.
	 * 1) init phase: the graph, the environment, the 
	 * mobility model and the broadcasting algorithm are 
	 * initialized. 
	 * 2) iterative phase: at each time step, all nodes enable
	 *    to communicate, perform the broadcast, then all nodes 
	 *    are moving according to the chosen mobility model.
	 *    For simple flooding, this phase stops as soon as 
	 *    during one time step no transmission has been done.  
	 * 3) statistical results are displayed in the console/terminal.
 	 */
	private void moveAndBroadcast() {
		// execution of the algorithm		
		boolean finished = false;
		int nbIterations = 0;		
		int nbOn = 0, nbOff = 0;
		ArrayList<Node> vT = new ArrayList<>();
		ArrayList<Node> vTprev = new ArrayList<>();
		ArrayList<Edge> eT = new ArrayList<>();
		ArrayList<Edge> eTprev = new ArrayList<>();
		
		while(!finished) {
			nbIterations++;
			if(broadcastingStrategy == NO_BROADCASTING) {
				if(nbIterations > maxIterations) finished = true;
			} else {
				for(Node u:readyToSend) {
					switch(broadcastingStrategy) {
					case SIMPLE_FLOODING:
						simpleFlooding(u); 
						break;
					case SIMPLE_FLOODING_WITH_DELAY:
						break;
					}
				}
				readyToSend.clear();
				for(Node u:stations) {
					if(u.hasAttribute("readyToSend") && !readyToSend.contains(u)) {
						readyToSend.add(u);
						u.removeAttribute("readyToSend");
					}
				}
				if(readyToSend.size() == 0) finished=true;
			}
			// moving
			for(Node u:stations) {
				switch(mobilityModel) {
				case RWP:
					moveRWP(u);
					break;
				case MANHATTAN:
					moveManhattan(u);
					break;
				case MARKOVIAN:
					moveMarkovian(u);
					break;
				}
			}			
			verifyEdges();			
			switch(Scenario) {
			case 1:							
				for(Node u: stations) {
					int lifetime = u.getAttribute("message_lifetime");
					if(lifetime>0) {
						u.setAttribute("message_lifetime", lifetime-1);
						if (lifetime-1 <= 0) {
							if(u.hasAttribute("hasTheMessage")) {
								u.removeAttribute("hasTheMessage");}
							if(u.hasAttribute("nbOfReceptions")){u.removeAttribute("nbOfReceptions");}
							if(u.hasAttribute("readyToSend")){u.removeAttribute("readyToSend");}
							if(u.hasAttribute("notTransmittedYet")){u.setAttribute("notTransmittedYet", false);}
							if(u.hasAttribute("ui.style")){u.setAttribute("ui.style", TTLexpired);}						
						}
					}
				
				}
				break;
			case 2:				
				Collection<Node> nodes = g.getNodeSet();
				double nodeSize = nodes.size();
				double numofNodes = nodeSize*r;
				int numberofNodes = (int) numofNodes;		
				
				removeFromGraph(g, numberofNodes,envSize);	
				addToGraph(g,numberofNodes, d,envSize,indexNodes);
				updateGraph();
				for(Node u:stations)
				{
					chooseDestination(u);
				}
				indexNodes += numberofNodes;
				break;
			}
			switch(mobilityModel) {		
			case MARKOVIAN:		
				for(Edge e:g.getEdgeSet()) {
					if(e.getAttribute("state")==null) {
						e.addAttribute("state", alea.nextBoolean());
						if((boolean)e.getAttribute("state")) {
							e.addAttribute("ui.style",edgeONstyle);
							nbOn++;
						}
						else {
							e.addAttribute("ui.style",edgeOFFstyle);
							nbOff++;
						}
					}
					else {			
						if((boolean)e.getAttribute("state")) { // edge is on
							if(alea.nextDouble() > p) { // change of its state
								e.addAttribute("state",false);
								e.addAttribute("ui.style",edgeOFFstyle);
								nbOff++;
							} else {
								e.addAttribute("ui.style",edgeONstyle);
								nbOn++;
							}
						} else { // edge is off 
							if(alea.nextDouble() > q) { // change of its state 
								e.addAttribute("state",true);
								e.addAttribute("ui.style",edgeONstyle);
								nbOn++;
							} else {
								e.addAttribute("ui.style",edgeOFFstyle);
								nbOff++;
							}
						}
					}
				}
				break;
			default:
				break;
			}			
			Tools.pause(delay);
			for(Node u:stations) {
				vT.add(u);
			}
			for(Edge e:g.getEdgeSet()) {
				if(mobilityModel == MARKOVIAN) {
					if((boolean)e.getAttribute("state")) {
						eT.add(e);
					}
				}
				else {					
					eT.add(e);
				}				
			}			
			
			iterationStatistics(nbIterations, vT, vTprev, eT, eTprev);
			for (Node u: vT) {
				vTprev.add(u);
			}			
			vT.clear();
			for (Edge e: eT) {
				eTprev.add(e);
			}			
			eT.clear();
			System.out.println("nb iterations:"+nbIterations);
			Tools.hitakey("Start new interation");
			System.out.println("\n");
			
		}	
		
		statistics(nbIterations);
	}
	
	
	// ================= STATISTICS ================
	
	public void iterationStatistics(int nbIter, ArrayList<Node> vT,	ArrayList<Node> vTprev,	ArrayList<Edge> eT, 
	ArrayList<Edge> eTprev) {
		double dens = 0;
		int hasMessage = 0;
		double vNervousness = 0;
		double eNervousness = 0;		
		ConnectedComponents cc = new ConnectedComponents();
		if (mobilityModel != MARKOVIAN) {
			dens = Toolkit.density(g);
			cc.init(g);
		}
		else {
			SingleGraph h = g;
			for(Edge e:h.getEdgeSet()) {
				if(!(boolean)e.getAttribute("state")) {
					h.removeEdge(e);
				}
			}
			dens = Toolkit.density(h);				
			cc.init(h);
		}
		for(Node u:stations) {
			if(u.hasAttribute("hasTheMessage")) {
				hasMessage += 1;
			}
		}
		if (Scenario == 2) {
			//Vertices nervousness
			ArrayList<Node> vSum = new ArrayList<>();
			ArrayList<Node> vCross = new ArrayList<>();
			for(Node u: vT) {
				vSum.add(u);
				if(vTprev.contains(u)) {
					vCross.add(u);
				}
			}
			for(Node u: vTprev) {
				if(!vSum.contains(u)) {
					vSum.add(u);
				}
			}
			ArrayList<Node> vDiff = new ArrayList<>();
			for (Node u: vSum) {
				if(!vCross.contains(u)) {
					vDiff.add(u);
				}				
			}
			vNervousness = (double)vDiff.size()/(double)vSum.size();
		}
		//Edge nervousness
		ArrayList<Edge> eSum = new ArrayList<>();
		ArrayList<Edge> eCross = new ArrayList<>();
		for(Edge e: eT) {
			eSum.add(e);
			if(eTprev.contains(e)) {
				eCross.add(e);
			}
		}
		for(Edge e: eTprev) {
			if(!eSum.contains(e)) {
				eSum.add(e);
			}
		}
		ArrayList<Edge> eDiff = new ArrayList<>();
		for (Edge e: eSum) {
			if(!eCross.contains(e)) {
				eDiff.add(e);
			}				
		}
		eNervousness = (double)eDiff.size()/(double)eSum.size();
		System.out.println("Statistics at iteration "+ nbIter + ":");
		System.out.println("Graph density:"+ dens);
		System.out.println("Vertices owning the message:"+ hasMessage);
		if (Scenario == 2) {
			System.out.println("Vertice nervousness (compared with the previous iteration):"+ vNervousness);
		}
		System.out.println("Edge nervousness(compared with the previous iteration):"+ eNervousness);
		System.out.println("Connected components:"+ cc.getConnectedComponentsCount());	
		
	}	
	
	
	/**
	 * in this method, we measure:
	 * the Performance (of the algorithm): ratio between the number of 
	 *       nodes with the message over the total number of stations
	 * the Efficiency (of the algorithm): average number of received 
	 * message for the stations which receive messages 
	 * Max performance: 1, min performance: 1/n 
	 * Max Efficiency: 1, min efficiency: large number  
	 * 
	 */	
	public void statistics(int nbIter) {
		int nbReachedStations = 0;
		int sumOfReceivedMessages = 0;
		for(Node u: stations) {
			if(u.hasAttribute("hasTheMessage")) {
				nbReachedStations++;
				sumOfReceivedMessages += (int)u.getAttribute("nbOfReceptions");
			}
		}
		System.out.println("Performance ["+stations.size()+"/"+d+"/"
				+mobilityModel+"/"+broadcastingStrategy+"] "
				+"("+nbIter+","
				+nbReachedStations+","+(float)sumOfReceivedMessages/nbReachedStations+")");
	}
	
	// ==================== BROADCASTING STRATEGIES ===========
	

	
	/**
	 * each node verifies if it has a message
	 * if it has already received the message, and if it 
	 * has not been transmitted it yet, AND if the 
	 * message was NOT received during the current time step
	 * (otherwise, the simulation would not be synchronous),
	 * it sends the message to all its neighbors. 
	 * --> each node requires two attributes: theMessage and 
	 * the information about its transmission
	 * we can add also the number of times each node receives 
	 * the message
	 * @param u
	 */
	public void simpleFlooding(Node u) {
		if((boolean)u.getAttribute("notTransmittedYet")) {			
			if(stepByStep) Tools.hitakey("node "+u.getId()+" will broadcast");
			u.setAttribute("notTransmittedYet",false);
			if(u.getId() != source.getId()) u.addAttribute("ui.style",completedTaskStyle);
			Iterator<Node> neighbors = u.getNeighborNodeIterator();
			while(neighbors.hasNext()) {
				Node v = neighbors.next();
				boolean CanReceive = true;
				if (Scenario == 1) {
					//can receive if TTL = 0
					int lifetime = v.getAttribute("message_lifetime");
					if(lifetime > 0) {
						CanReceive = false;
					}
				}
				if(CanReceive && !v.hasAttribute("hasTheMessage")) {
					v.addAttribute("hasTheMessage",true);
					v.addAttribute("nbOfReceptions",1);
					v.addAttribute("readyToSend",true);
					v.addAttribute("notTransmittedYet",true);
					v.addAttribute("ui.style",senderStyle);
					if (Scenario == 1) {
						v.setAttribute("message_lifetime", TTL);
					}
				} else { // we add 1 to the number of receptions
					v.addAttribute("nbOfReceptions",
						(int)v.getAttribute("nbOfReceptions")+1); 
				}	
			}
			}
		}
	
	
	
	
	// ==================== MOBILITY MODELS ==================


	/**
	 * Node u moves according to the Manhattan mobility model 
	 */
	public void moveManhattan(Node u) {
		if(arrivedAtDestination(u)) chooseDestination(u);
		else moveStraight(u);
	} 

	/**
	 * Individual movement of a node according to the RWP 
	 * mobility model
	 * @param u
	 */
	public void moveRWP(Node u) {
		if(arrivedAtDestination(u)) chooseDestination(u);
		else moveStraight(u);
	}
	
	public void moveMarkovian(Node u) {		
		if(arrivedAtDestination(u)) chooseDestination(u);
		else moveStraight(u);
	}
	
	
	/**
	 * meethod verifies if the station is at destination
	 * @param u
	 * @return
	 */
	public boolean arrivedAtDestination(Node u) {
		boolean arrived = false;
		double ux = u.getAttribute("x");
		double uy = u.getAttribute("y");
		double dx = u.getAttribute("xdest");
		double dy = u.getAttribute("ydest");
		if((ux == dx) && (uy == dy)) arrived = true;
		return arrived;
	}
	
	/**
	 * this method computes the next position of node u
	 * @param u
	 */
	public void moveStraight(Node u) {
		double ux = u.getAttribute("x");
		double uy = u.getAttribute("y");
		double dx = u.getAttribute("xdest");
		double dy = u.getAttribute("ydest");
		if(Tools.distance(ux,uy,dx,dy) > proximityThreshold) {
			double xMove = dx-ux;
			double yMove = dy-uy;
			double Norm = Math.sqrt(xMove*xMove + yMove*yMove);
			double speed = (double)u.getAttribute("speed");
			double newX = ux + speed*(xMove/Norm);
			double newY = uy + speed*(yMove/Norm);
			u.addAttribute("x",newX);
			u.addAttribute("y",newY);
		} else {
			u.addAttribute("x",dx);
			u.addAttribute("y",dy);
		}
	}
	
	/**
	 * for the RWP mobility model chooses a new destination.
	 * @param u
	 * @param mS
	 * @param model
	 */
	public void chooseDestination(Node u) {
		u.setAttribute("speed",1+alea.nextDouble()*maxSpeed);
		switch(mobilityModel) {
		case RWP:
			u.setAttribute("xdest",alea.nextDouble()*envSize);
			u.setAttribute("ydest",alea.nextDouble()*envSize);
			break;
		case MANHATTAN:
			if(alea.nextBoolean()) { // movement on the x axis
				u.setAttribute("xdest",(double)(1+alea.nextInt(nbParallelStreets-1))*distanceInterStreets);
				u.setAttribute("ydest",(double)u.getAttribute("y"));
			} else {
				u.setAttribute("xdest",(double)u.getAttribute("x")); 
				u.setAttribute("ydest",(double)(1+alea.nextInt(nbParallelStreets-1))*distanceInterStreets);
			}
			break;
		case MARKOVIAN:
			//same here as RWP as edge markovian is more about the state of the edges than about way of choosing destination
			u.setAttribute("xdest",alea.nextDouble()*envSize);
			u.setAttribute("ydest",alea.nextDouble()*envSize);
			break;
		}
	}
	
	/**
	 * long edges has to be removed and new ones have to be added
	 */
	public void verifyEdges() {
		for(Node u:stations) {
			for(Node v:stations) {
				if(u.getId() != v.getId()) {
					if((Generator.distance(u,v) < d) && (!u.hasEdgeBetween(v))) {
						g.addEdge(u.getId()+"--"+v.getId(),u.getId(),v.getId());
					} else if((Generator.distance(u,v) > d) && (u.hasEdgeBetween(v))) {
						g.removeEdge((u.getEdgeBetween(v)).getId());
					}
				}
			}
		}
	}
	
	
	// ============= INITIALIZATION METHODS ===============
	

	/**
	 * initialization of the broadcast
	 */
	public void initBroadcast() {
		source = null;
		readyToSend = new ArrayList<>();
		source = Toolkit.randomNode(g);
		source.addAttribute("hasTheMessage",true);
		source.addAttribute("nbOfReceptions",0);
		source.addAttribute("notTransmittedYet",true);
		source.addAttribute("ui.style",sourceStyle);
		if (Scenario == 1)
		{
			source.setAttribute("message_lifetime", TTL);
		}
		readyToSend.add(source);
	}
	

	public void initMobility() {
		switch(mobilityModel) {
		case RWP:
			for(Node u:stations) chooseDestination(u);
			break;
		case MANHATTAN:
			for(Node u:stations) chooseFirstDestination(u);
			// for avoiding streets at the border of the environment
			if(envSize % nbParallelStreets == 0) {
				distanceInterStreets = envSize/nbParallelStreets - 2;
			}
			break;
		case MARKOVIAN:
			//same as RWP
			for(Node u:stations) chooseDestination(u);
			break;			
		}
	}
	
	
	public void chooseFirstDestination(Node u) {
		u.setAttribute("speed",1+alea.nextDouble()*maxSpeed);
		double x = u.getAttribute("x");
		double y = u.getAttribute("y");
		if(alea.nextBoolean()) {
			if(x/distanceInterStreets < 1) x = distanceInterStreets;
			else x = (int)(x/distanceInterStreets)*distanceInterStreets;
			u.setAttribute("x",x);
			u.setAttribute("xdest",x);
			u.setAttribute("ydest",(double)(1+alea.nextInt(nbParallelStreets-1))*distanceInterStreets);
		} else {
			if(y/distanceInterStreets < 1) y = distanceInterStreets;
			else y = (int)(y/distanceInterStreets)*distanceInterStreets;
			u.setAttribute("y",y);
			u.setAttribute("xdest",(double)(1+alea.nextInt(nbParallelStreets-1))*distanceInterStreets);
			u.setAttribute("ydest",y);
		}
	}
	
	//podać parametry to punktów i krawędzi odpowiednie dla:
	//edge-markovian (krawędzie obecne lub nie)
	//scenario 1 (message lifetime)
	//oprócz tego dla Scenario 2 coś zrobić z generacją (nowa metoda?)
	/**
	 * For the first scenario, generators, RWP, Manhattan and Edge-Markovian, can be used in their
original versions. For the second scenario however, it is necessary to remove and add vertices
between two consecutive time steps. We consider a specific parameter, called r ranging from 0
to 1 for modifying the generators. If Vt denotes the set of vertices at time t and nt = |Vt|, then
between t and t + 1, r × nt vertices are removed from the set and r × nt are added to the set,
such that |Vt+1| = |Vt|. In other words, the order remains constant during the tests. The removed
nodes are randomly chosen and new added nodes are randomly positioned in the area for RWP
and Manhattan.
	 */
	public void initGraph() {
		g = Generator.randomGeometricGraphW(nbNodes,d,envSize);
		g.addAttribute("ui.antialias");
		g.display(false);
		stations = new ArrayList<>();
		for(Node u: g.getNodeSet()) {
			stations.add(u);
			if(labelOnNodes) {
				u.setAttribute("ui.label",u.getId());
				u.setAttribute("ui.style","text-alignment:above;");
			}
			switch(Scenario) {
			case(1):
				u.addAttribute("message_lifetime", 0);
				break;			
			case(2):
				break;
			}
		}
		// construction of the environment
		Node ne = g.addNode("north-east");
		ne.setAttribute("x",(double)envSize);
		ne.setAttribute("y",(double)envSize);
		ne.setAttribute("ui.style","fill-color:green;size:1px;");
		Node nw = g.addNode("north-west");
		nw.setAttribute("x",(double)0);
		nw.setAttribute("y",(double)envSize);
		nw.setAttribute("ui.style","fill-color:green;size:1px;");
		Node sw = g.addNode("south-west");
		sw.setAttribute("x",(double)0);
		sw.setAttribute("y",(double)0);
		sw.setAttribute("ui.style","fill-color:green;size:1px;");
		Node se = g.addNode("south-east");
		se.setAttribute("x",(double)envSize);
		se.setAttribute("y",(double)0);
		se.setAttribute("ui.style","fill-color:green;size:1px;");
		if (mobilityModel == MARKOVIAN) {
			for(Edge e: g.getEdgeSet()) {
				e.addAttribute("state",alea.nextBoolean()); // on is true off is false
			}
		}
		
	}
	
	public void updateGraph() {
		Iterator<Node> it = stations.iterator();
		while (it.hasNext()) {
			Node v = it.next();
			if (v.hasAttribute("remove")) {
				it.remove();
			}
		}
		/*for(Node u: stations) {
			if (u.hasAttribute("remove")) {
				stations.remove(u);
				u.removeAttribute("remove");
			}
		}*/
		for(Node u: g.getNodeSet()) {			
			if (u.hasAttribute("add")) {
				stations.add(u);
				u.removeAttribute("add");
			}
		}
	}
	public void removeFromGraph(SingleGraph g, int numberOfNodes, int environmentSize) {
		int n = numberOfNodes;
		int size = environmentSize;
		Random alea = new Random(System.currentTimeMillis());
		for(int i=0;i<n;i++) {			
			Node u = Toolkit.randomNode(g);					
				while (checkCorner(u, size))
					{
						u = Toolkit.randomNode(g);
					}
			u.addAttribute("remove", true);
			g.removeNode(u);
		}
	}
	
	public boolean checkCorner(Node u, int environmentSize)
	{
		int size = environmentSize;		
		double x = u.getAttribute("x");
		double y = u.getAttribute("y");
		if (x == size || x == 0) {
			if (y == size || y == 0) {
				return true;				
			}			
		}
		if (y == size || y == 0) {
			if (x == size || x == 0) {
				return true;				
			}	
		}
		return false;
		
	}
	
	public void addToGraph(SingleGraph g, int numberOfNodes, double distanceThrehold, int environmentSize, int index) {		
		int n = numberOfNodes;
		int size = environmentSize;
		double d = distanceThrehold;
		Random alea = new Random(System.currentTimeMillis());
		ArrayList<Node> added = new ArrayList<>();
		// Nodes creation with their coordinates
		for(int i=0;i<n;i++) {
			Node u = g.addNode("u_"+(index+i));
			added.add(u);
			// random position of the node within the environment
			double x = alea.nextDouble()*size;
			double y = alea.nextDouble()*size;
			u.setAttribute("x",x);
			u.setAttribute("y",y);
			u.addAttribute("add", true);
		}
		// add edges
		for(Node u:g.getNodeSet()) {
			for(Node v:added) {
				if(u.getId() != v.getId()) {
					if((Generator.distance(u,v) < d) && (!u.hasEdgeBetween(v)) && !checkCorner(u, envSize)) {
						Edge e = g.addEdge(u.getId()+"--"+v.getId(),u.getId(),v.getId());
						if(mobilityModel==MARKOVIAN) {
							e.addAttribute("state",alea.nextBoolean()); // on is true off is false
						}
					}
				}
			}
		}
	}
	
	// ============= MAIN ================
	
	public static void main(String[] args) {
        System.setProperty("org.graphstream.ui.renderer", 
        		"org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        new Broadcasting();
	}

}
