package org.hps.monitoring.svt;

import hep.aida.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.*;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.hps.util.Resettable;
import org.lcsim.hps.recon.tracking.SvtUtils;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTEventInfo extends Driver implements Resettable {

    private List<IPlotter> plotters = new ArrayList<IPlotter>();
    private AIDA aida = AIDA.defaultInstance();
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    IPlotter plotter;
    IPlotter plotter2;
    private String outputPlots = null;

    public SVTEventInfo() {
    }

    
    
    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");



        IAnalysisFactory fac = aida.analysisFactory();
        plotter = fac.createPlotterFactory().create("HPS SVT Events Plots");
        plotters.add(plotter);
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);



        plotter2 = fac.createPlotterFactory().create("HPS SVT Events Plots");
        plotters.add(plotter2);
        IPlotterStyle style2 = plotter2.style();
        style2.dataStyle().fillStyle().setColor("green");
        style2.dataStyle().errorBarStyle().setVisible(false);
        plotter2.createRegions(1, 2);
        style2.dataStyle().markerStyle().setSize(20);
        IHistogram1D nrawTopPlot = aida.histogram1D("Total Number of Raw Hits in Top Half", 20, 0, 19.0);
        IHistogram1D nrawBottomPlot = aida.histogram1D("Total Number of Raw Hits in Bottom Half", 20, 0, 19.0);
        IHistogram1D nlayersTopPlot = aida.histogram1D("Number of Layers Hit in Top Half", 11, 0, 10.0);
        IHistogram1D nlayersBottomPlot = aida.histogram1D("Number of Layers Hit in Bottom Half", 11, 0, 10.0);

        IProfile avgLayersTopPlot = aida.profile1D("Number of Hits per layer in Top Half", 10, 1, 11);
        IProfile avgLayersBottomPlot = aida.profile1D("Number of Hits per layer in Bottom Half", 10, 1, 11);

        plotter.region(0).plot(nrawTopPlot);
        plotter.region(1).plot(nrawBottomPlot);
        plotter.region(2).plot(nlayersTopPlot);
        plotter.region(3).plot(nlayersBottomPlot);

        plotter2.region(0).plot(avgLayersTopPlot);
        plotter2.region(1).plot(avgLayersBottomPlot);

        plotter.show();
        plotter2.show();
    }

    
    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }
    
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            int totalTopHit = 0;
            int totalBotHit = 0;

            int nlayersTopHit = 0;
            int nlayersBotHit = 0;
            int[] layersTop = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int[] layersBot = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            for (RawTrackerHit hit : rawHits) {
                int layerNumber = hit.getLayerNumber();
                boolean isTop = isHitOnTop(hit);
                if (isTop) {
                    totalTopHit++;
                    layersTop[layerNumber - 1]++;
                } else {
                    totalBotHit++;
                    layersBot[layerNumber - 1]++;
                }

            }

            System.out.println(totalTopHit);
            aida.histogram1D("Total Number of Raw Hits in Top Half").fill(totalTopHit);
            aida.histogram1D("Total Number of Raw Hits in Bottom Half").fill(totalBotHit);

            for (int i = 0; i < 10; i++) {
                if (layersTop[i] > 0)
                    nlayersTopHit++;
                if (layersBot[i] > 0)
                    nlayersBotHit++;
                aida.profile1D("Number of Hits per layer in Top Half").fill(i + 1, layersTop[i]);
                aida.profile1D("Number of Hits per layer in Bottom Half").fill(i + 1, layersBot[i]);
            }

            aida.histogram1D("Number of Layers Hit in Top Half").fill(nlayersTopHit);
            aida.histogram1D("Number of Layers Hit in Bottom Half").fill(nlayersBotHit);
        } else {
            aida.histogram1D("Total Number of Raw Hits in Top Half").fill(0);
            aida.histogram1D("Total Number of Raw Hits in Bottom Half").fill(0);
            for (int i = 0; i < 10; i++) {

                aida.profile1D("Number of Hits per layer in Top Half").fill(i + 1, 0);
                aida.profile1D("Number of Hits per layer in Bottom Half").fill(i + 1, 0);
            }
            aida.histogram1D("Number of Layers Hit in Top Half").fill(0);
            aida.histogram1D("Number of Layers Hit in Bottom Half").fill(0);
        }


    }

    public void endOfData() {
        if (plotter != null) {
            plotter.hide();
        }
        if (plotter2 != null) {
            plotter2.hide();
        }

     if (outputPlots != null)
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    private boolean isHitOnTop(RawTrackerHit hit) {
        SiSensor sensor = (SiSensor) hit.getDetectorElement();
        IIdentifier id = hit.getIdentifier();
        SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();

        ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
        SiSensorElectrodes electrodes = ((SiSensor) hit.getDetectorElement()).getReadoutElectrodes(carrier);
        if(!SvtUtils.getInstance().isTopLayer(sensor))
            return false;
        return true;
    }

    @Override
    public void reset() {
        aida.histogram1D("Total Number of Raw Hits in Top Half").reset();
        aida.histogram1D("Total Number of Raw Hits in Bottom Half").reset();
        aida.histogram1D("Number of Layers Hit in Top Half").reset();
        aida.profile1D("Number of Hits per layer in Top Half").reset();
        aida.histogram1D("Number of Layers Hit in Bottom Half").reset();
        aida.profile1D("Number of Hits per layer in Top Half").reset();
        aida.profile1D("Number of Hits per layer in Bottom Half").reset();
    }
}
