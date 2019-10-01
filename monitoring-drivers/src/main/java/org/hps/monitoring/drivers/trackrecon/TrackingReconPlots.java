package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.detector.hodoscope.HodoscopePixelDetectorElement;

import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.plot;
import org.hps.recon.ecal.HodoUtils;
import org.hps.recon.ecal.SimpleGenericObject;
import org.hps.recon.tracking.CoordinateTransformations;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
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

    double feeMomentumCut = 3.5;
    int nmodules = 7;

    IPlotter plotter;
    IPlotter plotter22;
    IPlotter plotterECal;
    IPlotter plotterFEE;
    IPlotter plotterLayers;
    IPlotter plotterHTH;
    IPlotter plotterXvsY;
    IPlotter plotterXvsYHOT;
    IPlotter plotterHodo;

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
    IHistogram1D htopLay;
    IHistogram1D hbotLay;

    IHistogram2D htrkProjH1TopMatch;
    IHistogram2D htrkProjH2TopMatch;
    IHistogram2D htrkProjH1BotMatch;
    IHistogram2D htrkProjH2BotMatch;

    IHistogram2D htrkProjH1TopNoMatch;
    IHistogram2D htrkProjH2TopNoMatch;
    IHistogram2D htrkProjH1BotNoMatch;
    IHistogram2D htrkProjH2BotNoMatch;

    IHistogram1D[] hthTop = new IHistogram1D[nmodules];
    IHistogram1D[] hthBot = new IHistogram1D[nmodules];
    IHistogram2D[] xvsyTop = new IHistogram2D[nmodules];
    IHistogram2D[] xvsyBot = new IHistogram2D[nmodules];
    IHistogram2D[] xvsyTopHOT = new IHistogram2D[nmodules];
    IHistogram2D[] xvsyBotHOT = new IHistogram2D[nmodules];
//HODOSCOPE Stuff
    private static final String SUBDETECTOR_NAME = "Hodoscope";
    private List<HodoscopePixelDetectorElement> pixels;
    private List<HodoscopeDetectorElement> hodos;
    private Map<IIdentifier, DetectorElement> hodoMap = new HashMap<IIdentifier, DetectorElement>();

    // ===== The Mode1 Hodo hit collection name =====
