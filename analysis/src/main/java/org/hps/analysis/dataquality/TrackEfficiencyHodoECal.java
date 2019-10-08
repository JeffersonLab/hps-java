package org.hps.analysis.dataquality;

import org.hps.recon.ecal.HodoUtils;
import org.hps.recon.ecal.HodoUtils.HodoTileIdentifier;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IProfile1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.hps.recon.ecal.SimpleGenericObject;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;

/**
 * DQM driver for calculating the track efficiency using a hit in the Hodoscope
 * + a corresponding ECal cluster for the tagging since it uses the hodoscope,
 * this only works for positron-side tracks and for data run > 2019;
 * 
 * For electron-side efficiency, still need to use the e+ track+cluster +
 * elecron side clusteras the tag (see _other_ DQM driver (tbd))
 *
 * @author mgraham on September 17, 2019
 */
// TODO:  Add some quantities for DQM monitoring:  e.g. <efficiency>, <eff>_findable
public class TrackEfficiencyHodoECal extends DataQualityMonitor {

    private static Logger LOGGER = Logger.getLogger(TrackEfficiencyHodoECal.class.getPackage().getName());

    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String trackHitCollectionName = "RotatedHelicalTrackHits";
    private String fittedSVTHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "TrackerHits";
    private String siClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String trackHitMCRelationsCollectionName = "RotatedHelicalTrackMCRelations";
    private String detectorFrameHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String trackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String trackCollectionName = "GBLTracks";

    // ===== The Mode1 Hodo hit collection name =====
    private String hodoHitsCollectionName = "HodoGenericHits";
    private String hodoClustersCollectionName = "HodoGenericClusters";
//  ===============  ECal Collection name (corrected) ======
    private String ecalClusterCorrName = "EcalClustersCorr";

    private IProfile1D peffFindable;
    private IProfile1D phieffFindable;
    private IProfile1D ctheffFindable;
    private IProfile1D peffElectrons;
    private IProfile1D phieffElectrons;
    private IProfile1D ctheffElectrons;
    
    IHistogram1D nECalClEvt;
    IHistogram1D nHodoClEvt;
    IHistogram1D nClHodoMatches;
    IHistogram1D nClTrkMatches;
    IHistogram1D dtEcalHodo;
    IHistogram1D clEnergyECal;
    IHistogram1D clXECal;
    IHistogram1D clYECal;
    IHistogram1D clYECalTop;
    IHistogram1D clYECalBot;
    IHistogram2D clXVsYECal;
    IHistogram1D trClXDiff;
    IHistogram1D trClYDiff;
    IHistogram1D trClTimeDiff;
    IHistogram1D clXECalMatch;
    IHistogram1D clYECalMatch;
    IHistogram2D clXVsYECalMatch;
    
    double beamP = 4.4;
    int nlayers = 14;
   
    private boolean debugTrackEfficiency = false;
    private String plotDir = "TrackEfficiencyHodoECal/";

    HodoUtils hodoutils = new HodoUtils();

    private int maxNECalCl = 1; // maximum number of ECal clusters
    private int maxNHodoCl = 9; // maximum number of Hodo clusters
    private double offsetDtEcalHodo = -5.0; // time offset between hodo and ECal clusters
    private double maxDtECalHodo = 10.0;// max |deltaT| between hodo and ECal clusters
    private double clTrTimeOffset=41;
    public void setTrackHitCollectionName(String trackHitCollectionName) {
        this.trackHitCollectionName = trackHitCollectionName;
    }

    public void setTrackHitMCRelationsCollectionName(String trackHitMCRelationsCollectionName) {
        this.trackHitMCRelationsCollectionName = trackHitMCRelationsCollectionName;
    }

    public void setDetectorFrameHitRelationsCollectionName(String detectorFrameHitRelationsCollectionName) {
        this.detectorFrameHitRelationsCollectionName = detectorFrameHitRelationsCollectionName;
    }

