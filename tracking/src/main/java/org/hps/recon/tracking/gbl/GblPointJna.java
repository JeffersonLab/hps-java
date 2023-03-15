package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 
import com.sun.jna.ptr.IntByReference;

import java.util.List;
import java.util.ArrayList;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.Vector;


public class GblPointJna { 
    
    public interface GblPointInterface extends Library { 
        
        GblPointInterface INSTANCE = (GblPointInterface) Native.loadLibrary("GBL", GblPointInterface.class); 
        
        Pointer GblPointCtor(double [] array); 
        //Pointer GblPointCtor2D(double matrix[][]); 

        void GblPoint_delete(Pointer self);
        
        void GblPoint_addMeasurement2D(Pointer self, double[] projArray, double[] resArray, double[] precArray, 
                                       double minPrecision);
        void GblPoint_addScatterer(Pointer self, double[] resArray, double[] precArray);
        
        void GblPoint_printPoint(Pointer self, int i);
        void GblPoint_addGlobals(Pointer self, int[] labels, int nlabels, double[] derArray);
        void GblPoint_getGlobalLabelsAndDerivatives(Pointer self, IntByReference nlabels, int[] labels, double[] ders);
    }
    
    private Pointer self; 

    public GblPointJna(Matrix m) { 
        double [] array = m.getColumnPackedCopy();
        //Jacobian is always 5x5
        
        if (m.getRowDimension() != 5 || m.getColumnDimension() != 5) 
            throw new RuntimeException("GBLPoint:: Malformed point. JacobianP2P needs to be a 5x5 matrix. ");
        self = GblPointInterface.INSTANCE.GblPointCtor(array); 
    }

    public void addGlobals(List<Integer> labels, Matrix globalDers) {
        
        double [] gders = globalDers.getRowPackedCopy(); 
        int  [] glabels = new int[labels.size()];
        for (int ilabel=0; ilabel<labels.size(); ilabel++) {
            glabels[ilabel]=labels.get(ilabel);
        }
        GblPointInterface.INSTANCE.GblPoint_addGlobals(self, glabels, labels.size(), gders);
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

    public void printPoint(int i) {
        GblPointInterface.INSTANCE.GblPoint_printPoint(self,i);
    }
    
    public Pointer getPtr() {
        return self;
    }

    public void delete() {
        GblPointInterface.INSTANCE.GblPoint_delete(self);
    }

    public void getGlobalLabelsAndDerivatives(List<Integer> labels, Matrix ders) {
        IntByReference nlabels = new IntByReference(0);
        int []labels_array = new int[1];
        double []ders_array = new double[1];
        GblPointInterface.INSTANCE.GblPoint_getGlobalLabelsAndDerivatives(self, nlabels, labels_array, ders_array);

        labels = new ArrayList<Integer>();
        for (int i = 0; i < nlabels.getValue(); ++i) {
            labels.add(labels_array[i]);
        }

        ders = new Matrix(1, nlabels.getValue());
        for (int i = 0; i < nlabels.getValue(); ++i) {
            ders.set(0,i,ders_array[i]);
        }
    }
}
