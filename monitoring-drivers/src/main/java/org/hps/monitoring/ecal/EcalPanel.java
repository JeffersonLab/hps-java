package org.hps.monitoring.ecal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JPanel;

/**
 * The class <code>EcalPanel</code> handles the rendering of the calorimeter
 * crystals as well as mapping colors to their values, rendering a color scale,
 * and marking cluster crystals.
 * 
 * @author Kyle McCarty
 **/
public class EcalPanel extends JPanel {
    // Java-suggested variable.
    private static final long serialVersionUID = 6292751227464151897L;
    // The color used for rendering seed hits.
    private Color clusterColor = Color.GREEN;
    // The default color of the calorimeter crystals.
    private Color defaultColor = null;
    // The color-mapping scale used by to color calorimeter crystals.
    private MultiGradientScale scale = MultiGradientScale.makeRainbowScale(0.0, 1.0);
    // The number of boxes in the x-direction.
    private int xBoxes = 1;
    // The number of boxes in the y-direction.
    private int yBoxes = 1;
    // The width of the scale.
    private int scaleWidth = 75;
    // Stores which calorimeter crystals are disabled.
    private boolean[][] disabled;
    // Stores the energies in each calorimeter crystal.
    private double[][] hit;
    // Stores whether a crystal is the location of a seed hit.
    private boolean[][] cluster;
    // Stores what color to highlight the crystal with.
    private Color[][] highlight;
    // The panel on which the scale is rendered.
    private ScalePanel scalePanel = new ScalePanel();
    // Store the size of the panel as of the last refresh.
	private int[] lastSize = new int[2];
    
    // Efficiency variables for crystal placement.
    private int[] widths;
    private int[] heights;
    private int[] xPosition;
    private int[] yPosition;
    private int[] clusterSpace = new int[3];
    
    /**
     * <b>EcalPanel</b><br/><br/>
     * Initializes the calorimeter panel.
     * @param numXBoxes - The number of crystals in the x-direction.
     * @param numYBoxes - The number of crystals in the y-direction.
     **/
    public EcalPanel(int numXBoxes, int numYBoxes) {
        // Initialize the base component.
        super();
        
        // Set the number of calorimeter crystals.
        xBoxes = numXBoxes;
        yBoxes = numYBoxes;
        
        // Initialize data the arrays.
        disabled = new boolean[xBoxes][yBoxes];
        hit = new double[xBoxes][yBoxes];
        cluster = new boolean[xBoxes][yBoxes];
        highlight = new Color[xBoxes][yBoxes];
        
        for(int x = 0; x < xBoxes; x++) {
        	for(int y = 0; y < yBoxes; y++) {
        		highlight[x][y] = null;
        	}
        }
        
        // Initialize the size arrays,
        widths = new int[xBoxes];
        heights = new int[yBoxes];
        xPosition = new int [xBoxes + 1];
        yPosition = new int[yBoxes + 1];
        
        // Add the scale panel.
        setLayout(null);
        add(scalePanel);
    }
    
    /**
     * <b>setCrystalEnabled</b><br/><br/>
     * <code>public void <b>setCrystalEnabled</b>(int xIndex, int yIndex, boolean active)</code><br/><br/>
     * Sets whether the indicated crystal is enabled or not. Invalid indices
     * will be ignored.
     * @param xIndex - The x-coordinate of the crystal.
     * @param yIndex - The y-coordinate of the crystal.
     * @param active - This should be <code>true</code> if the crystal is
     * active and <code>false</code> if it is not.
     * @throws IndexOutOfBoundsException Occurs when the given xy crystal
     * coordinate does not point to a crystal.
     **/
    public void setCrystalEnabled(int xIndex, int yIndex, boolean active) throws IndexOutOfBoundsException {
        if (xIndex >= 0 && xIndex < xBoxes && yIndex >= 0 && yIndex < yBoxes) {
            disabled[xIndex][yIndex] = !active;
        }
        else {
            throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", xIndex, yIndex));
        }
    }
    