    public void setTrackHitRelationsCollectionName(String trackHitRelationsCollectionName) {
        this.trackHitRelationsCollectionName = trackHitRelationsCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setDebugTrackEfficiency(boolean debug) {
        this.debugTrackEfficiency = debug;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().mkdir(plotDir);
        aida.tree().cd("/");
        IHistogramFactory hf = aida.histogramFactory();

        nECalClEvt = hf.createHistogram1D(plotDir + "N ECal Clusters in Event", 9, 0, 9.);
        nHodoClEvt = hf.createHistogram1D(plotDir + "N Hodo Clusters in Event", 9, 0, 9.);
        nClHodoMatches= hf.createHistogram1D(plotDir + "N Hodo-Clusters Matches", 9, 0, 9.);
        nClTrkMatches= hf.createHistogram1D(plotDir + "N Track-Clusters Matches", 9, 0, 9.);
        dtEcalHodo = hf.createHistogram1D(plotDir + "dt Ecal-Hodo", 50, -maxDtECalHodo, maxDtECalHodo);
        clEnergyECal = hf.createHistogram1D(plotDir + "ECal Cluster Energy (GeV)", 100, 0., 5.0);
        clXECal = hf.createHistogram1D(plotDir + "ECal Cluster X (mm)", 150, 100., 400.0);
        clYECal = hf.createHistogram1D(plotDir + "ECal Cluster |Y| (mm)", 60,28, 88.0);
        clYECalTop = hf.createHistogram1D(plotDir + "ECal Top Cluster |Y| (mm)", 100, 0., 100.0);
        clYECalBot = hf.createHistogram1D(plotDir + "ECal Bottom Cluster |Y| (mm)", 100, -100.0, 0.0);
        clXVsYECal = hf.createHistogram2D(plotDir + "ECal Cluster X (mm) vs Cluster Y (mm)", 100, 0., 300.0, 100, 0,
                100.0);
        
        clXECalMatch = hf.createHistogram1D(plotDir + "Matched ECal Cluster X (mm)", 150, 100., 400.0);
        clYECalMatch = hf.createHistogram1D(plotDir + "Matched ECal Cluster |Y| (mm)", 60,28, 88.0);

        trClXDiff = hf.createHistogram1D(plotDir + "Track-Cluster Diff X (mm)", 50, -50., 50.0);
        trClYDiff = hf.createHistogram1D(plotDir + "Track-Cluster Diff Y (mm)", 50, -30., 30.0);
        trClTimeDiff = hf.createHistogram1D(plotDir + "Track-Cluster Diff Time (mm)", 50,-10., 10.0);
        clXVsYECalMatch = hf.createHistogram2D(plotDir + "Matched ECal Cluster X (mm) vs Cluster Y (mm)", 100, 0., 300.0, 100, 0,   100.0);

    }

    @Override
    public void process(EventHeader event) {

        aida.tree().cd("/");

        // make sure the required collections exist
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + rawTrackerHitCollectionName);
            return;
        }
        if (!event.hasCollection(LCRelation.class, fittedSVTHitCollectionName))
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + fittedSVTHitCollectionName);
        // mg...2/1/2015...don't return if the fitted collection isn't there...
        // allow us to run if we simulated in "simple" mode (i.e. no time evolution)
        // return;
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + trackCollectionName);
            return;
        }
//        if (!event.hasCollection(LCRelation.class, trackHitMCRelationsCollectionName)) {
//            if (debug)
//                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + trackHitMCRelationsCollectionName);
//            return;
//        }
        if (!event.hasCollection(TrackerHit.class, siClusterCollectionName)) {
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + siClusterCollectionName);
            return;
        }

        // ==== Now check if there are clusters =====
        if (!event.hasCollection(Cluster.class, ecalClusterCorrName)) {
            return;
        }

        if (!event.hasCollection(SimpleGenericObject.class, hodoClustersCollectionName)) {
            LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + hodoClustersCollectionName);
            return;
        }

        // Get tracks
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        // Get ECal clusters from event
        List<Cluster> ecalClusters = event.get(Cluster.class, ecalClusterCorrName);
        // Get Hodoscope collection from event.
        List<SimpleGenericObject> hodoClusters = event.get(SimpleGenericObject.class, hodoClustersCollectionName);
        int n_hits = hodoClusters.get(0).getNInt();
