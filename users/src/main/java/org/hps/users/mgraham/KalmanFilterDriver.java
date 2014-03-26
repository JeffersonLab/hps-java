/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.mgraham;

import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.hps.recon.tracking.TrackAnalysis;
import org.hps.recon.tracking.kalman.FullFitKalman;
import org.hps.recon.tracking.kalman.ShapeDispatcher;
import org.hps.recon.tracking.kalman.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trffit.HTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ecfine (ecfine@slac.stanford.edu)
 */
/* Takes Matched Tracks from the Track Reconstruction Driver, and makes TRF
 * HTracks. Adds hits from the original tracks to the new HTrack, and runs a
 * Kalman filter over the HTrack.
 *
 * This only does a forward fit, and multiple scattering is only included at
 * interacting planes, and just by assuming that each plane is .01 radiation
 * lengths. Energy loss is not accounted for. Additionally, the method for
 * constructing hits assumes that every hit occurs at an XY plane, while ideally,
 * there would be a method which checks the lcsim detector geometry and then
 * decides what kind of surface to model the shape as. There may be some methods
 * in the ShapeHelper interface (particularly in TrdHelper) that would be useful
 * for this, but nothing completed. Additionally, to run realistic multiple
 * scattering with non-interacting detector elements, there would need to be a
 * way to find the intercepts between a specific track and the detector elements.
 *
 * Also, magnetic field is just set at 1.0 in each class. It should be taken from
 * the detector geometry. */
public class KalmanFilterDriver extends Driver {

    boolean _debug = false;
    private AIDA aida = AIDA.defaultInstance();
    ShapeDispatcher shapeDis = new ShapeDispatcher();
    TrackUtils trackUtils = new TrackUtils();
    Propagator prop = null;
    FullFitKalman fitk = null;
    KalmanGeom geom = null;
    Detector detector = null;
    HTrack ht = null;
    double bz = 0.5;

    public void detectorChanged(Detector det) {
        detector = det;
        geom = new KalmanGeom(detector); // new geometry containing detector info
        prop = geom.newPropagator();
        trackUtils.setBZ(geom.bz);
        System.out.println("geom field = " + geom.bz + ", trackUtils field = " + trackUtils.bz);

        fitk = new FullFitKalman(prop);
//        PropXYXY_Test foobar = new PropXYXY_Test();
//        foobar.testPropXYXY();
        //
    }

