package org.lcsim.hps.users.celentan;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalWindowEventPlots extends Driver implements ActionListener {

    private String inputCollection;
    private IPlotter plotter;
    private AIDA aida = AIDA.defaultInstance();
    //private AIDAFrame plotterFrame;
    private IHistogram1D[][] plots = new IHistogram1D[47][11];
    private JLabel xLabel, yLabel;
    private JComboBox xCombo;
    private JComboBox yCombo;
    private JButton blankButton;
    private static final Integer[] xList = new Integer[46];
    private static final Integer[] yList = new Integer[10];
    boolean hide = false;
    int window = 100;

    public void setWindow(int window) {
        this.window = window;
    }

    public EcalWindowEventPlots() {
        int count = 0;
        for (int i = -23; i <= 23; i++) {
            if (i != 0) {
                xList[count] = i;
                count++;
            }
        }
        count = 0;
        for (int i = -5; i <= 5; i++) {
            if (i != 0) {
                yList[count] = i;
                count++;
            }
        }
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    @Override
    public void detectorChanged(Detector detector) {
        if (inputCollection == null) {
            throw new RuntimeException("The inputCollection parameter was not set.");
        }

        // Setup the plotter.
        plotter = aida.analysisFactory().createPlotterFactory().create();
        plotter.setTitle("HPS ECal Window Event Plots");
        //plotterFrame = new AIDAFrame();
        //plotterFrame.addPlotter(plotter);

        aida = AIDA.defaultInstance();
        aida.tree().cd("/");

        for (int x = -23; x <= 23; x++) { // slot
            for (int y = -5; y <= 5; y++) { // crate                
                plots[x + 23][y + 5] = aida.histogram1D("ECAL window: x=" + x + "; y=" + y, window, -0.5, window - 0.5);
            }
        }

        // Create the plotter regions.
        plotter.createRegion();

        xCombo = new JComboBox(xList);
        xCombo.addActionListener(this);
        xLabel = new JLabel("x");
        xLabel.setLabelFor(xCombo);
        //plotterFrame.getControlsPanel().add(xLabel);
        //plotterFrame.getControlsPanel().add(xCombo);
        yCombo = new JComboBox(yList);
        yCombo.addActionListener(this);
        yLabel = new JLabel("y");
        yLabel.setLabelFor(yCombo);
        //plotterFrame.getControlsPanel().add(yLabel);
        //plotterFrame.getControlsPanel().add(yCombo);
        blankButton = new JButton("Hide histogram");
        //plotterFrame.getControlsPanel().add(blankButton);
        blankButton.addActionListener(this);

        //plotterFrame.pack();

        plotter.style().statisticsBoxStyle().setVisible(false);
        plotter.style().zAxisStyle().setParameter("allowZeroSuppression", "true");
        plotter.style().dataStyle().errorBarStyle().setVisible(false);

        plotter.region(0).plot(plots[-5 + 23][2 + 5]);
        xCombo.setSelectedIndex((-5 + 23));
        yCombo.setSelectedIndex((2 + 5 - 1));

        //if (!hide) {
        //    plotterFrame.setVisible(true);
        //}
    }

    @Override
    public void process(EventHeader event) {
        for (int x = -23; x <= 23; x++) { // slot
            for (int y = -5; y <= 5; y++) { // crate                
                plots[x + 23][y + 5].reset();
            }
        }

        if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
            for (RawTrackerHit hit : hits) {
                int x = hit.getIdentifierFieldValue("ix");
                int y = hit.getIdentifierFieldValue("iy");
                for (int i = 0; i < window; i++) {
                    plots[x + 23][y + 5].fill(i, hit.getADCValues()[i]);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Integer x, y;
        x = (Integer) xCombo.getSelectedItem();
        y = (Integer) yCombo.getSelectedItem();
        plotter.region(0).clear();
        plotter.region(0).plot(plots[x + 23][y + 5]);
    }
}