    /**
     * <b>addCrystalEnergy</b><br/><br/>
     * <code>public void <b>addCrystalEnergy</b>(int xIndex, int yIndex, double energy)</code><br/><br/>
     * Adds the indicated quantity of energy to the crystal at the given
     * coordinates.
     * @param xIndex - The x-coordinate of the crystal.
     * @param yIndex - The y-coordinate of the crystal.
     * @param energy - The energy to add.
     * @throws IndexOutOfBoundsException Occurs when the given xy crystal
     * coordinate does not point to a crystal.
     **/
    public void addCrystalEnergy(int xIndex, int yIndex, double energy) throws IndexOutOfBoundsException {
        if (xIndex >= 0 && xIndex < xBoxes && yIndex >= 0 && yIndex < yBoxes) {
            this.hit[xIndex][yIndex] += energy;
        }
        else {
            throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", xIndex, yIndex));
        }
    }
    
    /**
     * <b>setCrystalCluster</b><br/><br/>
     * <code>public void <b>setCrystalCluster</b>(int xIndex, int yIndex, boolean cluster)</code><br/><br/>
     * Sets whether a crystal is also the location of a seed hit.
     * @param xIndex - The x-coordinate of the crystal.
     * @param yIndex - The y-coordinate of the crystal.
     * @param cluster - This should be <code>true</code> if there
     * is a seed hit and <code>false</code> if there is not.
     * @throws IndexOutOfBoundsException Occurs when the given xy
     * crystal coordinate does not point to a crystal.
     **/
    public void setCrystalCluster(int xIndex, int yIndex, boolean cluster) throws IndexOutOfBoundsException {
        if (xIndex >= 0 && xIndex < xBoxes && yIndex >= 0 && yIndex < yBoxes) {
            this.cluster[xIndex][yIndex] = cluster;
        }
        else {
            throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", xIndex, yIndex));
        }
    }
    
    /**
     * <b>setCrystalHighlight</b><br/><br/>
     * <code>public void <b>setCrystalHighlight</b>(int xIndex, int yIndex, Color highlight)</code><br/><br/>
     * @param xIndex - The x-coordinate of the crystal.
     * @param yIndex - The y-coordinate of the crystal.
     * @param highlight - The color which the indicated crystal should
     * be highlighted. A value of <code>null</code> indicates that no
     * highlight should be used.
     * @throws IndexOutOfBoundsException Occurs when the given xy
     * crystal coordinate does not point to a crystal.
     */
    public void setCrystalHighlight(int xIndex, int yIndex, Color highlight) throws IndexOutOfBoundsException {
        if (xIndex >= 0 && xIndex < xBoxes && yIndex >= 0 && yIndex < yBoxes) {
            this.highlight[xIndex][yIndex] = highlight;
        }
        else {
            throw new IndexOutOfBoundsException(String.format("Invalid crystal address (%2d, %2d).", xIndex, yIndex));
        }
    }
    