//    private String rawCollectionName = "HodoReadoutHits";
//    private String hodoCollectionName = "HodoCalHits";
//    private final String hodoReadoutCollectionName = "HodoscopeHits";
//    private String hodoHitsCollectionName = "HodoGenericHits";
    private String hodoClustersCollectionName = "HodoGenericClusters";

    double pMax = 7.0;

    public void setFeeMomentumCut(double cut) {
        this.feeMomentumCut = cut;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
//        pixels = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HodoscopePixelDetectorElement.class);
//
//        for (HodoscopePixelDetectorElement pix : pixels) {
//            System.out.println("TrackingReconPlots:: pix = " + pix.getName() + " position = " + pix.getGeometry().getPosition().toString());
//            System.out.println("TrackingReconPlots:: cellID =" + pix.getIdentifier().toString());
//            hodoMap.put(pix.getIdentifier(), pix);
//        }
        hodoMap = HodoUtils.getHodoscopeMap(detector);

        //pix.getGeometry().getPhysicalVolume(pix.getGeometry().getPosition()).
//         hodos = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HodoscopeDetectorElement.class);
//        for (HodoscopeDetectorElement hod : hodos)
//            System.out.println("TrackingReconPlots:: hod = " + hod.getName() + " position = " + hod.getGeometry().getPosition().toString()); //pix.getGeometry().getPhysicalVolume(pix.getGeometry().getPosition()).
        aida.tree().cd("/");

        IAnalysisFactory fac = aida.analysisFactory();
        IPlotterFactory pfac = fac.createPlotterFactory("Track Recon");
        plotter = pfac.create("Momentum");

        plotter.createRegions(2, 3);
        // plotterFrame.addPlotter(plotter);

        nhits = aida.histogram1D("Hits per Track", 4, 4, 8);

        charge = aida.histogram1D("Track Charge", 3, -1, 2);
        trkPx = aida.histogram1D("Track Momentum (Px)", 50, -0.1, 0.2);
        trkPy = aida.histogram1D("Track Momentum (Py)", 50, -0.2, 0.2);
        trkPz = aida.histogram1D("Track Momentum (Pz)", 50, 0, pMax);
        trkChi2 = aida.histogram1D("Track Chi2/NDF", 50, 0, 15.0);

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
        heVsP = aida.histogram2D("Momentum vs ECal E ", 50, 0, 7.0, 50, 0, 7.0);

        plotterECal = pfac.create("Cluster Matching");
        plotterECal.createRegions(2, 2);
        plot(plotterECal, heOverP, null, 0);
        plot(plotterECal, hdelXECal, null, 1);
        plot(plotterECal, hdelYECal, null, 2);
        plot(plotterECal, heVsP, null, 3);

        plotterECal.show();

        // ******************************************************************
        // fix the ranges here...
        hfeeMom = aida.histogram1D("FEE Momentum", 50, feeMomentumCut, pMax);
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
        plotterHTH.createRegions(4, 4);
        plotterXvsY = pfac.create("3d Hit Positions");
        plotterXvsY.createRegions(4, 4);

        plotterXvsYHOT = pfac.create("3d Hits-On-Track");
        plotterXvsYHOT.createRegions(4, 4);

        for (int i = 0; i < nmodules; i++) {
            double maxHTHX = 150.0;
            double maxHTHY = 50.0;
            if (i < 2) {
                maxHTHX = 10;
                maxHTHY = 15;
            } else if (i < 4) {
                maxHTHX = 50;
                maxHTHY = 30;
            }
            xvsyTop[i] = aida.histogram2D("Module " + i + " Top (abs(Y))", 100, -maxHTHX, maxHTHX, 55, 0, maxHTHY);
            xvsyBot[i] = aida.histogram2D("Module " + i + " Bottom (abs(Y))", 100, -maxHTHX, maxHTHX, 55, 0, maxHTHY);
            xvsyTopHOT[i] = aida.histogram2D("Module " + i + " Top HOT (abs(Y))", 100, -maxHTHX, maxHTHX, 55, 0, maxHTHY);
            xvsyBotHOT[i] = aida.histogram2D("Module " + i + " Bottom HOT (abs(Y))", 100, -maxHTHX, maxHTHX, 55, 0, maxHTHY);
            hthTop[i] = aida.histogram1D("Module " + i + "Top: Track Hits", 25, 0, 25);
            hthBot[i] = aida.histogram1D("Module " + i + "Bot: Track Hits", 25, 0, 25);
            plot(plotterHTH, hthTop[i], null, computePlotterRegion(i, true));
            plot(plotterHTH, hthBot[i], null, computePlotterRegion(i, false));
            plot(plotterXvsY, xvsyTop[i], null, computePlotterRegion(i, true));
            plot(plotterXvsY, xvsyBot[i], null, computePlotterRegion(i, false));
            plot(plotterXvsYHOT, xvsyTopHOT[i], null, computePlotterRegion(i, true));
            plot(plotterXvsYHOT, xvsyBotHOT[i], null, computePlotterRegion(i, false));
        }
        plotterHTH.show();
        plotterXvsY.show();
        plotterXvsYHOT.show();

        htopLay = aida.histogram1D("Top Layers on Track", 7, 0, 7);
        hbotLay = aida.histogram1D("Bottom Layers on Track", 7, 0, 7);
        plotterLayers = pfac.create("Layers Hit on Track");
        plotterLayers.createRegions(1, 2);
        plot(plotterLayers, htopLay, null, 0);
        plot(plotterLayers, hbotLay, null, 1);
        plotterLayers.show();

        plotterHodo = pfac.create("Hodoscope Matching");
        plotterHodo.createRegions(2, 4);
        htrkProjH1TopMatch = aida.histogram2D("Top Hodoscope L1 Projection Match", 50, 0, 350, 50, 0, 100);
        htrkProjH2TopMatch = aida.histogram2D("Top Hodoscope L2 Projection Match", 50, 0, 350, 50, 0, 100);
        htrkProjH1BotMatch = aida.histogram2D("Bottom Hodoscope L1 Projection Match", 50, 0, 350, 50, 0, 100);
        htrkProjH2BotMatch = aida.histogram2D("Bottom Hodoscope L2 Projection Match", 50, 0, 350, 50, 0, 100);
        htrkProjH1TopNoMatch = aida.histogram2D("Top Hodoscope L1 Projection No Match", 50, 0, 350, 50, 0, 100);
        htrkProjH2TopNoMatch = aida.histogram2D("Top Hodoscope L2 Projection No Match", 50, 0, 350, 50, 0, 100);
        htrkProjH1BotNoMatch = aida.histogram2D("Bottom Hodoscope L1 Projection No Match", 50, 0, 350, 50, 0, 100);
        htrkProjH2BotNoMatch = aida.histogram2D("Bottom Hodoscope L2 Projection No Match", 50, 0, 350, 50, 0, 100);
        plot(plotterHodo, htrkProjH1TopMatch, null, 0);
        plot(plotterHodo, htrkProjH1BotMatch, null, 2);
        plot(plotterHodo, htrkProjH2TopMatch, null, 4);
        plot(plotterHodo, htrkProjH2BotMatch, null, 6);
        plot(plotterHodo, htrkProjH1TopNoMatch, null, 1);
        plot(plotterHodo, htrkProjH1BotNoMatch, null, 3);
        plot(plotterHodo, htrkProjH2TopNoMatch, null, 5);
        plot(plotterHodo, htrkProjH2BotNoMatch, null, 7);
        setZAxis(plotterHodo);
        plotterHodo.show();

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

        int[] topHits = {0, 0, 0, 0, 0, 0, 0};
        int[] botHits = {0, 0, 0, 0, 0, 0, 0};
        List<TrackerHit> hth = event.get(TrackerHit.class, helicalTrackHitCollectionName);
        for (TrackerHit hit : hth) {
            int module = getModuleNumber(hit);
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
            double theta = Math.asin(pt / pmag);

            trkPx.fill(momentum.y());
            trkPy.fill(momentum.z());
            trkPz.fill(momentum.x());
            trkChi2.fill(trk.getChi2() / trk.getNDF());

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

            List<TrackerHit> hitsOnTrack = trk.getTrackerHits();
            for (TrackerHit hthOnTrack : hitsOnTrack) {
                int module = getModuleNumber(hthOnTrack) - 1;
                HelicalTrackHit htc = (HelicalTrackHit) hthOnTrack;
//                System.out.println("HTC Corrected Position = "+htc.getCorrectedPosition().toString());
                if (htc.getPosition()[2] > 0) {
                    htopLay.fill(module);
                    xvsyTopHOT[module].fill(htc.getCorrectedPosition().y(), htc.getCorrectedPosition().z());
                } else {
                    hbotLay.fill(module);
                    xvsyBotHOT[module].fill(htc.getCorrectedPosition().y(), -1 * htc.getCorrectedPosition().z());
                }
            }

            TrackState stateAtEcal = TrackUtils.getTrackStateAtECal(trk);
            if (stateAtEcal == null) {
                System.out.println("Couldn't get track state at ECal");
                continue;
            }

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

            //////   Do Track-Hodoscope Matching  //// 
            // Get RawTrackerHit collection from event.
            List<SimpleGenericObject> reconHits = event.get(SimpleGenericObject.class, hodoClustersCollectionName);

            //System.out.println("Size of reconHitsi is " + reconHits.size());
            int n_hits = reconHits.get(0).getNInt();

//            // ======= Loop over hits, and fill corresponding histogram =======
//            for (int ihit = 0; ihit < n_hits; ihit++) {
//                int ix = reconHits.get(0).getIntVal(ihit);
//                int iy = reconHits.get(1).getIntVal(ihit);
//                int layer = reconHits.get(2).getIntVal(ihit);
//                double Energy = reconHits.get(3).getDoubleVal(ihit);
//                double hit_time = reconHits.get(4).getDoubleVal(ihit);
//                int detid = reconHits.get(5).getIntVal(ihit);
//            }
            TrackState stateAtHodo1 = TrackUtils.getTrackStateAtHodoL1(trk);
            TrackState stateAtHodo2 = TrackUtils.getTrackStateAtHodoL2(trk);
            if (stateAtHodo1 != null && stateAtHodo2 != null) {
                Hep3Vector posAtH1 = new BasicHep3Vector(stateAtHodo1.getReferencePoint());
                Hep3Vector posAtH2 = new BasicHep3Vector(stateAtHodo2.getReferencePoint());
                boolean isMatchHL1 = false;
                boolean isMatchHL2 = false;
                for (int ihit = 0; ihit < n_hits; ihit++) {
                    int detid = reconHits.get(5).getIntVal(ihit);
                    int layer = reconHits.get(2).getIntVal(ihit);
                    DetectorElement thisTile = hodoMap.get(new Identifier(detid));
                    if (thisTile == null) {
                        System.out.println("Could not find this tile~~~~");
                        continue;
                    }
                    if (layer == 0)
                        isMatchHL1 = isMatchHL1 || TrackUtils.detectorElementContainsPoint(CoordinateTransformations.transformVectorToDetector(posAtH1),
                                (DetectorElement) thisTile, 20.0);
                    if (layer == 1)
                        isMatchHL2 = isMatchHL2 || TrackUtils.detectorElementContainsPoint(CoordinateTransformations.transformVectorToDetector(posAtH2),
                                (DetectorElement) thisTile, 20.0);
                }
                if (posAtH1.z() > 0)
                    if (isMatchHL1)
                        htrkProjH1TopMatch.fill(posAtH1.y(), posAtH1.z());
                    else
                        htrkProjH1TopNoMatch.fill(posAtH1.y(), posAtH1.z());
                else if (isMatchHL1)
                    htrkProjH1BotMatch.fill(posAtH1.y(), -1 * posAtH1.z());
                else
                    htrkProjH1BotNoMatch.fill(posAtH1.y(), -1 * posAtH1.z());

                if (posAtH2.z() > 0)
                    if (isMatchHL2)
                        htrkProjH2TopMatch.fill(posAtH2.y(), posAtH2.z());
                    else
                        htrkProjH2TopNoMatch.fill(posAtH2.y(), posAtH2.z());
                else if (isMatchHL2)
                    htrkProjH2BotMatch.fill(posAtH2.y(), -1 * posAtH2.z());
                else
                    htrkProjH2BotNoMatch.fill(posAtH2.y(), -1 * posAtH2.z());
            }
        }
//                for (HodoscopePixelDetectorElement pix : pixels) {
////                    System.out.println("This pixel has hits = "+pix.getReadout().getHits(RawTrackerHit.class).size());
//                    boolean inH1 = TrackUtils.detectorElementContainsPoint(CoordinateTransformations.transformVectorToDetector(posAtH1), (DetectorElement) pix, 1.5);
//                }
    }

    @Override
    public void endOfData() {
        if (outputPlots != null)
            try {
                plotter.writeToFile(outputPlots + "-mom.gif");
                plotter22.writeToFile(outputPlots + "-trkparams.gif");

            } catch (IOException ex) {
                Logger.getLogger(TrackingReconPlots.class
                        .getName()).log(Level.SEVERE, null, ex);
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
//            if (dist < minDist && clEne < 3.0) {
            if (dist < minDist) {
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

    private int getModuleNumber(TrackerHit hit) {
        int module = -666;
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
        else if (layer < 12)
            module = 6;
        else
            module = 7;
        return module;
    }

    private void setZAxis(IPlotter plotter) {
        for (int i = 0; i < plotter.numberOfRegions(); i++) {
            plotter.region(i).style().setParameter("hist2DStyle", "colorMap");
            plotter.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            plotter.region(i).style().zAxisStyle().setVisible(true);
            plotter.region(i).style().zAxisStyle().setParameter("scale", "log");

        }
    }

}
