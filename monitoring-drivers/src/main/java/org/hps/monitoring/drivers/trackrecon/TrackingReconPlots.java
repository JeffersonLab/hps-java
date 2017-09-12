package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.plot;

import org.hps.recon.tracking.HpsHelicalTrackFit;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class TrackingReconPlots extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String trackCollectionName = "MatchedTracks";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    IDDecoder dec;
    private String outputPlots = null;
    private boolean debug = false;

    double feeMomentumCut = 0.8;
    int nmodules = 6;

    IPlotter plotter;
    IPlotter plotter22;
    IPlotter plotterECal;
    IPlotter plotterFEE;
    IPlotter plotterHTH;
    IPlotter plotterXvsY;

    IHistogram1D nTracks;
    IHistogram1D nhits;
    IHistogram1D charge;
    IHistogram1D trkPx;
    IHistogram1D trkPy;
    IHistogram1D trkPz;
    IHistogram1D trkChi2;
    IHistogram1D trkd0;
    IHistogram1D trkphi;
    IHistogram1D trkomega;
    IHistogram1D trklam;
    IHistogram1D trkz0;
    IHistogram1D heOverP;
    IHistogram1D hdelXECal;
    IHistogram1D hdelYECal;
    IHistogram2D heVsP;
    IHistogram1D hfeeMom;
    IHistogram1D hfeeTheta;
    IHistogram1D hfeePOverE;
    IHistogram2D hfeeClustPos;

    IHistogram1D[] hthTop = new IHistogram1D[nmodules];
    IHistogram1D[] hthBot = new IHistogram1D[nmodules];
    IHistogram2D[] xvsyTop = new IHistogram2D[nmodules];
    IHistogram2D[] xvsyBot = new IHistogram2D[nmodules];

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        IAnalysisFactory fac = aida.analysisFactory();
        IPlotterFactory pfac = fac.createPlotterFactory("Track Recon");
        plotter = pfac.create("Momentum");

        plotter.createRegions(2, 3);
        // plotterFrame.addPlotter(plotter);
        nhits = aida.histogram1D("Hits per Track", 2, 5, 7);
        charge = aida.histogram1D("Track Charge", 3, -1, 2);
        trkPx = aida.histogram1D("Track Momentum (Px)", 50, -0.1, 0.2);
        trkPy = aida.histogram1D("Track Momentum (Py)", 50, -0.2, 0.2);
        trkPz = aida.histogram1D("Track Momentum (Pz)", 50, 0, 3);
        trkChi2 = aida.histogram1D("Track Chi2", 50, 0, 25.0);

        plot(plotter, nhits, null, 0);
        plot(plotter, charge, null, 1);
        plot(plotter, trkPx, null, 2);
        plot(plotter, trkPy, null, 3);
        plot(plotter, trkPz, null, 4);
        plot(plotter, trkChi2, null, 5);

        plotter.show();

        // ******************************************************************
        nTracks = aida.histogram1D("Number of Tracks ", 7, 0, 7.0);
        trkd0 = aida.histogram1D("d0 ", 50, -5.0, 5.0);
        trkphi = aida.histogram1D("sinphi ", 50, -0.1, 0.15);
        trkomega = aida.histogram1D("omega ", 50, -0.0006, 0.0006);
        trklam = aida.histogram1D("tan(lambda) ", 50, -0.1, 0.1);
        trkz0 = aida.histogram1D("y0 ", 50, -1.0, 1.0);

        plotter22 = pfac.create("Track parameters");
        plotter22.createRegions(2, 3);
        plot(plotter22, nTracks, null, 0);
        plot(plotter22, trkd0, null, 1);
        plot(plotter22, trkphi, null, 2);
        plot(plotter22, trkomega, null, 3);
        plot(plotter22, trklam, null, 4);
        plot(plotter22, trkz0, null, 5);

        plotter22.show();

        // ******************************************************************
        heOverP = aida.histogram1D("Cluster Energy over Track Momentum ", 50, 0, 2.0);
        hdelXECal = aida.histogram1D("delta X @ ECal (mm) ", 50, -15.0, 15.0);
        hdelYECal = aida.histogram1D("delta Y @ ECal (mm) ", 50, -15.0, 15.0);
        heVsP = aida.histogram2D("Momentum vs ECal E ", 50, 0, 2.5, 50, 0, 2.5);

        plotterECal = pfac.create("Cluster Matching");
        plotterECal.createRegions(2, 2);
        plot(plotterECal, heOverP, null, 0);
        plot(plotterECal, hdelXECal, null, 1);
        plot(plotterECal, hdelYECal, null, 2);
        plot(plotterECal, heVsP, null, 3);

        plotterECal.show();

        // ******************************************************************
        // fix the ranges here...
        hfeeMom = aida.histogram1D("FEE Momentum", 50, feeMomentumCut, 2.2);
        hfeeTheta = aida.histogram1D("FEE Angle", 50, -15.0, 15.0);
        hfeePOverE = aida.histogram1D("FEE POverE", 50, 0, 1.5);
        hfeeClustPos = aida.histogram2D("FEE Cluster Position", 50, -2000.0, 2000.0, 50, -500, 500);

        plotterFEE = pfac.create("Full Energy Electrons");
        plotterFEE.createRegions(2, 2);
        plot(plotterFEE, hfeeMom, null, 0);
        plot(plotterFEE, hfeeTheta, null, 1);
        plot(plotterFEE, hfeePOverE, null, 2);
        plot(plotterFEE, hfeeClustPos, null, 3);

        plotterFEE.show();

        plotterHTH = pfac.create("Track Hits");
        plotterHTH.createRegions(3, 4);
        plotterXvsY = pfac.create("3d Hit Positions");
        plotterXvsY.createRegions(3, 4);

        for (int i = 1; i <= nmodules; i++) {

            xvsyTop[i - 1] = aida.histogram2D("Module " + i + " Top", 100, -100, 150, 55, 0, 55);
            xvsyBot[i - 1] = aida.histogram2D("Module " + i + " Bottom", 100, -100, 150, 55, 0, 55);
            hthTop[i - 1] = aida.histogram1D("Module " + i + "Top: Track Hits", 25, 0, 25);
            hthBot[i - 1] = aida.histogram1D("Module " + i + "Bot: Track Hits", 25, 0, 25);
            plot(plotterHTH, hthTop[i - 1], null, computePlotterRegion(i - 1, true));
            plot(plotterHTH, hthBot[i - 1], null, computePlotterRegion(i - 1, false));
            plot(plotterXvsY, xvsyTop[i - 1], null, computePlotterRegion(i - 1, true));
            plot(plotterXvsY, xvsyBot[i - 1], null, computePlotterRegion(i - 1, false));
        }
        plotterHTH.show();
        plotterXvsY.show();

    }

    public TrackingReconPlots() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setDebug(boolean dbg) {
        this.debug = dbg;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");

        if (!event.hasCollection(Track.class, trackCollectionName)) {
            nTracks.fill(0);
            return;
        }

        if (!event.hasCollection(TrackerHit.class, helicalTrackHitCollectionName))
            return;

        int[] topHits = {0, 0, 0, 0, 0, 0};
        int[] botHits = {0, 0, 0, 0, 0, 0};
        List<TrackerHit> hth = event.get(TrackerHit.class, helicalTrackHitCollectionName);
        for (TrackerHit hit : hth) {
            int module = -99;
            int layer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
            if (layer < 2)
                module = 1;
            else if (layer < 4)
                module = 2;
            else if (layer < 6)
                module = 3;
            else if (layer < 8)
                module = 4;
            else if (layer < 10)
                module = 5;
            else
                module = 6;

            if (hit.getPosition()[1] > 0) {
                topHits[module - 1]++;
                xvsyTop[module - 1].fill(hit.getPosition()[0], hit.getPosition()[1]);
            } else {
                botHits[module - 1]++;
                xvsyBot[module - 1].fill(hit.getPosition()[0], -1 * hit.getPosition()[1]);
            }
        }

        for (int i = 0; i < nmodules; i++) {
            hthTop[i].fill(topHits[i]);
            hthBot[i].fill(botHits[i]);
        }

        List<Track> tracks = event.get(Track.class, trackCollectionName);
        nTracks.fill(tracks.size());

        for (Track trk : tracks) {
            Hep3Vector momentum = new BasicHep3Vector(trk.getTrackStates().get(0).getMomentum());
            double pmag = momentum.magnitude();
            double pt = Math.sqrt(momentum.z() * momentum.z() + momentum.y() * momentum.y());
            double theta = Math.acos(pt / pmag);

            trkPx.fill(momentum.y());
            trkPy.fill(momentum.z());
            trkPz.fill(momentum.x());
            trkChi2.fill(trk.getChi2());

            nhits.fill(trk.getTrackerHits().size());
            charge.fill(-trk.getCharge());
            trkd0.fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
            trkphi.fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
            trkomega.fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
            trklam.fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
            trkz0.fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));

            if (pmag > feeMomentumCut && trk.getCharge() > 0) { // remember, hps-java track charge is opposite the real
                                                                // charge
                hfeeMom.fill(momentum.magnitude());
                hfeeTheta.fill(theta);
            }

            SeedTrack stEle = (SeedTrack) trk;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            HpsHelicalTrackFit hpstrk = new HpsHelicalTrackFit(ht);
            double svt_l12 = 900.00;// mm ~approximately...this doesn't matter much
            double ecal_face = 1393.00;// mm ~approximately ... this matters! Should use typical shower depth...or, once
                                       // have cluster match, use that value of Z
            TrackState stateAtEcal = TrackUtils.getTrackStateAtECal(trk);
            Hep3Vector posAtEcal = new BasicHep3Vector(stateAtEcal.getReferencePoint());
            // Hep3Vector posAtEcal = hpstrk.getPositionAtZMap(svt_l12, ecal_face, 5.0,
            // event.getDetector().getFieldMap())[0];
            List<Cluster> clusters = event.get(Cluster.class, ecalCollectionName);
            if (clusters != null) {
                if (debug)
                    System.out.println("Found " + clusters.size() + " clusters");
                Cluster clust = findClosestCluster(posAtEcal, clusters);
                if (clust != null) {
                    if (debug)
                        System.out.println("\t\t\t Found the best clusters");
                    Hep3Vector clusterPos = new BasicHep3Vector(clust.getPosition());
                    double zCluster = clusterPos.z();
                    // improve the extrapolation...use the reconstructed cluster z-position
                    // stateAtEcal = TrackUtils.extrapolateTrackUsingFieldMap(trk, svt_l12, zCluster, 5.0,
                    // event.getDetector().getFieldMap());
                    // posAtEcal = new BasicHep3Vector(stateAtEcal.getReferencePoint());
                    double eOverP = clust.getEnergy() / pmag;
                    double dx = posAtEcal.y() - clusterPos.x();
                    double dy = posAtEcal.z() - clusterPos.y();
                    heOverP.fill(eOverP);
                    hdelXECal.fill(dx);
                    hdelYECal.fill(dy);
                    heVsP.fill(pmag, clust.getEnergy());
                    if (pmag > feeMomentumCut && trk.getCharge() > 0) { // remember, hps-java track charge is opposite
                                                                        // the real charge
                        hfeePOverE.fill(pmag / clust.getEnergy());
                        hfeeClustPos.fill(posAtEcal.x(), posAtEcal.y());
                    }
                }
            }
        }

    }

    @Override
    public void endOfData() {
        if (outputPlots != null)
            try {
                plotter.writeToFile(outputPlots + "-mom.gif");
                plotter22.writeToFile(outputPlots + "-trkparams.gif");
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconPlots.class.getName()).log(Level.SEVERE, null, ex);
            }

        // plotterFrame.dispose();
        // topFrame.dispose();
        // bottomFrame.dispose();
    }

    /*
     * mg...3/26/15...use this for now; eventually use the FinalStateParticle matching
     */
    private Cluster findClosestCluster(Hep3Vector posonhelix, List<Cluster> clusters) {
        Cluster closest = null;
        double minDist = 9999;
        for (Cluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double clEne = cluster.getEnergy();
            // double dist = Math.sqrt(Math.pow(clPos[0] - posonhelix.y(), 2) + Math.pow(clPos[1] - posonhelix.z(), 2));
            // //coordinates!!!
            double dist = Math.sqrt(Math.pow(clPos[1] - posonhelix.z(), 2)); // coordinates!!!
            if (dist < minDist && clEne < 3.0) {
                closest = cluster;
                minDist = dist;
            }
        }
        return closest;
    }

    private int computePlotterRegion(int i, boolean istop) {

        int region = -99;
        if (i < 3)
            if (istop)
                region = i * 4;
            else
                region = i * 4 + 1;
        else if (istop)
            region = (i - 3) * 4 + 2;
        else
            region = (i - 3) * 4 + 3;
        // System.out.println("Setting region to "+region);
        return region;
    }

}
