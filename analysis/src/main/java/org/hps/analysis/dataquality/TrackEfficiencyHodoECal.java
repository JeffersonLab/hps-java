package org.hps.analysis.dataquality;

import org.hps.recon.ecal.HodoUtils;
import org.hps.recon.ecal.HodoUtils.HodoCluster;
import org.hps.recon.ecal.HodoUtils.HodoClusterPair;
import org.hps.recon.ecal.HodoUtils.HodoTileIdentifier;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IProfile1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    IHistogram1D nHodoCleanClEvt;
    IHistogram1D nTrkEvt;
    IHistogram1D nPosEvt;
    IHistogram1D nParedTrkEvt;
    IHistogram1D nParedPosEvt;
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
    IHistogram1D clYECalTopMatch;
    IHistogram1D clYECalBotMatch;
    IHistogram2D clXVsYECalMatch;

    IHistogram2D hodoDLayerL0L1vsL0Top;
    IHistogram2D hodoDLayerL0L1vsL0Bot;

    IHistogram2D hodoCleanDLayerL0L1vsL0Top;
    IHistogram2D hodoCleanDLayerL0L1vsL0Bot;

    IHistogram1D nHodoPairs;
    IHistogram1D hodoPairEnergy;
    IHistogram1D hodoPairTime;
    IHistogram1D hodoAllEnergy;
    IHistogram1D hodoAllTime;
    IHistogram1D hodoCleanEnergy;
    IHistogram1D hodoCleanTime;

    double beamP = 4.4;
    int nlayers = 14;
    double minHodoE = 200.; // ADC counts?
    double maxHodoDt = 8.; // ns
    double meanHodoPairTime = 43.;// ns
    double meanHodoPairTimeCut = 8.0;// ns
    private boolean debugTrackEfficiency = false;
    private String plotDir = "TrackEfficiencyHodoECal/";

    HodoUtils hodoutils = new HodoUtils();

    private int maxNECalCl = 1; // maximum number of ECal clusters
    private int maxNHodoCl = 9; // maximum number of Hodo clusters
    private double offsetDtEcalHodo = -5.0; // time offset between hodo and ECal clusters
    private double maxDtECalHodo = 10.0;// max |deltaT| between hodo and ECal clusters
    private double clTrTimeOffset = 41;

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
        nHodoClEvt = hf.createHistogram1D(plotDir + "N Hodo Clusters in Event", 15, 0, 15.);
        nHodoCleanClEvt = hf.createHistogram1D(plotDir + "N Hodo Clean Clusters in Event", 9, 0, 9.);
        nTrkEvt = hf.createHistogram1D(plotDir + "N Tracks in Event", 9, 0, 9.);
        nPosEvt = hf.createHistogram1D(plotDir + "N Positrons in Event", 9, 0, 9.);
        nParedTrkEvt = hf.createHistogram1D(plotDir + "N Pared Tracks in Event", 9, 0, 9.);
        nParedPosEvt = hf.createHistogram1D(plotDir + "N Pared Positrons in Event", 9, 0, 9.);
        nClHodoMatches = hf.createHistogram1D(plotDir + "N Hodo-Clusters Matches", 9, 0, 9.);
        nClTrkMatches = hf.createHistogram1D(plotDir + "N Track-Clusters Matches", 9, 0, 9.);
        dtEcalHodo = hf.createHistogram1D(plotDir + "dt Ecal-Hodo", 50, -maxDtECalHodo, maxDtECalHodo);
        clEnergyECal = hf.createHistogram1D(plotDir + "ECal Cluster Energy (GeV)", 100, 0., 5.0);
        clXECal = hf.createHistogram1D(plotDir + "ECal Cluster X (mm)", 150, 100., 400.0);
        clYECal = hf.createHistogram1D(plotDir + "ECal Cluster |Y| (mm)", 60, 28, 88.0);
        clYECalTop = hf.createHistogram1D(plotDir + "ECal Top Cluster |Y| (mm)", 60, 28, 88.0);
        clYECalBot = hf.createHistogram1D(plotDir + "ECal Bottom Cluster |Y| (mm)", 60, 28, 88.0);
        clXVsYECal = hf.createHistogram2D(plotDir + "ECal Cluster X (mm) vs Cluster Y (mm)", 150, 100.0, 400.0, 50, 0,
                100.0);

        hodoDLayerL0L1vsL0Top = hf.createHistogram2D(plotDir + "Top Hodo Cluster Delta L1-L0 vs L0", 5, 0, 5, 10, -5,
                5);
        hodoDLayerL0L1vsL0Bot = hf.createHistogram2D(plotDir + "Bot Hodo Cluster Delta L1-L0 vs L0", 5, 0, 5, 10, -5,
                5);
        hodoCleanDLayerL0L1vsL0Top = hf.createHistogram2D(plotDir + "Top Hodo Clean Cluster Delta L1-L0 vs L0", 5, 0, 5,
                10, -5, 5);
        hodoCleanDLayerL0L1vsL0Bot = hf.createHistogram2D(plotDir + "Bot Hodo Clean Cluster Delta L1-L0 vs L0", 5, 0, 5,
                10, -5, 5);

        clXECalMatch = hf.createHistogram1D(plotDir + "Matched ECal Cluster X (mm)", 150, 100., 400.0);
        clYECalMatch = hf.createHistogram1D(plotDir + "Matched ECal Cluster |Y| (mm)", 60, 28, 88.0);
        clYECalTopMatch = hf.createHistogram1D(plotDir + "Matched ECal Top Cluster |Y| (mm)", 60, 28, 88.0);
        clYECalBotMatch = hf.createHistogram1D(plotDir + "Matched ECal Bot Cluster |Y| (mm)", 60, 28, 88.0);
        trClXDiff = hf.createHistogram1D(plotDir + "Track-Cluster Diff X (mm)", 50, -50., 50.0);
        trClYDiff = hf.createHistogram1D(plotDir + "Track-Cluster Diff Y (mm)", 50, -30., 30.0);
        trClTimeDiff = hf.createHistogram1D(plotDir + "Track-Cluster Diff Time (mm)", 50, -10., 10.0);
        clXVsYECalMatch = hf.createHistogram2D(plotDir + "Matched ECal Cluster X (mm) vs Cluster Y (mm)", 150, 100.0,
                400.0, 50, 0, 100.0);

        nHodoPairs = hf.createHistogram1D(plotDir + "N Hodo Pairs in Event", 9, 0, 9);
        hodoPairEnergy = hf.createHistogram1D(plotDir + "Hodo Pair Mean Energy", 50, 100, 1500.);
        hodoPairTime = hf.createHistogram1D(plotDir + "Hodo Pair Mean Time", 50, -0, 100);
        hodoAllEnergy = hf.createHistogram1D(plotDir + "Hodo Energy: All Clusters", 50, 0, 2000.);
        hodoAllTime = hf.createHistogram1D(plotDir + "Hodo Time: All Clusters", 32, 0, 128);
        hodoCleanEnergy = hf.createHistogram1D(plotDir + "Hodo Energy: Clean Clusters", 50, 0, 2000.);
        hodoCleanTime = hf.createHistogram1D(plotDir + "Hodo Time: Clean Clusters", 32, 0, 128);

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
        nTrkEvt.fill(tracks.size());
        int nPos = 0;
        for (Track trk : tracks) {
            if (trk.getCharge() < 0)
                nPos++;
        }
        nPosEvt.fill(nPos);
        List<Track> paredTrkList = removeRedundentTracks(tracks);
        nParedTrkEvt.fill(paredTrkList.size());
        nPos = 0;
        for (Track trk : paredTrkList) {
            if (trk.getCharge() < 0)
                nPos++;
        }
        nParedPosEvt.fill(nPos);
        if (ecalClusters.size() > maxNECalCl || n_hits > maxNHodoCl)
            return;
        int nclhodomatches = 0;
        // ======= Loop over hits, and fill corresponding histogram =======
        for (int ihit = 0; ihit < n_hits; ihit++) {
            int ix = hodoClusters.get(0).getIntVal(ihit);
            int iy = hodoClusters.get(1).getIntVal(ihit);
            int layer = hodoClusters.get(2).getIntVal(ihit);
            double Energy = hodoClusters.get(3).getDoubleVal(ihit);
            double hit_time = hodoClusters.get(4).getDoubleVal(ihit);
            hodoAllEnergy.fill(Energy);
            hodoAllTime.fill(hit_time);
            if (layer != 0)
                continue;
            for (int jhit = 0; jhit < n_hits; jhit++) {
                int jlayer = hodoClusters.get(2).getIntVal(jhit);
                int jy = hodoClusters.get(1).getIntVal(jhit);
                int jx = hodoClusters.get(0).getIntVal(jhit);
                if (jlayer != 1)
                    continue;
                if (jy != iy)
                    continue;
                if (jy > 0)
                    hodoDLayerL0L1vsL0Top.fill(ix, jx - ix);
                else
                    hodoDLayerL0L1vsL0Bot.fill(ix, jx - ix);
            }
        }
