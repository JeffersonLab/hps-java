package org.hps.detector.ecal;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IDetectorElementContainer;
import org.lcsim.detector.converter.compact.SubdetectorDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;

/**
 * <p>
 * This is an implementation of a basic geometry API for the HPS ECAL.
 * <p>
 * The neighboring API and conventions are based on the page 7 diagram from the 
 * <a href="https://wiki.jlab.org/hps-run/images/f/f4/Ecal_manual_annex.pdf">ECAL Manual Annex</a>
 * in which the viewpoint is from the beam towards the detector.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * 
 * @see HPSEcalAPI
 * @see EcalCrystal
 * @see org.lcsim.detector.IDetectorElement
 * @see SubdetectorDetectorElement
 */
public final class HPSEcalDetectorElement extends SubdetectorDetectorElement implements HPSEcalAPI {
        
    private Map<EcalCrystal, List<EcalCrystal>> neighborMap;
    
    private int xIndexMax = Integer.MIN_VALUE;
    private int xIndexMin = Integer.MAX_VALUE;
    private int yIndexMax = Integer.MIN_VALUE;
    private int yIndexMin = Integer.MAX_VALUE;
    
    private List<Integer> xIndices;
    private List<Integer> yIndices;
    
    private CrystalRange beamGap;
                            
    public HPSEcalDetectorElement(String name, IDetectorElement parent) {
        super(name, parent);
    }
    
    /**
     * Set the index range for the beam gap.
     * @param beamGap The beam gap index range.
     */
    public void setBeamGapIndices(CrystalRange beamGap) {
        this.beamGap = beamGap;
    }
    
    @Override
    public int getXIndexMax() {
        return xIndexMax;
    }

    @Override
    public int getXIndexMin() {
        return xIndexMin;
    }

    @Override
    public int getYIndexMax() {
        return yIndexMax;
    }

    @Override
    public int getYIndexMin() {
        return yIndexMin;
    }
    
    @Override
    public List<Integer> getXIndices() {
        return Collections.unmodifiableList(xIndices);
    }

    @Override
    public List<Integer> getYIndices() {
        return Collections.unmodifiableList(yIndices);
    }
    
