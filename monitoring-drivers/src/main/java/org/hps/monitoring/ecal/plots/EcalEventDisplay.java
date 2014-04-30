package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.hps.util.Resettable;
import org.hps.monitoring.ecal.event.Cluster;
import org.hps.monitoring.ecal.event.EcalHit;
import org.hps.monitoring.ecal.ui.PEventViewer;
import org.hps.monitoring.ecal.util.CrystalEvent;
import org.hps.monitoring.ecal.util.CrystalListener;
import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *  The driver <code>EcalEvendDisplay</code> implements the histogram shown to the user 
 * in the fifth tab of the Monitoring Application, when using the Ecal monitoring lcsim file.
 * IT ALSO OPENS KYLE's EVENT DISPLAY <code>PEventViewer</code>.
 * The implementation is as follows:
 * - The event display is opened in a separate window
 * - It is updated regularly, according to the event refresh rate
 * - If the user clicks on a crystal, the corresponding energy and time distributions (both Histogram1D) are shown in the last panel of the MonitoringApplicatopn
 * The single channel plots are created in the  <code>EcalHitPlots</code> driver.
 * @author Andrea Celentano
 *  *
 */

public class EcalEventDisplay extends Driver implements CrystalListener,ActionListener,Resettable {

  
    String inputCollection = "EcalCalHits";
    String clusterCollection = "EcalClusters";
    private IPlotter plotter;
    private AIDA aida=AIDA.defaultInstance();
    private Detector detector;
    private IPlotterFactory plotterFactory;
    int eventRefreshRate = 1;
    int eventn = 0;
	int ix,iy,id;
    private PEventViewer viewer; //this is the Kyle event viewer.    

    IHistogram1D hEnergyDraw,hTimeDraw;
    IHistogram2D hTimeVsEnergyDraw;
    

    ArrayList<IHistogram1D> channelEnergyPlot;
    ArrayList<IHistogram1D> channelTimePlot;
    ArrayList<IHistogram2D> channelTimeVsEnergyPlot;
  
    double maxEch = 2500 * ECalUtils.MeV;
    
    public EcalEventDisplay() {
    	
    }

