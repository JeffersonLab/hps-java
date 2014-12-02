package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.lang.System;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;


import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;
import org.hps.monitoring.ecal.eventdisplay.ui.PEventViewer;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalEvent;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalListener;
import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *  The driver <code>EcalEventDisplay</code> implements the histogram shown to the user 
 * in the fifth tab of the Monitoring Application, when using the Ecal monitoring lcsim file.
 * IT ALSO OPENS KYLE's EVENT DISPLAY <code>PEventViewer</code>.
 * The implementation is as follows:
 * - The event display is opened in a separate window
 * - It is updated regularly, according to the event refresh rate
 * - If the user clicks on a crystal, the corresponding energy and time distributions (both Histogram1D) are shown in the last panel of the MonitoringApplication,
 * as well as a 2D histogram (hit time vs hit energy). Finally, if available, the raw waveshape (in mV) is displayed.
 * 
 * @author Andrea Celentano
 *  *
 */

public class EcalEventDisplay extends Driver implements CrystalListener, ActionListener {

  
    String inputCollection = "EcalCalHits";
    String inputCollectionRaw = "EcalReadoutHits";
    String clusterCollection = "EcalClusters";
    private IPlotter plotter;
    private AIDA aida=AIDA.defaultInstance();
    private Detector detector;
    private IPlotterFactory plotterFactory;
    int eventRefreshRate = 1;
    int eventn = 0;
	int ix,iy,id;
	int pedSamples=10;
	
	double amp,ped,sigma;
	double hitE;
    int[] windowRaw=new int[47*11];//in case we have the raw waveform, this is the window lenght (in samples)
	boolean[] isFirstRaw=new boolean[47*11];
	
	
	
	private PEventViewer viewer; //this is the Kyle event viewer.    

    
    ArrayList<IHistogram1D> channelEnergyPlot;
    ArrayList<IHistogram1D> channelTimePlot;
    ArrayList<IHistogram1D> channelRawWaveform;
   // ArrayList<ICloud1D> channelRawWaveform;
    ArrayList<IHistogram2D> channelTimeVsEnergyPlot;
   
    IPlotterStyle pstyle;
    
    
    double maxEch = 3500 * ECalUtils.MeV;
    double minEch = 10* ECalUtils.MeV;
    
    int itmpx,itmpy;
    
    long thisTime,prevTime;
    
    public EcalEventDisplay() {
    	
    }

   
    
    public void setMaxEch(double maxEch) {
        this.maxEch = maxEch;
    }
    
    public void setMinEch(double minEch) {
        this.minEch = minEch;
    }
    
