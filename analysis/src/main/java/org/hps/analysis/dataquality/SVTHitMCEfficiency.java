package org.hps.analysis.dataquality;

import hep.aida.IHistogramFactory;
import hep.aida.IProfile1D;
import java.util.List;
import java.util.Set;
import org.hps.recon.tracking.FittedRawTrackerHit;
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
 * DQM driver for the monte carlo SVT hit efficiency
 * April 29 -- first pass, makes the SimTrackerHits-->SiClusters efficiency vs position (with a settable t0 cut)
 * @author mgraham on April 29, 2014
 */
// TODO: Add HelicalTrackHit efficiency...this should include the fitted hit cuts (t0 & chi^2) automatically since that where the cut is applied
// TODO: Add some quantities for DQM monitoring:  e.g. <efficiency>, probably within first 1 cm or so.   
public class SVTHitMCEfficiency extends DataQualityMonitor {

    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "TrackerHits";
    private String siClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String svtTrueHitRelationName =  "SVTTrueHitRelations";
    private String trackerName = "Tracker";
    private Detector detector = null;
    private double t0Cut=16.0;
    private static final String nameStrip = "Tracker_TestRunModule_";
    private List<SiSensor> sensors;

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setT0Cut(double cut){
        this.t0Cut=cut;
    }
    
    @Override
    protected void detectorChanged(Detector detector) {
        this.detector = detector;
        aida.tree().cd("/");
        IHistogramFactory hf = aida.histogramFactory();


        // Make a list of SiSensors in the SVT.
        sensors = this.detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        // Setup the efficiency plots.
        //currently, just the Si cluster efficiency
        aida.tree().cd("/");
        for (int kk = 1; kk < 13; kk++) {
            IProfile1D clEffic = createLayerPlot("clusterEfficiency", kk, 50, 0, 25.);
        }
    }

    @Override
    public void process(EventHeader event) {

        aida.tree().cd("/");

        //make sure the required collections exist
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName))
            return;
        if (!event.hasCollection(FittedRawTrackerHit.class, fittedTrackerHitCollectionName))
            return;
       
        if (!event.hasCollection(SiTrackerHitStrip1D.class, siClusterCollectionName))
            return;

        if (!event.hasCollection(SimTrackerHit.class, trackerHitCollectionName))
            return;
        
           if (!event.hasCollection(LCRelation.class, svtTrueHitRelationName))
            return;
       
        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        //  Get the collections of SimTrackerHits
        List<List<SimTrackerHit>> simcols = event.get(SimTrackerHit.class);
        //  Loop over the SimTrackerHits and fill in the relational table
        for (List<SimTrackerHit> simlist : simcols) {
            for (SimTrackerHit simhit : simlist) {
                if (simhit.getMCParticle() != null)
                    mcHittomcP.add(simhit, simhit.getMCParticle());
            }
        }
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class,svtTrueHitRelationName)) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class,svtTrueHitRelationName);
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
            }
        }
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, trackerHitCollectionName);
        // make relational table for strip clusters to mc particle
        List<SiTrackerHitStrip1D> siClusters = event.get(SiTrackerHitStrip1D.class, siClusterCollectionName);
        RelationalTable clustertosimhit = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (SiTrackerHit cluster : siClusters) {
            List<RawTrackerHit> rawHits = cluster.getRawHits();
            for (RawTrackerHit rth : rawHits) {
                Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
                if (simTrackerHits != null)
                    for (SimTrackerHit simhit : simTrackerHits) {
                        clustertosimhit.add(cluster, simhit);
                    }
            }
        }
//relational tables from mc particle to raw and fitted tracker hits
        RelationalTable fittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<FittedRawTrackerHit> fittedTrackerHits = event.get(FittedRawTrackerHit.class, fittedTrackerHitCollectionName);
        for (FittedRawTrackerHit hit : fittedTrackerHits) {
            RawTrackerHit rth = hit.getRawTrackerHit();
            Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
            if (simTrackerHits != null)
                for (SimTrackerHit simhit : simTrackerHits) {
                    if (simhit.getMCParticle() != null)
                        fittomc.add(hit, simhit.getMCParticle());
                }
        }

        for (SimTrackerHit simhit : simHits) {
            double wgt = 0.0;
            Set<SiTrackerHitStrip1D> clusters = clustertosimhit.allTo(simhit);
            if (clusters != null) {
                for (SiTrackerHitStrip1D clust : clusters) {
                    if (Math.abs(clust.getTime()) < t0Cut)
                        wgt = 1.0;
                }
            }
            getLayerPlot("clusterEfficiency", simhit.getLayer()).fill(Math.abs(simhit.getPoint()[1]), wgt);
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


}
