package org.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import static java.lang.Math.abs;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.hps.util.BasicLogFormatter;
import org.lcsim.constants.Constants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.converter.MilleParameter;
import org.lcsim.util.Driver;
import org.lcsim.util.log.LogUtil;

/**
 * A Driver which refits tracks using GBL. Modeled on the hps-dst code written
 * by Per Hansson and Omar Moreno. Requires the GBL Collections and Relations to
 * be present in the event.
 *
 * @author Norman A Graf
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 * @version $Id: HpsGblRefitter.java 3460 2015-08-29 01:45:39Z
 * meeg@slac.stanford.edu $
 */
public class HpsGblRefitter extends Driver {

    static Formatter f = new BasicLogFormatter();
    private static Logger logger = LogUtil.create(HpsGblRefitter.class.getSimpleName(), f, Level.WARNING);
    //private static final Logger logger = Logger.getLogger(HpsGblRefitter.class.getName());
    private boolean _debug = false;
    private final String trackCollectionName = "MatchedTracks";
    private final String track2GblTrackRelationName = "TrackToGBLTrack";
    private final String gblTrack2StripRelationName = "GBLTrackToStripData";

    private MilleBinary mille;
    private String milleBinaryFileName = MilleBinary.DEFAULT_OUTPUT_FILE_NAME;
    private boolean writeMilleBinary = false;

    private final MakeGblTracks _makeTracks;

    public void setDebug(boolean debug) {
        _debug = debug;
        _makeTracks.setDebug(debug);
    }

    public void setMilleBinaryFileName(String filename) {
        milleBinaryFileName = filename;
    }

    public void setWriteMilleBinary(boolean writeMillepedeFile) {
        writeMilleBinary = writeMillepedeFile;
    }

    public HpsGblRefitter() {
        _makeTracks = new MakeGblTracks();
        _makeTracks.setDebug(_debug);
        logger.setLevel(Level.WARNING);
        System.out.println("level " + logger.getLevel().toString());
    }

    //@Override
    //public void setLogLevel(String logLevel) {
    //    logger.setLevel(Level.parse(logLevel));
    //}
    @Override
    protected void startOfData() {
        if (writeMilleBinary) {
            mille = new MilleBinary(milleBinaryFileName);
        }
    }

    @Override
    protected void endOfData() {
        if (writeMilleBinary) {
            mille.close();
        }
    }

    @Override
    protected void process(EventHeader event) {
//        Hep3Vector bfieldvec = event.getDetector().getFieldMap().getField(new BasicHep3Vector(0., 0., 1.));
        Hep3Vector bfieldvec = event.getDetector().getFieldMap().getField(new BasicHep3Vector(0., 0., 500.));//mg...get the b-field in the middle of the magnet
        double bfield = bfieldvec.y();
        //double bfac = 0.0002998 * bfield;
        double bfac = Constants.fieldConversion * bfield;

        // get the tracks
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            if (_debug) {
                System.out.printf("%s: No tracks in Event %d \n", this.getClass().getSimpleName(), event.getEventNumber());
            }
            return;
        }

        //get the relations to the GBLtracks
        if (!event.hasItem(track2GblTrackRelationName)) {
            System.out.println("Need Relations " + track2GblTrackRelationName);
            return;
        }
        // and strips
        if (!event.hasItem(gblTrack2StripRelationName)) {
            System.out.println("Need Relations " + gblTrack2StripRelationName);
            return;
        }

        List<LCRelation> track2GblTrackRelations = event.get(LCRelation.class, track2GblTrackRelationName);
        //need a map of GBLTrackData keyed on the Generic object from which it created
        Map<GenericObject, GBLTrackData> gblObjMap = new HashMap<GenericObject, GBLTrackData>();
        //need a map of SeedTrack to GBLTrackData keyed on the track object from which it created
        Map<GBLTrackData, Track> gblToSeedMap = new HashMap<GBLTrackData, Track>();

