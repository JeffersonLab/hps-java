package org.lcsim.detector.tracker.silicon;

import hep.physics.vec.Hep3Vector; 

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.Point3D;

/**
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class ThinSiStrips extends SiStrips {


    private int channelOffset = 256; 

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
    public int getNeighborCell(int cell, int nCells0, int nCells1) { 
        
        if ( (cell == (getNCells()/2 - 1)) && (nCells0 == 1) ) return -1;
        else if ( (cell == getNCells()) && (nCells0 == -1) ) return -1;

        return super.getNeighborCell(cell, nCells0, 0); 
    }


    @Override
    public int getCellID(Hep3Vector position) { 
        System.out.println("[ ThinSiStrips ][ getCellID ]: Local to Global: " + getLocalToGlobal().transformed(position).toString());  
        System.out.println("[ ThinSiStrips ][ getCellID ]: Local to Parent: " + getParentToLocal().inverse().transformed(position).toString()); 
        
        int id = (int)Math.round( (position.x() + _strip_offset)/_pitch); 
        System.out.println("[ ThinSiStrips ][ getCellID ]: ID Before check: " +  id);  
        if (position.y() > 0) id = (getNCells() - id - 1); 
        System.out.println("[ ThinSiStrips ][ getCellID ]: ID After check: " +  id);  
        return id;  
    }

    @Override
    protected void setStripNumbering() {
        
        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        for (Point3D vertex : getGeometry().getVertices()) {
            System.out.println("[ ThinSiStrips ][ setStripNumber ]: Vertex: " + vertex.toString()); 
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
        System.out.println("[ ThinSiStrips ][ setStripOffset ]: ((nStrips/2) - 1)*pitch)/2: " + ( ( (_nstrips/2) - 1)*_pitch)/2);  

        _strip_offset = ( ( (_nstrips/2) - 1)*_pitch)/2 - stripsCenter;

        System.out.println("[ ThinSiStrips ][ setStripOffset ]: Offset: " + _strip_offset);   
    }

    @Override
    public Hep3Vector getCellPosition(int stripNumber) {
       
        System.out.println("[ ThinSiStrips ][ getCellPosition ]: Before strip #: " + stripNumber);  
        //if ( stripNumber >= getNCells()/2 ) stripNumber = (getNCells() - stripNumber - 1);
        System.out.println("[ ThinSiStrips ][ getCellPosition ]: After strip #: " + stripNumber);  
        return super.getCellPosition(stripNumber);  
    }
}
