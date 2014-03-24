package org.lcsim.hps.users.celentan;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.IPlotterFactory;
import hep.aida.ref.plotter.PlotterUtilities;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Point;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.lcsim.geometry.Detector;
import org.lcsim.hps.monitoring.deprecated.AIDAFrame;
import org.lcsim.hps.monitoring.ecal.ui.PEventViewer;
import org.lcsim.hps.monitoring.ecal.event.Cluster;
import org.lcsim.hps.monitoring.ecal.event.EcalHit;
import org.lcsim.hps.recon.ecal.EcalConditions;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
//import org.lcsim.hps.users.celentan.EcalEventDisplayListener;
import org.lcsim.hps.monitoring.ecal.util.CrystalEvent;
import org.lcsim.hps.monitoring.ecal.util.CrystalListener;
import org.lcsim.hps.users.celentan.EcalMonitoringUtils;


public class EcalEventDisplay extends Driver implements CrystalListener,ActionListener {

  
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

    IHistogram1D hEnergy,hEnergyDraw,hTime,hTimeDraw;
    
    public EcalEventDisplay() {
    	
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }
    

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }
    
    @Override
    public void detectorChanged(Detector detector) {
    	
        this.detector = detector;
    	
    	aida.tree().cd("/");
    	
    	id=0;
       iy=EcalMonitoringUtils.getRowFromHistoID(id);
   	   ix=EcalMonitoringUtils.getColumnFromHistoID(id);     
    	
       	hEnergy = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy : " + (iy) + " "+ (ix)+ ": "+id);
       	hTime = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time : " + (iy) + " "+ (ix)+ ": "+id);
       			
      	
    	hEnergyDraw=aida.histogram1D("hEnergy",hEnergy.axis().bins(),hEnergy.axis().lowerEdge(),hEnergy.axis().upperEdge());
    	hTimeDraw=aida.histogram1D("hTime",hTime.axis().bins(),hTime.axis().lowerEdge(),hTime.axis().upperEdge());
 		
    			
    			
    			
    			
    			
    	plotterFactory = aida.analysisFactory().createPlotterFactory("Ecal single channel plots");
   
       
        plotter = plotterFactory.create("Single hits");
        plotter.setTitle("");
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2,2);
        plotter.region(0).plot(hEnergyDraw);
        plotter.region(1).plot(hTimeDraw);
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
    	
    	  if (++eventn % eventRefreshRate != 0) {
              return;
          }
    	
    	viewer.resetDisplay();
    	viewer.updateDisplay();
    	
    	viewer.printSize();
    	Cluster the_cluster;
    	
        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
        	List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {
                viewer.addHit(new EcalHit(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getRawEnergy()));
            }
        }
        if (event.hasCollection(HPSEcalCluster.class, clusterCollection)) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollection);
            for (HPSEcalCluster cluster : clusters) {
                CalorimeterHit seedHit = cluster.getSeedHit();
                the_cluster=new Cluster(seedHit.getIdentifierFieldValue("ix"), seedHit.getIdentifierFieldValue("iy"), cluster.getEnergy());
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    if (hit.getRawEnergy() != 0) 
                     the_cluster.addComponentHit(hit.getIdentifierFieldValue("ix"),hit.getIdentifierFieldValue("iy"));
                }         
                viewer.addCluster(the_cluster);
            }
        }
        
       
        viewer.updateDisplay();
        
        
        //need also to update single-hit histograms, since they're just a drawing copy!
        //get the new histograms
       	hEnergy = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy : " + (iy) + " "+ (ix)+ ": "+id);
    	//clone hEnergyDraw
       	hEnergyDraw.reset();
       	hEnergyDraw.setTitle(hEnergy.title());
       	hEnergyDraw.add(hEnergy);
       	
    	hTime = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time : " + (iy) + " "+ (ix)+ ": "+id);
       	hTimeDraw.reset();
       	hTimeDraw.setTitle(hTime.title());
       	hTimeDraw.add(hTime);
        
          
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
       	hEnergy = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy : " + (iy) + " "+ (ix)+ ": "+id);
    	//clone hEnergyDraw
       	hEnergyDraw.reset();
        plotter.region(0).setTitle(hEnergy.title());
       	hEnergyDraw.setTitle(hEnergy.title());
       	hEnergyDraw.add(hEnergy);
       	
    	hTime = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time : " + (iy) + " "+ (ix)+ ": "+id);
       	hTimeDraw.reset();
        plotter.region(1).setTitle(hTime.title());
       	hTimeDraw.setTitle(hTime.title());
       	hTimeDraw.add(hTime);
           	
        //A. Celentano: it seems in AIDA there is NOT a single way to plot another histogram in a region where an histogram is already plotted.
      
    //   	plotter.style().dataStyle().errorBarStyle().setVisible(false);
   //     plotter.refresh();
   //     plotter.show();
        //hTime = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Occupancy");
	}    
}

