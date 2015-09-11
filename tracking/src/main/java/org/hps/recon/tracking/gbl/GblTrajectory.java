package org.hps.recon.tracking.gbl;

/**
 *
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 * @author Norman A Graf
 *
 * @version $Id:
 *
 */
import static java.lang.Math.abs;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.gbl.matrix.BorderedBandMatrix;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.VVector;
import org.hps.recon.tracking.gbl.matrix.Vector;

public class GblTrajectory
{

    public GblTrajectory(List<GblPoint> listOfPoints)
    {
        this(listOfPoints, true, true, true);
    }

    public void fit(double m_chi2, int m_ndf, int m_lost_weight)
    {
        // TODO Auto-generated method stub
    }

    /**
     * \mainpage General information
     *
     * \section intro_sec Introduction
     *
     * For a track with an initial trajectory from a prefit of the measurements
     * (internal seed) or an external prediction (external seed) the description
     * of multiple scattering is added by offsets in a local system. Along the
     * initial trajectory points are defined with can describe a measurement or
     * a (thin) scatterer or both. Measurements are arbitrary functions of the
     * local track parameters at a point (e.g. 2D: position, 4D:
     * direction+position). The refit provides corrections to the local track
     * parameters (in the local system) and the corresponding covariance matrix
     * at any of those points. Non-diagonal covariance matrices will be
     * diagonalized internally. Outliers can be down-weighted by use of
     * M-estimators.
     *
     * The broken lines trajectory is defined by (2D) offsets at the first and
     * last point and all points with a scatterer. The prediction for a
     * measurement is obtained by interpolation of the enclosing offsets and for
     * triplets of adjacent offsets kink angles are determined. This requires
     * for all points the jacobians for propagation to the previous and next
     * offset. These are calculated from the point-to-point jacobians along the
     * initial trajectory. The sequence of points has to be strictly monotonic
     * in arc-length.
     *
     * Additional local or global parameters can be added and the trajectories
     * can be written to special binary files for calibration and alignment with
     * Millepede-II. (V. Blobel, NIM A, 566 (2006), pp. 5-13).
     *
     * Besides simple trajectories describing the path of a single particle
     * composed trajectories are supported. These are constructed from the
     * trajectories of multiple particles and some external parameters (like
     * those describing a decay) and transformations at the first points from
     * the external to the local track parameters.
     *
     * The conventions for the coordinate systems follow: Derivation of
     * Jacobians for the propagation of covariance matrices of track parameters
     * in homogeneous magnetic fields A. Strandlie, W. Wittek, NIM A, 566 (2006)
     * 687-698.
     *
     * \section call_sec Calling sequence
     *
     * -# Create list of points on initial trajectory:\n
     * <tt>List<GblPoint> list</tt>
     * -# For all points on initial trajectory: - Create points and add
     * appropriate attributes:\n - <tt>point = gbl::GblPoint(..)</tt>
     * - <tt>point.addMeasurement(..)</tt>
     * - Add additional local or global parameters to measurement:\n -
     * <tt>point.addLocals(..)</tt>
     * - <tt>point.addGlobals(..)</tt>
     * - <tt>point.addScatterer(..)</tt>
     * - Add point (ordered by arc length) to list:\n
     * <tt>list.add(point)</tt>
     * -# Create (simple) trajectory from list of points:\n
     * <tt>traj = gbl::GblTrajectory (list)</tt>
     * -# Optionally with external seed:\n
     * <tt>traj = gbl::GblTrajectory (list,seed)</tt>
     * -# Optionally check validity of trajectory:\n
     * <tt>if (not traj.isValid()) .. //abort</tt>
     * -# Fit trajectory, return error code, get Chi2, Ndf (and weight lost by
     * M-estimators):\n
     * <tt>ierr = traj.fit(..)</tt>
     * -# For any point on initial trajectory: - Get corrections and covariance
     * matrix for track parameters:\n
     * <tt>[..] = traj.getResults(label)</tt>
     * -# Optionally write trajectory to MP binary file:\n
     * <tt>traj.milleOut(..)</tt>
     *
     * \section loc_sec Local system and local parameters At each point on the
     * trajectory a local coordinate system with local track parameters has to
     * be defined. The first of the five parameters describes the bending, the
     * next two the direction and the last two the position (offsets). The
     * curvilinear system (T,U,V) with parameters (q/p, lambda, phi, x_t, y_t)
     * is well suited.
     *
     * \section impl_sec Implementation
     *
     * Matrices are implemented with ROOT (root.cern.ch). User input or output
     * is in the form of TMatrices. Internally SMatrices are used for fixes
     * sized and simple matrices based on List<> for variable sized matrices.
     *
     * \section ref_sec References - V. Blobel, C. Kleinwort, F. Meier, Fast
     * alignment of a complex tracking detector using advanced track models,
     * Computer Phys. Communications (2011), doi:10.1016/j.cpc.2011.03.017 - C.
     * Kleinwort, General Broken Lines as advanced track fitting method, NIM A,
     * 673 (2012), 107-110, doi:10.1016/j.nima.2012.01.024
     */
    int numAllPoints; ///< Number of all points on trajectory
    List< Integer> numPoints = new ArrayList<Integer>(); ///< Number of points on (sub)trajectory
    int numTrajectories; ///< Number of trajectories (in composed trajectory)
    int numOffsets; ///< Number of (points with) offsets on trajectory
    int numInnerTrans; ///< Number of inner transformations to external parameters
    int numCurvature; ///< Number of curvature parameters (0 or 1) or external parameters
    int numParameters; ///< Number of fit parameters
    int numLocals; ///< Total number of (additional) local parameters
    int numMeasurements; ///< Total number of measurements
    int externalPoint; ///< Label of external point (or 0)
    boolean constructOK; ///< Trajectory has been successfully constructed (ready for fit/output)
    boolean fitOK; ///< Trajectory has been successfully fitted (results are valid)
    List< Integer> theDimension = new ArrayList<Integer>(); ///< List of active dimensions (0=u1, 1=u2) in fit
    List<List<GblPoint>> thePoints = new ArrayList<List<GblPoint>>(); ///< (list of) List of points on trajectory
    List<GblData> theData = new ArrayList<GblData>(); ///< List of data blocks
    List<Integer> measDataIndex = new ArrayList<Integer>(); ///< mapping points to data blocks from measurements
    List<Integer> scatDataIndex = new ArrayList<Integer>();  ///< mapping points to data blocks from scatterers
    List<Integer> externalIndex; ///< List of fit parameters used by external seed
    SymMatrix externalSeed; ///< Precision (inverse covariance matrix) of external seed
    List<Matrix> innerTransformations; ///< Transformations at innermost points of
    // composed trajectory (from common external parameters)
    Matrix externalDerivatives; // Derivatives for external measurements of composed trajectory
    Vector externalMeasurements; // Residuals for external measurements of composed trajectory
    Vector externalPrecisions; // Precisions for external measurements of composed trajectory
    VVector theVector; ///< Vector of linear equation system
    BorderedBandMatrix theMatrix = new BorderedBandMatrix(); ///< (Bordered band) matrix of linear equation system

///// Create new (simple) trajectory from list of points.
//    /**
//     * Curved trajectory in space (default) or without curvature (q/p) or in one
//     * plane (u-direction) only. \param [in] aPointList List of points \param
//     * [in] flagCurv Use q/p \param [in] flagU1dir Use in u1 direction \param
//     * [in] flagU2dir Use in u2 direction
//     */
    GblTrajectory(List<GblPoint> aPointList,
                  boolean flagCurv, boolean flagU1dir, boolean flagU2dir)
    {
        numAllPoints = aPointList.size();
        numOffsets = 0;
        numInnerTrans = 0;
        numCurvature = (flagCurv ? 1 : 0);
        numParameters = 0;
        numLocals = 0;
        numMeasurements = 0;
        externalPoint = 0;

        if (flagU1dir) {
            theDimension.add(0);
        }
        if (flagU2dir) {
            theDimension.add(1);
        }
//        // simple (single) trajectory
        thePoints.add(aPointList);
        numPoints.add(numAllPoints);
        construct(); // construct trajectory
    }
/// Create new (simple) trajectory from list of points with external seed.

