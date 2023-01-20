package proj;


import java.io.IOException;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkImages;

public class Tools {
	
	
	/**
	 * add a delay during the evolution of the graph, 
	 * mainly for visualization purpose
	 * @param delay
	 */
	public final static void pause(long delay) {
		try {
			Thread.sleep(delay);
		} catch(InterruptedException ie) {}
	}
	
	
	/**
	 * Stops the execution of the algorithm operating on 
	 * the graph and ask the user to hit a key for
	 * enabling the process to go on. 
	 * @param message
	 */
	public final static void hitakey(String message) {
		System.out.println("-----------------------");
		System.out.println(message);
		System.out.println("----- Hit a Key ------");
		try {
			System.in.read();
		} catch(IOException ioe) {}
	}
	
	
	/**
	 * building a graph from a file containing its dgs 
	 * description
	 * @param filename
	 * @return
	 */
	public final static SingleGraph read(String filename) {
		SingleGraph myGraph = new SingleGraph(filename);
		try {
			myGraph.read(filename);
		} catch(Exception e) {}
		return myGraph;
	}
	
	
	/**
	 * writing the dgs description of myGraph into a file 
	 * @param filename
	 * @param myGraph
	 */
	public final static void write(String filename, SingleGraph myGraph) {
		try {
			myGraph.write(filename);
		} catch(Exception e) {}
	}
	
	
	public final static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}

	/**
	 * taking a picture of the graph at a given moment. 
	 * Be aware that the snapshot is performed asynchronously so the 
	 * result might be different of what is expected. A pause in the 
	 * evolution process of the graph may be a good idea.
	 * @param myGraph
	 * @param filename
	 */
	public final static void screenshot(SingleGraph myGraph, String filename) { 
		if(myGraph != null) if(myGraph.getNodeCount() > 0) {
			FileSinkImages fsi = new FileSinkImages(FileSinkImages.OutputType.PNG, 
					FileSinkImages.Resolutions.SVGA);
			fsi.setLayoutPolicy(FileSinkImages.LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);
			try {
				fsi.writeAll(myGraph, filename);
			} catch (IOException e) { e.printStackTrace(); } 
		}
	}

}
