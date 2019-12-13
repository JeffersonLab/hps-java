package org.hps.analysis.hodoscope;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

/**
 * <code>HodoscopePlotsDriver</code> creates plots for the energy and
 * position distributions of SLIC truth hodoscope hits, preprocessed
 * hodoscope hits, and digitized, converted hodoscope hits. Included
 * are plots of varying levels of specificity, from general all hits
 * plots to per-channel plots.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class HodoscopePlotsDriver extends ReadoutDriver {
    /** Tracks the current simulation time for the local driver. */
    private double localTime = 0;
    /** Maps SLIC channel ID hits to their indices. */
    private HodoscopeDetectorElement hodoscopeDetectorElement;
    /**
     * Maps channel IDs for hardware-channel hits to conditions
     * database {@link org.hps.conditions.hodoscope.HodoscopeChannel
     * HodoscopeChannel} objects.
     */
    private Map<Long, HodoscopeChannel> channelIDMap = new HashMap<Long, HodoscopeChannel>();
    
    /** Collection name for the truth hit collection. */
    private String hodoscopeTruthHitCollectionName = "HodoscopeHits";
    /** Collection name for the preprocessed hit collection. */
    private String hodoscopePreprocessedHitCollectionName = "HodoscopePreprocessedHits";
    /** Collection name for the digitized hit collection. */
    private String hodoscopeDigitizedHitCollectionName = "HodoscopeCorrectedHits";
    
    /**
     * Converts the type IDs given in {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_TRUTH
     * TYPE_TRUTH}, {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_PREPROCESSED
     * TYPE_PREPROCESSED}, and {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_DIGITIZED
     * TYPE_DIGITIZED} into a human-readable format.
     */
    private static final String[] TYPE_FOLDER = {
            "Truth Plots/",
            "Preprocessed Plots/",
            "Digitized Plots/"
    };
    
    /** Folder for position plots. */
    private static final String POSITION_FOLDER = "Position/";
    /** Folder for energy plots. */
    private static final String ENERGY_FOLDER = "Energy/";
    /** Folder for channel-specific plots. */
    private static final String CHANNEL_FOLDER = "Channel/";
    
    /** Type ID for truth hits. */
    private static final int TYPE_TRUTH = 0;
    /** Type ID for preprocessed hits. */
    private static final int TYPE_PREPROCESSED = 1;
    /** Type ID for digitized hits. */
    private static final int TYPE_DIGITIZED = 2;
    
    /** ID specifying the top half of the hodoscope. */
    private static final int TOP = 0;
    /** ID specifying the bottom half of the hodoscope. */
    private static final int BOT = 1;
    /** ID specifying the first layer of the hodoscope; i.e. the
     * layer closest to the target.
     */
    private static final int L1 = 0;
    /**
     * ID specifying the second layer of the hodoscope; i.e. the
     * layer farthest to the target.
     */
    private static final int L2 = 1;
    /**
     * ID specifying the hole closest to the electron side of the
     * calorimeter.
     */
    private static final int HOLE1 = 0;
    /**
     * ID specifying the hole farthest from the electron side of the
     * calorimeter.
     */
    private static final int HOLE2 = 1;
    /**
     * ID specifying that a scintillator has only one hole or that
     * the hole number is not known.
     */
    private static final int HOLE_NULL = Integer.MIN_VALUE;
    
    /**
     * Translates the IDs given in {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TOP TOP} and
     * {@link org.hps.analysis.hodoscope.HodoscopePlotsDriver#BOT
     * BOT} into human-readable forms.
     */
    private static final String[] TOP_BOT_TEXT = { "Top", "Bottom" };
    
    /** Creates and handles plots. */
    private static final AIDA aida = AIDA.defaultInstance();
    
    /**
     * Updates the channel mappings.
     */
    @Override
    public void detectorChanged(Detector detector) {
        populateChannelCollections();
        hodoscopeDetectorElement = (HodoscopeDetectorElement) detector.getSubdetector("Hodoscope").getDetectorElement();
    }
    
    @Override
    public void process(EventHeader event) {
        // Check whether all of the collections are ready.
        boolean truthIsReady = ReadoutDataManager.checkCollectionStatus(hodoscopeTruthHitCollectionName, localTime + 4.0);
        boolean preprocessedIsReady = ReadoutDataManager.checkCollectionStatus(hodoscopePreprocessedHitCollectionName, localTime + 4.0);
        boolean digitizedIsReady = ReadoutDataManager.checkCollectionStatus(hodoscopeDigitizedHitCollectionName, localTime + 4.0);
        
        // If it is, get the data. If not, wait.
        if(!truthIsReady || !preprocessedIsReady || !digitizedIsReady) { return; }
        Collection<SimTrackerHit> truthHits = ReadoutDataManager.getData(localTime, localTime + 4.0, hodoscopeTruthHitCollectionName, SimTrackerHit.class);
        Collection<SimCalorimeterHit> preprocessedHits = ReadoutDataManager.getData(localTime, localTime + 4.0, hodoscopePreprocessedHitCollectionName, SimCalorimeterHit.class);
        Collection<CalorimeterHit> digitizedHits = ReadoutDataManager.getData(localTime, localTime + 4.0, hodoscopeDigitizedHitCollectionName, CalorimeterHit.class);
        
        // Populate this plots.
        fillPlots(TYPE_TRUTH, truthHits, SimTrackerHit.class);
        fillPlots(TYPE_PREPROCESSED, preprocessedHits, SimCalorimeterHit.class);
        fillPlots(TYPE_DIGITIZED, digitizedHits, CalorimeterHit.class);
        
        // Increment the local time.
        localTime += 4.0;
    }
    
    /**
     * Sets the name of the collection containing the digitized hits.
     * @param collection - The collection name.
     */
    public void setDigitizedHitCollectionName(String collection) {
        hodoscopeDigitizedHitCollectionName = collection;
    }
    
    /**
     * Sets the name of the collection containing the preprocessed
     * hits.
     * @param collection - The collection name.
     */
    public void setPreprocessedHitCollectionName(String collection) {
        hodoscopePreprocessedHitCollectionName = collection;
    }
    
    /**
     * Sets the name of the collection containing the truth hits.
     * @param collection - The collection name.
     */
    public void setTruthHitCollectionName(String collection) {
        hodoscopeTruthHitCollectionName = collection;
    }
    
    /**
     * Instantiates all the plots.
     */
    @Override
    public void startOfData() {
        // Instantiate the SLIC-channel plots.
        makeEnergyPlot(getEnergyDistPlotName(TYPE_TRUTH));
        for(int layer = L1; layer <= L2; layer ++) {
            makePositionPlot(getPositionDistPlotName(TYPE_TRUTH, layer));
            for(int topBot = TOP; topBot <= BOT; topBot++) {
                makeEnergyPlot(getEnergyDistPlotName(TYPE_TRUTH, topBot, layer));
                for(int ix = 0; ix < 5; ix++) {
                    makeEnergyPlot(getEnergyDistPlotName(TYPE_TRUTH, ix, topBot, layer, HOLE_NULL));
                }
            }
        }
        
        // Instantiate the hardware-channel channel-specific plots.
        final DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        final HodoscopeChannelCollection channels = conditions.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        for(HodoscopeChannel channel : channels) {
            // Get the values for the channel.
            int layer = channel.isLayer1() ? L1 : L2;
            int topBot = channel.isTop() ? TOP : BOT;
            int ix = channel.getIX();
            int hole = channel.getHole() == HodoscopeChannel.HOLE_LOW_X ? HOLE1 : HOLE2;
            
            // Make the plots.
            makeEnergyPlot(getEnergyDistPlotName(TYPE_PREPROCESSED, ix, topBot, layer, hole));
            makeEnergyPlot(getEnergyDistPlotName(TYPE_DIGITIZED, ix, topBot, layer, hole));
        }
        
        // Instantiate the hardware-channel general plots.
        for(int type = TYPE_PREPROCESSED; type <= TYPE_DIGITIZED; type++) {
            makeEnergyPlot(getEnergyDistPlotName(type));
            for(int layer = L1; layer <= L2; layer ++) {
                makePositionPlot(getPositionDistPlotName(type, layer));
                for(int topBot = TOP; topBot <= BOT; topBot++) {
                    makeEnergyPlot(getEnergyDistPlotName(type, topBot, layer));
                }
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
     * Fills all of the plots of type <code>typeID</code> for the
     * specified collection of hits.
     * @param <T> The object class of the hits.
     * @param typeID - The type of plots to fill. May only be one of
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_TRUTH
     * TYPE_TRUTH}, {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_PREPROCESSED
     * TYPE_PREPROCESSED}, or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_DIGITIZED
     * TYPE_DIGITIZED}.
     * @param hits - The hits with which to populate the plots. These
     * may be only of type {@link org.lcsim.event.CalorimeterHit
     * CalorimeterHit}, {@link org.lcsim.event.SimCalorimeterHit
     * SimCalorimeterHit}, or {@link org.lcsim.event.SimTrackerHit
     * SimTrackerHit}.
     * @param objectType - The {@link java.lang.Class Class} object
     * for the hits' object class.
     * @throws IllegalArgumentException Occurs if an unsupported
     * type ID or hit class is given.
     */
    private final <T> void fillPlots(int typeID, Collection<T> hits, Class<T> objectType) throws IllegalArgumentException {
        // If an invalid type is given, throw an exception.
        if(typeID != TYPE_TRUTH && typeID != TYPE_PREPROCESSED && typeID != TYPE_DIGITIZED) {
            throw new IllegalArgumentException("Unrecognized plot category ID \"" + typeID + "\".");
        }
        
        // Get the index and energy methods for this object type.
        Method indexMethod;
        Method energyMethod;
        try {
            indexMethod = HodoscopePlotsDriver.class.getDeclaredMethod("getIndices", objectType);
            energyMethod = HodoscopePlotsDriver.class.getDeclaredMethod("getEnergy", objectType);
        } catch(NoSuchMethodException e) {
            throw new IllegalArgumentException("Unsupported hit type \"" + objectType.getSimpleName() + "\".");
        }
        
        // Populate the plots.
        for(T hit : hits) {
            // Get the plot values.
            int[] indices;
            double energy;
            try {
                indices = (int[]) indexMethod.invoke(this, hit);
                energy = (double) energyMethod.invoke(this, hit);
            } catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            
            // Fill the plots.
            fillPlots(typeID, indices[0], indices[1], indices[2], indices[3], energy);
        }
    }
    
    /**
     * Fills the plots of the type <code>typeID</code> for a hit with
     * an energy <code>energy</code> GeV and occurring on the
     * indicated scintillator.
     * @param typeID - The type of plots to fill. May only be one of
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_TRUTH
     * TYPE_TRUTH}, {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_PREPROCESSED
     * TYPE_PREPROCESSED}, or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_DIGITIZED
     * TYPE_DIGITIZED}.
     * @param ix - The x-index of the scintillator.
     * @param topBot - Whether the hit occurs on the top or bottom of
     * the hodoscope. This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TOP
     * TOP} or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#BOT BOT}.
     * @param layer - Whether the hit occurs on layer 1 or layer 2.
     * This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#L1 L1} or
     * {@link org.hps.analysis.hodoscope.HodoscopePlotsDriver#L2 L2}.
     * @param hole - Which optic fiber hole in the scintillator is
     * the plot is for. This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#HOLE1 HOLE1}
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#HOLE2 HOLE2},
     * or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#HOLE_NULL
     * HOLE_NULL}.
     * @param energy - The energy of the hit in GeV.
     * @throws IllegalArgumentException Occurs if <code>layer</code>,
     * <code>topBot</code>, <code>typeID</code>, <code>hole</code>
     * are not supported values.
     */
    private static final void fillPlots(int typeID, int ix, int topBot, int layer, int hole, double energy) throws IllegalArgumentException {
        // Validate the arguments.
        if(typeID != TYPE_TRUTH && typeID != TYPE_PREPROCESSED && typeID != TYPE_DIGITIZED) {
            throw new IllegalArgumentException("Unrecognized plot category ID \"" + typeID + "\".");
        }
        if(topBot != TOP && topBot != BOT) {
            throw new IllegalArgumentException("Unrecognized top/bottom ID \"" + topBot + "\".");
        }
        if(layer != L1 && layer != L2) {
            throw new IllegalArgumentException("Unrecognized layer ID \"" + layer + "\".");
        }
        if(hole != HOLE1 && hole != HOLE2 && hole != HOLE_NULL) {
            throw new IllegalArgumentException("Unrecognized hole ID \"" + hole + "\".");
        }
        
        // Switch to units of MeV.
        energy = energy * 1000;
        
        // Fill the all hits plot.
        aida.histogram1D(getEnergyDistPlotName(typeID)).fill(energy);
        
        // Fill the layer-appropriate position plot.
        aida.histogram2D(getPositionDistPlotName(typeID, layer)).fill(ix + 1, layer == TOP ? 1 : -1);
        
        // Fill the specific plots.
        aida.histogram1D(getEnergyDistPlotName(typeID, topBot, layer)).fill(energy);
        aida.histogram1D(getEnergyDistPlotName(typeID, ix, layer, topBot, hole)).fill(energy);
    }
    
    /**
     * Gets the energy for a {@link org.lcsim.event.CalorimeterHit
     * CalorimeterHit} object. This method is used as part of the
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     * fillPlots(int, Collection, Class)} method.
     * @param hit - The hit.
     * @return Returns the energy in GeV.
     * @see org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     */
    private static final Double getEnergy(CalorimeterHit hit) {
        return Double.valueOf(hit.getRawEnergy());
    }
    
    /**
     * Gets the energy for a {@link org.lcsim.event.SimCalorimeterHit
     * SimCalorimeterHit} object. This method is used as part of the
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     * fillPlots(int, Collection, Class)} method.
     * @param hit - The hit.
     * @return Returns the energy in GeV.
     * @see org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     */
    @SuppressWarnings("unused")
    private static final Double getEnergy(SimCalorimeterHit hit) {
        return getEnergy((CalorimeterHit) hit);
    }
    
    /**
     * Gets the energy for a {@link org.lcsim.event.SimTrackerHit
     * SimTrackerHit} object. This method is used as part of the
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     * fillPlots(int, Collection, Class)} method.
     * @param hit - The hit.
     * @return Returns the energy in GeV.
     * @see org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     */
    @SuppressWarnings("unused")
    private static final Double getEnergy(SimTrackerHit hit) {
        return Double.valueOf(hit.getdEdx());
    }
    
    /**
     * Gets the name of the "all hits" energy plot for the indicated
     * plot type.
     * @param typeID - The type ID of the plot. May only be one of
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_TRUTH
     * TYPE_TRUTH}, {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_PREPROCESSED
     * TYPE_PREPROCESSED}, or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_DIGITIZED
     * TYPE_DIGITIZED}.
     * @return Returns the plot name.
     * @throws IllegalArgumentException Occurs if <code>typeID</code>
     * is not a supported value.
     */
    private static final String getEnergyDistPlotName(int typeID) throws IllegalArgumentException {
        // If an invalid type is given, throw an exception.
        if(typeID != TYPE_TRUTH && typeID != TYPE_PREPROCESSED && typeID != TYPE_DIGITIZED) {
            throw new IllegalArgumentException("Unrecognized plot category ID \"" + typeID + "\".");
        }
        
        // Return the plot name.
        return TYPE_FOLDER[typeID] + ENERGY_FOLDER + "Energy Distribution (All Hits)";
    }
    
    /**
     * Gets the name of the "top/bottom and layer hits" plots for the
     * indicated plot type and top/bottom and layer combination.
     * @param typeID - The type ID of the plot. May only be one of
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_TRUTH
     * TYPE_TRUTH}, {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_PREPROCESSED
     * TYPE_PREPROCESSED}, or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_DIGITIZED
     * TYPE_DIGITIZED}.
     * @param topBot - Whether the plot is for the top or bottom of
     * the hodoscope. This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TOP
     * TOP} or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#BOT BOT}.
     * @param layer - Whether the plot is for layer 1 or layer 2.
     * This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#L1 L1} or
     * {@link org.hps.analysis.hodoscope.HodoscopePlotsDriver#L2 L2}.
     * @return Returns the plot name.
     * @throws IllegalArgumentException Occurs if
     * <code>typeID</code>, <code>topBot</code>, or
     * <code>layer</code> is not a supported value.
     */
    private static final String getEnergyDistPlotName(int typeID, int topBot, int layer) throws IllegalArgumentException {
        // If invalid arguments are given, throw an exception.
        if(typeID != TYPE_TRUTH && typeID != TYPE_PREPROCESSED && typeID != TYPE_DIGITIZED) {
            throw new IllegalArgumentException("Unrecognized plot category ID \"" + typeID + "\".");
        }
        if(topBot != TOP && topBot != BOT) {
            throw new IllegalArgumentException("Unrecognized top/bottom ID \"" + topBot + "\".");
        }
        if(layer != L1 && layer != L2) {
            throw new IllegalArgumentException("Unrecognized layer ID \"" + layer + "\".");
        }
        
        // Return the plot name.
        return TYPE_FOLDER[typeID] + ENERGY_FOLDER + "Energy Distribution (Layer " + (layer + 1) + " " + TOP_BOT_TEXT[topBot] + " Hits)";
    }
    
    /**
     * Gets the plot name for the "specific scintillator" plots for a
     * given scintillator.
     * @param typeID - The type ID of the plot. May only be one of
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_TRUTH
     * TYPE_TRUTH}, {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_PREPROCESSED
     * TYPE_PREPROCESSED}, or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_DIGITIZED
     * TYPE_DIGITIZED}.
     * @param ix - The x-index of the scintillator.
     * @param topBot - Whether the plot is for the top or bottom of
     * the hodoscope. This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TOP
     * TOP} or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#BOT BOT}.
     * @param layer - Whether the plot is for layer 1 or layer 2.
     * This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#L1 L1} or
     * {@link org.hps.analysis.hodoscope.HodoscopePlotsDriver#L2 L2}.
     * @param hole - Which optic fiber hole in the scintillator is
     * the plot is for. This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#HOLE1 HOLE1}
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#HOLE2 HOLE2},
     * or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#HOLE_NULL
     * HOLE_NULL}.
     * @return Returns the plot name.
     * @throws IllegalArgumentException Occurs if
     * <code>typeID</code>, <code>topBot</code>,
     * <code>layer</code>, or <code>hole</code> is not a supported
     * value.
     */
    private static final String getEnergyDistPlotName(int typeID, int ix, int topBot, int layer, int hole) throws IllegalArgumentException {
        // If invalid arguments are given, throw an exception.
        if(typeID != TYPE_TRUTH && typeID != TYPE_PREPROCESSED && typeID != TYPE_DIGITIZED) {
            throw new IllegalArgumentException("Unrecognized plot category ID \"" + typeID + "\".");
        }
        if(topBot != TOP && topBot != BOT) {
            throw new IllegalArgumentException("Unrecognized top/bottom ID \"" + topBot + "\".");
        }
        if(layer != L1 && layer != L2) {
            throw new IllegalArgumentException("Unrecognized layer ID \"" + layer + "\".");
        }
        if(hole != HOLE1 && hole != HOLE2 && hole != HOLE_NULL) {
            throw new IllegalArgumentException("Unrecognized hole ID \"" + hole + "\".");
        }
        
        // Return the plot name.
        String holeText = hole == HOLE_NULL ? "" : " Hole " + (hole + 1);
        return TYPE_FOLDER[typeID] + ENERGY_FOLDER + CHANNEL_FOLDER
                + "Energy Distribution (Layer " + (layer + 1) + " " + TOP_BOT_TEXT[topBot] + " Scintillator " + (ix + 1) + holeText + " Hits)";
    }
    
    /**
     * Gets the indices for a {@link org.lcsim.event.CalorimeterHit
     * CalorimeterHit} object. This method is used as part of the
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     * fillPlots(int, Collection, Class)} method.
     * @param hit - The hit.
     * @return Returns the indices in the form of <code>{ x-index,
     * top/bottom, layer, hole number }</code>.
     * @see org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     */
    private final int[] getIndices(CalorimeterHit hit) {
        HodoscopeChannel channel = channelIDMap.get(Long.valueOf(hit.getCellID()));
        int layer = channel.isLayer1() ? L1 : L2;
        int topBot = channel.isTop() ? TOP : BOT;
        int ix = channel.getIX();
        int hole = channel.getHole() == HodoscopeChannel.HOLE_LOW_X ? HOLE1 : HOLE2;
        return new int[] { ix, topBot, layer, hole };
    }
    
    /**
     * Gets the indices for a {@link
     * org.lcsim.event.SimCalorimeterHit SimCalorimeterHit} object.
     * This method is used as part of the {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     * fillPlots(int, Collection, Class)} method.
     * @param hit - The hit.
     * @return Returns the indices in the form of <code>{ x-index,
     * top/bottom, layer, hole number }</code>.
     * @see org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     */
    @SuppressWarnings("unused")
    private final int[] getIndices(SimCalorimeterHit hit) {
        return getIndices((CalorimeterHit) hit);
    }
    
    /**
     * Gets the indices for a {@link org.lcsim.event.SimTrackerHit
     * SimTrackerHit} object. This method is used as part of the
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     * fillPlots(int, Collection, Class)} method.
     * @param hit - The hit.
     * @return Returns the indices in the form of <code>{ x-index,
     * top/bottom, layer, hole number }</code>.
     * @see org.hps.analysis.hodoscope.HodoscopePlotsDriver#fillPlots(int, Collection, Class)
     */
    @SuppressWarnings("unused")
    private final int[] getIndices(SimTrackerHit hit) {
        int[] indices = hodoscopeDetectorElement.getHodoscopeIndices(hit);
        int layer = indices[2] == 0 ? L1 : L2;
        int topBot = indices[1] == 1 ? TOP : BOT;
        int ix = indices[0];
        return new int[] { ix, topBot, layer, HOLE_NULL };
    }
    
    /**
     * Gets the name of the "all hits" energy plot for the indicated
     * plot type.
     * @param typeID - The type ID of the plot. May only be one of
     * {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_TRUTH
     * TYPE_TRUTH}, {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_PREPROCESSED
     * TYPE_PREPROCESSED}, or {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#TYPE_DIGITIZED
     * TYPE_DIGITIZED}.
     * @param layer - Whether the plot is for layer 1 or layer 2.
     * This must correspond to either {@link
     * org.hps.analysis.hodoscope.HodoscopePlotsDriver#L1 L1} or
     * {@link org.hps.analysis.hodoscope.HodoscopePlotsDriver#L2 L2}.
     * @return Returns the plot name.
     * @throws IllegalArgumentException Occurs if <code>typeID</code>
     * or <code>layer</code> is not a supported value.
     */
    private static final String getPositionDistPlotName(int typeID, int layer) throws IllegalArgumentException {
        // If an invalid type is given, throw an exception.
        if(typeID != TYPE_TRUTH && typeID != TYPE_PREPROCESSED && typeID != TYPE_DIGITIZED) {
            throw new IllegalArgumentException("Unrecognized plot category ID \"" + typeID + "\".");
        }
        if(layer != L1 && layer != L2) {
            throw new IllegalArgumentException("Unrecognized layer ID \"" + layer + "\".");
        }
        
        // Return the plot name.
        return TYPE_FOLDER[typeID] + POSITION_FOLDER + "Position Distribution (Layer " + (layer + 1) + ")";
    }
    
    /**
     * Creates an energy plot with the default parameters for a
     * hodoscope hit.
     * @param plotName - The name of the plot.
     * @return Returns the newly created plot.
     */
    private static final IHistogram1D makeEnergyPlot(String plotName) {
        return aida.histogram1D(plotName, 250, 0.000, 5.000);
    }
    
    /**
     * Creates a position plot with the default parameters for a
     * hodoscope hit.
     * @param plotName - The name of the plot.
     * @return Returns the newly created plot.
     */
    private static final IHistogram2D makePositionPlot(String plotName) {
        return aida.histogram2D(plotName, 6, 0.5, 5.5, 3, -1.5, 1.5);
    }
    
    /**
     * Populates the channel ID set and maps all existing channels to
     * their respective conditions.
     */
    private void populateChannelCollections() {
        // Load the conditions database and get the hodoscope channel
        // collection data.
        final DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        final HodoscopeChannelCollection channels = conditions.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        
        // Store the set of all channel IDs.
        for(HodoscopeChannel channel : channels) {
            channelIDMap.put(Long.valueOf(channel.getChannelId().intValue()), channel);
        }
    }
}