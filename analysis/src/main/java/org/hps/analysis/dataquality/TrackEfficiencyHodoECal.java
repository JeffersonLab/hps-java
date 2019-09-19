package org.hps.analysis.dataquality;

import org.hps.recon.ecal.HodoUtils;
import org.hps.recon.ecal.HodoUtils.HodoTileIdentifier;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IProfile1D;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.hps.recon.ecal.SimpleGenericObject;

import org.lcsim.event.Cluster;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;

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
    private String hodoClustersCollectionName = "HodoGenericClusters";
//  ===============  ECal Collection name (corrected) ======
    private String ecalClusterCorrName = "EcalClustersCorr";

    private IProfile1D peffFindable;
    private IProfile1D phieffFindable;
    private IProfile1D ctheffFindable;
    private IProfile1D peffElectrons;
    private IProfile1D phieffElectrons;
    private IProfile1D ctheffElectrons;
    double beamP = 4.4;
    int nlayers = 14;
    int totelectrons = 0;
    double foundelectrons = 0;
    int findableelectrons = 0;
    int findableTracks = 0;
    double foundTracks = 0;
    private boolean debugTrackEfficiency = false;
    private String plotDir = "TrackEfficiency/";
    private String resDir = "TrackResolution/";

    HodoUtils hodoutils = new HodoUtils();

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
        aida.tree().mkdir(resDir);
        aida.tree().cd("/");
        IHistogramFactory hf = aida.histogramFactory();

        peffFindable = hf.createProfile1D(plotDir + "Findable Efficiency vs p", "", 20, 0., beamP);
        phieffFindable = hf.createProfile1D(plotDir + "Findable Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffFindable = hf.createProfile1D(plotDir + "Findable Efficiency vs cos(theta)", "", 25, -0.25, 0.25);
        peffElectrons = hf.createProfile1D(plotDir + "Electrons Efficiency vs p", "", 20, 0., beamP);
        phieffElectrons = hf.createProfile1D(plotDir + "Electrons Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffElectrons = hf.createProfile1D(plotDir + "Electrons Efficiency vs cos(theta)", "", 25, -0.25, 0.25);

        IHistogram1D pMCRes = hf.createHistogram1D(resDir + "Momentum Resolution", 50, -0.5, 0.5);
        IHistogram1D phi0MCRes = hf.createHistogram1D(resDir + "phi0 Resolution", 50, -0.1, 0.1);
        IHistogram1D d0MCRes = hf.createHistogram1D(resDir + "d0 Resolution", 50, -0.5, 0.5);
        IHistogram1D z0MCRes = hf.createHistogram1D(resDir + "z0 Resolution", 50, -1.0, 1.0);
        IHistogram1D tanLambdaMCRes = hf.createHistogram1D(resDir + "tanLambda Resolution", 50, -0.1, 0.1);
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

        // ======= Loop over hits, and fill corresponding histogram =======
        for (int ihit = 0; ihit < n_hits; ihit++) {
            int ix = hodoClusters.get(0).getIntVal(ihit);
            int iy = hodoClusters.get(1).getIntVal(ihit);
            int layer = hodoClusters.get(2).getIntVal(ihit);
            int hole = hodoClusters.get(3).getIntVal(ihit);
            double Energy = hodoClusters.get(4).getDoubleVal(ihit);
            double hit_time = hodoClusters.get(5).getDoubleVal(ihit);       
            HodoUtils.HodoTileIdentifier tile_id = hodoutils.new HodoTileIdentifier(ix, iy, layer);
            Cluster matchedEcalCluster=findECalHodoMatch(tile_id,ecalClusters);
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
                match = ecalCluster;
            }
        }
        return match;
    }
}
