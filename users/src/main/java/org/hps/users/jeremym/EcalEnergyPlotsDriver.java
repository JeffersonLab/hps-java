package org.hps.users.jeremym;

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
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver will create a histogram for every channel in the ECAL and plot its corrected energy.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalEnergyPlotsDriver extends Driver {

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
            IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("ECAL Energy - Crate " + crate);
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
                                        
                    String energyPlotName = "Crystal Energy : " + crate + " : " + slot + " : " + channel;
                    IHistogram1D energyHistogram = aida.histogram1D(energyPlotName, 500, -50., 200.);
                    
                    plotter.region(channel).plot(energyHistogram, style);
                }
                plotter.show();
            }
        }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalCalHits");
            for (CalorimeterHit hit : hits) {
                EcalChannel channel = channels.findGeometric(hit.getCellID());
                if (channel != null) {
                    aida.histogram1D("Crystal Energy : " + channel.getCrate() + " : " + channel.getSlot() + " : " + channel.getChannel())
                        .fill(hit.getCorrectedEnergy() * 1000);
                }                   
            }
        }
    }
}
