package org.lcsim.geometry.compact.converter.lcdd;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.lcdd.util.Box;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.Material;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

public class Hodoscope_v1 extends LCDDSubdetector {
	/**
	 * Defines the distance between the forward and rear layers that
	 * is to be left open in order to allow for a buffer material.
	 * The rear layer will start at a position
	 * <code>zLayer1 + depthLayer1 + layerBuffer</code>.
	 */
	private double layerBuffer;
	/**
	 * Specifies the displacements for each element of the hodoscope.
	 * It is possible to specify displacements for each layer,
	 * further sub-divided into the top and bottom portions. All of
	 * the units are millimeters.
	 * <br/><br/>
	 * The first array index specifies the layer, and can be accessed
	 * via the static class variables {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#LAYER1
     * LAYER1} or {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#LAYER2
     * LAYER2}. The second index specifies the half of the detector,
     * top or bottom, to which the displacement refers. This can be
     * defined using the static class variables {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#TOP
     * TOP} or {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#BOTTOM
     * BOTTOM}. Lastly, the third index specifies the coordinate axis
     * in which the displacement applies. This can be defined by the
     * static class variables {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#X X},
     * {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#Y Y},
     * or {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#Z Z}.
	 */
	private double[][][] positionValues = new double[2][2][3];
	/**
	 * Specifies the front (closest to the SVT) hodoscope layer.
	 */
	private static final int LAYER1 = 0;
	/**
	 * Specifies the rear (closest to the calorimeter) hodoscope
	 * layer.
	 */
	private static final int LAYER2 = 1;
	/**
	 * Specifies the top portion hodoscope layers.
	 */
	private static final int TOP = 0;
	/**
	 * Specifies the bottom portion hodoscope layers.
	 */
	private static final int BOTTOM = 1;
	/**
	 * Specifies the displacement of an element in the x-axis. This
	 * defines the point where the edge of the element closest to the
	 * calorimeter center begins.
	 */
	private static final int X = 0;
	/**
	 * Specifies the displacement of an element in the y-axis. This
	 * defines the point where the edge of the element closest to the
	 * beam gap center begins.
	 */
	private static final int Y = 1;
	/**
	 * Specifies the displacement of an element in the z-axis. This
	 * defines the point where the forward (SVT-facing) edge of the
	 * element begins.
	 */
	private static final int Z = 2;
    /**
	 * Specifies the size in the y-direction of the hodoscope pixels.
	 * Units are in millimeters.
     */
	private static final double PIXEL_HEIGHT = 59.225;
	/**
	 * Specifies the size in the z-direction of the hodoscope pixels.
	 * Units are in millimeters.
	 */
	private static final double PIXEL_DEPTH = 10;
	/**
	 * Specifies the global shift needed to align geometry with the
	 * calorimeter face in the x-direction, as the calorimeter center
	 * does not occur at x = 0 mm.
	 */
	private static final double X_SHIFT = 21.17;
	/**
	 * The default material for hodoscope pixels, if no other
	 * material is defined.
	 */
	private static final String DEFAULT_MATERIAL="Polystyrene";
	/**
	 * Defines the rotation used by all hodoscope pixels.
	 */
	private static final Rotation PIXEL_ROTATION = new Rotation("hodo_rot", 0.0, 0.0, 0.0);
	
    Hodoscope_v1(Element node) throws JDOMException {
    	// Instantiate the superclass.
        super(node);
        
        // Set the default positioning values.
        layerBuffer = 10;
        positionValues[LAYER1][TOP][X] = 43.458;
        positionValues[LAYER1][TOP][Y] = 14.21392678;
        positionValues[LAYER1][TOP][Z] = 1090;
        positionValues[LAYER1][BOTTOM][X] = 43.458;
        positionValues[LAYER1][BOTTOM][Y] = 14.21392678;
        positionValues[LAYER1][BOTTOM][Z] = 1090;
        
        positionValues[LAYER2][TOP][X] = 48;
        positionValues[LAYER2][TOP][Y] = 14.21392678;
        positionValues[LAYER2][BOTTOM][X] = 48;
        positionValues[LAYER2][BOTTOM][Y] = 14.21392678;
    }
    
