package org.hps.monitoring.ecal.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import org.hps.monitoring.ecal.event.Association;
import org.hps.monitoring.ecal.util.MultiGradientScale;

/**
 * The class <code>CalorimeterPanel</code> handles the rendering of the
 * calorimeter crystals as well as mapping colors to their values,
 * rendering a color scale, and marking cluster crystals.
 * 
 * @author Kyle McCarty
 **/
public final class CalorimeterPanel extends JPanel {
    // Java-suggested variable.
    private static final long serialVersionUID = 6292751227464151897L;
    // The color used for rendering seed hits.
    private Color clusterColor = Color.GREEN;
    // The default color of the calorimeter crystals.
    private Color defaultColor = null;
    // The color if disabled crystals.
    private Color disabledColor = Color.BLACK;
	// The color for the selected crystal.
	private Color selectedColor = new Color(90, 170, 250);
	// Whether to allow highlighting of the selected crystal.
	private boolean enabledSelection = true;
	// Whether the crystals should redraw automatically or not.
	private boolean suppress = false;
    // The color-mapping scale used by to color calorimeter crystals.
    private MultiGradientScale scale = MultiGradientScale.makeRainbowScale(0.0, 1.0);
    // The number of boxes in the x-direction.
    private int xBoxes = 1;
    // The number of boxes in the y-direction.
    private int yBoxes = 1;
    // The width of the scale.
    private int scaleWidth = 75;
    // Store the crystal panels.
    private Crystal[][] crystal;
    // Store the highest and lowest energy value observed.
    private double[] extremum = { Double.MAX_VALUE, 0.0 };
    // Store the currently selected crystal.
    private Point selectedCrystal = null;
    // The panel on which the scale is rendered.
    private ScalePanel scalePanel = new ScalePanel();
    
    // Efficiency variables for crystal placement.
    private int[] xPosition;
    private int[] yPosition;
    private int[] clusterSpace = new int[3];
    
    // ================================================================================
    // === Constructors ===============================================================
    // ================================================================================
    
    /**
     * <b>EcalPanel</b><br/><br/>
     * Initializes the calorimeter panel.
     * @param numXBoxes - The number of crystals in the x-direction.
     * @param numYBoxes - The number of crystals in the y-direction.
     **/
    public CalorimeterPanel(int numXBoxes, int numYBoxes) {
        // Initialize the base component.
        super();
        
        // Set the number of calorimeter crystals.
        xBoxes = numXBoxes;
        yBoxes = numYBoxes;
        
        // Initialize the crystal arrays.
        crystal = new Crystal[xBoxes][yBoxes];
        
        for(int x = 0; x < xBoxes; x++) {
        	for(int y = 0; y < yBoxes; y++) {
        		crystal[x][y] = new Crystal();
        		add(crystal[x][y]);
        	}
        }
        
        // Initialize the size arrays,
        xPosition = new int [xBoxes + 1];
        yPosition = new int[yBoxes + 1];
        
        // Add the scale panel.
        setLayout(null);
        add(scalePanel);
    }
    
    // ================================================================================
    // === Methods ====================================================================
    // ================================================================================
    
    /**
     * <b>addAssociation</b><br/><br/>
     * <code>public void <b>addAssociation</b>(Association crystalAssociation)</code><br/><br/>
     * Connects the parent crystal to the child crystal such that when
     * the parent crystal is active, the child will be highlighted with
     * the highlight color set in the <code>Asscoiation</code> object.
     * @param crystalAssociation
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates for either the parent or child crystals do
     * not correspond to a crystal.
     */
    public void addAssociation(Association crystalAssociation) throws IndexOutOfBoundsException {
    	// Validate the parent crystal's indices.
    	Point parent = crystalAssociation.getParentCrystal();
    	if(validateIndices(parent)) {
        	// Validate the parent crystal's indices.
    		Point child = crystalAssociation.getChildCrystal();
    		if(validateIndices(child)) {
    			// If both crystal index sets are valid, add the association.
    			crystal[parent.x][parent.y].addAssociation(crystalAssociation);
    		}
        	else { throw new IndexOutOfBoundsException(String.format("Invalid child crystal address (%2d, %2d).", parent.x, parent.y)); }
    	}
    	else { throw new IndexOutOfBoundsException(String.format("Invalid parent crystal address (%2d, %2d).", parent.x, parent.y)); }
    }
    
