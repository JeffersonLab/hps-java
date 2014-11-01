package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.hps.monitoring.ecal.eventdisplay.io.EventManager;

/**
 * Abstract class <code>ActiveViewer</code> describes a <code>Viewer
 * </code> object that is connected to a static data source. It is
 * designed to instruct the data source when to provide new events.
 * 
 * @author Kyle McCarty
 */
public abstract class ActiveViewer extends Viewer {
    private static final long serialVersionUID = -6107646224627009923L;
    // Stores whether the background color is set or not.
    private boolean background = false;
    // Gets events from some file.
    protected final EventManager em;
    
    /**
     * Creates an active-type <code>Viewer</code> window which draws
     * events from the indicated data source with additional status
     * fields defined by the <code>fieldNames</code> argument.
     * @param em - The data source event manager.
     * @param fieldNames - An array of additional status fields
     * that should be displayed.
     */
    public ActiveViewer(EventManager em) {
        // Pass any additional field values to the super class.
        super();
        
        // Set the data source.
        this.em = em;
        
        // Make a key listener to change events.
        addKeyListener(new EcalKeyListener());
    }
    
    /**
     * Feeds the calorimeter panel the data from the next event.
     * @throws IOException Occurs when there is an issue with reading the data file.
     **/
    public abstract void displayNextEvent() throws IOException;
    
    /**
     * Feeds the calorimeter panel the data from the previous event.
     * @throws IOException Occurs when there is an issue with reading the data file.
     **/
    public abstract void displayPreviousEvent() throws IOException;
    
    /**
     * The <code>EcalListener</code> class binds keys to actions.
     * Bound actions include:
     * [Right Arrow] :: Next event (stand-alone mode only)
     * [Left Arrow ] :: Previous event (stand-alone mode only)
     * b             :: Toggle color-mapping for 0 energy crystals
     * h             :: Toggle selected crystal highlighting
     * l             :: Toggle logarithmic versus linear scaling
     * s             :: Saves the current display to a file
     **/
    private class EcalKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) { }
        
        public void keyReleased(KeyEvent e) {
            // If right-arrow was pressed, go to the next event.
            if (e.getKeyCode() == 39) {
                try { displayNextEvent(); }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }
            
            // If right-arrow was pressed, go to the next event.
            else if (e.getKeyCode() == 37) {
                try { displayPreviousEvent(); }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }
            
            // 'b' toggles the default white background.
            else if(e.getKeyCode() == 66) {
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
            
            // 'x' toggles x-axis mirroring.
            else if(e.getKeyCode() == 88) {
                ecalPanel.setMirrorX(!ecalPanel.isMirroredX());
                updateStatusPanel();
            }
            
            // 'y' toggles y-axis mirroring.
            else if(e.getKeyCode() == 89) {
                ecalPanel.setMirrorY(!ecalPanel.isMirroredY());
                updateStatusPanel();
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
