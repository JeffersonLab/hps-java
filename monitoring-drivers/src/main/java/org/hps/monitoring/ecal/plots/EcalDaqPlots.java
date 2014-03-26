package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.deprecated.EcalConditions;
import org.hps.util.Resettable;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
//THIS IS THE OLD ONE
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

    public EcalDaqPlots() {
    }

    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void detectorChanged(Detector detector) {

        this.detector = detector;

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
            	
            	//EcalChannel.GeometryId geomID;
            	//EcalChannel channel;
            	//geomID.x=hit.getIdentifierFieldValue("ix");
            	//geomID.y=hit.getIdentifierFieldValue("iy");
            	//channel=EcalChannel.findChannel(geomID);
            			
                Long daqId = EcalConditions.physicalToDaqID(hit.getCellID());
                int crate = EcalConditions.getCrate(daqId);
                int slot = EcalConditions.getSlot(daqId);
                int channel = EcalConditions.getChannel(daqId);
                int id = getSlotIndex(slot)+(crate-1)*14;
                plots.get(id).fill(channel);
            }
        }
/*        
 *          if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
            for (RawTrackerHit hit : hits) {
                Long daqId = EcalConditions.physicalToDaqID(hit.getCellID());
                int crate = EcalConditions.getCrate(daqId);
                int slot =  EcalConditions.getSlot(daqId);
                int channel = EcalConditions.getChannel(daqId);
                int id = getSlotIndex(slot)+(crate-1)*14;
                plots.get(id).fill(channel);
            }
        }
        */
    }
    
    
    
    public int getSlotIndex(int slot){
        int ret=-1;
        for (int ii=0;ii<14;ii++){
            if (slots[ii]==slot) ret=ii;
        }
        return ret;
    }
    
    
    
    
    
}