    public void process(EventHeader event) {
        /* Get the tracklist for each event, and then for each track
         * get the starting track parameters and covariance matrix for an
         * outward fit from the seedtracker. */
        String kaldir = "KalmanFit/";
        String kaldirPos = "KalmanFit/GoodChi2/";
        String kaldirBad = "KalmanFit/BadChi2/";
        String bkwdkaldir = "KalmanFit/BackwardFit/";
        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> mcrelations = event.get(LCRelation.class, "HelicalTrackMCRelations");

        for (LCRelation relation : mcrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittomc.add(relation.getFrom(), relation.getTo());

        if (event.hasItem("MatchedTracks")) {
            List<Track> trklist = (List<Track>) event.get("MatchedTracks");
            if (_debug)
                System.out.println("number of tracks: " + trklist.size());
            for (int i = 0; i < trklist.size(); i++) {

                /* Start with a HelicalTrackFit, turn it into a VTrack,
                 * turn that into an ETrack, and turn that into an HTrack.
                 * Then add detector hits from the original track. */
                if (_debug)
                    System.out.println("Making tracks...");
                Track track = trklist.get(i);
                TrackAnalysis tkanal = new TrackAnalysis(track, hittomc);
                HelicalTrackFit helicalTrack = shapeDis.trackToHelix(track);
                VTrack vt = trackUtils.makeVTrack(helicalTrack);
                TrackError initialError = trackUtils.getInitalError(helicalTrack);
                ETrack et = new ETrack(vt, initialError);
                ht = new HTrack(et);

                /* Add hits from original track */
                for (int k = 0; k < track.getTrackerHits().size(); k++) {
                    TrackerHit thit = track.getTrackerHits().get(k);
//                    System.out.println("Adding hit...");
                    ht = geom.addTrackerHit(thit, ht, helicalTrack, vt);
                }
                /* Once we have an HTrack with the ordered list of hits, we pass
                 * this to the Kalman fitter. */
//                System.out.println("Running Kalman fit...");



                /*

                int fstarf = fitk.fit(ht);
                double kfchi2 = ht.chisquared();


                SurfXYPlane srfc = new SurfXYPlane(0.001, 0);
                 */

                //                System.out.println("Nominal Fit Vector : "+vt.toString());
                TrackVector nomFit = vt.vector();
                double nomDCA = nomFit.get(0);
                double nomz0 = nomFit.get(1);
                double nomphi0 = Math.sin(nomFit.get(2));
                double nomslope = nomFit.get(3);
                double nomqOverpt = nomFit.get(4);


                /*
                ETrack fitET = ht.newTrack();
                if (kfchi2 < 10000 && kfchi2 > 0) {
                //                    ht.propagate(prop, srfc);
                if(_debug)  System.out.println("Fitted Track Before Propagation:  "+ht.toString());
                //                    ht.propagate(prop, srfc, PropDir.BACKWARD_MOVE);
                ht.propagate(prop, s, PropDir.BACKWARD);

                if(_debug)    System.out.println("Fitted Track After Propagation:  "+ht.toString());

                TrackError fitErr = fitET.error();
                double kfDCA = fitET.vector().get(0);
                double kfz0 = fitET.vector().get(1);
                double kfphi0 = Math.sin(fitET.vector().get(2));
                double kfslope = fitET.vector().get(3);
                double kfqOverpt = fitET.vector().get(4);
                aida.histogram1D(kaldir + "Kalman Fit Chi2", 50, 0, 100).fill(kfchi2);
                aida.histogram1D(kaldir + "Kalman Fit DCA", 50, -0.5, 0.5).fill(kfDCA);
                aida.histogram1D(kaldir + "Kalman Fit z0", 50, -0.25, 0.25).fill(kfz0);
                aida.histogram1D(kaldir + "Kalman Fit sin(phi0)", 50, -0.2, 0.2).fill(kfphi0);
                aida.histogram1D(kaldir + "Kalman Fit tanlambda", 50, -0.1, 0.1).fill(kfslope);
                aida.histogram1D(kaldir + "Kalman Fit qOverpt", 50, -5, 5).fill(kfqOverpt);
                aida.histogram1D(kaldir + "Kalman-Nominal DCA", 50, -.5, 0.5).fill(kfDCA - nomDCA);
                aida.histogram1D(kaldir + "Kalman-Nominal z0", 50, -0.5, 0.5).fill(kfz0 - nomz0);
                aida.histogram1D(kaldir + "Kalman-Nominal sin(phi0)", 50, -0.05, 0.05).fill(kfphi0 - nomphi0);
                aida.histogram1D(kaldir + "Kalman-Nominal slope", 50, -0.05, 0.05).fill(kfslope - nomslope);
                aida.histogram1D(kaldir + "Kalman-Nominal qOverpt", 50, -0.1, 0.1).fill(kfqOverpt - nomqOverpt);


                }
                 */

                double d0 = helicalTrack.dca();
                double z0 = helicalTrack.z0();
                double slope = helicalTrack.slope();
                double phi0 = helicalTrack.phi0();
                double R = helicalTrack.R();
                double sTarg = HelixUtils.PathToXPlane(helicalTrack, 0, 1e4, 1).get(0);
                double phi = sTarg / R - phi0;


                int fstarfBkg = fitk.fitBackward(ht);
                double bkwdkfchi2 = ht.chisquared();

                double mmTocm = 0.1;
                double dcaX = mmTocm * HelixUtils.PointOnHelix(helicalTrack, 0).x();
                double dcaY = mmTocm * HelixUtils.PointOnHelix(helicalTrack, 0).y();
                if (_debug)
                    System.out.println("Kalman Filter Driver:  DCA X=" + dcaX + "; Y=" + dcaY);
//                SurfDCA s = new SurfDCA(dcaX, dcaY);
                SurfDCA s = new SurfDCA(0, 0);
//                 SurfXYPlane srfc = new SurfXYPlane(0.001, 0);

                ht.propagate(prop, s, PropDir.BACKWARD);
                ETrack bkwdET = ht.newTrack();
                TrackError bkwdfitErr = bkwdET.error();
                double bkwdkfDCA = bkwdET.vector().get(0);
                double bkwdkfz0 = bkwdET.vector().get(1);
                double bkwdkfphi0 = Math.sin(bkwdET.vector().get(2));
                double bkwdkfslope = bkwdET.vector().get(3);
                double bkwdkfqOverpt = bkwdET.vector().get(4);

                aida.histogram1D(bkwdkaldir + "Kalman Fit Chi2", 50, 0, 100).fill(bkwdkfchi2);
                aida.histogram1D(kaldir + "Nominal Fit Chi2", 50, 0, 100).fill(track.getChi2());
                if (bkwdkfchi2 > 0) {

                    aida.histogram1D(bkwdkaldir + "Kalman Fit DCA", 50, -0.1, 0.1).fill(bkwdkfDCA);
                    aida.histogram1D(bkwdkaldir + "Kalman Fit z0", 50, -0.1, 0.1).fill(bkwdkfz0);
                    aida.histogram1D(bkwdkaldir + "Kalman Fit sin(phi0)", 50, -0.2, 0.2).fill(bkwdkfphi0);
                    aida.histogram1D(bkwdkaldir + "Kalman Fit tanlambda", 50, -0.1, 0.1).fill(bkwdkfslope);
                    aida.histogram1D(bkwdkaldir + "Kalman Fit qOverpt", 50, -5, 5).fill(bkwdkfqOverpt);
                    aida.histogram1D(bkwdkaldir + "Kalman-Nominal DCA", 50, -.1, 0.1).fill(bkwdkfDCA - nomDCA);
                    aida.histogram1D(bkwdkaldir + "Kalman-Nominal z0", 50, -0.1, 0.1).fill(bkwdkfz0 - nomz0);
                    aida.histogram1D(bkwdkaldir + "Kalman-Nominal sin(phi0)", 50, -0.05, 0.05).fill(bkwdkfphi0 - nomphi0);
                    aida.histogram1D(bkwdkaldir + "Kalman-Nominal slope", 50, -0.05, 0.05).fill(bkwdkfslope - nomslope);
                    aida.histogram1D(bkwdkaldir + "Kalman-Nominal qOverpt", 50, -0.1, 0.1).fill(bkwdkfqOverpt - nomqOverpt);
                    aida.histogram1D(kaldir + "Nominal Fit DCA", 50, -0.1, 0.1).fill(nomDCA);
                    aida.histogram1D(kaldir + "Nominal Fit z0", 50, -0.1, 0.1).fill(nomz0);
                    aida.histogram1D(kaldir + "Nominal Fit sin(phi0)", 50, -0.2, 0.2).fill(nomphi0);
                    aida.histogram1D(kaldir + "Nominal Fit tanlambda", 50, -0.1, 0.1).fill(nomslope);
                    aida.histogram1D(kaldir + "Nominal Fit qOverpt", 50, -5, 5).fill(nomqOverpt);
                }

                MCParticle mcp=tkanal.getMCParticle();
                VTrack vmcp=trackUtils.makeVTrack(mcp);
                Hep3Vector pmc=trackUtils.getMomentum(vmcp);
                Hep3Vector pkal=trackUtils.getMomentum(bkwdET);
                Hep3Vector pnom=trackUtils.getMomentum(vt);

                double pkaldiff=pkal.magnitude()-pmc.magnitude();
                double pnomdiff=pnom.magnitude()-pmc.magnitude();
                 aida.histogram1D(kaldir + "Kalman-MC Momentum Difference", 50, -0.2, 0.2).fill(pkaldiff);
                    aida.histogram1D(kaldir + "Nominal-MC Momentum Difference", 50, -0.2, 0.2).fill(pnomdiff);
                    
            }
        }


    }
//        } else {
//            System.out.println("No tracks!");
//        }
}
