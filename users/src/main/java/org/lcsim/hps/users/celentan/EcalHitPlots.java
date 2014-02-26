package org.lcsim.hps.users.celentan;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ref.plotter.PlotterUtilities;

import java.util.List;
import java.util.ArrayList;


import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.evio.TriggerData;
import org.lcsim.hps.monitoring.deprecated.Resettable;
import org.lcsim.hps.recon.ecal.ECalUtils;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import org.freehep.swing.popup.GlobalMouseListener;
import org.freehep.swing.popup.GlobalPopupListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent; 

import javax.swing.JPanel;

public class EcalHitPlots extends Driver implements Resettable,MouseListener{

    //AIDAFrame plotterFrame;
    String inputCollection = "EcalCalHits";
    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter, plotter2, plotter3, plotter4;

    IHistogram1D hitCountPlot;
    IHistogram1D hitTimePlot;
    IHistogram1D hitEnergyPlot;
    IHistogram1D hitMaxEnergyPlot;
    IHistogram1D topTimePlot, botTimePlot, orTimePlot;
    IHistogram1D topTrigTimePlot, botTrigTimePlot, orTrigTimePlot;
    IHistogram2D topTimePlot2D, botTimePlot2D, orTimePlot2D;
   // IHistogram2D topX, botX, topY, botY;
    IHistogram2D edgePlot;
    
    PlotterUtilities thePlotterUtilities;
  
    //Plotter5
    IPlotter plotter5;
    ArrayList<IHistogram1D> channelEnergyPlot;
    GlobalMouseListener plotter5listener;
    
    int eventn = 0;
    int dummy = 0;
    double maxE = 5000 * ECalUtils.MeV;
    double maxEch = 2500 * ECalUtils.MeV;
    boolean logScale = false;
    boolean hide = false;
    
    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setMaxE(double maxE) {
        this.maxE = maxE;
    }
    
