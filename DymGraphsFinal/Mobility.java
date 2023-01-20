package y2022_2023.uksw;

import org.graphstream.graph.implementations.SingleGraph;
import java.util.Random;

import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Node;

public class Mobility {

	Random alea = new Random(System.currentTimeMillis());
	SingleGraph g;
	int d = 100;
	int envSize = 1000;
	int delay = 20;
	
	public Mobility() {
		randomGeometric();
	}
	

	private void randomGeometric() {
		g = Generator.randomGeometricGraphW(150,d,envSize);
		g.addAttribute("ui.antialias");
		g.display(false);
		Tools.hitakey("move");
		//brownianMotion();
		//randomWayPoint();
		//manhattan();
		followerMobilityModel();
	}
	
	
	
	/**
	 * in the follower mobility model, each node chooses not a 
	 * location as destination but a target. 
	 * At each time step the node goes into the direction of 
	 * this target.
	 */
	public void followerMobilityModel() {
		double maxSpeed = 5.0;
		// destination choice 
		for(Node u:g.getNodeSet()) { 
			chooseTarget(u,maxSpeed);
			u.setAttribute("tracking",true);
		}
		// movement
		double proximityThreshold = 4;
		for(int iter=0;iter<10000;iter++) {
			for(Node u:g.getNodeSet()) {
				if((boolean) u.getAttribute("tracking")) {
					double ux = u.getAttribute("x");
					double uy = u.getAttribute("y");
					Node target = u.getAttribute("target");
					double dx = target.getAttribute("x");
					double dy = target.getAttribute("y");
					// test if u is very close (threshold) to dest.
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
						u.setAttribute("tracking",false);
						u.setAttribute("ui.style","fill-color:"+chooseColor());
					}
				}
			}
			verifyEdges();
			Tools.pause(delay);
 		}
	}
	
	public void chooseTarget(Node u, double mS) {
		u.setAttribute("speed",1+alea.nextDouble()*mS);
		Node target = Toolkit.randomNode(g);
		while(target.getId().equals(u.getId()))
			target = Toolkit.randomNode(g);
		u.addAttribute("target",target);
	}
	
	/**
	 * each node chooses a destination located either on 
	 * its x axis or on its y axis. 
	 */
	public void manhattan() {
		double maxSpeed = 5.0;
		// destination choice 
		for(Node u:g.getNodeSet()) chooseDestination(u,maxSpeed,"manhattan");
		// movement
		double proximityThreshold = 4;
		for(int iter=0;iter<10000;iter++) {
			for(Node u:g.getNodeSet()) {
				double ux = u.getAttribute("x");
				double uy = u.getAttribute("y");
				double dx = u.getAttribute("xdest");
				double dy = u.getAttribute("ydest");
				if((ux == dx) && (uy == dy)) {
					chooseDestination(u,maxSpeed,"manhattan");
				}
				// test if u is very close (threshold) to dest.
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
					//u.addAttribute("ui.style","fill-color:"+chooseColor());
				}
			}
			verifyEdges();
			Tools.pause(delay);
 		}
	}
	
	/*
	 * each node chooses a destination and 
	 * at each time step goes into the direction of 
	 * the destination. 
	 * 
	 */
	public void randomWayPoint() {
		double maxSpeed = 5.0;
		// destination choice 
		for(Node u:g.getNodeSet()) chooseDestination(u,maxSpeed,"rwp");
		// movement
		double proximityThreshold = 4;
		for(int iter=0;iter<10000;iter++) {
			for(Node u:g.getNodeSet()) {
				double ux = u.getAttribute("x");
				double uy = u.getAttribute("y");
				double dx = u.getAttribute("xdest");
				double dy = u.getAttribute("ydest");
				if((ux == dx) && (uy == dy)) {
					chooseDestination(u,maxSpeed,"rwp");
				}
				// test if u is very close (threshold) to dest.
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
					u.addAttribute("ui.style","fill-color:"+chooseColor());
				}
			}
			verifyEdges();
			Tools.pause(delay);
 		}
	}
	
	public void chooseDestination(Node u, double mS, String model) {
		u.setAttribute("speed",1+alea.nextDouble()*mS);
		if(model.equals("rwp")) {
			u.setAttribute("xdest",alea.nextDouble()*envSize);
			u.setAttribute("ydest",alea.nextDouble()*envSize);
		} else if(model.equals("manhattan")) {
			if(alea.nextBoolean()) { // movement on the x axis
				u.setAttribute("xdest",(double)((int)(alea.nextDouble()*envSize/100)*100));
				u.setAttribute("ydest",(double)u.getAttribute("y"));
			} else {
				u.setAttribute("xdest",(double)u.getAttribute("x")); 
				u.setAttribute("ydest",(double)((int)(alea.nextDouble()*envSize/100)*100));
			}
		}
	}
	
	
	public String chooseColor() {
		String color = "rgb(";
		color+=alea.nextInt(250)+","; //red 
		color+=alea.nextInt(250)+","; // green
		color+=alea.nextInt(250)+");"; //blue
		return color;
	}
	
	
	public void brownianMotion() {
		int step = 5;
		for(int iter=0;iter<1000;iter++) {
			for(Node u:g.getNodeSet()) {
				double x = u.getAttribute("x");
				double y = u.getAttribute("y");
				double nx = x+step-alea.nextDouble()*(2*step);
				double ny = y+step-alea.nextDouble()*(2*step);
				if((nx <= envSize) && (nx >= 0)) u.setAttribute("x",nx);
				if((ny <= envSize) && (ny >= 0)) u.setAttribute("y",ny);
			}
			verifyEdges();
			Tools.pause(100);
		}
	}
	
	/**
	 * long edges has to be removed and new ones have to be added
	 */
	public void verifyEdges() {
		for(Node u:g.getNodeSet()) {
			for(Node v:g.getNodeSet()) {
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
	
	
	
	
	public static void main(String[] args) {
        System.setProperty("org.graphstream.ui.renderer", 
        		"org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        new Mobility();
	}

}
