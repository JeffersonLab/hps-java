package org.hps.conditions.deprecated;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.geometry.Detector;

import org.hps.util.Pair;

import static org.hps.conditions.deprecated.StereoPair.detectorVolume;

/**
 * A class providing various utilities related to the HPS SVT. 
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: SvtUtils.java,v 1.19 2013/11/06 20:25:37 omoreno Exp $
 */
public class SvtUtils {

    private static SvtUtils INSTANCE = null;
    // Set of sensors
    private Set<SiSensor> sensors = new HashSet<SiSensor>();
    // Set of stereo pairs
    private Set<StereoPair> stereoPairs = new HashSet<StereoPair>(); 
    // Map from Sensor to Hybrid/FPGA pair
    private Map<SiSensor /* sensor */, Pair<Integer /* FPGA */, Integer /* Hybrid */>> sensorToDaqPair = new HashMap<SiSensor, Pair<Integer, Integer>>();
    // Map from Hybrid/FPGA pair
    private Map<Pair<Integer /* FPGA */, Integer /* Hybrid */>, SiSensor /* Sensor*/> daqPairToSensor = new HashMap<Pair<Integer, Integer>, SiSensor>();
    // Map from SVT top layer to Hybrid/FPGA pair
    private Map<Integer /* Layer */, List<Pair<Integer /* FPGA */, Integer /* Hybrid */>>> topLayerDaqMap = new HashMap<Integer, List<Pair<Integer, Integer>>>();
    // Map from SVT bottom layer to Hybrid/FPGA pair
    private Map<Integer /* Layer # */, List<Pair<Integer /* FPGA */, Integer /* Hybrid */>>> bottomLayerDaqMap = new HashMap<Integer, List<Pair<Integer, Integer>>>();
    // Map sensor to an SVT top layer
    private Map<SiSensor /* Sensor */, Integer /* Layer */> sensorToTopLayer = new HashMap<SiSensor, Integer>();
    // Map sensor to an SVT bottom layer
    private Map<SiSensor /* Sensor */, Integer /* Layer */> sensorToBottomLayer = new HashMap<SiSensor, Integer>();
    // Map sensor to descriptor
    private Map<SiSensor /* Sensor */, String /* Description */> sensorToDescriptor = new HashMap<SiSensor, String>();
    // Map layer to top SVT sensor 
    private Map<Integer /* Layer */, List<SiSensor> /* Sensor */> topLayerToSensor = new HashMap<Integer, List<SiSensor>>();
    // Map layer to bottom SVT sensor
    private Map<Integer /* Layer */, List<SiSensor> /* Sensor */> bottomLayerToSensor = new HashMap<Integer, List<SiSensor>>();
    private SiSensor[][] sensorArray;
    private IIdentifierHelper helper;
    String subdetectorName = "Tracker";
    ConditionsManager manager = ConditionsManager.defaultInstance();
    int maxModuleNumber = 0;
    int maxLayerNumber = 0;
    private Set<Integer> fpgaNumbers = new HashSet<Integer>();
    private boolean isSetup = false;
    boolean debug = false;

    /*
     * Private ctor to keep user from instantiating 
     */
    private SvtUtils() {
    }

    /**
     * 
     */
    public static SvtUtils getInstance() {

        // Use lazy instantiation
        if (INSTANCE == null) {
            INSTANCE = new SvtUtils();
        }
        return INSTANCE;
    }

    /**
     * @return true if all maps have been loaded, false otherwise
     */
    public boolean isSetup() {
        return isSetup;
    }

    /**
     * Get the plane number to which the sensor belongs to. This is does not 
     * return the SVT layer number.
     * 
     * @param sensor
     * @return The layer number
     */
    // TODO: Change the name to clarify that what is being returned is the plane number
    public int getLayerNumber(SiSensor sensor) {
        if (sensorToTopLayer.containsKey(sensor)) {
            return sensorToTopLayer.get(sensor);
        } else if (sensorToBottomLayer.containsKey(sensor)) {
            return sensorToBottomLayer.get(sensor);
        } else {
            throw new RuntimeException("The sensor " + sensor.getName() 
            								+ " does not have an associated plane number");
        }
    }

