package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 

import java.util.List;
import java.util.ArrayList;


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
        
        void GblPoint_printPoint(Pointer self, int i);
        void GblPoint_addGlobals(Pointer self, int[] labels, int nlabels, double[] derArray);
        void GblPoint_getGlobalLabels(Pointer self, int[] labels);
        int GblPoint_getNumGlobals(Pointer self);
        void GblPoint_getGlobalDerivatives(Pointer self, double[] derArray);
        
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

    public void printPoint(int i) {
        GblPointInterface.INSTANCE.GblPoint_printPoint(self,i);
    }
    
    public Pointer getPtr() {
        return self;
    }
    
    public int getNumGlobals() {
        return GblPointInterface.INSTANCE.GblPoint_getNumGlobals(self);
    }
    
    public List<Integer> getGlobalLabels(){ 
        
        int nlabels = getNumGlobals();
        int [] labels = new int[nlabels];
        List<Integer> glabels = new ArrayList<Integer>();

        GblPointInterface.INSTANCE.GblPoint_getGlobalLabels(self, labels);
        
        for (int i=0; i<nlabels; i++) {
            glabels.add(labels[i]);
            //System.out.println(glabels.get(i));
        }
        
        

        return glabels;
    }

    public Matrix getGlobalDerivatives() {
        
        int ngders = getNumGlobals();
        
        Matrix gders = new Matrix(1,ngders);
        double [] ders = new double[ngders];
        
        GblPointInterface.INSTANCE.GblPoint_getGlobalDerivatives(self, ders);
        
        for (int igd = 0; igd<ngders; igd++) {
            gders.set(0,igd,ders[igd]);
        }
        
        
        //System.out.println("Derivatives");
        //gders.print(6,6);
        
        return gders;
    }
    
    
}