    /**
     * Curved trajectory in space (default) or without curvature (q/p) or in one
     * plane (u-direction) only. \param [in] aPointList List of points \param
     * [in] aLabel (Signed) label of point for external seed (<0: in front, >0:
     * after point, slope changes at scatterer!) \param [in] aSeed Precision
     * matrix of external seed \param [in] flagCurv Use q/p \param [in]
     * flagU1dir Use in u1 direction \param [in] flagU2dir Use in u2 direction
     */
    GblTrajectory(List<GblPoint> aPointList,
                  int aLabel, SymMatrix aSeed, boolean flagCurv,
                  boolean flagU1dir, boolean flagU2dir)
    {
        numAllPoints = aPointList.size();
        numOffsets = 0;
        numInnerTrans = 0;
        numCurvature = (flagCurv ? 1 : 0);
        numParameters = 0;
        numLocals = 0;
        numMeasurements = 0;

        if (flagU1dir) {
            theDimension.add(0);
        }
        if (flagU2dir) {
            theDimension.add(1);
        }
        // simple (single) trajectory
        thePoints.add(aPointList);
        numPoints.add(numAllPoints);
        construct(); // construct trajectory
    }

/// Retrieve validity of trajectory
    boolean isValid()
    {
        return constructOK;
    }

/// Retrieve number of point from trajectory
    int getNumPoints()
    {
        return numAllPoints;
    }

/// Construct trajectory from list of points.
    /**
     * Trajectory is prepared for fit or output to binary file, may consists of
     * sub-trajectories.
     */
    void construct()
    {

        constructOK = false;
        fitOK = false;
        int aLabel = 0;
        // loop over trajectories
        numTrajectories = thePoints.size();
        for (List<GblPoint> list : thePoints) {
            for (GblPoint p : list) {
                numLocals = max(numLocals, p.getNumLocals());
                numMeasurements += p.hasMeasurement();
                p.setLabel(++aLabel);
            }
        }
        defineOffsets();
        calcJacobians();
        prepare();
        constructOK = true;
        // number of fit parameters
        numParameters = (numOffsets - 2 * numInnerTrans) * theDimension.size()
                + numCurvature + numLocals;
    }
//
/// Define offsets from list of points.

