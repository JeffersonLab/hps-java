package org.hps.monitoring.drivers.svt;

import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ref.histogram.DataPoint;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


//===> import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
//===> import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class PedestalPlots extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private Map<SiSensor, IHistogram2D> hists;
    private Map<SiSensor, int[]> counts;
    private Map<SiSensor, double[]> means;
    private Map<SiSensor, double[]> sumsqs;
    private Map<SiSensor, IDataPointSet[]> plots;
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fitFile = null;
    private boolean plotTimeSeries = false;
    private static final String subdetectorName = "Tracker";
    
    
    public void setFitFile(String fitFile) {
        this.fitFile = fitFile;
    }

    public void setPlotTimeSeries(boolean plotTimeSeries) {
        this.plotTimeSeries = plotTimeSeries;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        aida.tree().cd("/");

        hists = new HashMap<SiSensor, IHistogram2D>();
        counts = new HashMap<SiSensor, int[]>();
        means = new HashMap<SiSensor, double[]>();
        sumsqs = new HashMap<SiSensor, double[]>();
        plots = new HashMap<SiSensor, IDataPointSet[]>();

        List<SiSensor> sensors = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(SiSensor.class);
        
        //===> for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {
        for (SiSensor sensor : sensors) {
            hists.put(sensor, aida.histogram2D(sensor.getName() + " sample 1 vs. ch", 640, -0.5, 639.5, 500, -500.0, 3000.0));
            if (plotTimeSeries) {
                counts.put(sensor, new int[640]);
                means.put(sensor, new double[640]);
                sumsqs.put(sensor, new double[640]);
                IDataPointSet[] plotArray = new IDataPointSet[640];
                plots.put(sensor, plotArray);
                for (int i = 0; i < 640; i++) {
                    plotArray[i] = aida.analysisFactory().createDataPointSetFactory(aida.tree()).create(sensor.getName() + ", channel " + i + " pedestal vs. event", 2);
                }
            }
        }



    }

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            // Get RawTrackerHit collection from event.
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

            for (RawTrackerHit hit : rawTrackerHits) {
                HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                int strip = hit.getIdentifierFieldValue("strip");
                double pedestal = sensor.getPedestal(strip, 0);
                //===> double pedestal = HPSSVTCalibrationConstants.getPedestal(sensor, strip);
                hists.get(sensor).fill(strip, hit.getADCValues()[0] - pedestal);

                if (plotTimeSeries) {

                    counts.get(sensor)[strip]++;
                    double delta = hit.getADCValues()[0] - pedestal - means.get(sensor)[strip];
                    means.get(sensor)[strip] += delta / counts.get(sensor)[strip];
                    sumsqs.get(sensor)[strip] += delta * (hit.getADCValues()[0] - pedestal - means.get(sensor)[strip]);

                    if (counts.get(sensor)[strip] >= 100) {
                        double[] data = {event.getEventNumber(), means.get(sensor)[strip]};
                        double[] errs = {0.0, Math.sqrt(sumsqs.get(sensor)[strip] / counts.get(sensor)[strip])};
                        IDataPoint point = new DataPoint(data, errs);
                        plots.get(sensor)[strip].addPoint(point);
                        counts.get(sensor)[strip] = 0;
                        means.get(sensor)[strip] = 0;
                        sumsqs.get(sensor)[strip] = 0;
                    }
                }
            }
        }
    }

    @Override
    public void endOfData() {
        if (fitFile == null) {
            return;
        }

        IFitter fitter = aida.analysisFactory().createFitFactory().createFitter("chi2");
//        fitter.setFitMethod("CleverChiSquared");
//        fitter.setFitMethod("binnedMaximumLikelihood");

        PrintWriter fitWriter = null;
        try {
            fitWriter = new PrintWriter(fitFile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PedestalPlots.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (SiSensor sensor : hists.keySet()) {
            fitWriter.println(sensor.getName());
            IHistogram2D hist = hists.get(sensor);
            IHistogram1D fit = aida.histogram1D("1D fit", hist.yAxis().bins(), hist.yAxis().lowerEdge(), hist.yAxis().upperEdge());
            for (int i = 0; i < 640; i++) {
                fitWriter.format("%d\t", i);
                for (int y = 0; y < hist.yAxis().bins(); y++) {
                    for (int j = 0; j < hist.binHeight(i, y); j++) {
                        fit.fill(hist.binMeanY(i, y));
                    }
                }
                fitWriter.format("%f\t%f\t%f\t", fit.sumBinHeights(), fit.mean(), fit.rms());
                if (fit.sumBinHeights() > 100) {
                    IFitResult result = fitter.fit(fit, "g");

                    if (result.isValid()) {
                        fitWriter.format("%f\t%f\t", result.fittedParameter("mean"), result.fittedParameter("sigma"));
                    }
                }
                fitWriter.println();
                fit.reset();
            }
            fitWriter.flush();
        }
        fitWriter.close();
        aida.tree().rm("1D fit");
    }
}
