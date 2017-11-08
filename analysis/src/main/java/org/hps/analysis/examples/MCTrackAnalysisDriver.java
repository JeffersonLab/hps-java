package org.hps.analysis.examples;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class MCTrackAnalysisDriver extends Driver {

    boolean debug = true;
    private AIDA aida = AIDA.defaultInstance();
    private IHistogram1D trkChisqNdfTop = aida.histogram1D("Top Track Chisq per DoF", 100, 0., 100.);
    private IHistogram1D trkChisqProbTop = aida.histogram1D("Top Track Chisq Prob", 100, 0., 1.);
    private IHistogram1D trkNhitsTop = aida.histogram1D("Top Track Number of Hits", 7, -0.5, 6.5);
    private IHistogram1D trkMomentumTop = aida.histogram1D("Top Track Momentum", 200, 0., 3.);
    private IHistogram1D trkMomentumTop5 = aida.histogram1D("Top 5 Hit Track Momentum", 200, 0., 3.);
    private IHistogram1D trkMomentumTop6 = aida.histogram1D("Top 6 Hit Track Momentum", 200, 0., 3.);
    private IHistogram1D trkdEdXTop5 = aida.histogram1D("Top 5 Track dEdx", 100, 0., .00015);
    private IHistogram1D trkdEdXTop6 = aida.histogram1D("Top 6 Track dEdx", 100, 0., .00015);
    private IHistogram1D trkthetaTop = aida.histogram1D("Top Track theta", 100, 0.01, 0.05);
    private IHistogram1D trkX0Top = aida.histogram1D("Top Track X0", 100, -0.5, 0.5);
    private IHistogram1D trkY0Top = aida.histogram1D("Top Track Y0", 100, -5.0, 5.0);
    private IHistogram1D trkZ0Top = aida.histogram1D("Top Track Z0", 100, -1.0, 1.0);

    private IHistogram1D trkChisqNdfBottom = aida.histogram1D("Bottom Track Chisq per DoF", 100, 0., 100.);
    private IHistogram1D trkChisqProbBottom = aida.histogram1D("Bottom Track Chisq Prob", 100, 0., 1.);
    private IHistogram1D trkNhitsBottom = aida.histogram1D("Bottom Track Number of Hits", 7, -0.5, 6.5);
    private IHistogram1D trkMomentumBottom = aida.histogram1D("Bottom Track Momentum", 200, 0., 3.);
    private IHistogram1D trkMomentumBottom5 = aida.histogram1D("Bottom 5 Hit Track Momentum", 200, 0., 3.);
    private IHistogram1D trkMomentumBottom6 = aida.histogram1D("Bottom 6 Hit Track Momentum", 200, 0., 3.);
    private IHistogram1D trkdEdXBottom5 = aida.histogram1D("Bottom 5 Track dEdx", 100, 0., .00015);
    private IHistogram1D trkdEdXBottom6 = aida.histogram1D("Bottom 6 Track dEdx", 100, 0., .00015);
    private IHistogram1D trkthetaBottom = aida.histogram1D("Bottom Track theta", 100, 0.01, 0.05);
    private IHistogram1D trkX0Bottom = aida.histogram1D("Bottom Track X0", 100, -0.5, 0.5);
    private IHistogram1D trkY0Bottom = aida.histogram1D("Bottom Track Y0", 100, -5.0, 5.0);
    private IHistogram1D trkZ0Bottom = aida.histogram1D("Bottom Track Z0", 100, -1.0, 1.0);

    double[] zs = {5., 4., 3., 2., 1., 0., -1., -2., -3., -4., -5., -6., -7};

    List<IHistogram1D> xExtrapTopHist = new ArrayList<IHistogram1D>();
    List<IHistogram1D> yExtrapTopHist = new ArrayList<IHistogram1D>();
    List<IHistogram2D> xvsyExtrapTopHist = new ArrayList<IHistogram2D>();
    List<IHistogram1D> xExtrapBottomHist = new ArrayList<IHistogram1D>();
    List<IHistogram1D> yExtrapBottomHist = new ArrayList<IHistogram1D>();
    List<IHistogram2D> xvsyExtrapBottomHist = new ArrayList<IHistogram2D>();

    private final String finalStateParticlesColName = "FinalStateParticles";

    private Double _beamEnergy = 1.056;
    private double _percentFeeCut = 0.8;
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        for (int kk = 0; kk < zs.length; ++kk) {
            xExtrapTopHist.add(aida.histogram1D("Top/" + zs[kk] + "/ Track extrap X at " + zs[kk], 100, -3.0, 3.0));
            xExtrapBottomHist.add(aida.histogram1D("Bottom/" + zs[kk] + "/ Track extrap X at " + zs[kk], 100, -3.0, 3.0));
            yExtrapTopHist.add(aida.histogram1D("Top/" + zs[kk] + "/ Track extrap Y at " + zs[kk], 100, -1.0, 1.0));
            yExtrapBottomHist.add(aida.histogram1D("Bottom/" + zs[kk] + "/ Track extrap Y at " + zs[kk], 100, -1.0, 1.0));
            xvsyExtrapTopHist.add(aida.histogram2D("Top/" + zs[kk] + "/ Track extrap X vs Y at " + zs[kk], 100, -3.0, 3.0, 100, -1.0, 1.0));
            xvsyExtrapBottomHist.add(aida.histogram2D("Bottom/" + zs[kk] + "/ Track extrap X vs Y at " + zs[kk], 100, -3.0, 3.0, 100, -1.0, 1.0));
        }
//        ctMin = 55.;
//        ctMax = 61.;
    }

    protected void process(EventHeader event) {

//        setupSensors(event);
        List<Track> tracks = event.get(Track.class, "GBLTracks");
        for (Track t : tracks) {
            double chiSquared = t.getChi2();
            int ndf = t.getNDF();
            double chi2Ndf = t.getChi2() / t.getNDF();
            double chisqProb = ChisqProb.gammp(ndf, chiSquared);
            int nHits = t.getTrackerHits().size();
            double dEdx = t.getdEdx();
            TrackState ts = t.getTrackStates().get(0);
            System.out.println(ts);
            double[] p = ts.getMomentum();
            Hep3Vector pvec = new BasicHep3Vector(p);
            //rotate into physiscs frame of reference
            Hep3Vector rprot = VecOp.mult(beamAxisRotation, pvec);
            double theta = Math.acos(rprot.z() / rprot.magnitude());

            // debug diagnostics to set cuts
            if (debug) {
                aida.cloud1D("Track chisq per df").fill(chiSquared / ndf);
                aida.cloud1D("Track chisq prob").fill(chisqProb);
                aida.cloud1D("Track nHits").fill(t.getTrackerHits().size());
                aida.cloud1D("Track momentum").fill(pvec.magnitude());
                aida.cloud1D("Track deDx").fill(t.getdEdx());
                aida.cloud1D("Track theta").fill(theta);
                aida.cloud2D("Track theta vs p").fill(theta, pvec.magnitude());
                aida.cloud1D("rp x0").fill(TrackUtils.getX0(t));
                aida.cloud1D("rp y0").fill(TrackUtils.getY0(t));
                aida.cloud1D("rp z0").fill(TrackUtils.getZ0(t));
            }
        }
    }

    private boolean isTopTrack(Track t) {
        List<TrackerHit> hits = t.getTrackerHits();
        int n[] = {0, 0};
        int nHits = hits.size();
        for (TrackerHit h : hits) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) h.getRawHits().get(0)).getDetectorElement());
            if (sensor.isTopLayer()) {
                n[0] += 1;
            } else {
                n[1] += 1;
            }
        }
        if (n[0] == nHits && n[1] == 0) {
            return true;
        }
        if (n[1] == nHits && n[0] == 0) {
            return false;
        }
        throw new RuntimeException("mixed top and bottom hits on same track");

    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }
}
