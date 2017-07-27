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
    private static final double BUFFER_DEPTH = 10;
    private static final double BUFFER_WIDTH = 182.2;
    private static final double BUFFER_X = 45.7738565735;
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
     * Specifies the default size in the y-direction of the hodoscope
     * scintillators. Units are in millimeters.
     */
    private static final double PIXEL_HEIGHT = 59.225;
    /**
     * Specifies the default size in the z-direction of the hodoscope
     * scintillators. Units are in millimeters.
     */
    private static final double PIXEL_DEPTH = 9.5;
    /**
     * Specifies the default size in the z-direction of the hodoscope
     * scintillator cover material. Units are in millimeters.
     */
    private static final double COVER_DEPTH = 0.25;
    /**
     * Specifies the default thickness of the hodoscope scintillator
     * reflector material. Units are in millimeters.
     */
    private static final double REFLECTOR_DEPTH = 0.05;
    /**
     * Specifies for the widths for each pixel of the hodoscope
     * layers. The top and bottom layers are taken to have the same
     * number of pixels of the same widths.
     */
    private double[][] widths = {
            { 15, 44, 44, 44, 22 },
            { 37, 44, 44, 44 }
    };
    /**
     * Specifies the global shift needed to align geometry with the
     * calorimeter face in the x-direction, as the calorimeter center
     * does not occur at x = 0 mm.
     */
    private static final double X_SHIFT = 21.17;
    /**
     * Defines the rotation used by all hodoscope pixels.
     */
    private static final Rotation PIXEL_ROTATION = new Rotation("hodo_rot", 0.0, 0.0, 0.0);
    
    Hodoscope_v1(Element node) throws JDOMException {
        // Instantiate the superclass.
        super(node);
        
        // Set the default positioning values.
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
        System.out.println("Hopdoscop_v1.addToLCDD(LCDD, SensitiveDetector)");
        
        /*
         * Get the basic hodoscope definition variables from the XML
         * file. These are defined in the detector definition.
         */
        
        double temp = Double.NaN;
        
        // Get basic hodoscope positioning data. Note that there is
        // no layer 2 variable defining the z-displacement. This is
        // instead determined as a function of the position of layer1
        // in z and the layer buffer value. Layer 2 is defined to
        // begin immediately after the buffer, which occurs directly
        // after layer 1 ends.
        final String[][][] varNames = {
                {
                    { "layer1_top_x", "layer1_top_y" },
                    { "layer1_bot_x", "layer1_bot_y" }
                },
                {
                    { "layer2_top_x", "layer2_top_y" },
                    { "layer2_bot_x", "layer2_bot_y" }
                }
        };
        for(int layer = LAYER1; layer <= LAYER2; layer++) {
            for(int topBot = TOP; topBot <= BOTTOM; topBot++) {
                for(int coor = X; coor <= Y; coor++) {
                    temp = getDoubleVariable(node, varNames[layer][topBot][coor]);
                    if(!Double.isNaN(temp)) {
                        positionValues[layer][topBot][coor] = temp;
                    }
                }
            }
        }
        
        // Handle the layer 1 z-positioning.
        temp = getDoubleVariable(node, "layer1_top_z");
        if(!Double.isNaN(temp)) { positionValues[LAYER1][TOP][Z] = temp; }
        temp = getDoubleVariable(node, "layer1_bot_z");
        if(!Double.isNaN(temp)) { positionValues[LAYER1][BOTTOM][Z] = temp; }
        
        // Get the layer buffer.
        double bufferDepth = getDoubleVariable(node, "buffer_depth");
        if(Double.isNaN(bufferDepth)) { bufferDepth = BUFFER_DEPTH; }
        double bufferWidth = getDoubleVariable(node, "buffer_width");
        if(Double.isNaN(bufferWidth)) { bufferWidth = BUFFER_WIDTH; }
        double bufferX = getDoubleVariable(node, "buffer_x");
        if(Double.isNaN(bufferX)) { bufferX = BUFFER_X; }
        
        // Load the universal pixel parameters.
        PixelParameters params = new PixelParameters();
        params.scintillatorHeight = getDoubleVariable(node, "scintillator_depth_height");
        if(Double.isNaN(params.scintillatorHeight)) { params.scintillatorHeight = PIXEL_HEIGHT; }
        params.scintillatorDepth = getDoubleVariable(node, "scintillator_depth");
        if(Double.isNaN(params.scintillatorDepth)) { params.scintillatorDepth = PIXEL_DEPTH; }
        params.coverDepth = getDoubleVariable(node, "cover_depth");
        if(Double.isNaN(params.coverDepth)) { params.coverDepth = COVER_DEPTH; }
        params.reflectorDepth = getDoubleVariable(node, "reflector_depth");
        if(Double.isNaN(params.reflectorDepth)) { params.reflectorDepth = REFLECTOR_DEPTH; }
        
        // Define the layer 2 z-position based on the layer buffer
        // and the position of layer 1. The layer buffer should start
        // immediately after layer 1, and layer 2 immediately after
        // the layer buffer.
        positionValues[LAYER2][TOP][Z] = positionValues[LAYER1][TOP][Z] + params.scintillatorDepth
                + (2 * params.coverDepth) + bufferDepth;
        positionValues[LAYER2][BOTTOM][Z] = positionValues[LAYER1][BOTTOM][Z] + params.scintillatorDepth
                + (2 * params.coverDepth) + bufferDepth;
        
        // Get the hodoscope pixel widths.
        double[] tempArray = getDoubleArrayVariable(node, "scintillator_width_layer1");
        if(tempArray != null) { widths[LAYER1] = tempArray; }
        tempArray = getDoubleArrayVariable(node, "scintillator_width_layer2");
        if(tempArray != null) { widths[LAYER2] = tempArray; }
        
        // Get the material for the hodoscope crystals.
        params.scintillatorMaterial = getMaterialVariable(lcdd, node, "scintillator_material");
        if(params.scintillatorMaterial == null) {
            throw new IllegalArgumentException(getClass().getSimpleName()
                    + ": Mandatory variable \"scintillator_material\" is not defined.");
        }
        
        params.coverMaterial = getMaterialVariable(lcdd, node, "cover_material");
        if(params.coverMaterial == null) {
            throw new IllegalArgumentException(getClass().getSimpleName()
                    + ": Mandatory variable \"cover_material\" is not defined.");
        }
        
        params.reflectorMaterial = getMaterialVariable(lcdd, node, "reflector_material");
        if(params.reflectorMaterial == null) {
            throw new IllegalArgumentException(getClass().getSimpleName()
                    + ": Mandatory variable \"reflector_material\" is not defined.");
        }
        
        Material bufferMaterial = getMaterialVariable(lcdd, node, "buffer_material");
        if(bufferMaterial == null) {
            throw new IllegalArgumentException(getClass().getSimpleName()
                    + ": Mandatory variable \"buffer_material\" is not defined.");
        }
        
        // DEBUG :: Output the values that have been read in.
        System.out.println("Layer 1:");
        System.out.println("\tTop:");
        System.out.println("\t\tdx: " + positionValues[LAYER1][TOP][X] + " mm");
        System.out.println("\t\tdy: " + positionValues[LAYER1][TOP][Y] + " mm");
        System.out.println("\t\tdz: " + positionValues[LAYER1][TOP][Z] + " mm");
        System.out.println("\tBottom:");
        System.out.println("\t\tdx: " + positionValues[LAYER1][BOTTOM][X] + " mm");
        System.out.println("\t\tdy: " + positionValues[LAYER1][BOTTOM][Y] + " mm");
        System.out.println("\t\tdz: " + positionValues[LAYER1][BOTTOM][Z] + " mm");
        System.out.println("Layer 2:");
        System.out.println("\tTop:");
        System.out.println("\t\tdx: " + positionValues[LAYER2][TOP][X] + " mm");
        System.out.println("\t\tdy: " + positionValues[LAYER2][TOP][Y] + " mm");
        System.out.println("\t\tdz: " + positionValues[LAYER2][TOP][Z] + " mm");
        System.out.println("\tBottom:");
        System.out.println("\t\tdx: " + positionValues[LAYER2][BOTTOM][X] + " mm");
        System.out.println("\t\tdy: " + positionValues[LAYER2][BOTTOM][Y] + " mm");
        System.out.println("\t\tdz: " + positionValues[LAYER2][BOTTOM][Z] + " mm");
        System.out.println();
        System.out.println("Other Values:");
        System.out.println("\tReflector Thickness: " + params.reflectorDepth + " mm");
        System.out.println("\tCover z-Dimension: " + params.coverDepth + " mm");
        System.out.println("\tBuffer Spacing: " + bufferDepth + " mm");
        System.out.println("\tScintillator y-Dimension: " + params.scintillatorHeight + " mm");
        System.out.println("\tScintillator z-Dimension: " + params.scintillatorDepth + " mm");
        System.out.println("\tScintillator x-Dimension:");
        System.out.print("\t\tLayer 1: ");
        for(double d : widths[LAYER1]) {
            System.out.print(d + "    ");
        }
        System.out.print("\n\t\tLayer 2: ");
        for(double d : widths[LAYER2]) {
            System.out.print(d + "    ");
        }
        System.out.println();
        
        /*
         * Make the hodoscope geometry.
         */
        
        // Define constant rotations used for all hodoscope crystals.
        lcdd.getDefine().addRotation(PIXEL_ROTATION);
        
        // Create the hodoscope pixels.
        for(int layer = LAYER1; layer <= LAYER2; layer++) {
            double xShift = 0.0;
            for(int pixel = 0; pixel < widths[layer].length; pixel++) {
                for(int topBot = TOP; topBot <= BOTTOM; topBot++) {
                    makePixel(lcdd, sens, params, layer, topBot, pixel, widths[layer][pixel], xShift);
                }
                xShift += widths[layer][pixel] + (2 * params.reflectorDepth);// + 1;
            }
        }
        
        // Create the foam shape and define its material.
        Box bufferShape = new Box("hodo_buffer", bufferWidth,
                params.scintillatorHeight + (2 * params.reflectorDepth), bufferDepth);
        Volume bufferVolume = new Volume("hodo_buffer_vol", bufferShape, bufferMaterial);
        setVisAttributes(lcdd, getNode(), bufferVolume);
        lcdd.add(bufferShape);
        lcdd.add(bufferVolume);
        
        // Define the buffer position.
        for(int topBot = TOP; topBot <= BOTTOM; topBot++) {
            Position bufferPos = new Position("hodo_buffer" + (topBot == TOP ? 'T' : 'B') + "_pos",
                    X_SHIFT + bufferX + (bufferShape.getX() / 2),
                    (topBot == TOP ? 1 : -1) * (positionValues[LAYER1][topBot][Y] + params.reflectorDepth
                            + (params.scintillatorHeight / 2)),
                    positionValues[LAYER1][topBot][Z] + params.scintillatorDepth + (2 * params.coverDepth)
                    + (bufferDepth / 2));
            lcdd.getDefine().addPosition(bufferPos);
            new PhysVol(bufferVolume, lcdd.pickMotherVolume(this), bufferPos, PIXEL_ROTATION);
        }
        
        // BooleanSolid
        //org.lcsim.geometry.compact.converter.lcdd.util.BooleanSolid pmet = null;
        // Takes two strings - GDML object references? Does it take references to Solid, Volume, or PhysVol objects?
        //org.lcsim.geometry.compact.converter.lcdd.util.Tube test
        //		= new org.lcsim.geometry.compact.converter.lcdd.util.Tube("testTube", 0, 3, params.scintillatorHeight);
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
    private static final double getDoubleVariable(Element root, String varName) throws DataConversionException, RuntimeException {
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
                throw new RuntimeException(Hodoscope_v1.class.getSimpleName() + ": Node \""
                        + varName + "\" is missing attribute \"value\".");
            }
            
            // Otherwise, parse the value and store it.
            return valueNode.getAttribute("value").getDoubleValue();
        } else {
            return Double.NaN;
        }
    }
    
    private static final double[] getDoubleArrayVariable(Element root, String varName)
            throws IllegalArgumentException, NumberFormatException {
        // Get the value node. If it exists, attempt to access the
        // variable. Otherwise, just return null.
        Element valueNode = root.getChild(varName);
        if(valueNode != null) {
            // Attempt to obtain the variable attribute. If it does
            // not exist, there is a formatting problem with the
            // detector declaration in the compact.xml. Produce an
            // exception and alert the user.
            Attribute valueAttribute = valueNode.getAttribute("value");
            if(valueAttribute == null) {
                throw new RuntimeException(Hodoscope_v1.class.getSimpleName() + ": Node \""
                        + varName + "\" is missing attribute \"value\".");
            }
            
            // The value should take the form of a comma-delimited
            // string of doubles. First, sanitize the string to
            // remove any whitespace. Also ensure that it contains
            // only numbers, decimal points, and commas.
            int entries = 1;
            StringBuffer sanitizationBuffer = new StringBuffer();
            for(char c : valueAttribute.getValue().toCharArray()) {
                if(Character.isDigit(c) || c == ',' || Character.isWhitespace(c) || c == '.') {
                    if(c == ',') { entries++; }
                    if(!Character.isWhitespace(c)) {
                        sanitizationBuffer.append(c);
                    }
                } else {
                    throw new IllegalArgumentException(Hodoscope_v1.class.getSimpleName() + ": Numeric array variable \""
                            + varName + "\" contains unsupported character \'" + Character.toString(c) + "\'.");
                }
            }
            
            // If the input string is sanitized, prepare it for
            // parsing.
            String input = sanitizationBuffer.toString();
            
            // If the input is an empty string, return a size zero
            // array of type double.
            if(input.length() == 0) {
                return new double[0];
            }
            
            // Extract all double values from the input.
            int curIndex = 0;
            double[] values = new double[entries];
            StringBuffer valueBuffer = new StringBuffer();
            for(char c : input.toCharArray()) {
                // All digits aside from the delimiter should be
                // added to the buffer for later parsing.
                if(c != ',') {
                    valueBuffer.append(c);
                }
                
                // If the delimiter has been seen, parse the digit.
                else {
                    // If there is no content in the buffer, it is
                    // an error.
                    if(valueBuffer.length() == 0) {
                        throw new IllegalArgumentException(Hodoscope_v1.class.getSimpleName() + ": Numeric array variable \""
                                + varName + "\" is missing one or more value declarations.");
                    }
                    
                    // Otherwise, attempt to parse the digit.
                    values[curIndex] = Double.parseDouble(valueBuffer.toString());
                    
                    // Reset the buffer an increment the index.
                    valueBuffer = new StringBuffer();
                    curIndex++;
                }
            }
            
            // There should be one digit left in the buffer at the
            // end of the loop.
            if(valueBuffer.length() == 0) {
                throw new IllegalArgumentException(Hodoscope_v1.class.getSimpleName() + ": Numeric array variable \""
                        + varName + "\" is missing one or more value declarations.");
            }
            values[curIndex] = Double.parseDouble(valueBuffer.toString());
            valueBuffer = new StringBuffer();
            curIndex++;
            
            // Return the result.
            return values;
        } else {
            return null;
        }
    }
    
    /**
     * Reads the name of a material from the specified variable node
     * in the parent node <code>root</code> and attempts to obtain
     * the material definition from the LCDD file <code>lcdd</code>.
     * If it exists, the material object is returned. Otherwise, a
     * value of <code>null</code> is returned.
     * @param lcdd - The LCDD file object, in which the material data
     * definition may be found.
     * @param root - The detector XML node which serves as a parent
     * to the variable nodes.
     * @param varName - The {@link java.lang.String String} name of
     * the variables.
     * @return Returns a {@link
     * org.lcsim.geometry.compact.converter.lcdd.util.Material
     * Material} object if the variable requested exists. Otherwise,
     * <code>null</code> will be returned.
     * @throws JDOMException Occurs if there is an error accessing
     * the material definition from the LCDD file.
     * @throws RuntimeException  Occurs if the material declaration
     * attribute in the XML is missing.
     */
    private static final Material getMaterialVariable(LCDD lcdd, Element root, String varName) throws JDOMException, RuntimeException {
        // Get the value node. If it exists, attempt to access the
        // variable. Otherwise, just return null.
        Element valueNode = root.getChild(varName);
        if(valueNode != null) {
            // Attempt to obtain the variable attribute. If it does
            // not exist, there is a formatting problem with the
            // detector declaration in the compact.xml. Produce an
            // exception and alert the user.
            Attribute valueAttribute = valueNode.getAttribute("value");
            if(valueAttribute == null) {
                throw new RuntimeException(Hodoscope_v1.class.getSimpleName() + ": Node \""
                        + varName + "\" is missing attribute \"value\".");
            }
            
            // Otherwise, parse the value and store it.
            return lcdd.getMaterial(valueAttribute.getValue());
        } else {
            return null;
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
    
    private final void makePixel(LCDD lcdd, SensitiveDetector sens, PixelParameters params,
            int layer, int topBot, int ix, double pixelWidth, double xShift) {
        // Get a unique string that represents this pixel.
        String uid = getName(layer, topBot, ix);
        
        // Create the geometric shapes that define the pixel. These
        // are always rectangular prisms. The prism is then used to
        // define a volume, which sets the material. Lastly, it is
        // assigned the display properties for the hodoscope and is
        // attached to the hodoscope's sensitive detector.
        Box pixelShape = new Box("hodo_pixel_" + uid, pixelWidth, params.scintillatorHeight, params.scintillatorDepth);
        Volume pixelVolume = new Volume("hodo_vol_" + uid, pixelShape, params.scintillatorMaterial);
        pixelVolume.setSensitiveDetector(sens);
        setVisAttributes(lcdd, getNode(), pixelVolume);
        lcdd.add(pixelShape);
        lcdd.add(pixelVolume);
        
        // Create the geometric shapes that define the scintillator
        // covers. These are rectangular prisms with the same x- and
        // y-dimensions as the scintillator.
        Box coverShape = new Box("hodo_cover_" + uid, pixelWidth, params.scintillatorHeight, params.coverDepth);
        Volume coverVolume = new Volume("hodo_cover_vol_" + uid, coverShape, params.coverMaterial);
        setVisAttributes(lcdd, getNode(), coverVolume);
        lcdd.add(coverShape);
        lcdd.add(coverVolume);
        
        // Create the geometric shapes that define the scintillator
        // side reflectors. These have a depth and height equal to
        // the scintillator, but define their own width.
        Box sideReflectorShape = new Box("hodo_siderefl_" + uid, params.reflectorDepth, params.scintillatorHeight,
                params.scintillatorDepth + (2 * params.coverDepth));
        Volume sideReflectorVolume = new Volume("hodo_siderefl_vol_" + uid, sideReflectorShape, params.reflectorMaterial);
        setVisAttributes(lcdd, getNode(), sideReflectorVolume);
        lcdd.add(sideReflectorShape);
        lcdd.add(sideReflectorVolume);
        
        // Create the geometric shapes that define the scintillator
        // top reflectors. These have a depth and width equal to the
        // scintillator, but define their own height.
        Box topReflectorShape = new Box("hodo_toprefl_" + uid,
                pixelWidth + (2 * params.reflectorDepth),
                params.reflectorDepth,
                params.scintillatorDepth + (2 * params.coverDepth));
        Volume topReflectorVolume = new Volume("hodo_toprefl_vol_" + uid, topReflectorShape, params.reflectorMaterial);
        setVisAttributes(lcdd, getNode(), topReflectorVolume);
        lcdd.add(topReflectorShape);
        lcdd.add(topReflectorVolume);
        
        // Define the position of the crystal. Note that the position
        // of a volume is defined as the position of its centerpoint,
        // so it must shifted by half its width and height to account
        // for this and place its edge at the correct position. After
        // this, the position and default rotation defines should be
        // must be added.
        Position scinPos = new Position("hodo_pos_" + uid,
                X_SHIFT + xShift + positionValues[layer][topBot][X] + sideReflectorShape.getX() + (pixelShape.getX() / 2),
                (topBot == TOP ? 1 : -1) * (positionValues[layer][topBot][Y] + topReflectorShape.getY() + (pixelShape.getY() / 2)),
                positionValues[layer][topBot][Z] + coverShape.getZ() + (pixelShape.getZ() / 2));
        lcdd.getDefine().addPosition(scinPos);
        
        // Create the physical object representing the pixel. This
        // can also have a number of additional properties that can
        // be attached to it.
        PhysVol physvol = new PhysVol(pixelVolume, lcdd.pickMotherVolume(this), scinPos, PIXEL_ROTATION);
        physvol.addPhysVolID("system", getSystemID());
        physvol.addPhysVolID("ix", ix);
        physvol.addPhysVolID("iy", topBot == TOP ? 1 : -1);
        physvol.addPhysVolID("iz", layer == LAYER1 ? 1 : 2);
        
        // Define the positions of the scintillator covers. These sit
        // both in front of and behind the scintillator.
        Position[] coverPos = {
            new Position("hodo_forecover_pos_" + uid, scinPos.x(), scinPos.y(),
                    positionValues[layer][topBot][Z] + (coverShape.getZ() / 2)),
            new Position("hodo_postcover_pos_" + uid, scinPos.x(), scinPos.y(),
                    positionValues[layer][topBot][Z] + coverShape.getZ() + pixelShape.getZ() + (coverShape.getZ() / 2))
        };
        
        // Create the physical cover objects.
        for(Position pos : coverPos) {
            lcdd.getDefine().addPosition(pos);
            new PhysVol(coverVolume, lcdd.pickMotherVolume(this), pos, PIXEL_ROTATION);
        }
        
        // Define the positions of the scintillator side reflectors.
        // These sit on either side of a scintillator crystal.
        Position[] sideReflectorPos = {
                new Position("hodo_sidereflR_pos_" + uid,
                        X_SHIFT + xShift + positionValues[layer][topBot][X] + (sideReflectorShape.getX() / 2),
                        scinPos.y(), scinPos.z()),
                new Position("hodo_siderefl2L_pos_" + uid,
                        X_SHIFT + xShift + positionValues[layer][topBot][X] + sideReflectorShape.getX()
                        + pixelShape.getX() + (sideReflectorShape.getX() / 2),
                        scinPos.y(), scinPos.z())
        };
        
        // Create the physical side reflector objects.
        for(Position pos : sideReflectorPos) {
            lcdd.getDefine().addPosition(pos);
            new PhysVol(sideReflectorVolume, lcdd.pickMotherVolume(this), pos, PIXEL_ROTATION);
        }
        
        // Define the positions of the scintillator top reflectors.
        // These sit on the top and bottom of a scintillator crystal.
        Position[] topReflectorPos = {
                new Position("hodo_topreflT_pos_" + uid,
                        scinPos.x(),
                        scinPos.y() + (params.scintillatorHeight / 2) + (params.reflectorDepth / 2),
                        scinPos.z()),
                new Position("hodo_toprefl2B_pos_" + uid,
                        scinPos.x(),
                        scinPos.y() - (params.scintillatorHeight / 2) - (params.reflectorDepth / 2),
                        scinPos.z())
        };
        
        // Create the physical top reflector objects.
        for(Position pos : topReflectorPos) {
            lcdd.getDefine().addPosition(pos);
            new PhysVol(topReflectorVolume, lcdd.pickMotherVolume(this), pos, PIXEL_ROTATION);
        }
    }
    
    public boolean isCalorimeter() {
        return true;
    }
    
    private class PixelParameters {
        public double coverDepth = 0;
        public double reflectorDepth = 0;
        public double scintillatorDepth = 0;
        public double scintillatorHeight = 0;
        
        public Material coverMaterial = null;
        public Material reflectorMaterial = null;
        public Material scintillatorMaterial = null;
    }
}