    /**
     * <b>clearCrystals</b><br/><br/>
     * <code>public void <b>clearCrystals</b>()</code><br/><br/>
     * Sets all crystal energies to zero and removes all clusters. This
     * <b>does not</b> enable disabled crystals.
     **/
    public void clearCrystals() {
        for (int x = 0; x < xBoxes; x++) {
            for (int y = 0; y < yBoxes; y++) {
                hit[x][y] = 0.0;
                cluster[x][y] = false;
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
            for (int y = 0; y < yBoxes; y++) { highlight[x][y] = null; }
        }
    }
    
    /**
     * <b>setClusterColor</b><br/><br/>
     * <code>public void <b>setClusterColor</b>(Color c)</code><br/><br/>
     * Sets the color of the seed hit marker.
     * @param c - The color to be used for seed hit markers. A value of <code>null
     * </code> will result in seed hit markers being the inverse color of the crystal
     * in which they appear.
     **/
    public void setClusterColor(Color c) { clusterColor = c; }
    
    /**
     * <b>setMinimum</b><br/><br>
     * <code>public void <b>setMinimum</b>(double minimum)</code><br/><br/>
     * Sets the minimum value of the color mapping scale. Energies below this
     * value will all be the same minimum color.
     * @param minimum - The minimum energy to be mapped.
     **/
    public void setMinimum(double minimum) {
        scale.setMinimum(minimum);
    }
    
    /**
     * <b>setMaximum</b><br/><br/>
     * <code>public void <b>setMaximum</b>(double maximum)</code><br/><br/>
     * Sets the maximum value of the color mapping scale. Energies above this
     * value will all be the same maximum color.
     * @param maximum - The maximum energy to be mapped.
     **/
    public void setMaximum(double maximum) {
        scale.setMaximum(maximum);
    }
    
    /**
     * <b>setScalingLinear</b><br/><br/>
     * <code>public void <b>setScalingLinear</b>()<br/><br/>
     * Sets the color mapping scale behavior to linear mapping.
     **/
    public void setScalingLinear() {
        scale.setScalingLinear();
    }
    
    /**
     * <b>setScalingLogarithmic</b><br/><br/>
     * <code>public void <b>setScalingLogarithmic</b>()</code><br/><br/>
     * Sets the color mapping scale behavior to logarithmic mapping.
     **/
    public void setScalingLogarithmic() {
        scale.setScalingLogarithmic();
    }
    
    /**
     * <b>isScalingLinear</b><br/><br/>
     * <code>public void <b>isScalingLinear</b></code>()<br/><br/>
     * Indicates whether the crystal colors are mapped linearly.
     * @return Returns <code>true</code> if the mapping is linear
     * and <code>false</code> otherwise.
     */
    public boolean isScalingLinear() { return scale.isLinearScale(); }
    
    /**
     * <b>isScalingLogarithmic</b><br/><br/>
     * <code>public void <b>isScalingLogarithmic</b></code>()<br/><br/>
     * Indicates whether the crystal colors are mapped logarithmically.
     * @return Returns <code>true</code> if the mapping is logarithmic
     * and <code>false</code> otherwise.
     */
    public boolean isScalingLogarithmic() { return scale.isLogairthmicScale(); }
    
    /**
     * <b>isCluster</b><br/><br/>
     * <code>public boolean <b>isCluster</b></code>()<br/><br/>
     * Determines if the crystal at the given coordinates is a cluster
     * center or not.
     * @param xCoor - The x-coordinate of the crystal.
     * @param yCoor - The y-coordinate of the crystal.
     * @return Returns <code>true</code> if the crystal is a cluster
     * center and <code>false</code> if it is not or if the indices
     * are invalid.
     */
    public boolean isCluster(int xCoor, int yCoor) {
    	// If the coordinates are invalid, return false.
    	if(!validateIndices(xCoor, yCoor)) { return false; }
    	
    	// Otherwise, check if it is a cluster.
    	else { return cluster[xCoor][yCoor]; }
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
     * <b>setCrystalDefaultColor</b><br/><br/>
     * <code>public void <b>setCrystalDefaultColor</b>(Color c)</code><br/><br/>
     * Sets the color that crystals with zero energy will display.
     * @param c - The color to use for zero energy crystals. A value
     * of <code>null</code> will use the appropriate energy color
     * map value.
     */
    public void setCrystalDefaultColor(Color c) { defaultColor = c; }
    
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
     * <b>getCrystalEnergy</b><br/><br/>
     * <code>public double <b>getCrystalEnergy</b>(int ix, int iy)</code><br/><br/>
     * Provides the energy stored in the indicated crystal.
     * @param ix - The crystal's x-index.
     * @param iy - The crystal's y-index.
     * @return Returns the energy as a <code>double</code>.
     * @throws IndexOutOfBoundsException - Occurs when either of the
     * given indices are invalid.
     */
    public double getCrystalEnergy(int ix, int iy) throws IndexOutOfBoundsException {
    	if(!validateIndices(ix, iy)) {
    		throw new IndexOutOfBoundsException("Invalid crystal index.");
    	}
    	else { return hit[ix][iy]; }
    }
    
    public Color getCrystalHighlight(int ix, int iy) throws IndexOutOfBoundsException {
    	if(!validateIndices(ix, iy)) {
    		throw new IndexOutOfBoundsException("Invalid crystal index.");
    	}
    	else { return highlight[ix][iy]; }
    }
    
    /**
     * <b>redraw</b><br/><br/>
     * <code>public void <b>redraw</b>()</code> Re-renders the calorimeter
     * panel.
     **/
    public void redraw() { super.repaint(); }
    
    public void setSize(Dimension d) { setSize(d.width, d.height); }
    
    public void setSize(int width, int height) {
        super.setSize(width, height);
        scalePanel.setLocation(width - scaleWidth, 0);
        scalePanel.setSize(scaleWidth, height);
    }
    
    protected void paintComponent(Graphics g) {
    	// Check to see if the panel has changed sizes since the last
    	// time it was rendered.
    	boolean sizeChanged = false;
        if(getWidth() != lastSize[0] || getHeight() != lastSize[1]) {
			lastSize[0] = getWidth();
			lastSize[1] = getHeight();
			sizeChanged = true;
		}

        // If the size of the panel has changed, we need to update
        // the crystal locations.
        if (sizeChanged) {
            // Determine the width and heights of the calorimeter crystals.
            int width;
            if (scalePanel.isVisible()) { width = getWidth() - scaleWidth; }
            else { width = getWidth(); }
            int height = getHeight();
            
            int boxWidth = width / xBoxes;
            int widthRem = width % xBoxes;
            int boxHeight = height / yBoxes;
            int heightRem = height % yBoxes;
            
            // Store the widths for each crystal.
            for(int x = 0; x < xBoxes; x++) {
            	widths[x] = boxWidth;
            	if(widthRem > 0) {
            		widths[x]++;
            		widthRem--;
            	}
            	xPosition[x + 1] = xPosition[x] + widths[x];
            }
            
            // Store the height for each crystal.
            for(int y = 0; y < yBoxes; y++) {
            	heights[y] = boxHeight;
            	if(heightRem > 0) {
            		heights[y]++;
            		heightRem--;
            	}
            	yPosition[y + 1] = yPosition[y] + heights[y];
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
        
        // Render the crystals at the locations calculated in the size
        // change block.
        for (int x = 0; x < xBoxes; x++) {
            for (int y = 0; y < yBoxes; y++) {
                // Determine the appropriate color for the box.
                Color crystalColor;
                if (disabled[x][y]) { crystalColor = Color.BLACK; }
                else if(defaultColor != null && hit[x][y] == 0) { crystalColor = defaultColor; }
                else { crystalColor = scale.getColor(hit[x][y]); }
                g.setColor(crystalColor);
                
                // Draw the crystal energy color.
                g.fillRect(xPosition[x], yPosition[y], widths[x], heights[y]);
                
                // Draw the crystal border.
                g.setColor(Color.BLACK);
                g.drawRect(xPosition[x], yPosition[y], widths[x] - 1, heights[y] - 1);
                
                // Draw a highlight, if needed.
                if(highlight[x][y] != null && !disabled[x][y]) {
                	g.setColor(highlight[x][y]);
                	g.drawRect(xPosition[x] + 1, yPosition[y] + 1, widths[x] - 3, heights[y] - 3);
                }
                
                // If there is a cluster, draw a circle.
                if (cluster[x][y]) {
                    // Get the appropriate cluster color.
                    Color c;
                    if (clusterColor == null) {
                    	int red = Math.abs(255 - crystalColor.getRed());
                        int blue = Math.abs(255 - crystalColor.getBlue());
                        int green = Math.abs(255 - crystalColor.getGreen());
                        c = new Color(red, green, blue);
                    }
                    else { c = clusterColor; }
                    
                    // Draw an circle on the cluster crystal.
                    g.setColor(c);
                    g.fillOval(xPosition[x] + clusterSpace[0], yPosition[y] + clusterSpace[1],
                    		clusterSpace[2], clusterSpace[2]);
                }
          	}
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
            boolean linear = scale.isLinearScale();
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
