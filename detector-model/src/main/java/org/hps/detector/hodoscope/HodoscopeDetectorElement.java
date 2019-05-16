package org.hps.detector.hodoscope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.converter.compact.SubdetectorDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.solids.Box;
import org.lcsim.event.SimTrackerHit;

public final class HodoscopeDetectorElement extends SubdetectorDetectorElement {
    private List<IIdentifier> scintillatorIDList = new ArrayList<IIdentifier>();
    private Map<Integer, IIdentifier> scintillatorToIDMap = new HashMap<Integer, IIdentifier>();
    private Map<Integer, List<HodoscopeChannel>> scintillatorPositionToChannelMap = new HashMap<Integer, List<HodoscopeChannel>>();
    
    public HodoscopeDetectorElement(String name, IDetectorElement parent) {
        super(name, parent);
    }
    
    /**
     * Gets the hardware channels that correspond to a scintillator.
     * Note that method {@link
     * org.hps.detector.hodoscope.HodoscopeDetectorElement#updateScintillatorChannelMap(HodoscopeChannelCollection)
     * updateScintillatorChannelMap(HodoscopeChannelCollection)} must
     * be run before this method will function.
     * @param ix - The x-index of the scintillator. This should range
     * from 0 (for the closest to the beam) to 4 (for the farthest
     * from the beam).
     * @param iy - The y-index of the hodoscope. This may be either
     * -1 for the bottom half of the hodoscope or 1 for the top half.
     * @param layer - The layer number of the hodoscope. This may be
     * either 0 for layer 1 or 1 for layer 2.
     * @return Returns the hardware channel(s) for the scintillator
     * as a {@link java.util.List List} of {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel
     * HodoscopeChannel} objects. If the scintillator is invalid, 
     * then a value of <code>null</code> will be returned.
     */
    public List<HodoscopeChannel> getHardwareChannels(int ix, int iy, int layer) {
        return getHardwareChannels(getScintillatorUniqueKey(ix, iy, layer));
    }
    
    /**
     * Gets the hardware channels that correspond to the scintillator
     * on which an argument hit occurs.
     * Note that method {@link
     * org.hps.detector.hodoscope.HodoscopeDetectorElement#updateScintillatorChannelMap(HodoscopeChannelCollection)
     * updateScintillatorChannelMap(HodoscopeChannelCollection)} must
     * be run before this method will function.
     * @param hit - The hit.
     * @return Returns the hardware channel(s) for the scintillator
     * as a {@link java.util.List List} of {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel
     * HodoscopeChannel} objects. If the scintillator is invalid, 
     * then a value of <code>null</code> will be returned.
     */
    public List<HodoscopeChannel> getHardwareChannels(SimTrackerHit hit) {
        return getHardwareChannels(getScintillatorUniqueKey(hit));
    }
    
    /**
     * Gets the x-, y-, and z-indices of the hodoscope scintillator
     * on which the hit occurred.
     * @param id - The hit's <code>IIdentifier</code> object.
     * @return Returns the scintillator indices in the form of an
     * <code>int</code> array with the format <code>{ ix, iy, iz
     * }</code>.
     */
    public final int[] getHodoscopeIndices(IIdentifier id) {
        IIdentifierHelper helper = getIdentifierHelper();
        IIdentifierDictionary dict = helper.getIdentifierDictionary();
        IExpandedIdentifier expId = helper.unpack(id);
        return new int[] { expId.getValue(dict.getFieldIndex("ix")), 
                expId.getValue(dict.getFieldIndex("iy")), 
                expId.getValue(dict.getFieldIndex("layer")) };
    }
    
    /**
     * Gets the x-, y-, and z-indices of the hodoscope scintillator
     * on which the hit occurred.
     * @param hit - The hit.
     * @return Returns the scintillator indices in the form of an
     * <code>int</code> array with the format <code>{ ix, iy, iz
     * }</code>.
     */
    public final int[] getHodoscopeIndices(SimTrackerHit hit) {
        return getHodoscopeIndices(hit.getIdentifier());
    }
    
