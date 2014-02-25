package org.hps.monitoring.ecal;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * The class <code>Main</code> can be used to create an event display that
 * reads from file. By default it reads from "cluster-hit.txt" at the class
 * path root. This can be changed by altering the line<br/>
 * <code>window.setDataSource("cluster-hit.txt")</code><br/>
 **/
public class Main {
    private static final Viewer window = new Viewer();
    
    public static void main(String[] args) throws IOException {
        // Get screen size of primary monitor
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        
        // Set the viewer location and make it visible
        window.setLocation((screenWidth - window.getPreferredSize().width) / 2,
                (screenHeight - window.getPreferredSize().height) / 2);
        window.setDataSource("cluster-hit.txt");
        window.displayNextEvent();
        
        /**
        int key = 0;
        while((key = System.in.read()) != 10) { }
        **/
        window.setVisible(true);
    }
    
    static void makeData() {
        // Generate a random test input file.
        Random rng = new Random();
        try {
            // Make a file writer to write the results.
            FileWriter writer = new FileWriter("cluster-hit.txt");
            
            // Make 10 - 100 events.
            int events = 10 + rng.nextInt(91);
            
            // For each events, generate some data.
            for (int e = 0; e < events; e++) {
                // Write the event header.
                writer.append("Event\n");
                
                // Make 3 - 15 hits.
                int hits = 3 + rng.nextInt(13);
                for (int h = 0; h < hits; h++) {
                    // Write identifier.
                    writer.append("EcalHit\t");
                    
                    // Make a random address.
                    // x = [0, 46); y = [0, 11)
                    int ix = rng.nextInt(46);
                    int iy = rng.nextInt(11);
                    writer.append(ix + "\t" + iy + "\n");
                }
                
                // Make 0 - 4 clusters.
                int clusters = rng.nextInt(5);
                for (int c = 0; c < clusters; c++) {
                    // Write identifier.
                    writer.append("Cluster\t");
                    
                    // Make a random address.
                    // x = [0, 46); y = [0, 11)
                    int ix = rng.nextInt(46);
                    int iy = rng.nextInt(11);
                    writer.append(ix + "\t" + iy + "\n");
                }
            }
            
            // Close the writer.
            writer.close();
        }
        catch (IOException e) {
            System.err.println(e.getStackTrace());
            System.exit(1);
        }
    }
}
