package org.hps.analysis.testrun.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;
import jas.hist.JASHist;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.deprecated.EcalConditions;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalDaqPlots extends Driver {

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

		setupPlots();
	}

	private void setupPlots() {
		plots = new ArrayList<IHistogram1D>();
		aida = AIDA.defaultInstance();
		aida.tree().cd("/");
		plotter = aida.analysisFactory().createPlotterFactory().create("HPS ECAL DAQ Plots");
		IPlotterStyle pstyle = plotter.style();
		pstyle.dataStyle().fillStyle().setColor("orange");
		pstyle.dataStyle().markerStyle().setColor("orange");
		pstyle.dataStyle().errorBarStyle().setVisible(false);
		plotter.createRegions(7, 4);
		int region = 0;
		for (int i = 0; i < 14; i++) { // slot
			for (int j = 1; j < 3; j++) { // crate                
				//System.out.println("creating plot: " + "ECAL: Crate " + j + "; Slot " + i + " in region " + region);
				IHistogram1D hist = aida.histogram1D("ECAL: Crate " + j + "; Slot " + slots[i], 16, 0, 16);
				plots.add(hist);
				plotter.region(region).plot(hist);
				JASHist jhist = ((PlotterRegion) plotter.region(region)).getPlot();
				jhist.setAllowUserInteraction(false);
				jhist.setAllowPopupMenus(false);
				region++;
			}
		}
		plotter.show();
	}

	public void process(EventHeader event) {
		if (event.hasCollection(RawCalorimeterHit.class, inputCollection)) {
			List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, inputCollection);
			for (RawCalorimeterHit hit : hits) {
				Long daqId = EcalConditions.physicalToDaqID(hit.getCellID());
				int crate = EcalConditions.getCrate(daqId);
				int slot = EcalConditions.getSlot(daqId);
				int channel = EcalConditions.getChannel(daqId);
				//System.out.println("crate="+crate+"; slot="+slot+"; channel="+channel);
				//System.out.println("filling plot: " + "ECAL: Crate " + crate + "; Slot " + slot);
				aida.histogram1D("ECAL: Crate " + crate + "; Slot " + slot).fill(channel);
			}
		}
	}
}