    /**
     * Checks if a sensor is part of the top SVT volume.
     *
     * @return true if it is, false if it belongs to the bottom volume
     */
    public boolean isTopLayer(SiSensor sensor) {
        if (sensorToTopLayer.containsKey(sensor)) {
            return true;
        } else if (sensorToBottomLayer.containsKey(sensor)) {
            return false;
        } else {
            throw new RuntimeException("There is no layer associated with sensor " + sensor.getName());
        }
    }

    /**
     * Checks if the orientation of the sensor is axial.
     * 
     *  @return true if it is, false if it is a stereo sensor
     */
    public boolean isAxial(SiSensor sensor) {
        if (this.isTopLayer(sensor) && this.getLayerNumber(sensor) % 2 == 1) {
            return true;
        } else if (!this.isTopLayer(sensor) && this.getLayerNumber(sensor) % 2 == 0) {
            return true;
        }
        return false;
    }

    /**
     * Get a sensor by its plane and module id. Both are zero-indexed.
     * 
     *  @param moduleNumber : module id
     *  @param planeNumber : plane id (typically 1 less than the layer id)
     *  @return sensor 
     */
    public SiSensor getSensor(int moduleNumber, int planeNumber) {
        if (moduleNumber < 0 || moduleNumber > maxModuleNumber) {
            throw new RuntimeException("Module number " + moduleNumber + " is out of range!");
        } else if (planeNumber < 0 || planeNumber >= maxLayerNumber) {
            throw new RuntimeException("Plane number " + planeNumber + " is out of range!");
        }
        return sensorArray[moduleNumber][planeNumber];
    }

    /**
     * Get a sensor by its FPGA and hybrid number.
     * 
     *  @param daqPair : a Pair of the form Pair<FPGA number, hybrid number>
     *  @return sensor
     */
    public SiSensor getSensor(Pair<Integer, Integer> daqPair) {
        return daqPairToSensor.get(daqPair);
    }

    /**
     * Get a sensor from the top SVT volume by its plane number and sensor 
     * position within a plane.
     * 
     *  @param planeNumber
     *  @param sensorNumber : position within a plane
     *  @return sensor or null if the sensor doesn't exist in that position
     */
    public SiSensor getTopSensor(int planeNumber, int sensorNumber) {
    	if(topLayerToSensor.get(planeNumber).size() <= sensorNumber) return null;
        return topLayerToSensor.get(planeNumber).get(sensorNumber);
    }

    /**
     * Get a sensor from the bottom SVT volume by its plane number and sensor 
     * position within a plane.
     * 
     *  @param planeNumber
     *  @param sensorNumber : position within a plane
     *  @return sensor or null if the sensor doesn't exist in that position
     */
    public SiSensor getBottomSensor(int layer, int sensorNumber) {
    	if(bottomLayerToSensor.get(layer).size() <= sensorNumber) return null;
        return bottomLayerToSensor.get(layer).get(sensorNumber);
    }

    /**
     * Get the Set of all sensors which compose the SVT.
     * 
     *  @return set of sensors
     */
    public Set<SiSensor> getSensors() {
        return sensors;
    }

    /**
     * Get the Set of all FPGA numbers. 
     * 
     * @return set of FPGA numbers
     */
    public Set<Integer> getFpgaNumbers() {
        return fpgaNumbers;
    }

    /**
     * Get the FPGA number associated with a sensor.
     * 
     * @return FPGA number
     */
    public int getFPGA(SiSensor sensor) {
        return sensorToDaqPair.get(sensor).getFirstElement();
    }

    /**
     * Get the hybrid number associated with a sensor.
     * 
     * @return hybrid number
     */
    public int getHybrid(SiSensor sensor) {
        return sensorToDaqPair.get(sensor).getSecondElement();
    }

