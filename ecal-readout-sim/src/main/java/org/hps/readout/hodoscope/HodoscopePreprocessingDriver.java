package org.hps.readout.hodoscope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.conditions.hodoscope.HodoscopeChannel;
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

/**
 * <code>HodoscopeEnergySplitDriver</code> handles the preprocessing
 * of hodoscope hits from SLIC. It is responsible for converting the
 * hits from {@link org.lcsim.event.SimTrackerHit SimTrackerHit}
 * objects into {@link org.lcsim.event.SimCalorimeterHit
 * SimCalorimeterHit} objects. It is also responsible for converting
 * hits from scintillator-based channels to those that match the FADC
 * channels in the hardware. This entails splitting the energy of
 * those hits where the physical scintillator feeds into multiple
 * FADC channels.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class HodoscopePreprocessingDriver extends ReadoutDriver {
    /** Object for accessing the geometric data of the hodoscope detector model. */
    private HodoscopeDetectorElement hodoscopeDetectorElement;
    /** The name of the SLiC truth hit collection. */
    private String truthHitCollectionName = "HodoscopeHits";
    /** Name of the output preprocessed hit collection. */
    private String outputHitCollectionName = "HodoscopePreprocessedHits";
    
    @Override
    public void detectorChanged(Detector detector) {
        // Update the hodoscope detector object.
        hodoscopeDetectorElement = (HodoscopeDetectorElement) detector.getSubdetector("Hodoscope").getDetectorElement();
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
                fadcChannelIDs[getHolePositionArrayIndex(fadcChannelIDList.get(0))] = fadcChannelIDList.get(0).getChannelId();
                fadcChannelIDs[getHolePositionArrayIndex(fadcChannelIDList.get(1))] = fadcChannelIDList.get(1).getChannelId();
                
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
    
    /**
     * Sets the name of the hodoscope output hit collection.
     * @param collection - The name of the collection containing the
     * preprocessed hodoscope hits.
     */
    public void setOutputHitCollectionName(String collection) {
        outputHitCollectionName = collection;
    }
    
    /**
     * Sets the name of the hodoscope hits collection.
     * @param collection - The name of the collection containing the
     * hodoscope hits.
     */
    public void setTruthHitCollectionName(String collection) {
        truthHitCollectionName = collection;
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
     * Translates a hole index into a form usable in an array.
     * @param channel - A hodoscope channel.
     * @return Returns a value of <code>0</code> for the hole closest
     * to the calorimeter center and a value of <code>1</code> for
     * hole closest to the positron-side of the calorimeter.
     * @throws IllegalArgumentException Occurs if the input channel
     * either has an unrecognized hole number, or only has one hole.
     */
    private static final int getHolePositionArrayIndex(HodoscopeChannel channel) throws IllegalArgumentException {
        if(channel.getHole().intValue() == HodoscopeChannel.HOLE_LOW_X) { return 0; }
        else if(channel.getHole().intValue() == HodoscopeChannel.HOLE_HIGH_X) { return 1; }
        else { throw new IllegalArgumentException("Unexpected hole number \"" + channel.getHole().intValue() + "\"!"); }
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