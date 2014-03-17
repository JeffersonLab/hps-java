package org.lcsim.hps.users.celentan;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;
import jas.hist.JASHist;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.hps.monitoring.deprecated.Resettable;
import org.lcsim.hps.recon.ecal.EcalConditions;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

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
				/*JASHist jhist = ((PlotterRegion) plotter.region(region)).getPlot();
				jhist.setAllowUserInteraction(false);
				jhist.setAllowPopupMenus(false);
				*/
			}
		}
		plotter.show();
	}

//	public void endOfData() {
//		if (plotter != null) {
//			plotter.hide();
//		}
//	}

	public void reset() {
		if (plotter != null) {
		//	plotter.hide();
		//	plotter.destroyRegions();
			for (IHistogram1D plot : plots) {
				plot.reset();
			}
			//detectorChanged(detector);
		}
	}

	public void process(EventHeader event) {
		if (event.hasCollection(RawCalorimeterHit.class, inputCollection)) {
			List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, inputCollection);
			for (RawCalorimeterHit hit : hits) {
				Long daqId = EcalConditions.physicalToDaqID(hit.getCellID());
				int crate = EcalConditions.getCrate(daqId);
				int slot = EcalConditions.getSlot(daqId);
				int channel = EcalConditions.getChannel(daqId);
				int id = getSlotIndex(slot)+(crate-1)*14;
				
				//System.out.println("crate="+crate+"; slot="+slot+"; channel="+channel);
				//System.out.println("filling plot: " + "ECAL: Crate " + crate + "; Slot " + slot+ "(" + getSlotIndex(slot)+ ")"+" id: "+id );	
				plots.get(id).fill(channel);
			}
		}
		if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
			List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
			for (RawTrackerHit hit : hits) {
				Long daqId = EcalConditions.physicalToDaqID(hit.getCellID());
				int crate = EcalConditions.getCrate(daqId);
				int slot =  EcalConditions.getSlot(daqId);
				int channel = EcalConditions.getChannel(daqId);
				//System.out.println("crate="+crate+"; slot="+slot+"; channel="+channel);
				//System.out.println("filling plot: " + "ECAL: Crate " + crate + "; Slot " + slot);
				int id = getSlotIndex(slot)+(crate-1)*14;
				plots.get(id).fill(channel);
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
