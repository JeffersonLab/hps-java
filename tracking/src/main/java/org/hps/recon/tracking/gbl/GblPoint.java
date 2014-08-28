package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.gbl.matrix.EigenvalueDecomposition;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;

/**
 *
 * @author phansson
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class GblPoint {

    public GblPoint(hep.physics.matrix.BasicMatrix jacPointToPoint) {
        theLabel = 0;
        theOffset = 0;
        measDim = 0;
        transFlag = false;
        //measTransformation() 
        scatFlag = false;
        //localDerivatives()
        //globalLabels()
        //globalDerivatives()

        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                p2pJacobian.set(i, j, jacPointToPoint.e(i, j));
            }
        }
    }

    public void addMeasurement(hep.physics.matrix.Matrix proL2m, hep.physics.matrix.BasicMatrix meas, hep.physics.matrix.BasicMatrix measPrec) {

        int ncols = proL2m.getNColumns();
        int nrows = proL2m.getNRows();
        System.out.println("proL2m has "+nrows+" rows and "+ ncols+ "columns");
        Matrix a = new Matrix(nrows, ncols);
        for(int i=0; i<nrows; ++i)
        {
            for(int j=0; j<ncols; ++j)
            {
                a.set(i,j,proL2m.e(i, j));
            }
        }
        System.out.println("GblPoint add matrix: ");
        a.print(10, 6);
        
        
        
        int measnrows = meas.getNRows();
        int measncols = meas.getNColumns();
        System.out.println("meas has "+measnrows+" rows and "+measncols+" columns");
        
        Vector measvec = new Vector(measncols);
        for(int i=0; i<measnrows; ++i)
        {
            measvec.set(i, meas.e(0, i));
        }
        System.out.println("GblPoint add meas: ");
        measvec.print(10, 6);
        
        
        int measPrecnrows = measPrec.getNRows();
        int measPrecncols = measPrec.getNColumns();
        
        System.out.println("measPrec has "+measPrecnrows+" rows and "+measPrecncols+" columns");
        Vector measPrecvec = new Vector(measPrecncols);
        for(int i=0; i<measPrecnrows; ++i)
        {
            measPrecvec.set(i, measPrec.e(0, i));
        }
        System.out.println("GblPoint add measPrec: ");
        measPrecvec.print(10, 6); 
        
        addMeasurement(a, measvec, measPrecvec, 0.);
    }

    public void addScatterer(hep.physics.matrix.BasicMatrix scat, hep.physics.matrix.BasicMatrix scatPrec) {
        // TODO Auto-generated method stub

    }

    private int theLabel; ///< Label identifying point
    private int theOffset; ///< Offset number at point if not negative (else interpolation needed)
    private Matrix p2pJacobian = new SymMatrix(5); ///< Point-to-point jacobian from previous point
    private Matrix prevJacobian = new SymMatrix(5); ///< Jacobian to previous scatterer (or first measurement)
    private Matrix nextJacobian = new SymMatrix(5); ///< Jacobian to next scatterer (or last measurement)
    private int measDim; ///< Dimension of measurement (1-5), 0 indicates absence of measurement
    private SymMatrix measProjection = new SymMatrix(5); ///< Projection from measurement to local system
    private Vector measResiduals = new Vector(5); ///< Measurement residuals
    private Vector measPrecision = new Vector(5); ///< Measurement precision (diagonal of inverse covariance matrix)
    private boolean transFlag; ///< Transformation exists?
    private Matrix measTransformation; ///< Transformation of diagonalization (of meas. precision matrix)
    private boolean scatFlag; ///< Scatterer present?
    private SymMatrix scatTransformation = new SymMatrix(2); ///< Transformation of diagonalization (of scat. precision matrix)
    private Vector scatResiduals = new Vector(2); ///< Scattering residuals (initial kinks if iterating)
    private Vector scatPrecision = new Vector(2); ///< Scattering precision (diagonal of inverse covariance matrix)
    private Matrix localDerivatives = new Matrix(0, 0); ///< Derivatives of measurement vs additional local (fit) parameters
    private List<Integer> globalLabels = new ArrayList<Integer>(); ///< Labels of global (MP-II) derivatives
    private Matrix globalDerivatives = new Matrix(0, 0); ///< Derivatives of measurement vs additional global (MP-II) parameters

/// Create a point.
    /**
     * Create point on (initial) trajectory. Needs transformation jacobian from
     * previous point. \param [in] aJacobian Transformation jacobian from
     * previous point
     */
    public GblPoint(Matrix aJacobian) {

        theLabel = 0;
        theOffset = 0;
        measDim = 0;
        transFlag = false;
        //measTransformation() 
        scatFlag = false;
        //localDerivatives()
        //globalLabels()
        //globalDerivatives()

        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                p2pJacobian.set(i, j, aJacobian.get(i, j));
            }
        }
    }

    public GblPoint(SymMatrix aJacobian) {
        theLabel = 0;
        theOffset = 0;
        p2pJacobian = new SymMatrix(aJacobian);
        measDim = 0;
        transFlag = false;
        //measTransformation() 
        scatFlag = false;
        //localDerivatives() 
        //globalLabels() 
        //globalDerivatives()

    }