    /**
     * Gets the number of unique channels contained in the specified
     * scintillator. Some scintillators will have two optical fiber
     * holes which lead to separate channels. Others have either one
     * optical fiber hole or two, but each connect to the same FADC
     * channel.
     * @param ix - The x-index of the scintillator. This should range
     * from 0 (for the closest to the beam) to 4 (for the farthest
     * from the beam).
     * @param iy - The y-index of the hodoscope. This may be either
     * -1 for the bottom half of the hodoscope or 1 for the top half.
     * @param layer - The layer number of the hodoscope. This may be
     * either 0 for layer 1 or 1 for layer 2.
     * @return Returns the number of unique FADC channels as an
     * <code>int</code>. This is <code>1</code> for scintillators
     * that have either one fiber hole or both fiber holes connect to
     * the same FADC channel. It is <code>2</code> otherwise.
     */
    public int getScintillatorChannelCount(int ix, int iy, int layer) {
        // Get the unique key for this scintillator.
        Integer posvar = Integer.valueOf(getScintillatorUniqueKey(ix, iy, layer));
        
        // Make sure that the channel map is instantiated.
        if(scintillatorPositionToChannelMap.isEmpty()) { populatePositionToChannelMap(); }
        
        // Return the number of unique channels.
        return scintillatorPositionToChannelMap.get(posvar).size();
    }
    
    /**
     * Gets the number of unique channels contained in the specified
     * scintillator. Some scintillators will have two optical fiber
     * holes which lead to separate channels. Others have either one
     * optical fiber hole or two, but each connect to the same FADC
     * channel.
     * @param id - The <code>IIdentifier</code> object associated
     * with the scintillator.
     * @return Returns the number of unique FADC channels as an
     * <code>int</code>. This is <code>1</code> for scintillators
     * that have either one fiber hole or both fiber holes connect to
     * the same FADC channel. It is <code>2</code> otherwise.
     */
    public int getScintillatorChannelCount(IIdentifier id) {
        int[] indices = getHodoscopeIndices(id);
        return getScintillatorChannelCount(indices[0], indices[1], indices[2]);
    }
    
    /**
     * Gets the number of unique channels contained in the specified
     * scintillator. Some scintillators will have two optical fiber
     * holes which lead to separate channels. Others have either one
     * optical fiber hole or two, but each connect to the same FADC
     * channel.
     * @param hit - A hit occurring on the scintillator.
     * @return Returns the number of unique FADC channels as an
     * <code>int</code>. This is <code>1</code> for scintillators
     * that have either one fiber hole or both fiber holes connect to
     * the same FADC channel. It is <code>2</code> otherwise.
     */
    public int getScintillatorChannelCount(SimTrackerHit hit) {
        return getScintillatorChannelCount(hit.getIdentifier());
    }
    
    /**
     * Gets the dimensions of the scintillator on which the hit
     * occurred. Returned values are one half the actual width,
     * height, and depth of the scintillator.
     * @param id - The hit's <code>IIdentifier</code> object.
     * @return Returns a <code>double</code> array in the format of
     * <code>{ width / 2, height / 2, depth / 2 }</code>.
     */
    public final double[] getScintillatorHalfDimensions(IIdentifier id) {
        IDetectorElement idDetElem = findDetectorElement(id).get(0);
        Box box = (Box) idDetElem.getGeometry().getLogicalVolume().getSolid();
        return new double[] { box.getXHalfLength() + 0.05, box.getYHalfLength() + 0.05, box.getZHalfLength() + 0.25 };
    }
    
    /**
     * Gets the dimensions of the scintillator on which the hit
     * occurred. Returned values are one half the actual width,
     * height, and depth of the scintillator.
     * @param hit - The hit.
     * @return Returns a <code>double</code> array in the format of
     * <code>{ width / 2, height / 2, depth / 2 }</code>.
     */
    public final double[] getScintillatorHalfDimensions(SimTrackerHit hit) {

        return getScintillatorPosition(hit.getIdentifier());

    }
    
