/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.event;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import org.lcsim.detector.Rotation3D;
import org.lcsim.detector.Transform3D;

/**
 *  Class that contains the transformations between the JLAB and lcsim tracking coordinate systems
 * @author mgraham, phansson
 * created 6/27/2011
 * made static 10/14/2013
 * 
 */
public class HPSTransformations {

    private static final Transform3D _detToTrk = HPSTransformations.initialize();

    /**
     * Private constructor to prevent initialization
     */
    private HPSTransformations() {
    }

    /**
     * Static private initialization of transform
     * @return transform
     */
    private static Transform3D initialize() {
    	BasicHep3Matrix tmp = new BasicHep3Matrix();
        tmp.setElement(0, 2, 1);
        tmp.setElement(1, 0, 1);
        tmp.setElement(2, 1, 1);
        return new Transform3D(new Rotation3D(tmp));
    }
    
    public static Hep3Vector transformVectorToTracking(Hep3Vector vec) {
        return _detToTrk.transformed(vec);
    }

    public static SymmetricMatrix transformCovarianceToTracking(SymmetricMatrix cov) {
        return _detToTrk.transformed(cov);
    }

    public static Hep3Vector transformVectorToDetector(Hep3Vector vec) {
        return (_detToTrk.inverse()).transformed(vec);
    }

    public static SymmetricMatrix transformCovarianceToDetector(SymmetricMatrix cov) {
        return (_detToTrk.inverse()).transformed(cov);
    }
    public static Transform3D getTransform(){
        return _detToTrk;
    }
    public static Hep3Matrix getMatrix(){
        return _detToTrk.getRotation().getRotationMatrix();
    }
    
}
