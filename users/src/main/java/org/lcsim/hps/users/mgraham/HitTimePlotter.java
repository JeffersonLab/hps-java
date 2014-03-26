package org.lcsim.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.util.AIDAFrame;
import org.hps.util.Resettable;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class HitTimePlotter extends Driver implements Resettable {

    private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IPlotter plotter2;
    IPlotter plotter3;
    IPlotter plotter4;
    IPlotter plotter5;
    IPlotter plotter6;
    IPlotter plotter7;
    IAnalysisFactory fac = aida.analysisFactory();
    private String trackCollectionName = "MatchedTracks";
    private String outputPlots = null;
    private Map<String, Integer> sensorRegionMap;
    private String trackerName = "Tracker";
    private List<SiSensor> sensors;

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        plotterFrame = new AIDAFrame();
        plotterFrame.setTitle("HPS Tracking Plots");
        sensors = detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);
        sensorRegionMap = new HashMap<String, Integer>();
        for (SiSensor sensor : sensors) {
            int region = computePlotterRegion(sensor);
            sensorRegionMap.put(sensor.getName(), region);
        }

        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Hit Times");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(3, 2);
        plotterFrame.addPlotter(plotter);

        IHistogram1D ecalHT = aida.histogram1D("ECAL Hit Time", 100, 0, 400);
        IHistogram1D svtHT = aida.histogram1D("SVT Hit Time", 125, -50, 75);
        IHistogram1D svtHTCorrected = aida.histogram1D("Trigger Corrected SVT Hit Time", 120, -240, -120);
        IHistogram1D svtHTModCorrected = aida.histogram1D("Module Corrected SVT Hit Time", 120, -50, 70);
        IHistogram1D svtHTCorrectedZoom = aida.histogram1D("Trigger Corrected SVT Hit Time Zoom", 80, -206, -166);
        IHistogram1D svtHTModCorrectedZoom = aida.histogram1D("Module Corrected SVT Hit Time Zoom", 80, -20, 20);
        plotter.region(0).plot(ecalHT);
        plotter.region(1).plot(svtHT);
        plotter.region(2).plot(svtHTCorrected);
        plotter.region(3).plot(svtHTModCorrected);
            plotter.region(4).plot(svtHTCorrectedZoom);
        plotter.region(5).plot(svtHTModCorrectedZoom);
        /*
         * plotter.setTitle("Momentum"); IPlotterStyle style = plotter.style();
         * style.dataStyle().fillStyle().setColor("yellow");
         * style.dataStyle().errorBarStyle().setVisible(false);
         * plotter.createRegions(2, 3); plotterFrame.addPlotter(plotter);
         *
         * IHistogram1D trkPx = aida.histogram1D("Track Momentum (Px)", 25,
         * -0.25, 0.25); IHistogram1D trkPy = aida.histogram1D("Track Momentum
         * (Py)", 25, -0.1, 0.1); IHistogram1D trkPz = aida.histogram1D("Track
         * Momentum (Pz)", 25, 0, 3.5); IHistogram1D trkChi2 =
         * aida.histogram1D("Track Chi2", 25, 0, 25.0); IHistogram1D xAtConvert
         * = aida.histogram1D("X (mm) @ Converter", 50, -50, 50); IHistogram1D
         * yAtConvert = aida.histogram1D("Y (mm) @ Converter", 50, -20, 20);
         * plotter.region(0).plot(trkPx); plotter.region(1).plot(trkPy);
         * plotter.region(2).plot(trkPz); plotter.region(3).plot(trkChi2);
         * plotter.region(4).plot(xAtConvert);
         * plotter.region(5).plot(yAtConvert);
         */

        plotter2 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter2.setTitle("Inner Tracker");
        IPlotterStyle style2 = plotter2.style();
        style2.dataStyle().fillStyle().setColor("yellow");
        style2.dataStyle().errorBarStyle().setVisible(false);
        plotter2.createRegions(6, 2);
        plotterFrame.addPlotter(plotter2);

        plotter3 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter3.setTitle("Outer Tracker");
        plotter3.setStyle(style2);
        plotter3.createRegions(6, 4);
        plotterFrame.addPlotter(plotter3);

        plotter4 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter4.setTitle("Corrected Times:  Inner Tracker");
        IPlotterStyle style4 = plotter4.style();
        style4.dataStyle().fillStyle().setColor("yellow");
        style4.dataStyle().errorBarStyle().setVisible(false);
        plotter4.createRegions(6, 2);
        plotterFrame.addPlotter(plotter4);

        plotter5 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter5.setTitle("Outer Tracker");
        plotter5.setStyle(style2);
        plotter5.createRegions(6, 4);
        plotterFrame.addPlotter(plotter5);


        int region = 0;
        for (SiSensor sensor : sensors) {
            IHistogram1D svtTimePlot = aida.histogram1D(sensor.getName() + "_Time", 60, -200, -170);
            IHistogram1D svtCorTimePlot = aida.histogram1D(sensor.getName() + "_CorrectedTime", 100, -100, 100);
//            int region = sensorRegionMap.get(sensor.getName());  // this doesn't work anymore...
            IIdentifierHelper helper = sensor.getIdentifierHelper();
            IIdentifier id = sensor.getIdentifier();

            int layer = helper.getValue(id, "layer"); // 1-12; axial layers are odd layers; stereo layers are even
            int module = helper.getValue(id, "module");

            svtTimePlot.setTitle("Layer " + layer + " Module " + module);
            if (region < 12) {
                plotter2.region(region).plot(svtTimePlot);
                plotter4.region(region).plot(svtCorTimePlot);
                region++;
            } else {
                plotter3.region(region - 12).plot(svtTimePlot);
                plotter5.region(region - 12).plot(svtCorTimePlot);
                region++;
            }
        }


        plotter6 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter6.setTitle("Tracks");
        plotter6.setStyle(style2);
        plotter6.createRegions(2, 2);
        plotterFrame.addPlotter(plotter6);
        IHistogram1D px = aida.histogram1D("Track Momentum(Px)", 50, -0.2, 0.2);
        IHistogram1D py = aida.histogram1D("Track Momentum(Py)", 50, -0.2, 0.2);
        IHistogram1D pz = aida.histogram1D("Track Momentum(Pz)", 50, 0, 2.2);
        IHistogram1D chi2 = aida.histogram1D("TrackChi2", 50, 0, 25);
        plotter6.region(0).plot(px);
        plotter6.region(1).plot(py);
        plotter6.region(2).plot(pz);
        plotter6.region(3).plot(chi2);


        plotter7 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter7.setTitle("Per Event");
        plotter7.setStyle(style2);
        plotter7.createRegions(2, 2);
        plotterFrame.addPlotter(plotter7);

        IHistogram1D nclus = aida.histogram1D("Strip Clusters per Event", 100, 0, 99);
        IHistogram1D nhth = aida.histogram1D("Stereo Hits per Event", 100, 0, 99);
        IHistogram1D ntrk = aida.histogram1D("Tracks per Event", 40, 0, 39);

        plotter7.region(0).plot(nclus);
        plotter7.region(1).plot(nhth);
        plotter7.region(2).plot(ntrk);



        plotterFrame.pack();
        plotterFrame.setVisible(true);
    }

    public void process(EventHeader event) {
        aida.tree().cd("/");

        List<TrackerHit> trackerHits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
        List<CalorimeterHit> ecalHits = event.get(CalorimeterHit.class, "EcalCalHits");


        List<HelicalTrackHit> hth = event.get(HelicalTrackHit.class, "HelicalTrackHits");
        //        System.out.println("Event with ECal timestamp " + ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_ECAL, event)
//                + ", SVT timestamp " + ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event)
//                + ", Trigger timestamp " + ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRIGGER, event));
        double t0Ecal = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_ECAL, event);
        double t0Svt = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event);
        double t0Trig = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRIGGER, event);
        for (CalorimeterHit hit : ecalHits) {
            double cor = hit.getTime() + (t0Ecal - t0Trig);
            aida.histogram1D("ECAL Hit Time").fill(hit.getTime());
//            aida.histogram1D("Trigger Corrected ECAL Hit Time").fill(cor);
//            System.out.println("Ecal: " + (hit.getTime()) + ";  Corrected = " + cor);
        }
        for (TrackerHit hit : trackerHits) {

            IIdentifierHelper helper = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierHelper();
            IIdentifier id = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifier();
            int layer = helper.getValue(id, "layer"); // 1-10; axial layers are odd layers; stereo layers are even
            int module = helper.getValue(id, "module"); // 0-1; module number is top or bottom

            String sensorName = ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement().getName();
            double cor = hit.getTime() + (t0Svt - t0Trig);
            double corMod = cor - getT0Shift(layer, module);
            aida.histogram1D("SVT Hit Time").fill(hit.getTime());
            aida.histogram1D("Trigger Corrected SVT Hit Time").fill(cor);
            aida.histogram1D("Module Corrected SVT Hit Time").fill(corMod);
             aida.histogram1D("Trigger Corrected SVT Hit Time Zoom").fill(cor);
            aida.histogram1D("Module Corrected SVT Hit Time Zoom").fill(corMod);
            aida.histogram1D(sensorName + "_Time").fill(cor);
            aida.histogram1D(sensorName + "_CorrectedTime").fill(corMod);

            //System.out.println("Svt: " + (hit.getTime()) + ";  Corrected = " + cor+";  Module Corrected = " + corMod);
        }
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            return;
        }
        List<Track> tracks = event.get(Track.class, trackCollectionName);

        for (Track trk : tracks) {
            aida.histogram1D("Track Momentum(Px)").fill(trk.getPY());
            aida.histogram1D("Track Momentum(Py)").fill(trk.getPZ());
            aida.histogram1D("Track Momentum(Pz)").fill(trk.getPX());
            aida.histogram1D("TrackChi2").fill(trk.getChi2());
        }

        aida.histogram1D("Strip Clusters per Event").fill(trackerHits.size());
        aida.histogram1D("Stereo Hits per Event").fill(hth.size());
        aida.histogram1D("Tracks per Event").fill(tracks.size());

    }

    private int computePlotterRegion(SiSensor sensor) {

        IIdentifierHelper helper = sensor.getIdentifierHelper();
        IIdentifier id = sensor.getIdentifier();

        int layer = helper.getValue(id, "layer"); // 1-12; axial layers are odd layers; stereo layers are even
        int module = helper.getValue(id, "module"); // 0-1; module number is top (evens) or bottom (odds)
        int region = -99;
        // Compute the sensor's x and y grid coordinates and then translate to region number.
        int ix, iy;
        if (layer < 7) {
            ix = (layer - 1) / 2;
            iy = 0;
            if (module > 0) {
                iy += 2;
            }
            if (layer % 2 == 0) {
                iy += 1;
            }
            region = ix * 2 + iy;
        } else {
            ix = (layer - 7) / 2;
            iy = module;

            region = ix * 4 + iy;
            region += 100;
        }
        System.out.println(sensor.getName() + "; lyr=" + layer + "; mod=" + module + " -> xy[" + ix + "][" + iy + "] -> reg=" + region);

        return region;
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void endOfData() {
        System.out.println("Output");
        IFitFactory fitFactory = fac.createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");

        for (SiSensor sensor : sensors) {
            IHistogram1D svtTimePlot = aida.histogram1D(sensor.getName() + "_Time", 150, -250, -100);
            IHistogram1D svtCorTimePlot = aida.histogram1D(sensor.getName() + "_CorrectedTime", 100, -100, 100);
//            int region = sensorRegionMap.get(sensor.getName());  // this doesn't work anymore...
            IIdentifierHelper helper = sensor.getIdentifierHelper();
            IIdentifier id = sensor.getIdentifier();

            int layer = helper.getValue(id, "layer"); // 1-12; axial layers are odd layers; stereo layers are even
            int module = helper.getValue(id, "module");
            IHistogram1D hist = aida.histogram1D(sensor.getName() + "_Time");
            System.out.println("Number of entries = " + hist.allEntries());
            IFitResult result = fitter.fit(hist, "g");

//            System.out.println(sensor.getName() + " Peak Position = " + getPeakPosition(aida.histogram1D(sensor.getName() + "_Time")));
            System.out.println(sensor.getName() + " " + result.fittedParameterNames()[0] + " = " + result.fittedParameters()[0] + " +/- " + result.errors()[0]);
            System.out.println(sensor.getName() + " " + result.fittedParameterNames()[1] + " = " + result.fittedParameters()[1] + " +/- " + result.errors()[1]);
            System.out.println(sensor.getName() + " " + result.fittedParameterNames()[2] + " = " + result.fittedParameters()[2] + " +/- " + result.errors()[2]);

        }
        
          IHistogram1D hist = aida.histogram1D("Trigger Corrected SVT Hit Time Zoom");
           System.out.println("Number of entries = " + hist.allEntries());
            IFitResult result = fitter.fit(hist, "g");

//            System.out.println(sensor.getName() + " Peak Position = " + getPeakPosition(aida.histogram1D(sensor.getName() + "_Time")));
            System.out.println("Trigger Corrected SVT Hit Time Zoom "  + result.fittedParameterNames()[0] + " = " + result.fittedParameters()[0] + " +/- " + result.errors()[0]);
            System.out.println("Trigger Corrected SVT Hit Time Zoom "  + result.fittedParameterNames()[1] + " = " + result.fittedParameters()[1] + " +/- " + result.errors()[1]);
            System.out.println("Trigger Corrected SVT Hit Time Zoom "  + result.fittedParameterNames()[2] + " = " + result.fittedParameters()[2] + " +/- " + result.errors()[2]);

           hist = aida.histogram1D("Module Corrected SVT Hit Time Zoom");
           System.out.println("Number of entries = " + hist.allEntries());
             result = fitter.fit(hist, "g");

//            System.out.println(sensor.getName() + " Peak Position = " + getPeakPosition(aida.histogram1D(sensor.getName() + "_Time")));
            System.out.println("Module Corrected SVT Hit Time Zoom "  + result.fittedParameterNames()[0] + " = " + result.fittedParameters()[0] + " +/- " + result.errors()[0]);
            System.out.println("Module Corrected SVT Hit Time Zoom "  + result.fittedParameterNames()[1] + " = " + result.fittedParameters()[1] + " +/- " + result.errors()[1]);
            System.out.println("Module Corrected SVT Hit Time Zoom "  + result.fittedParameterNames()[2] + " = " + result.fittedParameters()[2] + " +/- " + result.errors()[2]);

        
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(HitTimePlotter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private double getPeakPosition(IHistogram1D hist) {
        int nbins = hist.axis().bins();
        int maxBin = -99;
        int max = -99;
        for (int i = 0; i < nbins; i++) {
            if (hist.binEntries(i) > max) {
                maxBin = i;
                max = hist.binEntries(i);
            }
        }
        return hist.binMean(maxBin);

    }

    private double getT0Shift(int layer, int module) {
        double value = 0;
        switch (module) {
            case 0:
                switch (layer) {
                    case 1:
                        value = -186.1;
                        break;
                    case 2:
                        value = -186.1;
                        break;
                    case 3:
                        value = -185.8;
                        break;
                    case 4:
                        value = -185.9;
                        break;
                    case 5:
                        value = -185.8;
                        break;
                    case 6:
                        value = -185.8;
                        break;
                    case 7:
                        value = -185.8;
                        break;
                    case 8:
                        value = -185.9;
                        break;
                    case 9:
                        value = -185.8;
                        break;
                    case 10:
                        value = -185.8;
                        break;
                    case 11:
                        value = -185.6;
                        break;
                    case 12:
                        value = -186.0;
                        break;
                }
                break;
            case 1:
                switch (layer) {
                    case 1:
                        value = -185.6;
                        break;
                    case 2:
                        value = -185.3;
                        break;
                    case 3:
                        value = -185.9;
                        break;
                    case 4:
                        value = -185.7;
                        break;
                    case 5:
                        value = -185.6;
                        break;
                    case 6:
                        value = -185.9;
                        break;
                    case 7:
                        value = -185.8;
                        break;
                    case 8:
                        value = -186.1;
                        break;
                    case 9:
                        value = -186.0;
                        break;
                    case 10:
                        value = -185.9;
                        break;
                    case 11:
                        value = -185.9;
                        break;
                    case 12:
                        value = -186.1;
                        break;
                }
                break;
            case 2:
                switch (layer) {
                    case 7:
                        value = -185.7;
                        break;
                    case 8:
                        value = -185.7;
                        break;
                    case 9:
                        value = -185.9;
                        break;
                    case 10:
                        value = -186.0;
                        break;
                    case 11:
                        value = -185.8;
                        break;
                    case 12:
                        value = -185.9;
                        break;
                }
                break;
            case 3:
                switch (layer) {
                    case 7:
                        value = -185.5;
                        break;
                    case 8:
                        value = -185.6;
                        break;
                    case 9:
                        value = -185.5;
                        break;
                    case 10:
                        value = -185.5;
                        break;
                    case 11:
                        value = -186.6;
                        break;
                    case 12:
                        value = -186.7;
                        break;
                }
                break;
        }
        return value;
    }
}