    /**
     * Define offsets at points with scatterers and first and last point. All
     * other points need interpolation from adjacent points with offsets.
     */
    void defineOffsets()
    {

        // loop over trajectories
        for (int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
            List<GblPoint> list = thePoints.get(iTraj);
            int size = list.size();
            // first point is offset
            list.get(0).setOffset(numOffsets++);		// intermediate scatterers are offsets
            for (int i = 1; i < size - 1; ++i) {
                GblPoint p = list.get(i);
                if (p.hasScatterer()) {
                    p.setOffset(numOffsets++);
                } else {
                    p.setOffset(-numOffsets);
                }
            }
            // last point is offset
            list.get(size - 1).setOffset(numOffsets++);
        }
    }

/// Calculate Jacobians to previous/next scatterer from point to point ones.
    void calcJacobians()
    {

        Matrix scatJacobian = new Matrix(5, 5);
        // loop over trajectories
        for (int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
            List<GblPoint> list = thePoints.get(iTraj);
            int size = list.size();
            // forward propagation (all)
            GblPoint previousPoint = list.get(0);
            int numStep = 0;
            for (int i = 1; i < size; ++i) {
                GblPoint p = list.get(i);
                if (numStep == 0) {
                    scatJacobian = p.getP2pJacobian();
                } else {
                    scatJacobian = p.getP2pJacobian().times(scatJacobian);
                }
                numStep++;
                p.addPrevJacobian(scatJacobian);// iPoint -> previous scatterer
                if (p.getOffset() >= 0) {
                    previousPoint.addNextJacobian(scatJacobian); // lastPoint -> next scatterer
                    numStep = 0;
                    previousPoint = p;
                }
            }
            // backward propagation (without scatterers)            
            for (int i = size - 1; i > 0; --i) {
                GblPoint p = list.get(i);
                if (p.getOffset() >= 0) {
                    scatJacobian = p.getP2pJacobian();
                    continue; // skip offsets
                }
                p.addNextJacobian(scatJacobian); // iPoint -> next scatterer
                scatJacobian = scatJacobian.times(p.getP2pJacobian());
            }
        }
    }

/// Get jacobian for transformation from fit to track parameters at point.
    /**
     * Jacobian broken lines (q/p,..,u_i,u_i+1..) to track (q/p,u',u) parameters
     * including additional local parameters. \param [in] aSignedLabel (Signed)
     * label of point for external seed (<0: in front, >0: after point, slope
     * changes at scatterer!) \return List of fit parameters with non zero
     * derivatives and corresponding transformation matrix
     */
    Pair<List< Integer>, Matrix> getJacobian(int aSignedLabel)
    {

        int nDim = theDimension.size();
        int nCurv = numCurvature;
        int nLocals = numLocals;
        int nBorder = nCurv + nLocals;
        int nParBRL = nBorder + 2 * nDim;
        int nParLoc = nLocals + 5;
        List< Integer> anIndex = new ArrayList<Integer>();

        Matrix aJacobian = new Matrix(nParLoc, nParBRL);

        int aLabel = abs(aSignedLabel);
        int firstLabel = 1;
        int lastLabel = 0;
        int aTrajectory = 0;
        // loop over trajectories
        for (int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
            aTrajectory = iTraj;
            lastLabel += numPoints.get(iTraj);
            if (aLabel <= lastLabel) {
                break;
            }
            if (iTraj < numTrajectories - 1) {
                firstLabel += numPoints.get(iTraj);
            }
        }
        int nJacobian; // 0: prev, 1: next
        // check consistency of (index, direction)
        if (aSignedLabel > 0) {
            nJacobian = 1;
            if (aLabel >= lastLabel) {
                aLabel = lastLabel;
                nJacobian = 0;
            }
        } else {
            nJacobian = 0;
            if (aLabel <= firstLabel) {
                aLabel = firstLabel;
                nJacobian = 1;
            }
        }
        GblPoint aPoint = thePoints.get(aTrajectory).get(aLabel - firstLabel);
        List< Integer> labDer = new ArrayList<Integer>();
        for (int i = 0; i < 5; ++i) {
            labDer.add(0);
        }
        Matrix matDer = new Matrix(5, 5);
        getFitToLocalJacobian(labDer, matDer, aPoint, 5, nJacobian);

        // from local parameters
        for (int i = 0; i < nLocals; ++i) {
            aJacobian.set(i + 5, i, 1.0);
            anIndex.add(i + 1);
        }
        // from trajectory parameters
        int iCol = nLocals;
        for (int i = 0; i < 5; ++i) {
            if (labDer.get(i) > 0) {
                anIndex.add(labDer.get(i));
                for (int j = 0; j < 5; ++j) {
                    aJacobian.set(j, iCol, matDer.get(j, i));
                }
                ++iCol;
            }
        }
        return new Pair(anIndex, aJacobian);
    }

/// Get (part of) jacobian for transformation from (trajectory) fit to track parameters at point.
    /**
     * Jacobian broken lines (q/p,..,u_i,u_i+1..) to local (q/p,u',u)
     * parameters. \param [out] anIndex List of fit parameters with non zero
     * derivatives \param [out] aJacobian Corresponding transformation matrix
     * \param [in] aPoint Point to use \param [in] measDim Dimension of
     * 'measurement' (<=2: calculate only offset part, >2: complete matrix)
     * \param [in] nJacobian Direction (0: to previous offset, 1: to next
     * offset)
     */
    void getFitToLocalJacobian(List<Integer> anIndex,
                               Matrix aJacobian, GblPoint aPoint, int measDim,
                               int nJacobian)
    {

        int nDim = theDimension.size();
        int nCurv = numCurvature;
        int nLocals = numLocals;

        int nOffset = aPoint.getOffset();

        if (nOffset < 0) // need interpolation
        {
            Matrix prevW = new Matrix(2, 2);
            Matrix prevWJ = new Matrix(2, 2);
            Matrix nextW = new Matrix(2, 2);
            Matrix nextWJ = new Matrix(2, 2);
            Matrix matN = new Matrix(2, 2);
            Vector prevWd = new Vector(2);
            Vector nextWd = new Vector(2);
            int ierr;
            aPoint.getDerivatives(0, prevW, prevWJ, prevWd); // W-, W- * J-, W- * d-
            aPoint.getDerivatives(1, nextW, nextWJ, nextWd); // W-, W- * J-, W- * d-
            Matrix sumWJ = prevWJ.plus(nextWJ);
//?		matN = sumWJ.inverse(ierr); // N = (W- * J- + W+ * J+)^-1
            // derivatives for u_int
            Matrix prevNW = matN.times(prevW); // N * W-
            Matrix nextNW = matN.times(nextW); // N * W+
            Vector prevNd = matN.times(prevWd); // N * W- * d-
            Vector nextNd = matN.times(nextWd); // N * W+ * d+

            int iOff = nDim * (-nOffset - 1) + nLocals + nCurv + 1; // first offset ('i' in u_i)

            // local offset
            if (nCurv > 0) {
                Vector negDiff = prevNd.minus(nextNd).uminus();
                aJacobian.placeInCol(negDiff, 3, 0); // from curvature
                anIndex.set(0, nLocals + 1);
            }
            aJacobian.placeAt(prevNW, 3, 1); // from 1st Offset
            aJacobian.placeAt(nextNW, 3, 3); // from 2nd Offset
            for (int i = 0; i < nDim; ++i) {
                anIndex.set(1 + theDimension.get(i), iOff + i);
                anIndex.set(3 + theDimension.get(i), iOff + nDim + i);
            }
        } else { // at point
            // anIndex must be sorted
            // forward : iOff2 = iOff1 + nDim, index1 = 1, index2 = 3
            // backward: iOff2 = iOff1 - nDim, index1 = 3, index2 = 1
            int iOff1 = nDim * nOffset + nCurv + nLocals + 1; // first offset ('i' in u_i)
            int index1 = 3 - 2 * nJacobian; // index of first offset
            int iOff2 = iOff1 + nDim * (nJacobian * 2 - 1); // second offset ('i' in u_i)
            int index2 = 1 + 2 * nJacobian; // index of second offset
            // local offset
            aJacobian.set(3, index1, 1.0); // from 1st Offset
            aJacobian.set(4, index1 + 1, 1.0);
            for (int i = 0; i < nDim; ++i) {
                anIndex.set(index1 + theDimension.get(i), iOff1 + i);
            }

            // local slope and curvature
            if (measDim > 2) {
                Matrix matW = new Matrix(2, 2);
                Matrix matWJ = new Matrix(2, 2);
                Vector vecWd = new Vector(2);
                aPoint.getDerivatives(nJacobian, matW, matWJ, vecWd); // W, W * J, W * d
                double sign = (nJacobian > 0) ? 1. : -1.;
                if (nCurv > 0) {
                    aJacobian.set(0, 0, 1.0);
                    aJacobian.placeInCol(vecWd.timesScalar(-sign), 1, 0); // from curvature
                    anIndex.set(0, nLocals + 1);
                }
                aJacobian.placeAt(matWJ.times(-sign), 1, index1); // from 1st Offset
                aJacobian.placeAt(matW.times(sign), 1, index2); // from 2nd Offset
                for (int i = 0; i < nDim; ++i) {
                    anIndex.set(index2 + theDimension.get(i), iOff2 + i);
                }
            }
        }
    }

/// Get jacobian for transformation from (trajectory) fit to kink parameters at point.
    /**
     * Jacobian broken lines (q/p,..,u_i-1,u_i,u_i+1..) to kink (du')
     * parameters. \param [out] anIndex List of fit parameters with non zero
     * derivatives \param [out] aJacobian Corresponding transformation matrix
     * \param [in] aPoint Point to use
     */
    void getFitToKinkJacobian(List< Integer> anIndex,
                              Matrix aJacobian, GblPoint aPoint)
    {

        //nb aJacobian has dimension 2,7
        int nDim = theDimension.size();
        int nCurv = numCurvature;
        int nLocals = numLocals;

        int nOffset = aPoint.getOffset();

        Matrix prevW = new Matrix(2, 2);
        Matrix prevWJ = new Matrix(2, 2);
        Matrix nextW = new Matrix(2, 2);
        Matrix nextWJ = new Matrix(2, 2);
        Vector prevWd = new Vector(2);
        Vector nextWd = new Vector(2);
        aPoint.getDerivatives(0, prevW, prevWJ, prevWd); // W-, W- * J-, W- * d-
        aPoint.getDerivatives(1, nextW, nextWJ, nextWd); // W-, W- * J-, W- * d-
        Matrix sumWJ = prevWJ.plus(nextWJ); // W- * J- + W+ * J+
        Vector sumWd = prevWd.plus(nextWd); // W+ * d+ + W- * d-

        int iOff = (nOffset - 1) * nDim + nCurv + nLocals + 1; // first offset ('i' in u_i)

        // local offset
        if (nCurv > 0) {
            aJacobian.placeInCol(sumWd.uminus(), 0, 0); // from curvature
            anIndex.set(0, nLocals + 1);
        }
        aJacobian.placeAt(prevW, 0, 1); // from 1st Offset
        aJacobian.placeAt(sumWJ.uminus(), 0, 3); // from 2nd Offset
        aJacobian.placeAt(nextW, 0, 5); // from 1st Offset
        for (int i = 0; i < nDim; ++i) {
            anIndex.set(1 + theDimension.get(i), iOff + i);
            anIndex.set(3 + theDimension.get(i), iOff + nDim + i);
            anIndex.set(5 + theDimension.get(i), iOff + nDim * 2 + i);
        }
    }

/// Get fit results at point.
    /**
     * Get corrections and covariance matrix for local track and additional
     * parameters in forward or backward direction. \param [in] aSignedLabel
     * (Signed) label of point on trajectory (<0: in front, >0: after point,
     * slope changes at scatterer!) \param [out] localPar Corrections for local
     * parameters \param [out] localCov Covariance for local parameters \return
     * error code (non-zero if trajectory not fitted successfully)
     */
    int getResults(int aSignedLabel, Vector localPar,
                   SymMatrix localCov)
    {
        if (!fitOK) {
            return 1;
        }
        Pair<List< Integer>, Matrix> indexAndJacobian = getJacobian(aSignedLabel);
        int nParBrl = indexAndJacobian.getFirst().size();
        Vector aVec = new Vector(nParBrl); // compressed vector
        for (int i = 0; i < nParBrl; ++i) {
            aVec.set(i, theVector.get(indexAndJacobian.getFirst().get(i) - 1));
        }
        SymMatrix aMat = theMatrix.getBlockMatrix(indexAndJacobian.getFirst()); // compressed matrix
        localPar.placeAt(indexAndJacobian.getSecond().times(aVec), 0, 0);
        localCov.placeAt(aMat.Similarity(indexAndJacobian.getSecond()), 0, 0);
        return 0;
    }
/// Build linear equation system from data (blocks).