/// Add a measurement to a point.
    /**
     * Add measurement (in meas. system) with diagonal precision (inverse
     * covariance) matrix. ((up to) 2D: position, 4D: slope+position, 5D:
     * curvature+slope+position) \param [in] aProjection Projection from local
     * to measurement system \param [in] aResiduals Measurement residuals \param
     * [in] aPrecision Measurement precision (diagonal) \param [in] minPrecision
     * Minimal precision to accept measurement
     */
    public void addMeasurement(Matrix aProjection,
            Vector aResiduals, Vector aPrecision,
            double minPrecision) {
        measDim = aResiduals.getRowDimension();
        int iOff = 5 - measDim;
        for (int i = 0; i < measDim; ++i) {
            measResiduals.set(iOff + i, aResiduals.get(i));
            measPrecision.set(iOff + i, (aPrecision.get(i) >= minPrecision ? aPrecision.get(i) : 0.));
            for (int j = 0; j < measDim; ++j) {
                measProjection.set(iOff + i, iOff + j, aProjection.get(i, j));
            }
        }
    }
/// Add a measurement to a point.

    /**
     * Add measurement (in meas. system) with arbitrary precision (inverse
     * covariance) matrix. Will be diagonalized. ((up to) 2D: position, 4D:
     * slope+position, 5D: curvature+slope+position) \param [in] aProjection
     * Projection from local to measurement system \param [in] aResiduals
     * Measurement residuals \param [in] aPrecision Measurement precision
     * (matrix) \param [in] minPrecision Minimal precision to accept measurement
     */
    public void addMeasurement(Matrix aProjection,
            Vector aResiduals, SymMatrix aPrecision,
            double minPrecision) {
        measDim = aResiduals.getRowDimension();
        EigenvalueDecomposition measEigen = new EigenvalueDecomposition(aPrecision);
        measTransformation.ResizeTo(measDim, measDim);
        measTransformation = measEigen.getV();
        measTransformation.transposeInPlace();
        transFlag = true;
        Vector transResiduals = measTransformation.times(aResiduals);
        Vector transPrecision = new Vector(measEigen.getRealEigenvalues());
        Matrix transProjection = measTransformation.times(aProjection);
        int iOff = 5 - measDim;
        for (int i = 0; i < measDim; ++i) {
            measResiduals.set(iOff + i, transResiduals.get(i));
            measPrecision.set(iOff + i, (transPrecision.get(i) >= minPrecision ? transPrecision.get(i) : 0.));
            for (int j = 0; j < measDim; ++j) {
                measProjection.set(iOff + i, iOff + j, transProjection.get(i, j));
            }
        }
    }

/// Add a measurement to a point.
    /**
     * Add measurement in local system with diagonal precision (inverse
     * covariance) matrix. ((up to) 2D: position, 4D: slope+position, 5D:
     * curvature+slope+position) \param [in] aResiduals Measurement residuals
     * \param [in] aPrecision Measurement precision (diagonal) \param [in]
     * minPrecision Minimal precision to accept measurement
     */
    public void addMeasurement(Vector aResiduals,
            Vector aPrecision, double minPrecision) {
        measDim = aResiduals.getRowDimension();
        int iOff = 5 - measDim;
        for (int i = 0; i < measDim; ++i) {
            measResiduals.set(iOff + i, aResiduals.get(i));
            measPrecision.set(iOff + i,
                    aPrecision.get(i) >= minPrecision ? aPrecision.get(i) : 0.);
        }
        measProjection.setToIdentity();
    }

