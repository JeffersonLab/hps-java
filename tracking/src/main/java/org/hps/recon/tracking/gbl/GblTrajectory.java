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

import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.gbl.matrix.BorderedBandMatrix;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.VVector;
import org.hps.recon.tracking.gbl.matrix.Vector;


public class GblTrajectory {

	public GblTrajectory(List<GblPoint> listOfPoints) {
		this(listOfPoints, 0, new SymMatrix(0), false, false, false);
	}


	public void fit(double m_chi2, int m_ndf, int m_lost_weight) {
		// TODO Auto-generated method stub
		
	}

	public void milleOut(MilleBinary mille) {
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
    List<Integer> scatDataIndex; ///< mapping points to data blocks from scatterers
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
//    GblTrajectory(List<GblPoint> aPointList,
//            boolean flagCurv, boolean flagU1dir, boolean flagU2dir)
//    {
//        numAllPoints = aPointList.size();
//        //numPoints()
//        numOffsets = 0;
//        numInnerTrans = 0;
//        numCurvature = (flagCurv ? 1 : 0);
//        numParameters = 0;
//        numLocals = 0;
//        numMeasurements = 0;
//        externalPoint = 0;
//                //externalSeed()
//        //innerTransformations()
//        //externalDerivatives()
//        //externalMeasurements()
//        // externalPrecisions() 
//
//        if (flagU1dir)
//        {
//            theDimension.add(0);
//        }
//        if (flagU2dir)
//        {
//            theDimension.add(1);
//        }
//        // simple (single) trajectory
//        thePoints.add(aPointList);
//        numPoints.add(numAllPoints);
////	construct(); // construct trajectory
//    }
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
        //numPoints(), 
        numOffsets = 0;
        numInnerTrans = 0;
        numCurvature = (flagCurv ? 1 : 0);
        numParameters = 0;
        numLocals = 0;
        numMeasurements = 0;
        //externalPoint(aLabel) 
        //theDimension(),
        //thePoints(), 
        //theData(), 
        //measDataIndex(), 
        //scatDataIndex(), 
        //externalIndex(), 
        //externalSeed(aSeed), 
        //innerTransformations(), 
        //externalDerivatives(), 
        //externalMeasurements(), 
        //externalPrecisions() {

        if (flagU1dir)
        {
            theDimension.add(0);
        }
        if (flagU2dir)
        {
            theDimension.add(1);
        }
        // simple (single) trajectory
        thePoints.add(aPointList);
        numPoints.add(numAllPoints);
        construct(); // construct trajectory
    }

///// Create new composed trajectory from list of points and transformations.
///**
// * Composed of curved trajectory in space.
// * \param [in] aPointsAndTransList List containing pairs with list of points and transformation (at inner (first) point)
// */
//GblTrajectory(
//		const List<std::pair<List<GblPoint>, TMatrixD> > &aPointsAndTransList) :
//		numAllPoints(), numPoints(), numOffsets(0), numInnerTrans(
//				aPointsAndTransList.size()), numParameters(0), numLocals(0), numMeasurements(
//				0), externalPoint(0), theDimension(0), thePoints(), theData(), measDataIndex(), scatDataIndex(), externalIndex(), externalSeed(), innerTransformations(), externalDerivatives(), externalMeasurements(), externalPrecisions() {
//
//	for ( int iTraj = 0; iTraj < aPointsAndTransList.size(); ++iTraj) {
//		thePoints.add(aPointsAndTransList[iTraj].first);
//		numPoints.add(thePoints.back().size());
//		numAllPoints += numPoints.back();
//		innerTransformations.add(aPointsAndTransList[iTraj].second);
//	}
//	theDimension.add(0);
//	theDimension.add(1);
//	numCurvature = innerTransformations[0].GetNcols();
//	construct(); // construct (composed) trajectory
//}
//
///// Create new composed trajectory from list of points and transformations with (independent) external measurements.
///**
// * Composed of curved trajectory in space.
// * \param [in] aPointsAndTransList List containing pairs with list of points and transformation (at inner (first) point)
// * \param [in] extDerivatives Derivatives of external measurements vs external parameters
// * \param [in] extMeasurements External measurements (residuals)
// * \param [in] extPrecisions Precision of external measurements
// */
//GblTrajectory(
//		const List<std::pair<List<GblPoint>, TMatrixD> > &aPointsAndTransList,
//		const TMatrixD &extDerivatives, const TVectorD &extMeasurements,
//		const TVectorD &extPrecisions) :
//		numAllPoints(), numPoints(), numOffsets(0), numInnerTrans(
//				aPointsAndTransList.size()), numParameters(0), numLocals(0), numMeasurements(
//				0), externalPoint(0), theDimension(0), thePoints(), theData(), measDataIndex(), scatDataIndex(), externalIndex(), externalSeed(), innerTransformations(), externalDerivatives(
//				extDerivatives), externalMeasurements(extMeasurements), externalPrecisions(
//				extPrecisions) {
//
//	for ( int iTraj = 0; iTraj < aPointsAndTransList.size(); ++iTraj) {
//		thePoints.add(aPointsAndTransList[iTraj].first);
//		numPoints.add(thePoints.back().size());
//		numAllPoints += numPoints.back();
//		innerTransformations.add(aPointsAndTransList[iTraj].second);
//	}
//	theDimension.add(0);
//	theDimension.add(1);
//	numCurvature = innerTransformations[0].GetNcols();
//	construct(); // construct (composed) trajectory
//}
//
//~GblTrajectory() {
//}
//
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
        //std::cout << " numTrajectories: " << numTrajectories << ", "<< innerTransformations.size()  << std::endl;
//	for ( int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
//		List<GblPoint>::iterator itPoint;
//		for (itPoint = thePoints[iTraj].begin();
//				itPoint < thePoints[iTraj].end(); ++itPoint) {
//			numLocals = std::max(numLocals, itPoint->getNumLocals());
//			numMeasurements += itPoint->hasMeasurement();
//			itPoint->setLabel(++aLabel);
//		}
//	}
        for (List<GblPoint> list : thePoints)
        {
            for (GblPoint p : list)
            {
                numLocals = max(numLocals, p.getNumLocals());
                numMeasurements += p.hasMeasurement();
                p.setLabel(++aLabel);
            }
        }
        defineOffsets();
        calcJacobians();
//	try {
		prepare();
//	} catch (std::overflow_error &e) {
//		std::cout << " GblTrajectory construction failed: " << e.what()
//				<< std::endl;
//		return;
//	}
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
        for (int iTraj = 0; iTraj < numTrajectories; ++iTraj)
        {
            List<GblPoint> list = thePoints.get(iTraj);
            int size = list.size();
            // first point is offset
//		thePoints[iTraj].front().setOffset(numOffsets++);
            list.get(0).setOffset(numOffsets++);		// intermediate scatterers are offsets
//		List<GblPoint>::iterator itPoint;
//		for (itPoint = thePoints[iTraj].begin() + 1;
//				itPoint < thePoints[iTraj].end() - 1; ++itPoint) {
//			if (itPoint->hasScatterer()) {
//				itPoint->setOffset(numOffsets++);
//			} else {
//				itPoint->setOffset(-numOffsets);
//			}
//		}
            for (int i = 1; i < size - 1; ++i)
            {
                GblPoint p = list.get(i);
                if (p.hasScatterer())
                {
                    p.setOffset(numOffsets++);
                } else
                {
                    p.setOffset(-numOffsets);
                }

            }
            // last point is offset
//		thePoints[iTraj].back().setOffset(numOffsets++);
            list.get(size - 1).setOffset(numOffsets++);
        }
    }

