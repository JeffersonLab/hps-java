package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.util.List;

import org.hps.recon.ecal.EcalUtils;
import org.hps.record.triggerbank.SSPData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * The driver <code>EcalHitPlots</code> implements the histogram shown to the user 
 * in the second tab of the Monitoring Application, when using the Ecal monitoring lcsim file.
 * These histograms shows single-channels distributions:
 * - First sub-tab shows the hits distribution*  (Histogram2D), the occupancy* (Histogram2D), the number of hits per event (Histogram1D),
 *   the time distribution of the hits (Histogram1D)
 *   The first two histograms are defined in <code>EcalMonitoringPlots</code>.
 * - Second sub-tab shows the energy distribution of the hits (Histogram1D), and the maximum energy in each event (Histogram1D)
 * - Third sub-tab shows the time distribution of the first hit per event, for the Ecal top (Histogram1D),  for the Ecal bottom (Histogram1D),  for both  for the Ecal top (Histogram1D).
 * 
 * Histograms are updated continously, expect those marked with *, that are updated regularly depending on the event refresh rate configured in the <code> EcalMonitoringPlots </code> driver 
 * @author Andrea Celentano
 *
 */
public class EcalHitPlots extends Driver {

    //AIDAFrame plotterFrame;
    String inputCollection = "EcalCalHits";
    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter, plotter2, plotter3;

    IHistogram1D hitCountPlot;
    IHistogram1D hitTimePlot;
    IHistogram1D hitEnergyPlot;
    IHistogram1D hitMaxEnergyPlot;
    IHistogram1D topTimePlot, botTimePlot, orTimePlot;
    IHistogram1D topTrigTimePlot, botTrigTimePlot, orTrigTimePlot;
    IHistogram2D topTimePlot2D, botTimePlot2D, orTimePlot2D;
    IHistogram2D hitNumberPlot;
    IHistogram2D occupancyPlot;


    IPlotterFactory plotterFactory;

    int eventn = 0;

    double maxE = 5000 * EcalUtils.MeV;

    boolean logScale = false;
    boolean hide = false;

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setMaxE(double maxE) {
        this.maxE = maxE;
    }


    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        //System.out.println("Detector changed called: "+ detector.getClass().getName());
        aida.tree().cd("/");
        plotterFactory = aida.analysisFactory().createPlotterFactory("Ecal Hit Plots");



        // Setup plots.
        hitCountPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Count In Event", 30, -0.5, 29.5);     
        hitNumberPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Rate KHz");        
        occupancyPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Occupancy");
        topTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Top", 100, 0, 100 * 4.0);
        botTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Bottom", 100, 0, 100 * 4.0);
        orTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Or", 100, 0, 100 * 4.0);

        
        hitTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time", 100, 0 * 4.0, 100 * 4.0);
        topTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Top", 101, -2, 402);
        botTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Bottom", 101, -2, 402);
        orTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Or", 1024, 0, 4096);

        topTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Top", 100, 0, 100 * 4.0, 101, -2, 402);
        botTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Bottom", 100, 0, 100 * 4.0, 101, -2, 402);
        orTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Or", 100, 0, 100 * 4.0, 101, -2, 402);

        hitEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy", 100, -0.1, maxE);
        hitMaxEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Maximum Hit Energy In Event", 100, -0.1, maxE);



        // Setup the plotter.
        plotter = plotterFactory.create("Hit Counts");
        plotter.setTitle("Hit Counts");
        IPlotterStyle pstyle=this.createDefaultStyle();       
        pstyle.setParameter("hist2DStyle", "colorMap");
        pstyle.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        pstyle.dataStyle().fillStyle().setParameter("showZeroHeightBins",Boolean.FALSE.toString());


        // Create the plotter regions.
        plotter.createRegions(2,2);
        plotter.region(0).plot(hitNumberPlot);
        plotter.region(1).plot(hitTimePlot,pstyle);
        plotter.region(2).plot(occupancyPlot,pstyle);
        plotter.region(3).plot(hitCountPlot,pstyle);



        if (logScale){
            pstyle.zAxisStyle().setParameter("scale", "log");
        }
        else pstyle.zAxisStyle().setParameter("scale", "lin");
        //        plotter.region(0).plot(hitNumberPlot,pstyle);


        // Setup the plotter.
        plotter2 = plotterFactory.create("Hit Energies");
        plotter2.setTitle("Hit Energies");
        pstyle.zAxisStyle().setParameter("scale", "lin");

        if (logScale) {
            pstyle.yAxisStyle().setParameter("scale", "log");
        }
        else  pstyle.yAxisStyle().setParameter("scale", "lin");
        // Create the plotter regions.
        plotter2.createRegions(1, 2);
        plotter2.region(0).plot(hitEnergyPlot,pstyle);
        plotter2.region(1).plot(hitMaxEnergyPlot,pstyle); 

        plotter3 = plotterFactory.create("Hit Times");
        plotter3.setTitle("Hit Times");
        plotter3.createRegions(3, 3);      

        if (logScale) {
            pstyle.yAxisStyle().setParameter("scale", "log");
        }
        else  pstyle.yAxisStyle().setParameter("scale", "lin");