        // loop over the relations
        for (LCRelation relation : track2GblTrackRelations) {
            Track t = (Track) relation.getFrom();
            GenericObject gblTrackObject = (GenericObject) relation.getTo();
            GBLTrackData gblT = new GBLTrackData(gblTrackObject);
            gblObjMap.put(gblTrackObject, gblT);
            gblToSeedMap.put(gblT, t);
        } //end of loop over tracks

        //get the strip hit relations
        List<LCRelation> gblTrack2StripRelations = event.get(LCRelation.class, gblTrack2StripRelationName);

        // need a map of lists of strip data keyed by the gblTrack to which they correspond
        Map<GBLTrackData, List<GBLStripClusterData>> stripsGblMap = new HashMap<GBLTrackData, List<GBLStripClusterData>>();
        for (LCRelation relation : gblTrack2StripRelations) {
            //from GBLTrackData to GBLStripClusterData
            GenericObject gblTrackObject = (GenericObject) relation.getFrom();
            //Let's get the GBLTrackData that corresponds to this object...
            GBLTrackData gblT = gblObjMap.get(gblTrackObject);
            GBLStripClusterData sd = new GBLStripClusterData((GenericObject) relation.getTo());
            if (stripsGblMap.containsKey(gblT)) {
                stripsGblMap.get(gblT).add(sd);
            } else {
                stripsGblMap.put(gblT, new ArrayList<GBLStripClusterData>());
                stripsGblMap.get(gblT).add(sd);
            }
        }

        // loop over the tracks and do the GBL fit
        List<FittedGblTrajectory> trackFits = new ArrayList<FittedGblTrajectory>();
        int trackNum = 0;
        logger.info("Trying to fit " + stripsGblMap.size() + " tracks");
        for (GBLTrackData t : stripsGblMap.keySet()) {
            FittedGblTrajectory traj = fit(stripsGblMap.get(t), bfac, _debug);
            ++trackNum;
            if (traj != null) {
                logger.info("GBL fit successful");
                if (_debug) {
                    System.out.printf("%s: GBL fit successful.\n", getClass().getSimpleName());
                }
                // write to MP binary file
                if (writeMilleBinary) {
                    traj.get_traj().milleOut(mille);
                }
                traj.set_seed(gblToSeedMap.get(t));
                traj.set_track_data(t);
                trackFits.add(traj);
            } else {
                logger.info("GBL fit failed");
                if (_debug) {
                    System.out.printf("%s: GBL fit failed.\n", getClass().getSimpleName());
                }
            }
        }

        logger.info(event.get(Track.class, trackCollectionName).size() + " tracks in collection \"" + trackCollectionName + "\"");
        logger.info(gblObjMap.size() + " tracks in gblObjMap");
        logger.info(gblToSeedMap.size() + " tracks in gblToSeedMap");
        logger.info(stripsGblMap.size() + " tracks in stripsGblMap");
        logger.info(trackFits.size() + " fitted GBL tracks before adding to event");

        _makeTracks.Process(event, trackFits, bfield);