/// Calculate Jacobians to previous/next scatterer from point to point ones.
    void calcJacobians()
    {

        Matrix scatJacobian = new Matrix(5, 5);
        // loop over trajectories
        for (int iTraj = 0; iTraj < numTrajectories; ++iTraj)
        {
            List<GblPoint> list = thePoints.get(iTraj);
            int size = list.size();
            // forward propagation (all)
            GblPoint previousPoint = list.get(0);
            int numStep = 0;
            for (int i = 1; i < size; ++i)
            {
                GblPoint p = list.get(i);
                if (numStep == 0)
                {
                    scatJacobian = p.getP2pJacobian();
                } else
                {
                    scatJacobian = p.getP2pJacobian().times(scatJacobian);
                }
                numStep++;
                p.addPrevJacobian(scatJacobian);// iPoint -> previous scatterer
                if (p.getOffset() >= 0)
                {
                    previousPoint.addNextJacobian(scatJacobian); // lastPoint -> next scatterer
                    numStep = 0;
                    previousPoint = p;
                }
            }
//            List<GblPoint>::iterator itPoint;
//            for (itPoint = thePoints[iTraj].begin() + 1;
//                    itPoint < thePoints[iTraj].end(); ++itPoint)
//            {
//                if (numStep == 0)
//                {
//                    scatJacobian = itPoint -> getP2pJacobian();
//                } else
//                {
//                    scatJacobian = itPoint -> getP2pJacobian() * scatJacobian;
//                }
//                numStep++;
//                itPoint -> addPrevJacobian(scatJacobian); // iPoint -> previous scatterer
//                if (itPoint -> getOffset() >= 0)
//                {
//                    previousPoint -> addNextJacobian(scatJacobian); // lastPoint -> next scatterer
//                    numStep = 0;
//                    previousPoint =  & ( * itPoint);
//                }
//            }
//            // backward propagation (without scatterers)
//            for (itPoint = thePoints[iTraj].end() - 1;
//                    itPoint > thePoints[iTraj].begin(); --itPoint)
//            {
//                if (itPoint -> getOffset() >= 0)
//                {
//                    scatJacobian = itPoint -> getP2pJacobian();
//                    continue; // skip offsets
//                }
//                itPoint -> addNextJacobian(scatJacobian); // iPoint -> next scatterer
//                scatJacobian = scatJacobian * itPoint -> getP2pJacobian();
//            }
            // backward propagation (without scatterers)            
            for (int i = size - 1; i > 0; --i)
            {
                GblPoint p = list.get(i);
                if (p.getOffset() >= 0)
                {
                    scatJacobian = p.getP2pJacobian();
                    continue; // skip offsets
                }
                p.addNextJacobian(scatJacobian); // iPoint -> next scatterer
                scatJacobian = scatJacobian.times(p.getP2pJacobian());
            }
        }
    }
