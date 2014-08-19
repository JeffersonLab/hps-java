package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.hps.monitoring.ecal.eventdisplay.event.Association;
import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;

/**
 * Class <code>PEventViewer</code> represents a <code>PassiveViewer
 * </code> implementation which displays hits and clusters.
 * 
 * @author Kyle McCarty
 */
public class PEventViewer extends PassiveViewer {
	private static final long serialVersionUID = -7479125553259270894L;
	// Stores whether the background color is set or not.
	private boolean background = false;
	// Stores cluster objects.
	protected ArrayList<Cluster> clusterList = new ArrayList<Cluster>();
	// Stores hit objects.
	protected ArrayList<EcalHit> hitList = new ArrayList<EcalHit>();
	
	/**
	 * <b>PEventViewer</b><br/><br/>
	 * <code>public <b>PEventViewer</b>(String... fieldValues)</code><br/><br/>
	 * Creates a passive viewer for displaying hits and clusters in
	 * an event.
	 * @param fieldValues - Any additional status fields to display.
	 */
	public PEventViewer(String... fieldValues) {
		// Pass the field values to the superclass.
		super(fieldValues);
		
		// Set the key bindings.
		addKeyListener(new EcalKeyListener());
	}
	
	public void addHit(EcalHit hit) { hitList.add(hit); }
	
	public void addCluster(Cluster cluster) { clusterList.add(cluster); }
	
	/**
	 * <b>clearHits</b><br/><br/>
	 * <code>public void <b>clearHits</b>()</code><br/><br/>
	 * Removes all of the hit data from the viewer.
	 */
	public void clearHits() { hitList.clear(); }
	
	/**
	 * <b>clearClusters</b><br/><br/>
	 * <code>public void <b>clearClusters</b>()</code><br/><br/>
	 * Removes all of the cluster data from the viewer.
	 */
	public void clearClusters() { hitList.clear(); }
	
	public void resetDisplay() {
		// Reset the hit and cluster lists.
		hitList.clear();
		clusterList.clear();
	}
	
	public void updateDisplay() {
		// Suppress the calorimeter panel's redrawing.
		ecalPanel.setSuppressRedraw(true);
		
		// Clear the panel data.
		ecalPanel.clearCrystals();
		
        // Display the hits.
        for (EcalHit h : hitList) {
            int ix = toPanelX(h.getX());
            int iy = toPanelY(h.getY());
            ecalPanel.addCrystalEnergy(ix, iy, h.getEnergy());
        }
        
        // Display the clusters.
        for(Cluster cluster : clusterList) {
        	Point rawCluster = cluster.getClusterCenter();
        	Point clusterCenter = toPanelPoint(rawCluster);
            ecalPanel.setCrystalCluster(clusterCenter.x, clusterCenter.y, true);
            
        	// Add component hits to the calorimeter panel.
        	for(Point ch : cluster.getComponentHits()) {
        		ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(ch), HIGHLIGHT_CLUSTER_COMPONENT));
        	}
        	
        	// Add shared hits to the calorimeter panel.
        	for(Point sh : cluster.getSharedHits()) {
        		ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(sh), HIGHLIGHT_CLUSTER_SHARED));
        	}
        }
        
        // Stop suppressing the redraw and order the panel to update.
        ecalPanel.setSuppressRedraw(false);
        ecalPanel.repaint();
        
        // Update the status panel to account for the new event.
        updateStatusPanel();
	}
	
    /**
     * The <code>EcalListener</code> class binds keys to actions.
     * Bound actions include:
     * b             :: Toggle color-mapping for 0 energy crystals
     * h             :: Toggle selected crystal highlighting
     * l             :: Toggle logarithmic versus linear scaling
     * s			 :: Saves the current display to a file
     **/
    private class EcalKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) { }
        
        public void keyReleased(KeyEvent e) {
            // 'b' toggles the default white background.
            if(e.getKeyCode() == 66) {
            	if(background) { ecalPanel.setDefaultCrystalColor(null); }
            	else { ecalPanel.setDefaultCrystalColor(Color.GRAY); }
            	background = !background;
            }
            
            // 'h' toggles highlighting the crystal under the cursor.
            else if(e.getKeyCode() == 72) { ecalPanel.setSelectionHighlighting(!ecalPanel.isSelectionEnabled()); }
            
            // 'l' toggles linear or logarithmic scaling.
            else if(e.getKeyCode() == 76) {
            	if(ecalPanel.isScalingLinear()) { ecalPanel.setScalingLogarithmic(); }
            	else { ecalPanel.setScalingLinear(); }
            }
            
            // 's' saves the panel to a file.
            else if(e.getKeyCode() == 83) {
            	// Make a new buffered image on which to draw the content pane.
            	BufferedImage screenshot = new BufferedImage(getContentPane().getWidth(),
            			getContentPane().getHeight(), BufferedImage.TYPE_INT_ARGB);
            	
            	// Paint the content pane to image.
            	getContentPane().paint(screenshot.getGraphics());
            	
            	// Get the lowest available file name.
            	int fileNum = 0;
            	File imageFile = new File("screenshot_" + fileNum + ".png");
            	while(imageFile.exists()) {
            		fileNum++;
            		imageFile = new File("screenshot_" + fileNum + ".png");
            	}
            	
            	// Save the image to a PNG file.
            	try { ImageIO.write(screenshot, "PNG", imageFile); }
            	catch(IOException ioe) {
            		System.err.println("Error saving file \"screenshot.png\".");
            	}
            	System.out.println("Screenshot saved to: " + imageFile.getAbsolutePath());
            }
            
            // Otherwise, print out the key code for the pressed key.
            else { System.out.printf("Key Code: %d%n", e.getKeyCode()); }
        }
        
        public void keyTyped(KeyEvent e) { }
    }
}
