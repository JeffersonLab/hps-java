package org.hps.recon.tracking.gbl;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.MultipleScattering.ScatterPoint;
import org.hps.recon.tracking.MultipleScattering.ScatterPoints;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.lcsim.constants.Constants;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

/**
 * Class to running GBL on HPS track.
 *
 * @author phansson
 *
 * @version $Id:
 *
 */
public class HpsGblFitter {

    private final boolean _debug = true;
    private double _B = 0.;
    private double _bfac = 0.;
    private boolean isMC = false;
    TrackerHitUtils _trackHitUtils = null;
    MultipleScattering _scattering = null;
    private double m_chi2 = -1.;
    private int m_ndf = -1;
    private int m_lost_weight = -1;
    GblTrajectory _traj = null;
    MilleBinary _mille = null;

    public HpsGblFitter() {
        _B = -0.5;
        _bfac = _B * Constants.fieldConversion;
        _trackHitUtils = new TrackerHitUtils();
        _scattering = new MultipleScattering(new MaterialSupervisor());
        System.out.println("Default constructor");
    }

    public HpsGblFitter(double Bz, MultipleScattering scattering, boolean isMCFlag) {
        System.out.printf("%s: Constructor\n", getClass().getSimpleName());
        isMC = isMCFlag;
        _B = Bz;
        _bfac = Bz * Constants.fieldConversion;
        _trackHitUtils = new TrackerHitUtils();
        _scattering = scattering;
        System.out.printf("%s: b-field set to %f (%f)\n", getClass().getSimpleName(), _B, _bfac);
        System.out.printf("%s: Constructor end\n", getClass().getSimpleName());
    }

    public void setMilleBinary(MilleBinary mille) {
        _mille = mille;
    }

    public void clear() {
        m_chi2 = -1.;
        m_ndf = -1;
        m_lost_weight = -1;
        _traj = null;
    }

