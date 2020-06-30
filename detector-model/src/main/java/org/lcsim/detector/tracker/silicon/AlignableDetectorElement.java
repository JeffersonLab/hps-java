package org.lcsim.detector.tracker.silicon;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.IPhysicalVolumePath;
//import org.lcsim.detector.IRotation3D;
//import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.ITransform3D;
//import org.lcsim.detector.Translation3D;
import org.lcsim.detector.identifier.IIdentifier;
//import org.lcsim.detector.identifier.IdentifierHelper;
import org.lcsim.geometry.compact.converter.JavaSurveyVolume;
import java.util.List;
import java.util.ArrayList;


/*
//Load the C-Matrices
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.FrameToFrameDers;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.Hep3Matrix;
*/

//Class to transport the Survey Volume transformations to the reconstruction software

public class AlignableDetectorElement extends DetectorElement {
    
    ITransform3D localToGlobal;
    String aname;
    int mpid=-999;
    
    /*
    Matrix c_matrix = null;
    FrameToFrameDers f2fD = new FrameToFrameDers();
    */
    
    public AlignableDetectorElement(String name, JavaSurveyVolume volume, IDetectorElement parent, IIdentifier id) {
        super(name,parent,id);
        //checkName(name);
        aname=name;
        localToGlobal = new Transform3D(volume.getSVl2gTransform().getTranslation(), volume.getSVl2gTransform().getRotation());
    }
    
    public AlignableDetectorElement(String name, IDetectorElement parent, IPhysicalVolumePath support, IIdentifier id) {
        super(name,parent,support,id);
        aname=name;
        localToGlobal = super.getGeometry().getLocalToGlobal();
    }
    
    public AlignableDetectorElement(String name, IDetectorElement parent, String support, IIdentifier id) {
        super(name,parent,support,id);
        aname = name;
        localToGlobal = super.getGeometry().getLocalToGlobal();
    }
    
    public Transform3D getlocalToGlobal() {
        return (Transform3D) localToGlobal;
    }
    
    public void setMillepedeId(int mpid) {
        this.mpid = mpid;
    }

    public int getMillepedeId() {
        return  mpid;
    }
    
    //This should be moved
    public List<Integer> getMPIILabels() {
        
        List<Integer> labels = new ArrayList<Integer>();
        
        //volume type axis sensorId sensorId == vtass
        
        int vOffset = 10000;
        if (isBottomLayer())
            vOffset = 20000;
        
        //type loop
        for (int j=1000; j<=2000; j+=1000) {
            // axis loop
            for (int i=100; i<=300; i+=100) {
                labels.add(vOffset+j+i+getMillepedeId());
            }
        }
        
        return labels;
    }
    
    /*
    //Compute the C-Matrix for composite to sub-component
    public void computeCMatrix(Hep3Matrix Rgtosc, Hep3Vector Tgtosc) {
        
        Hep3Matrix Rgtoc = this.getlocalToGlobal().getRotation().getRotationMatrix();
        Hep3Vector Tgtoc = this.getlocalToGlobal().getTranslation().getTranslationVector();
        
        c_matrix = f2fD.getDerivative(Rgtosc, Rgtoc, Tgtosc, Tgtoc);
    }
    
    //Get the C_matrix
    public Matrix getCMatrix() {
        return c_matrix;
    }

    //Get the C_matrix inverse
    public Matrix getCMatrixInv() {
        if (c_matrix != null)
            return c_matrix.inverse();
        return null;
    }

    */

    
    //TODO::These should all go

    public boolean isTopLayer() {
        if (aname.contains("top")) 
            return true;
        return false;
    }
    
    public boolean isBottomLayer() {
        if (aname.contains("bottom"))
            return true;
        return false;
    }
        
    //Hack to get the name.
    //@Override
    //public String getName() {
    //  return aname;
    //}
}