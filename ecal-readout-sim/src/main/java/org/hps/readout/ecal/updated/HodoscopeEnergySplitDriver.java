package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private DatabaseConditionsManager conditionsManager = null;
    
    @Override
    public void detectorChanged(Detector detector) {
        // Get the an instance of the conditions database.
        conditionsManager = DatabaseConditionsManager.getInstance();
        
        // Update the hodoscope detector object.
        hodoscopeDetectorElement = (HodoscopeDetectorElement) detector.getSubdetector("Hodoscope").getDetectorElement();
        
        // Populate the scintillator channel map for the new detector.
        hodoscopeDetectorElement.updateScintillatorChannelMap(conditionsManager.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData());
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
            // Get the number of FADC channels that are associated
            // with this scintillator.
            int fadcChannels = hodoscopeDetectorElement.getScintillatorChannelCount(hit);
            
            // Get the SLIC-to-hardware mapping key.
            
            // If there is only one hit, then create a calorimeter
            // hit object that contains the full energy of the truth
            // hit. There is no need to split it.
            if(fadcChannels == 1) {
                // Get the hardware channel for this scintillator.
                int cellID = hodoscopeDetectorElement.getHardwareChannels(hit).get(0).getChannelId().intValue();
                
                // Convert the hit to a calorimeter hit with the new
                // hardware channel ID and store it.
                outputHits.add(makeHit(hit, cellID));
            }
            
            // If there are two channels, the energy must be split
            // between them.
            else {
                // Get the positions of the two fiber bundle holes.
                double[] holePos = hodoscopeDetectorElement.getScintillatorHolePositions(hit);
                
                // Get the hardware channel IDs for the FADC channels
                // associated with each fiber bundle hole.
                int[] fadcChannelIDs = new int[2];
                List<HodoscopeChannel> fadcChannelIDList = hodoscopeDetectorElement.getHardwareChannels(hit);
                fadcChannelIDs[fadcChannelIDList.get(0).getHole()] = fadcChannelIDList.get(0).getChannelId();
                fadcChannelIDs[fadcChannelIDList.get(1).getHole()] = fadcChannelIDList.get(1).getChannelId();
                
                System.out.printf("Channel <%d, %2d, %d>%n", fadcChannelIDList.get(0).getX(), fadcChannelIDList.get(0).getY(), fadcChannelIDList.get(0).getLayer());
                System.out.println("\tHit Energy     :: " + hit.getdEdx());
                System.out.println("\tHole 0 Channel :: " + fadcChannelIDs[0]);
                System.out.println("\tHole 1 Channel :: " + fadcChannelIDs[1]);
                
                // Get the x-position of the hit.
                double hitXPos = hit.getPosition()[0];
                
                // If the hit exists entirely before the first fiber
                // bundle hole, all its energy goes to that channel.
                // Likewise, if the hit exists after the second hole,
                // all of its energy goes to the second channel.
                if(hitXPos < holePos[0]) {
                    outputHits.add(makeHit(hit, fadcChannelIDs[0]));
                    System.out.println("\tChannel Energy :: " + hit.getdEdx() + " --> " + fadcChannelIDs[0]);
                } else if(hitXPos > holePos[1]) {
                    outputHits.add(makeHit(hit, fadcChannelIDs[1]));
                    System.out.println("\tChannel Energy :: " + hit.getdEdx() + " --> " + fadcChannelIDs[1]);
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
                    System.out.println("\tChannel Energy :: " + (ra * hit.getdEdx()) + " --> " + fadcChannelIDs[0]);
                    System.out.println("\tChannel Energy :: " + (rb * hit.getdEdx()) + " --> " + fadcChannelIDs[1]);
                }
            }
        }
        
        // Output the preprocessed hits to the data manager.
        ReadoutDataManager.addData(outputHitCollectionName, outputHits, SimCalorimeterHit.class);
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