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

package y2022_2023.uksw;

import org.graphstream.graph.implementations.SingleGraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Node;


public class Broadcasting {
	
	public final static int RWP = 1;
	public final static int MANHATTAN = 2;

	public final static int NO_BROADCASTING = 10;
	public final static int SIMPLE_FLOODING = 11;
	public final static int SIMPLE_FLOODING_WITH_DELAY = 12;

	public final static String sourceStyle = "fill-color: red;shape:cross;";
	public final static String senderStyle = "fill-color:blue;";
	public final static String completedTaskStyle = "fill-color:green;";


	// execution parameters 
	int delay = 50;
	Random alea = new Random(System.currentTimeMillis());
	SingleGraph g;
	ArrayList<Node> stations;
	boolean stepByStep = true;
	boolean labelOnNodes = false;
	int maxIterations = 10000; // if no broadcasting algo is running
	
	// mobility and graph parameters
	int nbNodes = 300;
	double maxSpeed = 5.0;
	double proximityThreshold = 4;
	int d = 70;
	int envSize = 1000;
	int mobilityModel = RWP; // MANHATTAN; // 
	int nbParallelStreets = 10;
	int distanceInterStreets = (int)(envSize/nbParallelStreets);
	
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
				}
			}
			verifyEdges();
			Tools.pause(delay);
			System.out.println("nb iterations:"+nbIterations);
		}
		statistics(nbIterations);
	}
	
	
	// ================= STATISTICS ================
	
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
				if(!v.hasAttribute("hasTheMessage")) {
					v.addAttribute("hasTheMessage",true);
					v.addAttribute("nbOfReceptions",1);
					v.addAttribute("readyToSend",true);
					v.addAttribute("notTransmittedYet",true);
					v.addAttribute("ui.style",senderStyle);
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
		}
		// construction of the environment
		Node ne = g.addNode("north-east");
		ne.setAttribute("x",envSize);
		ne.setAttribute("y",envSize);
		ne.setAttribute("ui.style","fill-color:green;size:1px;");
		Node nw = g.addNode("north-west");
		nw.setAttribute("x",0);
		nw.setAttribute("y",envSize);
		nw.setAttribute("ui.style","fill-color:green;size:1px;");
		Node sw = g.addNode("south-west");
		sw.setAttribute("x",0);
		sw.setAttribute("y",0);
		sw.setAttribute("ui.style","fill-color:green;size:1px;");
		Node se = g.addNode("south-east");
		se.setAttribute("x",envSize);
		se.setAttribute("y",0);
		se.setAttribute("ui.style","fill-color:green;size:1px;");
		
	}
	
	// ============= MAIN ================
	
	public static void main(String[] args) {
        System.setProperty("org.graphstream.ui.renderer", 
        		"org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        new Broadcasting();
	}

}
