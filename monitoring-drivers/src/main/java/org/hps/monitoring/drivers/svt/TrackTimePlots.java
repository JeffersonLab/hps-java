package org.hps.monitoring.drivers.svt;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;

import java.util.List;

import org.hps.conditions.deprecated.SvtUtils;
import org.hps.readout.ecal.TriggerData;
import org.hps.util.Resettable;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author meeg
 * @version $Id: SVTHitReconstructionPlots.java,v 1.14 2012/05/18 07:41:49 meeg
 * Exp $
 */
public class TrackTimePlots extends Driver implements Resettable {

	//private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    private String hitCollection = "StripClusterer_SiTrackerHitStrip1D";
    private String trackCollection = "MatchedTracks";
    IPlotter plotter, plotter2, plotter3, plotter4, plotter5, plotter6, plotter7;
    private IHistogram1D[][] t0 = new IHistogram1D[2][10];
    private IHistogram1D[][] trackHitT0 = new IHistogram1D[2][10];
    private IHistogram1D[][] trackHitDt = new IHistogram1D[2][10];
    private IHistogram2D[][] trackHit2D = new IHistogram2D[2][10];
    private IHistogram1D[] trackT0 = new IHistogram1D[2];
    private IHistogram2D[] trackTrigTime = new IHistogram2D[2];
    private IHistogram2D[][] trackHitDtChan = new IHistogram2D[2][10];
    private IHistogram1D[] trackTimeRange = new IHistogram1D[2];
    private IHistogram2D[] trackTimeMinMax = new IHistogram2D[2];

    @Override
    protected void detectorChanged(Detector detector) {
    	//plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS SVT Track Time Plots");

        aida.tree().cd("/");

        IPlotterFactory fac = aida.analysisFactory().createPlotterFactory();

        IPlotterStyle style;

        plotter = fac.create("HPS SVT Timing Plots");
        plotter.setTitle("Hit Times");
        //plotterFrame.addPlotter(plotter);
        style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(4, 5);

        plotter2 = fac.create("HPS SVT Track Time");
        plotter2.setTitle("Track Time");
        //plotterFrame.addPlotter(plotter2);
        style = plotter2.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter2.createRegions(2, 2);

        plotter3 = fac.create("HPS SVT Timing Plots");
        plotter3.setTitle("Track Hit Time");
        //plotterFrame.addPlotter(plotter3);
        style = plotter3.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(4, 5);

        plotter4 = fac.create("HPS SVT Timing Plots");
        plotter4.setTitle("Track Hit dt");
        //plotterFrame.addPlotter(plotter4);
        style = plotter4.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter4.createRegions(4, 5);

        plotter5 = fac.create("HPS SVT Timing Plots");
        plotter5.setTitle("Track Time vs. dt");
        //plotterFrame.addPlotter(plotter5);
        style = plotter5.style();
        style.statisticsBoxStyle().setVisible(false);
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.zAxisStyle().setParameter("scale", "log");
        plotter5.createRegions(4, 5);

        plotter6 = fac.create("HPS SVT Timing Plots");
        plotter6.setTitle("Track dt vs. Channel");
        //plotterFrame.addPlotter(plotter6);
        style = plotter6.style();
        style.statisticsBoxStyle().setVisible(false);
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.zAxisStyle().setParameter("scale", "log");
        plotter6.createRegions(4, 5);

        plotter7 = fac.create("HPS SVT Track Hit Time Range");
        plotter7.setTitle("Track Hit Time Range");
        //plotterFrame.addPlotter(plotter7);
        style = plotter7.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter7.createRegions(2, 2);

        for (int module = 0; module < 2; module++) {
            for (int layer = 0; layer < 10; layer++) {
                SiSensor sensor = SvtUtils.getInstance().getSensor(module, layer);
                int region = computePlotterRegion(layer + 1, module);
                t0[module][layer] = aida.histogram1D(sensor.getName() + "_timing", 75, -50, 100.0);
                plotter.region(region).plot(t0[module][layer]);
                ((PlotterRegion) plotter.region(region)).getPlot().getXAxis().setLabel("Hit time [ns]");
                trackHitT0[module][layer] = aida.histogram1D(sensor.getName() + "_trackHit_timing", 75, -50, 4000.0);
                plotter3.region(region).plot(trackHitT0[module][layer]);
                ((PlotterRegion) plotter3.region(region)).getPlot().getXAxis().setLabel("Hit time [ns]");
                trackHitDt[module][layer] = aida.histogram1D(sensor.getName() + "_trackHit_dt", 50, -20, 20.0);
                plotter4.region(region).plot(trackHitDt[module][layer]);
                ((PlotterRegion) plotter4.region(region)).getPlot().getXAxis().setLabel("Hit time residual [ns]");
                trackHit2D[module][layer] = aida.histogram2D(sensor.getName() + "_trackHit_dt_2D", 75, -50, 100.0, 50, -20, 20.0);
                plotter5.region(region).plot(trackHit2D[module][layer]);
                ((PlotterRegion) plotter5.region(region)).getPlot().getXAxis().setLabel("Track time [ns]");
                ((PlotterRegion) plotter5.region(region)).getPlot().getYAxis().setLabel("Hit time [ns]");
                trackHitDtChan[module][layer] = aida.histogram2D(sensor.getName() + "_trackHit_dt_chan", 200, -20, 20, 50, -20, 20.0);
                plotter6.region(region).plot(trackHitDtChan[module][layer]);
                ((PlotterRegion) plotter6.region(region)).getPlot().getXAxis().setLabel("Hit position [mm]");
                ((PlotterRegion) plotter6.region(region)).getPlot().getYAxis().setLabel("Hit time residual [ns]");
            }
            trackT0[module] = aida.histogram1D((module == 0 ? "Top" : "Bottom") + " Track Time", 75, -50, 100.0);
            plotter2.region(module).plot(trackT0[module]);
            ((PlotterRegion) plotter2.region(module)).getPlot().getXAxis().setLabel("Track time [ns]");
            trackTrigTime[module] = aida.histogram2D((module == 0 ? "Top" : "Bottom") + " Track Time vs. Trig Time", 75, -50, 100.0, 33, -1, 32);
            plotter2.region(module + 2).plot(trackTrigTime[module]);
            ((PlotterRegion) plotter2.region(module+2)).getPlot().getXAxis().setLabel("Track time [ns]");
            ((PlotterRegion) plotter2.region(module+2)).getPlot().getYAxis().setLabel("Trigger time [clocks]");
            style = plotter2.region(module + 2).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            style.zAxisStyle().setParameter("scale", "log");

            trackTimeRange[module] = aida.histogram1D((module == 0 ? "Top" : "Bottom") + " Track Hit Time Range", 75, 0, 30.0);
            plotter7.region(module).plot(trackTimeRange[module]);
            ((PlotterRegion) plotter7.region(module)).getPlot().getXAxis().setLabel("Track time range [ns]");
            trackTimeMinMax[module] = aida.histogram2D((module == 0 ? "Top" : "Bottom") + " First and Last Track Hit Times", 75, -50, 100.0, 75, -50, 100.0);
            plotter7.region(module + 2).plot(trackTimeMinMax[module]);
            ((PlotterRegion) plotter7.region(module+2)).getPlot().getXAxis().setLabel("First track hit time [ns]");
            ((PlotterRegion) plotter7.region(module+2)).getPlot().getYAxis().setLabel("Last track hit time [ns]");
            style = plotter7.region(module + 2).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            style.zAxisStyle().setParameter("scale", "log");
        }
//        shape = aida.histogram2D("Shape", 200, -1, 3, 200, -0.5, 2);
//        plotter5.region(0).plot(shape);

        //plotterFrame.pack();
        //plotterFrame.setVisible(true);
    }

