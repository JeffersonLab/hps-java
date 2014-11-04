package org.hps.monitoring.ecal.eventdisplay.exec;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

import org.hps.monitoring.ecal.eventdisplay.ui.FileViewer;
import org.hps.monitoring.ecal.eventdisplay.ui.DataFileViewer;

/**
 * The class <code>Main</code> can be used to create an event display
 * that reads from file. The input file can be set with the "-i" command,
 * though any file can be loaded from the file menu after initialization.
 * Calorimeter wiring information can be loaded with the"-w" command.
 */
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
                System.out.printf("%-4s%-12s%s%n", "-w", "--wiring", "Defines the wiring data file.");
                System.exit(0);
            }
        }
        
        // If -i or --input was given, set the input file.
        File dataSource = null;
        String filepath = null;
        for(int i = 0; i < args.length; i++) {
            if(args[i].compareTo("-i") == 0 || args[i].compareTo("--input") == 0) {
                if(args.length <= i + 1) { System.out.println("Error: Expected additional argument."); }
                else { filepath = args[i + 1]; }
            }
        }
        if(filepath != null) { dataSource = new File(filepath); }
        
        // If -w or --wiring was given, set the input file.
        String wiringPath = null;
        for(int i = 0; i < args.length; i++) {
            if(args[i].compareTo("-w") == 0 || args[i].compareTo("--witing") == 0) {
                if(args.length <= i + 1) { System.out.println("Error: Expected additional argument."); }
                else { wiringPath = args[i + 1]; }
            }
        }
        if(filepath != null) { dataSource = new File(filepath); }
        
        // Define the viewer.
        FileViewer window;
        if(wiringPath != null) { window = new DataFileViewer(dataSource, wiringPath); }
        else { window = new FileViewer(dataSource); }
        
        // Get screen size of primary monitor
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        
        // Set the viewer window location and make it visible.
        window.setLocation((screenWidth - window.getPreferredSize().width) / 2,
                (screenHeight - window.getPreferredSize().height) / 2);
        window.setVisible(true);
    }
}