    void addToLCDD(LCDD lcdd, SensitiveDetector sens) throws JDOMException {
        System.out.println("Hopdoscop_v1.addToLCDD");
        
        /*
         * Get the basic hodoscope definition variables from the XML
         * file. These are defined in the detector definition.
         */
        // Get basic hodoscope positioning data. Note that there is
        // no layer 2 variable defining the z-displacement. This is
        // instead determined as a function of the position of layer1
        // in z and the layer buffer value. Layer 2 is defined to
        // begin immediately after the buffer, which occurs directly
        // after layer 1 ends.
        setPositionValue(node, "layer1_top_x", LAYER1, TOP, X);
        setPositionValue(node, "layer1_top_y", LAYER1, TOP, Y);
        setPositionValue(node, "layer1_top_z", LAYER1, TOP, Z);
        setPositionValue(node, "layer1_bot_x", LAYER1, TOP, X);
        setPositionValue(node, "layer1_bot_y", LAYER1, TOP, Y);
        setPositionValue(node, "layer1_bot_z", LAYER1, TOP, Z);
        setPositionValue(node, "layer2_top_x", LAYER2, TOP, X);
        setPositionValue(node, "layer2_top_y", LAYER2, TOP, Y);
        
        // Get the layer buffer.
        setLayerBuffer(node, "buffer_size");
        
        // Define the layer 2 z-position based on the layer buffer
        // and the position of layer 1. The layer buffer should start
        // immediately after layer 1, and layer 2 immediately after
        // the layer buffer.
        positionValues[LAYER2][TOP][Z] = positionValues[LAYER1][TOP][Z] + PIXEL_DEPTH + layerBuffer;
        positionValues[LAYER2][BOTTOM][Z] = positionValues[LAYER1][BOTTOM][Z] + PIXEL_DEPTH + layerBuffer;
        
        // Get the material for the hodoscope crystals.
        String materialName = DEFAULT_MATERIAL;
        Element materialNode = node.getChild("material");
        if(materialNode != null) {
    		// Attempt to obtain the variable attribute. If it does
    		// not exist, there is a formatting problem with the
    		// detector declaration in the compact.xml. Produce an
    		// exception and alert the user.
        	Attribute materialAttribute = materialNode.getAttribute("name");
    		if(materialAttribute == null) {
    			throw new RuntimeException(getClass().getSimpleName() + ": Node \"" + materialAttribute
    					+ "\" is missing attribute \"name\".");
    		}
        	materialName = materialAttribute.getValue();
        }
        Material material = lcdd.getMaterial(materialName);
        
        // DEBUG :: Output the values that have been read in.
        System.out.println("Layer 1:");
        System.out.println("\tTop:");
        System.out.println("\t\tdx: " + positionValues[LAYER1][TOP][X]);
        System.out.println("\t\tdy: " + positionValues[LAYER1][TOP][Y]);
        System.out.println("\t\tdz: " + positionValues[LAYER1][TOP][Z]);
        System.out.println("\tBottom:");
        System.out.println("\t\tdx: " + positionValues[LAYER1][BOTTOM][X]);
        System.out.println("\t\tdy: " + positionValues[LAYER1][BOTTOM][Y]);
        System.out.println("\t\tdz: " + positionValues[LAYER1][BOTTOM][Z]);
        System.out.println("Layer 2:");
        System.out.println("\tTop:");
        System.out.println("\t\tdx: " + positionValues[LAYER2][TOP][X]);
        System.out.println("\t\tdy: " + positionValues[LAYER2][TOP][Y]);
        System.out.println("\t\tdz: " + positionValues[LAYER2][TOP][Z]);
        System.out.println("\tBottom:");
        System.out.println("\t\tdx: " + positionValues[LAYER2][BOTTOM][X]);
        System.out.println("\t\tdy: " + positionValues[LAYER2][BOTTOM][Y]);
        System.out.println("\t\tdz: " + positionValues[LAYER2][BOTTOM][Z]);
        
        // Define constant rotations used for all hodoscope crystals.
        lcdd.getDefine().addRotation(PIXEL_ROTATION);
        
        // Define the hodoscope size parameters.
        final double[][] widths = {
        		{ 15, 44, 44, 44, 22 },
        		{ 37, 44, 44, 44 }
        };
        
        // Create the hodoscope pixels.
        for(int layer = LAYER1; layer <= LAYER2; layer++) {
        	double xShift = 0.0;
        	for(int pixel = 0; pixel < widths[layer].length; pixel++) {
        		for(int topBot = TOP; topBot <= BOTTOM; topBot++) {
        			makePixel(lcdd, sens, material, layer, topBot, pixel, widths[layer][pixel], xShift);
        		}
    			xShift += widths[layer][pixel] + 1;
        	}
        }
    }
    
