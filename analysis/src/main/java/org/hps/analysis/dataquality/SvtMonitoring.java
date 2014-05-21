package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import org.lcsim.geometry.Detector;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;

/**
 *  DQM driver for the monte carlo for reconstructed track quantities
 *  plots things like occupancy, t0, amplitude, chi^2 (from APV25 sampling fit); each on a per/sensor basis
 * saves to DQM database:  <occupancy> 
 * @author mgraham on Mar 28, 2014
 */
//TODO:  add some more quantities to DQM database:  <t0> or <sigma>_t0 for intime events;  <chi^2>, <amplitude> etc
public class SvtMonitoring extends DataQualityMonitor {

    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private Detector detector = null;
    private IPlotter plotter;
    private String trackerName = "Tracker";
    private List<SiSensor> sensors;
    private Map<String, int[]> occupancyMap;
    private Map<String, Double> avgOccupancyMap;
    private Map<String, String> avgOccupancyNames;
    private int eventCountRaw = 0;
    private int eventCountFit = 0;
    private int eventCountCluster = 0;
    private static final String nameStrip = "Tracker_TestRunModule_";
    private static final int maxChannels = 640;
    private String plotDir="SvtMonitoring/";

    public void setRawTrackerHitCollectionName(String inputCollection) {
        this.rawTrackerHitCollectionName = inputCollection;
    }

    public void setFittedTrackerHitCollectionName(String inputCollection) {
        this.fittedTrackerHitCollectionName = inputCollection;
    }

    public void setTrackerHitCollectionName(String inputCollection) {
        this.trackerHitCollectionName = inputCollection;
    }

    protected void detectorChanged(Detector detector) {
        System.out.println("SvtMonitoring::detectorChanged  Setting up the plotter");
        this.detector = detector;
        aida.tree().cd("/");
      
        // Make a list of SiSensors in the SVT.
        sensors = this.detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        // Reset the data structure that keeps track of strip occupancies.
        resetOccupancyMap();
     
        // Setup the occupancy plots.
        aida.tree().cd("/");
        for (SiSensor sensor : sensors) {
            //IHistogram1D occupancyPlot = aida.histogram1D(sensor.getName().replaceAll("Tracker_TestRunModule_", ""), 640, 0, 639);
            IHistogram1D occupancyPlot = createSensorPlot(plotDir+"occupancy_",sensor, maxChannels, 0, maxChannels - 1);
            IHistogram1D t0Plot = createSensorPlot(plotDir+"t0_",sensor,50,-50.,50.);
            IHistogram1D amplitudePlot = createSensorPlot(plotDir+"amplitude_",sensor,50,0,2000);
            IHistogram1D chi2Plot = createSensorPlot(plotDir+"chi2_",sensor,50,0,25);
            occupancyPlot.reset();
        }

    }

    public SvtMonitoring() {
    }

    public void process(EventHeader event) {
        /*  increment the strip occupancy arrays */
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            for (RawTrackerHit hit : rawTrackerHits) {
                int[] strips = occupancyMap.get(hit.getDetectorElement().getName());
                strips[hit.getIdentifierFieldValue("strip")] += 1;
            }
            ++eventCountRaw;
        } else
            return; /* kick out of this if the even has none of these...*/
        /*  fill the FittedTrackerHit related histograms */
        if (event.hasCollection(FittedRawTrackerHit.class, fittedTrackerHitCollectionName)){
             List<FittedRawTrackerHit> fittedTrackerHits = event.get(FittedRawTrackerHit.class, fittedTrackerHitCollectionName);
             for(FittedRawTrackerHit hit: fittedTrackerHits){
                 String sensorName= hit.getRawTrackerHit().getDetectorElement().getName();
                 double t0=hit.getT0();
                 double amp=hit.getAmp();
                 double chi2=hit.getShapeFitParameters().getChiSq();
                 getSensorPlot(plotDir+"t0_",sensorName).fill(t0);
                 getSensorPlot(plotDir+"amplitude_",sensorName).fill(amp);  
                  getSensorPlot(plotDir+"chi2_",sensorName).fill(chi2); 
             }
             ++eventCountFit;
        } else
            return;
    }

    private IHistogram1D getSensorPlot( String prefix, SiSensor sensor) {
        return aida.histogram1D(prefix+sensor.getName());
    }
    
      private IHistogram1D getSensorPlot( String prefix, String sensorName) {
        return aida.histogram1D(prefix+sensorName);
    }

    private IHistogram1D createSensorPlot( String prefix,SiSensor sensor, int nchan, double min, double max) {
        IHistogram1D hist = aida.histogram1D(prefix+sensor.getName(),nchan,min,max);
        hist.setTitle(sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens"));
        return hist;
    }

    private void resetOccupancyMap() {
        occupancyMap = new HashMap<>();
        avgOccupancyMap = new HashMap<>();
        avgOccupancyNames = new HashMap<>();
        for (SiSensor sensor : sensors) {
            occupancyMap.put(sensor.getName(), new int[640]);
            avgOccupancyMap.put(sensor.getName(), -999.);
            String occName = "avgOcc_" + getNiceSensorName(sensor);
            avgOccupancyNames.put(sensor.getName(), occName);
        }
    }

    private String getNiceSensorName(SiSensor sensor) {
        return sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens");
    }

    public void reset() {
        eventCountRaw = 0;
        eventCountFit = 0;
        eventCountCluster = 0;
        resetOccupancyMap();
    }

    @Override
    public void fillEndOfRunPlots() {
        // Plot strip occupancies.
        System.out.println("SvtMonitoring::endOfData  filling occupancy plots");
        for (SiSensor sensor : sensors) {
            Double avg = 0.0;
            //IHistogram1D sensorHist = aida.histogram1D(sensor.getName());
            IHistogram1D sensorHist = getSensorPlot(plotDir+"occupancy_",sensor);
            sensorHist.reset();
            int[] strips = occupancyMap.get(sensor.getName());
            for (int i = 0; i < strips.length; i++) {
                double stripOccupancy = (double) strips[i] / (double) (eventCountRaw);
                if (stripOccupancy != 0)
                    sensorHist.fill(i, stripOccupancy);
                avg += stripOccupancy;
            }
        //do the end-of-run quantities here too since we've already done the loop.  
            avg /= strips.length;        
            avgOccupancyMap.put(sensor.getName(), avg);
        }
    }

    @Override
    public void dumpDQMData() {
        System.out.println("SvtMonitoring::endOfData filling DQM database");
        double s1occ = 0.99;
        String put = "update dqm SET avgOcc_T1=" + s1occ + " WHERE " + getRunRecoString();
//        manager.updateQuery(put);        
    }

    @Override
    public void printDQMData() {
        for (SiSensor sensor : sensors) {
            System.out.println(avgOccupancyNames.get(sensor.getName()) + ":  " + avgOccupancyMap.get(sensor.getName()));
        }
    }
      
      @Override
    public void printDQMStrings() {
        for (SiSensor sensor : sensors) {
            System.out.println(avgOccupancyNames.get(sensor.getName()));
        }
    }
}
