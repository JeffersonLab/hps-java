package org.hps.analysis.ecal.cosmic;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver will create a histogram for every channel in the ECAL and plot its ADC values
 * from the LCSim event collection of raw mode hits, without performing any pedestal subtraction.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class RawModeADCHistogramDriver extends Driver {

    EcalConditions conditions = null;
    EcalChannelCollection channels = null;

    List<List<IPlotter>> plotterLists = new ArrayList<List<IPlotter>>();

    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();

    public void detectorChanged(Detector detector) {

        conditions = DatabaseConditionsManager.getInstance().getEcalConditions();

        channels = conditions.getChannelCollection();

        Set<Integer> crates = new HashSet<Integer>();
        Set<Integer> slots = new HashSet<Integer>();
        Set<Integer> channels = new HashSet<Integer>();

        for (EcalChannel channel : conditions.getChannelCollection()) {
            crates.add(channel.getCrate());
            slots.add(channel.getSlot());
            channels.add(channel.getChannel());
        }
                
        for (Integer crate : crates) {
            IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("ECAL Raw ADC - Crate " + crate);
            IPlotterStyle style = plotterFactory.createPlotterStyle();
            style.dataStyle().lineStyle().setVisible(false);
            style.legendBoxStyle().setVisible(false);
            int plottersIndex = crate - 1;
            plotterLists.add(new ArrayList<IPlotter>());
            List<IPlotter> plotters = plotterLists.get(plottersIndex);
            for (Integer slot : slots) {
                IPlotter plotter = plotterFactory.create("Slot " + slot);
                plotters.add(plotter);
                plotter.createRegions(4, 4);
                for (Integer channel : channels) {
                    String histogramName = "ADC Values : " + crate + " : " + slot + " : " + channel;
                    IHistogram1D histogram = aida.histogram1D(histogramName, 150, 50, 200.);
                    plotter.region(channel).plot(histogram, style);
                }
                plotter.show();
            }
        }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, "EcalReadoutHits")) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, "EcalReadoutHits");
            for (RawTrackerHit hit : hits) {
                EcalChannel channel = channels.findGeometric(hit.getCellID());
                if (channel != null) {
                    for (short adcValue : hit.getADCValues()) {
                        try {
                            aida.histogram1D("ADC Values : " + channel.getCrate() + " : " + channel.getSlot() + " : " + channel.getChannel()).fill(adcValue);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.err.println("EcalChannel not found for cell ID 0x" + String.format("%08d", Long.toHexString(hit.getCellID())));
                }
            }
        }
    }
}
