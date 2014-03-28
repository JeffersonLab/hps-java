package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.util.ArrayList;
import java.util.List;

import org.hps.util.Resettable;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
/*Conditions system imports*/
import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.DefaultTestSetup;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannelConstants;



/**
 * The driver <code>EcalDaqPlots</code> implements the histogram shown to the user 
 * in the fourth tab of the Monitoring Application, when using the Ecal monitoring lcsim file.
 * It contains only a sub-tab, showing the number of hits recorded by the different FADC channels.
 * It is a very preliminary driver to monitor the DAQ status.
 * These plots are updated continuosly.
 * @author Andrea Celentano
 * @TODO: integrate with the new conditions system.
 *
 */

public class EcalDaqPlots extends Driver implements Resettable {

    private String subdetectorName = "Ecal";
    private String inputCollection = "EcalReadoutHits";
    private IPlotter plotter;
    private AIDA aida;
    private Detector detector;
    private List<IHistogram1D> plots;
    private static final short[] slots = {10, 13, 9, 14, 8, 15, 7, 16, 6, 17, 5, 18, 4, 19};

    private EcalChannel.EcalChannelCollection channels;
    private DatabaseConditionsManager manager;
    public EcalDaqPlots() {
    	manager = DatabaseConditionsManager.getInstance();
    }

    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void detectorChanged(Detector detector) {

    	
    
    	
    	
    	
        this.detector = detector;
    	/*Setup the conditions system*/
        
    	 // Get the channel information from the database.                
        channels = manager.getCachedConditions(EcalChannel.EcalChannelCollection.class, "ecal_channels").getCachedData();
     
        
        if (subdetectorName == null) {
            throw new RuntimeException("The subdetectorName parameter was not set.");
        }

        if (inputCollection == null) {
            throw new RuntimeException("The inputCollection parameter was not set.");
        }

        Subdetector subdetector = detector.getSubdetector(subdetectorName);
        
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        plots = new ArrayList<IHistogram1D>();
        
        
        for (int i = 1; i < 3; i++) { // crate
            for (int j = 0; j < 14; j++) { // slot           
                plots.add(aida.histogram1D("ECAL: Crate " + i + "; Slot " + slots[j], 16, 0, 16));
            }
        }

        IPlotterFactory factory= aida.analysisFactory().createPlotterFactory("ECAL DAQ Plots");
        plotter =factory.create("DAQ Plots");
        IPlotterStyle pstyle = plotter.style();
        pstyle.dataStyle().fillStyle().setColor("orange");
        pstyle.dataStyle().markerStyle().setColor("orange");
        pstyle.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(7, 4);
        
        int id,plot_id;
        for (int i = 1; i < 3; i++) { // crate
            for (int j = 0; j < 14; j++) { // slot               
                //System.out.println("creating plot: " + "ECAL: Crate " + j + "; Slot " + i + " in region " + region);
                id = (i-1)*14+(j);
                plot_id = 0;
                if (i==1){
                    if (j%2==0) plot_id=j*2;
                    else plot_id=(j-1)*2+1;
                }
                else if (i==2){
                    if (j%2==0) plot_id=j*2+2;
                    else plot_id=(j-1)*2+3;
                }
                        
                plotter.region(plot_id).plot(plots.get(id));
            }
        }
        plotter.show();
    }
    
    @Override
    public void reset() {
        if (plotter != null) {
            for (IHistogram1D plot : plots) {
                plot.reset();
            }
        }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {
            	
           

            	// Make an ID to find.
          //  	EcalChannel daq = new EcalChannel.DaqId();
            //	daq.crate = 1;
            //	daq.slot = 2;
           // 	daq.channel = 3;

            	// Find the matching channel.                                     	
            	EcalChannel.GeometryId geomID=new EcalChannel.GeometryId();
            	geomID.x=hit.getIdentifierFieldValue("ix");
            	geomID.y=hit.getIdentifierFieldValue("iy");
            	EcalChannel channel=channels.findChannel(geomID);
            			
            	int crateN=channel.getCrate();
            	int slotN=channel.getSlot();
            	int channelN=channel.getChannel();

            	System.out.println("found channel at " + geomID.x + " " + geomID.y + " corresponding to DAQ crate/slot/channel " + crateN + " "+slotN+" "+channelN);
            }
        }
    }
    
    
    
    public int getSlotIndex(int slot){
        int ret=-1;
        for (int ii=0;ii<14;ii++){
            if (slots[ii]==slot) ret=ii;
        }
        return ret;
    }
    
    
    
    
    
}
