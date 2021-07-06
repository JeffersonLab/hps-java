package org.hps.monitoring.drivers.monitoring2021.headless;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

public class KFTrackRecoHeadless extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String trackCollectionName = "KalmanFullTracks";
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
//HODOSCOPE Stuff
    private Map<IIdentifier, DetectorElement> hodoMap = new HashMap<IIdentifier, DetectorElement>();

    private String hodoClustersCollectionName = "HodoGenericClusters";

    double pMax = 7.0;

    public void setFeeMomentumCut(double cut) {
        this.feeMomentumCut = cut;
    }

    @Override
    protected void detectorChanged(Detector detector) {


        hodoMap = HodoUtils.getHodoscopeMap(detector);

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

        // ******************************************************************
        nTracks = aida.histogram1D("Number of Tracks ", 7, 0, 7.0);
        trkd0 = aida.histogram1D("d0 ", 50, -5.0, 5.0);
        trkphi = aida.histogram1D("sinphi ", 50, -0.1, 0.15);
        trkomega = aida.histogram1D("omega ", 50, -0.0006, 0.0006);
        trklam = aida.histogram1D("tan(lambda) ", 50, -0.1, 0.1);
        trkz0 = aida.histogram1D("y0 ", 50, -1.0, 1.0);

        // ******************************************************************
        heOverP = aida.histogram1D("Cluster Energy over Track Momentum ", 50, 0, 2.0);
        hdelXECal = aida.histogram1D("delta X @ ECal (mm) ", 50, -15.0, 15.0);
        hdelYECal = aida.histogram1D("delta Y @ ECal (mm) ", 50, -15.0, 15.0);
        heVsP = aida.histogram2D("Momentum vs ECal E ", 50, 0, 7.0, 50, 0, 7.0);

        // ******************************************************************
        // fix the ranges here...
        hfeeMom = aida.histogram1D("FEE Momentum", 50, feeMomentumCut, pMax);
        hfeeTheta = aida.histogram1D("FEE Angle", 50, -15.0, 15.0);
        hfeePOverE = aida.histogram1D("FEE POverE", 50, 0, 1.5);
        hfeeClustPos = aida.histogram2D("FEE Cluster Position", 50, -2000.0, 2000.0, 50, -500, 500);

   

        htopLay = aida.histogram1D("Top Layers on Track", 7, 0, 7);
        hbotLay = aida.histogram1D("Bottom Layers on Track", 7, 0, 7);     
    
        htrkProjH1TopMatch = aida.histogram2D("Top Hodoscope L1 Projection Match", 50, 0, 350, 50, 0, 100);
        htrkProjH2TopMatch = aida.histogram2D("Top Hodoscope L2 Projection Match", 50, 0, 350, 50, 0, 100);
        htrkProjH1BotMatch = aida.histogram2D("Bottom Hodoscope L1 Projection Match", 50, 0, 350, 50, 0, 100);
        htrkProjH2BotMatch = aida.histogram2D("Bottom Hodoscope L2 Projection Match", 50, 0, 350, 50, 0, 100);
        htrkProjH1TopNoMatch = aida.histogram2D("Top Hodoscope L1 Projection No Match", 50, 0, 350, 50, 0, 100);
        htrkProjH2TopNoMatch = aida.histogram2D("Top Hodoscope L2 Projection No Match", 50, 0, 350, 50, 0, 100);
        htrkProjH1BotNoMatch = aida.histogram2D("Bottom Hodoscope L1 Projection No Match", 50, 0, 350, 50, 0, 100);
        htrkProjH2BotNoMatch = aida.histogram2D("Bottom Hodoscope L2 Projection No Match", 50, 0, 350, 50, 0, 100);      

    }

    public KFTrackRecoHeadless() {
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
    }

    @Override
    public void endOfData() {
        if (outputPlots != null)
            try {
                plotter.writeToFile(outputPlots + "-mom.gif");
                plotter22.writeToFile(outputPlots + "-trkparams.gif");

            } catch (IOException ex) {
                Logger.getLogger(KFTrackRecoHeadless.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
    }

    /*
     * mg...3/26/15...use this for now; eventually use the FinalStateParticle matching
     */
    private Cluster findClosestCluster(Hep3Vector posonhelix, List<Cluster> clusters) {
        Cluster closest = null;
        double minDist = 9999;
        for (Cluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double dist = Math.sqrt(Math.pow(clPos[1] - posonhelix.z(), 2)); // coordinates!!!
            if (dist < minDist) {
                closest = cluster;
                minDist = dist;
            }
        }
        return closest;
    }

}