/// Add a measurement to a point.
    /**
     * Add measurement in local system with arbitrary precision (inverse
     * covariance) matrix. Will be diagonalized. ((up to) 2D: position, 4D:
     * slope+position, 5D: curvature+slope+position) \param [in] aResiduals
     * Measurement residuals \param [in] aPrecision Measurement precision
     * (matrix) \param [in] minPrecision Minimal precision to accept measurement
     */
    public void addMeasurement(Vector aResiduals,
            SymMatrix aPrecision, double minPrecision) {
        measDim = aResiduals.getRowDimension();
        EigenvalueDecomposition measEigen = new EigenvalueDecomposition(aPrecision);
        measTransformation.ResizeTo(measDim, measDim);
        measTransformation = measEigen.getV();
        measTransformation.transposeInPlace();
        transFlag = true;
        Vector transResiduals = measTransformation.times(aResiduals);
        Vector transPrecision = new Vector(measEigen.getRealEigenvalues());
        int iOff = 5 - measDim;
        for (int i = 0; i < measDim; ++i) {
            measResiduals.set(iOff + i, transResiduals.get(i));
            measPrecision.set(iOff + i, (transPrecision.get(i)) >= minPrecision ? transPrecision.get(i) : 0.);
            for (int j = 0; j < measDim; ++j) {
                measProjection.set(iOff + i, iOff + j, measTransformation.get(i, j));
            }
        }
    }

/// Check for measurement at a point.
    /**
     * Get dimension of measurement (0 = none). \return measurement dimension
     */
    int hasMeasurement() {
        return measDim;
    }

/// Retrieve measurement of a point.
    /**
     * \param [out] aProjection Projection from (diagonalized) measurement to
     * local system \param [out] aResiduals Measurement residuals \param [out]
     * aPrecision Measurement precision (diagonal)
     */
    public void getMeasurement(SymMatrix aProjection, Vector aResiduals,
            Vector aPrecision) {
        aProjection = measProjection;
        aResiduals = measResiduals;
        aPrecision = measPrecision;
    }

/// Get measurement transformation (from diagonalization).
    /**
     * \param [out] aTransformation Transformation matrix
     */
    public void getMeasTransformation(Matrix aTransformation) {
        aTransformation.ResizeTo(measDim, measDim);
        if (transFlag) {
            aTransformation = measTransformation;
        } else {
            aTransformation.UnitMatrix();
        }
    }

/// Add a (thin) scatterer to a point.
    /**
     * Add scatterer with diagonal precision (inverse covariance) matrix.
     * Changes local track direction.
     *
     * \param [in] aResiduals Scatterer residuals \param [in] aPrecision
     * Scatterer precision (diagonal of inverse covariance matrix)
     */
    public void addScatterer(Vector aResiduals,
            Vector aPrecision) {
        scatFlag = true;
        scatResiduals.set(0, aResiduals.get(0));
        scatResiduals.set(1, aResiduals.get(1));
        scatPrecision.set(0, aPrecision.get(0));
        scatPrecision.set(1, aPrecision.get(1));
        scatTransformation.setToIdentity();
    }

/// Add a (thin) scatterer to a point.
    /**
     * Add scatterer with arbitrary precision (inverse covariance) matrix. Will
     * be diagonalized. Changes local track direction.
     *
     * The precision matrix for the local slopes is defined by the angular
     * scattering error theta_0 and the scalar products c_1, c_2 of the offset
     * directions in the local frame with the track direction:
     *
     * (1 - c_1*c_1 - c_2*c_2) | 1 - c_1*c_1 - c_1*c_2 | P =
     * ~~~~~~~~~~~~~~~~~~~~~~~ * | | theta_0*theta_0 | - c_1*c_2 1 - c_2*c_2 |
     *
     * \param [in] aResiduals Scatterer residuals \param [in] aPrecision
     * Scatterer precision (matrix)
     */
    public void addScatterer(Vector aResiduals,
            SymMatrix aPrecision) {
        scatFlag = true;
        EigenvalueDecomposition scatEigen = new EigenvalueDecomposition(aPrecision);
        Matrix aTransformation = scatEigen.getEigenVectors();
        aTransformation.transposeInPlace();
        Vector transResiduals = aTransformation.times(aResiduals);
        Vector transPrecision = new Vector(scatEigen.getRealEigenvalues());
        for (int i = 0; i < 2; ++i) {
            scatResiduals.set(i, transResiduals.get(i));
            scatPrecision.set(i, transPrecision.get(i));
            for (int j = 0; j < 2; ++j) {
                scatTransformation.set(i, j, aTransformation.get(i, j));
            }
        }
    }