//        System.out.println("hodo hits  = " + n_hits);
//        System.out.println("ecal hits  = " +ecalClusters.size() );
        nECalClEvt.fill(ecalClusters.size());
        nHodoClEvt.fill(n_hits);
        if (ecalClusters.size() > maxNECalCl || n_hits > maxNHodoCl)
            return;
        int nclhodomatches=0;
        // ======= Loop over hits, and fill corresponding histogram =======
        for (int ihit = 0; ihit < n_hits; ihit++) {
            int ix = hodoClusters.get(0).getIntVal(ihit);
            int iy = hodoClusters.get(1).getIntVal(ihit);
            int layer = hodoClusters.get(2).getIntVal(ihit);
//            int hole = hodoClusters.get(3).getIntVal(ihit);
            double Energy = hodoClusters.get(3).getDoubleVal(ihit);
            double hit_time = hodoClusters.get(4).getDoubleVal(ihit);
//            System.out.println("hodo time = "+hit_time);
            HodoUtils.HodoTileIdentifier tile_id = hodoutils.new HodoTileIdentifier(ix, iy, layer);
            Cluster matchedEcalCluster = findECalHodoMatch(tile_id, ecalClusters);
            if (matchedEcalCluster == null)
                continue;
            double dt = matchedEcalCluster.getCalorimeterHits().get(0).getTime() - hit_time - offsetDtEcalHodo;
            System.out.println("Found matching cluster dt = " + dt);
            if (Math.abs(dt) > maxDtECalHodo)
                continue;
            nclhodomatches++;
            dtEcalHodo.fill(dt);
            clEnergyECal.fill(matchedEcalCluster.getEnergy());
            clXECal.fill(matchedEcalCluster.getPosition()[0]);
            clXVsYECal.fill(matchedEcalCluster.getPosition()[0], Math.abs(matchedEcalCluster.getPosition()[1]));
            clYECal.fill(Math.abs(matchedEcalCluster.getPosition()[1]));
            int ncltrkmatches=0;
            for (Track trk : tracks) {
                int charge = -1 * trk.getCharge();// switch sign
                TrackState stateAtEcal = TrackUtils.getTrackStateAtECal(trk);
                Hep3Vector tPosAtECal = new BasicHep3Vector(stateAtEcal.getReferencePoint());
                Hep3Vector clPos = new BasicHep3Vector(matchedEcalCluster.getPosition());
                if (tPosAtECal.y() * clPos.y() < 0)
                    continue;
                System.out.println("trkPos = "+tPosAtECal.toString());
                System.out.println("clPos = "+clPos.toString());
                trClXDiff.fill(tPosAtECal.y() - clPos.x());
                trClYDiff.fill(tPosAtECal.z() - clPos.y());
                double trkTime = this.getTrackTime(trk);
                ncltrkmatches++;
                trClTimeDiff.fill( matchedEcalCluster.getCalorimeterHits().get(0).getTime()-trkTime -clTrTimeOffset);
                clXECalMatch.fill(matchedEcalCluster.getPosition()[0]);
                clXVsYECalMatch.fill(matchedEcalCluster.getPosition()[0], Math.abs(matchedEcalCluster.getPosition()[1]));
                clYECalMatch.fill(Math.abs(matchedEcalCluster.getPosition()[1]));
                
            }
            nClTrkMatches.fill(ncltrkmatches);
        }
        nClHodoMatches.fill(nclhodomatches);
        
        for (int ihit = 0; ihit < n_hits; ihit++) {
            int ix = hodoClusters.get(0).getIntVal(ihit);
            int iy = hodoClusters.get(1).getIntVal(ihit);
            int layer = hodoClusters.get(2).getIntVal(ihit);
//            int hole = hodoClusters.get(3).getIntVal(ihit);
            double Energy = hodoClusters.get(3).getDoubleVal(ihit);
            double hit_time = hodoClusters.get(4).getDoubleVal(ihit);
            if(layer!=0)//only loop over first layer
                continue;
        }
    }

    @Override
    public void fillEndOfRunPlots() {
    }

    @Override
    public void dumpDQMData() {
    }

    private IProfile1D getLayerPlot(String prefix, int layer) {
        return aida.profile1D(prefix + "_layer" + layer);
    }

    private IProfile1D createLayerPlot(String prefix, int layer, int nchan, double min, double max) {
        IProfile1D hist = aida.profile1D(prefix + "_layer" + layer, nchan, min, max);
        return hist;
    }

    private boolean hasHTHInEachLayer(Set<HelicalTrackCross> list, Set<FittedRawTrackerHit> fitlist) {
        if (list.isEmpty())
            return false;
        if (!(list.toArray()[0] instanceof HelicalTrackCross))
            return false;
        for (int layer = 1; layer < nlayers - 2; layer += 2) {
            boolean hasThisLayer = false;
            for (HelicalTrackCross hit : list)
                if (hit.Layer() == layer)
                    hasThisLayer = true;
            if (!hasThisLayer) {
//                LOGGER.info("Missing reconstructed hit in layer = " + layer);
                boolean hasFitHitSL1 = false;
                boolean hasFitHitSL2 = false;
                FittedRawTrackerHit fitSL1 = null;
                FittedRawTrackerHit fitSL2 = null;
//                LOGGER.info("fitted hit list size = " + fitlist.size());
                for (FittedRawTrackerHit fit : fitlist) {
//                    LOGGER.info("fitted hit layer number = " + fit.getRawTrackerHit().getLayerNumber());
                    if (fit.getRawTrackerHit().getLayerNumber() == layer) {
                        hasFitHitSL1 = true;
                        fitSL1 = fit;
//                        LOGGER.info("Found a hit in SL1 with t0 = " + fitSL1.getT0() + "; amp = " + fitSL1.getAmp() + "; chi^2 = " + fitSL1.getShapeFitParameters().getChiProb() + "; strip = " + fitSL1.getRawTrackerHit().getCellID());
                    }
                    if (fit.getRawTrackerHit().getLayerNumber() == layer + 1) {
                        hasFitHitSL2 = true;
                        fitSL2 = fit;
//                        LOGGER.info("Found a hit in SL2 with t0 = " + fitSL2.getT0() + "; amp = " + fitSL2.getAmp() + "; chi^2 = " + fitSL2.getShapeFitParameters().getChiProb() + "; strip = " + fitSL2.getRawTrackerHit().getCellID());

                    }
                }
//                if (!hasFitHitSL1)
//                    LOGGER.info("MISSING a hit in SL1!!!");
//                if (!hasFitHitSL2)
//                    LOGGER.info("MISSING a hit in SL2!!!");

                return false;
            }
        }
        return true;
    }

    private double calcMagnitude(double[] vec) {
        return Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
    }

    private Cluster findECalHodoMatch(HodoTileIdentifier tile_id, List<Cluster> ecalClusters) {
        Cluster match = null;
        for (Cluster ecalCluster : ecalClusters) {
            if (hodoutils.hasRightClust(tile_id, ecalCluster.getPosition()[0], ecalCluster.getEnergy())) {
                System.out.println("Found matched cluster");
                match = ecalCluster;
            }
        }
        return match;
    }

    private double getTrackTime(Track track) {
        int hitCount = 0;
        double trackTime = 0;
        for (TrackerHit hitCross : track.getTrackerHits())
            for (HelicalTrackStrip hit : ((HelicalTrackCross) hitCross).getStrips()) {
                SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.rawhits().get(0)).getDetectorElement();
                trackTime += hit.time();
                hitCount++;
            }
        return trackTime / hitCount;
    }
}