    void buildLinearEquationSystem()
    {
        int nBorder = numCurvature + numLocals;
        theVector = new VVector(numParameters);
        theMatrix.resize(numParameters, nBorder, 5);
        double[] retVals = new double[2];
        double aValue, aWeight;
        int nData = 0;
        for (GblData d : theData) {
            int size = d.getNumParameters();
            int[] indLocal = new int[size];
            double[] derLocal = new double[size];
            d.getLocalData(retVals, indLocal, derLocal);
            aValue = retVals[0];
            aWeight = retVals[1];
            for (int j = 0; j < size; ++j) {
                theVector.addTo(indLocal[j] - 1, derLocal[j] * aWeight * aValue);
            }
            theMatrix.addBlockMatrix(aWeight, indLocal, derLocal);
            nData++;
        }
    }
//}

/// Prepare fit for simple or composed trajectory
    /**
     * Generate data (blocks) from measurements, kinks, external seed and
     * measurements.
     */
    void prepare()
    {
        int nDim = theDimension.size();
        // upper limit
        int maxData = numMeasurements + nDim * (numOffsets - 2);

        for (int i = 0; i < numAllPoints + 3; ++i) {
            measDataIndex.add(i, 0);
        }
        for (int i = 0; i < numAllPoints + 1; ++i) {
            scatDataIndex.add(i, 0);
        }
        int nData = 0;
        List<Matrix> innerTransDer = new ArrayList<Matrix>();
        List<List<Integer>> innerTransLab = new ArrayList<List<Integer>>();
        // measurements
        Matrix matP = new Matrix(5, 5);
        // loop over trajectories
        for (int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
            List<GblPoint> list = thePoints.get(iTraj);
            for (int itPoint = 0; itPoint < list.size(); ++itPoint) {
                GblPoint point = list.get(itPoint);
                Vector aMeas = new Vector(5);
                Vector aPrec = new Vector(5);
                int nLabel = point.getLabel();
                int measDim = point.hasMeasurement();
                if (measDim > 0) {
                    Matrix localDer = point.getLocalDerivatives();
                    List<Integer> globalLab = point.getGlobalLabels();
                    Matrix globalDer = point.getGlobalDerivatives();
                    Matrix transDer = null;
                    point.getMeasurement(matP, aMeas, aPrec);
                    int iOff = 5 - measDim;
                    List<Integer> labDer = new ArrayList<Integer>(5);
                    for (int i = 0; i < 5; ++i) {
                        labDer.add(0);
                    }
                    Matrix matDer = new Matrix(5, 5);
                    Matrix matPDer = new Matrix(5, 5);
                    int nJacobian = (itPoint < list.size() - 1) ? 1 : 0; // last point needs backward propagation
                    getFitToLocalJacobian(labDer, matDer, point, measDim, nJacobian);
                    if (measDim > 2) {
                        matPDer = matP.times(matDer);
                    } else { // 'shortcut' for position measurements
                        Matrix tmp = matP.sub(2, 2, 3, 3).times(matDer.sub(2, 5, 3, 0));
                        matPDer.placeAt(tmp, 3, 0);
                    }

                    for (int i = iOff; i < 5; ++i) {
                        if (aPrec.get(i) > 0.) {
                            GblData aData = new GblData(nLabel, aMeas.get(i), aPrec.get(i));
                            aData.addDerivatives(i, labDer, matPDer, iOff, localDer, globalLab, globalDer, numLocals, transDer);
                            theData.add(aData);
                            nData++;
                        }
                    }
                }// end of check on measDim
                measDataIndex.set(nLabel, nData);
            } //end of loop over points
        } // end loop over trajectories

        // pseudo measurements from kinks
        Matrix matT = new Matrix(2, 2);
        scatDataIndex.set(0, nData);
        scatDataIndex.set(1, nData);
        // loop over trajectories
        for (int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
            List<GblPoint> list = thePoints.get(iTraj);
            for (int itPoint = 1; itPoint < list.size() - 1; ++itPoint) {
                GblPoint point = list.get(itPoint);
                Vector aMeas = new Vector(2);
                Vector aPrec = new Vector(2);
                int nLabel = point.getLabel();
                if (point.hasScatterer()) {
                    point.getScatterer(matT, aMeas, aPrec);
                    Matrix transDer = null;
                    List< Integer> labDer = new ArrayList<Integer>(7);
                    for (int i = 0; i < 7; ++i) {
                        labDer.add(0);
                    }
                    Matrix matDer = new Matrix(2, 7);
                    Matrix matTDer = new Matrix(2, 7);
                    getFitToKinkJacobian(labDer, matDer, point);
                    matTDer = matT.times(matDer);
                    for (int i = 0; i < nDim; ++i) {
                        int iDim = theDimension.get(i);
                        if (aPrec.get(iDim) > 0.) {
                            GblData aData = new GblData(nLabel, aMeas.get(iDim), aPrec.get(iDim));
                            aData.addDerivatives(iDim, labDer, matTDer, numLocals, transDer);
                            theData.add(aData);
                            nData++;
                        }
                    }
                } //end of check on hasScatter
                scatDataIndex.set(nLabel, nData);
            }//end loop over points
            scatDataIndex.set(list.get(list.size() - 1).getLabel(), nData);
        } // end loop over trajectories
        measDataIndex.set(numAllPoints + 1, nData);
        measDataIndex.set(numAllPoints + 2, nData);
    }
//
/// Calculate predictions for all points.