//
///// Get jacobian for transformation from fit to track parameters at point.
///**
// * Jacobian broken lines (q/p,..,u_i,u_i+1..) to track (q/p,u',u) parameters
// * including additional local parameters.
// * \param [in] aSignedLabel (Signed) label of point for external seed
// * (<0: in front, >0: after point, slope changes at scatterer!)
// * \return List of fit parameters with non zero derivatives and
// * corresponding transformation matrix
// */
//std::pair<List< int>, TMatrixD> getJacobian(
//		int aSignedLabel) const {
//
//	 int nDim = theDimension.size();
//	 int nCurv = numCurvature;
//	 int nLocals = numLocals;
//	 int nBorder = nCurv + nLocals;
//	 int nParBRL = nBorder + 2 * nDim;
//	 int nParLoc = nLocals + 5;
//	List< int> anIndex;
//	anIndex.reserve(nParBRL);
//	TMatrixD aJacobian(nParLoc, nParBRL);
//	aJacobian.Zero();
//
//	 int aLabel = abs(aSignedLabel);
//	 int firstLabel = 1;
//	 int lastLabel = 0;
//	 int aTrajectory = 0;
//	// loop over trajectories
//	for ( int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
//		aTrajectory = iTraj;
//		lastLabel += numPoints[iTraj];
//		if (aLabel <= lastLabel)
//			break;
//		if (iTraj < numTrajectories - 1)
//			firstLabel += numPoints[iTraj];
//	}
//	int nJacobian; // 0: prev, 1: next
//	// check consistency of (index, direction)
//	if (aSignedLabel > 0) {
//		nJacobian = 1;
//		if (aLabel >= lastLabel) {
//			aLabel = lastLabel;
//			nJacobian = 0;
//		}
//	} else {
//		nJacobian = 0;
//		if (aLabel <= firstLabel) {
//			aLabel = firstLabel;
//			nJacobian = 1;
//		}
//	}
//	const GblPoint aPoint = thePoints[aTrajectory][aLabel - firstLabel];
//	List< int> labDer(5);
//	SMatrix55 matDer;
//	getFitToLocalJacobian(labDer, matDer, aPoint, 5, nJacobian);
//
//	// from local parameters
//	for ( int i = 0; i < nLocals; ++i) {
//		aJacobian(i + 5, i) = 1.0;
//		anIndex.add(i + 1);
//	}
//	// from trajectory parameters
//	 int iCol = nLocals;
//	for ( int i = 0; i < 5; ++i) {
//		if (labDer[i] > 0) {
//			anIndex.add(labDer[i]);
//			for ( int j = 0; j < 5; ++j) {
//				aJacobian(j, iCol) = matDer(j, i);
//			}
//			++iCol;
//		}
//	}
//	return std::make_pair(anIndex, aJacobian);
//}

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
            if (nCurv > 0)
            {
                Vector negDiff = prevNd.minus(nextNd).uminus();
                aJacobian.placeInCol(negDiff, 3, 0); // from curvature
                anIndex.set(0, nLocals + 1);
            }
            aJacobian.placeAt(prevNW, 3, 1); // from 1st Offset
            aJacobian.placeAt(nextNW, 3, 3); // from 2nd Offset
            for (int i = 0; i < nDim; ++i)
            {
                anIndex.set(1 + theDimension.get(i), iOff + i);
                anIndex.set(3 + theDimension.get(i), iOff + nDim + i);
            }
