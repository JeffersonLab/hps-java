package org.hps.recon.tracking.gbl;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.DoubleByReference;

//import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.gbl.matrix.Matrix;
//import org.hps.recon.tracking.gbl.matrix.Vector;


public class GblTrajectoryJna {
    
    public interface GblTrajectoryInterface extends Library {
        GblTrajectoryInterface INSTANCE = (GblTrajectoryInterface) Native.loadLibrary("GBL", GblTrajectoryInterface.class); 
        
        Pointer GblTrajectoryCtor(int flagCurv, int flagU1dir, int flagU2dir);
        Pointer GblTrajectoryCtorPtrArray(Pointer [] points, int npoints, int flagCurv, int flagU1dir, int flagU2dir);
        Pointer GblTrajectoryCtorPtrArraySeed(Pointer [] points, int npoints, int aLabel, double [] seedArray, int flagCurv, int flagU1dir, int flagU2dir);
        
        void GblTrajectory_fit(Pointer self, DoubleByReference Chi2, IntByReference Ndf, DoubleByReference lostWeight, char [] optionList, int aLabel);
        void GblTrajectory_addPoint(Pointer self, Pointer point);
        int GblTrajectory_isValid(Pointer self);
        //void GblTrajectory_milleOut(Pointer self);
        
    }
    
    private Pointer self;
    
    public GblTrajectoryJna() {
        self = GblTrajectoryInterface.INSTANCE.GblTrajectoryCtor(1,1,1);
    }
    
    public GblTrajectoryJna(int flagCurv, int flagU1dir, int flagU2dir) {
        self = GblTrajectoryInterface.INSTANCE.GblTrajectoryCtor(flagCurv, flagU1dir, flagU2dir);
    }
    
    public GblTrajectoryJna(List<GblPointJna> points, int flagCurv, int flagU1dir, int flagU2dir) { 
        
        Pointer [] ppoints = new Pointer[points.size()];

        int ipoint=-1;
        for (GblPointJna point : points) {
            ipoint+=1;
            ppoints[ipoint]  = point.getPtr();
        }
        
        self = GblTrajectoryInterface.INSTANCE.GblTrajectoryCtorPtrArray(ppoints, points.size(), flagCurv, flagU1dir, flagU2dir);
        
    }


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

    
    
    
    public void fit(DoubleByReference Chi2, IntByReference Ndf, DoubleByReference lostWeight, String optionList) {
        
        char[] c_optionList = optionList.toCharArray();
        GblTrajectoryInterface.INSTANCE.GblTrajectory_fit(self, Chi2, Ndf, lostWeight, c_optionList,-999);
    }
    
    
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
    
    /*
    public void milleOut() {       
        return GblTrajectoryInterface.INSTANCE.GblTrajectory_milleOut(self);
    }
    */
        
}