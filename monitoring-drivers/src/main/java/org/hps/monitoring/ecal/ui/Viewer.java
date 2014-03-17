package org.hps.monitoring.ecal.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

import javax.swing.JFrame;

import org.hps.monitoring.ecal.util.CrystalEvent;
import org.hps.monitoring.ecal.util.CrystalListener;

/**
 * The abstract class <code>Viewer</code> handles initialization of the
 * calorimeter panel with the proper settings and provides a window for
 * it to live in. Subclasses of <code>Viewer</code> should implement a
 * means for events to be fed to the calorimeter display.
 * 
 * @author Kyle McCarty
 **/
public abstract class Viewer extends JFrame {
    // Java-suggested variable.
    private static final long serialVersionUID = -2022819652687941812L;
    // A map of field names to field indices.
    private final HashMap<String, Integer> fieldMap = new HashMap<String, Integer>();
    // A list of crystal listeners attached to the viewer.
    private ArrayList<CrystalListener> listenerList = new ArrayList<CrystalListener>();
    // The default field names.
    private static final String[] defaultFields = { "x Index", "y Index", "Cell Value" };
    
    /**
     * <b><statusPanel/b><br/><br/>
     * <code>protected final StatusPanel <b>statusPanel</b></code><br/><br/>
     * The component responsible for displaying status information 
     * about the currently selected crystal.
     */
    protected final StatusPanel statusPanel;
    
    /**
     * <b>ecalPanel</b><br/><br/>
     * <code>protected final CalorimeterPanel <b>ecalPanel</b></code><br/><br/>
     * The panel displaying the calorimeter crystals and scale.
     */
    protected final CalorimeterPanel ecalPanel = new CalorimeterPanel(46, 11);
	
    /**
     * <b>HIGHLIGHT_CLUSTER_COMPONENT</b><br/><br/>
     * <code>public static final Color <b>HIGHLIGHT_CLUSTER_COMPONENT</b></code><br/><br/>
     * The default color for highlighting cluster components.
     */
	public static final Color HIGHLIGHT_CLUSTER_COMPONENT = Color.RED;
	
	/**
	 * <b>HIGHLIGHT_CLUSTER_SHARED</b><br/><br/>
     * <code>public static final Color <b>HIGHLIGHT_CLUSTER_SHARED</b></code><br/><br/>
     * The default color for highlighting cluster shared hits.
	 */
	public static final Color HIGHLIGHT_CLUSTER_SHARED = Color.YELLOW;
    
