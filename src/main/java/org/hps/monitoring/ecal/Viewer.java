package org.hps.monitoring.ecal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import org.lcsim.event.EventHeader;
import org.lcsim.hps.recon.ecal.HPSCalorimeterHit;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;

/**
 * The class <code>Viewer</code> handles initialization of the calorimeter panel
 * with the proper settings, provides a window for it to live in, and feeds it
 * events.
 * 
 * @author Kyle McCarty
 **/
public class Viewer extends JFrame {
    // Java-suggested variable.
    private static final long serialVersionUID = -2022819652687941812L;
    // The calorimeter panel.
    private final EcalPanel ecalPanel = new EcalPanel(46, 11);
    // The crystal status panel.
    private final StatusPanel statusPanel = new StatusPanel("x Index", "y Index", "Energy");
    // The event data reader.
    private EventManager em = null;
    // Map the cluster location to the cluster object.
    private HashMap<Point, Cluster> clusterMap = new HashMap<Point, Cluster>();
    // Whether crystal should be highlighted when hovered over.
    private boolean highlight = true;
    // Store the last crystal to be highlighted.
	private Point lastCrystal = null;
	// DEPRECATED :: Store the old highlight color.
	//private Color oldHighlight = null;
	// DEPRECATED :: Store the currently highlighted cluster.
	//private Point lastCluster = null;
	// Stores whether the background color is set or not.
	private boolean background = false;
	// Define the highlight color for dark backgrounds.
	private static final Color HIGHLIGHT_CURSOR_DARK = new Color(90, 170, 250);
	// Define the highlight color for light backgrounds.
	private static final Color HIGHLIGHT_CURSOR_LIGHT = Color.BLUE;
	// Define the highlight color for cluster component hits.
	private static final Color HIGHLIGHT_CLUSTER_COMPONENT = Color.RED;
	// Define the highlight color for cluster shared hits.
	private static final Color HIGHLIGHT_CLUSTER_SHARED = Color.YELLOW;
	private boolean processing = false;
    