    /**
     * Gets the absolute positioning of the optical fiber hole(s) in
     * the scintillator in which a hit occurred. The array will be
     * either size 1 for scintillators with only one fiber hole or
     * size 2 for this with two. The first index corresponds to the
     * fiber hole that is closest to the center of the detector, and
     * the second to the one that is closer to the positron side of
     * the detector.
     * @param id - The hit's <code>IIdentifier</code> object.
     * @return Returns a <code>double</code> array containing the
     * absolute positions of the optical fiber holes in the
     * scintillator.
     */
    public final double[] getScintillatorHolePositions(IIdentifier id) {
        // TODO: This is not ideal - ideally we would set this in the geometry somehow. This could hypothetically become invalid.
        // Get the x position at which the scintillator starts.
        double[] position = getScintillatorPosition(id);
        
        int ix = this.getIdentifierHelper().getValue(id, "ix");
        
        // The scintillators ix = 0 have only a single hole that is
        // in the exact geometric center of the scintillator.
        if(ix == 0) {
            return new double[] { position[0] };
        }
        
        // All other scintillators have two holes that all follow the
        // same paradigm.
        double[] halfDimension = getScintillatorHalfDimensions(id);
        double xStart = position[0] - halfDimension[0];
        double holeDisplacement = ((halfDimension[0] * 2) - 22.0) / 2;
        return new double[] { xStart + holeDisplacement, xStart + holeDisplacement + 22.0 };
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
    public final double[] getScintillatorHolePositions(SimTrackerHit hit) {
        return getScintillatorHolePositions(hit.getIdentifier());
    }
    
    /**
     * Gets the {@link org.lcsim.detector.identifier.IIdentifier
     * IIdentifier} object corresponding to the specified
     * scintillator indices.
     * @param key - The unique key corresponding to the scintillator.
     * it may be acquired via the method {@link
     * org.hps.detector.hodoscope.HodoscopeDetectorElement#getScintillatorUniqueKey(int, int, int)
     * getScintillatorUniqueKey(int, int, int)}.
     * @return Returns the <code>IIdentifier</code> associated with
     * the specified scintillator.
     */
    public final IIdentifier getScintillatorIdentifier(int key) {
        return scintillatorToIDMap.get(Integer.valueOf(key));
    }
    
    /**
     * Gets the {@link org.lcsim.detector.identifier.IIdentifier
     * IIdentifier} object corresponding to the specified
     * scintillator indices.
     * @param ix - The x-index of the scintillator. This should range
     * from 0 (for the closest to the beam) to 4 (for the farthest
     * from the beam).
     * @param iy - The y-index of the hodoscope. This may be either
     * -1 for the bottom half of the hodoscope or 1 for the top half.
     * @param layer - The layer number of the hodoscope. This may be
     * either 0 for layer 1 or 1 for layer 2.
     * @return Returns the <code>IIdentifier</code> associated with
     * the specified scintillator.
     */
    public final IIdentifier getScintillatorIdentifier(int ix, int iy, int layer) {
        return getScintillatorIdentifier(getScintillatorUniqueKey(ix, iy, layer));
    }
    
    /**
     * Gets a list all the {@link
     * org.lcsim.detector.identifier.IIdentifier IIdentifier} objects
     * corresponding to the hodoscope scintillators.
     * @return Returns the hodoscope scintillator ID objects in an
     * unmodifiable {@link java.util.List List}.
     */
    public final List<IIdentifier> getScintillatorIdentifiers() {
        return Collections.unmodifiableList(scintillatorIDList);
    }
    
    /**
     * Gets the position of the center of the scintillator on which
     * the hit occurred.
     * @param id - The hit's <code>IIdentifier</code> object.
     * @return Returns a <code>double</code> array in the format of
     * <code>{ x, y, z }</code>.
     */
    public final double[] getScintillatorPosition(IIdentifier id) {
        IDetectorElement idDetElem = findDetectorElement(id).get(0);
        IGeometryInfo geom = idDetElem.getGeometry();
        return new double[] { geom.getPosition().x(), geom.getPosition().y(), geom.getPosition().z() };
    }
    
    /**
     * Gets the position of the center of the scintillator on which
     * the hit occurred.
     * @param hit - The hit.
     * @return Returns a <code>double</code> array in the format of
     * <code>{ x, y, z }</code>.
     */
    public final double[] getScintillatorPosition(SimTrackerHit hit) {

        return getScintillatorPosition(hit.getIdentifier());

    }
    
    /**
     * Create a unique key for the hodoscope scintillator.
     * @param ix - The x-index of the scintillator. This should range
     * from 0 (for the closest to the beam) to 4 (for the farthest
     * from the beam).
     * @param iy - The y-index of the hodoscope. This may be either
     * -1 for the bottom half of the hodoscope or 1 for the top half.
     * @param layer - The layer number of the hodoscope. This may be
     * either 0 for layer 1 or 1 for layer 2.
     * @return Returns a unique integer representing these values.
     * The last bit represents the layer, the penultimate bit the top
     * or bottom position, and the remaining bits the x-index.
     */
    public static final int getScintillatorUniqueKey(int ix, int iy, int layer) {
        int var = (ix << 2);
        var = var | ((iy == -1 ? 0 : 1) << 1);
        var = var | layer;
        return var;
    }
    
    /**
     * Create a unique key for the hodoscope scintillator.
     * @param id - The {@link
     * org.lcsim.detector.identifier.IIdentifier IIdentifier} object
     * that corresponds to the scintillator.
     * @return Returns a unique integer representing these values.
     * The last bit represents the layer, the penultimate bit the top
     * or bottom position, and the remaining bits the x-index.
     */
    public final int getScintillatorUniqueKey(IIdentifier id) {
        int[] indices = getHodoscopeIndices(id);
        return getScintillatorUniqueKey(indices[0], indices[1], indices[2]);
    }
    
    /**
     * Create a unique key for the scintillator on which a hit
     * occurs.
     * @param hit - The hit.
     * @return Returns a unique integer representing these values.
     * The last bit represents the layer, the penultimate bit the top
     * or bottom position, and the remaining bits the x-index.
     */
    public final int getScintillatorUniqueKey(SimTrackerHit hit) {
        int[] indices = getHodoscopeIndices(hit);
        return getScintillatorUniqueKey(indices[0], indices[1], indices[2]);
    }

    /**
     * Initialize the detector element.
     */
    @Override
    public void initialize() {
        // Populate the ID collections.
        this.populateIDCollections();
    }
    
    /**
     * Gets the hardware channels that correspond to the scintillator
     * associated with the argument unique key.
     * Note that method {@link
     * org.hps.detector.hodoscope.HodoscopeDetectorElement#updateScintillatorChannelMap(HodoscopeChannelCollection)
     * updateScintillatorChannelMap(HodoscopeChannelCollection)} must
     * be run before this method will function.
     * @param key - The unique key.
     * @return Returns the hardware channel(s) for the scintillator
     * as a {@link java.util.List List} of {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel
     * HodoscopeChannel} objects. If the scintillator is invalid, 
     * then a value of <code>null</code> will be returned.
     */
    private List<HodoscopeChannel> getHardwareChannels(int key) {
        // Make sure that the channel map is instantiated.
        if(scintillatorPositionToChannelMap.isEmpty()) { populatePositionToChannelMap(); }
        
        // Get the mapped value.
        return scintillatorPositionToChannelMap.get(Integer.valueOf(key));
    }
    
    /**
     * Populates the list of scintillator identifiers and also the
     * mapping of scintillator indices-to-identifiers.
     */
    private void populateIDCollections() {
        for(IDetectorElement ele : getChildren()) {
            scintillatorIDList.add(ele.getIdentifier());
            int[] indices = getHodoscopeIndices(ele.getIdentifier());
            Integer posvar = Integer.valueOf(getScintillatorUniqueKey(indices[0], indices[1], indices[2]));
            scintillatorToIDMap.put(posvar, ele.getIdentifier());
        }
    }
    
    /**
     * Fills the map of hardware FADC channels to scintillator
     * indices.
     */
    private void populatePositionToChannelMap() {
        // Load the conditions database and get the hodoscope channel
        // collection data.
        DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        HodoscopeChannelCollection channels = conditions.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        
        // Iterate over the channels and map each hardware channel to
        // the indices of the scintillator it exists within.
        for(HodoscopeChannel channel : channels) {
            // Get the unique key for the scintillator.
            Integer posvar = Integer.valueOf(getScintillatorUniqueKey(channel.getIX().intValue(), channel.getIY().intValue(), channel.getLayer().intValue()));
            
            // Map the scintillator to its indices.
            if(scintillatorPositionToChannelMap.containsKey(posvar)) {
                scintillatorPositionToChannelMap.get(posvar).add(channel);
            } else {
                List<HodoscopeChannel> channelList = new ArrayList<HodoscopeChannel>();
                channelList.add(channel);
                scintillatorPositionToChannelMap.put(posvar, channelList);
            }
        }
    }
}
