package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseSimCalorimeterHit;
import org.lcsim.geometry.Detector;

public class HodoscopeEnergySplitDriver extends ReadoutDriver {
    private HodoscopeDetectorElement hodoscopeDetectorElement;
    private String truthHitCollectionName = "HodoscopeHits";
    private String outputHitCollectionName = "HodoscopePreprocessedHits";
    private Map<Integer, List<HodoscopeChannel>> scintillatorPositionToChannelMap = null;
    
    //private Map<Integer, String> hodoscopeBoundsMap = new HashMap<Integer, String>();

    private DatabaseConditionsManager conditionsManager = null;
    
    @Override
    public void detectorChanged(Detector detector) {
        
        conditionsManager = DatabaseConditionsManager.getInstance();
        
        // Update the hodoscope detector object.
        hodoscopeDetectorElement = (HodoscopeDetectorElement) detector.getSubdetector("Hodoscope").getDetectorElement();
        
        // Populate the scintillator channel map for the new detector.
        hodoscopeDetectorElement.populateScintillatorChannelMap(
                conditionsManager.getCachedConditions(HodoscopeChannelCollection.class, 
                        "hodo_channels").getCachedData());
        
        scintillatorPositionToChannelMap = hodoscopeDetectorElement.getScintillatorPositionToChannelMap();
    }
    
