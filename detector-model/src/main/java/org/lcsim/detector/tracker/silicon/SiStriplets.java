package org.lcsim.detector.tracker.silicon; 

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;


/**
 * Class describing striplet electrodes of a silicon sensor. This class extends
 * {@link SiPixels} but overrides the calculation of the strip ID and constructs
 * 1D strip hits instead of 2D pixel hits. 
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

}

