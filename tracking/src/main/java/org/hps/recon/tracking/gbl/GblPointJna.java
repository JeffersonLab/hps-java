package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 

//import jeigen.DenseMatrix;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.Vector;


public class GblPointJna { 
    
    public interface GblPointInterface extends Library { 
        
        GblPointInterface INSTANCE = (GblPointInterface) Native.loadLibrary("GBL", GblPointInterface.class); 
        
        Pointer GblPointCtor(double [] array); 
        Pointer GblPointCtor2D(double matrix[][]); 
        
        int GblPoint_hasMeasurement(Pointer self);
        double GblPoint_getMeasPrecMin(Pointer self);
        void GblPoint_addMeasurement2D(Pointer self, double[] projArray, double[] resArray, double[] precArray, 
                                       double minPrecision);
        void GblPoint_addScatterer(Pointer self, double[] resArray, double[] precArray);
                    
    }
    
    private Pointer self; 

    public GblPointJna(Matrix m) { 
        double [] array = m.getColumnPackedCopy();
        //Jacobian is always 5x5
        
        if (m.getRowDimension() != 5 || m.getColumnDimension() != 5) 
            throw new RuntimeException("GBLPoint:: Malformed point. JacobianP2P needs to be a 5x5 matrix. ");
        self = GblPointInterface.INSTANCE.GblPointCtor(array); 
    }
    
    public int hasMeasurement() {
        return GblPointInterface.INSTANCE.GblPoint_hasMeasurement(self);
    }
    
    public double getMeasPrecMin() {
        return GblPointInterface.INSTANCE.GblPoint_getMeasPrecMin(self);
    }
    
    public void addMeasurement(Matrix aProjection, Vector aResiduals, Vector aPrecision) {
        addMeasurement(aProjection, aResiduals, aPrecision,0.);
    }
    
    public void addMeasurement(Matrix aProjection, Vector aResiduals, Vector aPrecision, double minPrecision) {
        
        double [] projArray = aProjection.getColumnPackedCopy();
        double [] resArray  = aResiduals.getColumnPackedCopy();
        double [] precArray = aPrecision.getColumnPackedCopy();
        
        if (aProjection.getRowDimension() == 2 )
            GblPointInterface.INSTANCE.GblPoint_addMeasurement2D(self,projArray, resArray, precArray, minPrecision);
        else
            throw new RuntimeException("GBLPoint:: unsupported call to addMeasurement. RowDim==2 only..");
    }
    
    public void addScatterer(Vector aResiduals, Vector aPrecision) {
        double [] resArray  = aResiduals.getColumnPackedCopy();
        double [] precArray = aPrecision.getColumnPackedCopy();
        
        if (aPrecision.getColumnDimension() == 1 )
            GblPointInterface.INSTANCE.GblPoint_addScatterer(self,resArray,precArray);
        else 
            throw new RuntimeException("GBLPoint:: unsupported call to addMeasurement. ColDim==1 only..");
    }
    

    public GblPointJna(double matrix[][]) { 
        //do nothing for the moment
        //The 2d should be unpacked here.
        //self = GblPointInterface.INSTANCE.GblPointCtor(matrixArray); 
    }
    
}