//        List<HodoCluster> hodoClusterList = hodoutils.makeHodoClusterList(hodoClusters);
        List<HodoCluster> hodoClusterList = hodoutils.makeCleanHodoClusterList(hodoClusters, 200, 43, maxDtECalHodo);
        nHodoCleanClEvt.fill(hodoClusterList.size());
        Map<HodoClusterPair, Cluster> hodoPairECalMap = new HashMap<HodoClusterPair, Cluster>();
        int nhodopairs = 0;
        for (HodoCluster cl : hodoClusterList) {
            int ix = cl.getXId();
            int iy = cl.getYId();
            int layer = cl.getLayer();
            double energy = cl.getEnergy();
            double hit_time = cl.getTime();
            hodoCleanEnergy.fill(energy);
            hodoCleanTime.fill(hit_time);
            System.out.println("hodoCluster layer = " + layer);
            if (layer != 0)// only loop over first layer
                continue;
            for (HodoCluster jcl : hodoClusterList) {
                int jx = jcl.getXId();
                int jy = jcl.getYId();
                int jlayer = jcl.getLayer();
                if (jlayer != 1)
                    continue;
                if (jy != iy)
                    continue;
                if (jy > 0)
                    hodoCleanDLayerL0L1vsL0Top.fill(ix, jx - ix);
                else
                    hodoCleanDLayerL0L1vsL0Bot.fill(ix, jx - ix);
            }
            HodoClusterPair pair = hodoutils.findHodoClusterPair(cl, hodoClusterList, minHodoE, maxHodoDt);
            if (pair == null)
                continue;
            System.out.println("Found a hodo pair:  L0 energy = " + pair.getLayer0Cluster().getEnergy() + "; time = "
                    + pair.getLayer0Cluster().getTime() + "; L1 energy = " + pair.getLayer1Cluster().getEnergy()
                    + "; time = " + pair.getLayer1Cluster().getTime());
            nhodopairs++;
            hodoPairEnergy.fill(pair.getMeanEnergy());
            hodoPairTime.fill(pair.getMeanTime());
            if (Math.abs(pair.getMeanTime() - meanHodoPairTime) > meanHodoPairTimeCut)// cut around the mean pair time
                continue;
            HodoUtils.HodoTileIdentifier tile_id = hodoutils.new HodoTileIdentifier(ix, iy, layer);
            Cluster matchedEcalCluster = findECalHodoMatch(tile_id, ecalClusters);
            if (matchedEcalCluster == null)
                continue;
            hodoPairECalMap.put(pair, matchedEcalCluster);
            nclhodomatches++;
            double dt = matchedEcalCluster.getCalorimeterHits().get(0).getTime() - hit_time - offsetDtEcalHodo;
            dtEcalHodo.fill(dt);
            clEnergyECal.fill(matchedEcalCluster.getEnergy());
            clXECal.fill(matchedEcalCluster.getPosition()[0]);
            clXVsYECal.fill(matchedEcalCluster.getPosition()[0], Math.abs(matchedEcalCluster.getPosition()[1]));
            clYECal.fill(Math.abs(matchedEcalCluster.getPosition()[1]));
            if (matchedEcalCluster.getPosition()[1] > 0)
                clYECalTop.fill(matchedEcalCluster.getPosition()[1]);
            else
                clYECalBot.fill(Math.abs(matchedEcalCluster.getPosition()[1]));

        }
        nHodoPairs.fill(nhodopairs);
        nClHodoMatches.fill(nclhodomatches);
        System.out.println("Number of pair-cluster matches = " + hodoPairECalMap.size());
        if (hodoPairECalMap.size() != 1) { // require a single pair-cluster match in the event
            return;
        }
        int ncltrkmatches = 0;
        Iterator<Map.Entry<HodoClusterPair, Cluster>> itr = hodoPairECalMap.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<HodoClusterPair, Cluster> entry = itr.next();
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            HodoClusterPair pair = entry.getKey();
            Cluster matchedEcalCluster = entry.getValue();
            for (Track trk : paredTrkList) {
                int charge = -1 * trk.getCharge();// switch sign
                if (charge < 0)// only pick positrons
                    continue;
                TrackState stateAtEcal = TrackUtils.getTrackStateAtECal(trk);
                Hep3Vector tPosAtECal = new BasicHep3Vector(stateAtEcal.getReferencePoint());
                Hep3Vector clPos = new BasicHep3Vector(matchedEcalCluster.getPosition());
                System.out.println("trkPos = " + tPosAtECal.toString());
                System.out.println("clPos = " + clPos.toString());

                if (tPosAtECal.z() * clPos.y() < 0)
                    continue;
                double trkTime = this.getTrackTime(trk);
                ncltrkmatches++;
                trClXDiff.fill(tPosAtECal.y() - clPos.x());
                trClYDiff.fill(tPosAtECal.z() - clPos.y());
                trClTimeDiff.fill(matchedEcalCluster.getCalorimeterHits().get(0).getTime() - trkTime - clTrTimeOffset);
                clXECalMatch.fill(matchedEcalCluster.getPosition()[0]);
                clXVsYECalMatch.fill(matchedEcalCluster.getPosition()[0],
                        Math.abs(matchedEcalCluster.getPosition()[1]));
                clYECalMatch.fill(Math.abs(matchedEcalCluster.getPosition()[1]));
                if (matchedEcalCluster.getPosition()[1] > 0)
                    clYECalTopMatch.fill(matchedEcalCluster.getPosition()[1]);
                else
                    clYECalBotMatch.fill(Math.abs(matchedEcalCluster.getPosition()[1]));
            }
        }
        nClTrkMatches.fill(ncltrkmatches);
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

    private List<Track> removeRedundentTracks(List<Track> origTrks) {
        List<Track> paredList = new ArrayList<Track>();
        for (int i = 0; i < origTrks.size(); i++) {
            Track origTrk = origTrks.get(i);
            boolean originalIsBest = true;
            for (int j = i + 1; j < origTrks.size(); j++) {
                Track testTrk = origTrks.get(j);
                if (!checkOverlapAndCheckBestTrack(origTrk, testTrk)) {
                    originalIsBest = false;
                    break;
                }
            }
            if (originalIsBest)
                paredList.add(origTrk);
        }
        System.out.println("Original List Length = " + origTrks.size());
        System.out.println("Pared    List Length = " + paredList.size());
        return paredList;
    }

    // check two track for overlapping hits...if > maxOverlap,
    // then check if the test track is "better" than original track
    // using nHits then chi squared
    // return true if original track is best, false otherwise
    private boolean checkOverlapAndCheckBestTrack(Track origTrk, Track testTrk) {
        int maxOverlap = 1;
        if (checkHitOverlap(origTrk, testTrk) <= maxOverlap) // not an overlapping track...return original
            return true;
        // otherwise, there is overlap and now choose best one
        int nHitsOrig = origTrk.getTrackerHits().size();
        int nHitsTest = testTrk.getTrackerHits().size();
        if (nHitsOrig > nHitsTest)
            return true;
        if (nHitsOrig < nHitsTest)
            return false;
        // if we get here, these have same number of 3d hits...check chiSq
        if (origTrk.getChi2() > testTrk.getChi2())
            return true;
        return false;
    }

    private int checkHitOverlap(Track trk1, Track trk2) {
        int nOverlap = 0;
        List<TrackerHit> trk1Hth = trk1.getTrackerHits();
        List<TrackerHit> trk2Hth = trk2.getTrackerHits();
        for (TrackerHit h1 : trk1Hth) {
            if (trk2Hth.contains(h1))
                nOverlap++;
        }
        System.out.println("Found an overlap of " + nOverlap);
        return nOverlap;
    }
}
