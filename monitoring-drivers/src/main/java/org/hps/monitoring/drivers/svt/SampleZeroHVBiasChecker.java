package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.run.RunSpreadsheet.RunMap;
import org.hps.conditions.svt.SvtBiasConditionsLoader;
import org.hps.conditions.svt.SvtBiasMyaDumpReader;
import org.hps.conditions.svt.SvtBiasMyaDumpReader.SvtBiasRunRange;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.log.LogUtil;
import org.hps.monitoring.drivers.svt.SvtPlotUtils;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.HeadBankData;
import org.hps.util.BasicLogFormatter;


/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SampleZeroHVBiasChecker extends Driver {

    // Logger
    Logger logger = LogUtil.create(getName(), new BasicLogFormatter(), Level.INFO);
    static {
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    private static ITree tree;
    IHistogramFactory histogramFactory;
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();

    // Histogram maps
    IPlotter plotter1;
    IPlotter plotter2;
    IPlotter plotter3;
    IPlotter plotter4;
    

    List<HpsSiSensor> sensors;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadc;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoise;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoiseON;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoiseOFF;
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String triggerBankCollectionName ="TriggerBank";
    private String fitFile = null;
    private boolean plotTimeSeries = false;
    private static final String subdetectorName = "Tracker";
    List<SvtBiasRunRange> runRanges;
    SvtBiasRunRange runRange = null;
    private int eventRefreshRate;
    private int eventCount;
    private Date eventDate = null;

    public void setFitFile(String fitFile) {
        this.fitFile = fitFile;
    }

    public void setPlotTimeSeries(boolean plotTimeSeries) {
        this.plotTimeSeries = plotTimeSeries;
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    @Override
    protected void detectorChanged(Detector detector) {
   
	tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        hists_rawadc = new HashMap<HpsSiSensor, IHistogram1D>();
        hists_rawadcnoise = new HashMap<HpsSiSensor, IHistogram1D>();
        hists_rawadcnoiseON = new HashMap<HpsSiSensor, IHistogram1D>();
        hists_rawadcnoiseOFF = new HashMap<HpsSiSensor, IHistogram1D>();
        
        sensors = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(HpsSiSensor.class);

        plotter1 = plotterFactory.create("Pedestal subtracted zero Sample ADC");
        plotter1.createRegions(6, 6);
        plotter2 = plotterFactory.create("Pedestal subtracted zero Sample ADC MaxSample>4");
        plotter2.createRegions(6, 6);
        plotter3 = plotterFactory.create("Pedestal subtracted zero Sample ADC MaxSample>4 ON");
        plotter3.createRegions(6, 6);
        plotter4 = plotterFactory.create("Pedestal subtracted zero Sample ADC MaxSample>4 OFF");
        plotter4.createRegions(6, 6);

        for (HpsSiSensor sensor : sensors) {
            hists_rawadc.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " raw adc - ped", 100, -1000.0, 5000.0));
            plotter1.region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists_rawadc.get(sensor));
            hists_rawadcnoise.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " raw adc - ped maxSample>4", 100, -1000.0, 1000.0));
            plotter2.region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists_rawadcnoise.get(sensor));
            hists_rawadcnoiseON.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " raw adc - ped maxSample>4 ON", 100, -1000.0, 1000.0));
            plotter3.region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists_rawadcnoiseON.get(sensor));
            hists_rawadcnoiseOFF.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " raw adc - ped maxSample>4 OFF", 100, -1000.0, 1000.0));
            plotter4.region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists_rawadcnoiseOFF.get(sensor));
        }

        plotter1.show();
        plotter2.show();
        plotter3.show();
        plotter4.show();



        RunMap runmap = SvtBiasConditionsLoader.getRunMapFromSpreadSheet("/Users/phansson/work/HPS/software/kepler2/hps-java-sandbox/HPS_Runs_2015-SVT_timing_guesses_for_Jeremy.csv");            
        SvtBiasMyaDumpReader biasDumpReader = new SvtBiasMyaDumpReader("/Users/phansson/work/HPS/software/kepler2/hps-java-sandbox/biascrawling/svtbiasmon/SVT:bias:bot:20:v_sens.mya");
        //SvtBiasConditionsLoader.setTimeOffset(Calendar.)
        runRanges = SvtBiasConditionsLoader.getBiasRunRanges(runmap, biasDumpReader);
        logger.info("Print all " + runRanges.size() + " bias run ranges:");
        for(SvtBiasRunRange r : runRanges) {
            logger.info(r.toString());
        }


        
    }

    
    private Date getEventTimeStamp(EventHeader event) {
        List<GenericObject> intDataCollection = event.get(GenericObject.class, triggerBankCollectionName);
        for (GenericObject data : intDataCollection) {
            if (AbstractIntData.getTag(data) == HeadBankData.BANK_TAG) {
                Date date = HeadBankData.getDate(data);
                if (date != null) {
                    return date;
                } 
            }
        }
        return null;
    }
    
    
    @Override
    public void process(EventHeader event) {
        
        
        
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            // Get RawTrackerHit collection from event.
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            eventCount++;
            if(runRange==null) {
                for(SvtBiasRunRange r : runRanges) {
                    if (r.getRun().getRun()==event.getRunNumber()) {
                        runRange = r;
                    }
                }
            }
            for (RawTrackerHit hit : rawTrackerHits) {
                HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                int strip = hit.getIdentifierFieldValue("strip");
                double pedestal = sensor.getPedestal(strip, 0);
                hists_rawadc.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                
                int maxSample = 0;
                double maxSampleValue = 0;
                for(int s=0;s<6;++s) {
                    if(((double)hit.getADCValues()[s] - pedestal)>maxSampleValue) {
                        maxSample = s;
                        maxSampleValue =  ((double) hit.getADCValues()[s]) - pedestal;
                    }
                  }
                if(maxSample>=4) {
                    hists_rawadcnoise.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                    
                    Date newEventDate = getEventTimeStamp(event);
                    if(newEventDate!=null) {
                        eventDate = newEventDate;
                    }
                    if(eventDate!=null) {
                        boolean hvOn = runRange.getRanges().includes(eventDate);
                        logger.info("Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOn " + (hvOn?"YES":"NO"));
                        
                        if(hvOn) {
                            hists_rawadcnoiseON.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                        } else {
                            hists_rawadcnoiseOFF.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                        }
                    } else {
                        logger.warning("No eventDatae for run " + event.getRunNumber() + " Event " + event.getEventNumber());
                    }
                }
                    //hists_rawadc.get(sensor).fill(strip, hit.getADCValues()[0] - pedestal);

                

            }
//            if (eventCount % eventRefreshRate == 0) {
//                for (HpsSiSensor sensor : sensors) {
//                    IHistogram2D hist = hists.get(sensor);
////                    hist.
//                }
//            }

        }
    }

