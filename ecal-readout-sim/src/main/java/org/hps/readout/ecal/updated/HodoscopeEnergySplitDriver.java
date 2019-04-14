package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.solids.Box;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;

public class HodoscopeEnergySplitDriver extends ReadoutDriver {
    private HodoscopeDetectorElement hodoscopeDetectorElement;
    private String truthHitCollectionName = "HodoscopeHits";
    private Map<Integer, List<HodoscopeChannel>> crystalPositionToChannelMap = new HashMap<Integer, List<HodoscopeChannel>>();
    
    private Map<Integer, String> hodoscopeBoundsMap = new HashMap<Integer, String>();
    
    @Override
    public void detectorChanged(Detector detector) {
        // Update the hodoscope detector object.
        hodoscopeDetectorElement = (HodoscopeDetectorElement) detector.getSubdetector("Hodoscope").getDetectorElement();
        
        // Get the conditions database.
        final DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        
        // Store each of the hodoscope channels according to its
        // crystal indices. These are managed by storing each index
        // value in an integer to create a unique key.
        final HodoscopeChannelCollection channels = mgr.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        for(HodoscopeChannel channel : channels) {
            Integer var = Integer.valueOf(getHodoscopePositionVar(channel.getX(), channel.getY(), channel.getLayer()));
            if(crystalPositionToChannelMap.containsKey(var)) {
                crystalPositionToChannelMap.get(var).add(channel);
            } else {
                List<HodoscopeChannel> channelList = new ArrayList<HodoscopeChannel>();
                channelList.add(channel);
                crystalPositionToChannelMap.put(var, channelList);
            }
        }
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
        // Get the hodoscope hits from the data manager.
        // Get current SLIC truth energy depositions.
        Collection<SimTrackerHit> hodoscopeHits = ReadoutDataManager.getData(ReadoutDataManager.getCurrentTime(), ReadoutDataManager.getCurrentTime() + 2.0,
                truthHitCollectionName, SimTrackerHit.class);
        
        // Iterate over the hodoscope hits and create new calorimeter
        // hit objects from them. For hodoscope scintillators with
        // multiple FADC channels, split the energy between them.
        for(SimTrackerHit hit : hodoscopeHits) {
            int[] hodoIndices = getHodoscopeIndices(hit);
            Integer posvar = Integer.valueOf(getHodoscopePositionVar(hodoIndices[0], hodoIndices[1], hodoIndices[2]));
            if(!hodoscopeBoundsMap.containsKey(posvar)) {
                hodoscopeBoundsMap.put(posvar, getHodoscopeBounds(hit));
            }
        }
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
     * Gets the x-, y-, and z-indices of the hodoscope scintillator
     * on which the hit occurred.
     * @param hit - The hit.
     * @return Returns the scintillator indices in the form of an
     * <code>int</code> array with the format <code>{ ix, iy, iz
     * }</code>.
     */
    private final int[] getHodoscopeIndices(SimTrackerHit hit) {
        IDDecoder decoder = ReadoutDataManager.getIDDecoder(truthHitCollectionName);
        decoder.setID(hit.getCellID64());
        return new int[] { decoder.getValue("ix"), decoder.getValue("iy"), hit.getLayer() };
    }
    
    /**
     * Create a unique key for the hodoscope crystal.
     * @param ix - The x-index of the crystal. This should range from
     * 0 (for the closest to the beam) to 4 (for the farthest from
     * the beam).
     * @param iy - The y-index of the hodoscope. This may be either
     * -1 for the bottom half of the hodoscope or 1 for the top half.
     * @param layer - The layer number of the hodoscope. This may be
     * either 0 for layer 1 or 1 for layer 2.
     * @return Returns a unique integer representing these values.
     * The last bit represents the layer, the penultimate bit the top
     * or bottom position, and the remaining bits the x-index.
     */
    private static final int getHodoscopePositionVar(int ix, int iy, int layer) {
        int var = (ix << 2);
        var = var | ((iy == -1 ? 0 : 1) << 1);
        var = var | layer;
        return var;
    }
    
    /**
     * Gets the dimensions of the scintillator on which the hit
     * occurred. Returned values are one half the actual width,
     * height, and depth of the scintillator.
     * @param hit - The hit.
     * @return Returns a <code>double</code> array in the format of
     * <code>{ width / 2, height / 2, depth / 2 }</code>.
     */
    private final double[] getScintillatorHalfDimensions(SimTrackerHit hit) {
        IDetectorElement idDetElem = hodoscopeDetectorElement.findDetectorElement(hit.getIdentifier()).get(0);
        Box box = (Box) idDetElem.getGeometry().getLogicalVolume().getSolid();
        return new double[] { box.getXHalfLength(), box.getYHalfLength(), box.getZHalfLength() };
    }
    
    /**
     * Gets the number of unique channels contained in the specified
     * scintillator. Some scintillators will have two optical fiber
     * holes which lead to separate channels. Others have either one
     * optical fiber hole or two, but each connect to the same FADC
     * channel.
     * @param ix - The x-index for the crystal.
     * @param iy - The y-index for the crystal.
     * @param iz - The layer number for the crystal.
     * @return Returns the number of unique FADC channels as an
     * <code>int</code>. This is <code>1</code> for scintillators
     * that have either one fiber hole or both fiber holes connect to
     * the same FADC channel. It is <code>2</code> otherwise.
     */
    private final int getScintillatorChannelCount(int ix, int iy, int iz) {
        // Get the unique key for this scintillator.
        Integer var = Integer.valueOf(getHodoscopePositionVar(ix, iy, iz));
        
        // Return the number of unique channels.
        return crystalPositionToChannelMap.get(var).size();
    }
    
    /**
     * Gets the number of unique channels contained in the specified
     * scintillator. Some scintillators will have two optical fiber
     * holes which lead to separate channels. Others have either one
     * optical fiber hole or two, but each connect to the same FADC
     * channel.
     * @param hit - A hit occuring on the desired scintillator.
     * @return Returns the number of unique FADC channels as an
     * <code>int</code>. This is <code>1</code> for scintillators
     * that have either one fiber hole or both fiber holes connect to
     * the same FADC channel. It is <code>2</code> otherwise.
     */
    private final int getScintillatorChannelCount(SimTrackerHit hit) {
        int[] indices = getHodoscopeIndices(hit);
        return getScintillatorChannelCount(indices[0], indices[1], indices[2]);
    }
    
    /**
     * Gets the absolute positioning of the optical fiber hole(s) in
     * the scintillator in which a hit occurred. The array will be
     * either size 1 for scintillators with only one fiber hole or
     * size 2 for this with two. The first index corresponds to the
     * fiber hole that is closest to the center of the detector, and
     * the second to the one that is closer to the positron side of
     * the detector.
     * @param hit - The hit.
     * @return Returns a <code>double</code> array containing the
     * absolute positions of the optical fiber holes in the
     * scintillator.
     */
    private final double[] getScintillatorHolePositions(SimTrackerHit hit) {
        // TODO: This is not ideal - ideally we would set this in the geometry somehow. This could hypothetically become invalid.
        // Get the x position at which the scintillator starts.
        double[] position = getScintillatorPosition(hit);
        
        // Get the indices for the scintillator.
        IDDecoder decoder = ReadoutDataManager.getIDDecoder(truthHitCollectionName);
        int ix = decoder.getValue("ix");
        
        // The scintillators ix = 0 have only a single hole that is
        // in the exact geometric center of the scintillator.
        if(ix == 0) {
            return new double[] { position[0] };
        }
        
        // All other scintillators have two holes that all follow the
        // same paradigm.
        double[] halfDimension = getScintillatorHalfDimensions(hit);
        double xStart = position[0] - halfDimension[0];
        double holeDisplacement = ((halfDimension[0] * 2) - 22.0) / 2;
        return new double[] { xStart + holeDisplacement, xStart + holeDisplacement + 22.0 };
    }
    
    /**
     * Gets the position of the center of the scintillator on which
     * the hit occurred.
     * @param hit - The hit.
     * @return Returns a <code>double</code> array in the format of
     * <code>{ x, y, z }</code>.
     */
    private final double[] getScintillatorPosition(SimTrackerHit hit) {
        IDetectorElement idDetElem = hodoscopeDetectorElement.findDetectorElement(hit.getIdentifier()).get(0);
        IGeometryInfo geom = idDetElem.getGeometry();
        return new double[] { geom.getPosition().x(), geom.getPosition().y(), geom.getPosition().z() };
    }
    
    private final String getHodoscopeBounds(SimTrackerHit hit) {
        int[] indices = getHodoscopeIndices(hit);
        double[] dimensions = getScintillatorHalfDimensions(hit);
        double[] position = getScintillatorPosition(hit);
        double[] holeX = getScintillatorHolePositions(hit);
        return String.format("Bounds for scintillator <%d, %2d, %d> :: x = [ %f, %f ], y = [ %f, %f ], z = [ %f, %f ];   FADC Channels :: %d;   Hole Positions:  x1 = %f, x2 = %s",
                indices[0], indices[1], indices[2], position[0] - dimensions[0], position[0] + dimensions[0],
                position[1] - dimensions[1], position[1] + dimensions[1], position[2] - dimensions[2], position[2] + dimensions[2],
                getScintillatorChannelCount(hit), holeX[0], holeX.length > 1 ? String.format("%f", holeX[1]) : "N/A");
    }
}