    public void setPedSamples(int pedSamples) {
        this.pedSamples = pedSamples;
    }
    
    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }
    
    public void setInputCollectionRaw(String inputCollectionRaw) {
        this.inputCollectionRaw = inputCollectionRaw;
    }
    
    public void setInputClusterCollection(String inputClusterCollection) {
        this.clusterCollection = inputClusterCollection;
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
       channelRawWaveform=new ArrayList<IHistogram1D>();
       //channelRawWaveform=new ArrayList<ICloud1D>();
       channelTimeVsEnergyPlot=new ArrayList<IHistogram2D>();
       //create the histograms for single channel energy and time distribution.
       for(int ii = 0; ii < (47*11); ii = ii +1){
             int row=ECalUtils.getRowFromHistoID(ii);
             int column=ECalUtils.getColumnFromHistoID(ii);      
             channelEnergyPlot.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy : " + (column) + " "+ (row)+ ": "+ii, 100, -.2, maxEch));  
             channelTimePlot.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time : " + (column) + " "+ (row)+ ": "+ii, 100, 0, 400));     
             channelTimeVsEnergyPlot.add(aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time Vs Energy : " + (column) + " "+ (row)+ ": "+ii, 100, 0, 400,100, -.2, maxEch));              
             channelRawWaveform.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy : " + (column) + " "+ (row)+ ": "+ii));
             //the above instruction is a terrible hack, just to fill the arrayList with all the elements. They'll be initialized properly during the event readout,
             //since we want to account for possibly different raw waveform dimensions!
             //channelRawWaveform.add(aida.cloud1D(detector.getDetectorName() + " : " + inputCollection + " : Raw Waveform : " + (column) + " "+ (row)+ ": "+ii,1000000000));
             
             isFirstRaw[ii]=true;
             windowRaw[ii]=1;
       }
       id=0;
       iy=ECalUtils.getRowFromHistoID(id);
   	   ix=ECalUtils.getColumnFromHistoID(id);  
   	   
   	   
    	plotterFactory = aida.analysisFactory().createPlotterFactory("Single channel");       
        plotter = plotterFactory.create("Single channel");
   	    pstyle = this.createDefaultStyle(); 		
        plotter.setTitle("");
   
     
        plotter.createRegions(2,2);


        pstyle.xAxisStyle().setLabel("Hit energy (GeV)");
        pstyle.yAxisStyle().setLabel("");
        plotter.region(0).plot(channelEnergyPlot.get(id),pstyle);
   
        pstyle.xAxisStyle().setLabel("Hit Time (ns)");
        pstyle.yAxisStyle().setLabel("");    
        plotter.region(1).plot(channelTimePlot.get(id),pstyle);           	    
        
        pstyle.xAxisStyle().setLabel("Hit Time (ns)");
        pstyle.yAxisStyle().setLabel("Hit Energy (GeV)");    
    	plotter.region(2).plot(channelTimeVsEnergyPlot.get(id),pstyle);

    	pstyle.xAxisStyle().setLabel("Hit Energy (GeV)");    
        pstyle.yAxisStyle().setLabel("");
        pstyle.dataStyle().fillStyle().setColor("orange");
        pstyle.dataStyle().markerStyle().setColor("orange");
        pstyle.dataStyle().errorBarStyle().setVisible(false);
	    plotter.region(3).plot(channelRawWaveform.get(id),pstyle);
	
        
        
        System.out.println("Create the event viewer");
        viewer=new PEventViewer();
        viewer.addCrystalListener(this);
        viewer.setScaleMinimum(minEch);
        viewer.setScaleMaximum(maxEch);
        System.out.println("Done");
        
      
        plotter.show();
       
        viewer.setVisible(true);
        
        prevTime=0; //init the time 
        thisTime=0; //init the time 
    }

    @Override
    public void endOfData() {
        viewer.setVisible(false);
        viewer.dispose();
    }

    @Override
    public void process(EventHeader event){
    	
    	  int ii;
          int row = 0;
          int column = 0;
          double[] result;
          
          boolean do_update=false;
          thisTime=System.currentTimeMillis()/1000;
          
          if ((thisTime-prevTime)>eventRefreshRate){
        	  prevTime=thisTime;
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
                if ((row!=0)&&(column!=0)){
                    ii = ECalUtils.getHistoIDFromRowColumn(row,column);
                    hitE=hit.getCorrectedEnergy();
                    if (hitE > 0) { //A.C. > 0 for the 2D plot drawing                 	
                    	channelEnergyPlot.get(ii).fill(hit.getCorrectedEnergy());
                        channelTimePlot.get(ii).fill(hit.getTime());
                        channelTimeVsEnergyPlot.get(ii).fill(hit.getTime(),hit.getCorrectedEnergy());                                    
                    }
                    if ((do_update)){
                    	if ((hitE>minEch)&&(hitE<maxEch)){
                    		viewer.addHit(new EcalHit(column,row, hitE));  //before was in >0 check
                    	}
                    	else if (hitE>maxEch){
                    		viewer.addHit(new EcalHit(column,row, maxEch));  
                    	}                	
                    }
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
                	hitE=hit.getCorrectedEnergy();
                	if ((hitE>minEch)&&(hitE<maxEch)){               
                		column=hit.getIdentifierFieldValue("ix");
                        row=hit.getIdentifierFieldValue("iy");                	
                        the_cluster.addComponentHit(hit.getIdentifierFieldValue("ix"),hit.getIdentifierFieldValue("iy"));
                    }
                }         
                viewer.addCluster(the_cluster);
               }
            }
        }
        
        //here follows the code for the raw waveform
        if (event.hasCollection(RawTrackerHit.class, inputCollectionRaw)){       	
        	List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollectionRaw);
        	for (RawTrackerHit hit : hits) {
        		 row=hit.getIdentifierFieldValue("iy");
                 column=hit.getIdentifierFieldValue("ix");
                 if ((row!=0)&&(column!=0)){
                	 if (!ECalUtils.isInHole(row,column)){
                	 
                		 ii = ECalUtils.getHistoIDFromRowColumn(row,column);
                		 if (isFirstRaw[ii]){ //at the very first hit we read for this channel, we need to read the window length and save it
                			 isFirstRaw[ii]=false;
                			 windowRaw[ii]=hit.getADCValues().length;                   	 
                			 channelRawWaveform.set(ii,aida.histogram1D(detector.getDetectorName() + " : " + inputCollectionRaw + " : Raw Waveform : " + (column) + " "+ (row)+ ": "+ii,windowRaw[ii],-0.5*ECalUtils.ecalReadoutPeriod,(-0.5+windowRaw[ii])*ECalUtils.ecalReadoutPeriod));
                		 }
                		 if (do_update){
                			 channelRawWaveform.get(ii).reset();                     
                			 for (int jj = 0; jj < windowRaw[ii]; jj++) {
                				 channelRawWaveform.get(ii).fill(jj*ECalUtils.ecalReadoutPeriod, hit.getADCValues()[jj]*ECalUtils.adcResolution*1000);
                			 }                  
                			 result=ECalUtils.computeAmplitude(hit.getADCValues(),windowRaw[ii],pedSamples);
                			 channelRawWaveform.get(ii).setTitle("Ampl: "+String.format("%.2f",result[0])+" mV , ped : "+String.format("%.2f",result[1])+" "+String.format("%.2f",result[2])+" ADC counts");
                			 plotter.region(3).refresh();
                		 } 	
                	 }
                 }
            }
        }
        
        
        if (do_update){
          viewer.updateDisplay(); 
        }
    }
    
    /*
    @Override
    public void reset(){
        for(int ii = 0; ii < (47*11); ii = ii +1){         
            channelEnergyPlot.get(ii).reset();
            channelTimePlot.get(ii).reset();
            channelTimeVsEnergyPlot.get(ii).reset();
        }
    }
    */
    
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
    	itmpx=(int) ecalPoint.getX(); //column
    	itmpy=(int) ecalPoint.getY(); //row
    	
    	if ((itmpx!=0)&&(itmpy!=0))
    		if (!ECalUtils.isInHole(itmpy,itmpx)){
    			ix=itmpx;
    			iy=itmpy;
    			id=ECalUtils.getHistoIDFromRowColumn(iy,ix);
    			System.out.println("Crystal event: "+ix+" "+iy+" "+id);
        
    	    
    	    
    	    
    	    
    			plotter.region(0).clear();
    			pstyle.xAxisStyle().setLabel("Hit energy (GeV)");
    			pstyle.yAxisStyle().setLabel("");
    			plotter.region(0).plot(channelEnergyPlot.get(id),pstyle);
       
    			plotter.region(1).clear();
    			pstyle.xAxisStyle().setLabel("Hit Time (ns)");
    			pstyle.yAxisStyle().setLabel("");    
    			plotter.region(1).plot(channelTimePlot.get(id),pstyle);           	    
     
    			plotter.region(2).clear();
    			pstyle.xAxisStyle().setLabel("Hit Time (ns)");
    			pstyle.yAxisStyle().setLabel("Hit Energy (GeV)");    
    			plotter.region(2).plot(channelTimeVsEnergyPlot.get(id),pstyle);

    			plotter.region(3).clear();
        	
        	 
    			if (!isFirstRaw[id]){
    				pstyle.yAxisStyle().setLabel("Signal amplitude (mV)");
    				pstyle.xAxisStyle().setLabel("Time (ns)");
    				pstyle.dataStyle().fillStyle().setColor("orange");
    				pstyle.dataStyle().markerStyle().setColor("orange");
    				pstyle.dataStyle().errorBarStyle().setVisible(false);
    			}
    			else{
    				pstyle.xAxisStyle().setLabel("Hit Energy (GeV)");    
    				pstyle.yAxisStyle().setLabel("");  	    
    			}
    			plotter.region(3).plot(channelRawWaveform.get(id),pstyle);       
    	}
    }    
    
    
    /*
     * This method set the default style.
     */
    public IPlotterStyle createDefaultStyle() {
    	IPlotterStyle pstyle = plotterFactory.createPlotterStyle();
    	// Axis appearence.
    	pstyle.xAxisStyle().labelStyle().setBold(true);
    	pstyle.yAxisStyle().labelStyle().setBold(true);
    	pstyle.xAxisStyle().tickLabelStyle().setBold(true);
    	pstyle.yAxisStyle().tickLabelStyle().setBold(true);
    	pstyle.xAxisStyle().lineStyle().setColor("black");
    	pstyle.yAxisStyle().lineStyle().setColor("black");
    	pstyle.xAxisStyle().lineStyle().setThickness(2);
    	pstyle.yAxisStyle().lineStyle().setThickness(2);
    	
    	pstyle.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
    	pstyle.dataStyle().fillStyle().setParameter("showZeroHeightBins",Boolean.FALSE.toString());
    	pstyle.dataStyle().errorBarStyle().setVisible(false);
        pstyle.setParameter("hist2DStyle", "colorMap");
    	// Force auto range to zero.
    	pstyle.yAxisStyle().setParameter("allowZeroSuppression", "false");
    	pstyle.xAxisStyle().setParameter("allowZeroSuppression", "false");
    	// Title style.
    	pstyle.titleStyle().textStyle().setFontSize(20);
    	// Draw caps on error bars.
    	pstyle.dataStyle().errorBarStyle().setParameter("errorBarDecoration", (new Float(1.0f)).toString());
    	// Turn off grid lines until explicitly enabled.
    	pstyle.gridStyle().setVisible(false);
    	return pstyle;
    	}   
}