/// Check for scatterer at a point.
    boolean hasScatterer() {
        return scatFlag;
    }

/// Retrieve scatterer of a point.
    /**
     * \param [out] aTransformation Scatterer transformation from
     * diagonalization \param [out] aResiduals Scatterer residuals \param [out]
     * aPrecision Scatterer precision (diagonal)
     */
    public void getScatterer(SymMatrix aTransformation, Vector aResiduals,
            Vector aPrecision) {
        aTransformation = scatTransformation;
        aResiduals = scatResiduals;
        aPrecision = scatPrecision;
    }

/// Get scatterer transformation (from diagonalization).
    /**
     * \param [out] aTransformation Transformation matrix
     */
    public void getScatTransformation(Matrix aTransformation) {
        aTransformation.ResizeTo(2, 2);
        if (scatFlag) {
            for (int i = 0; i < 2; ++i) {
                for (int j = 0; j < 2; ++j) {
                    aTransformation.set(i, j, scatTransformation.get(i, j));
                }
            }
        } else {
            aTransformation.UnitMatrix();
        }
    }

/// Add local derivatives to a point.
    /**
     * Point needs to have a measurement. \param [in] aDerivatives Local
     * derivatives (matrix)
     */
    public void addLocals(Matrix aDerivatives) {
        if (measDim != 0) {
            localDerivatives.ResizeTo(aDerivatives.getRowDimension(), aDerivatives.getColumnDimension());
            if (transFlag) {
                localDerivatives = measTransformation.times(aDerivatives);
            } else {
                localDerivatives = aDerivatives;
            }
        }
    }

/// Retrieve number of local derivatives from a point.
    int getNumLocals() {
        return localDerivatives.getColumnDimension();
    }

/// Retrieve local derivatives from a point.
    Matrix getLocalDerivatives() {
        return localDerivatives;
    }

/// Add global derivatives to a point.
    /**
     * Point needs to have a measurement. \param [in] aLabels Global derivatives
     * labels \param [in] aDerivatives Global derivatives (matrix)
     */
    public void addGlobals(List<Integer> aLabels,
            Matrix aDerivatives) {
        if (measDim != 0) {
            globalLabels = aLabels;
            globalDerivatives.ResizeTo(aDerivatives.getRowDimension(), aDerivatives.getColumnDimension());
            if (transFlag) {
                globalDerivatives = measTransformation.times(aDerivatives);
            } else {
                globalDerivatives = aDerivatives;
            }

        }
    }

/// Retrieve number of global derivatives from a point.
    int getNumGlobals() {
        return globalDerivatives.getColumnDimension();
    }

/// Retrieve global derivatives labels from a point.
    List<Integer> getGlobalLabels() {
        return globalLabels;
    }

/// Retrieve global derivatives from a point.
    Matrix getGlobalDerivatives() {
        return globalDerivatives;
    }

/// Define label of point (by GBLTrajectory constructor)
    /**
     * \param [in] aLabel Label identifying point
     */
    public void setLabel(int aLabel) {
        theLabel = aLabel;
    }

/// Retrieve label of point
    int getLabel() {
        return theLabel;
    }

/// Define offset for point (by GBLTrajectory constructor)
    /**
     * \param [in] anOffset Offset number
     */
    public void setOffset(int anOffset) {
        theOffset = anOffset;
    }

/// Retrieve offset for point
    int getOffset() {
        return theOffset;
    }

/// Retrieve point-to-(previous)point jacobian
    Matrix getP2pJacobian() {
        return p2pJacobian;
    }

/// Define jacobian to previous scatterer (by GBLTrajectory constructor)
    /**
     * \param [in] aJac Jacobian
     */
    public void addPrevJacobian(Matrix aJac) {
        int ifail = 0;
// to optimize: need only two last rows of inverse
//	prevJacobian = aJac.InverseFast(ifail);
//  block matrix algebra
        Matrix CA = aJac.sub(2, 3, 3, 0).times(aJac.sub(3, 0, 0).inverse()); // C*A^-1
        Matrix DCAB = aJac.sub(2, 3, 3).minus(CA.times(aJac.sub(3, 2, 0, 3))); // D - C*A^-1 *B
        DCAB = DCAB.inverse();
        prevJacobian.placeAt(DCAB, 3, 3);
        prevJacobian.placeAt(DCAB.times(CA).uminus(), 3, 0);
    }
