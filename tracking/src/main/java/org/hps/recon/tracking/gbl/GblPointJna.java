package org.hps.recon.tracking.gbl; 

import java.util.List;

import com.sun.jna.Pointer; 
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.Vector;

/**
 * wrapper for the GblPoint JNA functions
 * <p>
 * This class helps aid in the translation between java structures
 * and the JNA function types. Specifically, the work done here 
 * re-promotes the GblPoint JNA functions to be member functions
 * of a class (like they are in the original C++) and then also
 * provides translations between the more heirarchical matrix.Matrix
 * and matrix.Vector classes with the C-style arrays that are
 * required to pass the data into the C++ library via JNA.
 */
public class GblPointJna { 
    private Pointer self; 

    public GblPointJna(Matrix m) { 
        double [] array = m.getColumnPackedCopy();
        //Jacobian is always 5x5
        
        if (m.getRowDimension() != 5 || m.getColumnDimension() != 5) 
            throw new RuntimeException("GBLPoint:: Malformed point. JacobianP2P needs to be a 5x5 matrix. ");
        self = GblInterface.INSTANCE.GblPointCtor(array); 
    }

    public int getNumMeasurements() {
        return GblInterface.INSTANCE.GblPoint_getNumMeasurements(self);
    }

    public void addGlobals(List<Integer> labels, Matrix globalDers) {
        double [] gders = globalDers.getRowPackedCopy(); 
        int  [] glabels = new int[labels.size()];
        for (int ilabel=0; ilabel<labels.size(); ilabel++) {
            glabels[ilabel]=labels.get(ilabel);
        }
        GblInterface.INSTANCE.GblPoint_addGlobals(self, glabels, labels.size(), gders);
    }
    
    public void addMeasurement(Matrix aProjection, Vector aResiduals, Vector aPrecision) {
        addMeasurement(aProjection, aResiduals, aPrecision,0.);
    }
    
    public void addMeasurement(Matrix aProjection, Vector aResiduals, Vector aPrecision, double minPrecision) {
        double [] projArray = aProjection.getColumnPackedCopy();
        double [] resArray  = aResiduals.getColumnPackedCopy();
        double [] precArray = aPrecision.getColumnPackedCopy();
        
        if (aProjection.getRowDimension() == 2 )
            GblInterface.INSTANCE.GblPoint_addMeasurement2D(self,projArray, resArray, precArray, minPrecision);
        else
            throw new RuntimeException("GBLPoint:: unsupported call to addMeasurement. RowDim==2 only..");
    }
    
    public void addScatterer(Vector aResiduals, Vector aPrecision) {
        double [] resArray  = aResiduals.getColumnPackedCopy();
        double [] precArray = aPrecision.getColumnPackedCopy();
        
        if (aPrecision.getColumnDimension() == 1 )
            GblInterface.INSTANCE.GblPoint_addScatterer(self,resArray,precArray);
        else 
            throw new RuntimeException("GBLPoint:: unsupported call to addMeasurement. ColDim==1 only..");
    }

    public void printPoint(int i) {
        GblInterface.INSTANCE.GblPoint_printPoint(self,i);
    }
    
    public Pointer getPtr() {
        return self;
    }

    public void delete() {
        GblInterface.INSTANCE.GblPoint_delete(self);
    }

    public Matrix getGlobalLabelsAndDerivatives(List<Integer> labels) {
        IntByReference nlabels = new IntByReference(0);
        PointerByReference labels_array_ptr = new PointerByReference();
        PointerByReference ders_array_ptr = new PointerByReference();
        GblInterface.INSTANCE.GblPoint_getGlobalLabelsAndDerivatives(self, nlabels, labels_array_ptr, ders_array_ptr);

        int [] labels_array = labels_array_ptr.getValue().getIntArray(0, nlabels.getValue());
        double [] ders_array = ders_array_ptr.getValue().getDoubleArray(0, nlabels.getValue());

        labels.clear();
        for (int i = 0; i < nlabels.getValue(); ++i) {
            labels.add(labels_array[i]);
        }

        Matrix ders = new Matrix(1, nlabels.getValue());
        for (int i = 0; i < nlabels.getValue(); ++i) {
            ders.set(0,i,ders_array[i]);
        }

        return ders;
    }
}
