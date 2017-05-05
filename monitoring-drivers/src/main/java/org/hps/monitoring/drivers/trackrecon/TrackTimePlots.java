package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.style.registry.IStyleStore;
import hep.aida.ref.plotter.style.registry.StyleRegistry;
import java.util.List;

import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.plot;

import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TrackTimePlots extends Driver {

    //private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    private String hitCollection = "StripClusterer_SiTrackerHitStrip1D";
    private String trackCollectionName = "MatchedTracks";
    IPlotter plotter, plotter2, plotter3, plotter4, plotter5, plotter6, plotter7;
    private IHistogram1D[][] t0 = new IHistogram1D[4][12];
    private IHistogram1D[][] trackHitT0 = new IHistogram1D[4][12];
    private IHistogram1D[][] trackHitDt = new IHistogram1D[4][12];
    private IHistogram2D[] trackHit2D = new IHistogram2D[12];
    private IHistogram1D[] trackT0 = new IHistogram1D[4];
    private IHistogram2D[] trackTrigTime = new IHistogram2D[4];
    private IHistogram2D[] trackHitDtChan = new IHistogram2D[12];
    private IHistogram1D[] trackTimeRange = new IHistogram1D[4];
    private IHistogram2D[] trackTimeMinMax = new IHistogram2D[4];

    private static final String subdetectorName = "Tracker";
    int nlayers = 12;

    public void setTrackCollectionName(String name) {
        this.trackCollectionName = name;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        aida.tree().cd("/");

        List<HpsSiSensor> sensors = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(HpsSiSensor.class);

        IPlotterFactory fac = aida.analysisFactory().createPlotterFactory("Timing");

        StyleRegistry styleRegistry = StyleRegistry.getStyleRegistry();
        IStyleStore store = styleRegistry.getStore("DefaultStyleStore");
        IPlotterStyle style2d = store.getStyle("DefaultColorMapStyle");
        style2d.setParameter("hist2DStyle", "colorMap");
        style2d.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        //style2d.zAxisStyle().setParameter("scale", "log");
        style2d.zAxisStyle().setVisible(false);
        style2d.dataBoxStyle().setVisible(false);

        IPlotterStyle styleOverlay = store.getStyle("DefaultHistogram1DStyle");
        styleOverlay.dataStyle().errorBarStyle().setVisible(true);
        styleOverlay.dataStyle().fillStyle().setVisible(false);
        styleOverlay.legendBoxStyle().setVisible(false);
        styleOverlay.dataStyle().outlineStyle().setVisible(false);

        plotter = fac.create("Hit Times");
        plotter.createRegions(3, 4);

        plotter2 = fac.create("Track Time");
        plotter2.createRegions(2, 2);

        plotter3 = fac.create("Track Hit Time");
        plotter3.createRegions(3, 4);
        plotter4 = fac.create("Track Hit dt");
        plotter4.createRegions(3, 4);

        plotter5 = fac.create("Track Time vs. dt");
        plotter5.createRegions(3, 4);

        plotter6 = fac.create("Track dt vs. Channel");
        plotter6.createRegions(3, 4);

        plotter7 = fac.create("Track Hit Time Range");
        plotter7.createRegions(2, 2);

        for (HpsSiSensor sensor : sensors) {
            int module = sensor.getModuleNumber();
            int layer = sensor.getLayerNumber() - 1;
            int region = computePlotterRegion(layer);
            styleOverlay.dataStyle().lineStyle().setColor(getColor(module));
            System.out.println(sensor.getName() + ":   module = " + module + "; layer = " + layer);
            t0[module][layer] = aida.histogram1D(sensor.getName() + "_timing", 75, -50, 100.0);
            plot(plotter, t0[module][layer], styleOverlay, region);
            trackHitT0[module][layer] = aida.histogram1D(sensor.getName() + "_trackHit_timing", 75, -50, 4000.0);
            plot(plotter3, trackHitT0[module][layer], styleOverlay, region);
            trackHitDt[module][layer] = aida.histogram1D(sensor.getName() + "_trackHit_dt", 50, -20, 20.0);
            plot(plotter4, trackHitDt[module][layer], styleOverlay, region);

        }

        for (int i = 0; i < nlayers; i++) {
            int region = computePlotterRegion(i);
            trackHit2D[i] = aida.histogram2D("Layer " + i + " trigger phase vs dt", 80, -20, 20.0, 6, 0, 24.0);
            plot(plotter5, trackHit2D[i], style2d, region);
            trackHitDtChan[i] = aida.histogram2D("Layer " + i + " dt vs position", 200, -20, 20, 50, -20, 20.0);
            plot(plotter6, trackHitDtChan[i], style2d, region);
        }
        plotter.show();
        plotter3.show();
        plotter4.show();
        plotter5.show();//"Track Time vs. dt"
        plotter6.show();// "Track dt vs. Channel"

        for (int module = 0; module < 2; module++) {
            trackT0[module] = aida.histogram1D((module == 0 ? "Top" : "Bottom") + " Track Time", 80, -20, 20.0);
            plot(plotter2, trackT0[module], null, module);
            trackTrigTime[module] = aida.histogram2D((module == 0 ? "Top" : "Bottom") + " Track Time vs. Trig Time", 80, -20, 20.0, 6, 0, 24);
            plot(plotter2, trackTrigTime[module], style2d, module + 2);

            trackTimeRange[module] = aida.histogram1D((module == 0 ? "Top" : "Bottom") + " Track Hit Time Range", 75, 0, 30.0);
            plot(plotter7, trackTimeRange[module], null, module);
            trackTimeMinMax[module] = aida.histogram2D((module == 0 ? "Top" : "Bottom") + " First and Last Track Hit Times", 80, -20, 20.0, 80, -20, 20.0);
            plot(plotter7, trackTimeMinMax[module], style2d, module + 2);
        }

        plotter2.show();
        plotter7.show(); //"Track Hit Time Range"
    }

    public void setHitCollection(String hitCollection) {
        this.hitCollection = hitCollection;
    }

    @Override
    public void process(EventHeader event) {
        int trigTime = (int) (event.getTimeStamp() % 24);

        //===> IIdentifierHelper helper = SvtUtils.getInstance().getHelper();
        List<SiTrackerHitStrip1D> hits = event.get(SiTrackerHitStrip1D.class, hitCollection);
        for (SiTrackerHitStrip1D hit : hits) {
            //===> IIdentifier id = hit.getSensor().getIdentifier();
            //===> int layer = helper.getValue(id, "layer");
            int layer = ((HpsSiSensor) hit.getSensor()).getLayerNumber();
            int module = ((HpsSiSensor) hit.getSensor()).getModuleNumber();
            //===> int module = helper.getValue(id, "module");
//            System.out.format("%d, %d, %d\n",hit.getCellID(),layer,module);
            t0[module][layer - 1].fill(hit.getTime());
        }
//

        List<Track> tracks = event.get(Track.class, trackCollectionName);
        for (Track track : tracks) {
            int trackModule;
            if (track.getTrackerHits().get(0).getPosition()[2] > 0) {
                trackModule = 0;
            } else {
                trackModule = 1;
            }
            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;
            int hitCount = 0;
            double trackTime = 0;
            for (TrackerHit hitCross : track.getTrackerHits()) {
                for (HelicalTrackStrip hit : ((HelicalTrackCross) hitCross).getStrips()) {
                    int layer = hit.layer();
                    int module = ((RawTrackerHit) hit.rawhits().get(0)).getIdentifierFieldValue("module");
                    trackHitT0[module][layer - 1].fill(hit.dEdx() / DopedSilicon.ENERGY_EHPAIR);
                    trackTime += hit.time();
                    hitCount++;
                    if (hit.time() > maxTime) {
                        maxTime = hit.time();
                    }
                    if (hit.time() < minTime) {
                        minTime = hit.time();
                    }
                }
            }
            trackTimeMinMax[trackModule].fill(minTime, maxTime);
            trackTimeRange[trackModule].fill(maxTime - minTime);
            trackTime /= hitCount;
            trackT0[trackModule].fill(trackTime);
            if (trackModule == 0) {
                trackTrigTime[trackModule].fill(trackTime, trigTime);
            } else {
                trackTrigTime[trackModule].fill(trackTime, trigTime);
            }
            for (TrackerHit hitCross : track.getTrackerHits()) {
                for (HelicalTrackStrip hit : ((HelicalTrackCross) hitCross).getStrips()) {
                    int layer = hit.layer();
                    int module = ((RawTrackerHit) hit.rawhits().get(0)).getIdentifierFieldValue("module");
                    trackHitDt[module][layer - 1].fill(hit.time() - trackTime);
                    trackHit2D[layer - 1].fill(hit.time() - trackTime, event.getTimeStamp() % 24);
                    trackHitDtChan[layer - 1].fill(hit.umeas(), hit.time() - trackTime);
                }
            }
        }
    }

    @Override
    public void endOfData() {
        //plotterFrame.dispose();
    }

//    private int computePlotterRegion(int layer, int module) {
//        int iy = (layer) / 2;
//        int ix = 0;
//        if (module > 0)
//            ix += 2;
//        if (layer % 2 == 0)
//            ix += 1;
//        int region = ix * 5 + iy;
//        return region;
//    }
    //layer 1-12
    //module 0-4...0,2 = top; 1,3=bottom
    //this computePlotterRegion puts top&bottom modules on same region
    //and assume plotter is split in 3 columns, 4 rows...L0-5 on top 2 rows; L6-11 on bottom 2
    private int computePlotterRegion(int layer) {

        if (layer == 0) {
            return 0;
        }
        if (layer == 1) {
            return 1;
        }
        if (layer == 2) {
            return 4;
        }
        if (layer == 3) {
            return 5;
        }
        if (layer == 4) {
            return 8;
        }
        if (layer == 5) {
            return 9;
        }

        if (layer == 6) {
            return 2;
        }
        if (layer == 7) {
            return 3;
        }
        if (layer == 8) {
            return 6;
        }
        if (layer == 9) {
            return 7;
        }
        if (layer == 10) {
            return 10;
        }
        if (layer == 11) {
            return 11;
        }
        return -1;
    }

    private String getColor(int module) {
        String color = "Black";
        if (module == 1) {
            color = "Green";
        }
        if (module == 2) {
            color = "Blue";
        }
        if (module == 3) {
            color = "Red";
        }
        return color;
    }
}
