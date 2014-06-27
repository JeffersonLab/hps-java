package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import java.util.List;
import org.hps.recon.tracking.TrackResidualsData;
import org.hps.recon.tracking.TrackTimeData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;

/**
 * DQM driver for  track residuals in position and time
 *
 * @author mgraham on June 5, 2014
 */
// TODO:  Add some quantities for DQM monitoring: 
public class TrackingResiduals extends DataQualityMonitor {

    // Collection Names
    String trackTimeDataCollectionName = "TrackTimeData";
    String trackResidualsCollectionName = "TrackResiduals";

    
    int nEvents = 0;
    
    private String plotDir = "TrackResiduals/";
    String[] trackingQuantNames = {};
    int nmodules = 6;

    @Override
    protected void detectorChanged(Detector detector) {
        
        aida.tree().cd("/");

        for (int i = 0; i < nmodules; i++) {
            IHistogram1D xresid = aida.histogram1D(plotDir + "Layer " + i + " x Residual", 50, -2.5, 2.5);
            IHistogram1D yresid = aida.histogram1D(plotDir + "Layer " + i + " y Residual", 50, -1, 1);
        }

        for (int i = 0; i < nmodules * 2; i++) {
            IHistogram1D tresid = aida.histogram1D(plotDir + "Half-Layer " + i + " t Residual", 50, -20, 20);
        }
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(GenericObject.class, trackTimeDataCollectionName))
            return;
        if (!event.hasCollection(GenericObject.class, trackResidualsCollectionName))
            return;
        nEvents++;
        List<GenericObject> trdList = event.get(GenericObject.class, trackResidualsCollectionName);
        for (GenericObject trd : trdList) {
            int nResid = trd.getNDouble();
            for (int i = 0; i < nResid; i++) {
                aida.histogram1D(plotDir + "Layer " + i + " x Residual").fill(trd.getDoubleVal(i));//x is the double value in the generic object
                aida.histogram1D(plotDir + "Layer " + i + " y Residual").fill(trd.getFloatVal(i));//y is the float value in the generic object
            }
        }

        List<GenericObject> ttdList = event.get(GenericObject.class, trackTimeDataCollectionName);
        for (GenericObject ttd : ttdList) {
            int nResid = ttd.getNDouble();
            for (int i = 0; i < nResid; i++)
                aida.histogram1D(plotDir + "Half-Layer " + i + " t Residual").fill(ttd.getDoubleVal(i));//x is the double value in the generic object               
        }
    }

    @Override
    public void calculateEndOfRunQuantities() {
//        monitoredQuantityMap.put(trackingQuantNames[0], (double) nTotTracks / nEvents);
    }

    @Override
    public void printDQMData() {
//        System.out.println("ReconMonitoring::printDQMData");
//        for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet())
//            System.out.println(entry.getKey() + " = " + entry.getValue());
//        System.out.println("*******************************");
    }

    @Override
    public void printDQMStrings() {
//        for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet())
//            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
    }

}