    public int getFPGACount() {
        return 0;
    }

    /**
     * Get the FPGA and hybrid number associated with a sensor as a pair of
     * values.
     * 
     * @param sensor
     * @return pair of the form Pair<FPGA, hybrid>
     */
    public Pair<Integer, Integer> getDaqPair(SiSensor sensor) {
        return sensorToDaqPair.get(sensor);
    }

    /**
     * Get the IIdentifierHelper associated with the SVT.
     * 
     * @return helper
     */
    public IIdentifierHelper getHelper() {
        return helper;
    }

    /**
     * Get a string describing a sensor.
     * 
     * @param sensor
     * @return String describing a sensor
     */
    public String getDescription(SiSensor sensor) {
        return this.sensorToDescriptor.get(sensor);
    }

    /**
     * Make the cell ID for any channel on a sensor.
     * 
     * @param sensor
     * @param channel : Physical channel number
     * @return cell ID
     */
    public static long makeCellID(SiSensor sensor, int channel) {
        int sideNumber;
        if (sensor.hasElectrodesOnSide(ChargeCarrier.HOLE)) {
            sideNumber = ChargeCarrier.HOLE.charge();
        } else {
            sideNumber = ChargeCarrier.ELECTRON.charge();
        }
        return sensor.makeStripId(channel, sideNumber).getValue();
    }
    
    /**
     * Get the set of all stereo pairs which compose the SVT.
     * 
     *  @return Set of stereo pairs
     */
    public Set<StereoPair> getStereoPairs(){
    	return stereoPairs; 
    }

    /**
     * Creates and fill the various utility maps.
     * 
     * @param detector : The SVT detector being used
     */
    public void setup(Detector detector) {

        this.printDebug("Method: setupMaps: \n\tDetector: " + detector.getDetectorName());
        
        // Load the DAQ Map from the conditions database
        this.loadSvtDaqMap();
        
        // Instantiate 'sensorArray' which allows for retrieval of a SiSensor
        // by module and layer id
        sensorArray = new SiSensor[maxModuleNumber + 1][maxLayerNumber];

        // Get all of the sensors composing the SVT and add them to the set of all sensors
        IDetectorElement detectorElement = detector.getDetectorElement().findDetectorElement(subdetectorName);
        helper = detectorElement.getIdentifierHelper();
        sensors.addAll(detectorElement.findDescendants(SiSensor.class));
        this.printDebug("\tTotal number of sensors: " + sensors.size());

        IIdentifier sensorIdent;
        SiTrackerIdentifierHelper sensorHelper;
        String description;
        // Loop through all of the sensors and fill the maps
        for (SiSensor sensor : sensors) {

            // Get the sensor identifier
            sensorIdent = sensor.getIdentifier();

            // Get the sensor identifier helper in order to decode the id fields
            sensorHelper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();

            // Get the sensor layer and module id
            int layerNumber = sensorHelper.getLayerValue(sensorIdent);
            int moduleNumber = sensorHelper.getModuleValue(sensorIdent);
            sensorArray[moduleNumber][layerNumber - 1] = sensor;
            int listPosition = 0;
            switch (moduleNumber % 2) {
                // Top Layer
            	case 0:
                	
                    listPosition = moduleNumber / 2;
                    sensorToTopLayer.put(sensor, layerNumber);
                    sensorToDaqPair.put( sensor, topLayerDaqMap.get(layerNumber).get(listPosition));
                    topLayerToSensor.get(layerNumber).set(listPosition, sensor);
                    daqPairToSensor.put(topLayerDaqMap.get(layerNumber).get(listPosition), sensor);
                    description = "Top Layer " + layerNumber + " - Sensor " + listPosition;
                    this.printDebug("\tDescription: " + description);
                    sensorToDescriptor.put(sensor, description);
                    break;
                // Bottom Layer
                case 1:
                
                	listPosition = (moduleNumber - 1) / 2;
                    sensorToBottomLayer.put(sensor, layerNumber);
                    sensorToDaqPair.put(sensor, bottomLayerDaqMap.get(layerNumber).get(listPosition));
                    bottomLayerToSensor.get(layerNumber).set(listPosition, sensor);
                    daqPairToSensor.put(bottomLayerDaqMap.get(layerNumber).get(listPosition), sensor);
                    description = "Bottom Layer " + layerNumber + " - Sensor " + listPosition;
                    this.printDebug("\tDescription: " + description);
                    sensorToDescriptor.put(sensor, description);
                    break;
                default:
                    throw new RuntimeException("Invalid module number: " + moduleNumber);
            }
        }
        
        // Create stereo pairs
        this.createStereoPairs();
        
        isSetup = true;
    }

