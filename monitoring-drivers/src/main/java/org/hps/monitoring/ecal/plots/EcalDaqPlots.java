package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * The driver <code>EcalDaqPlots</code> implements the histogram shown to the user in the fourth tab of the
 * Monitoring Application, when using the Ecal monitoring lcsim file. It contains only a sub-tab, showing
 * the number of hits recorded by the different FADC channels. It is a very preliminary driver to monitor
 * the DAQ status. These plots are updated continuously.
 * 
 * @author Andrea Celentano
 */
public class EcalDaqPlots extends Driver {

    private String subdetectorName = "Ecal";
    private String inputCollection = "EcalCalHits";
    private IPlotter plotter;
    private AIDA aida;

    private List<IHistogram1D> plots;

    private List<Integer> slotsT, slotsB, crates;

    private EcalChannel.EcalChannelCollection channels;
    private DatabaseConditionsManager manager;

    // private ConditionsManager manager;
    public EcalDaqPlots() {
        manager = DatabaseConditionsManager.getInstance();
        // manager = ConditionsManager.defaultInstance();
    }

    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void detectorChanged(Detector detector) {

        if (subdetectorName == null) {
            throw new RuntimeException("The subdetectorName parameter was not set.");
        }

        if (inputCollection == null) {
            throw new RuntimeException("The inputCollection parameter was not set.");
        }

        // Get the channel information from the database.
        channels = manager.getCachedConditions(EcalChannel.EcalChannelCollection.class, "ecal_channels")
                .getCachedData();

        /*
         * I do not want the ECAL Crates and Slots to be hard-coded. It is fine to assume that the FADC channels are
         * from 0 to 15:
         * This is determined by JLAB FADC architecture.
         * It is also fine to say that there are 14 slots occupied by FADCs in each crate: 14*16=224, number of channel
         * in each Ecal sector
         * (apart from the hole).
         */
        slotsT = new ArrayList<Integer>();
        slotsB = new ArrayList<Integer>();
        crates = new ArrayList<Integer>();

        // Loop over channels and get the list of slots-crates
        for (EcalChannel channel : channels) {

            // y>0 means TOP, y<0 means BOTTOM
            int y = channel.getY();
            int slot = channel.getSlot();
            int crate = channel.getCrate();

            if (y > 0) {
                if (!slotsT.contains(slot))
                    slotsT.add(slot);
            } else if (y < 0) {
                if (!slotsB.contains(slot))
                    slotsB.add(slot);
            }
            if (!crates.contains(crate))
                crates.add(crate);
        }
        /* Order the slots in increasing order */
        Collections.sort(slotsB);
        Collections.sort(slotsT);

        /*
         * System.out.println("These DAQ slots found:"); System.out.println("TOP: ");
         * for (int slot : slotsT){ System.out.print(slot+" "); }
         * System.out.println(""); System.out.println("BOTTOM: "); for
         * (int slot : slotsB){ System.out.print(slot+" "); } System.out.println("");
         */

        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        plots = new ArrayList<IHistogram1D>();

        for (int j = 0; j < 14; j++) { // TOP slot
            plots.add(aida.histogram1D("ECAL: Top Crate Slot " + slotsT.get(j), 16, 0, 16));
        }

        for (int j = 0; j < 14; j++) { // BOTTOM slot
            plots.add(aida.histogram1D("ECAL: Bottom Crate Slot " + slotsB.get(j), 16, 0, 16));
        }

        IPlotterFactory factory = aida.analysisFactory().createPlotterFactory("ECAL DAQ Plots");
        plotter = factory.create("Crates");
        IPlotterStyle pstyle = plotter.style();
        pstyle.dataStyle().fillStyle().setColor("orange");
        // pstyle.dataStyle().markerStyle().setColor("orange");
        pstyle.dataStyle().errorBarStyle().setVisible(false);
        pstyle.legendBoxStyle().setVisible(false);
        plotter.createRegions(7, 4);

        int id, plot_id;
        for (int i = 0; i < 2; i++) { // crate
            for (int j = 0; j < 14; j++) { // slot
                id = i * 14 + j;
                plot_id = 0;
                if (i == 0) { // first-crate
                    if (j % 2 == 0)
                        plot_id = j * 2;
                    else
                        plot_id = (j - 1) * 2 + 1;
                } else if (i == 1) { // second-crate
                    if (j % 2 == 0)
                        plot_id = j * 2 + 2;
                    else
                        plot_id = (j - 1) * 2 + 3;
                }
                // System.out.println("Plot in region " + plot_id + " the plot " + plots.get(id).title() + "(index: " +
                // id + ")");
                plotter.region(plot_id).plot(plots.get(id));
            }
        }
        plotter.show();
    }

    public void process(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {

                // Make an ID to find.
                // EcalChannel daq = new EcalChannel.DaqId();
                // daq.crate = 1;
                // daq.slot = 2;
                // daq.channel = 3;

                // Find the matching channel.
                EcalChannel channel = channels.findGeometric(hit.getCellID());
                int row = hit.getIdentifierFieldValue("iy");
                int column = hit.getIdentifierFieldValue("ix");

                int crateN = channel.getCrate();
                int slotN = channel.getSlot();
                int channelN = channel.getChannel();

                // System.out.println("found channel at " + column + " " + row +
                // " corresponding to DAQ crate/slot/channel " + crateN + " "+slotN+" "+channelN);

                // Top CRATE
                if (row > 0) {
                    int index = slotsT.indexOf(slotN);
                    plots.get(index).fill(channelN);
                } else if (row < 0) {
                    int index = slotsB.indexOf(slotN);
                    plots.get(index + 14).fill(channelN);
                }
            }
        }
    }
}
