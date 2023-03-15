package org.hps.recon.tracking.gbl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.DoubleByReference;

import java.util.List;
import org.apache.commons.math3.util.Pair;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;

public class GblTrajectoryJna {
    
    public interface GblTrajectoryInterface extends Library {
        GblTrajectoryInterface INSTANCE = (GblTrajectoryInterface) Native.loadLibrary("GBL", GblTrajectoryInterface.class); 
        
        Pointer GblTrajectoryCtor(int flagCurv, int flagU1dir, int flagU2dir);
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
        
    }
    
    private Pointer self;
    
    public GblTrajectoryJna() {
        self = GblTrajectoryInterface.INSTANCE.GblTrajectoryCtor(1,1,1);
    }
    
    public GblTrajectoryJna(int flagCurv, int flagU1dir, int flagU2dir) {
        self = GblTrajectoryInterface.INSTANCE.GblTrajectoryCtor(flagCurv, flagU1dir, flagU2dir);
    }
    
    // copy a java-style list into a JNA C-style array that can then be copied into a std vector in GBL
    public GblTrajectoryJna(List<GblPointJna> points, int flagCurv, int flagU1dir, int flagU2dir) { 
        
        Pointer [] ppoints = new Pointer[points.size()];

        int ipoint=-1;
        for (GblPointJna point : points) {
            ipoint+=1;
            ppoints[ipoint]  = point.getPtr();
        }
        
        self = GblTrajectoryInterface.INSTANCE.GblTrajectoryCtorPtrArray(ppoints, points.size(), flagCurv, flagU1dir, flagU2dir);
        if (self == Pointer.NULL)
            System.out.println("Failed generating trajectory");
                
    }

    //Simple trajectory constructor with seed 
    public GblTrajectoryJna(List<GblPointJna> points, int aLabel, Matrix seed, int flagCurv, int flagU1dir, int flagU2dir) { 
        
        Pointer [] ppoints = new Pointer[points.size()];

        int ipoint=-1;
        for (GblPointJna point : points) {
            ipoint+=1;
            ppoints[ipoint]  = point.getPtr();
        }
        
        if (seed.getRowDimension() != 5 || seed.getColumnDimension() != 5)
            throw new RuntimeException("GBLTrajectoryJna::Seed must be 5x5 matrix.\n");
        
        //The seed is 5x5 matrix
        double [] seedArray = seed.getColumnPackedCopy();
        
        
        self = GblTrajectoryInterface.INSTANCE.GblTrajectoryCtorPtrArraySeed(ppoints, points.size(), aLabel, seedArray, flagCurv, flagU1dir, flagU2dir);
        
    }
    
    //Composed (curved) trajectory constructor from a list of points and transformation (at inner (first) point)
    //Only 2 tracks, supported for the moment
    
    public GblTrajectoryJna(List < Pair <List<GblPointJna>, Matrix> > PointsAndTransList) {
        
        Pointer [] ppoints_1 = new Pointer[PointsAndTransList.get(0).getFirst().size()];
        Pointer [] ppoints_2 = new Pointer[PointsAndTransList.get(0).getFirst().size()];

        int npoints_1 = PointsAndTransList.get(0).getFirst().size();
        int npoints_2 = PointsAndTransList.get(1).getFirst().size();
       
        
        int ipoint=-1;
        for (GblPointJna point : PointsAndTransList.get(0).getFirst()) {
            ipoint+=1;
            ppoints_1[ipoint]  = point.getPtr();
        }
        
        ipoint=-1;
        for (GblPointJna point : PointsAndTransList.get(1).getFirst()) {
            ipoint+=1;
            ppoints_1[ipoint]  = point.getPtr();
        }
        
        double [] trafo_1 = PointsAndTransList.get(0).getSecond().getColumnPackedCopy();
        double [] trafo_2 = PointsAndTransList.get(1).getSecond().getColumnPackedCopy();
        
        self = GblTrajectoryInterface.INSTANCE.GblTrajectoryCtorPtrComposed(ppoints_1, npoints_1, trafo_1,
                                                                            ppoints_2, npoints_2, trafo_2);
    }
    