    /**
     * <b>addCrystalEnergy</b><br/><br/>
     * <code>public void <b>addCrystalEnergy</b>(int xIndex, int yIndex, double energy)</code><br/><br/>
     * Adds the indicated quantity of energy to the crystal at the given
     * coordinates.
     * @param ix - The x-index of the crystal.
     * @param iy - The y-index of the crystal.
     * @param energy - The energy to add.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     **/
    public void addCrystalEnergy(int ix, int iy, double energy) throws IndexOutOfBoundsException {
        if (validateIndices(ix, iy)) { crystal[ix][iy].addEnergy(energy); }
        else { throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy)); }
    }
    
    /**
     * <b>addCrystalEnergy</b><br/><br/>
     * <code>public void <b>addCrystalEnergy</b>(Point ixy, double energy)</code><br/><br/>
     * Adds the indicated quantity of energy to the crystal at the given
     * coordinates.
     * @param ixy - The crystal's x/y-indices.
     * @param energy - The energy to add.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     **/
    public void addCrystalEnergy(Point ixy, double energy) throws IndexOutOfBoundsException {
        addCrystalEnergy(ixy.x, ixy.y, energy);
    }
    
    /**
     * <b>autoScale</b><br/><br/>
     * <code>public void <b>autoScale</b>()</code><br/><br/>
     * Chooses a maximum and minimum value for the scale that goes
     * from slightly under the smallest recorded value to the highest
     * recorded value.
     */
	public void autoScale() {
		// If there is no energy on the calorimeter, just set the
		// scale to some default value.
		if(extremum[0] == Double.MAX_VALUE && extremum[1] == 0.0) {
			scale.setMaximum(1.0);
			scale.setMinimum(0.01);
		}
		
		// If the minimum is defined, set the scale such that it
		// will map that value to the low end and the highest value
		// to the top.
		else if(extremum[0] != Double.MAX_VALUE) {
			scale.setMaximum(extremum[1]);
			scale.setMinimum(extremum[0] / 2);
		}
	}
    
    /**
     * <b>clearCrystals</b><br/><br/>
     * <code>public void <b>clearCrystals</b>()</code><br/><br/>
     * Sets all crystal energies to zero, removes all clusters, and
     * clears all highlighting. This <b>does not</b> enable disabled
     * crystals.
     **/
    public void clearCrystals() {
        for (int ix = 0; ix < xBoxes; ix++) {
            for (int iy = 0; iy < yBoxes; iy++) {
            	crystal[ix][iy].setState(0.0, false, null);
            	crystal[ix][iy].clearAssociations();
            	extremum[0] = Double.MAX_VALUE;
            	extremum[1] = 0.0;
            }
        }
    }
    
    /**
     * <b>clearHighlight</b><br/><br/>
     * <code>public void <b>clearHighlight</b>()</code><br/><br/>
     * Clears any highlighting on the crystals.
     */
    public void clearHighlight() {
        for (int x = 0; x < xBoxes; x++) {
            for (int y = 0; y < yBoxes; y++) { crystal[x][y].setHighlight(null); }
        }
    }
    
    /**
     * <b>clearSelectedCrystal</b><br/><br/>
     * <code>public void <b>clearSelectedCrystal</b>()</code><br/><br/>
     * Clears crystal selection and sets it to <code>null</code>.
     */
    public void clearSelectedCrystal() {
    	// Clear any currently selected crystals.
    	if(selectedCrystal != null) { crystal[selectedCrystal.x][selectedCrystal.y].setSelected(false); }
    	
    	// Set the selected crystal to null.
    	selectedCrystal = null;
    }
    
    /**
     * <b>getCrystalEnergy</b><br/><br/>
     * <code>public double <b>getCrystalEnergy</b>(int ix, int iy)</code><br/><br/>
     * Provides the energy stored in the indicated crystal.
     * @param ix - The crystal's x-index.
     * @param iy - The crystal's y-index.
     * @return Returns the energy as a <code>double</code>.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public double getCrystalEnergy(int ix, int iy) throws IndexOutOfBoundsException {
    	if(validateIndices(ix, iy)) { return crystal[ix][iy].getEnergy(); }
        else { throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy)); }
    }
    
    /**
     * <b>getCrystalEnergy</b><br/><br/>
     * <code>public double <b>getCrystalEnergy</b>(Point ixy)</code><br/><br/>
     * Provides the energy stored in the indicated crystal.
     * @param ixy - The crystal's x/y-indices.
     * @return Returns the energy as a <code>double</code>.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public double getCrystalEnergy(Point ixy) throws IndexOutOfBoundsException {
    	return getCrystalEnergy(ixy.x, ixy.y);
    }
    
    /**
     * <b>getCrystalHighlight</b><br/><br/>
     * <code>public Color <b>getCrystalHighlight</b>(int ix, int iy)</code><br/><br/>
     * Provides the highlight color for the indicated crystal.
     * @param ix - The crystal's x-index.
     * @param iy - The crystal's y-index.
     * @return Returns the highlight color as a <code>Color</code>
     * object.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public Color getCrystalHighlight(int ix, int iy) throws IndexOutOfBoundsException {
    	if(validateIndices(ix, iy)) { return crystal[ix][iy].getHighlight(); }
        else { throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy)); }
    }
    
    /**
     * <b>getCrystalHighlight</b><br/><br/>
     * <code>public Color <b>getCrystalHighlight</b>(Point ixy)</code><br/><br/>
     * Provides the highlight color for the indicated crystal.
     * @param ixy - The crystal's x/y-indices.
     * @return Returns the highlight color as a <code>Color</code>
     * object.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public Color getCrystalHighlight(Point ixy) throws IndexOutOfBoundsException {
    	return getCrystalHighlight(ixy.x, ixy.y);
    }
    
    /**
     * <b>getCrystalID</b><br/><br/>
     * <code>public Point <b>getCrystalID</b>(int xCoor, int yCoor)</code><br/><br/>
     * Determines the panel crystal index of the crystal at the given
     * panel coordinates.
     * @param xCoor - The x-coordinate on the panel.
     * @param yCoor - The y-coordinate on the panel.
     * @return Returns a <code>Point</code> object containing the panel
     * crystal indices of the crystal at the given panel coordinates.
     * Returns <code>null</code> if the coordinates do not map to a crystal.
     */
    public Point getCrystalID(int xCoor, int yCoor) {
    	// If either coordinate is negative, return the null result.
    	if(xCoor < 0 || yCoor < 0) { return null; }
    	
    	// If either coordinate is too large, return the nul result.
    	if(xCoor > xPosition[xBoxes] || yCoor > yPosition[yBoxes]) {
    		return null;
    	}
    	
    	// Make a point to identify the crystal index.
    	Point loc = new Point(-1, -1);
    	
    	// Determine which y index it is.
    	for(int y = 0; y < yBoxes; y++) {
    		if(yCoor <= yPosition[y + 1]) {
    			loc.y = y;
    			break;
    		}
    	}
    	
    	// Determine which x index it is.
    	for(int x = 0; x < xBoxes; x++) {
    		if(xCoor <= xPosition[x + 1]) {
    			loc.x = x;
    			break;
    		}
    	}
    	
    	// If either coordinate is not valid, return null.
    	if(loc.x == -1 || loc.y == -1) { return null; }
    	
    	// Return the crystal identifier.
    	return loc;
    }
    
    /**
     * <b>getCrystalID</b><br/><br/>
     * <code>public Point <b>getCrystalID</b>(Point panelCoor)</code><br/><br/>
     * Determines the panel crystal index of the crystal at the given
     * panel coordinates.
     * @param panelCoor - The x/y-coordinates on the panel.
     * @return Returns a <code>Point</code> object containing the panel
     * crystal indices of the crystal at the given panel coordinates.
     * Returns <code>null</code> if the coordinates do not map to a crystal.
     */
    public Point getCrystalID(Point panelCoor) { return getCrystalID(panelCoor.x, panelCoor.y); }
    
    /**
     * <b>getCrystalBounds</b><br/><br/>
     * <code>public Dimension <b>getCrystalBounds</b>()</code><br/><br/>
     * Returns calorimeter panel's width and height in crystals.
     * @return Returns the number of crystals are on the calorimeter
     * in width and height.
     */
    public Dimension getCrystalBounds() { return new Dimension(xBoxes, yBoxes); }
    
    /**
     * <b>getNeighbors</b><br/><br/>
     * <code>public Set<Point> <b>getNeighbors</b>(int cix, int ciy)</code><br/><br/>
     * Gets the set of valid crystals that immediately surround the
     * central crystal. Valid crystals must both have valid indices
     * and also be enabled.
     * @param cix - The x-index of the central crystal.
     * @param ciy - The y-index of the central crystal.
     * @return Returns the neighboring crystals as a <code>Set</code>
     * of <code>Point</code> objects, with each element containing the
     * coordinates of a valid neighbor.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y central crystal coordinates do not correspond to a crystal.
     */
    public Set<Point> getNeighbors(int cix, int ciy) { return getNeighbors(new Point(cix, ciy)); }
    
    /**
     * <b>getNeighbors</b><br/><br/>
     * <code>public Set<Point> <b>getNeighbors</b>(Point centralCrystal)</code><br/><br/>
     * Gets the set of valid crystals that immediately surround the
     * central crystal. Valid crystals must both have valid indices
     * and also be enabled.
     * @param centralCrystal - What crystal the neighbors should surround.
     * @return Returns the neighboring crystals as a <code>Set</code>
     * of <code>Point</code> objects, with each element containing the
     * coordinates of a valid neighbor.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y central crystal coordinates do not correspond to a crystal.
     */
    public Set<Point> getNeighbors(Point centralCrystal) throws IndexOutOfBoundsException {
    	// Make sure that the root is a valid crystal.
    	if(!validateIndices(centralCrystal)) {
            throw new IndexOutOfBoundsException(String.format("Invalid central crystal address (%2d, %2d).",
            		centralCrystal.x, centralCrystal.y));
    	}
    	
    	// Make a set to store the neighbors in.
    	HashSet<Point> neighborSet = new HashSet<Point>();
    	
    	// Check all the crystals in a 3-by-3 square around the root.
    	// If they are valid crystals and they are not disable, then
    	// they are neighbors.
    	for(int xMod = -1; xMod <= 1; xMod++) {
    		for(int yMod = -1; yMod <= 1; yMod++) {
    			// Get the possible neighbor.
    			Point possibleNeighbor = new Point(centralCrystal.x + xMod, centralCrystal.y + yMod);
    			
    			// Make sure that the possible neighbor is neither the
    			// root crystal nor invalid.
    			boolean isRoot = centralCrystal.equals(possibleNeighbor);
    			boolean isValid = validateIndices(possibleNeighbor);
    			
    			// If the neighbor passes these tests, add it to the set
    			// if it is active.
    			if(!isRoot && isValid) {
    				if(!crystal[possibleNeighbor.x][possibleNeighbor.y].isDisabled()) {
    					neighborSet.add(possibleNeighbor);
    				}
    			}
    		}
    	}
    	
    	// Return the neighbor set.
    	return neighborSet;
    }
    
    /**
     * <b>getSelectedCrystal</b><br/><br/>
     * <code>public Point <b>getSelectedCrystal</b>()</code><br/><br/>
     * Gives the x/y indices for the currently selected crystal.
     * @return Returns the x/y indices in a <code>Point</code> object.
     * If no crystal is currently selected, returns <code>null</code>.
     */
    public Point getSelectedCrystal() { return selectedCrystal; }
    
    /**
     * <b>isCluster</b><br/><br/>
     * <code>public boolean <b>isCluster</b></code>(int ix, int iy)<br/><br/>
     * Determines if the crystal at the given coordinates is a cluster
     * center or not.
     * @param ix - The x-index of the crystal.
     * @param iy - The y-index of the crystal.
     * @return Returns <code>true</code> if the crystal is a cluster
     * center and <code>false</code> if it is not or if the indices
     * are invalid.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public boolean isCrystalCluster(int ix, int iy) throws IndexOutOfBoundsException {
    	if(validateIndices(ix, iy)) { return crystal[ix][iy].isClusterCenter(); }
        else { throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy)); }
    }
    
    /**
     * <b>isCluster</b><br/><br/>
     * <code>public boolean <b>isCluster</b></code>(Point ixy)<br/><br/>
     * Determines if the crystal at the given coordinates is a cluster
     * center or not.
     * @param ixy - The crystal's x/y-indices.
     * @return Returns <code>true</code> if the crystal is a cluster
     * center and <code>false</code> if it is not or if the indices
     * are invalid.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public boolean isCrystalCluster(Point ixy) throws IndexOutOfBoundsException {
    	return isCrystalCluster(ixy.x, ixy.y);
    }
    
    /**
     * <b>isCrystalDisabled</b><br/><br/>
     * <code>public boolean <b>isCrystalDisabled</b></code>(int ix, int iy)<br/><br/>
     * Determines if the crystal at the given coordinates is a active
     * or not.
     * @param xCoor - The x-index of the crystal.
     * @param yCoor - The y-index of the crystal.
     * @return Returns <code>true</code> if the crystal is active
     * and <code>false</code> if it is not.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public boolean isCrystalDisabled(int ix, int iy) throws IndexOutOfBoundsException {
    	if(validateIndices(ix, iy)) { return crystal[ix][iy].isDisabled(); }
        else { throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy)); }
    }
    
    /**
     * <b>isCrystalDisabled</b><br/><br/>
     * <code>public boolean <b>isCrystalDisabled</b></code>(Point ixy)<br/><br/>
     * Determines if the crystal at the given coordinates is a active
     * or not.
     * @param ixy - The crystal's x/y-indices.
     * @return Returns <code>true</code> if the crystal is active
     * and <code>false</code> if it is not.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public boolean isCrystalDisabled(Point ixy) throws IndexOutOfBoundsException {
    	return isCrystalDisabled(ixy.x, ixy.y);
    }
    
    /**
     * <b>isScalingLinear</b><br/><br/>
     * <code>public boolean <b>isScalingLinear</b></code>()<br/><br/>
     * Indicates whether the crystal colors are mapped linearly.
     * @return Returns <code>true</code> if the mapping is linear
     * and <code>false</code> otherwise.
     */
    public boolean isScalingLinear() { return scale.isLinear(); }
    
    /**
     * <b>isScalingLogarithmic</b><br/><br/>
     * <code>public boolean <b>isScalingLogarithmic</b></code>()<br/><br/>
     * Indicates whether the crystal colors are mapped logarithmically.
     * @return Returns <code>true</code> if the mapping is logarithmic
     * and <code>false</code> otherwise.
     */
    public boolean isScalingLogarithmic() { return scale.isLogarithmic(); }
    
    /**
     * <b>isSelectionEnabled</b><br/><br/>
     * <code>public boolean <b>isSelectionEnabled</b></code>()<br/><br/>
     * Indicates whether highlighting of the currently selected crystal
     * is active or not.
     * @return Returns <code>true</code> if the currently selected
     * crystal will be highlighted and <code>false</code> otherwise.
     */
    public boolean isSelectionEnabled() { return enabledSelection; }
    
    /**
     * <b>setClusterColor</b><br/><br/>
     * <code>public void <b>setClusterColor</b>(Color c)</code><br/><br/>
     * Sets the color of the cluster center marker.
     * @param c - The color to be used for cluster center markers. A
     * value of <code>null</code> will result in seed hit markers being
     * the inverse color of the crystal in which they appear.
     **/
    public void setClusterColor(Color c) { clusterColor = c; }
    
    /**
     * <b>setCrystalCluster</b><br/><br/>
     * <code>public void <b>setCrystalCluster</b>(int ix, int iy, boolean cluster)</code><br/><br/>
     * Sets whether a crystal is also the location of a cluster center.
     * @param ix - The x-index of the crystal.
     * @param iy - The y-index of the crystal.
     * @param cluster - This should be <code>true</code> if there
     * is a cluster center and <code>false</code> if there is not.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     **/
    public void setCrystalCluster(int ix, int iy, boolean cluster) throws IndexOutOfBoundsException {
        if (validateIndices(ix, iy)) { crystal[ix][iy].setClusterCenter(cluster); }
        else { throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy)); }
    }
    
    /**
     * <b>setCrystalCluster</b><br/><br/>
     * <code>public void <b>setCrystalCluster</b>(Point ixy, boolean cluster)</code><br/><br/>
     * Sets whether a crystal is also the location of a seed hit.
     * @param ixy - The crystal's x/y-indices.
     * @param cluster - This should be <code>true</code> if there
     * is a cluster center and <code>false</code> if there is not.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     **/
    public void setCrystalCluster(Point ixy, boolean cluster) throws IndexOutOfBoundsException {
        setCrystalCluster(ixy.x, ixy.y, cluster);
    }
    
    /**
     * <b>setCrystalEnabled</b><br/><br/>
     * <code>public void <b>setCrystalEnabled</b>(int ix, int iy, boolean active)</code><br/><br/>
     * Sets whether the indicated crystal is enabled or not.
     * @param ix - The x-index of the crystal.
     * @param iy - The y-index of the crystal.
     * @param active - This should be <code>true</code> if the crystal is
     * active and <code>false</code> if it is not.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     **/
    public void setCrystalEnabled(int ix, int iy, boolean active) throws IndexOutOfBoundsException {
        if (validateIndices(ix, iy)) { crystal[ix][iy].setDisabled(!active); }
        else { throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy)); }
    }
    
    /**
     * <b>setCrystalEnabled</b><br/><br/>
     * <code>public void <b>setCrystalEnabled</b>(Point ixy, boolean active)</code><br/><br/>
     * Sets whether the indicated crystal is enabled or not.
     * @param ixy - The crystal's x/y-indices.
     * @param active - This should be <code>true</code> if the crystal is
     * active and <code>false</code> if it is not.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     **/
    public void setCrystalEnabled(Point ixy, boolean active) throws IndexOutOfBoundsException {
        setCrystalEnabled(ixy.x, ixy.y, active);
    }
    
    /**
     * <b>setCrystalHighlight</b><br/><br/>
     * <code>public void <b>setCrystalHighlight</b>(int ix, int iy, Color highlight)</code><br/><br/>
     * @param ix - The x-index of the crystal.
     * @param iy - The y-index of the crystal.
     * @param highlight - The color which the indicated crystal should
     * be highlighted. A value of <code>null</code> indicates that no
     * highlight should be used.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public void setCrystalHighlight(int ix, int iy, Color highlight) throws IndexOutOfBoundsException {
        if (validateIndices(ix, iy)) { crystal[ix][iy].setHighlight(highlight); }
        else { throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy)); }
    }
    
    /**
     * <b>setCrystalHighlight</b><br/><br/>
     * <code>public void <b>setCrystalHighlight</b>(Point ixy, Color highlight)</code><br/><br/>
     * @param ixy - The crystal's x/y-indices.
     * @param highlight - The color which the indicated crystal should
     * be highlighted. A value of <code>null</code> indicates that no
     * highlight should be used.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public void setCrystalHighlight(Point ixy, Color highlight) throws IndexOutOfBoundsException {
        setCrystalHighlight(ixy.x, ixy.y, highlight);
    }
    
    /**
     * <b>setCrystalDefaultColor</b><br/><br/>
     * <code>public void <b>setCrystalDefaultColor</b>(Color c)</code><br/><br/>
     * Sets the color that crystals with zero energy will display.
     * @param c - The color to use for zero energy crystals. A value
     * of <code>null</code> will use the appropriate energy color
     * map value.
     */
    public void setDefaultCrystalColor(Color c) {
    	// Only update the crystals if the default color has changed.
    	if(c != defaultColor) {
    		// Store the new default color.
    		defaultColor = c;
    		
    		// Inform the crystals of the change.
    		for(int ix = 0; ix < xBoxes; ix++) {
    			for(int iy = 0; iy < yBoxes; iy++) { crystal[ix][iy].setUseDefaultColor(c != null, false); }
    		}
    		
    		// Reset the colors and repaint.
    		resetCrystalColors();
    		repaint();
    	}
    }
    
    /**
     * <b>setScaleEnabled</b><br/><br/>
     * <code>public void <b>setScaleEnabled</b>(boolean enabled)</code><br/><br/>
     * Sets whether the scale should be visible or not.
     * @param enabled - <code>true</code> indicates that the scale should
     * be visible and <code>false</code> that it should be hidden.
     **/
    public void setScaleEnabled(boolean enabled) {
        if (scalePanel.isVisible() != enabled) {
            scalePanel.setVisible(enabled);
        }
    }
    
    /**
     * <b>setScaleMaximum</b><br/><br/>
     * <code>public void <b>setScaleMaximum</b>(double maximum)</code><br/><br/>
     * Sets the maximum value of the color mapping scale. Energies above this
     * value will all be the same maximum color.
     * @param maximum - The maximum energy to be mapped.
     **/
    public void setScaleMaximum(double maximum) {
        scale.setMaximum(maximum);
    }
    
    /**
     * <b>setScaleMinimum</b><br/><br>
     * <code>public void <b>setScaleMinimum</b>(double minimum)</code><br/><br/>
     * Sets the minimum value of the color mapping scale. Energies below this
     * value will all be the same minimum color.
     * @param minimum - The minimum energy to be mapped.
     **/
    public void setScaleMinimum(double minimum) {
        scale.setMinimum(minimum);
    }
    
    /**
     * <b>setScalingLinear</b><br/><br/>
     * <code>public void <b>setScalingLinear</b>()<br/><br/>
     * Sets the color mapping scale behavior to linear mapping.
     **/
    public void setScalingLinear() {
        scale.setScalingLinear();
        resetCrystalColors();
        repaint();
    }
    
    /**
     * <b>setScalingLogarithmic</b><br/><br/>
     * <code>public void <b>setScalingLogarithmic</b>()</code><br/><br/>
     * Sets the color mapping scale behavior to logarithmic mapping.
     **/
    public void setScalingLogarithmic() {
        scale.setScalingLogarithmic();
        resetCrystalColors();
        repaint();
    }
    
    /**
     * <b>setSelectedCrystal</b><br/><br/>
     * <code>public void <b>setSelectedCrystal</b></code>(int ix, int iy)<br/><br/>
     * Sets which crystal is currently selected.
     * @param ix - The x-index of the crystal.
     * @param iy - The y-index of the crystal.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     */
    public void setSelectedCrystal(int ix, int iy) {
        if (validateIndices(ix, iy)) {
        	if(selectedCrystal != null) { crystal[selectedCrystal.x][selectedCrystal.y].setSelected(false); }
            crystal[ix][iy].setSelected(true);
            selectedCrystal = new Point(ix, iy);
        }
        else {
            throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", ix, iy));
        }
    }
    
    /**
     * <b>setSelectedCrystal</b><br/><br/>
     * <code>public void <b>setSelectedCrystal</b></code>(Point ixy)<br/><br/>
     * Sets which crystal is currently selected.
     * @param ixy - The crystal's x/y-indices.
     * @throws IndexOutOfBoundsException Occurs when either of the given
     * x/y crystal coordinates do not correspond to a crystal.
     * @throws NullPointerException Occurs when the argument <code>Point
     * </code> is <code>null</code>.
     */
    public void setSelectedCrystal(Point ixy) throws NullPointerException {
    	if(crystal != null) { setSelectedCrystal(ixy.x, ixy.y); }
    	else { throw new NullPointerException("Crystal ID must be defined."); }
    }
    
    /**
     * <b>setSelectedCrystalHighlight</b><br/><br/>
     * <code>public void <b>setSelectedCrystalHighlight</b>(Color c)</code><br/><br/>
     * Sets the color in which selected crystals should be highlighted.
     * @param c - The new selection highlight color.
     * @throws IllegalArgumentException Occurs if the selection color
     * is set to <code>null</code>.
     */
    public void setSelectedCrystalHighlight(Color c) throws IllegalArgumentException {
    	// We do not allow null values for the selected crystal color.
    	if(c == null) { throw new IllegalArgumentException("Crystal selection color can not be null."); }
    	else { selectedColor = c; }
    }
    
    /**
     * <b>setSelectionHighlighting</b><br/><br/>
     * <code>public void <b>setSelectionHighlighting</b>(boolean state)</code><br/><br/>
     * Sets whether or not the currently selected crystal should be
     * highlighted or not.
     * @param state - <code>true</code> indicates that the selected
     * crystals should be highlighted and <code>false</code> that
     * it should not.
     */
    public void setSelectionHighlighting(boolean state) {
    	if(enabledSelection != state) {
    		enabledSelection = state;
    		if(selectedCrystal != null) { crystal[selectedCrystal.x][selectedCrystal.y].repaint(); }
    	}
    }
    
    public void setSize(Dimension d) { setSize(d.width, d.height); }
    
    public void setSize(int width, int height) {
    	// Run the superclass method.
        super.setSize(width, height);
        
        // Resize the scale panel.
        scalePanel.setLocation(width - scaleWidth, 0);
        scalePanel.setSize(scaleWidth, height);
        
        // Determine the width and heights of the calorimeter crystals.
        if (scalePanel.isVisible()) { width = getWidth() - scaleWidth; }
        else { width = getWidth(); }
        
        int boxWidth = width / xBoxes;
        int widthRem = width % xBoxes;
        int boxHeight = height / yBoxes;
        int heightRem = height % yBoxes;
        
        // Set positioning and sizing variables.
        int[] crystalRem = { widthRem, heightRem };
        int curX = 0;
        int curY = 0;
        
        // Loop over the rows of crystals.
        xPosition[0] = 0;
        for(int x = 0; x < xBoxes; x++) {
        	// Get the appropriate width for a crystal.
        	int crystalWidth = boxWidth;
        	if(crystalRem[0] != 0) {
        		crystalWidth++;
        		crystalRem[0]--;
        	}
        	
        	// Note the x-coordinate for this column.
        	xPosition[x + 1] = xPosition[x] + crystalWidth;
        	
        	// Loop over the columns of crystals.
        	for(int y = 0; y < yBoxes; y++) {
        		// Get the appropriate height for a crystal.
        		int crystalHeight = boxHeight;
        		if(crystalRem[1] != 0) {
        			crystalHeight++;
        			crystalRem[1]--;
        		}
        		
        		// Note the y-coordinate for this row.
        		yPosition[y + 1] = yPosition[y] + crystalHeight;
        		
        		// Set the crystal size and location.
        		crystal[x][y].setSize(crystalWidth, crystalHeight);
        		crystal[x][y].setLocation(curX, curY);
        		
        		// Increment the current y-coordinate.
        		curY += crystalHeight;
        	}
        	
        	// Increment the current x-coordinate and reset the current y-coordinate.
        	curX += crystalWidth;
        	curY = 0;
        	
        	// Reset the height remainder for the next column.
        	crystalRem[1] = heightRem;
        }
            
        // Calculate the cluster position variables.
        double ltw = 0.25 * boxWidth;
        double lth = 0.25 * boxHeight;
        
        if(ltw > lth) {
        	clusterSpace[0] = (int)Math.round((boxWidth - lth - lth) / 2.0);
        	clusterSpace[1] = (int)Math.round(lth);
        	clusterSpace[2] = (int)Math.round(lth + lth);
        }
        else {
        	clusterSpace[0] = (int)Math.round(ltw);
        	clusterSpace[1] = (int)Math.round((boxHeight - ltw - ltw) / 2.0);
        	clusterSpace[2] = (int)Math.round(ltw + ltw);
        }
	}
    
    /**
     * <b>setSuppressRedraw</b><br/><br/>
     * <code>public void <b>setSuppressRedraw</b>(boolean state)</code><br/><br/>
     * Sets whether the panel crystals should repaint automatically
     * whenever their state changes.
     * @param state - <code>true</code> indicates that the crystal
     * panels will repaint automatically, while <code>false</code>
     * indicates that they will not repaint.
     */
    public void setSuppressRedraw(boolean state) { suppress = state; }
    
    // ================================================================================
    // === Private Methods ============================================================
    // ================================================================================
    
    /**
     * <b>resetCrystalColors</b><br/><br/>
     * <code>private void <b>resetCrystalColors</b>()</code><br/><br/>
     * Forces all crystals to revalidate their colors.
     */
    private void resetCrystalColors() {
    	// Force all the crystals to update their colors.
    	for(int ix = 0; ix < xBoxes; ix++) {
    		for(int iy = 0; iy < yBoxes; iy++) { crystal[ix][iy].resetColor(); }
    	}
    }
    
    /**
     * <b>validateIndices</b><br/><br/>
     * <code>private boolean <b>validateIndices</b>(int ix, int iy)</code><br/><br/>
     * Indicates whether the given indices corresponds to a valid
     * crystal or not.
     * @param ix - The crystal's x index.
     * @param iy - The crystal's y index.
     * @return Returns <code>true</code> if the indices are valid
     * and <code>false</code> if they are not.
     */
    private boolean validateIndices(int ix, int iy) {
    	boolean lowX = (ix > -1);
    	boolean highX = (ix < xBoxes);
    	boolean lowY = (iy > -1);
    	boolean highY = (iy < yBoxes);
    	
    	return (lowX && highX && lowY && highY);
    }
    
    /**
     * <b>validateIndices</b><br/><br/>
     * <code>private boolean <b>validateIndices</b>(Point p)</code><br/><br/>
     * Indicates whether the given indices corresponds to a valid
     * crystal or not.
     * @param p - A <code>Point</code> object containing the crystal's
     * x and y indices.
     * @return Returns <code>true</code> if the indices are valid
     * and <code>false</code> if they are not.
     */
    private boolean validateIndices(Point p) { return validateIndices(p.x, p.y); }
    
    // ================================================================================
    // === Private Internal Classes ===================================================
    // ================================================================================
    
    /**
     * Class <code>Crystal</code> holds all pertinent information needed
     * to display a calorimeter crystal in the panel. It also handles
     * drawing itself.
     * @author Kyle McCarty
     */
    private class Crystal extends JPanel {
		private static final long serialVersionUID = -5666423016127997831L;
		// The energy stored in the crystal.
		private double energy = 0.0;
		// Whether the crystal can store energy.
		private boolean disabled = false;
		// Whether the crystal is a cluster center.
		private boolean cluster = false;
		// Whether zero-energy crystals should use color mapping.
		private boolean useDefaultColor = false;
		// Whether the crystal is selected.
		private boolean selected = false;
		// What color the crystal should be highlighted in.
		private Color highlight = null;
		// Store associated crystals.
		private ArrayList<Association> componentList = new ArrayList<Association>();
		
		/**
		 * <b>Crystal</b><br/><br/>
		 * <code>public <b>Crystal</b>()</code><br/><br/>
		 * Initializes a new calorimeter crystal panel.
		 */
    	public Crystal() {
    		setOpaque(true);
    		resetColor();
    	}
		
		/**
		 * <b>addAssociation</b><br/><br/>
		 * <code>public void <b>addAssociation</b>(Association a)</code><br/><br/>
		 * Adds a new associated crystal to this crystal.
		 * @param a - The <code>Association</code> object representing
		 * the associated crystal and its highlighting color.
		 */
		public void addAssociation(Association a) {
			// Add the association.
			componentList.add(a);
			
			// If this crystal is selected, then activate the new crystal.
			if(selected) { setCrystalHighlight(a.getChildCrystal(), a.getHighlight()); }
		}
    	
    	/**
    	 * <b>addEnergy</b><br/><br/>
		 * <code>public void <b>addEnergy</b>(double energy)</code><br/><br/>
		 * Increments the crystal's energy by the given amount.
    	 * @param energy - The energy by which the crystal's stored
    	 * energy should be increased.
    	 */
    	public void addEnergy(double energy) { setEnergy(this.energy + energy); }
    	
		/**
		 * <b>clearAssociations</b><br/><br/>
		 * <code>public void <b>clearAssociations</b>()</code><br/><br/>
		 * Clears all the associated crystal from this crystal.
		 */
		public void clearAssociations() {
			// Remove the highlighting from any associated crystals,
			// if this crystal is selected.
			if(selected) {
	    		for(Association a : componentList) { setCrystalHighlight(a.getChildCrystal(), null); }
			}
			
			// Clear the list.
			componentList.clear();
		}
    	
    	/**
    	 * <b>getEnergy</b><br/><br/>
		 * <code>public double <b>getEnergy</b>()</code><br/><br/>
		 * Indicates how much energy is stored in the crystal.
    	 * @return Returns the crystal's energy as a <code>double</code>.
    	 */
    	public double getEnergy() { return energy; }
    	
    	/**
    	 * <b>isClusterCenter</b><br/><br/>
		 * <code>public boolean <b>isClusterCenter</b>()</code><br/><br/>
		 * Indicates whether this crystal is also a cluster center.
    	 * @return Returns <code>true</code> if the crystal is a cluster
    	 * center and <code>false</code> if it is not.
    	 */
    	public boolean isClusterCenter() { return cluster; }
    	
    	/**
    	 * <b>isDisabled</b><br/><br/>
		 * <code>public boolean <b>isDisabled</b>()</code><br/><br/>
		 * Indicates whether the crystal is disabled.
    	 * @return Returns <code>true</code> if the crystal is disabled
    	 * and <code>false</code> if it not.
    	 */
    	public boolean isDisabled() { return disabled; }
    	
    	public void paint(Graphics g) {
    		// If the crystal's redraw is suppressed, do nothing.
    		if(suppress) { return; }
    		
    		// Run the superclass paint method to draw the background.
    		super.paint(g);
    		
    		// Draw the crystal border.
    		g.setColor(Color.BLACK);
    		g.drawRect(0, 0, getWidth(), getHeight());
    		g.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
    		
    		// If the crystal is not disabled, we may also add a
    		// highlight to the crystal.
    		if(!disabled) {
    			// Highlighting from a crystal being selected overrides
    			// any other form of highlighting.
    			if(selected && enabledSelection) {
        			g.setColor(selectedColor);
        			g.drawRect(2, 2, getWidth() - 4, getHeight() - 4);
    			}
    			// Otherwise, if the crystal has a highlighting color
    			// set, apply that color for the highlight.
    			else if(getHighlight() != null) {
        			g.setColor(getHighlight());
        			g.drawRect(2, 2, getWidth() - 4, getHeight() - 4);
        		}
    		}
    		
    		// If the crystal contains a cluster, draw the cluster dot.
    		if(cluster) {
    			g.setColor(clusterColor);
    			g.fillOval(clusterSpace[0], clusterSpace[1], clusterSpace[2], clusterSpace[2]);
    		}
    	}
    	
    	/**
    	 * <b>resetColor</b><br/><br/>
		 * <code>public void <b>resetColor</b>()</code><br/><br/>
		 * Sets the crystals color to the appropriate value based on
		 * its settings.
    	 */
    	public void resetColor() {
    		// If the crystal is disabled, it is always disabledColor.
    		if(disabled) { setBackground(disabledColor); }
    		
    		// If the crystal has zero energy and we are using a default
    		// color, then we use that.
    		else if(energy == 0.0 && useDefaultColor) { setBackground(defaultColor); }
    		
    		// Otherwise, we use the color chosen by the scale.
    		else { setBackground(scale.getColor(energy)); }
    	}
		
		/**
		 * <b>setAssociatedActive</b><br/><br/>
		 * <code>public void <b>setAssociatedActive</b>(boolean state)</code><br/><br/>
		 * Sets whether the highlighting on this crystal's associated
		 * crystals should be active or not.
		 * @param state - <code>true</code> indicates that the crystal
		 * highlighting should be active and <code>false</code> that
		 * it should be inactive.
		 */
		public void setAssociatedActive(boolean state) {
    		// If it has any associated crystals, either activate
    		// or deactivate their highlighting, as per the current
    		// crystal selection state.
    		for(Association a : componentList) {
    			if(state) { setCrystalHighlight(a.getChildCrystal(), a.getHighlight()); }
    			else { setCrystalHighlight(a.getChildCrystal(), null); }
    		}
		}
    	
    	/**
    	 * <b>setClusterCenter</b><br/><br/>
		 * <code>public void <b>setClusterCenter</b>(boolean state)</code><br/><br/>
		 * Sets whether or not this crystal is a cluster center.
    	 * @param state - <code>true</code> indicates that this is a
    	 * cluster center and <code>false</code> that it is not.
    	 */
    	public void setClusterCenter(boolean state) {
    		if(cluster != state) {
    			cluster = state;
    			repaint();
    		}
    	}
    	
    	/**
    	 * <b>setDisabled</b><br/><br/>
		 * <code>public void <b>setDisabled</b>(boolean state)</code><br/><br/>
		 * Sets whether or not this crystal can store energy.
    	 * @param state - <code>true</code> means the crystal can not
    	 * store energy and <code>false</code> that it can.
    	 */
    	public void setDisabled(boolean state) {
    		// If the state has changed, reset the color.
    		if(disabled != state) {
    			disabled = state;
    			resetColor();
    		}
    	}
    	
    	/**
    	 * <b>setEnergy</b><br/><br/>
		 * <code>public void <b>setEnergy</b>(double energy)</code><br/><br/>
		 * Sets the crystal's stored energy.
    	 * @param energy - The energy stored in the crystal.
    	 */
    	public void setEnergy(double energy) {
    		if(this.energy != energy) {
    			this.energy = energy;
    			if(energy > extremum[1]) { extremum[1] = energy; }
    			if(energy < extremum[0]) { extremum[0] = energy; }
    			resetColor();
    		}
    	}
    	
    	/**
    	 * <b>setHighlight</b><br/><br/>
		 * <code>public void <b>setHighlight</b>(Color highlight)</code><br/><br/>
		 * Sets what color the crystal should be highlighted with.
		 * Note that selected crystals will always be highlighted with
		 * the selected crystal color, though selecting a crystal does
		 * not overwrite its highlight color.
    	 * @param highlight - The color the crystal should be
		 * highlighted in.
    	 */
    	public void setHighlight(Color highlight) {
    		if(this.highlight != highlight) {
    			this.highlight = highlight;
    			repaint();
    		}
    	}
    	
    	/**
    	 * <b>setSelected</b><br/><br/>
		 * <code>public void <b>setSelected</b>(boolean state)</code><br/><br/>
		 * Sets whether or not this crystal should be highlighted as
		 * a selected crystal.
    	 * @param state - <code>true</code> means the crystal will be
    	 * highlighted using the selected color and <code>false</code>
    	 * will cause it to use the standard highlighting rules.
    	 */
    	public void setSelected(boolean state) {
    		if(selected != state) {
    			// Store the crystal's state and redraw it.
        		selected = state;
        		this.repaint();
        		
        		// Activate or deactivate the associated crystals.
        		setAssociatedActive(state);
    		}
    	}
    	
    	/**
    	 * <b>setState</b><br/><br/>
		 * <code>public void <b>setState</b>(double energy, boolean cluster, Color highlight)</code><br/><br/>
		 * Sets the crystal's energy, cluster center status, and
		 * highlighting color. The crystal will redraw itself if needed.
    	 * @param energy - The crystal's energy.
    	 * @param cluster - <code>true</code> indicates that the crystal
    	 * is a cluster center, and <code>false</code> that is not.
    	 * @param highlight - What color in which the crystal should be
    	 * highlighted. <code>null</code> indicates that the crystal
    	 * should not be highlighted.
    	 */
    	public void setState(double energy, boolean cluster, Color highlight) {
    		// Track whether the crystal has changed states.
    		boolean changed = false;
    		
    		// Change the energy if needed.
    		if(this.energy != energy) {
    			this.energy = energy;
    			resetColor();
    		}
    		
    		// Change the cluster state if needed.
    		if(this.cluster != cluster) {
    			this.cluster = cluster;
    			changed = true;
    		}
    		
    		// Change the highlighting if needed.
    		if(this.highlight != highlight) {
    			this.highlight = highlight;
    			changed = true;
    		}
    		
    		// If the state has changed, redraw the crystal.
    		if(changed) { repaint(); }
    	}
    	
    	/**
    	 * <b>setUseDefaultColor</b><br/><br/>
		 * <code>public void <b>setUseDefaultColor</b>(boolean state, boolean autoRepaint)</code><br/><br/>
		 * Sets whether the crystal should use a default color when it
		 * has no energy.
    	 * @param state - <code>true</code> means the crystal will render
    	 * using a default color when it has no energy and <code>false
    	 * </code> that it will use the scale mapping color at all times.
    	 * @param autoRepaint
    	 */
    	public void setUseDefaultColor(boolean state, boolean autoRepaint) {
    		// If the state has changed, reset the color.
    		if(state != useDefaultColor) {
    			useDefaultColor = state;
    			if(autoRepaint) { resetColor(); }
    		}
    	}
    	
    	/**
    	 * <b>getHighlight</b><br/><br/>
		 * <code>public Color <b>getHighlight</b>()</code><br/><br/>
		 * Gets the highlight color assigned to this crystal.
    	 * @return Returns the highlight color as a <code>Color</code>
    	 * object if it exists. Otherwise, returns <code>null</code>.
    	 */
    	public Color getHighlight() { return highlight; }
    }
    
    /**
     * The local class <b>ScalePanel</b> renders the scale for the calorimeter
     * color map.
     **/
    private class ScalePanel extends JPanel {
		private static final long serialVersionUID = -2644562244208528609L;
        
        protected void paintComponent(Graphics g) {
            // Set the text region width.
            int textWidth = 45;
            boolean useText;
            
            // Store height and width.
            int height = getHeight();
            int width;
            if (getWidth() > textWidth) {
                width = getWidth() - textWidth;
                useText = true;
            }
            else {
                width = getWidth();
                useText = false;
            }
            
            // Define the step size for the scale. This will differ depending
            // on whether we employ a linear or logarithmic scale.
            double step;
            double curValue;
            boolean linear = scale.isLinear();
            if (linear) {
                step = (scale.getMaximum() - scale.getMinimum()) / height;
                curValue = scale.getMinimum();
            }
            else {
                double max = Math.log10(scale.getMaximum());
                double min = Math.log10(scale.getMinimum());
                step = (max - min) / height;
                curValue = min;
            }
            
            // Color the text area.
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, width, height);
            g.drawRect(1, 1, width - 1, height - 1);
            g.fillRect(width, 0, textWidth, height);
            
            // Render the scale.
            int sy = height;
            int[] sx = { 0, width };
            for (int i = 0; i <= height; i++) {
                // Get the appropriate value for the current pixel.
                double scaledValue;
                if (linear) { scaledValue = curValue; }
                else { scaledValue = Math.pow(10, curValue); }
                g.setColor(scale.getColor(scaledValue));
                
                // Draw a line.
                g.drawLine(sx[0], sy, sx[1], sy);
                
                // Update the spacing variables.
                curValue += step;
                sy--;
            }
            
            // Generate the scale text.
            if (useText) {
                // Determine the spacing of the text.
                FontMetrics fm = g.getFontMetrics(g.getFont());
                int fontHeight = fm.getHeight();
                
                // Populate the first and last values.
                NumberFormat nf = new DecimalFormat("0.#E0");
                g.setColor(Color.WHITE);
                g.drawString(nf.format(scale.getMaximum()), width + 5, fontHeight);
                g.drawString(nf.format(scale.getMinimum()), width + 5, height - 3);
                
                // Calculate text placement variables.
                double heightAvailable = height - 2.0 * fontHeight;
                double heightDefault = heightAvailable / (1.5 * fontHeight);
                int num = (int) Math.floor(heightAvailable / heightDefault);
                double heightRemainder = heightAvailable - (num * heightDefault);
                double heightExtra = heightRemainder / num;
                double lSpacing = heightDefault + heightExtra;
                double lHalfSpacing = lSpacing / 2.0;
                int lHeight = fontHeight + 3;
                int[] lX = { width - 4, width, width + 5 };
                int lShift = (int) (fontHeight * 0.25 + lHalfSpacing);
                double lTemp = 0.0;
                
                // Calculate value conversion variables.
                double lMin = scale.getMinimum();
                double lScale;
                if (linear) {
                    lMin = scale.getMinimum();
                    lScale = scale.getMaximum() - scale.getMinimum();
                }
                else {
                    double min = Math.log10(scale.getMinimum());
                    double max = Math.log10(scale.getMaximum());
                    lMin = min;
                    lScale = max - min;
                }
                
                // Write the labels.
                for (int i = 0; i < num; i++) {
                    g.setColor(Color.BLACK);
                    int h = (int) (lHeight + lHalfSpacing);
                    g.drawLine(lX[0], h, lX[1], h);
                    g.setColor(Color.WHITE);
                    double lVal = lMin + (1.0 - ((double) h / height)) * lScale;
                    if (!linear) { lVal = Math.pow(10, lVal); }
                    g.drawString(nf.format(lVal), lX[2], lHeight + lShift);
                    lTemp += lSpacing;
                    lHeight = (int) (fontHeight + lTemp);
                }
            }
        }
    }
}
