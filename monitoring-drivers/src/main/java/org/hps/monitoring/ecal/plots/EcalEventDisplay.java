package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

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
 *  The driver <code>EcalEvendDisplay</code> implements the histogram shown to the user 
 * in the fifth tab of the Monitoring Application, when using the Ecal monitoring lcsim file.
 * IT ALSO OPENS KYLE's EVENT DISPLAY <code>PEventViewer</code>.
 * The implementation is as follows:
 * - The event display is opened in a separate window
 * - It is updated regularly, according to the event refresh rate
 * - If the user clicks on a crystal, the corresponding energy and time distributions (both Histogram1D) are shown in the last panel of the MonitoringApplication,
 * as well as a 2D histogram (hit time vs hit energy). Finally, if available, the raw waveshape (in mV) is displayed.
 * 
 * The single channel plots are created in the  <code>EcalHitPlots</code> driver.
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
	
    int[] windowRaw=new int[47*11];//in case we have the raw waveform, this is the window lenght (in samples)
	boolean[] isFirstRaw=new boolean[47*11];
	
    private PEventViewer viewer; //this is the Kyle event viewer.    

    
    ArrayList<IHistogram1D> channelEnergyPlot;
    ArrayList<IHistogram1D> channelTimePlot;
    ArrayList<IHistogram1D> channelRawWaveform;
   // ArrayList<ICloud1D> channelRawWaveform;
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
       //these are NOT shown in this plotter, but are used in the event display.
       for(int ii = 0; ii < (47*11); ii = ii +1){
             int row=ECalUtils.getRowFromHistoID(ii);
             int column=ECalUtils.getColumnFromHistoID(ii);      
             channelEnergyPlot.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy : " + (column) + " "+ (row)+ ": "+ii, 100, 0, maxEch));  
             channelTimePlot.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time : " + (column) + " "+ (row)+ ": "+ii, 100, 0, 400));     
             channelTimeVsEnergyPlot.add(aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time Vs Energy : " + (column) + " "+ (row)+ ": "+ii, 100, 0, 400,100, 0, maxEch));              
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
   	   
   	   
    	
    			
    			
    			
    	plotterFactory = aida.analysisFactory().createPlotterFactory("Ecal single channel plots");
   
       
        plotter = plotterFactory.create("Single hits");
        plotter.setTitle("");
        plotter.style().setParameter("hist2DStyle", "colorMap");
        plotter.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter.style().dataStyle().fillStyle().setParameter("showZeroHeightBins",Boolean.FALSE.toString());
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2,2);
        plotter.region(0).plot(channelEnergyPlot.get(0));
        plotter.region(1).plot(channelTimePlot.get(0));
        plotter.region(2).plot(channelTimeVsEnergyPlot.get(0));
        plotter.region(3).plot(channelRawWaveform.get(0));
        plotter.region(3).style().yAxisStyle().setLabel("Amplitude (mV)");
        plotter.region(3).style().xAxisStyle().setLabel("Time (ns)");
        plotter.region(3).style().dataStyle().fillStyle().setColor("orange");
        plotter.region(3).style().dataStyle().markerStyle().setColor("orange");
        plotter.region(3).style().dataStyle().errorBarStyle().setVisible(false);
        
        
        System.out.println("Create the event viewer");
        viewer=new PEventViewer();
        viewer.addCrystalListener(this);
        System.out.println("Done");
        
      
        plotter.show();
       
        viewer.setVisible(true);
        
    }

    @Override
    public void endOfData() {
        viewer.setVisible(false);
        viewer.dispose();
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
                if ((row!=0)&&(column!=0)){
                    ii = ECalUtils.getHistoIDFromRowColumn(row,column);
                    if (hit.getCorrectedEnergy() > 0) { //A.C. > 0 for the 2D plot drawing                 	
                    	channelEnergyPlot.get(ii).fill(hit.getCorrectedEnergy());
                        channelTimePlot.get(ii).fill(hit.getTime());
                        channelTimeVsEnergyPlot.get(ii).fill(hit.getTime(),hit.getCorrectedEnergy());        
                        if (do_update) viewer.addHit(new EcalHit(column,row, hit.getCorrectedEnergy()));         
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
                    if (hit.getRawEnergy() > 0) 
                        column=hit.getIdentifierFieldValue("ix");
                        row=hit.getIdentifierFieldValue("iy");                	
                        the_cluster.addComponentHit(hit.getIdentifierFieldValue("ix"),hit.getIdentifierFieldValue("iy"));
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
    
    	int itmpx,itmpy;
    	Point displayPoint,ecalPoint;
    	displayPoint=e.getCrystalID();
    	ecalPoint=viewer.toEcalPoint(displayPoint);
    	itmpx=(int) ecalPoint.getX(); //column
    	itmpy=(int) ecalPoint.getY(); //row
    	
    	if (!ECalUtils.isInHole(itmpy,itmpx)){
    		ix=itmpx;
    		iy=itmpy;
    	    id=ECalUtils.getHistoIDFromRowColumn(iy,ix);
    	    System.out.println("Crystal event: "+ix+" "+iy+" "+id);
        
    	
       	
           	plotter.region(0).clear();
            plotter.region(0).plot(channelEnergyPlot.get(id));
    
            plotter.region(1).clear();
            plotter.region(1).plot(channelTimePlot.get(id));           	    
   

    	    plotter.region(2).clear();
        	plotter.region(2).plot(channelTimeVsEnergyPlot.get(id));
    	
    	    plotter.region(3).clear();
    	    plotter.region(3).plot(channelRawWaveform.get(id));
    	}
    }    
}

