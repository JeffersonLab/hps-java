package org.lcsim.detector.tracker.silicon;

//import hep.physics.vec.Hep3Vector; 

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.Point3D;

/**
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class ThinSiStrips extends SiStrips {


    /**
     * Constructor
     */
    public ThinSiStrips(ChargeCarrier carrier, double pitch, IDetectorElement detector, ITransform3D parentToLocal) { 
        super(carrier, pitch, detector, parentToLocal);  
    }

    /**
     *
     */
    public ThinSiStrips(ChargeCarrier carrier, double pitch, int nStrips, IDetectorElement detector, ITransform3D parentToLocal) {
        super(carrier, pitch, nStrips, detector, parentToLocal); 
    }

    @Override
    protected void setStripNumbering() {
        
        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        for (Point3D vertex : getGeometry().getVertices()) {
            xmin = Math.min(xmin,vertex.x());
            xmax = Math.max(xmax,vertex.x());
        }

        System.out.println("[ ThinSiStrips ][ setStripNumbering ]: x min: " + xmin);
        System.out.println("[ ThinSiStrips ][ setStripNumbering ]: x max: " + xmax);

        int nStrips = 2*((int) Math.ceil( (xmax - xmin)/getPitch(0) ));
    
        System.out.println("[ ThinSiStrips ][ setStripNumbering ]: Number of strips: " + nStrips); 

        super.setNStrips(nStrips); 
    }

    @Override
    protected void setStripOffset() { 
        
        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        for (Point3D vertex : getGeometry().getVertices()) {
            xmin = Math.min(xmin,vertex.x());
            xmax = Math.max(xmax,vertex.x());
        }

        System.out.println("[ ThinSiStrips ][ setStripOffset ]: x min: " + xmin);
        System.out.println("[ ThinSiStrips ][ setStripOffset ]: x max: " + xmax);
        
        double stripsCenter = (xmin+xmax)/2;
        System.out.println("[ ThinSiStrips ][ setStripOffset ]: strips center: " + stripsCenter); 

        _strip_offset = ( ( (_nstrips/2) - 1)*_pitch)/2 - stripsCenter;

        System.out.println("[ ThinSiStrips ][ setStripOffset ]: Offset: " + _strip_offset);  
        
    }
}
