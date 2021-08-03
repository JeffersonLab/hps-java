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
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import org.hps.recon.ecal.EcalUtils;

public class EcalWindowPlotsXY extends Driver implements ActionListener {

    private String subdetectorName = "Ecal";
    private String inputCollection;
    private IPlotter plotter;
    // private AIDAFrame plotterFrame;
    private AIDA aida;
    private Detector detector;
    private IDDecoder dec;
    private IHistogram1D windowPlot;
    private IHistogram1D windowPlot1;
    private int window = 10;
    private JLabel xLabel, yLabel;
    private JComboBox xCombo;
    private JComboBox yCombo;
    private static final String[] xList = new String[47];
    private static final String[] yList = new String[11];
    private boolean testX = false;
    private boolean testY = false;
    private int plotX, plotY;
    private boolean isFirst = true;

    public EcalWindowPlotsXY() {
        int count = 0;
        xList[0] = "all";
        for (int i = -23; i <= 23; i++) {
            if (i != 0) {
                count++;
                xList[count] = Integer.toString(i);
            }
        }
        count = 0;
        yList[0] = "all";
        for (int i = -5; i <= 5; i++) {
            if (i != 0) {
                count++;
                yList[count] = Integer.toString(i);
            }
        }
    }

    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setWindow(int window) {
        this.window = window;
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
        dec = subdetector.getReadout().getIDDecoder();

        setupPlots();
    }

    private void setupPlots() {
        System.out.println("ECAL WINDOW PLOTS START");
        // if (plotterFrame != null) {
        // plotterFrame.dispose();
        // }

        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        plotter = aida.analysisFactory().createPlotterFactory("ECAL Window plots").create("HPS ECAL Window Plots");

        // plotterFrame = new AIDAFrame();
        // plotterFrame.addPlotter(plotter);
        // plotterFrame.setVisible(true);
        IPlotterStyle pstyle = plotter.style();
        pstyle.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(1, 1);
        windowPlot1 = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : dummy", 1, -0.5,
                1 - 0.5);
        plotter.region(0).plot(windowPlot1);
        plotter.show();
    }

    public void endOfData() {
        // if (plotterFrame != null) {
        // plotterFrame.dispose();
        // }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
            for (RawTrackerHit hit : hits) {
                dec.setID(hit.getCellID());
                int x = dec.getValue("ix");
                int y = dec.getValue("iy");
                // System.out.println("got hit: x= " + x + ", y= " + y);
                if (isFirst) {
                    System.out.println("FIRST!!!");
                    isFirst = false;
                    window = hit.getADCValues().length;
                    windowPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection
                            + " : Window Mode Data", window, -0.5, window - 0.5);
                    plotter.region(0).clear();
                    plotter.region(0).plot(windowPlot);
                    plotter.region(0).refresh();

                }
                if (testX && x != plotX) {
                    continue;
                }
                if (testY && y != plotY) {
                    continue;
                }
                windowPlot.reset();
                for (int i = 0; i < window; i++) {
                    windowPlot.fill(i, hit.getADCValues()[i] * EcalUtils.adcResolution);

                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String selItem;
        selItem = (String) xCombo.getSelectedItem();
        if (selItem.equals("all")) {
            testX = false;
        } else {
            testX = true;
            plotX = Integer.decode(selItem);
        }

        selItem = (String) yCombo.getSelectedItem();
        if (selItem.equals("all")) {
            testY = false;
        } else {
            testY = true;
            plotY = Integer.decode(selItem);
        }
    }
}
