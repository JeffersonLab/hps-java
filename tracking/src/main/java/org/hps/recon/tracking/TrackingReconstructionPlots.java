package org.hps.recon.tracking;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
//import hep.aida.IProfile;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Analysis class to check recon.
 * 
 * @author phansson
 * @author mdiamond <mdiamond@slac.stanford.edu>
 * @version $id: 2.0 06/04/17$
 */
public class TrackingReconstructionPlots extends Driver {

    //static {
    //    hep.aida.jfree.AnalysisFactory.register();
    //}

    public AIDA aida;
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String stripClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private boolean doAmplitudePlots = false;
    private boolean doECalClusterPlots = false;
    private boolean doHitsOnTrackPlots = false;
    private boolean doResidualPlots = false;
    private boolean doMatchedClusterPlots = false;
    private boolean doElectronPositronPlots = false;
    private boolean doStripHitPlots = false;

    private String trackCollectionName = "MatchedTracks";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    IDDecoder dec;
    private Map<Track, Cluster> eCanditates;
    private Map<Track, Cluster> pCanditates;

    private String outputPlots = "TrackingRecoPlots.aida";

    ShaperFitAlgorithm _shaper = new DumbShaperFit();
    HelixConverter converter = new HelixConverter(0);
    private static Logger LOGGER = Logger.getLogger(TrackingReconstructionPlots.class.getName());
    private List<HpsSiSensor> sensors = new ArrayList<HpsSiSensor>();
    private double bfield;

    @Override
    protected void detectorChanged(Detector detector) {
        if (aida == null)
            aida = AIDA.defaultInstance();

        aida.tree().cd("/");
        for (HpsSiSensor s : detector.getDetectorElement().findDescendants(HpsSiSensor.class)) {
            if (s.getName().startsWith("module_") && s.getName().endsWith("sensor0")) {
                sensors.add(s);
            }
        }
        LOGGER.info("Found " + sensors.size() + " SiSensors.");

        Hep3Vector fieldInTracker = TrackUtils.getBField(detector);
        this.bfield = Math.abs(fieldInTracker.y());

        setupPlots();
    }