        if (_debug) {
            System.out.printf("%s: Done.\n", getClass().getSimpleName());
        }

    }

    private static FittedGblTrajectory fit(List<GBLStripClusterData> hits, double bfac, boolean debug) {
        // path length along trajectory
        double s = 0.;

        // jacobian to transport errors between points along the path
        Matrix jacPointToPoint = new Matrix(5, 5);
        jacPointToPoint.UnitMatrix();
        // Vector of the strip clusters used for the GBL fit
        List<GblPoint> listOfPoints = new ArrayList<GblPoint>();

        // Store the projection from local to measurement frame for each strip cluster
        Map< Integer, Matrix> proL2m_list = new HashMap<Integer, Matrix>();
        // Save the association between strip cluster and label

        //start trajectory at refence point (s=0) - this point has no measurement
        GblPoint ref_point = new GblPoint(jacPointToPoint);
        listOfPoints.add(ref_point);

        // Loop over strips
        int n_strips = hits.size();
        for (int istrip = 0; istrip != n_strips; ++istrip) {
            GBLStripClusterData strip = hits.get(istrip);
            //MG--9/18/2015--beamspot has Id=666/667...don't include it in the GBL fit
            if (strip.getId() > 99) {
                continue;
            }
            if (debug) {
                System.out.println("HpsGblFitter: Processing strip " + istrip + " with id/layer " + strip.getId());
            }
            // Path length step for this cluster
            double step = strip.getPath3D() - s;
            if (debug) {
                System.out.println("HpsGblFitter: " + "Path length step " + step + " from " + s + " to " + strip.getPath3D());
            }

            // get measurement frame unit vectors
            Hep3Vector u = strip.getU();
            Hep3Vector v = strip.getV();
            Hep3Vector w = strip.getW();

            // Measurement direction (perpendicular and parallel to strip direction)
            Matrix mDir = new Matrix(2, 3);
            mDir.set(0, 0, u.x());
            mDir.set(0, 1, u.y());
            mDir.set(0, 2, u.z());
            mDir.set(1, 0, v.x());
            mDir.set(1, 1, v.y());
            mDir.set(1, 2, v.z());
            if (debug) {
                System.out.println("HpsGblFitter: " + "mDir");
                mDir.print(4, 6);
            }
            Matrix mDirT = mDir.copy().transpose();

            if (debug) {
                System.out.println("HpsGblFitter: " + "mDirT");
                mDirT.print(4, 6);
            }

            // Track direction 
            double sinLambda = sin(strip.getTrackLambda());//->GetLambda());
            double cosLambda = sqrt(1.0 - sinLambda * sinLambda);
            double sinPhi = sin(strip.getTrackPhi());//->GetPhi());
            double cosPhi = sqrt(1.0 - sinPhi * sinPhi);

            if (debug) {
                System.out.println("HpsGblFitter: " + "Track direction sinLambda=" + sinLambda + " sinPhi=" + sinPhi);
            }

            // Track direction in curvilinear frame (U,V,T)
            // U = Z x T / |Z x T|, V = T x U
            Matrix uvDir = new Matrix(2, 3);
            uvDir.set(0, 0, -sinPhi);
            uvDir.set(0, 1, cosPhi);
            uvDir.set(0, 2, 0.);
            uvDir.set(1, 0, -sinLambda * cosPhi);
            uvDir.set(1, 1, -sinLambda * sinPhi);
            uvDir.set(1, 2, cosLambda);

            if (debug) {
                System.out.println("HpsGblFitter: " + "uvDir");
                uvDir.print(6, 4);
            }

            // projection from  measurement to local (curvilinear uv) directions (duv/dm)
            Matrix proM2l = uvDir.times(mDirT);

            //projection from local (uv) to measurement directions (dm/duv)
            Matrix proL2m = proM2l.copy();
            proL2m = proL2m.inverse();
            proL2m_list.put(strip.getId(), proL2m.copy()); // is a copy needed or is that just a C++/root thing?

            if (debug) {
                System.out.println("HpsGblFitter: " + "proM2l:");
                proM2l.print(4, 6);
                System.out.println("HpsGblFitter: " + "proL2m:");
                proL2m.print(4, 6);
                System.out.println("HpsGblFitter: " + "proM2l*proL2m (should be unit matrix):");
                (proM2l.times(proL2m)).print(4, 6);
            }

            // measurement/residual in the measurement system
            // only 1D measurement in u-direction, set strip measurement direction to zero
            Vector meas = new Vector(2);
//		double uRes = strip->GetUmeas() - strip->GetTrackPos().x(); // how can this be correct?
            double uRes = strip.getMeas() - strip.getTrackPos().x();
            meas.set(0, uRes);
            meas.set(1, 0.);
//		//meas[0][0] += deltaU[iLayer] # misalignment
            Vector measErr = new Vector(2);
            measErr.set(0, strip.getMeasErr());
            measErr.set(1, 0.);
            Vector measPrec = new Vector(2);
            measPrec.set(0, 1.0 / (measErr.get(0) * measErr.get(0)));
            measPrec.set(1, 0.);

            if (debug) {
                System.out.println("HpsGblFitter: " + "meas: ");
                meas.print(4, 6);
                System.out.println("HpsGblFitter: " + "measErr:");
                measErr.print(4, 6);
                System.out.println("HpsGblFitter: " + "measPrec:");
                measPrec.print(4, 6);
            }

            //Find the Jacobian to be able to propagate the covariance matrix to this strip position
            jacPointToPoint = gblSimpleJacobianLambdaPhi(step, cosLambda, abs(bfac));

            if (debug) {
                System.out.println("HpsGblFitter: " + "jacPointToPoint to extrapolate to this point:");
                jacPointToPoint.print(4, 6);
            }

            // Get the transpose of the Jacobian
            Matrix jacPointToPointTransposed = jacPointToPoint.copy().transpose();

            // Option to use uncorrelated  MS errors
            // This is similar to what is done in lcsim seedtracker
            // The msCov below holds the MS errors
            // This is for testing purposes only.
            boolean useUncorrMS = false;
            Matrix msCov = new Matrix(5, 5);

            // Propagate the MS covariance matrix (in the curvilinear frame) to this strip position
            msCov = msCov.times(jacPointToPointTransposed);
            msCov = jacPointToPoint.times(msCov);

            // Get the MS covariance for the measurements in the measurement frame
            Matrix proL2mTransposed = proL2m.copy().transpose();

            Matrix measMsCov = proL2m.times((msCov.getMatrix(3, 4, 3, 4)).times(proL2mTransposed));

            if (debug) {
                System.out.println("HpsGblFitter: " + " msCov at this point:");
                msCov.print(4, 6);
                System.out.println("HpsGblFitter: " + "measMsCov at this point:");
                measMsCov.print(4, 6);
            }

            GblPoint point = new GblPoint(jacPointToPoint);
            //Add measurement to the point
            point.addMeasurement(proL2m, meas, measPrec, 0.);
            //Add scatterer in curvilinear frame to the point
            // no direction in this frame
            Vector scat = new Vector(2);

            // Scattering angle in the curvilinear frame
            //Note the cosLambda to correct for the projection in the phi direction
            Vector scatErr = new Vector(2);
            scatErr.set(0, strip.getScatterAngle());
            scatErr.set(1, strip.getScatterAngle() / cosLambda);
            Vector scatPrec = new Vector(2);
            scatPrec.set(0, 1.0 / (scatErr.get(0) * scatErr.get(0)));
            scatPrec.set(1, 1.0 / (scatErr.get(1) * scatErr.get(1)));

            // add scatterer if not using the uncorrelated MS covariances for testing
            if (!useUncorrMS) {
                point.addScatterer(scat, scatPrec);
                if (debug) {
                    System.out.println("HpsGblFitter: " + "adding scatError to this point:");
                    scatErr.print(4, 6);
                }
            }

            // Add this GBL point to list that will be used in fit
            listOfPoints.add(point);
            int iLabel = listOfPoints.size();

            // Update MS covariance matrix 
            msCov.set(1, 1, msCov.get(1, 1) + scatErr.get(0) * scatErr.get(0));
            msCov.set(2, 2, msCov.get(2, 2) + scatErr.get(1) * scatErr.get(1));

            //// Calculate global derivatives for this point
            // track direction in tracking/global frame
            Hep3Vector tDirGlobal = new BasicHep3Vector(cosPhi * cosLambda, sinPhi * cosLambda, sinLambda);

            // Cross-check that the input is consistent
            if (VecOp.sub(tDirGlobal, strip.getTrackDirection()).magnitude() > 0.00001) {
                throw new RuntimeException("track directions are inconsistent: " + tDirGlobal.toString() + " and " + strip.getTrackDirection().toString());
            }
            // rotate track direction to measurement frame     
            Hep3Vector tDirMeas = new BasicHep3Vector(VecOp.dot(tDirGlobal, u), VecOp.dot(tDirGlobal, v), VecOp.dot(tDirGlobal, w));
            // TODO this is a trivial one. Fix it.
            Hep3Vector normalMeas = new BasicHep3Vector(VecOp.dot(w, u), VecOp.dot(w, v), VecOp.dot(w, w));

            // vector coplanar with measurement plane from origin to prediction
            Hep3Vector tPosMeas = strip.getTrackPos();

            // measurements: non-measured directions 
            double vmeas = 0.;
            double wmeas = 0.;

            // calculate and add derivatives to point
            GlobalDers glDers = new GlobalDers(strip.getId(), meas.get(0), vmeas, wmeas, tDirMeas, tPosMeas, normalMeas);

            //TODO find a more robust way to get half.
            boolean isTop = Math.sin(strip.getTrackLambda()) > 0;

            // Get the list of millepede parameters
            List<MilleParameter> milleParameters = glDers.getDers(isTop);

            // need to make vector and matrices for interface
            List<Integer> labGlobal = new ArrayList<Integer>();
            Matrix addDer = new Matrix(1, milleParameters.size());
            for (int i = 0; i < milleParameters.size(); ++i) {
                labGlobal.add(milleParameters.get(i).getId());
                addDer.set(0, i, milleParameters.get(i).getValue());
            }
            point.addGlobals(labGlobal, addDer);
            String logders = "";
            for (int i = 0; i < milleParameters.size(); ++i) {
                logders += labGlobal.get(i) + "\t" + addDer.get(0, i) + "\n";
            }
            logger.info("\n" + logders);

            logger.info("uRes " + strip.getId() + " uRes " + uRes + " pred (" + strip.getTrackPos().x() + "," + strip.getTrackPos().y() + "," + strip.getTrackPos().z() + ") s(3D) " + strip.getPath3D());

            //go to next point
            s += step;

        } //strips

        //create the trajectory
        GblTrajectory traj = new GblTrajectory(listOfPoints); //,seedLabel, clSeed);

        if (!traj.isValid()) {
            System.out.println("HpsGblFitter: " + " Invalid GblTrajectory -> skip");
            return null; //1;//INVALIDTRAJ;
        }

        // print the trajectory
        if (debug) {
            System.out.println("%%%% Gbl Trajectory ");
            traj.printTrajectory(1);
            traj.printData();
            traj.printPoints(4);
        }
        // fit trajectory
        double[] dVals = new double[2];
        int[] iVals = new int[1];
        traj.fit(dVals, iVals, "");
        logger.info("fit result: Chi2=" + dVals[0] + " Ndf=" + iVals[0] + " Lost=" + dVals[1]);
        Vector aCorrection = new Vector(5);
        SymMatrix aCovariance = new SymMatrix(5);
        traj.getResults(1, aCorrection, aCovariance);
        if (debug) {
            System.out.println(" cor ");
            aCorrection.print(6, 4);
            System.out.println(" cov ");
            aCovariance.print(6, 4);
        }

        logger.fine("locPar " + aCorrection.toString());

//
        return new FittedGblTrajectory(traj, dVals[0], iVals[0], dVals[1]);
    }

    @Override
    protected void detectorChanged(Detector detector) {
    }

    private static Matrix gblSimpleJacobianLambdaPhi(double ds, double cosl, double bfac) {
        /**
         * Simple jacobian: quadratic in arc length difference. using lambda phi
         * as directions
         *
         * @param ds: arc length difference
         * @type ds: float
         * @param cosl: cos(lambda)
         * @type cosl: float
         * @param bfac: Bz*c
         * @type bfac: float
         * @return: jacobian to move by 'ds' on trajectory
         * @rtype: matrix(float) ajac(1,1)= 1.0D0 ajac(2,2)= 1.0D0
         * ajac(3,1)=-DBLE(bfac*ds) ajac(3,3)= 1.0D0
         * ajac(4,1)=-DBLE(0.5*bfac*ds*ds*cosl) ajac(4,3)= DBLE(ds*cosl)
         * ajac(4,4)= 1.0D0 ajac(5,2)= DBLE(ds) ajac(5,5)= 1.0D0 ''' jac =
         * np.eye(5) jac[2, 0] = -bfac * ds jac[3, 0] = -0.5 * bfac * ds * ds *
         * cosl jac[3, 2] = ds * cosl jac[4, 1] = ds return jac
         */
        Matrix jac = new Matrix(5, 5);
        jac.UnitMatrix();
        jac.set(2, 0, -bfac * ds);
        jac.set(3, 0, -0.5 * bfac * ds * ds * cosl);
        jac.set(3, 2, ds * cosl);
        jac.set(4, 1, ds);
        return jac;
    }

    private static class GlobalDers {

        private final int _layer;
        private final double _umeas; // measurement direction
        private final double _vmeas; // unmeasured direction
        private final double _wmeas; // normal to plane
        private final Hep3Vector _t; // track direction
        private final Hep3Vector _p; // track prediction
        private final Hep3Vector _n; // normal to plane
        private final Matrix _dm_dg; //Global derivaties of the local measurements
        private final Matrix _dr_dm; //Derivatives of residuals w.r.t. measurement
        private final Matrix _dr_dg; //Derivatives of residuals w.r.t. global parameters

        public GlobalDers(int layer, double umeas, double vmeas, double wmeas, Hep3Vector tDir, Hep3Vector tPred, Hep3Vector normal) {
            _layer = layer;
            _umeas = umeas;
            _vmeas = vmeas;
            _wmeas = wmeas;
            _t = tDir;
            _p = tPred;
            _n = normal;
            // Derivatives of residuals w.r.t. perturbed measurement 
            _dr_dm = getResDers();
            // Derivatives of perturbed measurement w.r.t. global parameters
            _dm_dg = getMeasDers();
            // Calculate, by chain rule, derivatives of residuals w.r.t. global parameters
            _dr_dg = _dr_dm.times(_dm_dg);

            //logger.log(Level.FINER," dr_dm\n"+ _dr_dm.toString() + "\ndm_dg\n" + _dm_dg.toString() + "\ndr_dg\n" +_dr_dg.toString());
            //logger.info("loglevel " + logger.getLevel().toString());
            //print 'dm_dg'
            //print dm_dg
            //print 'dr_dm'
            //print dr_dm
            //print 'dr_dg'
            //print self.dr_dg
        }

        /**
         * Derivative of mt, the perturbed measured coordinate vector m w.r.t.
         * to global parameters: u,v,w,alpha,beta,gamma
         */
        private Matrix getMeasDers() {

            //Derivative of the local measurement for a translation in u
            double dmu_du = 1.;
            double dmv_du = 0.;
            double dmw_du = 0.;
            // Derivative of the local measurement for a translation in v
            double dmu_dv = 0.;
            double dmv_dv = 1.;
            double dmw_dv = 0.;
            // Derivative of the local measurement for a translation in w
            double dmu_dw = 0.;
            double dmv_dw = 0.;
            double dmw_dw = 1.;
            // Derivative of the local measurement for a rotation around u-axis (alpha)
            double dmu_dalpha = 0.;
            double dmv_dalpha = _p.z(); // self.wmeas
            double dmw_dalpha = -1.0 * _p.y(); // -1.0 * self.vmeas
            // Derivative of the local measurement for a rotation around v-axis (beta)
            double dmu_dbeta = -1.0 * _p.z(); //-1.0 * self.wmeas
            double dmv_dbeta = 0.;
            double dmw_dbeta = _p.x(); //self.umeas
            // Derivative of the local measurement for a rotation around w-axis (gamma)
            double dmu_dgamma = _p.y(); // self.vmeas
            double dmv_dgamma = -1.0 * _p.x();  // -1.0 * self.umeas 
            double dmw_dgamma = 0.;
            // put into matrix
            Matrix dm_dg = new Matrix(3, 6);
            dm_dg.set(0, 0, dmu_du);
            dm_dg.set(0, 1, dmu_dv);
            dm_dg.set(0, 2, dmu_dw);
            dm_dg.set(0, 3, dmu_dalpha);
            dm_dg.set(0, 4, dmu_dbeta);
            dm_dg.set(0, 5, dmu_dgamma);
            dm_dg.set(1, 0, dmv_du);
            dm_dg.set(1, 1, dmv_dv);
            dm_dg.set(1, 2, dmv_dw);
            dm_dg.set(1, 3, dmv_dalpha);
            dm_dg.set(1, 4, dmv_dbeta);
            dm_dg.set(1, 5, dmv_dgamma);
            dm_dg.set(2, 0, dmw_du);
            dm_dg.set(2, 1, dmw_dv);
            dm_dg.set(2, 2, dmw_dw);
            dm_dg.set(2, 3, dmw_dalpha);
            dm_dg.set(2, 4, dmw_dbeta);
            dm_dg.set(2, 5, dmw_dgamma);
            //dmdg = np.array([[dmu_du, dmu_dv, dmu_dw, dmu_dalpha, dmu_dbeta, dmu_dgamma],[dmv_du, dmv_dv, dmv_dw, dmv_dalpha, dmv_dbeta, dmv_dgamma],[dmw_du, dmw_dv, dmw_dw, dmw_dalpha, dmw_dbeta, dmw_dgamma]])
            return dm_dg;
        }

        /**
         * Derivatives of the local perturbed residual w.r.t. the measurements m
         * (u,v,w)'
         */
        private Matrix getResDers() {
            double tdotn = VecOp.dot(_t, _n);
            Matrix dr_dm = Matrix.identity(3, 3);
            //print 't ', self.t, ' n ', self.n, ' dot(t,n) ', tdotn
            //logger.info("t " + _t.toString() +" n " + _n.toString() + " dot(t,n) " + tdotn);
            double delta, val;
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    delta = i == j ? 1. : 0.;
                    val = delta - _t.v()[i] * _n.v()[j] / tdotn;
                    dr_dm.set(i, j, val);
                }
            }
            return dr_dm;
        }

        /**
         * Turn derivative matrix into @Milleparameter
         *
         * @param isTop - top or bottom track
         * @return list of @Milleparameters
         */
        private List<MilleParameter> getDers(boolean isTop) {
            int transRot;
            int direction;
            int label;
            double value;
            List<MilleParameter> milleParameters = new ArrayList<MilleParameter>();
            int topBot = isTop == true ? 1 : 2;
            for (int ip = 1; ip < 7; ++ip) {
                if (ip > 3) {
                    transRot = 2;
                    direction = ((ip - 1) % 3) + 1;
                } else {
                    transRot = 1;
                    direction = ip;
                }
                label = topBot * MilleParameter.half_offset + transRot * MilleParameter.type_offset + direction * MilleParameter.dimension_offset + _layer;
                value = _dr_dg.get(0, ip - 1);
                MilleParameter milleParameter = new MilleParameter(label, value, 0.0);
                milleParameters.add(milleParameter);
            }
            return milleParameters;
        }


        /*

         class globalDers:
         def __init__(self,layer,umeas,vmeas,wmeas, tDir, tPred, normal):
         self.layer = layer # measurement direction
         self.umeas = umeas # measurement direction
         self.vmeas = vmeas # unmeasured direction
         self.wmeas = wmeas # normal to plane
         self.t = tDir # track direction
         self.p = tPred # track prediction
         self.n = normal # normal to plane
         # Global derivaties of the local measurements
         self.dm_dg = self.getMeasDers()
         # Derivatives of residuals w.r.t. measurement 
         self.dr_dm = self.getResDers()
         # Derivatives of residuals w.r.t. global parameters
         self.dr_dg = np.dot(self.dr_dm, self.dm_dg)
         #print 'dm_dg'
         #print dm_dg
         #print 'dr_dm'
         #print dr_dm
         #print 'dr_dg'
         #print self.dr_dg

         def dump(self):
         print 'globalDers:'
         print 'layer ', self.layer
         print 'umeas ', self.umeas, ' vmeas ', self.vmeas, ' wmeas ', self.wmeas
         print 't ', self.t, ' p ', self.p, ' n ', self.n
         print 'dm_dg\n',self.dm_dg, '\ndr_dm\n',self.dr_dm,'\ndr_dg\n',self.dr_dg

         def getDers(self,isTop):
         half_offset = 10000 
         translation_offset = 1000
         direction_offset = 100
         topBot = 1
         transRot = 1
         direction = 1
         if not isTop:
         topBot = 2
         res = {}
         labels = []
         ders = []
         for ip, name in global_params.iteritems():
         if ip > 3:
         transRot = 2
         direction = ((ip-1) % 3) + 1
         else:
         direction = ip
         label = (int)(topBot * half_offset + transRot * translation_offset + direction * direction_offset + self.layer)
         labels.append(label)
         ders.append(self.dr_dg[0,ip-1])

         return {'labels':np.array([labels]) , 'ders':np.array([ders])}




         def getResDers(self):
         # Derivatives of the local perturbed residual w.r.t. the measurements m (u,v,w)'
         tdotn = np.dot(self.t.T,self.n)
         drdg = np.eye(3)
         #print 't ', self.t, ' n ', self.n, ' dot(t,n) ', tdotn
         for i in range(3):
         for j in range(3):
         delta = 0.
         if i==j:
         delta = 1.       
         drdg[i][j] = delta - self.t[i]*self.n[j]/tdotn[0]
         return drdg




         def getMeasDers(self):
         # Derivative of mt, the perturbed measured coordinate vector m 
         # w.r.t. to global parameters: u,v,w,alpha,beta,gamma

         # Derivative of the local measurement for a translation in u
         dmu_du = 1.
         dmv_du = 0.
         dmw_du = 0.
         # Derivative of the local measurement for a translation in v
         dmu_dv = 0.
         dmv_dv = 1.
         dmw_dv = 0.
         # Derivative of the local measurement for a translation in w
         dmu_dw = 0.
         dmv_dw = 0.
         dmw_dw = 1.
         # Derivative of the local measurement for a rotation around u-axis (alpha)
         dmu_dalpha = 0.
         dmv_dalpha = self.p[2] # self.wmeas
         dmw_dalpha = -1.0 * self.p[1] # -1.0 * self.vmeas
         # Derivative of the local measurement for a rotation around v-axis (beta)
         dmu_dbeta = -1.0 * self.p[2] #-1.0 * self.wmeas
         dmv_dbeta = 0.
         dmw_dbeta = self.p[0] #self.umeas
         # Derivative of the local measurement for a rotation around w-axis (gamma)
         dmu_dgamma = self.p[1] # self.vmeas
         dmv_dgamma = -1.0 * self.p[0]  # -1.0 * self.umeas 
         dmw_dgamma = 0.
         # put into matrix
         dmdg = np.array([[dmu_du, dmu_dv, dmu_dw, dmu_dalpha, dmu_dbeta, dmu_dgamma],[dmv_du, dmv_dv, dmv_dw, dmv_dalpha, dmv_dbeta, dmv_dgamma],[dmw_du, dmw_dv, dmw_dw, dmw_dalpha, dmw_dbeta, dmw_dgamma]])
         #print dmw_dbeta
         #dmdg = np.array([[dmu_du, dmu_dv],[dmu_dw, dmu_dalpha], [dmw_dbeta, dmw_dgamma]])
         return dmdg
         */
    }
}
