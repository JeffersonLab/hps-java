package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IProfile1D;
import hep.aida.IProfile2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;

/**
 * DQM driver for the monte carlo SVT hit efficiency April 29 -- first pass,
 * makes the SimTrackerHits-->SiClusters efficiency vs position (with a settable
 * t0 cut)
 *
 * @author mgraham on April 29, 2014
 */
// TODO: Add HelicalTrackHit efficiency...this should include the fitted hit cuts (t0 & chi^2) automatically since that where the cut is applied
// TODO: Add some quantities for DQM monitoring:  e.g. <efficiency>, probably within first 1 cm or so.   
public class SVTHitMCEfficiency extends DataQualityMonitor {

    private final String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private final String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private final String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private final String trackerHitCollectionName = "TrackerHits";
    private final String siClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private final String svtTrueHitRelationName = "SVTTrueHitRelations";
    private final String trackerName = "Tracker";
    private Detector detector = null;
    private double t0Cut = 16.0;
    private static final String nameStrip = "Tracker_TestRunModule_";
    private List<SiSensor> sensors;
    private final String plotDir = "SvtHitMCEfficiency/";
    private Map<String, Double> avgClusterEffMap;
    private Map<String, String> avgClusterEffNames;

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setT0Cut(double cut) {
        this.t0Cut = cut;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        this.detector = detector;
        aida.tree().cd("/");

        // Make a list of SiSensors in the SVT.
        sensors = this.detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        // Setup the efficiency plots.
        //currently, just the Si cluster efficiency
        aida.tree().cd("/");
        for (int kk = 1; kk < 13; kk++) {
            createLayerPlot(plotDir + "clusterEfficiency", kk, 50, -40, 40.);
            createLayerPlot(plotDir + "readoutEfficiency", kk, 50, -40, 40.);
            createLayerPlot(plotDir + "rthToClusterEfficiency", kk, 50, -40, 40.);
            createLayerPlot2D(plotDir + "clusterEfficiency2D", kk, 50, -40, 40., 16, 0.5, 16.5);
            createLayerPlot2D(plotDir + "rthToClusterEfficiency2D", kk, 50, -40, 40., 16, 0.5, 16.5);
            createLayerPlot2D(plotDir + "allFits", kk, 200, -100, 100, 100, 0, 20000);
//            createLayerPlot2D(plotDir + "toogoodFits", kk, 200, -100, 100, 100, 0, 20000);
//            createLayerPlot2D(plotDir + "goodFits", kk, 200, -100, 100, 100, 0, 20000);
//            createLayerPlot2D(plotDir + "badFits", kk, 200, -100, 100, 100, 0, 20000);
            createLayerPlot2D(plotDir + "fitT0ChiProb", kk, 200, -100, 100, 100, 0, 1.0);
            createLayerPlot2D(plotDir + "fitAmpChiProb", kk, 200, 0, 20000, 100, 0, 1.0);
            createLayerPlot1D(plotDir + "signalClusterT0", kk, 500, -100, 100);
            createLayerPlot2D(plotDir + "badClusterFits", kk, 200, -100, 100, 100, 0, 20000);
        }
        resetEfficiencyMap();
    }

    @Override
    public void process(EventHeader event) {

        aida.tree().cd("/");

        //make sure the required collections exist
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            return;
        }
        if (!event.hasCollection(FittedRawTrackerHit.class, fittedTrackerHitCollectionName)) {
            return;
        }

        if (!event.hasCollection(SiTrackerHitStrip1D.class, siClusterCollectionName)) {
            return;
        }

        if (!event.hasCollection(SimTrackerHit.class, trackerHitCollectionName)) {
            return;
        }