    /**
     * Load the SVT DAQ Map from the conditions database
     * 
     */
    private void loadSvtDaqMap(){ 
    	
    	// Path to file in conditions database
        String filePath = "daqmap/svt_default.daqmap";
        BufferedReader daqMapReader = null; 
        String currentLine = null;
        try {
            daqMapReader = new BufferedReader(manager.getRawConditions(filePath).getReader());
          
            // Continue to read lines for the DAQ map until the end of file is 
            // reached. The DAQ map is has the following format
            //
            //  layer   module   fpga   hybrid
            // ---------------------------------
            while ((currentLine = daqMapReader.readLine()) != null) {
                
            	// If the line is a comment,skip it
                if (currentLine.indexOf("#") != -1) continue;
               
                // Split the line into tokens by whitespace
                StringTokenizer stringTok = new StringTokenizer(currentLine);
                int listPosition = 0;
                while (stringTok.hasMoreTokens()) {
                	
                	// Parse the line
                    int layer = Integer.valueOf(stringTok.nextToken());
                    int module = Integer.valueOf(stringTok.nextToken());
                    int fpga = Integer.valueOf(stringTok.nextToken());
                    int hybrid = Integer.valueOf(stringTok.nextToken());
                    
                    // Find the maximum layer number and module number.  This
                    // is used when instantiating 'sensorArray' and when creating
                    // stereo pairs.
                    maxModuleNumber = Math.max(maxModuleNumber, module);
                    maxLayerNumber = Math.max(maxLayerNumber, layer);
                   
                    // A pair is used to relate an FPGA value to a hybrid number
                    Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(fpga, hybrid);
                    fpgaNumbers.add(fpga);
                    
                    // Load the maps based on module number.
                    //
                    // Module 0 & 2 correspond to sensors which lie in the top SVT volume
                    // Module 1 & 3 correspond to sensors which lie in the bottom SVT volume
                    //
                    // A module number > 1 indicate that the sensor is part of a double layer.
                    // For the test run detector, all module numbers are either 0 or 1.
                    switch (module % 2) {
                        case 0:
                            listPosition = module / 2;
                            // If the top layer DAQ map doesn't contain the layer, add it
                            if (!topLayerDaqMap.containsKey(layer)) {
                                topLayerDaqMap.put(layer, new ArrayList<Pair<Integer, Integer>>());
                            }
                            this.printDebug("\tAdding FPGA: " + daqPair.getFirstElement() + ", Hybrid: " + daqPair.getSecondElement() + " to position: " + listPosition);
                            // Add the DAQ pair to the corresponding layer
                            topLayerDaqMap.get(layer).add(listPosition, daqPair);
                            
                            // If the top layer sensor map doesn't contain the layer, add it
                            if (!topLayerToSensor.containsKey(layer)) {
                                topLayerToSensor.put(layer, new ArrayList<SiSensor>());
                            }
                            // Create room within the sensor map for the sensor
                            topLayerToSensor.get(layer).add(null);
                            break;
                        case 1:
                            listPosition = (module - 1) / 2;
                            if (!bottomLayerDaqMap.containsKey(layer)) {
                                bottomLayerDaqMap.put(layer, new ArrayList<Pair<Integer, Integer>>());
                            }
                            this.printDebug("\tAdding FPGA: " + daqPair.getFirstElement() + ", Hybrid: " + daqPair.getSecondElement() + " to position: " + listPosition);
                            bottomLayerDaqMap.get(layer).add(listPosition, daqPair);
                            if (!bottomLayerToSensor.containsKey(layer)) {
                                bottomLayerToSensor.put(layer, new ArrayList<SiSensor>());
                            }
                            bottomLayerToSensor.get(layer).add(null);
                            break;
                        default:
                            throw new RuntimeException("Invalid module number: " + module);
                    }
                }
            }
            
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load DAQ Map from " + filePath, exception);
        }
    }
    
