package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.util.Redrawable;
import org.hps.util.Resettable;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * The driver <code>EcalMonitoringPlots</code> implements the histogram shown to the user 
 * in the first tab of the Monitoring Application, when using the Ecal monitoring lcsim file.
 * It contains only a sub-tab, with 3 histograms.
 * - Hit counts by channel (Histogram2D), Occupancy by channel (Histogram2D), Cluster counts by channel (Histogram2D) 
 * Each cluster is associated with the seed crystal.
 * 
 * These plots are updated regularly, according to the event refresh rate.
 * @author Andrea Celentano
 *
 */

public class EcalMonitoringPlots extends Driver implements Resettable, Redrawable {

    String inputCollection = "EcalReadoutHits";
    String clusterCollection = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IHistogram2D hitCountFillPlot;
    IHistogram2D hitCountDrawPlot;
    
    IHistogram2D occupancyDrawPlot;
    ArrayList<IHistogram1D> occupancyPlots;
    
    IHistogram2D clusterCountFillPlot;
    IHistogram2D clusterCountDrawPlot;
    int eventRefreshRate = 1;
    int eventn = 0;
    boolean hide = false;

    public EcalMonitoringPlots() {
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setClusterCollection(String clusterCollection) {
        this.clusterCollection = clusterCollection;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }
    
    /**
     * Set the refresh rate for histograms in this driver
     * @param eventRefreshRate: the refresh rate, defined as number of events to accumulate before
     * refreshing the plot
     */
    @Override
    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    protected void detectorChanged(Detector detector) {
        System.out.println("EcalMonitoringPlots:: detector changed was called");
        // Setup the plotter.
        plotter = aida.analysisFactory().createPlotterFactory("Ecal Monitoring Plots").create("HPS ECal Monitoring Plots");
        // Setup plots.
        aida.tree().cd("/");
        hitCountDrawPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Count", 47, -23.5, 23.5, 11, -5.5, 5.5);
        hitCountFillPlot = makeCopy(hitCountDrawPlot);
        occupancyDrawPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Occupancy", 47, -23.5, 23.5, 11, -5.5, 5.5);
        clusterCountDrawPlot = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Cluster Center Count", 47, -23.5, 23.5, 11, -5.5, 5.5);
        clusterCountFillPlot = makeCopy(clusterCountDrawPlot);

        occupancyPlots=new ArrayList<IHistogram1D>();
        for (int ii=0;ii<(11*47);ii++){
             int row=EcalMonitoringUtils.getRowFromHistoID(ii);
             int column=EcalMonitoringUtils.getColumnFromHistoID(ii);     
             occupancyPlots.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Occupancy : " + (row) + " "+ (column)+ ": "+ii, 101,0,1));  
        }
        
        
        // Create the plotter regions.
        plotter.createRegions(2, 2);
        plotter.style().statisticsBoxStyle().setVisible(false);
        IPlotterStyle style = plotter.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.dataStyle().fillStyle().setParameter("showZeroHeightBins",Boolean.FALSE.toString());
        plotter.region(0).plot(hitCountDrawPlot);
        style = plotter.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.dataStyle().fillStyle().setParameter("showZeroHeightBins",Boolean.FALSE.toString());
        plotter.region(1).plot(clusterCountDrawPlot);
        style = plotter.region(2).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.dataStyle().fillStyle().setParameter("showZeroHeightBins",Boolean.FALSE.toString());
        //style.zAxisStyle().setParameter("scale", "log");
        plotter.region(2).plot(occupancyDrawPlot);
        
        
        if (!hide) {
            plotter.show();
        }
    }
 
    public void process(EventHeader event) {
        int nhits=0;
        int chits[]=new int[11*47];
        if (event.hasCollection(BaseRawCalorimeterHit.class, inputCollection)) {
            List<BaseRawCalorimeterHit> hits = event.get(BaseRawCalorimeterHit.class, inputCollection);
            for (BaseRawCalorimeterHit hit : hits) {
                int column=hit.getIdentifierFieldValue("ix");
                int row=hit.getIdentifierFieldValue("iy");
                int id=EcalMonitoringUtils.getHistoIDFromRowColumn(row, column);
                hitCountFillPlot.fill(column,row);
                chits[id]++;
                nhits++;
            }
        }
        if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
            for (RawTrackerHit hit : hits) { 
                int column=hit.getIdentifierFieldValue("ix");
                int row=hit.getIdentifierFieldValue("iy");
                int id=EcalMonitoringUtils.getHistoIDFromRowColumn(row, column);
                hitCountFillPlot.fill(column,row);
                chits[id]++;
                nhits++;
            }
        }
        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {
                int column=hit.getIdentifierFieldValue("ix");
                int row=hit.getIdentifierFieldValue("iy");
                int id=EcalMonitoringUtils.getHistoIDFromRowColumn(row, column);
                hitCountFillPlot.fill(column,row);
                chits[id]++;
                nhits++;
            }
        }
        
        
        
        
        if (nhits>0) {
        for (int ii=0;ii<(11*47);ii++){
            occupancyPlots.get(ii).fill(chits[ii]*1./(nhits ));                
            }
        }        
        
            
        if (event.hasCollection(HPSEcalCluster.class, clusterCollection)) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollection);           
            for (HPSEcalCluster cluster : clusters) {
                clusterCountFillPlot.fill(cluster.getSeedHit().getIdentifierFieldValue("ix"), cluster.getSeedHit().getIdentifierFieldValue("iy"));
            }
        }
        if (eventRefreshRate > 0 && ++eventn % eventRefreshRate == 0) {
            redraw();
        }
    }

    public void endOfData() {
        plotter.hide();
        plotter.destroyRegions();
    }

    @Override
    public void reset() {
        hitCountFillPlot.reset();
        hitCountDrawPlot.reset();
        clusterCountFillPlot.reset();
        clusterCountDrawPlot.reset();
        
        occupancyDrawPlot.reset();
        for (int id=0;id<(47*11);id++){
            occupancyPlots.get(id).reset();
        }
    }

    @Override
    public void redraw() {
        hitCountDrawPlot.reset();
        hitCountDrawPlot.add(hitCountFillPlot);
        clusterCountDrawPlot.reset();
        clusterCountDrawPlot.add(clusterCountFillPlot);        
        occupancyDrawPlot.reset();
        for (int id=0;id<(47*11);id++){
                int row=EcalMonitoringUtils.getRowFromHistoID(id);
                int column=EcalMonitoringUtils.getColumnFromHistoID(id);
                double mean=occupancyPlots.get(id).mean();
                if ((row!=0)&&(column!=0)&&(!EcalMonitoringUtils.isInHole(row, column))) occupancyDrawPlot.fill(column,row,mean);
            }
        } 

  

    private IHistogram2D makeCopy(IHistogram2D hist) {
        return aida.histogram2D(hist.title() + "_copy", hist.xAxis().bins(), hist.xAxis().lowerEdge(), hist.xAxis().upperEdge(), hist.yAxis().bins(), hist.yAxis().lowerEdge(), hist.yAxis().upperEdge());
    }
}