//    private void getMean2D(IHistogram2D hist2D) {
//        int nx = hist2D.xAxis().bins();
//        int ny = hist2D.yAxis().bins();
//        double[][] means = new double[nx][ny];
//        for (int ix = 0; ix < nx; ix++) {
//            for (int iy = 0; iy < ny; iy++) {
//                means[ix][iy] = hist2D.binHeight(ix, iy) / hist2D.binEntries(ix, iy);
//            }
//        }
//        hist2D.reset();
//        for (int ix = 0; ix < nx; ix++) {
//            for (int iy = 0; iy < ny; iy++) {
//                double x = hist2D.xAxis().binCenter(ix);
//                double y = hist2D.yAxis().binCenter(iy);
//                hist2D.fill(x, y, means[ix][iy]);
//            }
//        }
//
//        IFitter fitter = AIDA.defaultInstance().analysisFactory().createFitFactory().createFitter("chi2");
//
//    }
//
//    IFitResult fitGaussian(IHistogram1D h1d, IFitter fitter, String range) {
//        double[] init = {h1d.maxBinHeight(), h1d.mean(), h1d.rms()};
//        IFitResult ifr = null;
//        try {
//            ifr = fitter.fit(h1d, "g", init, range);
//        } catch (RuntimeException ex) {
//            System.out.println(this.getClass().getSimpleName() + ":  caught exception in fitGaussian");
//        }
//        return ifr;
////        double[] init = {20.0, 0.0, 1.0, 20, -1};
////        return fitter.fit(h1d, "g+p1", init, range);
//    }
    @Override
    public void endOfData() {
        if (fitFile == null) {
            return;
        }

        /*
        IFitter fitter = aida.analysisFactory().createFitFactory().createFitter("chi2");
//        fitter.setFitMethod("CleverChiSquared");
//        fitter.setFitMethod("binnedMaximumLikelihood");

        PrintWriter fitWriter = null;
        try {
            fitWriter = new PrintWriter(fitFile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SampleZeroHVBiasChecker.class.getName()).log(Level.SEVERE, null, ex);
        }

//        for (SiSensor sensor : hists_rawadc.keySet()) {
//            fitWriter.println(sensor.getName());
//            IHistogram1D hist = hists_rawadc.get(sensor);
//            //IHistogram1D fit = aida.histogram1D("1D fit", hist.yAxis().bins(), hist.yAxis().lowerEdge(), hist.yAxis().upperEdge());
//            for (int i = 0; i < 640; i++) {
//                fitWriter.format("%d\t", i);
//                for (int y = 0; y < hist.yAxis().bins(); y++) {
//                    for (int j = 0; j < hist.binHeight(i, y); j++) {
//                        fit.fill(hist.binMeanY(i, y));
//                    }
//                }
//                fitWriter.format("%f\t%f\t%f\t", fit.sumBinHeights(), fit.mean(), fit.rms());
//                if (fit.sumBinHeights() > 100) {
//                    IFitResult result = fitter.fit(fit, "g");
//
//                    if (result.isValid()) {
//                        fitWriter.format("%f\t%f\t", result.fittedParameter("mean"), result.fittedParameter("sigma"));
//                    }
//                }
//                fitWriter.println();
//                fit.reset();
//            } 
//            fitWriter.flush();
        }
        fitWriter.close();
        aida.tree().rm("1D fit");
    */
    }
    
   
    
}