    public void setMaxEch(double maxEch) {
        this.maxEch = maxEch;
    }
    
    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }

    @Override
    protected void detectorChanged(Detector detector) {
    	
      //  plotterFrame = new AIDAFrame();
       // plotterFrame.setTitle("HPS ECal Hit Plots");
    	System.out.println("qui: "+ detector.getClass().getName());
    	aida.tree().cd("/");
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("Ecal Hit Plots");

        // Setup the plotter.
        plotter = plotterFactory.create("Hit Counts");
        plotter.setTitle("Hit Counts");
     //   plotterFrame.addPlotter(plotter);
        plotter.style().dataStyle().errorBarStyle().setVisible(false);

        // Setup plots.
        hitCountPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Count In Event", 10, -0.5, 9.5);
        hitTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time", 100, 0 * 4.0, 100 * 4.0);

        // Create the plotter regions.
        plotter.createRegions(1, 2);
        plotter.region(0).plot(hitCountPlot);
        plotter.region(1).plot(hitTimePlot);
     
      
        // Setup the plotter.
        plotter2 = plotterFactory.create("Hit Energies");
        plotter2.setTitle("Hit Energies");
      //  plotter2.addMouseListener(this);
     //   plotterFrame.addPlotter(plotter2);
        plotter2.style().dataStyle().errorBarStyle().setVisible(false);

        hitEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy", 1000, -0.1, maxE);
        hitMaxEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Maximum Hit Energy In Event", 1000, -0.1, maxE);

//        hitEnergyPlot.addMouseListener(this);
        
        if (logScale) {
            plotter2.style().yAxisStyle().setParameter("scale", "log");
        }

        // Create the plotter regions.
        plotter2.createRegions(1, 2);
        plotter2.region(0).plot(hitEnergyPlot);
        plotter2.region(1).plot(hitMaxEnergyPlot);

        //A. Celentano
        plotter5 = plotterFactory.create("Hit Energies - per channel");
        plotter5.setTitle("Hit Energies - per channel");
        plotter5.style().dataStyle().errorBarStyle().setVisible(false);
        plotter5.createRegions(47,11); //1 more, to have raw 0 and column 0 empty.
      
        plotter5listener=new GlobalMouseListener(thePlotterUtilities.componentForPlotter(plotter5));
        plotter5listener.addMouseListener(this);
        
        
        channelEnergyPlot=new ArrayList<IHistogram1D>();
      
        
        
    
       // plotter5Listener.addMouseListener(this);
        
        for(int id = 0; id < (47*11); id = id +1){
        	  
        	  int row=this.getRowFromHistoID(id);
        	  int column=this.getColumnFromHistoID(id);      
        	  
        	  //create the histogram, the global listener, and the specific listener.
        	  channelEnergyPlot.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Energy : " + row + " "+ column+ ": "+id, 1000, -0.1, maxEch));
        
        	  
        	  if ((row!=0)&&(column!=0)&&(!isInHole(row,column))){
        		  plotter5.region(id).plot(channelEnergyPlot.get(id));       	
        	  }
        	 }
               
        plotter3 = plotterFactory.create("Hit Times");
        plotter3.setTitle("Hit Times");
      //  plotterFrame.addPlotter(plotter3);
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(3, 3);

        topTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Top", 100, 0, 100 * 4.0);
        botTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Bottom", 100, 0, 100 * 4.0);
        orTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Or", 100, 0, 100 * 4.0);

        topTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Top", 32, 0, 32);
        botTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Bottom", 32, 0, 32);
        orTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Or", 32, 0, 32);

        topTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Top", 100, 0, 100 * 4.0, 32, 0, 32);
        botTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Bottom", 100, 0, 100 * 4.0, 32, 0, 32);
        orTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Or", 100, 0, 100 * 4.0, 32, 0, 32);

        // Create the plotter regions.
        plotter3.region(0).plot(topTimePlot);
        plotter3.region(1).plot(botTimePlot);
        plotter3.region(2).plot(orTimePlot);
        plotter3.region(3).plot(topTrigTimePlot);
        plotter3.region(4).plot(botTrigTimePlot);
        plotter3.region(5).plot(orTrigTimePlot);
        for (int i = 0; i < 6; i++) {
            if (plotter3.region(i).style() != null) {
                plotter3.region(i).style().yAxisStyle().setParameter("scale", "log");
            }
        }
        plotter3.region(6).plot(topTimePlot2D);
        plotter3.region(7).plot(botTimePlot2D);
        plotter3.region(8).plot(orTimePlot2D);
        for (int i = 6; i < 9; i++) {
            if (plotter3.region(i).style() != null) {
                plotter3.region(i).style().setParameter("hist2DStyle", "colorMap");
                plotter3.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                plotter3.region(i).style().zAxisStyle().setParameter("scale", "log");
            }
        }

        plotter4 = plotterFactory.create("Edges");
        plotter4.setTitle("Edges");
      //  plotterFrame.addPlotter(plotter4);
        plotter4.style().setParameter("hist2DStyle", "colorMap");
        plotter4.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter4.style().zAxisStyle().setParameter("scale", "log");
        plotter4.createRegion();

        edgePlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Pairs Across Crystal Edges", 93, -23.25, 23.25, 21, -5.25, 5.25);
        plotter4.region(0).plot(edgePlot);

        
        if (!hide) {
        	plotter.show();
        	plotter2.show();
        	plotter3.show();
        	plotter4.show();
        	plotter5.show();
        }
        //plotterFrame.setVisible(true);
        //plotterFrame.pack();
    }

    @Override
    public void process(EventHeader event) {
    	
    	
        int orTrigTime = -1;
        int topTrigTime = -1;
        int botTrigTime = -1;
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            if (!triggerList.isEmpty()) {
                GenericObject triggerData = triggerList.get(0);

                int orTrig = TriggerData.getOrTrig(triggerData);
                if (orTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & orTrig) != 0) {
                            orTrigTime = i;
                            orTrigTimePlot.fill(i);
                            break;
                        }
                    }
                }
                int topTrig = TriggerData.getTopTrig(triggerData);
                if (topTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & topTrig) != 0) {
                            topTrigTime = i;
                            topTrigTimePlot.fill(i);
                            break;
                        }
                    }
                }
                int botTrig = TriggerData.getBotTrig(triggerData);
                if (botTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & botTrig) != 0) {
                            botTrigTime = i;
                            botTrigTimePlot.fill(i);
                            break;
                        }
                    }
                }
            }
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
                if (hit.getIdentifierFieldValue("iy") > 0) {
      //              topX.fill(hit.getIdentifierFieldValue("ix"),hit.getPosition()[0]);
      //              topY.fill(hit.getIdentifierFieldValue("iy"),hit.getPosition()[1]);
                } else {
       //             botX.fill(hit.getIdentifierFieldValue("ix"),hit.getPosition()[0]);
       //             botY.fill(hit.getIdentifierFieldValue("iy"),hit.getPosition()[1]);                    
                }
               
                hitEnergyPlot.fill(hit.getRawEnergy());
                hitTimePlot.fill(hit.getTime());
                
                
                column=hit.getIdentifierFieldValue("ix");
                row=-hit.getIdentifierFieldValue("iy"); //sign "-" is due to AIDA having region 0 TOP-LEFT, while my numbering is with (0,0) bottom-left.
                if ((hit.getIdentifierFieldValue("ix")!=0)&&(hit.getIdentifierFieldValue("iy")!=0)){
                  	id = this.getHistoIDFromRowColumn(row,column);
                	//System.out.println("HIT: "+column+" "+row+" "+id);
                	channelEnergyPlot.get(id).fill(hit.getCorrectedEnergy());
                }
                
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
            for (int i = 0; i < hits.size(); i++) {
                CalorimeterHit hit1 = hits.get(i);
                int x1 = hit1.getIdentifierFieldValue("ix");
                int y1 = hit1.getIdentifierFieldValue("iy");
                for (int j = i + 1; j < hits.size(); j++) {
                    CalorimeterHit hit2 = hits.get(j);
                    int x2 = hit2.getIdentifierFieldValue("ix");
                    int y2 = hit2.getIdentifierFieldValue("iy");
                    if ((Math.abs(x1 - x2) <= 1 || x1 * x2 == -1) && (Math.abs(y1 - y2) <= 1)) {
                        if (x1 != x2 || y1 != y2) {
                            edgePlot.fill((x1 + x2) / 2.0, (y1 + y2) / 2.0);
                        }
                    }
                }
            }
        } else {
            hitCountPlot.fill(0);
        }
    }

    @Override
    public void reset() {
        hitCountPlot.reset();
        hitTimePlot.reset();
        hitEnergyPlot.reset();
        hitMaxEnergyPlot.reset();
        for(int id = 0; id < (47*11); id = id +1){   	  
      	  channelEnergyPlot.get(id).reset();
        }
    }

    @Override
    public void endOfData() {
        //plotterFrame.dispose();
    }
    
    public int getRowFromHistoID(int id){
        return ((id%11)-5);
    }

    public int getColumnFromHistoID(int id){
    	return ((id/11)-23);
    }
    
    public int getHistoIDFromRowColumn(int row,int column){
    	return (row+5)+11*(column+23);
    }
    
    public Boolean isInHole(int row,int column){
    	Boolean ret;
    	ret=false;
    	if ((row==1)||(row==-1)){
    		if ((column<=-2)&&(column>=-8)) ret=true;
    	}
    	return ret;	
    }
    
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {
        switch(e.getModifiers()) {
          case InputEvent.BUTTON1_MASK: {
            System.out.println("That's the LEFT button");    
            
            break;
            }
          case InputEvent.BUTTON2_MASK: {
            System.out.println("That's the MIDDLE button");     
            System.out.println(e.getX()+" "+e.getY());
            System.out.println(e.getSource().toString());
            System.out.println(((JPanel)e.getSource()).getComponent(0).toString());
//            System.out.println(((IHistogram1D)e.getSource()).title());
            break;
            }
          case InputEvent.BUTTON3_MASK: {
            System.out.println("That's the RIGHT button");     
            break;
            }
          }
        }   
}

   