    public int Fit(Track track) {

        // Check that things are setup
        if (_B == 0.) {
            System.out.printf("%s: B-field not set!\n", this.getClass().getSimpleName());
            return -1;
        }
        if (_scattering == null) {
            System.out.printf("%s: Multiple scattering calculator not set!\n", this.getClass().getSimpleName());
            return -2;
        }

        // Time the fits
        //clock_t startTime = clock();
        // path length along trajectory
        double s = 0.;
        // jacobian to transport errors between points along the path
        BasicMatrix jacPointToPoint = GblUtils.unitMatrix(5, 5);
        // Option to use uncorrelated  MS errors
        // This is similar to what is done in lcsim seedtracker
        // The msCov below holds the MS errors
        // This is for testing purposes only.
        boolean useUncorrMS = false;
//        BasicMatrix msCov = GblUtils.getInstance().zeroMatrix(5, 5);

        // Vector of the strip clusters used for the GBL fit
        List<GblPoint> listOfPoints = new ArrayList<GblPoint>();

        // Store the projection from local to measurement frame for each strip cluster
        // need to use pointer for TMatrix here?
//        Map<Integer, Matrix> proL2m_list = new HashMap<Integer, Matrix>();
        // Save the association between strip cluster and label	
//        Map<HelicalTrackStrip, Integer> stripLabelMap = new HashMap<HelicalTrackStrip, Integer>();
        //start trajectory at refence point (s=0) - this point has no measurement
        GblPoint ref_point = new GblPoint(jacPointToPoint);
        listOfPoints.add(ref_point);

        //Create a list of all the strip clusters making up the track 
        List<HelicalTrackStrip> stripClusters = new ArrayList<HelicalTrackStrip>();
        SeedCandidate seed = ((SeedTrack) track).getSeedCandidate();
        HelicalTrackFit htf = seed.getHelix();
        List<HelicalTrackHit> stereoHits = seed.getHits();
        for (int ihit = 0; ihit < stereoHits.size(); ++ihit) {
            HelicalTrackCross cross = (HelicalTrackCross) stereoHits.get(ihit);
            stripClusters.add(cross.getStrips().get(0));
            stripClusters.add(cross.getStrips().get(1));
        }

        // sort the clusters along path
        // TODO use actual path length and not layer id!
        //Collections.sort(stripClusters, new HelicalTrackStripComparer());
        Collections.sort(stripClusters, new Comparator<HelicalTrackStrip>() {
            @Override
            public int compare(HelicalTrackStrip o1, HelicalTrackStrip o2) {
                return o1.layer() < o2.layer() ? -1 : o1.layer() > o2.layer() ? 1 : 0;
            }
        });

        if (_debug) {
            System.out.printf(" %d strip clusters:\n", stripClusters.size());
            for (int istrip = 0; istrip < stripClusters.size(); ++istrip) {
                System.out.printf("layer %d origin %s\n", stripClusters.get(istrip).layer(), stripClusters.get(istrip).origin().toString());
            }
        }

        // Find scatter points along the path of the track
        ScatterPoints scatters = _scattering.FindHPSScatterPoints(htf);

        if (_debug) {
            System.out.printf(" Process %d strip clusters\n", stripClusters.size());
        }
        for (int istrip = 0; istrip < stripClusters.size(); ++istrip) {

            HelicalTrackStripGbl strip = new HelicalTrackStripGbl(stripClusters.get(istrip), true);

            if (_debug) {
                System.out.printf(" layer %d origin %s\n", strip.layer(), strip.origin().toString());
            }

            //Find intercept point with sensor in tracking frame
            Hep3Vector trkpos = TrackUtils.getHelixPlaneIntercept(htf, strip, Math.abs(_B));
            if (_debug) {
                System.out.printf("trkpos at intercept [%.10f %.10f %.10f]\n", trkpos.x(), trkpos.y(), trkpos.z());
            }

            //path length to intercept
            double path = HelixUtils.PathToXPlane(htf, trkpos.x(), 0, 0).get(0);
            double path3D = path / Math.cos(Math.atan(htf.slope()));

            // Path length step for this cluster
            double step = path3D - s;

            if (_debug) {
                System.out.printf("%s Path length step %f from %f to %f\n", this.getClass().getSimpleName(), step, s, path3D);
            }

            // Measurement direction (perpendicular and parallel to strip direction)
            BasicMatrix mDir = new BasicMatrix(2, 3);
            mDir.setElement(0, 0, strip.u().x());
            mDir.setElement(0, 1, strip.u().y());
            mDir.setElement(0, 2, strip.u().z());
            mDir.setElement(1, 0, strip.v().x());
            mDir.setElement(1, 1, strip.v().y());
            mDir.setElement(1, 2, strip.v().z());

            Matrix mDirT = MatrixOp.transposed(mDir); //new BasicMatrix(MatrixOp.transposed(mDir));
            if (_debug) {
                System.out.printf(" mDir \n%s\n%s\n", this.getClass().getSimpleName(), mDir.toString());
                System.out.printf(" mDirT \n%s\n%s\n", this.getClass().getSimpleName(), mDirT.toString());
            }

            // Track direction 
            double sinLambda = Math.sin(Math.atan(htf.slope()));
            double cosLambda = Math.sqrt(1.0 - sinLambda * sinLambda);
            double sinPhi = Math.sin(htf.phi0());
            double cosPhi = Math.sqrt(1.0 - sinPhi * sinPhi);

            // Track direction in curvilinear frame (U,V,T)
            // U = Z x T / |Z x T|, V = T x U
            BasicMatrix uvDir = new BasicMatrix(2, 3);
            uvDir.setElement(0, 0, -sinPhi);
            uvDir.setElement(0, 1, cosPhi);
            uvDir.setElement(0, 2, 0.);
            uvDir.setElement(1, 0, -sinLambda * cosPhi);
            uvDir.setElement(1, 1, -sinLambda * sinPhi);
            uvDir.setElement(1, 2, cosLambda);

            if (_debug) {
                System.out.printf(" uvDir \n%s\n%s\n", this.getClass().getSimpleName(), uvDir.toString());
            }

            // projection from  measurement to local (curvilinear uv) directions (duv/dm)
            Matrix proM2l = MatrixOp.mult(uvDir, mDirT); //uvDir * mDirT;

            //projection from local (curvilinear uv) to measurement directions (dm/duv)
            Matrix proL2m = MatrixOp.inverse(proM2l);

            //proL2m_list[strip->GetId()] = new TMatrixD(proL2m);
            if (_debug) {
                System.out.printf(" proM2l \n%s\n%s\n", this.getClass().getSimpleName(), proM2l.toString());
                System.out.printf(" proL2m \n%s\n%s\n", this.getClass().getSimpleName(), proL2m.toString());
            }

            // measurement/residual in the measurement system
            // start by find the distance vector between the center and the track position
            Hep3Vector vdiffTrk = VecOp.sub(trkpos, strip.origin());

            // then find the rotation from tracking to measurement frame
            Hep3Matrix trkToStripRot = _trackHitUtils.getTrackToStripRotation(strip.getStrip());

            // then rotate that vector into the measurement frame to get the predicted measurement position
            Hep3Vector trkpos_meas = VecOp.mult(trkToStripRot, vdiffTrk);

            // hit measurement and uncertainty in measurement frame
            Hep3Vector m_meas = new BasicHep3Vector(strip.umeas(), 0., 0.);

            // finally the residual
            Hep3Vector res_meas = VecOp.sub(m_meas, trkpos_meas);
            Hep3Vector res_err_meas = new BasicHep3Vector(strip.du(), (strip.vmax() - strip.vmin()) / Math.sqrt(12), 10.0 / Math.sqrt(12));

            // Move to matrix objects instead of 3D vectors
            // TODO use only one type
            // only 1D measurement in u-direction, set strip measurement direction to zero
            BasicMatrix meas = new BasicMatrix(1, 2);
            meas.setElement(0, 0, res_meas.x());
            meas.setElement(0, 1, 0.);
//			    //meas[0][0] += deltaU[iLayer] # misalignment

            BasicMatrix measErr = new BasicMatrix(1, 2);
            measErr.setElement(0, 0, res_err_meas.x());
            measErr.setElement(0, 1, 0.);

            BasicMatrix measPrec = new BasicMatrix(1, 2);
            measPrec.setElement(0, 0, 1.0 / (measErr.e(0, 0) * measErr.e(0, 0)));
            measPrec.setElement(0, 1, 0.);
            if (_debug) {
                System.out.printf("%s: meas \n%s\n", this.getClass().getSimpleName(), meas.toString());
                System.out.printf("%s: measErr \n%s\n", this.getClass().getSimpleName(), measErr.toString());
                System.out.printf("%s: measPrec \n%s\n", this.getClass().getSimpleName(), measPrec.toString());
            }

            //Find the Jacobian to be able to propagate the covariance matrix to this strip position
            jacPointToPoint = GblUtils.gblSimpleJacobianLambdaPhi(step, cosLambda, Math.abs(_bfac));

            if (_debug) {
                System.out.printf("%s: jacPointToPoint \n%s\n", this.getClass().getSimpleName(), jacPointToPoint.toString());
            }

            //propagate MS covariance matrix (in the curvilinear frame) to this strip position
            //msCov = np.dot(jacPointToPoint, np.dot(msCov, jacPointToPoint.T))
            //measMsCov = np.dot(proL2m, np.dot(msCov[3:, 3:], proL2m.T))
//                if (m_debug) {
//                  cout << "HpsGblFitter: " << " msCov at this point:" << endl;
//                  msCov.Print();
//                  //cout << "HpsGblFitter: " << "measMsCov at this point:" << endl;
//                  //measMsCov.Print();
//                }
            //Option to blow up measurement error according to multiple scattering
            //if useUncorrMS:
            //measPrec[0] = 1.0 / (measErr[0] ** 2 + measMsCov[0, 0])
            //  if debug:
            //print 'Adding measMsCov ', measMsCov[0,0]
            // point with independent measurement
            GblPoint point = new GblPoint(jacPointToPoint);

            //Add measurement to the point
            point.addMeasurement(proL2m, meas, measPrec);

            //Add scatterer in curvilinear frame to the point
            // no direction in this frame as it moves along the track
            BasicMatrix scat = GblUtils.zeroMatrix(0, 2);

            // find scattering angle
            ScatterPoint scatter = scatters.getScatterPoint(((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement());
            double scatAngle;

            if (scatter != null) {
                scatAngle = scatter.getScatterAngle().Angle();
            } else {
                if (_debug) {
                    System.out.printf("%s: WARNING cannot find scatter for detector %s with strip cluster at %s\n", this.getClass(), ((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement().getName(), strip.origin().toString());
                }
                scatAngle = GblUtils.estimateScatter(((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement(), htf, _scattering, _B);
            }

            // Scattering angle in the curvilinear frame
            //Note the cosLambda to correct for the projection in the phi direction
            BasicMatrix scatErr = new BasicMatrix(1, 2);
            scatErr.setElement(0, 0, scatAngle);
            scatErr.setElement(0, 1, scatAngle / cosLambda);
            BasicMatrix scatPrec = new BasicMatrix(1, 2);
            scatPrec.setElement(0, 0, 1.0 / (scatErr.e(0, 0) * scatErr.e(0, 0)));
            scatPrec.setElement(0, 1, 1.0 / (scatErr.e(0, 1) * scatErr.e(0, 1)));

            // add scatterer if not using the uncorrelated MS covariances for testing
            if (!useUncorrMS) {
                point.addScatterer(scat, scatPrec);
                if (_debug) {
                    System.out.printf("%s: scatError to this point \n%s\n", this.getClass().getSimpleName(), scatErr.toString());
                }
            }

            // Add this GBL point to list that will be used in fit
            listOfPoints.add(point);
            int iLabel = listOfPoints.size();

            // Update MS covariance matrix 
//            msCov.setElement(1, 1, msCov.e(1, 1) + scatErr.e(0, 0) * scatErr.e(0, 0));
//            msCov.setElement(2, 2, msCov.e(2, 2) + scatErr.e(0, 1) * scatErr.e(0, 1));

            /*

             ##### 
             ## Calculate global derivatives for this point
             # track direction in tracking/global frame
             tDirGlobal = np.array( [ [cosPhi * cosLambda, sinPhi * cosLambda, sinLambda] ] )        
             # Cross-check that the input is consistent
             if( np.linalg.norm( tDirGlobal - strip.tDir) > 0.00001):
             print 'ERROR: tDirs are not consistent!'
             sys.exit(1)
             # rotate track direction to measurement frame          
             tDirMeas = np.dot( tDirGlobal, np.array([strip.u, strip.v, strip.w]) )
             #tDirMeas = utils.rotateGlToMeas(strip,tDirGlobal)
             normalMeas = np.dot( strip.w , np.array([strip.u, strip.v, strip.w]) ) 
             #normalMeas = utils.rotateGlToMeas(strip,strip.w) 
             # non-measured directions 
             vmeas = 0.
             wmeas = 0.
             # calculate and add derivatives to point
             glDers = utils.globalDers(strip.layer,strip.meas,vmeas,wmeas,tDirMeas,normalMeas)
             ders = glDers.getDers(track.isTop())
             labGlobal = ders['labels']
             addDer = ders['ders']
             if debug:
             print 'global derivatives:'
             print labGlobal
             print addDer
             point.addGlobals(labGlobal, addDer)
             ##### 

             */
            //move on to next point
            s += step;

            // save strip and label map
            //stripLabelMap[strip] = iLabel;
        }

        //create the trajectory
        _traj = new GblTrajectory(listOfPoints); //,seedLabel, clSeed);

        if (!_traj.isValid()) {
            System.out.printf("%s:  Invalid GblTrajectory -> skip \n", this.getClass().getSimpleName());
            return -3;
        }

        // print the trajectory
        if (_debug) {
            System.out.println(" Gbl Trajectory ");
            _traj.printPoints(4);
        }
        // fit trajectory
        _traj.fit(m_chi2, m_ndf, m_lost_weight);

        //cng
//        System.out.println("fitting the traectory...");
//        double[] retDVals = new double[2];
//        int[] retIVals = new int[1];
//        int success = _traj.fit(retDVals, retIVals, "");
        //cng
        if (_debug) {
            System.out.printf("%s:  Chi2  Fit: %f , %d , %d\n", this.getClass().getSimpleName(), m_chi2, m_ndf, m_lost_weight);
        }

        // write to MP binary file
        if (_mille != null) {
            _traj.milleOut(_mille);
        }

        //stop the clock
        //clock_t endTime = clock();
        //double diff = endTime - startTime;
        //double cps = CLOCKS_PER_SEC;
        //if( m_debug ) {
        //	std::cout << "HpsGblFitter: " << " Time elapsed " << diff / cps << " s" << std::endl;
        //}
        if (_debug) {
            System.out.printf("%s:  Fit() done successfully.\n", this.getClass().getSimpleName());
        }

        return 0;
    }

    public static class HelicalTrackStripComparer implements Comparator<HelicalTrackStrip> {

        @Override
        public int compare(HelicalTrackStrip o1, HelicalTrackStrip o2) {
            // TODO Change this to path length!?
            return compare(o1.layer(), o2.layer());
        }

        private static int compare(int s1, int s2) {
            return s1 < s2 ? -1 : s2 > s1 ? 1 : 0;
        }

    }

}
