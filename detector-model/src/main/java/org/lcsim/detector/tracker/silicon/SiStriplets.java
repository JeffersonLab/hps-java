package org.lcsim.detector.tracker.silicon; 

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;

/**
 * Class describing striplet electrodes of a silicon sensor. This class extends
 * {@link SiPixels} but overrides the calculation of the strip ID and provides 
 * methods needed to create 1D strip hits. 
 *
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class SiStriplets extends SiPixels { 

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

    @Override
    public int getCellID(int row, int column) { 
        
        if (row < 0 || row >= getNCells(0)) return -1; 
        
        if (column < 0 || column >= getNCells(1)) return -1; 

        int id = (column == 0) ? row : ((column + 1)*getNCells(0) - row); 
        
        return id;
    }

    
    @Override 
    public int getRowNumber(int cellID) {
        
        int row = (getColumnNumber(cellID) == 0) ? cellID : (getColumnNumber(cellID) + 1)*getNCells(0) - cellID;
        
        return row;      
    }

    @Override
    public int getColumnNumber(int cellID) {
        return (int) Math.floor(cellID/getNCells(0));  
    }

    public Hep3Vector getStripCenter(int cellID) { 

        //System.out.println("SiStriplets::getStripCenter : Row Offset: " + _row_offset); 
        //System.out.println("SiStriplets::getStripCenter : Cell ID: " + cellID); 
        //System.out.println("SiStriplets::getStripCenter : Row: " + getRowNumber(cellID));  
        double u = getRowNumber(cellID)*_row_pitch - _row_offset;   
        //System.out.println("SiStriplets::getStripCenter : u: " + u);

        double v = getColumnNumber(cellID)*_col_pitch - _col_offset; 
        //System.out.println("SiStriplets::getStripCenter : v: " + v);

        return new BasicHep3Vector(u, v, 0.0); 
    }

    public double getStripLength(int cellID) {
        return _col_pitch; 
    }
    
}