    /**
     * Defines a unique name for a hodoscope pixel based on its
     * layer, whether it is a top or bottom pixel, and its x-index.
     * @param layer - The layer of the hodoscope for which this UID
     * is to correspond. This can only be defined as either of the
     * values {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#LAYER1
     * LAYER1} or {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#LAYER2
     * LAYER2}.
     * @param topBottom - Whether this UID corresponds to the top or
     * the bottom section of the hodoscope. This can only be defined
     * as one of the two variables {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#TOP
     * TOP} or {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#BOTTOM
     * BOTTOM}.
     * @param ix - The x-index for the pixel. This must be an integer
     * value that is non-negative.
     * @return Returns a unique identifier string.
     */
    private static final String getName(int layer, int topBot, int ix) {
    	return String.format("L" + (layer + 1) + (topBot == TOP ? 'T' : 'B') + 'P' + ix);
    }
    
    private final void makePixel(LCDD lcdd, SensitiveDetector sens, Material material, int layer, int topBot, int ix,
    		double pixelWidth, double xShift) {
    	// Get a unique string that represents this pixel.
    	String uid = getName(layer, topBot, ix);
    	
        // Create the geometric shapes that define the pixel. These
        // are always rectangular prisms. The prism is then used to
        // define a volume, which sets the material. Lastly, it is
        // assigned the display properties for the hodoscope and is
        // attached to the hodoscope's sensitive detector.
        Box pixelShape = new Box("hodo_pixel_" + uid, pixelWidth, PIXEL_HEIGHT, PIXEL_DEPTH);
        Volume pixelVolume = new Volume("hodo_vol_" + uid, pixelShape, material);
        pixelVolume.setSensitiveDetector(sens);
        setVisAttributes(lcdd, getNode(), pixelVolume);
        lcdd.add(pixelShape);
        lcdd.add(pixelVolume);
        
        // Define the position of the crystal. Note that the position
        // of a volume is defined as the position of its centerpoint,
        // so it must shifted by half its width and height to account
        // for this and place its edge at the correct position. After
        // this, the position and default rotation defines should be
        // must be added.
        Position pos = new Position("hodo_pos_" + uid,
        		X_SHIFT + xShift + positionValues[layer][topBot][X] + (pixelShape.getX() / 2),
        		(topBot == TOP ? 1 : -1) * (positionValues[layer][topBot][Y] + (pixelShape.getY() / 2)),
        		positionValues[layer][topBot][Z] + (pixelShape.getZ() / 2));
        lcdd.getDefine().addPosition(pos);
        
        // Lastly, create the physical object representing the pixel.
        // This can also have a number of additional properties that
        // can be attached to it.
        PhysVol physvolL1TP1 = new PhysVol(pixelVolume, lcdd.pickMotherVolume(this), pos, PIXEL_ROTATION);
        physvolL1TP1.addPhysVolID("system", getSystemID());
        physvolL1TP1.addPhysVolID("ix", ix);
        physvolL1TP1.addPhysVolID("iy", topBot == TOP ? 1 : -1);
        physvolL1TP1.addPhysVolID("iz", layer == LAYER1 ? 1 : 2);
    }
    
    /**
     * Sets the layer buffer value to the value stored in the
     * indicated variable element of the detector compact.xml
     * description. Note that it is required for the variable to
     * declared in the format <br/><br/>
     * <code>&lt;VAR_NAME value="DOUBLE_VALUE"/&gt;</code>
     * <br/><br/>
     * If the variable is not defined, the hard-coded default value
     * will be used instead.
     * @param root - The detector XML node which serves as a parent
     * to the variable nodes.
     * @param var - The {@link java.lang.String String} name of the
     * variables.
     * @throws DataConversionException Occurs if the value defined is
     * not convertible to a <code>double</code> primitive.
     * @throws RuntimeException Occurs if the variable node exists,
     * but is missing the necessary value attribute.
     */
    private final void setLayerBuffer(Element root, String var) throws DataConversionException, RuntimeException {
    	// Get the value node. If it does not exist, nothing needs
    	// to be done as the layer buffer variable is instantiated to
    	// its default values already. Otherwise, attempt to access
    	// the variable.
    	double varVal = getNodeVariable(root, var);
    	if(!Double.isNaN(varVal)) {
    		layerBuffer = varVal;
    	}
    }
    
