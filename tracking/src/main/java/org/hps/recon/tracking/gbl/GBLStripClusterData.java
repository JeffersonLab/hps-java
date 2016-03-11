package org.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.GenericObject;

/**
 * A class providing for storing strip clusters for GBL
 * 
 * @author phansson
 *
 * @version $Id:
 */
public class GBLStripClusterData implements GenericObject {
    
    /*
     * 
     * Interface enumerator to access the correct data
     * 
     */
    public static class GBLINT {
        public static final int ID = 0;
        public static final int BANK_INT_SIZE = 1;
    }
    public static class GBLDOUBLE {
        public static final int PATH3D = 0;
        public static final int PATH = 1;
        public static final int UX = 2;
        public static final int UY = 3;
        public static final int UZ = 4;
        public static final int VX = 5;
        public static final int VY = 6;
        public static final int VZ = 7;
        public static final int WX = 8;
        public static final int WY = 9;
        public static final int WZ = 10;    
        public static final int TDIRX = 11; 
        public static final int TDIRY = 12; 
        public static final int TDIRZ = 13; 
        public static final int TPHI = 14;  
        public static final int UMEAS = 15; 
        public static final int TPOSU = 16  ;   
        public static final int TPOSV = 17; 
        public static final int TPOSW = 18; 
        public static final int UMEASERR = 19;  
        public static final int MSANGLE = 20;
        public static final int TLAMBDA = 21;
        
        
        public static final int BANK_DOUBLE_SIZE = 22;
        
    }
    // array holding the integer data
    private int bank_int[] = new int[GBLINT.BANK_INT_SIZE];
    // array holding the double data
    private double bank_double[] = new double[GBLDOUBLE.BANK_DOUBLE_SIZE];
    
    /**
     * Default constructor
     */
    public GBLStripClusterData(int id) {
        setId(id);
    }
        
        /*
        * Constructor from GenericObject
        * TODO add size checks for backwards compatability
        */
        public GBLStripClusterData(GenericObject o)
        {
            for(int i=0; i<GBLINT.BANK_INT_SIZE; ++i)
            {
                bank_int[i] = o.getIntVal(i);
            }
            for(int i=0; i<GBLDOUBLE.BANK_DOUBLE_SIZE; ++i)
            {
                bank_double[i] = o.getDoubleVal(i);
            }
            
        }
    
    /**
     * @param val set track id to val
     */
    public void setId(int val) {
        bank_int[GBLINT.ID] = val;
    }
    
    /**
     * @return track id for this object
     */
    public int getId() {
        return this.getIntVal(GBLINT.ID);
    }
    
    /**
     * Set path length to this strip cluster
     * @param val
     */
    public void setPath(double val) {
        bank_double[GBLDOUBLE.PATH] = val;
    }
    
    /**
     * Get path length to this strip cluster
     */
    public double getPath() {
        return getDoubleVal(GBLDOUBLE.PATH);
    }

    /**
     * Set path length to this strip cluster
     * @param val
     */
    public void setPath3D(double val) {
        bank_double[GBLDOUBLE.PATH3D] = val;
    }
    
    /**
     * Get path length to this strip cluster
     */
    public double getPath3D() {
        return getDoubleVal(GBLDOUBLE.PATH3D);
    }


    /**
     *  Set and get u vector for this strip sensor
     */
    public void setU(Hep3Vector u) {
        bank_double[GBLDOUBLE.UX] = u.x();
        bank_double[GBLDOUBLE.UY] = u.y();
        bank_double[GBLDOUBLE.UZ] = u.z();      
    }
    public Hep3Vector getU() {
        return new BasicHep3Vector(getUx(),getUy(),getUz());
    }
    public double getUx() {
        return getDoubleVal(GBLDOUBLE.UX);
    }
    public double getUy() {
        return getDoubleVal(GBLDOUBLE.UY);
    }
    public double getUz() {
        return getDoubleVal(GBLDOUBLE.UZ);
    }
    
    /**
     *  Set and get v vector for this strip sensor
     */

    public void setV(Hep3Vector v) {
        bank_double[GBLDOUBLE.VX] = v.x();
        bank_double[GBLDOUBLE.VY] = v.y();
        bank_double[GBLDOUBLE.VZ] = v.z();      
    }
    public Hep3Vector getV() {
        return new BasicHep3Vector(getVx(),getVy(),getVz());
    }
    public double getVx() {
        return getDoubleVal(GBLDOUBLE.VX);
    }
    public double getVy() {
        return getDoubleVal(GBLDOUBLE.VY);
    }
    public double getVz() {
        return getDoubleVal(GBLDOUBLE.VZ);
    }

