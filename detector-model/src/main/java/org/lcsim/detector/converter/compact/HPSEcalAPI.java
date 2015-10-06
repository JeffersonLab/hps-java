package org.lcsim.detector.converter.compact;

import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.lcsim.detector.identifier.IIdentifier;

/**
 * This is a geometry API for the HPS ECAL detector.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface HPSEcalAPI {
    
    /**
     * Get the maximum X index of the crystals.
     * @return The maximum X index of the crystals.
     */
    int getXIndexMax();
    
    /**
     * Get the minimum X index of the crystals.
     * @return The minimum X index of the crystals.
     */
    int getXIndexMin();
    
    /**
     * Get the maximum Y index of the crystals.
     * @return The maximum Y index of the crystals.
     */
    int getYIndexMax();
    
    /**
     * Get the minimum Y index of the crystals.
     * @return The minimum Y index of the crystals.
     */
    int getYIndexMin();
    
    /**
     * Get an array with all the valid X indices.
     * @return An array with the X indices.
     */
    List<Integer> getXIndices();
    
    /**
     * Get an array with all the valid Y indices.
     * @return An array with the Y indices.
     */
    List<Integer> getYIndices();
    
    /**
     * True if the given indices are located in the beam gap
     * and so do not have crystals associated with them.
     * @param x The X index.
     * @param y The Y index.
     * @return True if indices reference a position in the beam gap.
     */
    boolean isInBeamGap(int x, int y);
       
    /**
     * Get the crystal at the given indices or null if it does not exist.
     * @param x The X index.
     * @param y The Y index.
     * @return The crystal at the given indices or null if it does not exist.
     */
    EcalCrystal getCrystal(int x, int y);
       
    /**
     * Get the crystal with the given ID or null if it does not exist
     * @param id The packed ID of the crystal.
     * @return The packed ID of the crystal.
     */
    EcalCrystal getCrystal(IIdentifier id);
    
    /**
     * Get the crystal at the given position in global coordinates or null if position
     * is not inside a crystal's volume.
     * @param position The position of the crystal.
     * @return The crystal at the given position or null if position is not inside crystal.
     */
    EcalCrystal getCrystal(Hep3Vector position);
    
    /**
     * Get the list of crystal objects.
     * @return The list of crystal objects.
     */
    List<EcalCrystal> getCrystals();
    
    /**
     * Get a row of crystals from the Y index.
     * @param y The Y index.
     * @return The row of crystals.
     */
    List<EcalCrystal> getRow(int y);
    
    /**
     * Get a column of crystals from the X index.
     * @param x The X index. 
     * @return The column of crystals.
     */
    List<EcalCrystal> getColumn(int x);
            
    /**
     * Get the neighbors of a crystal.
     * @param crystal A crystal object.
     * @return The list of neighbor objects.
     */
    List<EcalCrystal> getNeighbors(EcalCrystal crystal);
}