    /**
     * Sets the position value specified to the value stored in the
     * variable element of the detector compact.xml description. Note
     * that it is required for the variable to declared in the format
     * <br/><br/>
     * <code>&lt;VAR_NAME value="DOUBLE_VALUE"/&gt;</code>
     * <br/><br/>
     * If the variable is not defined, the hard-coded default value
     * will be used instead.
     * @param root - The detector XML node which serves as a parent
     * to the variable nodes.
     * @param var - The {@link java.lang.String String} name of the
     * variables.
     * @param layer - The layer of the hodoscope for which this value
     * is to be applied. This can only be defined as either of the
     * values {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#LAYER1
     * LAYER1} or {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#LAYER2
     * LAYER2}.
     * @param topBottom - Whether this variable is to be applied to
     * the top or the bottom section of the hodoscope. This can only
     * be defined as one of the two variables {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#TOP
     * TOP} or {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#BOTTOM
     * BOTTOM}.
     * @param coordinate - To which of the three spatial coordinates
     * the variable is to be applied. This can be one of the three
     * variables {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#X X},
     * {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#Y Y},
     * or {@link
     * org.lcsim.geometry.compact.converter.lcdd.Hodoscope_v1#Z Z}.
     * @throws DataConversionException Occurs if the value defined is
     * not convertible to a <code>double</code> primitive.
     * @throws IllegalArgumentException Occurs if any of the values
     * <code>layer</code>, <code>topBottom</code>, or
     * <code>coordinate</code> are not one of the allowed entries.
     * @throws RuntimeException Occurs if the variable node exists,
     * but is missing the necessary value attribute.
     */
    private final void setPositionValue(Element root, String var, int layer, int topBottom, int coordinate)
    		throws DataConversionException, IllegalArgumentException, RuntimeException {
    	// Validate the hodoscope position variable indices.
    	if(layer != LAYER1 && layer != LAYER2) {
    		throw new IllegalArgumentException(getClass().getSimpleName() + ": Unrecognized layer index " + layer + ".");
    	} if(topBottom != TOP && topBottom != BOTTOM) {
    		throw new IllegalArgumentException(getClass().getSimpleName() + ": Unrecognized top/bottom identifier " + topBottom + ".");
    	} if(coordinate != X && coordinate != Y && coordinate != Z) {
    		throw new IllegalArgumentException(getClass().getSimpleName() + ": Unrecognized coordinate index " + coordinate + ".");
    	}
    	
    	// Get the value node. If it does not exist, nothing needs
    	// to be done as the position variables are instantiated to
    	// their default values already. Otherwise, attempt to access
    	// the variable.
    	double varVal = getNodeVariable(root, var);
    	if(!Double.isNaN(varVal)) {
    		positionValues[layer][topBottom][coordinate] = varVal;
    	}
    }
    
    /**
     * Gets the value specified from the variable element of the
     * detector compact.xml description. Note that it is required for
     * the variable to declared in the format <br/><br/>
     * <code>&lt;VAR_NAME value="DOUBLE_VALUE"/&gt;</code>
     * <br/><br/>
     * If the specified variable node does not exist as a child of
     * the parent node <code>root</code>, a value of {@link
     * java.lang.Double.NaN Double.NaN} is returned instead.
     * @param root - The detector XML node which serves as a parent
     * to the variable nodes.
     * @param varName - The {@link java.lang.String String} name of
     * the variables.
     * @return Returns the variable, if the node exists. Otherwise, a
     * value of {@link java.lang.Double.NaN Double.NaN} is returned.
     * @throws DataConversionException Occurs if the value defined is
     * not convertible to a <code>double</code> primitive.
     * @throws RuntimeException Occurs if the variable node exists,
     * but is missing the necessary value attribute.
     */
    private final double getNodeVariable(Element root, String varName) throws DataConversionException, RuntimeException {
    	// Get the value node. If it exists, attempt to access the
    	// variable. Otherwise, just return Double.NaN.
    	Element valueNode = root.getChild(varName);
    	if(valueNode != null) {
    		// Attempt to obtain the variable attribute. If it does
    		// not exist, there is a formatting problem with the
    		// detector declaration in the compact.xml. Produce an
    		// exception and alert the user.
    		Attribute valueAttribute = valueNode.getAttribute("value");
    		if(valueAttribute == null) {
    			throw new RuntimeException(getClass().getSimpleName() + ": Node \"" + varName + "\" is missing attribute \"value\".");
    		}
    		
    		// Otherwise, parse the value and store it.
    		return valueNode.getAttribute("value").getDoubleValue();
    	} else {
    		return Double.NaN;
    	}
    }
    
    public boolean isCalorimeter() {
        return true;
    }
}