    public void setHitCollection(String hitCollection) {
        this.hitCollection = hitCollection;
    }

    public void setTrackCollection(String trackCollection) {
        this.trackCollection = trackCollection;
    }

    @Override
    public void process(EventHeader event) {
        int orTrig = 0;
        int topTrig = 0;
        int botTrig = 0;

        int orTrigTime = -1;
        int topTrigTime = -1;
        int botTrigTime = -1;
        if (event.hasCollection(TriggerData.class, "TriggerBank")) {
            List<TriggerData> triggerList = event.get(TriggerData.class, "TriggerBank");
            if (!triggerList.isEmpty()) {
                TriggerData triggerData = triggerList.get(0);

                orTrig = triggerData.getOrTrig();
                if (orTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & orTrig) != 0) {
                            orTrigTime = i;
                            break;
                        }
                    }
                }
                topTrig = triggerData.getTopTrig();
                if (topTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & topTrig) != 0) {
                            topTrigTime = i;
                            break;
                        }
                    }
                }
                botTrig = triggerData.getBotTrig();
                if (botTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & botTrig) != 0) {
                            botTrigTime = i;
                            break;
                        }
                    }
                }
            }
        }

        IIdentifierHelper helper = SvtUtils.getInstance().getHelper();
        List<SiTrackerHitStrip1D> hits = event.get(SiTrackerHitStrip1D.class, hitCollection);
        for (SiTrackerHitStrip1D hit : hits) {
            IIdentifier id = hit.getSensor().getIdentifier();
            int layer = helper.getValue(id, "layer");
            int module = helper.getValue(id, "module");
//            System.out.format("%d, %d, %d\n",hit.getCellID(),layer,module);
            t0[module][layer - 1].fill(hit.getTime());
        }
//

        List<Track> tracks = event.get(Track.class, trackCollection);
        for (Track track : tracks) {
            int trackModule = -1;
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
                    trackHitT0[trackModule][layer - 1].fill(hit.dEdx() / DopedSilicon.ENERGY_EHPAIR);
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
                trackTrigTime[trackModule].fill(trackTime, topTrigTime);
            } else {
                trackTrigTime[trackModule].fill(trackTime, botTrigTime);
            }
            for (TrackerHit hitCross : track.getTrackerHits()) {
                for (HelicalTrackStrip hit : ((HelicalTrackCross) hitCross).getStrips()) {
                    int layer = hit.layer();
                    trackHitDt[trackModule][layer - 1].fill(hit.time() - trackTime);
                    trackHit2D[trackModule][layer - 1].fill(trackTime, hit.time() - trackTime);
                    trackHitDtChan[trackModule][layer - 1].fill(hit.umeas(), hit.time() - trackTime);
                }
            }
        }
    }

    @Override
    public void endOfData() {
    	//plotterFrame.dispose();
    }

    @Override
    public void reset() {
        for (int module = 0; module < 2; module++) {
            for (int layer = 0; layer < 10; layer++) {
                trackHitT0[module][layer].reset();
                trackHitDt[module][layer].reset();
                t0[module][layer].reset();
                trackT0[module].reset();
            }
        }
    }

    private int computePlotterRegion(int layer, int module) {
        int iy = (layer - 1) / 2;
        int ix = 0;
        if (module > 0) {
            ix += 2;
        }
        if (layer % 2 == 0) {
            ix += 1;
        }
        int region = ix * 5 + iy;
        return region;
    }
}