//
////		// local slope and curvature
////		if (measDim > 2) {
////			// derivatives for u'_int
////			const SMatrix22 prevWPN(nextWJ * prevNW); // W+ * J+ * N * W-
////			const SMatrix22 nextWPN(prevWJ * nextNW); // W- * J- * N * W+
////			const SVector2 prevWNd(nextWJ * prevNd); // W+ * J+ * N * W- * d-
////			const SVector2 nextWNd(prevWJ * nextNd); // W- * J- * N * W+ * d+
////			if (nCurv > 0) {
////				aJacobian(0, 0) = 1.0;
////				aJacobian.Place_in_col(prevWNd - nextWNd, 1, 0); // from curvature
////			}
////			aJacobian.Place_at(-prevWPN, 1, 1); // from 1st Offset
////			aJacobian.Place_at(nextWPN, 1, 3); // from 2nd Offset
////		}
//	} else { // at point
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
            for (int i = 0; i < nDim; ++i)
            {
                anIndex.set(index1 + theDimension.get(i), iOff1 + i);
            }

            // local slope and curvature
            if (measDim > 2)
            {
                Matrix matW = new Matrix(2, 2);
                Matrix matWJ = new Matrix(2, 2);
                Vector vecWd = new Vector(2);
                aPoint.getDerivatives(nJacobian, matW, matWJ, vecWd); // W, W * J, W * d
                double sign = (nJacobian > 0) ? 1. : -1.;
                if (nCurv > 0)
                {
                    aJacobian.set(0, 0, 1.0);
                    aJacobian.placeInCol(vecWd.timesScalar(-sign), 1, 0); // from curvature
                    anIndex.set(0, nLocals + 1);
                }
                aJacobian.placeAt(matWJ.times(-sign), 1, index1); // from 1st Offset
                aJacobian.placeAt(matW.times(sign), 1, index2); // from 2nd Offset
                for (int i = 0; i < nDim; ++i)
                {
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
        if (nCurv > 0)
        {
            aJacobian.placeInCol(sumWd.uminus(), 0, 0); // from curvature
            anIndex.set(0, nLocals + 1);
        }
        aJacobian.placeAt(prevW, 0, 1); // from 1st Offset
        aJacobian.placeAt(sumWJ.uminus(), 0, 3); // from 2nd Offset
        aJacobian.placeAt(nextW, 0, 5); // from 1st Offset
        for (int i = 0; i < nDim; ++i)
        {
            anIndex.set(1 + theDimension.get(i), iOff + i);
            anIndex.set(3 + theDimension.get(i), iOff + nDim + i);
            anIndex.set(5 + theDimension.get(i), iOff + nDim * 2 + i);
        }
    }

///// Get fit results at point.
///**
// * Get corrections and covariance matrix for local track and additional parameters
// * in forward or backward direction.
// * \param [in] aSignedLabel (Signed) label of point on trajectory
// * (<0: in front, >0: after point, slope changes at scatterer!)
// * \param [out] localPar Corrections for local parameters
// * \param [out] localCov Covariance for local parameters
// * \return error code (non-zero if trajectory not fitted successfully)
// */
// int getResults(int aSignedLabel, TVectorD &localPar,
//		SymMatrix &localCov) const {
//	if (! fitOK)
//		return 1;
//	std::pair<List< int>, TMatrixD> indexAndJacobian =
//			getJacobian(aSignedLabel);
//	 int nParBrl = indexAndJacobian.first.size();
//	TVectorD aVec(nParBrl); // compressed vector
//	for ( int i = 0; i < nParBrl; ++i) {
//		aVec[i] = theVector(indexAndJacobian.first[i] - 1);
//	}
//	SymMatrix aMat = theMatrix.getBlockMatrix(indexAndJacobian.first); // compressed matrix
//	localPar = indexAndJacobian.second * aVec;
//	localCov = aMat.Similarity(indexAndJacobian.second);
//	return 0;
//}
//
///// Get residuals at point from measurement.
///**
// * Get (diagonalized) residual, error of measurement and residual and down-weighting
// * factor for measurement at point
// *
// * \param [in]  aLabel Label of point on trajectory
// * \param [out] numData Number of data blocks from measurement at point
// * \param [out] aResiduals Measurements-Predictions
// * \param [out] aMeasErrors Errors of Measurements
// * \param [out] aResErrors Errors of Residuals (including correlations from track fit)
// * \param [out] aDownWeights Down-Weighting factors
// * \return error code (non-zero if trajectory not fitted successfully)
// */
// int getMeasResults( int aLabel,
//		 int &numData, TVectorD &aResiduals, TVectorD &aMeasErrors,
//		TVectorD &aResErrors, TVectorD &aDownWeights) {
//	numData = 0;
//	if (! fitOK)
//		return 1;
//
//	 int firstData = measDataIndex[aLabel - 1]; // first data block with measurement
//	numData = measDataIndex[aLabel] - firstData; // number of data blocks
//	for ( int i = 0; i < numData; ++i) {
//		getResAndErr(firstData + i, aResiduals[i], aMeasErrors[i],
//				aResErrors[i], aDownWeights[i]);
//	}
//	return 0;
//}
//
///// Get (kink) residuals at point from scatterer.
///**
// * Get (diagonalized) residual, error of measurement and residual and down-weighting
// * factor for scatterering kinks at point
// *
// * \param [in]  aLabel Label of point on trajectory
// * \param [out] numData Number of data blocks from scatterer at point
// * \param [out] aResiduals (kink)Measurements-(kink)Predictions
// * \param [out] aMeasErrors Errors of (kink)Measurements
// * \param [out] aResErrors Errors of Residuals (including correlations from track fit)
// * \param [out] aDownWeights Down-Weighting factors
// * \return error code (non-zero if trajectory not fitted successfully)
// */
// int getScatResults( int aLabel,
//		 int &numData, TVectorD &aResiduals, TVectorD &aMeasErrors,
//		TVectorD &aResErrors, TVectorD &aDownWeights) {
//	numData = 0;
//	if (! fitOK)
//		return 1;
//
//	 int firstData = scatDataIndex[aLabel - 1]; // first data block with scatterer
//	numData = scatDataIndex[aLabel] - firstData; // number of data blocks
//	for ( int i = 0; i < numData; ++i) {
//		getResAndErr(firstData + i, aResiduals[i], aMeasErrors[i],
//				aResErrors[i], aDownWeights[i]);
//	}
//	return 0;
//}
//
///// Get (list of) labels of points on (simple) trajectory
///**
// * \param [out] aLabelList List of labels (aLabelList[i] = i+1)
// */
//void getLabels(List< int> &aLabelList) {
//	 int aLabel = 0;
//	 int nPoint = thePoints[0].size();
//	aLabelList.resize(nPoint);
//	for ( i = 0; i < nPoint; ++i) {
//		aLabelList[i] = ++aLabel;
//	}
//}
//
///// Get (list of lists of) labels of points on (composed) trajectory
///**
// * \param [out] aLabelList List of of lists of labels
// */
//void getLabels(
//		List<List< int> > &aLabelList) {
//	 int aLabel = 0;
//	aLabelList.resize(numTrajectories);
//	for ( int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
//		 int nPoint = thePoints[iTraj].size();
//		aLabelList[iTraj].resize(nPoint);
//		for ( i = 0; i < nPoint; ++i) {
//			aLabelList[iTraj][i] = ++aLabel;
//		}
//	}
//}
//
///// Get residual and errors from data block.
///**
// * Get residual, error of measurement and residual and down-weighting
// * factor for (single) data block
// * \param [in]  aData Label of data block
// * \param [out] aResidual Measurement-Prediction
// * \param [out] aMeasError Error of Measurement
// * \param [out] aResError Error of Residual (including correlations from track fit)
// * \param [out] aDownWeight Down-Weighting factor
// */
//void getResAndErr( int aData, double &aResidual,
//		double &aMeasError, double &aResError, double &aDownWeight) {
//
//	double aMeasVar;
//	List< int>* indLocal;
//	List<double>* derLocal;
//	theData[aData].getResidual(aResidual, aMeasVar, aDownWeight, indLocal,
//			derLocal);
//	 int nParBrl = (*indLocal).size();
//	TVectorD aVec(nParBrl); // compressed vector of derivatives
//	for ( int j = 0; j < nParBrl; ++j) {
//		aVec[j] = (*derLocal)[j];
//	}
//	SymMatrix aMat = theMatrix.getBlockMatrix(*indLocal); // compressed (covariance) matrix
//	double aFitVar = aMat.Similarity(aVec); // variance from track fit
//	aMeasError = sqrt(aMeasVar); // error of measurement
//	aResError = (aFitVar < aMeasVar ? sqrt(aMeasVar - aFitVar) : 0.); // error of residual
//}
/// Build linear equation system from data (blocks).
    void buildLinearEquationSystem()
    {
        int nBorder = numCurvature + numLocals;
//	theVector. resize(numParameters);
        theMatrix.resize(numParameters, nBorder, 5);
        double[] retVals = new double[2];
        double aValue, aWeight;
        int[] indLocal = null;
        double[] derLocal = null;
        for (GblData d : theData)
        {
            d.getLocalData(retVals, indLocal, derLocal);
            aValue = retVals[0];
            aWeight = retVals[1];
            for (int j = 0; j < indLocal.length; ++j)
            {
                theVector.addTo(indLocal[j] - 1, derLocal[j] * aWeight * aValue);
            }
            theMatrix.addBlockMatrix(aWeight, indLocal, derLocal);
        }
    }
//}

/// Prepare fit for simple or composed trajectory
/**
 * Generate data (blocks) from measurements, kinks, external seed and measurements.
 */
void prepare() {
	 int nDim = theDimension.size();
	// upper limit
	 int maxData = numMeasurements + nDim * (numOffsets - 2);
//cng			+ externalSeed.getRowDimension();
//	theData.reserve(maxData);
//	measDataIndex.resize(numAllPoints + 3); // include external seed and measurements
         //cng
         for(int i = 0; i<numAllPoints + 3; ++i) measDataIndex.add(i,0);
         //cng
//	scatDataIndex.resize(numAllPoints + 1);
	 int nData = 0;
	List<Matrix> innerTransDer = new ArrayList<Matrix>();
	List<List<Integer> > innerTransLab = new ArrayList<List<Integer>>();
//	// composed trajectory ?
//	if (numInnerTrans > 0) {
//		//std::cout << "composed trajectory" << std::endl;
//		for ( int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
//			// innermost point
//			GblPoint* innerPoint = &thePoints[iTraj].front();
//			// transformation fit to local track parameters
//			List< int> firstLabels(5);
//			SMatrix55 matFitToLocal, matLocalToFit;
//			getFitToLocalJacobian(firstLabels, matFitToLocal, *innerPoint, 5);
//			// transformation local track to fit parameters
//			int ierr;
//			matLocalToFit = matFitToLocal.Inverse(ierr);
//			TMatrixD localToFit(5, 5);
//			for ( int i = 0; i < 5; ++i) {
//				for ( int j = 0; j < 5; ++j) {
//					localToFit(i, j) = matLocalToFit(i, j);
//				}
//			}
//			// transformation external to fit parameters at inner (first) point
//			innerTransDer.add(localToFit * innerTransformations[iTraj]);
//			innerTransLab.add(firstLabels);
//		}
//	}
	// measurements
	Matrix matP = new Matrix(5,5);
//	// loop over trajectories
//	List<GblPoint>::iterator itPoint;
//	for ( int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
//		for (itPoint = thePoints[iTraj].begin();
//				itPoint < thePoints[iTraj].end(); ++itPoint) {
//			SVector5 aMeas, aPrec;
//			 int nLabel = itPoint->getLabel();
//			 int measDim = itPoint->hasMeasurement();
//			if (measDim) {
//				const TMatrixD localDer = itPoint->getLocalDerivatives();
//				const List<int> globalLab = itPoint->getGlobalLabels();
//				const TMatrixD globalDer = itPoint->getGlobalDerivatives();
//				TMatrixD transDer;
//				itPoint->getMeasurement(matP, aMeas, aPrec);
//				 int iOff = 5 - measDim; // first active component
//				List< int> labDer(5);
//				SMatrix55 matDer, matPDer;
//				 int nJacobian =
//						(itPoint < thePoints[iTraj].end() - 1) ? 1 : 0; // last point needs backward propagation
//				getFitToLocalJacobian(labDer, matDer, *itPoint, measDim,
//						nJacobian);
//				if (measDim > 2) {
//					matPDer = matP * matDer;
//				} else { // 'shortcut' for position measurements
//					matPDer.Place_at(
//							matP.Sub<SMatrix22>(3, 3)
//									* matDer.Sub<SMatrix25>(3, 0), 3, 0);
//				}
//
////				if (numInnerTrans > 0) {
////					// transform for external parameters
////					TMatrixD proDer(measDim, 5);
////					// match parameters
////					 int ifirst = 0;
////					 int ilabel = 0;
////					while (ilabel < 5) {
////						if (labDer[ilabel] > 0) {
////							while (innerTransLab[iTraj][ifirst]
////									!= labDer[ilabel] && ifirst < 5) {
////								++ifirst;
////							}
////							if (ifirst >= 5) {
////								labDer[ilabel] -= 2 * nDim * (iTraj + 1); // adjust label
////							} else {
////								// match
////								labDer[ilabel] = 0; // mark as related to external parameters
////								for ( int k = iOff; k < 5; ++k) {
////									proDer(k - iOff, ifirst) = matPDer(k,
////											ilabel);
////								}
////							}
////						}
////						++ilabel;
////					}
////					transDer.ResizeTo(measDim, numCurvature);
////					transDer = proDer * innerTransDer[iTraj];
////				}
//				for ( int i = iOff; i < 5; ++i) {
//					if (aPrec(i) > 0.) {
//						GblData aData(nLabel, aMeas(i), aPrec(i));
//						aData.addDerivatives(i, labDer, matPDer, iOff, localDer,
//								globalLab, globalDer, numLocals, transDer);
//						theData.add(aData);
//						nData++;
//					}
//				}
//
//			}
//			measDataIndex[nLabel] = nData;
//		}
//	} // end loop over trajectories

//	// pseudo measurements from kinks
//	SMatrix22 matT;
//	scatDataIndex[0] = nData;
//	scatDataIndex[1] = nData;
//	// loop over trajectories
//	for ( int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
//		for (itPoint = thePoints[iTraj].begin() + 1;
//				itPoint < thePoints[iTraj].end() - 1; ++itPoint) {
//			SVector2 aMeas, aPrec;
//			 int nLabel = itPoint->getLabel();
//			if (itPoint->hasScatterer()) {
//				itPoint->getScatterer(matT, aMeas, aPrec);
//				TMatrixD transDer;
//				List< int> labDer(7);
//				SMatrix27 matDer, matTDer;
//				getFitToKinkJacobian(labDer, matDer, *itPoint);
//				matTDer = matT * matDer;
////				if (numInnerTrans > 0) {
////					// transform for external parameters
////					TMatrixD proDer(nDim, 5);
////					// match parameters
////					 int ifirst = 0;
////					 int ilabel = 0;
////					while (ilabel < 7) {
////						if (labDer[ilabel] > 0) {
////							while (innerTransLab[iTraj][ifirst]
////									!= labDer[ilabel] && ifirst < 5) {
////								++ifirst;
////							}
////							if (ifirst >= 5) {
////								labDer[ilabel] -= 2 * nDim * (iTraj + 1); // adjust label
////							} else {
////								// match
////								labDer[ilabel] = 0; // mark as related to external parameters
////								for ( int k = 0; k < nDim; ++k) {
////									proDer(k, ifirst) = matTDer(k, ilabel);
////								}
////							}
////						}
////						++ilabel;
////					}
////					transDer.ResizeTo(nDim, numCurvature);
////					transDer = proDer * innerTransDer[iTraj];
////				}
//				for ( int i = 0; i < nDim; ++i) {
//					 int iDim = theDimension[i];
//					if (aPrec(iDim) > 0.) {
//						GblData aData(nLabel, aMeas(iDim), aPrec(iDim));
//						aData.addDerivatives(iDim, labDer, matTDer, numLocals,
//								transDer);
//						theData.add(aData);
//						nData++;
//					}
//				}
//			}
//			scatDataIndex[nLabel] = nData;
//		}
//		scatDataIndex[thePoints[iTraj].back().getLabel()] = nData;
//	} // end loop over trajectories

//	// external seed
//	if (externalPoint > 0) {
//		std::pair<List< int>, TMatrixD> indexAndJacobian =
//				getJacobian(externalPoint);
//		externalIndex = indexAndJacobian.first;
//		List<double> externalDerivatives(externalIndex.size());
//		const SymMatrixEigen externalEigen(externalSeed);
//		const TVectorD valEigen = externalEigen.GetEigenValues();
//		TMatrixD vecEigen = externalEigen.GetEigenVectors();
//		vecEigen = vecEigen.T() * indexAndJacobian.second;
//		for (int i = 0; i < externalSeed.GetNrows(); ++i) {
//			if (valEigen(i) > 0.) {
//				for (int j = 0; j < externalSeed.GetNcols(); ++j) {
//					externalDerivatives[j] = vecEigen(i, j);
//				}
//				GblData aData(externalPoint, 0., valEigen(i));
//				aData.addDerivatives(externalIndex, externalDerivatives);
//				theData.add(aData);
//				nData++;
//			}
//		}
//	}
	measDataIndex.set(numAllPoints + 1, nData);
	// external measurements
//	 int nExt = externalMeasurements.getRowDimension();
//	if (nExt > 0) {
//		List< int> index(numCurvature);
//		List<double> derivatives(numCurvature);
//		for ( int iExt = 0; iExt < nExt; ++iExt) {
//			for ( int iCol = 0; iCol < numCurvature; ++iCol) {
//				index[iCol] = iCol + 1;
//				derivatives[iCol] = externalDerivatives(iExt, iCol);
//			}
//			GblData aData(1U, externalMeasurements(iExt),
//					externalPrecisions(iExt));
//			aData.addDerivatives(index, derivatives);
//			theData.add(aData);
//			nData++;
//		}
//	}
	measDataIndex.set(numAllPoints + 2, nData);
        
}
//
/// Calculate predictions for all points.

    void predict()
    {
        for (GblData d : theData)
        {
            d.setPrediction(theVector);
        }
    }
//
///// Down-weight all points.
///**
// * \param [in] aMethod M-estimator (1: Tukey, 2:Huber, 3:Cauchy)
// */
//double downWeight( int aMethod) {
//	double aLoss = 0.;
//	List<GblData>::iterator itData;
//	for (itData = theData.begin(); itData < theData.end(); ++itData) {
//		aLoss += (1. - itData->setDownWeighting(aMethod));
//	}
//	return aLoss;
//}

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
        final double[] normChi2 =
        {
            1.0, 0.8737, 0.9326, 0.8228
        };
        String methodList = "TtHhCc";

        double Chi2 = 0.;
        int Ndf = -1;
        double lostWeight = 0.;
        if (!constructOK)
        {
            return 10;
        }

        int aMethod = 0;

        buildLinearEquationSystem();
        theMatrix.printMatrix();
        lostWeight = 0.;
        int ierr = 0;
//	try {

        theMatrix.solveAndInvertBorderedBand(theVector, theVector);
        predict();

//		for ( int i = 0; i < optionList.size(); ++i) // down weighting iterations
//				{
//			size_t aPosition = methodList.find(optionList[i]);
//			if (aPosition != std::string::npos) {
//				aMethod = aPosition / 2 + 1;
//				lostWeight = downWeight(aMethod);
//				buildLinearEquationSystem();
//				theMatrix.solveAndInvertBorderedBand(theVector, theVector);
//				predict();
//			}
//		}
        Ndf = theData.size() - numParameters;
        Chi2 = 0.;
        for (int i = 0; i < theData.size(); ++i)
        {
            Chi2 += theData.get(i).getChi2();
        }
        Chi2 /= normChi2[aMethod];
        fitOK = true;

//	} catch (int e) {
//		std::cout << " fit exception " << e << std::endl;
//		ierr = e;
//	}
        // now handle the return values
        retDVals[0] = Chi2;
        retDVals[1] = lostWeight;
        retIVals[0] = Ndf;
        return ierr;
    }