    //to perform the full fit
    public void fit(DoubleByReference Chi2, IntByReference Ndf, DoubleByReference lostWeight, String optionList) {
        
        char[] c_optionList = optionList.toCharArray();
        GblTrajectoryInterface.INSTANCE.GblTrajectory_fit(self, Chi2, Ndf, lostWeight, c_optionList,-999);
    }
    
    //To perform the fit removing a particular point
    public void fit(DoubleByReference Chi2, IntByReference Ndf,  DoubleByReference lostWeight, String optionList, int aLabel) {
        
        char [] c_optionList = optionList.toCharArray();
        GblTrajectoryInterface.INSTANCE.GblTrajectory_fit(self, Chi2, Ndf, lostWeight, c_optionList, aLabel);
    }
    
    public void addPoint(GblPointJna point) {
        GblTrajectoryInterface.INSTANCE.GblTrajectory_addPoint(self, point.getPtr());
    }
    
    public int isValid () {
        return GblTrajectoryInterface.INSTANCE.GblTrajectory_isValid(self);
        
    }

    public int getNumPoints() {
        return GblTrajectoryInterface.INSTANCE.GblTrajectory_getNumPoints(self);
    }

    public void printTrajectory(int level) {
        GblTrajectoryInterface.INSTANCE.GblTrajectory_printTrajectory(self,level);
    }
    
    public void printData() {
        GblTrajectoryInterface.INSTANCE.GblTrajectory_printData(self);
    }

    public void printPoints(int level) {
        GblTrajectoryInterface.INSTANCE.GblTrajectory_printPoints(self,level);
    }
    
    //Call delete on the underlying objects
    public void delete() {
        GblTrajectoryInterface.INSTANCE.GblTrajectory_delete(self);
    }
    
    
    public void getMeasResults(int aLabel, int numData[], List<Double> aResiduals,List<Double> aMeasErrors, List<Double> aResErrors, List<Double> aDownWeights) {
        
        double[] d_aResiduals  = new double[2];
        double[] d_aMeasErrors = new double[2];
        double[] d_aResErrors  = new double[2];
        double[] d_aDownWeights = new double[2];
        
        GblTrajectoryInterface.INSTANCE.GblTrajectory_getMeasResults(self, aLabel, numData, d_aResiduals, d_aMeasErrors, d_aResErrors, d_aDownWeights);

        for (int i=0; i<2; i++) {
            aResiduals.add(d_aResiduals[i]);
            aMeasErrors.add(d_aMeasErrors[i]);
            aResErrors.add(d_aResErrors[i]);
            aDownWeights.add(d_aDownWeights[i]);
        }
        
    }
    
    //!! Only 5-localPar and 5x5 local Cov for the moment
    public int getResults(int aSignedLabel, Vector localPar, SymMatrix localCov) {
        
        double[] d_localPar = new double[5];
        double[] d_localCov = new double[25];
        int[] nLocalPar = new int[1];
        int[] sizeLocalCov = new int[1];
        
        GblTrajectoryInterface.INSTANCE.GblTrajectory_getResults(self,aSignedLabel,d_localPar,nLocalPar,d_localCov,sizeLocalCov);
        
        for (int i=0; i<5;  i++) {
            localPar.set(i,d_localPar[i]);
        }
        
        for (int i=0; i<5; i++) {
            for (int j=0; j<5; j++) {
                localCov.set(i,j,d_localCov[i+5*j]);
            }
        }

        return 0;
    }

    
    public void milleOut(MilleBinaryJna millebinary) {       
        GblTrajectoryInterface.INSTANCE.GblTrajectory_milleOut(self, millebinary.getPtr());
    }
    
        
}
