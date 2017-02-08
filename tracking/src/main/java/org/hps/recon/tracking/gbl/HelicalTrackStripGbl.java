package org.hps.recon.tracking.gbl;

import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.hps.recon.tracking.CoordinateTransformations;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;

/**
 * Encapsulates the {@link org.lcsim.fit.helicaltrack.HelicalTrackStrip} to make sure that the local unit vectors are
 * coming from the underlying geometry.
 */
public  class HelicalTrackStripGbl { 
    
    protected HelicalTrackStrip _strip;
    private SiSensorElectrodes _electrodes = null;
    private Hep3Matrix _electrodesToTracking = null; 
    private Hep3Vector _u = null;
    private Hep3Vector _v = null;
    private Hep3Vector _w = null;
    private boolean _useGeomDef = false;
    
    public HelicalTrackStripGbl(HelicalTrackStrip strip, boolean useGeomDef) {
        _strip = strip;
        _useGeomDef = useGeomDef;
    }
    
    public double du() {
       return _strip.du();
    }
    
    public double vmin() {
       return _strip.vmin();
    }
    
    public double vmax() {
       return _strip.vmax();
    }
    
    public double umeas() {
       return _strip.umeas();
    }
    
    public HelicalTrackStrip getStrip() {
        return _strip;
    }
    
    public Hep3Vector origin() {
       return _strip.origin();
    }
    
    public int layer() {
       return _strip.layer();
    }
    
    private SiSensorElectrodes getElectrodes() {
        if(_electrodes==null) {
            RawTrackerHit rth = (RawTrackerHit) _strip.rawhits().get(0);
            IDetectorElement ide = rth.getDetectorElement();
            SiSensor sensor = ide.findDescendants(SiSensor.class).get(0);
            _electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);   
        }
        return _electrodes;
    }
    
    private Hep3Matrix getElectrodeToTrackingMatrix() {
        if(_electrodesToTracking==null) {
            SiSensorElectrodes electrodes = getElectrodes();
            _electrodesToTracking = VecOp.mult(CoordinateTransformations.getMatrix(), electrodes.getLocalToGlobal().getRotation().getRotationMatrix());
        }
        return _electrodesToTracking;
    }
    
    public Hep3Vector u() {
        if(_u == null) {
            if(_useGeomDef) {
                _u = VecOp.mult(getElectrodeToTrackingMatrix(), getElectrodes().getMeasuredCoordinate(0));
            }
            else {
                _u = _strip.u();
            }
        }
        return _u;
    }
    
    public Hep3Vector v() {
        if(_v == null) {
            if(_useGeomDef) {
                _v = VecOp.mult(getElectrodeToTrackingMatrix(), getElectrodes().getUnmeasuredCoordinate(0));
            } else {
                _v = _strip.v();
            }
        }
        return _v;
    }
    
    public Hep3Vector w() {
        if(_w==null) {
            _w = VecOp.cross(u(), v());
        }
        return _w;
    }
       
    @Override
    public String toString() {
        return ("GBl Strip with u="+u().toString()+"\n v="+v().toString()+ "\n w="+w().toString() + "\n vmin="+vmin() + "\n vmax="+vmax() + "\n umeas="+this.umeas()+"\n origin="+this.origin().toString()); 
    }
}