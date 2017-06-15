package org.hps.recon.tracking;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;

import org.lcsim.detector.Rotation3D;
import org.lcsim.detector.Transform3D;

/**
 * <p>
 * Class that contains the transformations between the JLAB and lcsim tracking coordinate systems.
 * </p>
 * <p>
 * <ul>
 * <li>created 6/27/2011</li>
 * <li>made static 10/14/2013</li>
 * </ul>
 * 
 * @author Matt Graham
 * @author Pelle Hansson
 * @author Miriam Diamond
 */
// FIXME: I am not sure this class should be located in this package. --JM
public class CoordinateTransformations {

    private static final Transform3D _detToTrk = CoordinateTransformations.initialize();
    private static final Transform3D _trkToDet = CoordinateTransformations.initializeInverse();

    /**
     * Private constructor to prevent initialization
     */
    private CoordinateTransformations() {
    }

    /**
     * Static private initialization of transform
     * 
     * @return transform
     */
    private static Transform3D initialize() {
        BasicHep3Matrix tmp = new BasicHep3Matrix();
        tmp.setElement(0, 2, 1);
        tmp.setElement(1, 0, 1);
        tmp.setElement(2, 1, 1);
        return new Transform3D(new Rotation3D(tmp));
    }

    private static Transform3D initializeInverse() {
        return _detToTrk.inverse();
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

    public static Transform3D getTransform() {
        return _detToTrk;
    }

    public static Transform3D getTransformInverse() {
        return _trkToDet;
    }

    public static Hep3Matrix getMatrix() {
        return _detToTrk.getRotation().getRotationMatrix();
    }

    public static Hep3Matrix getMatrixInverse() {
        return _trkToDet.getRotation().getRotationMatrix();
    }

}
