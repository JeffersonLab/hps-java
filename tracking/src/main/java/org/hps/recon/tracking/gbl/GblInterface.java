package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA Wrapper around the GBL C++ dynamic library
 *
 * All of the functions in this interface are bound to un-mangled wrapper functions
 * in the GBL C++ dynamic library.
 */
public interface GblInterface extends Library {
    GblInterface INSTANCE = (GblInterface) Native.loadLibrary("GBL", GblInterface.class);
    Pointer MilleBinaryCtor(String fileName, int filenamesize, int doublePrec, int keepZeros, int aSize);
    void MilleBinary_close(Pointer self);
    
    Pointer GblPointCtor(double [] array); 
    void GblPoint_delete(Pointer self);
    int GblPoint_getNumMeasurements(Pointer self);
    void GblPoint_addMeasurement2D(Pointer self, double[] projArray, double[] resArray, double[] precArray, 
                                   double minPrecision);
    void GblPoint_addScatterer(Pointer self, double[] resArray, double[] precArray);
    void GblPoint_printPoint(Pointer self, int i);
    void GblPoint_addGlobals(Pointer self, int[] labels, int nlabels, double[] derArray);
    void GblPoint_getGlobalLabelsAndDerivatives(Pointer self, IntByReference nlabels, PointerByReference labels, PointerByReference ders);

    Pointer GblTrajectoryCtorPtrArray(Pointer [] points, int npoints, int flagCurv, int flagU1dir, int flagU2dir);
    Pointer GblTrajectoryCtorPtrArraySeed(Pointer [] points, int npoints, int aLabel, double [] seedArray, int flagCurv, int flagU1dir, int flagU2dir);
    
    //Only supports 2 trajectories for e+/e- vertices or e- / e- 
    //Should/Could be extended to more 
    Pointer GblTrajectoryCtorPtrComposed(Pointer [] points_1, int npoints_1, double [] trafo_1, 
                                         Pointer [] points_2, int npoints_2, double [] trafo_2);
    
    void GblTrajectory_fit(Pointer self, DoubleByReference Chi2, IntByReference Ndf, DoubleByReference lostWeight, char [] optionList, int aLabel);
    void GblTrajectory_addPoint(Pointer self, Pointer point);
    int  GblTrajectory_isValid(Pointer self);
    void GblTrajectory_delete(Pointer self);
    int  GblTrajectory_getNumPoints(Pointer self);
    void GblTrajectory_printTrajectory(Pointer self, int level);
    void GblTrajectory_printData(Pointer self);
    void GblTrajectory_printPoints(Pointer self, int level);
    void GblTrajectory_getResults(Pointer self, int aSignedLabel, double[] localPar, int[] nLocalPar,
                                      double[] localCov, int[] sizeLocalCov);
    void GblTrajectory_milleOut(Pointer self, Pointer millebinary);
    void GblTrajectory_getMeasResults(Pointer self, int aLabel, int[] numdata, double[] aResiduals, double[] aMeasErrors, double[] aResErrors, double[] aDownWeights);
        
    Pointer GblHelixPredictionCtor(double sArc, double[] aPred,
                                   double [] tDir, double [] uDir, double [] vDir,
                                   double [] nDir, double [] aPos);
    
    void GblHelixPrediction_delete(Pointer self);
    double GblHelixPrediction_getArcLength(Pointer self);
    void GblHelixPrediction_getMeasPred(Pointer self, double [] prediction);
    void GblHelixPrediction_getPosition(Pointer self, double[] position);
    void GblHelixPrediction_getDirection(Pointer self,double[] direction);
    
    double GblHelixPrediction_getCosIncidence(Pointer self);
    
    //array for the curvilinear directions (2x3 matrix)
    void GblHelixPrediction_getCurvilinearDirs(Pointer self, double [] curvilinear);

    Pointer GblSimpleHelixCtor(double aRinv, double aPhi0, double aDca, double aDzds, double aZ0);
    void GblSimpleHelix_delete(Pointer self);
    double GblSimpleHelix_getPhi(Pointer self, double aRadius);
    double GblSimpleHelix_getArcLengthR(Pointer self, double aRadius);
    double GblSimpleHelix_getArcLengthXY(Pointer self, double xPos, double yPos);
    void GblSimpleHelix_moveToXY(Pointer self, double xPos, double yPos, double[] newPhi0, double[] newDca, double[] newZ0);
    Pointer GblSimpleHelix_getPrediction(Pointer self, double [] refPos, double [] uDir, double[] vDir);
}
