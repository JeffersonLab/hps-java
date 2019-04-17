package org.hps.analysis.hodoscope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

public class HodoscopeBoundsDebugDriver extends Driver {
    private String inputCollectionName = "HodoscopeHits";
    private HodoscopeDetectorElement hodoscopeDetectorElement;
    private DatabaseConditionsManager conditionsManager = null;
    private Map<Integer, String> hodoscopeBoundsMap = new HashMap<Integer, String>();
    
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
    public void endOfData() {
        List<String> boundsData = new ArrayList<String>(hodoscopeBoundsMap.size());
        boundsData.addAll(hodoscopeBoundsMap.values());
        Collections.sort(boundsData);
        for(String entry : boundsData) {
            System.out.println(entry);
        }
    }
    
    @Override
    public void process(EventHeader event) {
        // TODO: We should be able to get a list of IIdentifiers for each scintillator without needing hits.
        // Get the hodoscope hits from the data manager.
        List<SimTrackerHit> hodoscopeHits = null;
        if(event.hasCollection(SimTrackerHit.class, inputCollectionName)) {
            hodoscopeHits = event.get(SimTrackerHit.class, inputCollectionName);
        } else {
            hodoscopeHits = new ArrayList<SimTrackerHit>(0);
        }
        
        // Iterate over the hodoscope hits and, for every unique
        // scintillator, record the properties.
        for(SimTrackerHit hit : hodoscopeHits) {
            // Store the scintillator bounds for testing the detector
            // positioning.
            Integer posvar = Integer.valueOf(hodoscopeDetectorElement.getScintillatorUniqueKey(hit));
            if(!hodoscopeBoundsMap.containsKey(posvar)) {
                hodoscopeBoundsMap.put(posvar, getHodoscopeBounds(hit.getIdentifier()));
            }
        }
    }
    
    /**
     * Creates a concise {@link java.lang.String String} object that
     * contains the geometric details for the scintillator on which
     * the argument hit occurred.
     * @param hit - The hit.
     * @return Returns a <code>String</code> describing the geometric
     * details of the scintillator on which the hit occurred.
     */
    private String getHodoscopeBounds(IIdentifier id) {
        int[] indices = hodoscopeDetectorElement.getHodoscopeIndices(id);
        double[] dimensions = hodoscopeDetectorElement.getScintillatorHalfDimensions(id);
        double[] position = hodoscopeDetectorElement.getScintillatorPosition(id);
        double[] holeX = hodoscopeDetectorElement.getScintillatorHolePositions(id);
        return String.format("Bounds for scintillator <%d, %2d, %d> :: x = [ %6.2f, %6.2f ], y = [ %6.2f, %6.2f ], z = [ %7.2f, %7.2f ];   FADC Channels :: %d;   Hole Positions:  x1 = %6.2f, x2 = %s",
                indices[0], indices[1], indices[2], position[0] - dimensions[0], position[0] + dimensions[0],
                position[1] - dimensions[1], position[1] + dimensions[1], position[2] - dimensions[2], position[2] + dimensions[2],
                hodoscopeDetectorElement.getScintillatorChannelCount(id), holeX[0], holeX.length > 1 ? String.format("%6.2f", holeX[1]) : "N/A");
    }
    
    /**
     * Sets the name of the hodoscope hits collection.
     * @param collection - The name of the collection containing the
     * hodoscope hits.
     */
    public void setInputCollectionName(String collection) {
        inputCollectionName = collection;
    }
}