    public void setMaxEch(double maxEch) {
        this.maxEch = maxEch;
    }
    
    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }
    

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }
    
    @Override
    public void detectorChanged(Detector detector) {
    	System.out.println("Ecal event display detector changed");
        this.detector = detector;
    	
    	aida.tree().cd("/");
    	
      
    	
       channelEnergyPlot=new ArrayList<IHistogram1D>();
       channelTimePlot=new ArrayList<IHistogram1D>();
       channelTimeVsEnergyPlot=new ArrayList<IHistogram2D>();
       //create the histograms for single channel energy and time distribution.
       //these are NOT shown in this plotter, but are used in the event display.
       for(int ii = 0; ii < (47*11); ii = ii +1){
             int row=EcalMonitoringUtils.getRowFromHistoID(ii);
             int column=EcalMonitoringUtils.getColumnFromHistoID(ii);      
             channelEnergyPlot.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy : " + (row) + " "+ (column)+ ": "+ii, 100, -0.1, maxEch));  
             channelTimePlot.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time : " + (row) + " "+ (column)+ ": "+ii, 100, 0, 400));     
             channelTimeVsEnergyPlot.add(aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time Vs Energy : " + (row) + " "+ (column)+ ": "+ii, 100, 0, 400,100, -0.1, maxEch));              
       }
       id=0;
       iy=EcalMonitoringUtils.getRowFromHistoID(id);
   	   ix=EcalMonitoringUtils.getColumnFromHistoID(id);  
   	   
   	   
         			
      	
    	hEnergyDraw=aida.histogram1D("Energy",channelEnergyPlot.get(0).axis().bins(), channelEnergyPlot.get(0).axis().lowerEdge(),channelEnergyPlot.get(0).axis().upperEdge());
    	hTimeDraw=aida.histogram1D("Time", channelTimePlot.get(0).axis().bins(),channelTimePlot.get(0).axis().lowerEdge(),channelTimePlot.get(0).axis().upperEdge());
    	//hTimeVsEnergyDraw=aida.histogram2D("Hit Time Vs Energy" ,channelTimeVsEnergyPlot.get(0).xAxis().bins(),channelTimeVsEnergyPlot.get(0).xAxis().lowerEdge(),channelTimeVsEnergyPlot.get(0).xAxis().upperEdge(),channelTimeVsEnergyPlot.get(0).yAxis().bins(),channelTimeVsEnergyPlot.get(0).yAxis().lowerEdge(),channelTimeVsEnergyPlot.get(0).yAxis().upperEdge());
    	hTimeVsEnergyDraw=aida.histogram2D("Time Vs Energy",100,0,400,100,-0.1,maxEch);
    			
    			
    			
    			
    	plotterFactory = aida.analysisFactory().createPlotterFactory("Ecal single channel plots");
   
       
        plotter = plotterFactory.create("Single hits");
        plotter.setTitle("");
        plotter.style().dataStyle().fillStyle().setParameter("showZeroHeightBins",Boolean.FALSE.toString());
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2,2);
        plotter.region(0).plot(hEnergyDraw);
        plotter.region(1).plot(hTimeDraw);
        plotter.region(2).plot(hTimeVsEnergyDraw);
        //plotter.region(1).plot();
        //plotter.region(2).plot();
        //plotter.region(3).plot();
        
        //plotterFrame = new AIDAFrame(); //and not AIDA.defaultInstance();
        //plotterFrame.setVisible(true);
        //plotterFrame.addPlotter(plotter);
    
        
        
        System.out.println("Create the event viewer");
        viewer=new PEventViewer();
        viewer.addCrystalListener(this);
        System.out.println("Done");
        
      
        plotter.show();
       
        viewer.setVisible(true);
        
    }

    @Override
    public void endOfData() {
        
    }

    @Override
    public void process(EventHeader event) {
    	
    	  int ii;
          int row = 0;
          int column = 0;
          
          boolean do_update=false;
    	  if (++eventn % eventRefreshRate == 0) {
              do_update=true;
          }
    	
    	  if (do_update){
          	viewer.resetDisplay();
    	    viewer.updateDisplay();
    	  }
    
    	Cluster the_cluster;
    	
        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
        	List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {
                row=hit.getIdentifierFieldValue("iy");
                column=hit.getIdentifierFieldValue("ix");
            	if (do_update) viewer.addHit(new EcalHit(column,row, hit.getRawEnergy()));         
                if ((row!=0)&&(column!=0)){
                    ii = EcalMonitoringUtils.getHistoIDFromRowColumn(row,column);
                    channelEnergyPlot.get(ii).fill(hit.getCorrectedEnergy());
                    channelTimePlot.get(ii).fill(hit.getTime());
                    channelTimeVsEnergyPlot.get(ii).fill(hit.getTime(),hit.getRawEnergy());
                } 
            }
        }
        if (event.hasCollection(HPSEcalCluster.class, clusterCollection)) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollection);
            for (HPSEcalCluster cluster : clusters) {
                CalorimeterHit seedHit = cluster.getSeedHit();
                if (do_update){
                the_cluster=new Cluster(seedHit.getIdentifierFieldValue("ix"), seedHit.getIdentifierFieldValue("iy"), cluster.getEnergy());
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    if (hit.getRawEnergy() != 0) 
                        column=hit.getIdentifierFieldValue("ix");
                        row=hit.getIdentifierFieldValue("iy");                	
                        the_cluster.addComponentHit(hit.getIdentifierFieldValue("ix"),hit.getIdentifierFieldValue("iy"));
                }         
                viewer.addCluster(the_cluster);
               }
            }
        }
        
        if (do_update){
        viewer.updateDisplay();
        
        
        //need also to update single-hit histograms, since they're just a drawing copy!
        //get the new histograms
     
    	//clone hEnergyDraw
       	hEnergyDraw.reset();
       	hEnergyDraw.setTitle(channelEnergyPlot.get(id).title());
       	hEnergyDraw.add(channelEnergyPlot.get(id));
       	
   
       	hTimeDraw.reset();
       	hTimeDraw.setTitle(channelTimePlot.get(id).title());
       	hTimeDraw.add(channelTimePlot.get(id));
        
    	
      	hTimeVsEnergyDraw.reset();
       	hTimeVsEnergyDraw.setTitle(channelTimeVsEnergyPlot.get(id).title());
       	hTimeVsEnergyDraw.add(channelTimeVsEnergyPlot.get(id));
        }
    }
    

    @Override
    public void reset(){
        for(int ii = 0; ii < (47*11); ii = ii +1){         
            channelEnergyPlot.get(ii).reset();
            channelTimePlot.get(ii).reset();
            channelTimeVsEnergyPlot.get(ii).reset();
        }
    }
    @Override
    public void actionPerformed(ActionEvent ae) {
     
    }
    
    
    
    
    
    @Override
    public void crystalActivated(CrystalEvent e){
		
	}
	
	/**
	 * <b>crystalDeactivated</b><br/><br/>
	 * <code>public void <b>crystalDeactivated</b>(CrystalEvent e)</code><br/><br/>
	 * Invoked when a crystal ceases to be highlighted.
	 * @param e - An object describing the event.
	 */
    @Override
	public void crystalDeactivated(CrystalEvent e){
		
	}
	
	/**
	 * <b>crystalClicked</b><br/><br/>
	 * <code>public void <b>crystalClicked</b>(CrystalEvent e)</code><br/><br/>
	 * Invoked when a crystal is clicked
	 * @param e - An object describing the event.
	 */
    @Override
	public void crystalClicked(CrystalEvent e){
    
    	Point displayPoint,ecalPoint;
    	displayPoint=e.getCrystalID();
    	ecalPoint=viewer.toEcalPoint(displayPoint);
    	ix=(int) ecalPoint.getX(); //column
    	iy=(int) ecalPoint.getY(); //raw
    	id=EcalMonitoringUtils.getHistoIDFromRowColumn(iy,ix);
    	System.out.println("Crystal event: "+ix+" "+iy+" "+id);
    	
        //  plotter.hide();
    	//get the new histograms
    
    	//clone hEnergyDraw
       	hEnergyDraw.reset();
        plotter.region(0).setTitle(channelEnergyPlot.get(id).title());
       	hEnergyDraw.setTitle(channelEnergyPlot.get(id).title());
       	hEnergyDraw.add(channelEnergyPlot.get(id));
       	
    
       	hTimeDraw.reset();
        plotter.region(1).setTitle(channelTimePlot.get(id).title());
       	hTimeDraw.setTitle(channelTimePlot.get(id).title());
       	hTimeDraw.add(channelTimePlot.get(id));
       	
     	
       	hTimeVsEnergyDraw.reset();
        plotter.region(2).setTitle(channelTimeVsEnergyPlot.get(id).title());
       	hTimeVsEnergyDraw.setTitle(channelTimeVsEnergyPlot.get(id).title());
       	hTimeVsEnergyDraw.add(channelTimeVsEnergyPlot.get(id));
	}    
}