//
/// Define jacobian to next scatterer (by GBLTrajectory constructor)

    /**
     * \param [in] aJac Jacobian
     */
    public void addNextJacobian(Matrix aJac) {
        nextJacobian = aJac;
    }

/// Retrieve derivatives of local track model
    /**
     * Linearized track model: F_u(q/p,u',u) = J*u + S*u' + d*q/p, W is inverse
     * of S, negated for backward propagation. \param [in] aDirection
     * Propagation direction (>0 forward, else backward) \param [out] matW W
     * \param [out] matWJ W*J \param [out] vecWd W*d \exception
     * std::overflow_error : matrix S is singular.
     */
    public void getDerivatives(int aDirection, Matrix matW, Matrix matWJ,
            Vector vecWd) {

        Matrix matJ;
        Vector vecd;
        if (aDirection < 1) {
            matJ = prevJacobian.sub(2, 3, 3);
            matW = prevJacobian.sub(2, 3, 1).uminus();
            vecd = prevJacobian.subCol(2, 0, 3);
        } else {
            matJ = nextJacobian.sub(2, 3, 3);
            matW = nextJacobian.sub(2, 3, 1);
            vecd = nextJacobian.subCol(2, 0, 3);
        }

        matW = matW.inverse();
//	if (!matW.InvertFast()) {
//		std::cout << " getDerivatives failed to invert matrix: "
//				<< matW << "\n";
//		std::cout
//				<< " Possible reason for singular matrix: multiple GblPoints at same arc-length"
//				<< "\n";
//		throw std::overflow_error("Singular matrix inversion exception");
//	}
        matWJ = matW.times(matJ);
        vecWd = matW.times(vecd);

    }
//
/// Print GblPoint

    /**
     * \param [in] level print level (0: minimum, >0: more)
     */
    public void printPoint(int level) {
        StringBuffer sb = new StringBuffer("GblPoint");
        if (theLabel != 0) {
            sb.append(", label " + theLabel);
            if (theOffset >= 0) {
                sb.append(", offset " + theOffset);
            }
        }
        if (measDim != 0) {
            sb.append(", " + measDim + " measurements");
        }
        if (scatFlag) {
            sb.append(", scatterer");
        }
        if (transFlag) {
            sb.append(", diagonalized");
        }
        if (localDerivatives != null) {
            sb.append(", " + localDerivatives.getColumnDimension()
                    + " local derivatives");
        }
        if (globalDerivatives != null) {
            sb.append(", " + globalDerivatives.getColumnDimension()
                    + " global derivatives");
        }
        sb.append("\n");
        if (level > 0) {
            if (measDim != 0) {
                sb.append("  Measurement" + "\n");
                sb.append("   Projection: " + "\n" + measProjection
                        + "\n");
                sb.append("   Residuals: " + measResiduals + "\n");
                sb.append("   Precision: " + measPrecision + "\n");
            }
            if (scatFlag) {
                sb.append("  Scatterer" + "\n");
                sb.append("   Residuals: " + scatResiduals + "\n");
                sb.append("   Precision: " + scatPrecision + "\n");
            }
            if (localDerivatives.getColumnDimension() != 0) {
                sb.append("  Local Derivatives:" + "\n");
                localDerivatives.print(4, 6);
            }
            if (globalDerivatives.getColumnDimension() != 0) {
                sb.append("  Global Labels:");
                for (int i = 0; i < globalLabels.size(); ++i) {
                    sb.append(" " + globalLabels.get(i));
                }
                sb.append("\n");
                sb.append("  Global Derivatives:" + "\n");
                globalDerivatives.print(4, 6);
            }
            sb.append("  Jacobian " + "\n");
            sb.append("   Point-to-point " + "\n" + p2pJacobian
                    + "\n");
            if (theLabel != 0) {
                sb.append("   To previous offset " + "\n" + prevJacobian
                        + "\n");
                sb.append("   To next offset " + "\n" + nextJacobian
                        + "\n");
            }
        }
        System.out.println(sb.toString());
    }

}