    /**
     *  Set and get w vector for this strip sensor
     */

    public void setW(Hep3Vector v) {
        bank_double[GBLDOUBLE.WX] = v.x();
        bank_double[GBLDOUBLE.WY] = v.y();
        bank_double[GBLDOUBLE.WZ] = v.z();      
    }
    public Hep3Vector getW() {
        return new BasicHep3Vector(getWx(),getWy(),getWz());
    }
    public double getWx() {
        return getDoubleVal(GBLDOUBLE.WX);
    }
    public double getWy() {
        return getDoubleVal(GBLDOUBLE.WY);
    }
    public double getWz() {
        return getDoubleVal(GBLDOUBLE.WZ);
    }

    /**
     * Set track direction at this cluster
     * 
     * @param v the track direction
     */
    public void setTrackDir(Hep3Vector v) {
        bank_double[GBLDOUBLE.TDIRX] = v.x();
        bank_double[GBLDOUBLE.TDIRY] = v.y();
        bank_double[GBLDOUBLE.TDIRZ] = v.z();       
    }
    public Hep3Vector getTrackDirection() {
        return new BasicHep3Vector(getTx(),getTy(),getTz());
    }
    public double getTx() {
        return getDoubleVal(GBLDOUBLE.TDIRX);
    }
    public double getTy() {
        return getDoubleVal(GBLDOUBLE.TDIRY);
    }
    public double getTz() {
        return getDoubleVal(GBLDOUBLE.TDIRZ);
    }

    public void setTrackPhi(double phi) {
        bank_double[GBLDOUBLE.TPHI] = phi;
    }
    
    public double getTrackPhi() {
        return getDoubleVal(GBLDOUBLE.TPHI);
    }

    public void setTrackLambda(double lambda) {
        bank_double[GBLDOUBLE.TLAMBDA] = lambda;
    }
    
    public double getTrackLambda() {
        return getDoubleVal(GBLDOUBLE.TLAMBDA);
    }

    
    public void setMeas(double umeas) {
        bank_double[GBLDOUBLE.UMEAS] = umeas;
    }
    
    public double getMeas() {
        return getDoubleVal(GBLDOUBLE.UMEAS);
    }
    
    public void setMeasErr(double x) {
        bank_double[GBLDOUBLE.UMEASERR] = x;
    }

    public double getMeasErr() {
        return getDoubleVal(GBLDOUBLE.UMEASERR);
    }

    
    /**
     * Set track position in local frame
     * @param trkpos_meas
     */
    public void setTrackPos(Hep3Vector trkpos_meas) {
        bank_double[GBLDOUBLE.TPOSU] = trkpos_meas.x();
        bank_double[GBLDOUBLE.TPOSV] = trkpos_meas.y();
        bank_double[GBLDOUBLE.TPOSW] = trkpos_meas.z();
    }

    public Hep3Vector getTrackPos() {
        return new BasicHep3Vector(getTrackPosU(),getTrackPosV(),getTrackPosW());
    }
    
    public double getTrackPosU() {
        return getDoubleVal(GBLDOUBLE.TPOSU);
    }

    public double getTrackPosV() {
        return getDoubleVal(GBLDOUBLE.TPOSV);
    }

    public double getTrackPosW() {
        return getDoubleVal(GBLDOUBLE.TPOSW);
    }

    public void setScatterAngle(double scatAngle) {
        bank_double[GBLDOUBLE.MSANGLE] = scatAngle;
    }
    
    public double getScatterAngle() {
        return getDoubleVal(GBLDOUBLE.MSANGLE);
    }
    
    /*
     * The functions below are all overide from 
     * @see org.lcsim.event.GenericObject#getNInt()
     */
    
    public int getNInt() {
        return GBLINT.BANK_INT_SIZE;
    }

    public int getNFloat() {
        return 0;
    }

    public int getNDouble() {
        return GBLDOUBLE.BANK_DOUBLE_SIZE;
    }

    public int getIntVal(int index) {
        return bank_int[index];
    }

    public float getFloatVal(int index) {
        return 0;
    }

    public double getDoubleVal(int index) {
        return bank_double[index];
    }

    public boolean isFixedSize() {
        return false;
    }

}