    @Override
    public boolean isInBeamGap(int xIndex, int yIndex) {
        if((xIndex >= beamGap.xIndexMin && xIndex <= beamGap.xIndexMax) && 
                (yIndex >= beamGap.yIndexMin && yIndex <= beamGap.yIndexMax)) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public List<EcalCrystal> getRow(int yIndex) {
        List<EcalCrystal> row = new ArrayList<EcalCrystal>();
        for (int ix = xIndexMin; ix <= xIndexMax; ix++) {
            if (ix == 0)
                continue;
            EcalCrystal crystal = getCrystal(ix, yIndex);
            if (crystal != null) {
                row.add(crystal);
            }
        }
        return row;
    }
    
    @Override
    public List<EcalCrystal> getColumn(int xIndex) {
        List<EcalCrystal> column = new ArrayList<EcalCrystal>();
        for (int iy = yIndexMin; iy <= yIndexMax; iy++) {
            if (iy == 0)
                continue;
            EcalCrystal crystal = getCrystal(xIndex, iy);
            if (crystal != null) {
                column.add(crystal);
            }           
        }
        return column;
    }
                                         
    @Override
    public EcalCrystal getCrystal(int xIndex, int yIndex) {        
        IIdentifierHelper helper = getIdentifierHelper();
        IExpandedIdentifier expId = helper.createExpandedIdentifier();
        expId.setValue(helper.getFieldIndex("ix"), xIndex);
        expId.setValue(helper.getFieldIndex("iy"), yIndex);
        expId.setValue(helper.getFieldIndex("system"), getSystemID());
        return getCrystal(helper.pack(expId));
    }
    
    @Override
    public EcalCrystal getCrystal(IIdentifier id) {
        IDetectorElementContainer de = findDetectorElement(id);
        return de.isEmpty() ? null : (EcalCrystal) de.get(0);
    }    
          
    @Override
    public EcalCrystal getCrystal(Hep3Vector position) {
        IDetectorElement de = findDetectorElement(position);
        if (de instanceof EcalCrystal) {
            return (EcalCrystal) de;        
        } else {
            return null;
        }
    }

    @Override
    public List<EcalCrystal> getNeighbors(EcalCrystal crystal) {
        return neighborMap.get(crystal);
    }
           
    @Override
    public List<EcalCrystal> getCrystals() {
        return findDescendants(EcalCrystal.class);
    }
            
    @Override
    public void initialize() {
        computeIndexRanges();
        createNeighborMap();
    }
    
    void computeIndexRanges() {
        for (EcalCrystal crystal : getCrystals()) {
            if (crystal.getX() > xIndexMax) {
                xIndexMax = crystal.getX();
            }
            if (crystal.getX() < xIndexMin) {
                xIndexMin = crystal.getX();
            }
            if (crystal.getY() > yIndexMax) {
                yIndexMax = crystal.getY();
            }
            if (crystal.getY() < yIndexMin) {
                yIndexMin = crystal.getY();
            }
        }
                
        xIndices = new ArrayList<Integer>();
        for (int ix = xIndexMin; ix <= xIndexMax; ix++) {
            if (ix == 0)
                continue;
            xIndices.add(ix);
        }
        
        yIndices = new ArrayList<Integer>();
        for (int iy = yIndexMin; iy <= yIndexMax; iy++) {
            if (iy == 0)
                continue;
            yIndices.add(iy);
        }
    }
    
    // Constants for neighboring, relative to the beam direction as per diagram.
    enum NeighborDirection {
        NORTH,
        NORTHEAST,
        EAST,
        SOUTHEAST,
        SOUTH,
        SOUTHWEST,
        WEST,
        NORTHWEST
    }   
    
    /**
     * Create a map of a crystal to its adjacent neighbors in all eight cardinal directions.
     * Non-existent crystals are filtered out if the the geometry object does not exist, 
     * which automatically takes care of edge crystals and missing crystals from the beam gap
     * without explicitly needing to check those indices for validity.
     */
    private void createNeighborMap() {
        neighborMap = new HashMap<EcalCrystal, List<EcalCrystal>>();
        for (EcalCrystal crystal : getCrystals()) {            
            List<EcalCrystal> neighborCrystals = new ArrayList<EcalCrystal>();
            for (NeighborDirection neighborDirection : NeighborDirection.values()) {
                int[] xy = getNeighborIndices(crystal, neighborDirection);
                EcalCrystal neighborCrystal = getCrystal(xy[0], xy[1]);
                if (neighborCrystal != null) {
                    neighborCrystals.add(neighborCrystal);
                } 
            }            
            neighborMap.put(crystal, neighborCrystals);
        }
    }
              
    /**     
     * Get the neighbor indices of a crystal.
     * @param crystal The ECAL crystal geometry object.
     * @param direction The direction of the neighbor from integer encoding.
     * @return The neighbor indices.
     */   
    private static int[] getNeighborIndices(EcalCrystal crystal, NeighborDirection direction) {
        int[] xy = new int[2];
        int ix = crystal.getX();
        int iy = crystal.getY();
        switch (direction) {
            case NORTH:
                xy[0] = ix;
                xy[1] = iy + 1;
                break;
            case NORTHEAST:
                xy[0] = ix - 1;
                if (xy[0] == 0) xy[0] = -1;
                xy[1] = iy + 1;
                break;          
            case EAST:
                xy[0] = ix - 1;
                if (xy[0] == 0) xy[0] = -1;
                xy[1] = iy;                
                break;
            case SOUTHEAST:
                xy[0] = ix - 1;
                if (xy[0] == 0) xy[0] = -1;
                xy[1] = iy - 1;
                break;
            case SOUTH:
                xy[0] = ix;
                xy[1] = iy - 1;
                break;
            case SOUTHWEST:
                xy[0] = ix + 1;
                if (xy[0] == 0) xy[0] = 1;
                xy[1] = iy - 1;
                break;
            case WEST:
                xy[0] = ix + 1;
                if (xy[0] == 0) xy[0] = 1;
                xy[1] = iy;
                break;
            case NORTHWEST:
                xy[0] = ix + 1;
                if (xy[0] == 0) xy[0] = 1;
                xy[1] = iy + 1;
                break;
        }
        return xy;
    }
  
}