package org.hps.monitoring.ecal.exec;

import org.hps.monitoring.ecal.io.TextManager;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.IOException;

import org.hps.monitoring.ecal.ui.ActiveViewer;
import org.hps.monitoring.ecal.ui.ClusterViewer;
import org.hps.monitoring.ecal.ui.FileViewer;
import org.hps.monitoring.ecal.ui.OccupancyViewer;

/**
 * The class <code>Main</code> can be used to create an event display that
 * reads from file. By default it reads from "cluster-hit.txt" at the class
 * path root. This can be changed by altering the line<br/>
 * <code>window.setDataSource("cluster-hit.txt")</code><br/>
 **/
public class Main {
	
    public static void main(String[] args) throws IOException {
    	// If "-h" was given as a command line argument, print the
    	// help text.
    	for(String s : args) {
    		if(s.compareTo("-h") == 0 || s.compareTo("--help") == 0) {
    			System.out.println("HPS Event Display");
    			System.out.println("Options:");
    			System.out.printf("%-4s%-12s%s%n", "-h", "--help", "Display help text.");
    			System.out.printf("%-4s%-12s%s%n", "-i", "--input", "Defines the input file.");
    			System.out.printf("%-4s%-12s%s%n", "-t", "--type", "Select the type of display.");
    			System.out.printf("%-4s%-12s%s%n", "", "", "Allowed types: Event, Cluster, Occupancy");
    			System.exit(0);
    		}
    	}
    	
    	// If -i or --input was given, set the input file.
    	String filepath = "raw-hits.txt";
    	for(int i = 0; i < args.length; i++) {
    		if(args[i].compareTo("-i") == 0 || args[i].compareTo("--input") == 0) {
    			if(args.length <= i + 1) { System.out.println("Error: Expected additional argument."); }
    			else { filepath = args[i + 1]; }
    		}
    	}
    	
        // Define the viewer. By default, we employ a file viewer.
        TextManager dataManager = new TextManager(filepath);
        ActiveViewer window = new FileViewer(dataManager);
        
        // Command line argument "-t" allows a different type to be declared.
        if(args.length >= 2 && (args[0].compareTo("-t") == 0 || args[0].compareTo("--type") == 0)) {
        	// If an occupancy viewer has been specified...
        	if(args[1].compareToIgnoreCase("Occupancy") == 0) {
                window = new OccupancyViewer(dataManager);
        	}
        	// If an event viewer has been specified...
        	else if(args[1].compareToIgnoreCase("Event") == 0) {
                window = new FileViewer(dataManager);
        	}
        	// If a cluster viewer has been specified...
        	else if(args[1].compareToIgnoreCase("Cluster") == 0) {
                window = new ClusterViewer(dataManager, 2);
        	}
        	// Otherwise, it is an invalid type.
        	else {
        		System.out.println("Display type \"" + args[1] + "\" not recognized.");
        		System.exit(0);
        	}
        }
        
        // Get screen size of primary monitor
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        
        // Set the viewer window location and make it visible.
        window.setLocation((screenWidth - window.getPreferredSize().width) / 2,
                (screenHeight - window.getPreferredSize().height) / 2);
        window.setVisible(true);
        
        // Start the viewer with the first event.
        window.displayNextEvent();
    }
}
