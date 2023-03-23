package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.List;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.Vector;


public class GblPointJna { 
    
    public interface GblPointInterface extends Library { 
        
        GblPointInterface INSTANCE = (GblPointInterface) Native.loadLibrary("GBL", GblPointInterface.class); 
        
        Pointer GblPointCtor(double [] array); 
        //Pointer GblPointCtor2D(double matrix[][]); 

        void GblPoint_delete(Pointer self);
        int GblPoint_getNumMeasurements(Pointer self);
        
        void GblPoint_addMeasurement2D(Pointer self, double[] projArray, double[] resArray, double[] precArray, 
                                       double minPrecision);
        void GblPoint_addScatterer(Pointer self, double[] resArray, double[] precArray);
        
        void GblPoint_printPoint(Pointer self, int i);
        void GblPoint_addGlobals(Pointer self, int[] labels, int nlabels, double[] derArray);
        void GblPoint_getGlobalLabelsAndDerivatives(Pointer self, IntByReference nlabels, PointerByReference labels, PointerByReference ders);
    }
    
    private Pointer self; 

    public GblPointJna(Matrix m) { 
        System.out.println("DEBUG::Tom::java::GblPointJna(Matrix)");
        double [] array = m.getColumnPackedCopy();
        //Jacobian is always 5x5
        
        if (m.getRowDimension() != 5 || m.getColumnDimension() != 5) 
            throw new RuntimeException("GBLPoint:: Malformed point. JacobianP2P needs to be a 5x5 matrix. ");
        self = GblPointInterface.INSTANCE.GblPointCtor(array); 
    }

    public int getNumMeasurements() {
      return GblPointInterface.INSTANCE.GblPoint_getNumMeasurements(self);
    }

    public void addGlobals(List<Integer> labels, Matrix globalDers) {
        System.out.println("DEBUG::Tom::java::GblPointJna.addGlobals");
        
        double [] gders = globalDers.getRowPackedCopy(); 
        int  [] glabels = new int[labels.size()];
        for (int ilabel=0; ilabel<labels.size(); ilabel++) {
            glabels[ilabel]=labels.get(ilabel);
        }
        GblPointInterface.INSTANCE.GblPoint_addGlobals(self, glabels, labels.size(), gders);
    }
    
    public void addMeasurement(Matrix aProjection, Vector aResiduals, Vector aPrecision) {
        System.out.println("DEBUG::Tom::java::GblPointJna.addMeasurement(no min)");
        addMeasurement(aProjection, aResiduals, aPrecision,0.);
    }
    
    public void addMeasurement(Matrix aProjection, Vector aResiduals, Vector aPrecision, double minPrecision) {
        System.out.println("DEBUG::Tom::java::GblPointJna.addMeasurement");
        
        double [] projArray = aProjection.getColumnPackedCopy();
        double [] resArray  = aResiduals.getColumnPackedCopy();
        double [] precArray = aPrecision.getColumnPackedCopy();
        
        if (aProjection.getRowDimension() == 2 )
            GblPointInterface.INSTANCE.GblPoint_addMeasurement2D(self,projArray, resArray, precArray, minPrecision);
        else
            throw new RuntimeException("GBLPoint:: unsupported call to addMeasurement. RowDim==2 only..");
    }
    
    public void addScatterer(Vector aResiduals, Vector aPrecision) {
        System.out.println("DEBUG::Tom::java::GblPointJna.addScatterer");
        double [] resArray  = aResiduals.getColumnPackedCopy();
        double [] precArray = aPrecision.getColumnPackedCopy();
        
        if (aPrecision.getColumnDimension() == 1 )
            GblPointInterface.INSTANCE.GblPoint_addScatterer(self,resArray,precArray);
        else 
            throw new RuntimeException("GBLPoint:: unsupported call to addMeasurement. ColDim==1 only..");
    }

    public void printPoint(int i) {
        System.out.println("DEBUG::Tom::java::GblPointJna.printPoint");
        GblPointInterface.INSTANCE.GblPoint_printPoint(self,i);
    }
    
    public Pointer getPtr() {
        System.out.println("DEBUG::Tom::java::GblPointJna.getPtr");
        return self;
    }

    public void delete() {
        System.out.println("DEBUG::Tom::java::GblPointJna.delete");
        GblPointInterface.INSTANCE.GblPoint_delete(self);
    }

    public Matrix getGlobalLabelsAndDerivatives(List<Integer> labels) {
        System.out.println("DEBUG::Tom::java::GblPointJna.getGlobalLabelsAndDerivatives");
        IntByReference nlabels = new IntByReference(0);
        PointerByReference labels_array_ptr = new PointerByReference();
        PointerByReference ders_array_ptr = new PointerByReference();
        GblPointInterface.INSTANCE.GblPoint_getGlobalLabelsAndDerivatives(self, nlabels, labels_array_ptr, ders_array_ptr);

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