///// Write trajectory to Millepede-II binary file.
//void milleOut(MilleBinary &aMille) {
//	float fValue;
//	float fErr;
//	List< int>* indLocal;
//	List<double>* derLocal;
//	List<int>* labGlobal;
//	List<double>* derGlobal;
//
//	if (not constructOK)
//		return;
//
////   data: measurements, kinks and external seed
//	List<GblData>::iterator itData;
//	for (itData = theData.begin(); itData != theData.end(); ++itData) {
//		itData->getAllData(fValue, fErr, indLocal, derLocal, labGlobal,
//				derGlobal);
//		aMille.addData(fValue, fErr, *indLocal, *derLocal, *labGlobal,
//				*derGlobal);
//	}
//	aMille.writeRecord();
//}
//
///// Print GblTrajectory
///**
// * \param [in] level print level (0: minimum, >0: more)
// */
//void printTrajectory( int level) {
//	if (numInnerTrans) {
//		std::cout << "Composed GblTrajectory, " << numInnerTrans
//				<< " subtrajectories" << std::endl;
//	} else {
//		std::cout << "Simple GblTrajectory" << std::endl;
//	}
//	if (theDimension.size() < 2) {
//		std::cout << " 2D-trajectory" << std::endl;
//	}
//	std::cout << " Number of GblPoints          : " << numAllPoints
//			<< std::endl;
//	std::cout << " Number of points with offsets: " << numOffsets << std::endl;
//	std::cout << " Number of fit parameters     : " << numParameters
//			<< std::endl;
//	std::cout << " Number of measurements       : " << numMeasurements
//			<< std::endl;
//	if (externalMeasurements.GetNrows()) {
//		std::cout << " Number of ext. measurements  : "
//				<< externalMeasurements.GetNrows() << std::endl;
//	}
//	if (externalPoint) {
//		std::cout << " Label of point with ext. seed: " << externalPoint
//				<< std::endl;
//	}
//	if (constructOK) {
//		std::cout << " Constructed OK " << std::endl;
//	}
//	if (fitOK) {
//		std::cout << " Fitted OK " << std::endl;
//	}
//	if (level > 0) {
//		if (numInnerTrans) {
//			std::cout << " Inner transformations" << std::endl;
//			for ( int i = 0; i < numInnerTrans; ++i) {
//				innerTransformations[i].Print();
//			}
//		}
//		if (externalMeasurements.GetNrows()) {
//			std::cout << " External measurements" << std::endl;
//			std::cout << "  Measurements:" << std::endl;
//			externalMeasurements.Print();
//			std::cout << "  Precisions:" << std::endl;
//			externalPrecisions.Print();
//			std::cout << "  Derivatives:" << std::endl;
//			externalDerivatives.Print();
//		}
//		if (externalPoint) {
//			std::cout << " External seed:" << std::endl;
//			externalSeed.Print();
//		}
//		if (fitOK) {
//			std::cout << " Fit results" << std::endl;
//			std::cout << "  Parameters:" << std::endl;
//			theVector.print();
//			std::cout << "  Covariance matrix (bordered band part):"
//					<< std::endl;
//			theMatrix.printMatrix();
//		}
//	}
//}
//
/// Print \link GblPoint GblPoints \endlink on trajectory
    /**
     * \param [in] level print level (0: minimum, >0: more)
     */
    void printPoints(int level)
    {
        System.out.println("GblPoints ");

        for (List<GblPoint> list : thePoints)
        {

            for (GblPoint p : list)
            {
                p.printPoint(level);
            }
        }

//	for ( int iTraj = 0; iTraj < numTrajectories; ++iTraj) {
//		List<GblPoint>::iterator itPoint;
//		for (itPoint = thePoints[iTraj].begin();
//				itPoint < thePoints[iTraj].end(); ++itPoint) {
//			itPoint->printPoint(level);
//		}
//	}
    }
//
///// Print GblData blocks for trajectory
//void printData() {
//	std::cout << "GblData blocks " << std::endl;
//	List<GblData>::iterator itData;
//	for (itData = theData.begin(); itData < theData.end(); ++itData) {
//		itData->printData();
//	}
//}    
        
        
        
        
}
