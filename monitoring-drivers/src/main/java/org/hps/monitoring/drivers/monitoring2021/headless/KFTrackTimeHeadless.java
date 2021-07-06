package org.hps.monitoring.drivers.monitoring2021.headless;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.SvtPlotUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;

public class KFTrackTimeHeadless extends Driver {

    // private AIDAFrame plotterFrame;
    private String hitCollection = "StripClusterer_SiTrackerHitStrip1D";
    private String trackCollectionName = "KalmanFullTracks";   

    private static ITree tree = null;
    private final IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();   
    private IHistogramFactory histogramFactory = null;

    // Histogram Maps
    private static final Map<String, IHistogram1D> t0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackHitDt = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackHitT0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackT0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackTimeRange = new HashMap<String, IHistogram1D>();

    private static final Map<String, IHistogram2D> trackTrigTime = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackHitDtChan = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackHit2D = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackTimeMinMax = new HashMap<String, IHistogram2D>();

    private static final String subdetectorName = "Tracker";
    double minTime=-40;
    double maxTime=40;
    
    public void setTrackCollectionName(String name) {
        this.trackCollectionName = name;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        tree = analysisFactory.createTreeFactory().create();
        histogramFactory = analysisFactory.createHistogramFactory(tree);

        List<HpsSiSensor> sensors = detector.getSubdetector(subdetectorName).getDetectorElement()
                .findDescendants(HpsSiSensor.class);

          
        for (HpsSiSensor sensor : sensors) {
            t0.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - t0", 100, minTime, maxTime));
            trackHitT0.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - track hit t0", 100, minTime, maxTime));
            trackHitDt.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - track hit dt", 100, minTime, maxTime));

            trackHit2D.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - trigger phase vs dt", 80, -20, 20.0, 6, 0, 24.0));
            trackHitDtChan.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - dt vs position", 200, -20, 20, 50, -20, 20.0));         
        }

        trackT0.put("Top",
                histogramFactory.createHistogram1D("Top Track Time", 80, -40, 40.0));
 
        trackT0.put("Bottom",
                histogramFactory.createHistogram1D("Bottom Track Time", 80, -40, 40.0));
        trackTrigTime.put("Top",
                histogramFactory.createHistogram2D("Top Track Time vs. Trig Time", 80, -40, 40.0, 6, 0, 24));      
        trackTrigTime.put("Bottom",
                histogramFactory.createHistogram2D("Bottom Track Time vs. Trig Time", 80, -40, 40.0, 6, 0, 24));       
        trackTimeRange.put("Top",
                histogramFactory.createHistogram1D("Top Track Time Range", 75, 0, 30.0));      
        trackTimeRange.put("Bottom",
                histogramFactory.createHistogram1D("Bottom Track Time Range", 75, 0, 30.0));   
        trackTimeMinMax.put("Top",
                histogramFactory.createHistogram2D("Top Earliest vs Latest Track Hit Times", 80, -25, 25.0, 80, -25, 25.0));       
        trackTimeMinMax.put("Bottom",
                histogramFactory.createHistogram2D("Bottom Earliest vs Latest Track Hit Times", 80, -25, 25.0, 80, -25, 25.0));  
    }

    public void setHitCollection(String hitCollection) {
        this.hitCollection = hitCollection;
    }

    @Override
    public void process(EventHeader event) {
        int trigTime = (int) (event.getTimeStamp() % 24);

        // ===> IIdentifierHelper helper = SvtUtils.getInstance().getHelper();
        List<SiTrackerHitStrip1D> hits = event.get(SiTrackerHitStrip1D.class, hitCollection);
        for (SiTrackerHitStrip1D hit : hits) {           
            SiSensor sensor = hit.getSensor();
            t0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime());

        }
        //

        List<Track> tracks = event.get(Track.class, trackCollectionName);
        for (Track track : tracks) {          
            String moduleName = "Top";
            if (track.getTrackerHits().get(0).getPosition()[2] < 0) {           
                moduleName = "Bottom";              
            }
            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;
            int hitCount = 0;
            double trackTime = 0;
            for (TrackerHit hitTH: track.getTrackerHits()){
                SiTrackerHitStrip1D hit=(SiTrackerHitStrip1D) hitTH;
                SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                trackHitT0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime());
                trackTime += hit.getTime();
                hitCount++;
                if (hit.getTime() > maxTime)
                    maxTime = hit.getTime();
                if (hit.getTime() < minTime)
                    minTime = hit.getTime();
            }
            trackTimeMinMax.get(moduleName).fill(minTime, maxTime);
            trackTimeRange.get(moduleName).fill(maxTime - minTime);
            trackTime /= hitCount;
            trackT0.get(moduleName).fill(trackTime);
            trackTrigTime.get(moduleName).fill(trackTime, trigTime);

            for (TrackerHit hitTH: track.getTrackerHits()){
                SiTrackerHitStrip1D hit=(SiTrackerHitStrip1D) hitTH;            
                SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                trackHitDt.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime() - trackTime);
                trackHit2D.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime() - trackTime, event.getTimeStamp() % 24);
                trackHitDtChan.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getMeasuredCoordinate().x(), hit.getTime() - trackTime);
            }
        }
    }

    @Override
    public void endOfData() {
        // plotterFrame.dispose();
    }   

}