        if (!event.hasCollection(LCRelation.class, svtTrueHitRelationName)) {
            return;
        }

        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        //  Get the collections of SimTrackerHits
        List<List<SimTrackerHit>> simcols = event.get(SimTrackerHit.class);
        //  Loop over the SimTrackerHits and fill in the relational table
        for (List<SimTrackerHit> simlist : simcols) {
            for (SimTrackerHit simhit : simlist) {
                if (simhit.getMCParticle() != null) {
                    mcHittomcP.add(simhit, simhit.getMCParticle());
                }
            }
        }

        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trueHitRelations = event.get(LCRelation.class, svtTrueHitRelationName);
        for (LCRelation relation : trueHitRelations) {
            if (relation.getFrom() != null && relation.getTo() != null) {
                rawtomc.add(relation.getFrom(), relation.getTo());
            }
        }

        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, trackerHitCollectionName);
        // make relational table for strip clusters to mc particle
        List<SiTrackerHitStrip1D> siClusters = event.get(SiTrackerHitStrip1D.class, siClusterCollectionName);
        RelationalTable clustertosimhit = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (SiTrackerHit cluster : siClusters) {
            for (RawTrackerHit rth : cluster.getRawHits()) {
                Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
                if (simTrackerHits != null) {
                    for (SimTrackerHit simhit : simTrackerHits) {
                        if (simhit != null) {
                            clustertosimhit.add(cluster, simhit);
                        }
                    }
                }
            }
        }

        //relational tables from raw and fitted tracker hits to sim hit
        RelationalTable rthtofit = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        RelationalTable fittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<FittedRawTrackerHit> fittedTrackerHits = event.get(FittedRawTrackerHit.class, fittedTrackerHitCollectionName);
        for (FittedRawTrackerHit hit : fittedTrackerHits) {
            ShapeFitParameters oldfit = (ShapeFitParameters) rthtofit.to(hit.getRawTrackerHit());
            if (oldfit == null || Math.abs(oldfit.getT0()) > Math.abs(hit.getT0())) {
                rthtofit.add(hit.getRawTrackerHit(), hit.getShapeFitParameters());
            }
            Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(hit.getRawTrackerHit());
            for (SimTrackerHit simhit : simTrackerHits) {
                fittomc.add(hit, simhit);
            }
        }

        for (SimTrackerHit simhit : simHits) {
            Set<FittedRawTrackerHit> fittedRTH = fittomc.allTo(simhit);
            FittedRawTrackerHit signalHit = null;
            for (FittedRawTrackerHit frth : fittedRTH) {
                if (signalHit == null || Math.abs(frth.getT0()) < Math.abs(signalHit.getT0())) {
                    signalHit = frth;
                }
            }
            if (signalHit != null) {
//                System.out.format("chiprob %f, t0 %f, A %f\n", signalHit.getShapeFitParameters().getChiProb(), signalHit.getT0(), signalHit.getAmp());
                getLayerPlot2D(plotDir + "allFits", simhit.getLayer()).fill(signalHit.getT0(), signalHit.getAmp());
                getLayerPlot2D(plotDir + "fitT0ChiProb", simhit.getLayer()).fill(signalHit.getT0(), signalHit.getShapeFitParameters().getChiProb());
                getLayerPlot2D(plotDir + "fitAmpChiProb", simhit.getLayer()).fill(signalHit.getAmp(), signalHit.getShapeFitParameters().getChiProb());
//                if (signalHit.getShapeFitParameters().getChiProb() > 0.95) {
//                    getLayerPlot2D(plotDir + "toogoodFits", simhit.getLayer()).fill(signalHit.getT0(), signalHit.getAmp());
//                } else if (signalHit.getShapeFitParameters().getChiProb() < 0.05) {
//                    getLayerPlot2D(plotDir + "badFits", simhit.getLayer()).fill(signalHit.getT0(), signalHit.getAmp());
//                } else {
//                    getLayerPlot2D(plotDir + "goodFits", simhit.getLayer()).fill(signalHit.getT0(), signalHit.getAmp());
//                }
            }

            int gotCluster = 0;
            int[] gotClusterAtTime = new int[16];
            Set<SiTrackerHitStrip1D> clusters = clustertosimhit.allTo(simhit);
            if (clusters != null) {
                for (SiTrackerHitStrip1D clust : clusters) {
                    getLayerPlot1D(plotDir + "signalClusterT0", simhit.getLayer()).fill(clust.getTime());

                    for (int i = 0; i < 16; i++) {
                        if (Math.abs(clust.getTime()) < i + 1) {
                            gotClusterAtTime[i] = 1;
                        }
                    }
                    if (Math.abs(clust.getTime()) < t0Cut) {
                        gotCluster = 1;
                    } else {
                        for (RawTrackerHit rth : clust.getRawHits()) {
                            ShapeFitParameters fit = (ShapeFitParameters) rthtofit.to(rth);
                            getLayerPlot2D(plotDir + "badClusterFits", simhit.getLayer()).fill(fit.getT0(), fit.getAmp());
                        }
                    }

                }
            }
            Set<RawTrackerHit> rawhits = rawtomc.allTo(simhit);
            int gotRawHit = !rawhits.isEmpty() ? 1 : 0;

            double y = simhit.getDetectorElement().getGeometry().getGlobalToLocal().transformed(simhit.getPositionVec()).x() + 20.0 * Math.signum(simhit.getPoint()[1]) * (simhit.getLayer() % 2 == 0 ? -1 : 1);
            getLayerPlot(plotDir + "clusterEfficiency", simhit.getLayer()).fill(y, gotCluster);
            getLayerPlot(plotDir + "readoutEfficiency", simhit.getLayer()).fill(y, gotRawHit);
            if (gotRawHit == 1) {
                getLayerPlot(plotDir + "rthToClusterEfficiency", simhit.getLayer()).fill(y, gotCluster);
            }
            for (int i = 0; i < 16; i++) {
                getLayerPlot2D(plotDir + "clusterEfficiency2D", simhit.getLayer()).fill(y, i + 1, gotClusterAtTime[i]);
                if (gotRawHit == 1) {
                    getLayerPlot2D(plotDir + "rthToClusterEfficiency2D", simhit.getLayer()).fill(y, i + 1, gotClusterAtTime[i]);
                }
            }
        }
    }

    @Override
    public void fillEndOfRunPlots() {
        for (int kk = 1; kk < 13; kk++) {
            getMean2D(getLayerPlot2D(plotDir + "clusterEfficiency2D", kk));
            getMean2D(getLayerPlot2D(plotDir + "rthToClusterEfficiency2D", kk));
        }
    }

    @Override
    public void dumpDQMData() {
    }

    private void resetEfficiencyMap() {
        avgClusterEffMap = new HashMap<String, Double>();
        avgClusterEffNames = new HashMap<String, String>();
        for (SiSensor sensor : sensors) {
            String effName = "avgClusterEff_" + getNiceSensorName(sensor);
            avgClusterEffNames.put(sensor.getName(), effName);
        }
    }

    private String getNiceSensorName(SiSensor sensor) {
        return sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens");
    }

    private IProfile1D getLayerPlot(String prefix, int layer) {
        return aida.profile1D(prefix + "_layer" + layer);
    }

    private IProfile1D createLayerPlot(String prefix, int layer, int nchan, double min, double max) {
        return aida.profile1D(prefix + "_layer" + layer, nchan, min, max);
    }

    private void getMean2D(IHistogram2D hist2D) {
        int nx = hist2D.xAxis().bins();
        int ny = hist2D.yAxis().bins();
        double[][] means = new double[nx][ny];
        for (int ix = 0; ix < nx; ix++) {
            for (int iy = 0; iy < ny; iy++) {
                means[ix][iy] = hist2D.binHeight(ix, iy) / hist2D.binEntries(ix, iy);
            }
        }
        hist2D.reset();
        for (int ix = 0; ix < nx; ix++) {
            for (int iy = 0; iy < ny; iy++) {
                double x = hist2D.xAxis().binCenter(ix);
                double y = hist2D.yAxis().binCenter(iy);
                hist2D.fill(x, y, means[ix][iy]);
            }
        }
    }

    private IProfile2D getLayerProfile2D(String prefix, int layer) {
        return aida.profile2D(prefix + "_layer" + layer);
    }

    private IProfile2D createLayerProfile2D(String prefix, int layer, int nx, double minX, double maxX, int ny, double minY, double maxY) {
        return aida.profile2D(prefix + "_layer" + layer, nx, minX, maxX, ny, minY, maxY);
    }

    private IHistogram1D getLayerPlot1D(String prefix, int layer) {
        return aida.histogram1D(prefix + "_layer" + layer);
    }

    private IHistogram1D createLayerPlot1D(String prefix, int layer, int nchan, double min, double max) {
        return aida.histogram1D(prefix + "_layer" + layer, nchan, min, max);
    }

    private IHistogram2D getLayerPlot2D(String prefix, int layer) {
        return aida.histogram2D(prefix + "_layer" + layer);
    }

    private IHistogram2D createLayerPlot2D(String prefix, int layer, int nx, double minX, double maxX, int ny, double minY, double maxY) {
        return aida.histogram2D(prefix + "_layer" + layer, nx, minX, maxX, ny, minY, maxY);
    }

    @Override
    public void printDQMData() {
        for (SiSensor sensor : sensors) {
            System.out.println(avgClusterEffNames.get(sensor.getName()) + ":  " + avgClusterEffMap.get(sensor.getName()));
        }
    }

    @Override
    public void printDQMStrings() {
        for (SiSensor sensor : sensors) {
            System.out.println("ALTER TABLE dqm ADD " + avgClusterEffNames.get(sensor.getName()) + " double;");
        }
    }

}
