package org.hps.recon.tracking;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
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
        return _trkToDet.transformed(vec);
    }

    public static SymmetricMatrix transformCovarianceToDetector(SymmetricMatrix cov) {
        return _trkToDet.transformed(cov);
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

    /**
     * Rotate a general (possibly non-symmetric) 3x3 matrix from tracking frame to detector frame
     * via R*m*R^T. Needed for cross-covariance blocks (e.g. Cov(vertex position, momentum)) which
     * are not instances of SymmetricMatrix and so cannot use {@link #transformCovarianceToDetector}.
     */
    public static Matrix transformMatrixToDetector(Matrix m) {
        return rotate(m, getMatrixInverse());
    }

    /**
     * Rotate a general (possibly non-symmetric) 3x3 matrix from detector frame to tracking frame
     * via R*m*R^T. See {@link #transformMatrixToDetector(Matrix)}.
     */
    public static Matrix transformMatrixToTracking(Matrix m) {
        return rotate(m, getMatrix());
    }

    private static Matrix rotate(Matrix m, Hep3Matrix rot) {
        BasicMatrix r = new BasicMatrix(3, 3);
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                r.setElement(i, j, rot.e(i, j));
        return MatrixOp.mult(r, MatrixOp.mult(m, MatrixOp.transposed(r)));
    }

}
