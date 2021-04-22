package org.lcsim.detector.tracker.silicon; 

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;

/**
 * Class describing striplet electrodes of a silicon sensor. This class extends
 * {@link SiPixels} but overrides the calculation of the strip ID and provides 
 * methods needed to create 1D strip hits. 
 */
public class SiStriplets extends SiPixels { 

    final int CHANNEL_MAP_OFFSET = 1; 

    /**
     * Constructor.
     *
     * @param carrier 
     * @param rowPitch
     * @param colPitch
     * @param detector
     * @param parentToLocal 
     */
    public SiStriplets(ChargeCarrier carrier, double rowPitch, double colPitch, 
            IDetectorElement detector, ITransform3D parentToLocal) { 
        super(carrier, rowPitch, colPitch, detector, parentToLocal); 
    }

    /** Informations:
     *
     * NOTE:: cellID in this class IS NOT the RawHitTracker getCellID()
     * In this class, CellID is the strip number.
     * For example the strip number of and hit associated to a *sensor*
     * can be retrieved from a RawTrackerHit by using the decoder. See Snippet below
     *
     * Imports needed: 
     * import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper; 
     *
     * Snippet:
     * SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
     * int strip = _sid_helper.getElectrodeValue(hit.getIdentifier());
     *
     * Then strip can be used as cellID in this class to get row/columns/position 
     *
     **/
    @Override
    public int getCellID(int row, int column) { 
        if (row < 0 || row >= getNCells(0)) return -1; 
        
        if (column < 0 || column >= getNCells(1)) return -1; 

        int id = (column == 0) ? row + CHANNEL_MAP_OFFSET : ((column + 1)*getNCells(0) - row); 
       
        return id;
    }

    
    @Override 
    public int getRowNumber(int cellID) {
        int row = (getColumnNumber(cellID) == 0) ? (cellID - CHANNEL_MAP_OFFSET): (getColumnNumber(cellID) + 1)*getNCells(0) - cellID;
       
        return row;      
    }

    @Override
    public int getColumnNumber(int cellID) {
        int col = (int) Math.floor((cellID - CHANNEL_MAP_OFFSET)/getNCells(0));  
        return col; 
    }

    @Override
    public int getRowNumber(Hep3Vector position) {
        int row = super.getRowNumber(position);
        return row;
    }

    public Hep3Vector getStripCenter(int cellID) { 

        double u = getRowNumber(cellID)*_row_pitch - _row_offset;   
        double v = getColumnNumber(cellID)*_col_pitch - _col_offset; 

        return new BasicHep3Vector(u, v, 0.0); 
    }

    public double getStripLength(int cellID) {
        return _col_pitch; 
    }
    
}