    void predict()
    {
        for (GblData d : theData) {
            d.setPrediction(theVector);
        }
    }

/// Perform fit of trajectory.
    /**
     * Optionally iterate for outlier down-weighting. \param [out] Chi2 Chi2 sum
     * (corrected for down-weighting) \param [out] Ndf Number of degrees of
     * freedom \param [out] lostWeight Sum of weights lost due to down-weighting
     * \param [in] optionList Iterations for down-weighting (One character per
     * iteration: t,h,c (or T,H,C) for Tukey, Huber or Cauchy function) \return
     * Error code (non zero value indicates failure of fit)
     */
    int fit(double[] retDVals, int[] retIVals,
            String optionList)
    {
        final double[] normChi2
                = {
                    1.0, 0.8737, 0.9326, 0.8228
                };
        String methodList = "TtHhCc";

        double Chi2 = 0.;
        int Ndf = -1;
        double lostWeight = 0.;
        if (!constructOK) {
            return 10;
        }
        int aMethod = 0;
        buildLinearEquationSystem();
        lostWeight = 0.;
        int ierr = 0;
        theMatrix.solveAndInvertBorderedBand(theVector, theVector);
        predict();
        Ndf = theData.size() - numParameters;
        Chi2 = 0.;
        for (int i = 0; i < theData.size(); ++i) {
            Chi2 += theData.get(i).getChi2();
        }
        Chi2 /= normChi2[aMethod];
        fitOK = true;
        // now handle the return values
        retDVals[0] = Chi2;
        retDVals[1] = lostWeight;
        retIVals[0] = Ndf;
        return ierr;
    }


// Write trajectory to Millepede-II binary file.
    public void milleOut(MilleBinary aMille)
    {
        if (!constructOK) {
            throw new RuntimeException("GblTrajectory milleOut not properly constructed");
        }
//   data: measurements, kinks and external seed
        for (GblData d : theData) {
            float[] floats = new float[2]; // fValue , fErr
            List<Integer> indLocal = new ArrayList<Integer>();
            List<Double> derLocal = new ArrayList<Double>();
            List<Integer> labGlobal = new ArrayList<Integer>();
            List<Double> derGlobal = new ArrayList<Double>();
            d.getAllData(floats, indLocal, derLocal, labGlobal, derGlobal);
            aMille.addData(floats[0], floats[1], indLocal, derLocal, labGlobal, derGlobal);
        }
        aMille.writeRecord();
    }
/// Print GblTrajectory

