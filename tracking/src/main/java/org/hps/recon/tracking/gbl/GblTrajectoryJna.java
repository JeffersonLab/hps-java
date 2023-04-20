package org.hps.recon.tracking.gbl;

import java.util.List;
import org.apache.commons.math3.util.Pair;

import com.sun.jna.Pointer; 
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;

/**
 * wrapper class for GblTrajectory JNA functions
 * <p>
 * This class re-promotes these JNA functions to be
 * class member functions while providing some translation
 * capabilities form java types into their C-style
 * counterparts required by JNA.
 * <p>
 * <b>Note</b>: While the points are passed into this constructor,
 * the memory owned by those points is never owned by this
 * class. It is up to the user to delete the points when they
 * are done using them.
 */
public class GblTrajectoryJna {
    private Pointer self;
    
    // copy a java-style list into a JNA C-style array that can then be copied into a std vector in GBL
    public GblTrajectoryJna(List<GblPointJna> points, boolean flagCurv, boolean flagU1dir, boolean flagU2dir) { 
        Pointer [] ppoints = new Pointer[points.size()];

        int ipoint=-1;
        for (GblPointJna point : points) {
            ipoint+=1;
            ppoints[ipoint]  = point.getPtr();
        }
        
        self = GblInterface.INSTANCE.GblTrajectoryCtorPtrArray(ppoints, points.size(), 
            flagCurv?1:0, flagU1dir?1:0, flagU2dir?1:0);
        if (self == Pointer.NULL)
            System.out.println("Failed generating trajectory");
                
    }

    //Simple trajectory constructor with seed 
    public GblTrajectoryJna(List<GblPointJna> points, int aLabel, Matrix seed, boolean flagCurv, boolean flagU1dir, boolean flagU2dir) { 
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
        
        
        self = GblInterface.INSTANCE.GblTrajectoryCtorPtrArraySeed(ppoints, points.size(), aLabel, seedArray, flagCurv?1:0, flagU1dir?1:0, flagU2dir?1:0);
        
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
        
        self = GblInterface.INSTANCE.GblTrajectoryCtorPtrComposed(ppoints_1, npoints_1, trafo_1,
                                                                            ppoints_2, npoints_2, trafo_2);
    }
    
    //to perform the full fit
    public void fit(DoubleByReference Chi2, IntByReference Ndf, DoubleByReference lostWeight, String optionList) {
        char[] c_optionList = optionList.toCharArray();
        GblInterface.INSTANCE.GblTrajectory_fit(self, Chi2, Ndf, lostWeight, c_optionList,-999);
    }
    
    //To perform the fit removing a particular point
    public void fit(DoubleByReference Chi2, IntByReference Ndf,  DoubleByReference lostWeight, String optionList, int aLabel) {
        char [] c_optionList = optionList.toCharArray();
        GblInterface.INSTANCE.GblTrajectory_fit(self, Chi2, Ndf, lostWeight, c_optionList, aLabel);
    }
    
    public void addPoint(GblPointJna point) {
        GblInterface.INSTANCE.GblTrajectory_addPoint(self, point.getPtr());
    }
    
    public boolean isValid () {
        return (GblInterface.INSTANCE.GblTrajectory_isValid(self)!=0);
        
    }

    public int getNumPoints() {
        return GblInterface.INSTANCE.GblTrajectory_getNumPoints(self);
    }

    public void printTrajectory(int level) {
        GblInterface.INSTANCE.GblTrajectory_printTrajectory(self,level);
    }
    
    public void printData() {
        GblInterface.INSTANCE.GblTrajectory_printData(self);
    }

    public void printPoints(int level) {
        GblInterface.INSTANCE.GblTrajectory_printPoints(self,level);
    }
    
    //Call delete on the underlying objects
    public void delete() {
        GblInterface.INSTANCE.GblTrajectory_delete(self);
    }
    
    
    public void getMeasResults(int aLabel, int numData[], List<Double> aResiduals,List<Double> aMeasErrors, List<Double> aResErrors, List<Double> aDownWeights) {
        
        double[] d_aResiduals  = new double[2];
        double[] d_aMeasErrors = new double[2];
        double[] d_aResErrors  = new double[2];
        double[] d_aDownWeights = new double[2];
        
        GblInterface.INSTANCE.GblTrajectory_getMeasResults(self, aLabel, numData, d_aResiduals, d_aMeasErrors, d_aResErrors, d_aDownWeights);

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
        
        GblInterface.INSTANCE.GblTrajectory_getResults(self,aSignedLabel,d_localPar,nLocalPar,d_localCov,sizeLocalCov);
        
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
        GblInterface.INSTANCE.GblTrajectory_milleOut(self, millebinary.getPtr());
    }
}