    public TrackingReconstructionPlots() {
        LOGGER.setLevel(Level.WARNING);
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setDoAmplitudePlots(boolean value) {
        this.doAmplitudePlots = value;
    }

    public void setDoECalClusterPlots(boolean value) {
        this.doECalClusterPlots = value;
    }

    public void setDoHitsOnTrackPlots(boolean value) {
        this.doHitsOnTrackPlots = value;
    }

    public void setDoResidualPlots(boolean value) {
        this.doResidualPlots = value;
    }

    public void setDoMatchedClusterPlots(boolean value) {
        this.doMatchedClusterPlots = value;
    }

    public void setDoElectronPositronPlots(boolean value) {
        this.doElectronPositronPlots = value;
    }

    private void doStripHits(List<TrackerHit> stripClusters, Track trk, RelationalTable trackDataTable) {
        Map<HpsSiSensor, Integer> stripHits = new HashMap<HpsSiSensor, Integer>();

        for (TrackerHit stripHit : stripClusters) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) stripHit.getRawHits().get(0)).getDetectorElement());

            int n;
            if (stripHits.containsKey(sensor)) {
                n = stripHits.get(sensor);
            } else {
                n = 0;
            }
            n++;
            stripHits.put(sensor, n);
        }

        for (Map.Entry<HpsSiSensor, Integer> sensor : stripHits.entrySet()) {
            aida.histogram1D(sensor.getKey().getName() + " strip hits").fill(stripHits.get(sensor.getKey()));
        }

        if (trackDataTable == null)
            return;
        GenericObject trackData = (GenericObject) trackDataTable.from(trk);
        if (trackData == null) {
            System.out.println("null TrackData for isolation");
            return;
        }

        int numIso = trackData.getNDouble();
        for (int i = 0; i < numIso; i++) {
            aida.histogram1D(String.format("Layer %d Isolation", i)).fill(trackData.getDoubleVal(i));
        }

    }

    private void doECalClusters(List<Cluster> clusters, boolean tracksPresent) {
        int nBotClusters = 0;
        int nTopClusters = 0;

        for (Cluster cluster : clusters) {
            // Get the ix and iy indices for the seed.
            //                final int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
            //                final int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");

            //System.out.println("cluser position = ("+cluster.getPosition()[0]+","+cluster.getPosition()[1]+") with energy = "+cluster.getEnergy());
            if (cluster.getPosition()[1] > 0) {
                nTopClusters++;
                //System.out.println("cl " + cluster.getPosition()[0] + " " + cluster.getPosition()[1] + "  ix  " + ix + " iy " + iy);
                aida.histogram2D("Top ECal Cluster Position").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                aida.histogram1D("Top ECal Cluster Energy").fill(cluster.getEnergy());
            }
            if (cluster.getPosition()[1] < 0) {
                nBotClusters++;
                aida.histogram2D("Bottom ECal Cluster Position").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                aida.histogram1D("Bottom ECal Cluster Energy").fill(cluster.getEnergy());
            }

            if (tracksPresent) {
                if (cluster.getPosition()[1] > 0) {
                    aida.histogram2D("Top ECal Cluster Position (>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                }
                if (cluster.getPosition()[1] < 0) {
                    aida.histogram2D("Bottom ECal Cluster Position (>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                }

                if (cluster.getEnergy() > 0.1) {
                    if (cluster.getPosition()[1] > 0) {
                        aida.histogram2D("Top ECal Cluster Position (E>0.1,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                        aida.histogram2D("Top ECal Cluster Position w_E (E>0.1,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1], cluster.getEnergy());
                    }
                    if (cluster.getPosition()[1] < 0) {
                        aida.histogram2D("Bottom ECal Cluster Position (E>0.1,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                        aida.histogram2D("Bottom ECal Cluster Position w_E (E>0.1,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1], cluster.getEnergy());
                    }
                }
            }

        }

        aida.histogram1D("Number of Clusters Top").fill(nTopClusters);
        aida.histogram1D("Number of Clusters Bot").fill(nBotClusters);
    }

    private void doBasicTracks(List<Track> tracks) {
        int ntracksTop = 0;
        int ntracksBot = 0;
        double momentum_param = 2.99792458e-04;

        aida.histogram1D("Tracks per Event", 3, 0, 3).fill(tracks.size());

        for (Track trk : tracks) {

            boolean isTop = false;
            if (trk.getTrackerHits().get(0).getPosition()[2] > 0) {
                isTop = true;
            }

            double pt = Math.abs((1 / trk.getTrackStates().get(0).getOmega()) * bfield * momentum_param);
            double pz = pt * Math.cos(trk.getTrackStates().get(0).getPhi());
            double px = pt * Math.sin(trk.getTrackStates().get(0).getPhi());
            double py = pt * trk.getTrackStates().get(0).getTanLambda();
            aida.histogram1D("Track Momentum (Pz)").fill(pz);
            aida.histogram1D("Track Momentum (Py)").fill(py);
            aida.histogram1D("Track Momentum (Px)").fill(px);
            aida.histogram1D("Track Chi2").fill(trk.getChi2());

            aida.histogram1D("Hits per Track").fill(trk.getTrackerHits().size());
            if (isTop)
                aida.histogram1D("Hits per Track Top").fill(trk.getTrackerHits().size());
            else
                aida.histogram1D("Hits per Track Bottom").fill(trk.getTrackerHits().size());

            aida.histogram1D("d0 ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
            aida.histogram1D("sinphi ").fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
            aida.histogram1D("omega ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
            aida.histogram1D("tan(lambda) ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
            aida.histogram1D("z0 ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));

            if (isTop) {
                aida.histogram1D("Top Track Momentum (Px)").fill(px);
                aida.histogram1D("Top Track Momentum (Py)").fill(py);
                aida.histogram1D("Top Track Momentum (Pz)").fill(pz);
                aida.histogram1D("Top Track Chi2").fill(trk.getChi2());

                aida.histogram1D("d0 Top").fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
                aida.histogram1D("sinphi Top").fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
                aida.histogram1D("omega Top").fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
                aida.histogram1D("tan(lambda) Top").fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
                aida.histogram1D("z0 Top").fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));
                ntracksTop++;
            } else {
                aida.histogram1D("Bottom Track Momentum (Px)").fill(px);
                aida.histogram1D("Bottom Track Momentum (Py)").fill(py);
                aida.histogram1D("Bottom Track Momentum (Pz)").fill(pz);
                aida.histogram1D("Bottom Track Chi2").fill(trk.getChi2());

                aida.histogram1D("d0 Bottom").fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
                aida.histogram1D("sinphi Bottom").fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
                aida.histogram1D("omega Bottom").fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
                aida.histogram1D("tan(lambda) Bottom").fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
                aida.histogram1D("z0 Bottom").fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));
                ntracksBot++;
            }
        }

        aida.histogram1D("Tracks per Event Bot").fill(ntracksBot);
        aida.histogram1D("Tracks per Event Top").fill(ntracksTop);
    }

    private void doHitsOnTrack(Track trk) {
        Map<HpsSiSensor, Integer> stripHitsOnTrack = new HashMap<HpsSiSensor, Integer>();
        List<TrackerHit> hitsOnTrack = trk.getTrackerHits();

        for (TrackerHit hit : hitsOnTrack) {

            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement());

            if (stripHitsOnTrack.containsKey(sensor)) {
                stripHitsOnTrack.put(sensor, stripHitsOnTrack.get(sensor) + 1);
            } else {
                stripHitsOnTrack.put(sensor, 1);
            }
        }

        for (Map.Entry<HpsSiSensor, Integer> sensor : stripHitsOnTrack.entrySet()) {
            aida.histogram1D(sensor.getKey().getName() + " strip hits on track").fill(stripHitsOnTrack.get(sensor.getKey()));
        }
    }

    private void doResiduals(List<LCRelation> fittedHits, Track trk, RelationalTable trackResTable) {
        GenericObject trackRes = (GenericObject) trackResTable.from(trk);
        if (trackRes == null) {
            //System.out.println("null TrackResidualsData");
            return;
        }

        int numX = trackRes.getNDouble();
        for (int i = 0; i < numX; i++) {
            int layer = trackRes.getIntVal(i);
            String modNum = "Layer Unknown ";
            if (layer % 2 == 1)
                modNum = String.format("Layer %d ", layer / 2 + 1);
            aida.histogram1D(modNum + "Residual Y(mm)").fill(trackRes.getFloatVal(i));
            aida.histogram1D(modNum + "Residual X(mm)").fill(trackRes.getDoubleVal(i));
        }
    }

    private void doAmplitude(List<LCRelation> fittedHits, Track trk) {
        List<TrackerHit> hitsOnTrack = trk.getTrackerHits();

        for (TrackerHit hit : hitsOnTrack) {
            double clusterSum = 0;
            double clusterT0 = 0;
            int nHitsCluster = 0;

            for (RawTrackerHit rawHit : (List<RawTrackerHit>) hit.getRawHits()) {

                for (LCRelation fittedHit : fittedHits) {
                    if (rawHit.equals((RawTrackerHit) fittedHit.getFrom())) {
                        double amp = FittedRawTrackerHit.getAmp(fittedHit);
                        double t0 = FittedRawTrackerHit.getT0(fittedHit);
                        //System.out.println("to="+t0 + " amp=" + amp);
                        aida.histogram1D("Amp (HitOnTrack)").fill(amp);
                        if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                            aida.histogram1D("Amp Pz>0.8 (HitOnTrack)").fill(amp);
                        }
                        aida.histogram1D("t0 (HitOnTrack)").fill(t0);
                        if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                            aida.histogram1D("t0 Pz>0.8 (HitOnTrack)").fill(t0);
                        }
                        clusterSum += amp;
                        clusterT0 += t0;
                        nHitsCluster++;
                    }
                }
            }

            aida.histogram1D("Hits in Cluster (HitOnTrack)").fill(nHitsCluster);
            aida.histogram1D("Cluster Amp (HitOnTrack)").fill(clusterSum);
            if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                aida.histogram1D("Cluster Amp Pz>0.8 (HitOnTrack)").fill(clusterSum);
            }
            if (nHitsCluster > 0) {
                aida.histogram1D("Cluster t0 (HitOnTrack)").fill(clusterT0 / nHitsCluster);
                if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                    aida.histogram1D("Cluster t0 Pz>0.8 (HitOnTrack)").fill(clusterT0 / nHitsCluster);
                }
            }

        }
    }

    private void doClustersOnTrack(Track trk, List<Cluster> clusters) {
        Hep3Vector posAtEcal = TrackUtils.getTrackPositionAtEcal(trk);
        Cluster clust = findClosestCluster(posAtEcal, clusters);
        if (clust == null)
            return;

        boolean isTop = false;
        if (trk.getTrackerHits().get(0).getPosition()[2] > 0) {
            isTop = true;
        }

        // track matching requirement
        if (Math.abs(posAtEcal.x() - clust.getPosition()[0]) < 30.0 && Math.abs(posAtEcal.y() - clust.getPosition()[1]) < 30.0) {

            if (doElectronPositronPlots) {
                if (trk.getCharge() < 0)
                    pCanditates.put(trk, clust);
                else
                    eCanditates.put(trk, clust);
            }

            posAtEcal = TrackUtils.extrapolateTrack(trk, clust.getPosition()[2]);//.positionAtEcal();

            aida.histogram2D("Energy Vs Momentum").fill(clust.getEnergy(), trk.getTrackStates().get(0).getMomentum()[0]);
            aida.histogram1D("Energy Over Momentum").fill(clust.getEnergy() / (trk.getTrackStates().get(0).getMomentum()[0]));
            aida.histogram1D("deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
            aida.histogram1D("deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
            aida.histogram2D("X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
            aida.histogram2D("Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());

            if (isTop) {
                aida.histogram2D("Top Energy Vs Momentum").fill(clust.getEnergy(), trk.getTrackStates().get(0).getMomentum()[0]);
                //                    aida.histogram2D("Top Energy Vs Momentum").fill(posAtEcal.y(), trk.getTrackStates().get(0).getMomentum()[0]);
                aida.histogram1D("Top Energy Over Momentum").fill(clust.getEnergy() / (trk.getTrackStates().get(0).getMomentum()[0]));
                aida.histogram1D("Top deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
                aida.histogram1D("Top deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
                aida.histogram2D("Top deltaX vs X").fill(clust.getPosition()[0], clust.getPosition()[0] - posAtEcal.x());
                aida.histogram2D("Top deltaY vs Y").fill(clust.getPosition()[1], clust.getPosition()[1] - posAtEcal.y());
                aida.histogram2D("Top X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
                aida.histogram2D("Top Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());
            } else {
                aida.histogram2D("Bottom Energy Vs Momentum").fill(clust.getEnergy(), trk.getTrackStates().get(0).getMomentum()[0]);
                aida.histogram1D("Bottom Energy Over Momentum").fill(clust.getEnergy() / (trk.getTrackStates().get(0).getMomentum()[0]));
                aida.histogram1D("Bottom deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
                aida.histogram1D("Bottom deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
                aida.histogram2D("Bottom deltaX vs X").fill(clust.getPosition()[0], clust.getPosition()[0] - posAtEcal.x());
                aida.histogram2D("Bottom deltaY vs Y").fill(clust.getPosition()[1], clust.getPosition()[1] - posAtEcal.y());
                aida.histogram2D("Bottom X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
                aida.histogram2D("Bottom Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());
            }

            aida.histogram1D("Tracks matched").fill(0);
            if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                aida.histogram1D("Tracks matched (Pz>0.8)").fill(0);
            }
            if (isTop) {
                aida.histogram1D("Tracks matched Top").fill(0);
                if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                    aida.histogram1D("Tracks matched Top (Pz>0.8)").fill(0);
                }
            } else {
                aida.histogram1D("Tracks matched Bottom").fill(0);
                if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                    aida.histogram1D("Tracks matched Bottom (Pz>0.8)").fill(0);
                }
            }
        }

        else {
            aida.histogram1D("Tracks matched").fill(1);
            if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                aida.histogram1D("Tracks matched (Pz>0.8)").fill(1);
            }

            if (isTop) {
                aida.histogram1D("Tracks matched Top").fill(1);
                if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                    aida.histogram1D("Tracks matched Top (Pz>0.8)").fill(1);
                }
            } else {
                aida.histogram1D("Tracks matched Bottom").fill(1);
                if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                    aida.histogram1D("Tracks matched Bottom (Pz>0.8)").fill(1);
                }
            }
        }
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(TrackerHit.class, helicalTrackHitCollectionName)) {
            System.out.println(helicalTrackHitCollectionName + " does not exist; skipping event");
            return;
        }

        if (!event.hasCollection(Track.class, trackCollectionName)) {
            System.out.println(trackCollectionName + " does not exist; skipping event");
            aida.histogram1D("Number Tracks/Event").fill(0);
            return;
        }
        List<Track> tracks = event.get(Track.class, trackCollectionName);

        List<Cluster> clusters = null;
        if (event.hasCollection(Cluster.class, ecalCollectionName)) {
            clusters = event.get(Cluster.class, ecalCollectionName);
        } else {
            doECalClusterPlots = false;
            doMatchedClusterPlots = false;
            doElectronPositronPlots = false;
        }

        List<LCRelation> fittedHits = null;
        if (event.hasCollection(LCRelation.class, "SVTFittedRawTrackerHits")) {
            fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
        } else {
            doAmplitudePlots = false;
            doResidualPlots = false;
        }

        List<TrackerHit> stripClusters = null;
        if (event.hasCollection(TrackerHit.class, stripClusterCollectionName)) {
            stripClusters = event.get(TrackerHit.class, stripClusterCollectionName);
        } else {
            doStripHitPlots = false;
        }

        //RelationalTable hitToRotatedTable = TrackUtils.getHitToRotatedTable(event);
        //RelationalTable hitToStripsTable = TrackUtils.getHitToStripsTable(event);

        RelationalTable trackResidualsTable = null;
        if (event.hasCollection(LCRelation.class, "TrackResidualsRelations")) {
            trackResidualsTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> trackresRelation = event.get(LCRelation.class, "TrackResidualsRelations");
            for (LCRelation relation : trackresRelation) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    trackResidualsTable.add(relation.getFrom(), relation.getTo());
                }
            }
        } else {
            doResidualPlots = false;
        }
        RelationalTable trackDataTable = null;
        if (event.hasCollection(LCRelation.class, "TrackDataRelations")) {
            trackDataTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> trackdataRelation = event.get(LCRelation.class, "TrackDataRelations");
            for (LCRelation relation : trackdataRelation) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    trackDataTable.add(relation.getFrom(), relation.getTo());
                }
            }
        }

        doBasicTracks(tracks);

        if (doECalClusterPlots)
            doECalClusters(clusters, tracks.size() > 0);

        if (doElectronPositronPlots) {
            eCanditates = new HashMap<Track, Cluster>();
            pCanditates = new HashMap<Track, Cluster>();
        }

        for (Track trk : tracks) {
            if (doStripHitPlots)
                doStripHits(stripClusters, trk, trackDataTable);

            if (doHitsOnTrackPlots)
                doHitsOnTrack(trk);

            if (doResidualPlots)
                doResiduals(fittedHits, trk, trackResidualsTable);

            if (doAmplitudePlots)
                doAmplitude(fittedHits, trk);

            if (doMatchedClusterPlots)
                doClustersOnTrack(trk, clusters);
        }

        if (doElectronPositronPlots)
            doElectronPositron();

    }

    private void doElectronPositron() {

        Map.Entry<Track, Cluster> ecand_highestP = null;
        double e_pmax = -1;
        Map.Entry<Track, Cluster> pcand_highestP = null;
        double p_pmax = -1;
        for (Map.Entry<Track, Cluster> ecand : eCanditates.entrySet()) {
            double p = getMomentum(ecand.getKey());
            aida.histogram1D("p(e-)").fill(p);
            if (ecand_highestP == null) {
                ecand_highestP = ecand;
                e_pmax = getMomentum(ecand_highestP.getKey());
            } else {
                if (p > e_pmax) {
                    ecand_highestP = ecand;
                    e_pmax = getMomentum(ecand_highestP.getKey());
                }
            }
        }

        for (Map.Entry<Track, Cluster> pcand : pCanditates.entrySet()) {
            double p = getMomentum(pcand.getKey());
            aida.histogram1D("p(e+)").fill(p);
            if (pcand_highestP == null) {
                pcand_highestP = pcand;
                p_pmax = getMomentum(pcand_highestP.getKey());
            } else {
                if (p > p_pmax) {
                    pcand_highestP = pcand;
                    p_pmax = getMomentum(pcand_highestP.getKey());
                }
            }
        }

        aida.histogram1D("n(e-)").fill(eCanditates.size());
        aida.histogram1D("n(e+)").fill(pCanditates.size());
        if (ecand_highestP != null) {
            aida.histogram1D("p(e-) max").fill(e_pmax);
        }
        if (pcand_highestP != null) {
            aida.histogram1D("p(e+) max").fill(p_pmax);
        }
        if (ecand_highestP != null && pcand_highestP != null) {
            aida.histogram2D("p(e-) vs p(e+) max").fill(e_pmax, p_pmax);
        }
    }

    private double getMomentum(Track trk) {
        double p = Math.sqrt(trk.getTrackStates().get(0).getMomentum()[0] * trk.getTrackStates().get(0).getMomentum()[0] + trk.getTrackStates().get(0).getMomentum()[1] * trk.getTrackStates().get(0).getMomentum()[1] + trk.getTrackStates().get(0).getMomentum()[2] * trk.getTrackStates().get(0).getMomentum()[2]);
        return p;
    }

    private Cluster findClosestCluster(Hep3Vector posonhelix, List<Cluster> clusters) {
        Cluster closest = null;
        double minDist = 9999;
        for (Cluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double clEne = cluster.getEnergy();
            double dist = Math.sqrt(Math.pow(clPos[0] - posonhelix.x(), 2) + Math.pow(clPos[1] - posonhelix.y(), 2)); //coordinates!!!
            if (dist < minDist && clEne > 0.4) {
                closest = cluster;
                minDist = dist;
            }
        }
        return closest;
    }

    @Override
    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void setupPlots() {

        // Basic tracks
        IHistogram1D trkPx = aida.histogram1D("Track Momentum (Px)", 100, -0.15, 0.15);
        IHistogram1D trkPy = aida.histogram1D("Track Momentum (Py)", 100, -0.15, 0.15);
        IHistogram1D trkPz = aida.histogram1D("Track Momentum (Pz)", 100, 0, 1.5);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 25, 0, 25.0);

        IHistogram1D toptrkPx = aida.histogram1D("Top Track Momentum (Px)", 100, -0.15, 0.15);
        IHistogram1D toptrkPy = aida.histogram1D("Top Track Momentum (Py)", 100, -0.15, 0.15);
        IHistogram1D toptrkPz = aida.histogram1D("Top Track Momentum (Pz)", 100, 0, 1.5);
        IHistogram1D toptrkChi2 = aida.histogram1D("Top Track Chi2", 25, 0, 25.0);

        IHistogram1D bottrkPx = aida.histogram1D("Bottom Track Momentum (Px)", 100, -0.15, 0.15);
        IHistogram1D bottrkPy = aida.histogram1D("Bottom Track Momentum (Py)", 100, -0.15, 0.15);
        IHistogram1D bottrkPz = aida.histogram1D("Bottom Track Momentum (Pz)", 100, 0, 1.5);
        IHistogram1D bottrkChi2 = aida.histogram1D("Bottom Track Chi2", 25, 0, 25.0);

        IHistogram1D trkd0 = aida.histogram1D("d0 ", 100, -10.0, 10.0);
        IHistogram1D trkphi = aida.histogram1D("sinphi ", 100, -0.2, 0.2);
        IHistogram1D trkomega = aida.histogram1D("omega ", 100, -0.001, 0.001);
        IHistogram1D trklam = aida.histogram1D("tan(lambda) ", 100, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D("z0 ", 100, -4.0, 4.0);

        IHistogram1D toptrkd0 = aida.histogram1D("d0 Top", 100, -10.0, 10.0);
        IHistogram1D toptrkphi = aida.histogram1D("sinphi Top", 100, -0.2, 0.2);
        IHistogram1D toptrkomega = aida.histogram1D("omega Top", 100, -0.001, 0.001);
        IHistogram1D toptrklam = aida.histogram1D("tan(lambda) Top", 100, -0.1, 0.1);
        IHistogram1D toptrkz0 = aida.histogram1D("z0 Top", 100, -4.0, 4.0);

        IHistogram1D bottrkd0 = aida.histogram1D("d0 Bottom", 100, -10.0, 10.0);
        IHistogram1D bottrkphi = aida.histogram1D("sinphi Bottom", 100, -0.2, 0.2);
        IHistogram1D bottrkomega = aida.histogram1D("omega Bottom", 100, -0.001, 0.001);
        IHistogram1D bottrklam = aida.histogram1D("tan(lambda) Bottom", 100, -0.1, 0.1);
        IHistogram1D bottrkz0 = aida.histogram1D("z0 Bottom", 100, -4.0, 4.0);

        IHistogram1D nTracksBot = aida.histogram1D("Tracks per Event Bot", 10, 0, 10);
        IHistogram1D nTracksTop = aida.histogram1D("Tracks per Event Top", 10, 0, 10);
        IHistogram1D nHitsTop = aida.histogram1D("Hits per Track Top", 4, 3, 7);
        IHistogram1D nHitsBot = aida.histogram1D("Hits per Track Bottom", 4, 3, 7);
        IHistogram1D nHits = aida.histogram1D("Hits per Track", 4, 3, 7);
        IHistogram1D nTracks = aida.histogram1D("Tracks per Event", 10, 0, 10);

        if (doStripHitPlots) {
            int i = 0;
            for (SiSensor sensor : sensors) {
                IHistogram1D resX = aida.histogram1D(sensor.getName() + " strip hits", 10, 0, 10);
                i++;
            }

            for (i = 0; i < 12; i++) {
                IHistogram1D resX = aida.histogram1D(String.format("Layer %d Isolation", i), 50, 0, 5);
            }
        }

        if (doAmplitudePlots) {
            IHistogram1D nHitsCluster = aida.histogram1D("Hits in Cluster (HitOnTrack)", 4, 0, 4);

            IHistogram1D amp = aida.histogram1D("Amp (HitOnTrack)", 50, 0, 5000);
            IHistogram1D ampcl = aida.histogram1D("Cluster Amp (HitOnTrack)", 50, 0, 5000);
            IHistogram1D amp2 = aida.histogram1D("Amp Pz>0.8 (HitOnTrack)", 50, 0, 5000);
            IHistogram1D ampcl2 = aida.histogram1D("Cluster Amp Pz>0.8 (HitOnTrack)", 50, 0, 5000);

            IHistogram1D t0 = aida.histogram1D("t0 (HitOnTrack)", 50, -100, 100);
            IHistogram1D t0cl = aida.histogram1D("Cluster t0 (HitOnTrack)", 50, -100, 100);
            IHistogram1D t02 = aida.histogram1D("t0 Pz>0.8 (HitOnTrack)", 50, -100, 100);
            IHistogram1D t0cl2 = aida.histogram1D("Cluster t0 Pz>0.8 (HitOnTrack)", 50, -100, 100);
        }

        if (doResidualPlots) {

            IHistogram1D mod1ResX = aida.histogram1D("Layer 1 Residual X(mm)", 25, -1, 1);
            IHistogram1D mod1ResY = aida.histogram1D("Layer 1 Residual Y(mm)", 25, -0.04, 0.04);

            IHistogram1D mod2ResX = aida.histogram1D("Layer 2 Residual X(mm)", 25, -2, 2);
            IHistogram1D mod2ResY = aida.histogram1D("Layer 2 Residual Y(mm)", 25, -1, 1);

            IHistogram1D mod3ResX = aida.histogram1D("Layer 3 Residual X(mm)", 25, -2.5, 2.5);
            IHistogram1D mod3ResY = aida.histogram1D("Layer 3 Residual Y(mm)", 25, -1.5, 1.5);

            IHistogram1D mod4ResX = aida.histogram1D("Layer 4 Residual X(mm)", 25, -3.0, 3.0);
            IHistogram1D mod4ResY = aida.histogram1D("Layer 4 Residual Y(mm)", 25, -2, 2);

            IHistogram1D mod5ResX = aida.histogram1D("Layer 5 Residual X(mm)", 25, -4, 4);
            IHistogram1D mod5ResY = aida.histogram1D("Layer 5 Residual Y(mm)", 25, -3, 3);

            IHistogram1D mod6ResX = aida.histogram1D("Layer 6 Residual X(mm)", 25, -5, 5);
            IHistogram1D mod6ResY = aida.histogram1D("Layer 6 Residual Y(mm)", 25, -3, 3);
        }

        if (doMatchedClusterPlots) {

            IHistogram2D eVsP = aida.histogram2D("Energy Vs Momentum", 50, 0, 0.50, 50, 0, 1.5);
            IHistogram1D eOverP = aida.histogram1D("Energy Over Momentum", 50, 0, 2);

            IHistogram1D distX = aida.histogram1D("deltaX", 50, -100, 100);
            IHistogram1D distY = aida.histogram1D("deltaY", 50, -40, 40);

            IHistogram2D xEcalVsTrk = aida.histogram2D("X ECal Vs Track", 100, -400, 400, 100, -400, 400);
            IHistogram2D yEcalVsTrk = aida.histogram2D("Y ECal Vs Track", 100, -100, 100, 100, -100, 100);

            IHistogram2D topeVsP = aida.histogram2D("Top Energy Vs Momentum", 50, 0, 0.500, 50, 0, 1.5);
            IHistogram1D topeOverP = aida.histogram1D("Top Energy Over Momentum", 50, 0, 2);

            IHistogram1D topdistX = aida.histogram1D("Top deltaX", 50, -100, 100);
            IHistogram1D topdistY = aida.histogram1D("Top deltaY", 50, -40, 40);

            IHistogram2D topxEcalVsTrk = aida.histogram2D("Top X ECal Vs Track", 100, -400, 400, 100, -100, 100);
            IHistogram2D topyEcalVsTrk = aida.histogram2D("Top Y ECal Vs Track", 100, 0, 100, 100, 0, 100);

            IHistogram2D BottomeVsP = aida.histogram2D("Bottom Energy Vs Momentum", 50, 0, 0.500, 50, 0, 1.5);
            IHistogram1D BottomeOverP = aida.histogram1D("Bottom Energy Over Momentum", 50, 0, 2);

            IHistogram1D BottomdistX = aida.histogram1D("Bottom deltaX", 50, -100, 100);
            IHistogram1D BottomdistY = aida.histogram1D("Bottom deltaY", 50, -40, 40);

            IHistogram2D BottomxEcalVsTrk = aida.histogram2D("Bottom X ECal Vs Track", 100, -400, 400, 100, -400, 400);
            IHistogram2D BottomyEcalVsTrk = aida.histogram2D("Bottom Y ECal Vs Track", 100, -100, 0, 100, -100, 0);

            IHistogram2D topdistXvsX = aida.histogram2D("Top deltaX vs X", 51, -400, 400, 25, -100, 100);
            IHistogram2D topdistYvsY = aida.histogram2D("Top deltaY vs Y", 51, 0, 100, 25, -40, 40);

            IHistogram2D botdistXvsX = aida.histogram2D("Bottom deltaX vs X", 51, -400, 400, 25, -100, 100);
            IHistogram2D botdistYvsY = aida.histogram2D("Bottom deltaY vs Y", 51, -100, 0, 25, -40, 40);

            IHistogram1D trackmatchN = aida.histogram1D("Tracks matched", 3, 0, 3);
            IHistogram1D toptrackmatchN = aida.histogram1D("Tracks matched Top", 3, 0, 3);
            IHistogram1D bottrackmatchN = aida.histogram1D("Tracks matched Bottom", 3, 0, 3);
            IHistogram1D trackmatchN2 = aida.histogram1D("Tracks matched (Pz>0.8)", 3, 0, 3);
            IHistogram1D toptrackmatchN2 = aida.histogram1D("Tracks matched Top (Pz>0.8)", 3, 0, 3);
            IHistogram1D bottrackmatchN2 = aida.histogram1D("Tracks matched Bottom (Pz>0.8)", 3, 0, 3);
        }

        if (doElectronPositronPlots) {
            IHistogram2D trackPCorr = aida.histogram2D("p(e-) vs p(e+) max", 25, 0, 1.2, 25, 0, 1.2);
            IHistogram1D ne = aida.histogram1D("n(e-)", 3, 0, 3);
            IHistogram1D np = aida.histogram1D("n(e+)", 3, 0, 3);
            IHistogram1D pem = aida.histogram1D("p(e-) max", 25, 0, 1.5);
            IHistogram1D pe = aida.histogram1D("p(e-)", 25, 0, 1.5);
            IHistogram1D ppm = aida.histogram1D("p(e+) max", 25, 0, 1.5);
            IHistogram1D pp = aida.histogram1D("p(e+)", 25, 0, 1.5);
        }

        if (doECalClusterPlots) {
            IHistogram2D topECal = aida.histogram2D("Top ECal Cluster Position", 50, -400, 400, 10, 0, 100);
            IHistogram2D botECal = aida.histogram2D("Bottom ECal Cluster Position", 50, -400, 400, 10, -100, 0);
            IHistogram2D topECal1 = aida.histogram2D("Top ECal Cluster Position (>0 tracks)", 50, -400, 400, 10, 0, 100);
            IHistogram2D botECal1 = aida.histogram2D("Bottom ECal Cluster Position (>0 tracks)", 50, -400, 400, 10, -100, 0);
            IHistogram2D topECal2 = aida.histogram2D("Top ECal Cluster Position (E>0.1,>0 tracks)", 50, -400, 400, 10, 0, 100);
            IHistogram2D botECal2 = aida.histogram2D("Bottom ECal Cluster Position (E>0.1,>0 tracks)", 50, -400, 400, 10, -100, 0);
            IHistogram2D topECal3 = aida.histogram2D("Top ECal Cluster Position w_E (E>0.1,>0 tracks)", 50, -400, 400, 10, 0, 100);
            IHistogram2D botECal3 = aida.histogram2D("Bottom ECal Cluster Position w_E (E>0.1,>0 tracks)", 50, -400, 400, 10, -100, 0);

            IHistogram1D topECalE = aida.histogram1D("Top ECal Cluster Energy", 50, 0, 2);
            IHistogram1D botECalE = aida.histogram1D("Bottom ECal Cluster Energy", 50, 0, 2);
            IHistogram1D topECalN = aida.histogram1D("Number of Clusters Top", 6, 0, 6);
            IHistogram1D botECalN = aida.histogram1D("Number of Clusters Bot", 6, 0, 6);

        }

        if (doHitsOnTrackPlots) {
            int i = 0;
            for (SiSensor sensor : sensors) {
                IHistogram1D resX = aida.histogram1D(sensor.getName() + " strip hits on track", 50, 0, 5);
                i++;
            }
        }
    }
}
