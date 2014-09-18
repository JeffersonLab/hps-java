package org.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.abs;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;

/**
 * A Driver which refits tracks using GBL Modeled on the hps-dst code written by
 * Per Hansson and Omar Moreno Requires the GBL Collections and Relations to be
 * present in the event.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsGblRefitter extends Driver
{

    private boolean _debug = false;
    private final String trackCollectionName = "MatchedTracks";
    private final String track2GblTrackRelationName = "TrackToGBLTrack";
    private final String gblTrack2StripRelationName = "GBLTrackToStripData";

    public void setDebug(boolean debug)
    {
        _debug = debug;
    }

    @Override
    protected void process(EventHeader event)
    {
        Hep3Vector bfield = event.getDetector().getFieldMap().getField(new BasicHep3Vector(0., 0., 1.));
        double By = bfield.y();
        double bfac = 0.0002998 * By;
        // get the tracks
//        List<Track> tracks = null;
        if (event.hasCollection(Track.class, trackCollectionName)) {
//            tracks = event.get(Track.class, trackCollectionName);
            if (_debug) {
//                System.out.printf("%s: Event %d has %d tracks\n", this.getClass().getSimpleName(), event.getEventNumber(), tracks.size());
            }
        } else {
            if (_debug) {
                System.out.printf("%s: No tracks in Event %d \n", this.getClass().getSimpleName(), event.getEventNumber());
            }
            return;
        }
//        System.out.println("Tracks from event " + event.getRunNumber());
//        for (Track t : tracks) {
//            System.out.println(t);
//            System.out.println(t.getTrackStates().get(0));
//        }
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

        // loop over the relations
        for (LCRelation relation : track2GblTrackRelations) {
            Track t = (Track) relation.getFrom();
            GenericObject gblTrackObject = (GenericObject) relation.getTo();
            GBLTrackData gblT = new GBLTrackData(gblTrackObject);
            gblObjMap.put(gblTrackObject, gblT);
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

        Set<GBLTrackData> keys = stripsGblMap.keySet();
        int trackNum = 0;
        for (GBLTrackData t : keys) {
            int stat = fit(t, stripsGblMap.get(t), bfac);
            ++trackNum;
        }

    }

    private int fit(GBLTrackData track, List<GBLStripClusterData> hits, double bfac)
    {
        // path length along trajectory
        double s = 0.;

        // jacobian to transport errors between points along the path
        Matrix jacPointToPoint = new Matrix(5, 5);
        jacPointToPoint.UnitMatrix();
        // Option to use uncorrelated  MS errors
        // This is similar to what is done in lcsim seedtracker
        // The msCov below holds the MS errors
        // This is for testing purposes only.
        boolean useUncorrMS = false;
        Matrix msCov = new Matrix(5, 5);
        Matrix measMsCov = new Matrix(2, 2);
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
            if (_debug) {
                System.out.println("HpsGblFitter: Processing strip " + istrip + " with id/layer " + strip.getId());
            }
            // Path length step for this cluster
            double step = strip.getPath3D() - s;
            if (_debug) {
                System.out.println("HpsGblFitter: " + "Path length step " + step + " from " + s + " to " + strip.getPath());
            }
            // Measurement direction (perpendicular and parallel to strip direction)
            Matrix mDir = new Matrix(2, 3);
            mDir.set(0, 0, strip.getUx());
            mDir.set(0, 1, strip.getUy());
            mDir.set(0, 2, strip.getUz());
            mDir.set(1, 0, strip.getVx());
            mDir.set(1, 1, strip.getVy());
            mDir.set(1, 2, strip.getVz());
            if (_debug) {
                System.out.println("HpsGblFitter: " + "mDir");
                mDir.print(4, 6);
            }
            Matrix mDirT = mDir.copy().transpose();

            if (_debug) {
                System.out.println("HpsGblFitter: " + "mDirT");
                mDirT.print(4, 6);
            }

            // Track direction 
            double sinLambda = sin(strip.getTrackLambda());//->GetLambda());
            double cosLambda = sqrt(1.0 - sinLambda * sinLambda);
            double sinPhi = sin(strip.getTrackPhi());//->GetPhi());
            double cosPhi = sqrt(1.0 - sinPhi * sinPhi);

            if (_debug) {
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

            if (_debug) {
                System.out.println("HpsGblFitter: " + "uvDir");
                uvDir.print(6, 4);
            }

            // projection from  measurement to local (curvilinear uv) directions (duv/dm)
            Matrix proM2l = uvDir.times(mDirT);

            //projection from local (uv) to measurement directions (dm/duv)
            Matrix proL2m = proM2l.copy();
            proL2m = proL2m.inverse();
            proL2m_list.put(strip.getId(), proL2m.copy()); // is a copy needed or is that just a C++/root thing?

            if (_debug) {
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

            if (_debug) {
                System.out.println("HpsGblFitter: " + "meas: ");
                meas.print(4, 6);
                System.out.println("HpsGblFitter: " + "measErr:");
                measErr.print(4, 6);
                System.out.println("HpsGblFitter: " + "measPrec:");
                measPrec.print(4, 6);
            }

            //Find the Jacobian to be able to propagate the covariance matrix to this strip position
            jacPointToPoint = gblSimpleJacobianLambdaPhi(step, cosLambda, abs(bfac));

            if (_debug) {
                System.out.println("HpsGblFitter: " + "jacPointToPoint to extrapolate to this point:");
                jacPointToPoint.print(4, 6);
            }

            // Get the transpose of the Jacobian
            Matrix jacPointToPointTransposed = jacPointToPoint.copy().transpose();

            // Propagate the MS covariance matrix (in the curvilinear frame) to this strip position
            msCov = msCov.times(jacPointToPointTransposed);
            msCov = jacPointToPoint.times(msCov);

            // Get the MS covariance for the measurements in the measurement frame
            Matrix proL2mTransposed = proL2m.copy().transpose();

            measMsCov = proL2m.times((msCov.getMatrix(3, 4, 3, 4)).times(proL2mTransposed));

            if (_debug) {
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
                if (_debug) {
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
            if (_debug) {
                System.out.println("HpsGblFitter: " + "uRes " + strip.getId() + " uRes " + uRes + " pred (" + strip.getTrackPos().x() + "," + strip.getTrackPos().y() + "," + strip.getTrackPos().z() + ") s(3D) " + strip.getPath3D());
            }

            //go to next point
            s += step;

        } //strips

        //create the trajectory
        GblTrajectory traj = new GblTrajectory(listOfPoints); //,seedLabel, clSeed);

        if (!traj.isValid()) {
            System.out.println("HpsGblFitter: " + " Invalid GblTrajectory -> skip");
            return 1;//INVALIDTRAJ;
        }

        // print the trajectory
        if (_debug) {
            System.out.println("%%%% Gbl Trajectory ");
            traj.printTrajectory(1);
            traj.printData();
            traj.printPoints(4);
        }
        // fit trajectory
        double[] dVals = new double[2];
        int[] iVals = new int[1];
        traj.fit(dVals, iVals, "");
        if (_debug) {
            System.out.println("HpsGblFitter: Chi2 " + " Fit: " + dVals[0] + ", " + iVals[0] + ", " + dVals[1]);
        }

        Vector aCorrection = new Vector(5);
        SymMatrix aCovariance = new SymMatrix(5);
        traj.getResults(1, aCorrection, aCovariance);
        if (_debug) {
            System.out.println(" cor ");
            aCorrection.print(6, 4);
            System.out.println(" cov ");
            aCovariance.print(6, 4);
        }

//	// write to MP binary file
//	traj.milleOut(mille);
//
        return 0;
    }

    @Override
    protected void detectorChanged(Detector detector)
    {
    }

    private Matrix gblSimpleJacobianLambdaPhi(double ds, double cosl, double bfac)
    {
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

}
