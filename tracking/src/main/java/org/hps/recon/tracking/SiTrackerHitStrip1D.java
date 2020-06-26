package org.hps.recon.tracking; 

import java.util.List;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiStriplets; 
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;


/**
 * Class describing a 1D cluster.  This class inherits 
 * {@link SiTrackerHitStrip1D} in lcsim but overrides methods that require the 
 * use of both SiStrips and SiStriplets. 
 *
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class SiTrackerHitStrip1D extends org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D {
    
    /**
     * Creates a new instance of SiTrackerHitStrip1D
     */
    public SiTrackerHitStrip1D(Hep3Vector position, 
            SymmetricMatrix covarianceMatrix, double energy, double time, 
            List<RawTrackerHit> rawHits, TrackerHitType decodedType) {
        super(position, covarianceMatrix, energy, time, rawHits, decodedType);
    }

    public SiTrackerHitStrip1D(TrackerHit hit) {
        super(hit);
    }
    
    public SiTrackerHitStrip1D(TrackerHit hit, TrackerHitType.CoordinateSystem coordinateSystem) {
        super(hit,coordinateSystem);
    }


    @Override 
    public double getHitLength() {
        
        double maxHitLength = 0;
        for (RawTrackerHit rawHit : getRawHits()) {
            
            int cellID = getIdentifierHelper().getElectrodeValue(rawHit.getIdentifier()); 
            
            SiSensorElectrodes electrodes = getReadoutElectrodes(); 
            
            double hitLength = 0;
            if (electrodes instanceof SiStrips) hitLength = ((SiStrips) electrodes).getStripLength(cellID); 
            else if (electrodes instanceof SiStriplets) hitLength = ((SiStriplets) electrodes).getStripLength(cellID); 
            
            maxHitLength = Math.max( maxHitLength, hitLength );
            
        }
        
        //System.out.println("SiTrackerHitStrip1D::getHitLength : Hit length: " + maxHitLength);
        
        return maxHitLength;

    }

}