    /**
     * 
     */
    private void createStereoPairs(){
    	
    	// Loop through all the sensors in both top and bottom layers and
    	// create stereo pairs. 
    	SiSensor firstSensor = null;
    	SiSensor secondSensor = null;
    	int trackerLayer = 0; 
    	
    	// 
    	for(int layerNumber = 1; layerNumber <= maxLayerNumber; layerNumber+=2 ){
    		for(int sensorNumber = 0; sensorNumber < (maxModuleNumber+1)/2; sensorNumber++){
    			
    			firstSensor = this.getTopSensor(layerNumber, sensorNumber);
    			secondSensor = this.getTopSensor(layerNumber+1, sensorNumber);
    			
    			if(firstSensor == null || secondSensor == null) continue;
    			
    			trackerLayer = (layerNumber + 1)/2; 
    			if(this.isAxial(firstSensor)){
    				stereoPairs.add(new StereoPair(trackerLayer, detectorVolume.Top,  firstSensor, secondSensor));
    			} else {
    				stereoPairs.add(new StereoPair(trackerLayer, detectorVolume.Top, secondSensor, firstSensor));
    			}
    			
    			firstSensor = this.getBottomSensor(layerNumber, sensorNumber);
    			secondSensor = this.getBottomSensor(layerNumber+1, sensorNumber);
    			
    			if(firstSensor == null || secondSensor == null) continue;
    			
    			if(this.isAxial(firstSensor)){
    				stereoPairs.add(new StereoPair(trackerLayer, detectorVolume.Bottom, firstSensor, secondSensor));
    			} else {
    				stereoPairs.add(new StereoPair(trackerLayer, detectorVolume.Bottom, secondSensor, firstSensor));
    			}
    		}
    	}
    	
    	this.printDebug("Total number of stereo pairs created: " + stereoPairs.size());
    	for(StereoPair stereoPair : stereoPairs){
    		this.printDebug(stereoPair.toString());
    	}
    }

    /**
     * Print a debug message 
     * 
     * @param debugMessage : message to be printed
     */
    private void printDebug(String debugMessage) {
        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": " + debugMessage);
        }
    }
    
    public void reset() {
        sensors = new HashSet<SiSensor>();
        stereoPairs = new HashSet<StereoPair>(); 
        sensorToDaqPair = new HashMap<SiSensor, Pair<Integer, Integer>>();
        daqPairToSensor = new HashMap<Pair<Integer, Integer>, SiSensor>();
        topLayerDaqMap = new HashMap<Integer, List<Pair<Integer, Integer>>>();
        bottomLayerDaqMap = new HashMap<Integer, List<Pair<Integer, Integer>>>();
        sensorToTopLayer = new HashMap<SiSensor, Integer>();
        sensorToBottomLayer = new HashMap<SiSensor, Integer>();
        sensorToDescriptor = new HashMap<SiSensor, String>();
        topLayerToSensor = new HashMap<Integer, List<SiSensor>>();
        bottomLayerToSensor = new HashMap<Integer, List<SiSensor>>();
        sensorArray= null;
        fpgaNumbers = new HashSet<Integer>();
    }
}
