package org.hps.monitoring.ecal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
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
    // The color-mapping scale used by to color calorimeter crystals.
    private MultiGradientScale scale = MultiGradientScale.makeRainbowScale(0.0,
            1.0);
    // The number of boxes in the x-direction.
    private int xBoxes = 1;
    // The number of boxes in the y-direction.
    private int yBoxes = 1;
    // The width of the scale.
    private int scaleWidth = 75;
    // Whether the scale has changed or not since its last rendering.
    private boolean scaleChanged = true;
    // Stores which calorimeter crystals are disabled.
    private boolean[][] disabled;
    // Stores the energies in each calorimeter crystal.
    private double[][] hit;
    // Stores whether a crystal is the location of a seed hit.
    private boolean[][] cluster;
    // Stores whether a crystal has changed.
    private boolean changed[][];
    // Stores whether the panel size has chaged.
    private boolean sizeChanged = true;
    // The panel on which the scale is rendered.
    ScalePanel scalePanel = new ScalePanel();

    // Efficiency variables for crystal placement.
    int boxWidth = 0;
    int widthRem = 0;
    int boxHeight = 0;
    int heightRem = 0;

    /**
     * <b>EcalPanel</b><br/>
     * <br/>
     * Initializes the calorimeter panel.
     * 
     * @param numXBoxes
     *            - The number of crystals in the x-direction.
     * @param numYBoxes
     *            - The number of crystals in the y-direction.
     **/
    public EcalPanel(int numXBoxes, int numYBoxes) {
        // Initialize the base component.
        super();

        // Set the number of calorimeter crystals.
        xBoxes = numXBoxes;
        yBoxes = numYBoxes;

        // Initialize the arrays.
        disabled = new boolean[xBoxes][yBoxes];
        hit = new double[xBoxes][yBoxes];
        cluster = new boolean[xBoxes][yBoxes];
        changed = new boolean[xBoxes][yBoxes];

        // Add the scale panel.
        setLayout(null);
        add(scalePanel);
        sizeChanged = true;
        scaleChanged = true;
    }

    /**
     * <b>setCrystalEnabled</b><br/>
     * <br/>
     * <code>public void <b>setCrystalEnabled</b>(int xIndex, int yIndex, boolean active)</code>
     * <br/>
     * <br/>
     * Sets whether the indicated crystal is enabled or not. Invalid indices
     * will be ignored.
     * 
     * @param xIndex
     *            - The x-coordinate of the crystal.
     * @param yIndex
     *            - The y-coordinate of the crystal.
     * @param active
     *            - This should be <code>true</code> if the crystal is active
     *            and <code>false</code> if it is not.
     * @throws IndexOutOfBoundsException
     *             Occurs when the given xy crystal coordinate does not point to
     *             a crystal.
     **/
    public void setCrystalEnabled(int xIndex, int yIndex, boolean active)
            throws IndexOutOfBoundsException {
        if (xIndex >= 0 && xIndex < xBoxes && yIndex >= 0 && yIndex < yBoxes) {
            disabled[xIndex][yIndex] = !active;
            changed[xIndex][yIndex] = true;
        } else {
            throw new IndexOutOfBoundsException(String.format(
                    "Invalid crystal address (%2d, %2d).", xIndex, yIndex));
        }
    }

    /**
     * <b>addCrystalEnergy</b><br/>
     * <br/>
     * <code>public void <b>addCrystalEnergy</b>(int xIndex, int yIndex, double energy)</code>
     * <br/>
     * <br/>
     * Adds the indicated quantity of energy to the crystal at the given
     * coordinates.
     * 
     * @param xIndex
     *            - The x-coordinate of the crystal.
     * @param yIndex
     *            - The y-coordinate of the crystal.
     * @param energy
     *            - The energy to add.
     * @throws IndexOutOfBoundsException
     *             Occurs when the given xy crystal coordinate does not point to
     *             a crystal.
     **/
    public void addCrystalEnergy(int xIndex, int yIndex, double energy)
            throws IndexOutOfBoundsException {
        if (xIndex >= 0 && xIndex < xBoxes && yIndex >= 0 && yIndex < yBoxes) {
            this.hit[xIndex][yIndex] += energy;
            changed[xIndex][yIndex] = true;
        } else {
            throw new IndexOutOfBoundsException(String.format(
                    "Invalid crystal address (%2d, %2d).", xIndex, yIndex));
        }
    }

    /**
     * <b>setCrystalCluster</b>
     * <code>public void <b>setCrystalCluster</b>(int xIndex, int yIndex, boolean cluster)</code>
     * <br/>
     * <br/>
     * Sets whether a crystal is also the location of a seed hit.
     * 
     * @param xIndex
     *            - The x-coordinate of the crystal.
     * @param yIndex
     *            - The y-coordinate of the crystal.
     * @param cluster
     *            - This should be <code>true</code> if there is a seed hit and
     *            <code>false</code> if there is not.
     * @throws IndexOutOfBoundsException
     *             Occurs when the given xy crystal coordinate does not point to
     *             a crystal.
     **/
    public void setCrystalCluster(int xIndex, int yIndex, boolean cluster)
            throws IndexOutOfBoundsException {
        if (xIndex >= 0 && xIndex < xBoxes && yIndex >= 0 && yIndex < yBoxes) {
            this.cluster[xIndex][yIndex] = cluster;
            changed[xIndex][yIndex] = true;
        } else {
            throw new IndexOutOfBoundsException(String.format(
                    "Invalid crystal address (%2d, %2d).", xIndex, yIndex));
        }
    }

    /**
     * <b>clearCrystals</b><br/>
     * <br/>
     * <code>public void <b>clearCrystals</b>()</code><br/>
     * <br/>
     * Sets all crystal energies to zero and removes all clusters. This <b>does
     * not</b> enable any disabled crystals.
     **/
    public void clearCrystals() {
        for (int x = 0; x < xBoxes; x++) {
            for (int y = 0; y < yBoxes; y++) {
                if (hit[x][y] != 0.0) {
                    hit[x][y] = 0.0;
                    changed[x][y] = true;
                }
                if (cluster[x][y]) {
                    cluster[x][y] = false;
                    changed[x][y] = true;
                }
            }
        }
    }

    /**
     * <b>setClusterColor</b><br/>
     * <br/>
     * <code>public void <b>setClusterColor</b>(Color c)</code><br/>
     * <br/>
     * Sets the color of the seed hit marker.
     * 
     * @param c
     *            - The color to be used for seed hit markers. A value of
     *            <code>null</code> will result in seed hit markers being the
     *            inverse color of the crystal in which they appear.
     **/
    public void setClusterColor(Color c) {
        clusterColor = c;
    }

    /**
     * <b>setMinimum</b><br/>
     * <br>
     * <code>public void <b>setMinimum</b>(double minimum)</code><br/>
     * <br/>
     * Sets the minimum value of the color mapping scale. Energies below this
     * value will all be the same minimum color.
     * 
     * @param minimum
     *            - The minimum energy to be mapped.
     **/
    public void setMinimum(double minimum) {
        scale.setMinimum(minimum);
        scaleChanged = true;
    }

    /**
     * <b>setMaximum</b><br/>
     * <br/>
     * <code>public void <b>setMaximum</b>(double maximum)</code><br/>
     * <br/>
     * Sets the maximum value of the color mapping scale. Energies above this
     * value will all be the same maximum color.
     * 
     * @param maximum
     *            - The maximum energy to be mapped.
     **/
    public void setMaximum(double maximum) {
        scale.setMaximum(maximum);
        scaleChanged = true;
    }

    /**
     * <b>setScalingLinear</b><br/>
     * <br/>
     * <code>public void <b>setScalingLinear</b>()<br/><br/>
     * Sets the color mapping scale behavior to linear mapping.
     **/
    public void setScalingLinear() {
        scale.setScalingLinear();
        scaleChanged = true;
    }

    /**
     * <b>setScalingLogarithmic</b><br/>
     * <br/>
     * <code>public void <b>setScalingLogarithmic</b></code><br/>
     * <br/>
     * Sets the color mapping scale behavior to logarithmic mapping.
     **/
    public void setScalingLogarithmic() {
        scale.setScalingLogarithmic();
        scaleChanged = true;
    }

    /**
     * <b>setScaleEnabled</b><br/>
     * <br/>
     * <code>public void <b>setScaleEnabled</b>(boolean enabled)</code><br/>
     * <br/>
     * Sets whether the scale should be visible or not.
     * 
     * @param enabled
     *            - <code>true</code> indicates that the scale should be visible
     *            and <code>false</code> that it should be hidden.
     **/
    public void setScaleEnabled(boolean enabled) {
        if (scalePanel.isVisible() != enabled) {
            scalePanel.setVisible(enabled);
            scaleChanged = true;
            sizeChanged = true;
        }
    }

    /**
     * <b>redraw</b><br/>
     * <br/>
     * <code>public void <b>redraw</b>()</code> Re-renders the calorimeter
     * panel.
     **/
    public void redraw() {
        super.repaint();
    }

    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
        scalePanel.setLocation(width - scaleWidth, 0);
        scalePanel.setSize(scaleWidth, height);
        sizeChanged = true;
    }

    protected void paintComponent(Graphics g) {
        if (sizeChanged) {
            // Determine the width and heights of the calorimeter crystals.
            int width;
            if (scalePanel.isVisible()) {
                width = getWidth() - scaleWidth;
            } else {
                width = getWidth();
            }
            int height = getHeight();

            boxWidth = width / xBoxes;
            widthRem = width % xBoxes;
            boxHeight = height / yBoxes;
            heightRem = height % yBoxes;
        }
        int heightRemReset = heightRem;
        int widthRemReset = widthRem;

        // Start drawing the calorimeter crystals. To avoid having empty
        // space, we distribute the extra widthRem pixels to the boxes at
        // a rate of one pixel for box until we run out. We do the same thing
        // for the heightRem.
        int curX = 0;
        int curY = 0;
        for (int x = 0; x < xBoxes; x++) {
            // Determine if this column should use an extra pixel.
            int tw = boxWidth;
            if (widthRem != 0) {
                tw++;
                widthRem--;
            }
            for (int y = 0; y < yBoxes; y++) {
                // Determine if this row should use an extra pixel.
                int th = boxHeight;
                if (heightRem != 0) {
                    th++;
                    heightRem--;
                }

                if (sizeChanged || scaleChanged || changed[x][y]) {
                    // Determine the appropriate color for the box.
                    Color crystalColor;
                    if (disabled[x][y]) {
                        crystalColor = Color.BLACK;
                    } else {
                        crystalColor = scale.getColor(hit[x][y]);
                    }
                    g.setColor(crystalColor);

                    // Draw the box.
                    g.fillRect(curX, curY, tw, th);
                    g.setColor(Color.BLACK);
                    g.drawRect(curX, curY, tw, th);

                    // If there is a cluster, draw an x.
                    if (cluster[x][y]) {
                        // Get the correct coordinates.
                        double ltw = (0.3 * tw) / 2;
                        double lth = (0.5 * th) / 2;
                        double[] lx = { curX + ltw, curX + tw - ltw };
                        double[] ly = { curY + lth, curY + th - lth };

                        // Get the appropriate cluster color.
                        Color c;
                        if (clusterColor == null) {
                            int red = Math.abs(255 - crystalColor.getRed());
                            int blue = Math.abs(255 - crystalColor.getBlue());
                            int green = Math.abs(255 - crystalColor.getGreen());
                            c = new Color(red, green, blue);
                        } else {
                            c = clusterColor;
                        }

                        // Draw an x on the cluster crystal.
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setColor(c);
                        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        // RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setStroke(new BasicStroke(2));
                        g2.draw(new Line2D.Double(lx[0], ly[0], lx[1], ly[1]));
                        g2.draw(new Line2D.Double(lx[0], ly[1], lx[1], ly[0]));
                        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        // RenderingHints.VALUE_ANTIALIAS_OFF);
                    }

                    // Note that this crystals has been updated.
                    changed[x][y] = false;
                }

                // Increment the current y position.
                curY += th;
            }

            // Increment the current x position.
            curX += tw;

            // Reset the current y position and heightRem.
            curY = 0;
            heightRem = heightRemReset;
        }

        // If the scale has changed, redraw the scale panel as well.
        if (scaleChanged && scalePanel.isVisible()) {
            scalePanel.redraw();
        }

        // Indicate that the any size changes have been handled.
        scaleChanged = false;
        sizeChanged = false;

        // Reset the height and width remainder variables.
        heightRem = heightRemReset;
        widthRem = widthRemReset;
    }

    /**
     * The local class <b>ScalePanel</b> renders the scale for the calorimeter
     * color map.
     **/
    private class ScalePanel extends JPanel {
        /**
         * <b>redraw</b><br/>
         * <br/>
         * <code>public void <b>redraw</b>()</code><br/>
         * <br/>
         * Orders the scale to re-render itself.
         **/
        public void redraw() {
            super.repaint();
        }

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
            } else {
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
            } else {
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
                if (linear) {
                    scaledValue = curValue;
                } else {
                    scaledValue = Math.pow(10, curValue);
                }
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
                double fStep = (height - 2.0 * fontHeight) / fontHeight;
                double halfStep = fStep / 2.0;

                // Get the scaling value.
                double fScale;
                double fMin;
                if (linear) {
                    fScale = scale.getMaximum() - scale.getMinimum();
                    fMin = scale.getMinimum();
                } else {
                    fScale = Math.log10(scale.getMaximum())
                            - Math.log10(scale.getMinimum());
                    fMin = Math.log10(scale.getMinimum());
                }

                // Populate the first and last values.
                NumberFormat nf = new DecimalFormat("0.#E0");
                g.setColor(Color.WHITE);
                g.drawString(nf.format(scale.getMaximum()), width + 5,
                        fontHeight);
                g.drawString(nf.format(scale.getMinimum()), width + 5,
                        height - 3);

                // Calculate text placement variables.
                double heightAvailable = height - 2.0 * fontHeight;
                double heightDefault = heightAvailable / (1.5 * fontHeight);
                int num = (int) Math.floor(heightAvailable / heightDefault);
                double heightRemainder = heightAvailable
                        - (num * heightDefault);
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
                } else {
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
                    if (!linear) {
                        lVal = Math.pow(10, lVal);
                    }
                    g.drawString(nf.format(lVal), lX[2], lHeight + lShift);
                    lTemp += lSpacing;
                    lHeight = (int) (fontHeight + lTemp);
                }
            }
        }
    }
}