    @Override
    public void startOfData() {
        // Define the LCSim collection parameters for this driver's
        // output.
        LCIOCollectionFactory.setCollectionName(outputHitCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(0xe0000000);
        LCIOCollectionFactory.setReadoutName("HodoscopeHits");
        LCIOCollection<SimCalorimeterHit> hitCollectionParams = LCIOCollectionFactory.produceLCIOCollection(SimCalorimeterHit.class);
        ReadoutDataManager.registerCollection(hitCollectionParams, false);
    }
    
    @Override
    public void endOfData() {
        /*
        List<String> boundsData = new ArrayList<String>(hodoscopeBoundsMap.size());
        boundsData.addAll(hodoscopeBoundsMap.values());
        Collections.sort(boundsData);
        for(String entry : boundsData) {
            System.out.println(entry);
        }
        */
    }
    
    @Override
    public void process(EventHeader event) {
        // Get the hodoscope hits from the data manager.
        Collection<SimTrackerHit> hodoscopeHits = ReadoutDataManager.getData(ReadoutDataManager.getCurrentTime(), ReadoutDataManager.getCurrentTime() + 2.0,
                truthHitCollectionName, SimTrackerHit.class);
        
        // Create a list to store the output hits in.
        List<SimCalorimeterHit> outputHits = new ArrayList<SimCalorimeterHit>();
        
        // Iterate over the hodoscope hits and create new calorimeter
        // hit objects from them. For hodoscope scintillators with
        // multiple FADC channels, split the energy between them.
        for(SimTrackerHit hit : hodoscopeHits) {
            // DEBUG :: Store the scintillator bounds for testing the
            //          detector positioning.
            int[] hodoIndices = hodoscopeDetectorElement.getHodoscopeIndices(hit.getIdentifier());
            Integer posvar = Integer.valueOf(HodoscopeDetectorElement.getHodoscopePositionVar(hodoIndices[0], hodoIndices[1], hodoIndices[2]));
            /*
            if(!hodoscopeBoundsMap.containsKey(posvar)) {
                hodoscopeBoundsMap.put(posvar, getHodoscopeBounds(hit));
            }
            */
            
            // DEBUG
            System.out.printf("Getting channel data for <%d, %2d, %d> :: ", hodoIndices[0], hodoIndices[1], hodoIndices[2]);
            if(!scintillatorPositionToChannelMap.containsKey(posvar)) {
                System.out.println("Error: No match to key \"" + Integer.toBinaryString(posvar.intValue()) + "\".");
            } else if(scintillatorPositionToChannelMap.get(posvar).size() == 0) {
                System.out.println("Error: Key \"" + Integer.toBinaryString(posvar.intValue()) + "\" has no channel data entries.");
            } else {
                System.out.println(scintillatorPositionToChannelMap.get(posvar).size());
            }
            
            // Get the number of FADC channels that are associated
            // with this scintillator.
            int fadcChannels = hodoscopeDetectorElement.getScintillatorChannelCount(hit.getIdentifier());
            
            // If there is only one hit, then create a calorimeter
            // hit object that contains the full energy of the truth
            // hit. There is no need to split it.
            if(fadcChannels == 1) {
                // Get the hardware channel for this scintillator.
                int cellID = scintillatorPositionToChannelMap.get(posvar).get(0).getChannelId().intValue();
                
                // Convert the hit to a calorimeter hit with the new
                // hardware channel ID and store it.
                outputHits.add(makeHit(hit, cellID));
            }
            
            // If there are two channels, the energy must be split
            // between them.
            else {
                // Get the positions of the two fiber bundle holes.
                double[] holePos = hodoscopeDetectorElement.getScintillatorHolePositions(hit.getIdentifier());
                
                // Get the hardware channel IDs for the FADC channels
                // associated with each fiber bundle hole.
                int[] fadcChannelIDs = new int[2];
                List<HodoscopeChannel> fadcChannelIDList = scintillatorPositionToChannelMap.get(posvar);
                
                System.out.printf("Channel <%d, %2d, %d>", fadcChannelIDList.get(0).getX(), fadcChannelIDList.get(0).getY(), fadcChannelIDList.get(0).getLayer());
                System.out.println("Entry 0 Hole Number :: " + fadcChannelIDList.get(0).getHole().toString());
                System.out.println("Entry 1 Hole Number :: " + fadcChannelIDList.get(0).getHole().toString());
                
                fadcChannelIDs[fadcChannelIDList.get(0).getHole()] = fadcChannelIDList.get(0).getChannelId();
                fadcChannelIDs[fadcChannelIDList.get(1).getHole()] = fadcChannelIDList.get(1).getChannelId();
                
                // Get the x-position of the hit.
                double hitXPos = hit.getPosition()[0];
                
                // If the hit exists entirely before the first fiber
                // bundle hole, all its energy goes to that channel.
                // Likewise, if the hit exists after the second hole,
                // all of its energy goes to the second channel.
                if(hitXPos < holePos[0]) {
                    outputHits.add(makeHit(hit, fadcChannelIDs[0]));
                } else if(hitXPos > holePos[1]) {
                    outputHits.add(makeHit(hit, fadcChannelIDs[1]));
                }
                
                // Otherwise, the energy must be split linearly based
                // on its distance from each hole.
                else {
                    // Calculate the distance between each fiber
                    // bundle hole and the hit. Only the horizontal
                    // distance matters.
                    double la = Math.abs(holePos[0] - hitXPos);
                    double lb = Math.abs(holePos[1] - hitXPos);
                    
                    // Calculate the percentage of the hit's energy
                    // that should go to each channel. This should be
                    // linearly proportional to the x-displacement of
                    // the hit from that channel's fiber bundle hole.
                    double ra = lb / (la + lb);
                    double rb = la / (la + lb);
                    
                    // Create two new hits.
                    outputHits.add(makeHit(hit, fadcChannelIDs[0], ra));
                    outputHits.add(makeHit(hit, fadcChannelIDs[1], rb));
                }
            }
        }
        
        // Output the preprocessed hits to the data manager.
        ReadoutDataManager.addData(outputHitCollectionName, outputHits, SimCalorimeterHit.class);
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }

    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
    
    /**
     * Sets the name of the hodoscope hits collection.
     * @param collection - The name of the collection containing the
     * hodoscope hits.
     */
    public void setTruthHitCollectionName(String collection) {
        truthHitCollectionName = collection;
    }
             
    /**
     * Creates a {@link org.lcsim.event.SimCalorimeterHit
     * SimCalorimeterHit} object from an input {@link
     * org.lcsim.event.SimTrackerHit SimTrackerHit} object. The new
     * hit will have the same truth information, but will have the
     * cell ID specified instead.
     * @param hit - The hit to convert.
     * @param cellID - The new cell ID for the hit.
     * @return Returns a new <code>SimCalorimeterHit</code> object
     * that represents the same data as the input
     * <code>RawTrackerHit</code> object.
     */
    private static final SimCalorimeterHit makeHit(SimTrackerHit hit, int cellID) {
        return makeHit(hit, cellID, 1.0);
    }
    
    /**
     * Creates a {@link org.lcsim.event.SimCalorimeterHit
     * SimCalorimeterHit} object from an input {@link
     * org.lcsim.event.SimTrackerHit SimTrackerHit} object. The new
     * hit will have the same truth information, but will have the
     * cell ID specified and also its energy will be scaled by the
     * amount indicated by <code>energyScale</code>.
     * @param hit - The hit to convert.
     * @param cellID - The new cell ID for the hit.
     * @param energyScale - The amount by which the energy should be
     * scaled.
     * @return Returns a new <code>SimCalorimeterHit</code> object
     * that represents the same data as the input
     * <code>RawTrackerHit</code> object, but with the energy scaled
     * as specified.
     */
    private static final SimCalorimeterHit makeHit(SimTrackerHit hit, int cellID, double energyScale) {
        // Create the necessary data objects to clone the hit. Note
        // that SimTrackerHit objects only ever have a single truth
        // particle associated with them.
        int[] pdgs = new int[] { hit.getMCParticle().getPDGID() };
        float[] times = new float[] {  (float) hit.getTime() };
        float[] energies = new float[] { (float) (energyScale * hit.getdEdx()) };
        Object[] particles = new Object[] { hit.getMCParticle() };
        
        // Create the calorimeter hit from tracker hit data.
        return new BaseSimCalorimeterHit(cellID, (energyScale * hit.getdEdx()), hit.getTime(),
                particles, energies, times, pdgs, hit.getMetaData());
    }
}