    /**
     * \param [in] level print level (0: minimum, >0: more)
     */
    void printTrajectory(int level)
    {
        if (numInnerTrans != 0) {
            System.out.println("Composed GblTrajectory, " + numInnerTrans
                    + " subtrajectories");
        } else {
            System.out.println("Simple GblTrajectory");
        }
        if (theDimension.size() < 2) {
            System.out.println(" 2D-trajectory");
        }
        System.out.println(" Number of GblPoints          : " + numAllPoints
        );
        System.out.println(" Number of points with offsets: " + numOffsets);
        System.out.println(" Number of fit parameters     : " + numParameters
        );
        System.out.println(" Number of measurements       : " + numMeasurements
        );
        if (externalMeasurements != null) {//.getRowDimension()!=0) {
            System.out.println(" Number of ext. measurements  : "
                    + externalMeasurements.getRowDimension());
        }
        if (externalPoint != 0) {
            System.out.println(" Label of point with ext. seed: " + externalPoint
            );
        }
        if (constructOK) {
            System.out.println(" Constructed OK ");
        }
        if (fitOK) {
            System.out.println(" Fitted OK ");
        }
        if (level > 0) {
            if (numInnerTrans != 0) {
                System.out.println(" Inner transformations");
                for (int i = 0; i < numInnerTrans; ++i) {
                    innerTransformations.get(i).print(4, 6);
                }
            }
            if (externalMeasurements != null) { //.getRowDimension()!=0) {
                System.out.println(" External measurements");
                System.out.println("  Measurements:");
                externalMeasurements.print(4, 6);
                System.out.println("  Precisions:");
                externalPrecisions.print(4, 6);
                System.out.println("  Derivatives:");
                externalDerivatives.print(4, 6);
            }
            if (externalPoint != 0) {
                System.out.println(" External seed:");
                externalSeed.print(4, 6);
            }
            if (fitOK) {
                System.out.println(" Fit results");
                System.out.println("  Parameters:");
                theVector.print();
                System.out.println("  Covariance matrix (bordered band part):"
                );
                theMatrix.printMatrix();
            }
        }
    }
//
/// Print \link GblPoint GblPoints \endlink on trajectory

    /**
     * \param [in] level print level (0: minimum, >0: more)
     */
    void printPoints(int level)
    {
        System.out.println("GblPoints ");

        for (List<GblPoint> list : thePoints) {

            for (GblPoint p : list) {
                p.printPoint(level);
            }
        }
    }

/// Print GblData blocks for trajectory
    void printData()
    {
        System.out.println("GblData blocks ");
        for (GblData data : theData) {
            data.printData();
        }
    }

}