    /**
     * <b>Viewer</b><br/><br/>
     * <code>public <b>Viewer</b>()</code><br/><br/>
     * Initializes the viewer window and calorimeter panel.
     **/
    public Viewer() {
        // Initialize the viewer window
        super();
        setTitle("HPS Ecal Cluster Viewer");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1060, 600));
        setMinimumSize(new Dimension(1060, 525));
        setLayout(null);
        
        // Set the scaling settings.
        ecalPanel.setMinimum(0.0001);
        ecalPanel.setMaximum(3000);
        ecalPanel.setScalingLogarithmic();
        
        // Disable the crystals in the calorimeter panel along the beam gap.
        for (int i = -23; i < 24; i++) {
            ecalPanel.setCrystalEnabled(getPanelX(i), 5, false);
            if (i > -11 && i < -1) {
                ecalPanel.setCrystalEnabled(getPanelX(i), 4, false);
                ecalPanel.setCrystalEnabled(getPanelX(i), 6, false);
            }
        }
        
        // Make a key listener to change events.
        addKeyListener(new EcalKeyListener());
        
        // Make a mouse motion listener to monitor mouse hovering.
        getContentPane().addMouseListener(new EcalMouseListener());
        getContentPane().addMouseMotionListener(new EcalMouseMotionListener());
        
        // Add the panels.
        add(ecalPanel);
        add(statusPanel);
        
        // Add a listener to update everything when the window changes size
        addComponentListener(new ResizeListener());
    }
    
    public void setSize(int width, int height) {
        super.setSize(width, height);
        resize();
    }
    
    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }
    
    /**
     * <b>setDataSouce</b><br/><br/>
     * <code>public void <b>setDataSource</b>(String filepath)</code><br/><br/>
     * Sets the viewer to read from the indicated data source.
     * @param filepath - The full path to the desired data file.
     * @throws IOException Occurs when there is an error opening the data file.
     **/
    public void setDataSource(String filepath) throws IOException {
        em = new EventManager(filepath);
    }
    
    /**
     * <b>displayNextEvent</b><br/><br/>
     * <code>public void <b>displayNextEvent</b>()</code><br/><br/>
     * Feeds the calorimeter panel the data from the next event.
     * @throws IOException Occurs when there is an issue with reading the data file.
     **/
    public void displayNextEvent() throws IOException {
        // Clear the calorimeter panel.
        ecalPanel.clearCrystals();
        ecalPanel.clearHighlight();
        
        // Clear the cluster map and cluster highlighting.
        clusterMap.clear();
        resetCursor();
        
        // If there is no data source, we can not do anything.
        if (em == null) { return; }
        
        // Otherwise, get the next event.
        em.readEvent();
        
        // Display it.
        for (EcalHit h : em.getHits()) {
            int ix = getPanelX(h.getX());
            int iy = getPanelY(h.getY());
            ecalPanel.addCrystalEnergy(ix, iy, h.getEnergy());
        }
        for (Cluster c : em.getClusters()) {
        	Point seedHit = c.getSeedHit();
            int ix = getPanelX(seedHit.x);
            int iy = getPanelY(seedHit.y);
            ecalPanel.setCrystalCluster(ix, iy, true);
            clusterMap.put(new Point(getPanelX(seedHit.x), getPanelY(seedHit.y)), c);
        }
        
        // Redraw the calorimeter panel.
        ecalPanel.redraw();
    }
    
    public void displayLCIOEvent(EventHeader event, String ecalCollectionName, String clusterCollectionName) {
        // Make sure that a draw is not in process.
        if(processing) { return; }

        // Otherwise, we are now processing.
        processing = true;

        // Get the list of clusters and hits.
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollectionName);
        List<HPSCalorimeterHit> hits = event.get(HPSCalorimeterHit.class, ecalCollectionName);
        
        // Reset the calorimeter panel.
        ecalPanel.clearCrystals();
        
        // Add the calorimeter hits.
        for(HPSCalorimeterHit hit : hits) {
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            double energy = hit.getRawEnergy();
            ecalPanel.addCrystalEnergy(getPanelX(ix), getPanelY(iy), energy);
        }
        
        // Add clusters.
        for(HPSEcalCluster cluster : clusters) {
            HPSCalorimeterHit seed = (HPSCalorimeterHit) cluster.getSeedHit();
            int ix = seed.getIdentifierFieldValue("ix");
            int iy = seed.getIdentifierFieldValue("iy");
            ecalPanel.setCrystalCluster(getPanelX(ix), getPanelY(iy), true);
        }
        
        // Redraw the panel.
        ecalPanel.redraw();
        
        // We are finished drawing.
        processing = false;
    }
    
    /**
     * <b>resize</b><br/><br/>
     * <code>private void <b>resize</b>()</code><br/><br/>
     * Handles proper resizing of the window and its components.
     **/
    private void resize() {
    	// Define the status panel height.
    	int statusHeight = 125;
    	
        // Size and position the calorimeter display.
        ecalPanel.setLocation(0, 0);
        ecalPanel.setSize(getContentPane().getWidth(), getContentPane().getHeight() - statusHeight);
        
        // Size and position the status panel.
        statusPanel.setLocation(0, ecalPanel.getHeight());
        statusPanel.setSize(getContentPane().getWidth(), statusHeight);
    }
    
    /**
     * <b>getPanelX</b><br/><br/>
     * <code>public int <b>getPanelX</b>(int ecalX)</code><br/><br/>
     * Converts the LCSim x-coordinate to the calorimeter panel's coordinate
     * system.
     * @param ecalX - An LCSim calorimeter x-coordinate.
     * @return Returns the x-coordinate in the calorimeter panel's coordinate
     * system as an <code>int</code>.
     **/
    public int getPanelX(int ecalX) {
        if (ecalX <= 0) { return ecalX + 23; }
        else { return ecalX + 22; }
    }
    
    /**
     * <b>getPanelY</b><br/><br/>
     * <code>public int <b>getPanelY</b>(int ecalY)</code><br/><br/>
     * Converts the LCSim y-coordinate to the calorimeter panel's coordinate
     * system.
     * @param ecalY - An LCSim calorimeter y-coordinate.
     * @return Returns the y-coordinate in the calorimeter panel's coordinate
     * system as an <code>int</code>.
     **/
    public int getPanelY(int ecalY) { return 5 - ecalY; }
    
    /**
     * <b>getEcalX</b><br/><br/>
     * <code>public int <b>getEcalX</b>(int panelX)</code><br/><br/>
     * Converts the panel x-coordinate to the calorimeter's coordinate
     * system.
     * @param panelX - A panel x-coordinate.
     * @return Returns the x-coordinate in the calorimeter's coordinate
     * system as an <code>int</code>.
     */
    public int getEcalX(int panelX) {
    	if(panelX > 22) { return panelX - 22; }
    	else { return panelX - 23; }
    }
    
    /**
     * <b>getEcalY</b><br/><br/>
     * <code>public int <b>getEcalY</b>(int panelY)</code><br/><br/>
     * Converts the panel y-coordinate to the calorimeter's coordinate
     * system.
     * @param panelY - A panel y-coordinate.
     * @return Returns the y-coordinate in the calorimeter's coordinate
     * system as an <code>int</code>.
     */
    public int getEcalY(int panelY) { return 5 - panelY; }
    
    /**
     * <b>resetCursor</b><br/><br/>
     * <code>private void <b>resetCursor</b>()</code><br/><br/>
     * Performs the cursor highlight calculations for whatever the
     * most recently highlighted crystal was. This should be called
     * if the panel is cleared to reset the cursor highlight.
     */
    private void resetCursor() {
    	Point temp = lastCrystal;
    	lastCrystal = null;
    	setCursorHighlight(temp);
    }
    
    /**
     * <b>setCursorHighlight</b><br/><br/>
     * <code>private boolean <b>setCursorHighlight</b>(Point crystal)</code><br/><br/>
     * Sets the highlighting for the indicated crystal and clears the
     * highlighting on any previously highlighted crystal. Note that
     * this will clear any existing highlighting.
     * @param crystal - The crystal to highlight.
     * @return Returns <code>true</code> if a different crystal is
     * highlighted than before and <code>false</code> if it is the
     * same crystal.
     */
    private boolean setCursorHighlight(Point crystal) {
		// Get the appropriate highlight color.
		Color hc = null;
		if(!background) { hc = HIGHLIGHT_CURSOR_DARK; }
		else { hc = HIGHLIGHT_CURSOR_LIGHT; }
		
		// Define helper variables.
		boolean crystalChanged;
		boolean cNull = (crystal == null);
		boolean lNull = (lastCrystal == null);
		
		// Determine if the selected crystal has changed.
		if(cNull && lNull) { crystalChanged  = false; }
		else if(cNull ^ lNull) { crystalChanged = true; }
		else { crystalChanged = !lastCrystal.equals(crystal); }
		
		// If so, clear the highlighting and reset it.
		if(crystalChanged) {
			// Clear the old highlighting.
			ecalPanel.clearHighlight();
			
			// If the current crystal is a cluster, highlight the cluster.
			Cluster cluster = clusterMap.get(crystal);
			if(highlight && cluster != null) {
				for(Point ch : cluster.getComponentHits()) {
					ecalPanel.setCrystalHighlight(getPanelX(ch.x), getPanelY(ch.y), HIGHLIGHT_CLUSTER_COMPONENT);
				}
				for(Point sh : cluster.getSharedHits()) {
					ecalPanel.setCrystalHighlight(getPanelX(sh.x), getPanelY(sh.y), HIGHLIGHT_CLUSTER_SHARED);
				}
			}
			
			// If the current crystal is defined, highlight it.
			if(highlight && crystal != null) { ecalPanel.setCrystalHighlight(crystal.x, crystal.y, hc); }
		}
		
		// Set the last crystal to match the current one.
		lastCrystal = crystal;
		
		// Return whether a redraw is necessary.
		return crystalChanged;
    }
    
    /**
     * The <code>EcalListener</code> class binds the enter key to the
     * <code>displayNextEvent</code> method. Also allows for toggling
     * of highlighting with 'h' and background with 'b.' Swaps scale
     * from linear to logarithmic with 'l'.
     **/
    private class EcalKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) { }
        
        public void keyReleased(KeyEvent e) {
            // If enter was pressed, go to the next event.
            if (e.getKeyCode() == 10) {
                try { displayNextEvent(); }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }
            // 'b' toggles the default white background.
            else if(e.getKeyCode() == 66) {
            	if(background) { ecalPanel.setCrystalDefaultColor(null); }
            	else { ecalPanel.setCrystalDefaultColor(Color.WHITE); }
            	ecalPanel.redraw();
            	background = !background;
            }
            // 'h' toggles highlighting the crystal under the cursor.
            else if(e.getKeyCode() == 72) {
            	if(highlight) {
            		if(setCursorHighlight(null)) { ecalPanel.redraw(); }
            	}
            	highlight = !highlight;
            }
            // 'l' toggles linear or logarithmic scaling.
            else if(e.getKeyCode() == 76) {
            	if(ecalPanel.isScalingLinear()) { ecalPanel.setScalingLogarithmic(); }
            	else { ecalPanel.setScalingLinear(); }
            	ecalPanel.redraw();
            }
            else { System.out.printf("Key Code: %d%n", e.getKeyCode()); }
        }
        
        public void keyTyped(KeyEvent e) { }
    }
    
    /**
     * The <code>EcalMouseListener</code> handles removing highlighting
     * and crystal field information when the cursor leaves the window.
     */
    private class EcalMouseListener implements MouseListener {
		public void mouseClicked(MouseEvent e) { }
		
		public void mouseEntered(MouseEvent e) { }
		
		public void mouseExited(MouseEvent e) {
			setCursorHighlight(null);
			statusPanel.clearValues();
			ecalPanel.redraw();
		}
		
		public void mousePressed(MouseEvent e) { }
		
		public void mouseReleased(MouseEvent e) { }
    }
    
    /**
     * The <code>EcalMouseMotionListener</code> handles updating of
     * the highlighted crystal and status panel information when the
     * mouse moves over the window.
     */
    private class EcalMouseMotionListener implements MouseMotionListener {    	
		public void mouseDragged(MouseEvent arg0) { }
		
		public void mouseMoved(MouseEvent e) {
			// Get the mouse coordinates, corrected for the panel position.
			int correctedX = e.getX();
			int correctedY = e.getY();
			
			// Get the crystal that the mouse is in.
			Point crystal = ecalPanel.getCrystalID(correctedX, correctedY);
			
			// Mark the current crystal for highlighting.
			boolean redraw = setCursorHighlight(crystal);
			
			// If this necessitates a redraw of the panel, do so.
			if(redraw) {
				lastCrystal = crystal;
				ecalPanel.redraw();
				
				if(crystal != null) {
					// Determine if the crystal is in the beam gap.
					boolean[] beamGap = new boolean[2];
					beamGap[0] = (getEcalY(crystal.y) == 0);
					beamGap[1] = Math.abs(getEcalY(crystal.y)) == 1 &&
							(getEcalX(crystal.x) >= -10 && getEcalX(crystal.x) <= -2);
					
					if(beamGap[0] || beamGap[1]) { statusPanel.clearValues(); }
					else {
						statusPanel.setFieldValue(0, String.valueOf(getEcalX(crystal.x)));
						statusPanel.setFieldValue(1, String.valueOf(getEcalY(crystal.y)));
						DecimalFormat formatter = new DecimalFormat("0.####E0");
						String energy = formatter.format(ecalPanel.getCrystalEnergy(crystal.x, crystal.y));
						statusPanel.setFieldValue(2, energy);
					}
				}
				else { statusPanel.clearValues(); }
			}
		}
    }
    
    /**
     * The <code>ResizeListener</code> class ensures that the components remain
     * at the correct size and location when the window is resized.
     **/
    private class ResizeListener implements ComponentListener {
        public void componentResized(ComponentEvent e) { resize(); }
        
        public void componentHidden(ComponentEvent e) { }
        
        public void componentMoved(ComponentEvent e) { }
        
        public void componentShown(ComponentEvent e) { }
    }
}
