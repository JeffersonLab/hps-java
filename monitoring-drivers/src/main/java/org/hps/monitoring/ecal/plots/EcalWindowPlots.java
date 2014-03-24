package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.monitoring.deprecated.AIDAFrame;
import org.lcsim.hps.recon.ecal.EcalConditions;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalWindowPlots extends Driver implements ActionListener {

    private String inputCollection = "EcalReadoutHits";
    private IPlotter plotter;
    private AIDAFrame plotterFrame;
    private AIDA aida;
    private Detector detector;
    private IHistogram1D windowPlot;
    private int window = 100;
    private JLabel crateLabel, slotLabel, channelLabel;
    private JComboBox crateCombo;
    private JComboBox slotCombo;
    private JComboBox channelCombo;
    private static final String[] crateList = new String[3];
    private static final String[] slotList = new String[17];
    private static final String[] channelList = new String[17];
    private boolean testCrate = false;
    private boolean testSlot = false;
    private boolean testChannel = false;
    private int plotCrate, plotSlot, plotChannel;

    public EcalWindowPlots() {
        int count = 0;
        crateList[0] = "all";
        for (int i = 1; i <= 2; i++) {
            count++;
            crateList[count] = Integer.toString(i);
        }
        count = 0;
        slotList[0] = "all";
        for (int i = 0; i <= 15; i++) {
            count++;
            slotList[count] = Integer.toString(i);
        }
        count = 0;
        channelList[0] = "all";
        for (int i = 0; i <= 15; i++) {
            count++;
            channelList[count] = Integer.toString(i);
        }
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setWindow(int window) {
        this.window = window;
    }

    @Override
    public void detectorChanged(Detector detector) {

        this.detector = detector;

        if (inputCollection == null) {
            throw new RuntimeException("The inputCollection parameter was not set.");
        }

        setupPlots();
    }

    private void setupPlots() {
        if (plotterFrame != null) {
            plotterFrame.dispose(); //this clears the plotterFrame
        }

        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        plotter = aida.analysisFactory().createPlotterFactory().create("HPS ECAL Window Plots");

        plotterFrame = new AIDAFrame();
        plotterFrame.addPlotter(plotter);
        plotterFrame.setVisible(true);
        IPlotterStyle pstyle = plotter.style();
        pstyle.dataStyle().errorBarStyle().setVisible(false);
        windowPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Window Mode Data", window, -0.5, window - 0.5);
        plotter.region(0).plot(windowPlot);

        crateCombo = new JComboBox(crateList);
        crateCombo.addActionListener(this);
        crateLabel = new JLabel("crate");
        crateLabel.setLabelFor(crateCombo);
        plotterFrame.getControlsPanel().add(crateLabel);
        plotterFrame.getControlsPanel().add(crateCombo);
        slotCombo = new JComboBox(slotList);
        slotCombo.addActionListener(this);
        slotLabel = new JLabel("slot");
        slotLabel.setLabelFor(slotCombo);
        plotterFrame.getControlsPanel().add(slotLabel);
        plotterFrame.getControlsPanel().add(slotCombo);
        channelCombo = new JComboBox(channelList);
        channelCombo.addActionListener(this);
        channelLabel = new JLabel("channel");
        channelLabel.setLabelFor(channelCombo);
        plotterFrame.getControlsPanel().add(channelLabel);
        plotterFrame.getControlsPanel().add(channelCombo);
        plotterFrame.pack();
    }

    @Override
    public void endOfData() {
        if (plotterFrame != null) {
            plotterFrame.dispose();
        }
    }

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
            for (RawTrackerHit hit : hits) {
                Long daqId = EcalConditions.physicalToDaqID(hit.getCellID());
                int crate = EcalConditions.getCrate(daqId);
                int slot = EcalConditions.getSlot(daqId);
                int channel = EcalConditions.getChannel(daqId);
//System.out.println("got hit: crate " + crate + ", slot " + slot + ", channel " + channel);
                if (hit.getADCValues().length != window) {
                    throw new RuntimeException("Hit has unexpected window length " + hit.getADCValues().length + ", not " + window);
                }
                if (testCrate && crate != plotCrate) {
                    continue;
                }
                if (testSlot && slot != plotSlot) {
                    continue;
                }
                if (testChannel && channel != plotChannel) {
                    continue;
                }
                windowPlot.reset();
                for (int i = 0; i < window; i++) {
                    windowPlot.fill(i, hit.getADCValues()[i]);

                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String selItem;
        selItem = (String) crateCombo.getSelectedItem();
        if (selItem.equals("all")) {
            testCrate = false;
        } else {
            testCrate = true;
            plotCrate = Integer.decode(selItem);
        }

        selItem = (String) slotCombo.getSelectedItem();
        if (selItem.equals("all")) {
            testSlot = false;
        } else {
            testSlot = true;
            plotSlot = Integer.decode(selItem);
        }

        selItem = (String) channelCombo.getSelectedItem();
        if (selItem.equals("all")) {
            testChannel = false;
        } else {
            testChannel = true;
            plotChannel = Integer.decode(selItem);
        }
    }
}

