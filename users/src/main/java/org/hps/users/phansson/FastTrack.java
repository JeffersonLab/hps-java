package org.hps.users.phansson;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;

public class FastTrack {

    private Hep3Vector _startVec;
    private Hep3Vector _endVec;
    private boolean _debug;
    private double _slope;
    private double _const;
    double _beamGapCorr;
   
    
    public FastTrack(boolean debug) {
        _debug = debug; 
        _beamGapCorr = 0.0;
    }
    
    
    
    public FastTrack(Hep3Vector startPos, Hep3Vector endPos, double corr, boolean debug) {
        //create the track using a straight line in the non-bending plane
        //The non-bending plane is y-z:
        //    z is beam direction
        //    x is parallel to axial strips
        //    y is measurement direction/orthogonal to strip direction 
        _debug = debug;
        _beamGapCorr = corr;    
        setTrack(startPos,endPos);
        
    }
    
     public FastTrack(Hep3Vector startPos, double[] endPos, double corr, boolean debug) {
        //create the track using a straight line in the non-bending plane
        //The non-bending plane is y-z:
        //    z is beam direction
        //    x is parallel to axial strips
        //    y is measurement direction/orthogonal to strip direction 
        _debug = debug;
        _beamGapCorr = corr;    
        setTrack(startPos,endPos);
        
    }

     
    public void setTrack(Hep3Vector startPos, double[] endPos) {
        //create the track using a straight line in the non-bending plane
        //The non-bending plane is y-z:
        //    z is beam direction
        //    x is parallel to axial strips
        //    y is measurement direction/orthogonal to strip direction 
        Hep3Vector endPos_vec = new BasicHep3Vector(endPos[0],endPos[1],endPos[2]);
        setTrack(startPos,endPos_vec);
        
    }
     
    
    public double getCorrY(double y) {
        double ynew = y;
        if (y>0) ynew += _beamGapCorr;
        else ynew -= _beamGapCorr;
        return ynew;
    }
      
    public void setTrack(Hep3Vector startPos, Hep3Vector endPos) {
        //create the track using a straight line in the non-bending plane
        //The non-bending plane is y-z:
        //    z is beam direction
        //    x is parallel to axial strips
        //    y is measurement direction/orthogonal to strip direction 
        _startVec = startPos;
        //Correct for a possible beamgap difference in the geometry of the ecal
        double y = getCorrY(endPos.y());
        Hep3Vector endPos_corr = new BasicHep3Vector(endPos.x(),y,endPos.z());
        _endVec = endPos_corr;
        _slope = getSlope();
        _const = getConst();
        
        
    }
     
     
    private double getSlope() {
        double z1 = _startVec.z();  
        double z2 = _endVec.z();  
        double y1 = _startVec.y();  
        double y2 = _endVec.y();  
        return (y2-y1)/(z2-z1);
    }

    private double getConst() {
        return -1.0*(_slope*_startVec.z());  
    }

    
    public double eval(double x) {
        // non-bend plane is y-z
        return _slope*x+_const;
    }
    
    public double getFastTrackResidual(SiTrackerHitStrip1D stripCluster) {
        Hep3Vector posVec = stripCluster.getPositionAsVector();
        double res = posVec.y() - this.eval(posVec.z());
        
        if(_debug) {
            System.out.println("Hit pos " + posVec.toString());
            System.out.println("FastTrack position " + this.eval(posVec.z()) + " and z " + posVec.z() + " --> res " + res);
        }
        return res;        
    }
            
    public void setEcalBeamgapCorr(double offset) {
        this._beamGapCorr = offset;    
    }
    
    public String toString() {
        String s = "FastTrack info: ";
        s += " startPos: " + _startVec.toString() + "\n";
        s += " endPos: " + _endVec.toString() + "\n";
        s += " ECalOffsetY: " + _beamGapCorr + "\n";
        s += "slope: " + _slope;
        s += "const: " + _const;
        return s;
    }
    
}
