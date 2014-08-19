package org.hps.monitoring.ecal.ui;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.hps.monitoring.ecal.event.Cluster;
import org.hps.monitoring.ecal.event.EcalHit;

/**
 * Abstract class <code>PassiveViewer</code> represents a <code>Viewer
 * </code> implementation which updates based on information passed to
 * it by an external source.
 * 
 * @author Kyle McCarty
 */
public abstract class PassiveViewer extends Viewer {
	private static final long serialVersionUID = -7479125553259270894L;
	// Stores whether the background color is set or not.
	private boolean background = false;
	
	/**
	 * <b>PassiveViewer</b><br/><br/>
	 * <code>public <b>PassiveViewer</b>(String... fieldValues)</code><br/><br/>
	 * @param fieldValues
	 */
	public PassiveViewer(String... fieldValues) {
		// Pass the field values to the superclass.
		super(fieldValues);
		
		// Set the key bindings.
		addKeyListener(new EcalKeyListener());
	}
	
	/**
	 * <b>addHit</b><br/><br/>
	 * <code>public void <b>addHit</b>(EcalHit hit)</code><br/><br/>
	 * Adds a new hit to the display.
	 * @param hit - The hit to be added.
	 */
	public abstract void addHit(EcalHit hit);
	
	/**
	 * <b>addCluster</b><br/><br/>
	 * <code>public void <b>addCluster</b>(Cluster cluster)</code><br/><br/>
	 * Adds a new cluster to the display.
	 * @param cluster - The cluster to be added.
	 */
	public abstract void addCluster(Cluster cluster);
	
	/**
	 * <b>resetDisplay</b><br/><br/>
	 * <code>public void <b>resetDisplay</b>()</code><br/><br/>
	 * Clears any hits or clusters that have been added to the viewer.
	 * Note that this does not automatically update the displayed panel.
	 * <code>updateDisplay</code> must be called separately.
	 */
	public abstract void resetDisplay();
	
	/**
	 * <b>setScale</b><br/><br/>
	 * <code>public void <b>setScale</b>(int min, int max)</code><br/><br/>
	 * Sets the upper and lower bounds of for the calorimeter display's
	 * color mapping scale.
	 * @param min - The lower bound.
	 * @param max - The upper bound.
	 */
	public void setScale(int min, int max) {
		ecalPanel.setScaleMinimum(min);
		ecalPanel.setScaleMaximum(max);
	}
	
	/**
	 * <b>setScaleMaximum</b><br/><br/>
	 * <code>public void <b>setScaleMaximum</b>(int max)</code><br/><br/>
	 * Sets the upper bound for the calorimeter display's color mapping
	 * scale.
	 * @param max - The upper bound.
	 */
	public void setScaleMaximum(int max) { ecalPanel.setScaleMaximum(max); }
	
	/**
	 * <b>setScaleMinimum</b><br/><br/>
	 * <code>public void <b>setScaleMinimum</b>(int min)</code><br/><br/>
	 * Sets the lower bound for the calorimeter display's color mapping
	 * scale.
	 * @param min - The lower bound.
	 */
	public void setScaleMinimum(int min) { ecalPanel.setScaleMinimum(min); }
	
	/**
	 * <b>updateDisplay</b><br/><br/>
	 * <code>public void <b>updateDisplay</b>()</code><br/><br/>
	 * Displays the hits and clusters added by the <code>addHit</code>
	 * and <code>addCluster</code> methods.
	 */
	public abstract void updateDisplay();
	
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