        plotter3.region(0).plot(topTimePlot,pstyle);
        plotter3.region(1).plot(botTimePlot,pstyle);
        plotter3.region(2).plot(orTimePlot,pstyle);
        plotter3.region(3).plot(topTrigTimePlot,pstyle);
        plotter3.region(4).plot(botTrigTimePlot,pstyle);
        plotter3.region(5).plot(orTrigTimePlot,pstyle);

        pstyle.yAxisStyle().setParameter("scale", "lin");
        if (logScale){
            pstyle.zAxisStyle().setParameter("scale", "log");
        }
        else pstyle.zAxisStyle().setParameter("scale", "lin");

        plotter3.region(6).plot(topTimePlot2D,pstyle);
        plotter3.region(7).plot(botTimePlot2D,pstyle);
        plotter3.region(8).plot(orTimePlot2D,pstyle);


        if (!hide) {
            plotter.show();
            plotter2.show();
            plotter3.show(); 
        }
    }

    @Override
    public void process(EventHeader event) {


        int orTrigTime=4097;
        int topTrigTime=4097;
        int botTrigTime=4097;

        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            if (!triggerList.isEmpty()) {
                GenericObject triggerData = triggerList.get(0);
               
                if (triggerData instanceof SSPData){
                    // TODO: TOP, BOTTOM, OR, and AND triggers are test
                    // run specific parameters and are not supported
                    // by SSPData.
                    orTrigTime  = 0; //((SSPData)triggerData).getOrTrig();
                    topTrigTime = 0; //((SSPData)triggerData).getTopTrig();
                    botTrigTime = 0; //((SSPData)triggerData).getBotTrig(); 

                    
                    orTrigTimePlot.fill(orTrigTime);
                    topTrigTimePlot.fill(topTrigTime);
                    botTrigTimePlot.fill(botTrigTime);

                }       
                
            }//end if triggerList isEmpty
        }

        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            hitCountPlot.fill(hits.size());
            int id = 0;
            int row = 0;
            int column = 0;
            double maxEnergy = 0;
            double topTime = Double.POSITIVE_INFINITY;
            double botTime = Double.POSITIVE_INFINITY;
            double orTime = Double.POSITIVE_INFINITY;
            for (CalorimeterHit hit : hits) {


                hitEnergyPlot.fill(hit.getRawEnergy());
                hitTimePlot.fill(hit.getTime());


                if (hit.getTime() < orTime) {
                    orTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") > 0 && hit.getTime() < topTime) {
                    topTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") < 0 && hit.getTime() < botTime) {
                    botTime = hit.getTime();
                }
                if (hit.getRawEnergy() > maxEnergy) {
                    maxEnergy = hit.getRawEnergy();
                }
            }

            if (orTime != Double.POSITIVE_INFINITY) {
                orTimePlot.fill(orTime);
                orTimePlot2D.fill(orTime, orTrigTime);
            }
            if (topTime != Double.POSITIVE_INFINITY) {
                topTimePlot.fill(topTime);
                topTimePlot2D.fill(topTime, topTrigTime);
            }
            if (botTime != Double.POSITIVE_INFINITY) {
                botTimePlot.fill(botTime);
                botTimePlot2D.fill(botTime, botTrigTime);
            }
            hitMaxEnergyPlot.fill(maxEnergy);
        } else {
            hitCountPlot.fill(0);
        }         
    }

    @Override
    public void endOfData() {
        //plotterFrame.dispose();
    }

    public void setHide(boolean hide)
    {
        this.hide=hide;
    }


    /**
     * Initializes the default style for plots.
     * @return Returns an <code>IPlotterStyle</code> object that
     * represents the default style for plots.
     */
    public IPlotterStyle createDefaultStyle() {
        IPlotterStyle pstyle = plotterFactory.createPlotterStyle();
        // Set the appearance of the axes.
        pstyle.xAxisStyle().labelStyle().setBold(true);
        pstyle.yAxisStyle().labelStyle().setBold(true);
        pstyle.xAxisStyle().tickLabelStyle().setBold(true);
        pstyle.yAxisStyle().tickLabelStyle().setBold(true);
        pstyle.xAxisStyle().lineStyle().setColor("black");
        pstyle.yAxisStyle().lineStyle().setColor("black");
        pstyle.xAxisStyle().lineStyle().setThickness(2);
        pstyle.yAxisStyle().lineStyle().setThickness(2);

        // Set color settings.
        pstyle.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        pstyle.dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
        pstyle.dataStyle().errorBarStyle().setVisible(false);
        pstyle.setParameter("hist2DStyle", "colorMap");

        // Force auto range to zero.
        pstyle.yAxisStyle().setParameter("allowZeroSuppression", "false");
        pstyle.xAxisStyle().setParameter("allowZeroSuppression", "false");

        // Set the title style.
        pstyle.titleStyle().textStyle().setFontSize(20);

        // Draw caps on error bars.
        pstyle.dataStyle().errorBarStyle().setParameter("errorBarDecoration", (new Float(1.0f)).toString());

        // Turn off grid lines until explicitly enabled.
        pstyle.gridStyle().setVisible(false);

        // Return the style.
        return pstyle;
    }







}