    /**
     * <b>Viewer</b><br/><br/>
     * <code>public <b>Viewer</b>(String... statusFields)</code><br/><br/>
     * Initializes the viewer window and calorimeter panel.
     * @param statusFields - Additional fields to display in the status
     * panel. This can not be <code>null</code>.
     * @throws NullPointerException Occurs if any of the additional field
     * arguments are <code>null</code>.
     **/
    public Viewer(String... statusFields) throws NullPointerException {
        // Initialize the underlying JPanel.
        super();
        
        // Define the status panel fields and map them to indices.
        String[] fields = new String[statusFields.length + 3];
        for(int i = 0; i < defaultFields.length; i++) {
        	fields[i] = defaultFields[i];
        	fieldMap.put(defaultFields[i], i);
        }
        for(int i = 0; i < statusFields.length; i++) {
        	int index = i + 3;
        	fields[index] = statusFields[i];
        	fieldMap.put(statusFields[i], index);
        }
        
        // Generate the status panel.
        statusPanel = new StatusPanel(fields);
        
        // Set the scaling settings.
        ecalPanel.setScaleMinimum(0.0001);
        ecalPanel.setScaleMaximum(3000);
        ecalPanel.setScalingLogarithmic();
        
        // Disable the crystals in the calorimeter panel along the beam gap.
        for (int i = -23; i < 24; i++) {
            ecalPanel.setCrystalEnabled(toPanelX(i), 5, false);
            if (i > -11 && i < -1) {
                ecalPanel.setCrystalEnabled(toPanelX(i), 4, false);
                ecalPanel.setCrystalEnabled(toPanelX(i), 6, false);
            }
        }
        
        // Make a mouse motion listener to monitor mouse hovering.
        getContentPane().addMouseListener(new EcalMouseListener());
        getContentPane().addMouseMotionListener(new EcalMouseMotionListener());
        
        // Add the panels.
        add(ecalPanel);
        add(statusPanel);
        
        // Define viewer panel properties.
        setTitle("HPS Ecal Event Display");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1060, 600));
        setMinimumSize(new Dimension(1060, 525));
        setLayout(null);
        
        // Add a listener to update everything when the window changes size
        addComponentListener(new ResizeListener());
    }
    
    /**
     * <b>addCrystalListener</b><br/><br/>
     * <code>public void <b>addCrystalListener</b>(CrystalListener cl)</code><br/><br/>
     * Adds the specified crystal listener to receive crystal events
     * from this component when the calorimeter panel's crystal status
     * is changed. If listener <code>cl</code> is <code>null</code>,
     * no exception is thrown and no action is performed. 
     * @param cl - The listener to add.
     */
    public void addCrystalListener(CrystalListener cl) {
    	if(cl != null) { listenerList.add(cl); }
    }
    
    /**
     * <b>toEcalPoint</b><br/><br/>
     * <code>public Point <b>toEcalPoint</b>(Point panelPoint)</code><br/><br/>
     * Converts the calorimeter panel's coordinate pair to the LCSim
     * coordinate system.
     * @param panelPoint - A calorimeter panel coordinate pair..
     * @return Returns the coordinate pair in LCSim's coordinate system
     * as an <code>int</code>.
     **/
	public static final Point toEcalPoint(Point panelPoint) {
		// Convert the point coordinates.
		int ix = toEcalX(panelPoint.x);
		int iy = toEcalY(panelPoint.y);
		
		// Return the new point.
		return new Point(ix, iy);
	}
    
    /**
     * <b>toEcalX</b><br/><br/>
     * <code>public int <b>toEcalX</b>(int panelX)</code><br/><br/>
     * Converts the panel x-coordinate to the calorimeter's
     * coordinate system.
     * @param panelX - A panel x-coordinate.
     * @return Returns the x-coordinate in the calorimeter's
     * coordinate system as an <code>int</code>.
     */
    public static final int toEcalX(int panelX) {
    	if(panelX > 22) { return panelX - 22; }
    	else { return panelX - 23; }
    }
    
    /**
     * <b>toEcalY</b><br/><br/>
     * <code>public int <b>toEcalY</b>(int panelY)</code><br/><br/>
     * Converts the panel y-coordinate to the calorimeter's
     * coordinate system.
     * @param panelY - A panel y-coordinate.
     * @return Returns the y-coordinate in the calorimeter's
     * coordinate system as an <code>int</code>.
     */
    public static final int toEcalY(int panelY) { return 5 - panelY; }
    
    /**
     * <b>toPanelPoint</b><br/><br/>
     * <code>public Point <b>toPanelPoint</b>(Point ecalPoint)</code><br/><br/>
     * Converts the LCSim coordinate pair to the calorimeter panel's
     * coordinate system.
     * @param ecalPoint - An LCSim calorimeter coordinate pair..
     * @return Returns the coordinate pair in the calorimeter panel's
     * coordinate system as an <code>int</code>.
     **/
	public static final Point toPanelPoint(Point ecalPoint) {
		// Convert the point coordinates.
		int ix = toPanelX(ecalPoint.x);
		int iy = toPanelY(ecalPoint.y);
		
		// Return the new point.
		return new Point(ix, iy);
	}
    
    /**
     * <b>toPanelX</b><br/><br/>
     * <code>public int <b>toPanelX</b>(int ecalX)</code><br/><br/>
     * Converts the LCSim x-coordinate to the calorimeter panel's
     * coordinate system.
     * @param ecalX - An LCSim calorimeter x-coordinate.
     * @return Returns the x-coordinate in the calorimeter panel's
     * coordinate system as an <code>int</code>.
     **/
    public static final int toPanelX(int ecalX) {
        if (ecalX <= 0) { return ecalX + 23; }
        else { return ecalX + 22; }
    }
    
    /**
     * <b>toPanelY</b><br/><br/>
     * <code>public int <b>toPanelY</b>(int ecalY)</code><br/><br/>
     * Converts the LCSim y-coordinate to the calorimeter panel's
     * coordinate system.
     * @param ecalY - An LCSim calorimeter y-coordinate.
     * @return Returns the y-coordinate in the calorimeter panel's
     * coordinate system as an <code>int</code>.
     **/
    public static final int toPanelY(int ecalY) { return 5 - ecalY; }
    
    /**
     * <b>removeCrystalListener</b><br/><br/>
     * <code>public void <b>removeCrystalListener</b>(CrystalListener cl)</code><br/><br/>
     * Removes the specified crystal listener so that it no longer
     * receives crystal events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this
     * component. If listener <code>cl</code> is <code>null</code>, no
     * exception is thrown and no action is performed. 
     * @param cl - The listener to remove.
     */
    public void removeCrystalListener(CrystalListener cl) {
    	if(cl != null) { listenerList.remove(cl); }
    }
    
    public void setSize(int width, int height) {
        super.setSize(width, height);
        resize();
    }
    
    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }
    
    /**
     * <b>setStatusField</b><br/><br/>
     * <code>public void <b>setStatusField</b>(String fieldName, String value)</code><br/><br/>
     * Sets the value of the indicated status field on the calorimeter
     * display.
     * @param fieldName - The name of the field to set.
     * @param value - The value to display in relation to the field.
     * @throws NoSuchElementException Occurs if an invalid field name
     * is provided for argument <code>fieldName</code>.
     */
    public final void setStatusField(String fieldName, String value) throws NoSuchElementException {
    	// Get the index for the indicated field.
    	Integer index = fieldMap.get(fieldName);
    	
    	// If it is null, the field does not exist.
    	if(index == null) { throw new NoSuchElementException("Field \"" + fieldName + "\" does not exist."); }
    	
    	// Otherwise, set the field.
    	else { statusPanel.setFieldValue(index, value); }
    }
    
	/**
	 * <b>updateStatusPanel</b><br/><br/>
	 * <code>protected void <b>updateStatusPanel</b>()</code><br/><br/>
	 * Updates the information on the status panel to match that of
	 * the calorimeter panel's currently selected crystal.
	 */
	protected void updateStatusPanel() {
		// Get the currently selected crystal.
		Point crystal = ecalPanel.getSelectedCrystal();
		
		// If the crystal is null, there is no selection.
		if(crystal == null || ecalPanel.isCrystalDisabled(crystal.x, crystal.y)) { statusPanel.clearValues(); }
		
		// Otherwise, write the crystal's data to the panel.
		else {
			setStatusField(defaultFields[0], String.valueOf(toEcalX(crystal.x)));
			setStatusField(defaultFields[1], String.valueOf(toEcalY(crystal.y)));
			DecimalFormat formatter = new DecimalFormat("0.####E0");
			String energy = formatter.format(ecalPanel.getCrystalEnergy(crystal.x, crystal.y));
			setStatusField(defaultFields[2], energy);
		}
	}
    
    /**
     * <b>resize</b><br/><br/>
     * <code>private void <b>resize</b>()</code><br/><br/>
     * Handles proper resizing of the window and its components.
     **/
    private void resize() {
    	// Define the size constants.
    	int statusHeight = 125;
    	
        // Size and position the calorimeter display.
        ecalPanel.setLocation(0, 0);
        ecalPanel.setSize(getContentPane().getWidth(), getContentPane().getHeight() - statusHeight);
        
        // Size and position the status panel.
        statusPanel.setLocation(0, ecalPanel.getHeight());
        statusPanel.setSize(getContentPane().getWidth(), statusHeight);
    }
    
    /**
     * The <code>EcalMouseListener</code> handles removing highlighting
     * and crystal field information when the cursor leaves the window.
     * It also triggers crystal click events.
     */
    private class EcalMouseListener implements MouseListener {
		public void mouseClicked(MouseEvent e) {
			// If there is a selected crystal, trigger a crystal click event.
			if(ecalPanel.getSelectedCrystal() != null) {
				// Get the selected crystal.
				Point crystal = ecalPanel.getSelectedCrystal();
				
				// Construct a crystal event.
				CrystalEvent ce = new CrystalEvent(Viewer.this, crystal);
				
				// Loop through all the crystal listeners and trigger them.
				for(CrystalListener cl : listenerList) { cl.crystalClicked(ce); }
			}
		}
		
		public void mouseEntered(MouseEvent e) { }
		
		public void mouseExited(MouseEvent e) {
			ecalPanel.clearSelectedCrystal();
			statusPanel.clearValues();
		}
		
		public void mousePressed(MouseEvent e) { }
		
		public void mouseReleased(MouseEvent e) { }
    }
    
    /**
     * The <code>EcalMouseMotionListener</code> handles updating of
     * the highlighted crystal and status panel information when the
     * mouse moves over the window. Additionally triggers crystal
     * activation and deactivation events.
     */
    private class EcalMouseMotionListener implements MouseMotionListener {    	
		public void mouseDragged(MouseEvent arg0) { }
		
		public void mouseMoved(MouseEvent e) {
			// Get the panel coordinates.
			int x = e.getX();
			int y = e.getY();
			
			// Get the crystal index for these coordinates.
			Point crystal = ecalPanel.getCrystalID(x, y);
			
			// If either of the crystal indices are negative, then
			// the mouse is not in a crystal and the selection should
			// be cleared.
			boolean validCrystal = (crystal != null);
			
			// Get the currently selected calorimeter crystal.
			Point curCrystal = ecalPanel.getSelectedCrystal();
			
			// Perform event comparison checks.
			boolean[] nullCrystal = { !validCrystal, curCrystal == null };
			boolean[] disabledCrystal = { true, true };
			if(!nullCrystal[0]) { disabledCrystal[0] = ecalPanel.isCrystalDisabled(crystal); }
			if(!nullCrystal[1]) { disabledCrystal[1] = ecalPanel.isCrystalDisabled(curCrystal); }
			boolean sameCrystal = true;
			if(validCrystal) { sameCrystal = crystal.equals(curCrystal); }
			
			// If the crystals are the same, there are no events to throw.
			if(!sameCrystal) {
				// If the new crystal is non-null and enabled, throw an event.
				if(!nullCrystal[0] && !disabledCrystal[0]) { throwActivationEvent(crystal); }
				
				// If the old crystal is non-null and enabled, throw an event.
				if(!nullCrystal[1] && !disabledCrystal[1]) { throwDeactivationEvent(curCrystal); }
			}
			
			// If the crystal is valid, then set the selected crystal
			// to the current one.
			if(validCrystal) { ecalPanel.setSelectedCrystal(crystal); }
			
			// Otherwise, clear the selection.
			else { ecalPanel.clearSelectedCrystal(); }
			
			// Update the status panel.
			updateStatusPanel();
		}
		
		/**
		 * <b>throwActivationEvent</b><br/><br/>
		 * <code>private void <b>throwActivationEvent</b>()</code><br/><br/>
		 * Triggers crystal activation events on all listeners for
		 * this component.
		 * @param activatedCrystal - The panel coordinates for the
		 * activated crystal.
		 */
		private void throwActivationEvent(Point activatedCrystal) {
			// Create a crystal event.
			CrystalEvent ce = new CrystalEvent(Viewer.this, activatedCrystal);
			
			// Throw the event with every listener.
			for(CrystalListener cl : listenerList) { cl.crystalActivated(ce); }
		}
		
		/**
		 * <b>throwDeactivationEvent</b><br/><br/>
		 * <code>private void <b>throwDeactivationEvent</b>()</code><br/><br/>
		 * Triggers crystal deactivation events on all listeners for
		 * this component.
		 * @param deactivatedCrystal - The panel coordinates for the
		 * deactivated crystal.
		 */
		private void throwDeactivationEvent(Point deactivatedCrystal) {
			// Create a crystal event.
			CrystalEvent ce = new CrystalEvent(Viewer.this, deactivatedCrystal);
			
			// Throw the event with every listener.
			for(CrystalListener cl : listenerList) { cl.crystalDeactivated(ce); }
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
