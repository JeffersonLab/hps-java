package org.lcsim.hps.recon.ecal;

import hep.aida.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import org.lcsim.detector.identifier.*;
import org.lcsim.event.CalorimeterHit;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.evio.TriggerData;
import org.lcsim.hps.monitoring.deprecated.AIDAFrame;
import org.lcsim.hps.monitoring.deprecated.Redrawable;
import org.lcsim.hps.monitoring.deprecated.Resettable;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalCrystalFilter extends Driver implements Resettable, ActionListener, Redrawable {

    private String inputCollection;
    private String outputPlotFileName;
    private IPlotter plotter;
    private IPlotter plotter2;
    private IPlotter plotter3;
    private IPlotter plotter4;
    private IPlotter plotterTop;
    private IPlotter plotterTop2;
    private IPlotter plotterTop3;
    private IPlotter plotterTop4;
    private IPlotter plotterBot;
    private IPlotter plotterBot2;
    private IPlotter plotterBot3;
    private IPlotter plotterBot4;
    private AIDA aida = AIDA.defaultInstance();
    private AIDAFrame plotterFrame;
    private AIDAFrame plotterFrameTop;
    private AIDAFrame plotterFrameBot;
    private IHistogram1D aMeanPlot;
    private IHistogram1D aSigmaPlot;
    private IHistogram1D tMeanPlot;
    private IHistogram1D tSigmaPlot;
    private IHistogram1D aTMeanPlot;
    private IHistogram1D aTSigmaPlot;
    private IHistogram1D tTMeanPlot;
    private IHistogram1D tTSigmaPlot;
    private IHistogram1D aBMeanPlot;
    private IHistogram1D aBSigmaPlot;
    private IHistogram1D tBMeanPlot;
    private IHistogram1D tBSigmaPlot;
    private IHistogram2D aTOutMeanPlot;
    private IHistogram2D aTOutSigmaPlot;
    private IHistogram2D tTOutMeanPlot;
    private IHistogram2D tTOutSigmaPlot;
    private IHistogram2D aTTOutMeanPlot;
    private IHistogram2D aTTOutSigmaPlot;
    private IHistogram2D tTTOutMeanPlot;
    private IHistogram2D tTTOutSigmaPlot;
    private IHistogram2D aBTOutMeanPlot;
    private IHistogram2D aBTOutSigmaPlot;
    private IHistogram2D tBTOutMeanPlot;
    private IHistogram2D tBTOutSigmaPlot;
    private IHistogram1D[][] aPlots = new IHistogram1D[47][11];
    private IHistogram1D[][] tPlots = new IHistogram1D[47][11];
    private IHistogram1D[][] aTPlots = new IHistogram1D[47][11];
    private IHistogram1D[][] tTPlots = new IHistogram1D[47][11];
    private IHistogram1D[][] aBPlots = new IHistogram1D[47][11];
    private IHistogram1D[][] tBPlots = new IHistogram1D[47][11];
    private JLabel xLabel, yLabel;
    private JComboBox xCombo;
    private JComboBox yCombo;
    private JButton blankButton;
    private JLabel xLabelTop, yLabelTop;
    private JComboBox xComboTop;
    private JComboBox yComboTop;
    private JButton blankButtonTop;
    private JLabel xLabelBot, yLabelBot;
    private JComboBox xComboBot;
    private JComboBox yComboBot;
    private JButton blankButtonBot;
    private static final Integer[] xList = new Integer[46];
    private static final Integer[] yList = new Integer[10];
    private static final Integer[] xListTop = new Integer[46];
    private static final Integer[] yListTop = new Integer[10];
    private static final Integer[] xListBot = new Integer[46];
    private static final Integer[] yListBot = new Integer[10];
    int eventRefreshRate = 1;
    int eventn = 0;
    boolean hide = false;
    int calWindow = 0;
    double maxE = 1000;
    double tTOutNSigmaThr = 5.0;
    String hotCrystalFileName = "ecal_hotcrystals.txt";
    FileWriter fWriter;
    PrintWriter pWriter;
    int[] _trigger = new int[2];

    public EcalCrystalFilter() {
        int count = 0;
        for (int i = -23; i <= 23; i++) {
            if (i != 0) {
                xList[count] = i;
                xListTop[count] = i;
                xListBot[count] = i;

                count++;
            }
        }
        count = 0;
        for (int i = -5; i <= 5; i++) {
            if (i != 0) {
                yList[count] = i;
                yListTop[count] = i;
                yListBot[count] = i;
                count++;
            }
        }
        try {
            fWriter = new FileWriter(hotCrystalFileName);
            pWriter = new PrintWriter(fWriter);
        } catch (IOException ex) {
            Logger.getLogger(EcalCrystalFilter.class.getName()).log(Level.SEVERE, null, ex);
        }


        outputPlotFileName = "";

    }

    public void closeFile() throws IOException {
        pWriter.close();
        fWriter.close();
    }

    public void setMaxE(double maxE) {
        this.maxE = maxE;
    }

    public void setCalWindow(int calWindow) {
        this.calWindow = calWindow;
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

        aida = AIDA.defaultInstance();
        aida.tree().cd("/");

        aSigmaPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Sigma> (Amplitude) Filter", 50, 0, 200);
        aMeanPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Mean> (Amplitude) Filter", 50, 0, 1000);
        tSigmaPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Sigma> (Time) Filter", 50, 0, 50);
        tMeanPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Mean> (Time) Filter", 50, 0, 100);

        aTSigmaPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Sigma> (Amplitude) Top Trig Filter", 50, 0, 200);
        aTMeanPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Mean> (Amplitude) Top Trig Filter", 50, 0, 1000);
        tTSigmaPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Sigma> (Time) Top Trig Filter", 50, 0, 50);
        tTMeanPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Mean> (Time) Top Trig Filter", 50, 0, 100);

        aBSigmaPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Sigma> (Amplitude) Bottom Trig Filter", 50, 0, 200);
        aBMeanPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Mean> (Amplitude) Bottom Trig Filter", 50, 0, 1000);
        tBSigmaPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Sigma> (Time) Bottom Trig Filter", 50, 0, 50);
        tBMeanPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : <Mean> (Time) Bottom Trig Filter", 50, 0, 100);


        aTOutSigmaPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Sigma (Amplitude) Time Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        aTOutMeanPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Mean (Amplitude) Time Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        tTOutSigmaPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Sigma (Time) Time Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        tTOutMeanPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Mean (Time) Time Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);

        aTTOutSigmaPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Sigma (Amplitude) Time Bot Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        aTTOutMeanPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Mean (Amplitude) Time Bot Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        tTTOutSigmaPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Sigma (Time) Time Bot Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        tTTOutMeanPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Mean (Time) Time Bot Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);

        aBTOutSigmaPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Sigma (Amplitude) Time Top Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        aBTOutMeanPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Mean (Amplitude) Time Top Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        tBTOutSigmaPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Sigma (Time) Time Top Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);
        tBTOutMeanPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Mean (Time) Time Top Outliers", 47, -23.5, 23.5, 11, -5.5, 5.5);


        for (int x = -23; x <= 23; x++) { // slot
            for (int y = -5; y <= 5; y++) { // crate                
                //System.out.println("creating plot: " + "ECAL: Crate " + j + "; Slot " + i + " in region " + region);
//                IHistogram1D hist = aida.histogram1D("ECAL: x=" + i + "; y=" + j, 1000, 0, 16);
//                plots[x + 23][y + 5] = aida.cloud1D("ECAL: x=" + x + "; y=" + y);
                if (calWindow == 0) {
                    aPlots[x + 23][y + 5] = aida.histogram1D("ECAL Amplitudes: x=" + x + "; y=" + y, 500, -0.1, maxE);
                    aTPlots[x + 23][y + 5] = aida.histogram1D("Top ECAL Amplitudes: x=" + x + "; y=" + y, 500, -0.1, maxE);
                    aBPlots[x + 23][y + 5] = aida.histogram1D("Bottom ECAL Amplitudes: x=" + x + "; y=" + y, 500, -0.1, maxE);

                } else {
                    aPlots[x + 23][y + 5] = aida.histogram1D("ECAL Amplitudes: x=" + x + "; y=" + y, 1024, -0.5, 1023.5);
                    aTPlots[x + 23][y + 5] = aida.histogram1D("Top Trig CAL Amplitudes: x=" + x + "; y=" + y, 1024, -0.5, 1023.5);
                    aBPlots[x + 23][y + 5] = aida.histogram1D("Bottom Trig CAL Amplitudes: x=" + x + "; y=" + y, 1024, -0.5, 1023.5);
                }
                tPlots[x + 23][y + 5] = aida.histogram1D("ECAL Times: x=" + x + "; y=" + y, 100, 0, 100);
                tTPlots[x + 23][y + 5] = aida.histogram1D("Top Trig ECAL Times: x=" + x + "; y=" + y, 100, 0, 100);
                tBPlots[x + 23][y + 5] = aida.histogram1D("Bottom Trig ECAL Times: x=" + x + "; y=" + y, 100, 0, 100);
            }
        }

        plotterFrame = new AIDAFrame();
        plotterFrame.setTitle("HPS ECal Crystal Filter Plots");

        xCombo = new JComboBox(xList);
        xCombo.addActionListener(this);
        xLabel = new JLabel("x");
        xLabel.setLabelFor(xCombo);
        plotterFrame.getControlsPanel().add(xLabel);
        plotterFrame.getControlsPanel().add(xCombo);
        yCombo = new JComboBox(yList);
        yCombo.addActionListener(this);
        yLabel = new JLabel("y");
        yLabel.setLabelFor(yCombo);
        plotterFrame.getControlsPanel().add(yLabel);
        plotterFrame.getControlsPanel().add(yCombo);
        blankButton = new JButton("Hide histogram");
        plotterFrame.getControlsPanel().add(blankButton);
        blankButton.addActionListener(this);

        // Setup the plotter.
        plotter = aida.analysisFactory().createPlotterFactory().create();
        plotter.setTitle("HPS ECal Amplitude");
        plotterFrame.addPlotter(plotter);
        plotter.createRegions(1, 3);

        plotter.style().statisticsBoxStyle().setVisible(false);
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.style().zAxisStyle().setParameter("allowZeroSuppression", "true");
        IPlotterStyle style = plotter.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotter.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter.region(0).plot(aSigmaPlot);
        plotter.region(1).plot(aMeanPlot);
        plotter.style().dataStyle().fillStyle().setColor("yellow");

        // Setup the plotter.
        plotter2 = aida.analysisFactory().createPlotterFactory().create();
        plotter2.setTitle("HPS ECal Hit Time ");
        plotterFrame.addPlotter(plotter2);
        plotter2.createRegions(1, 3);

        plotter2.style().statisticsBoxStyle().setVisible(true);
        plotter2.style().dataStyle().errorBarStyle().setVisible(false);
        plotter2.style().zAxisStyle().setParameter("allowZeroSuppression", "true");
        style = plotter2.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotter2.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter2.region(0).plot(tSigmaPlot);
        plotter2.region(1).plot(tMeanPlot);
        plotter2.style().dataStyle().fillStyle().setColor("green");


        // Setup the plotter.
        plotter3 = aida.analysisFactory().createPlotterFactory().create();
        plotter3.setTitle("HPS ECal for Time Outliers ");
        plotterFrame.addPlotter(plotter3);
        plotter3.createRegions(1, 3);

        plotter3.style().statisticsBoxStyle().setVisible(false);
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.style().zAxisStyle().setParameter("allowZeroSuppression", "true");
        style = plotter3.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotter3.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter3.region(0).plot(tTOutSigmaPlot);
        plotter3.region(1).plot(tTOutMeanPlot);
        plotter3.style().dataStyle().fillStyle().setColor("green");

        // Setup the plotter.
        plotter4 = aida.analysisFactory().createPlotterFactory().create();
        plotter4.setTitle("HPS ECal Amplitude for Time Outliers ");
        plotterFrame.addPlotter(plotter4);
        plotter4.createRegions(1, 3);

        plotter4.style().statisticsBoxStyle().setVisible(false);
        plotter4.style().dataStyle().errorBarStyle().setVisible(false);
        plotter4.style().zAxisStyle().setParameter("allowZeroSuppression", "true");
        style = plotter4.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotter4.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter4.region(0).plot(aTOutSigmaPlot);
        plotter4.region(1).plot(aTOutMeanPlot);
        plotter4.style().dataStyle().fillStyle().setColor("green");

        plotter.region(2).plot(aPlots[-5 + 23][2 + 5 - 1]);
        plotter2.region(2).plot(tPlots[-5 + 23][2 + 5 - 1]);
        plotter3.region(2).plot(tPlots[-5 + 23][2 + 5 - 1]);
        plotter4.region(2).plot(aPlots[-5 + 23][2 + 5 - 1]);








        plotterFrameTop = new AIDAFrame();
        plotterFrameTop.setTitle("HPS Top Trig ECal Crystal Filter Plots");

        xComboTop = new JComboBox(xListTop);
        xComboTop.addActionListener(this);
        xLabelTop = new JLabel("xT");
        xLabelTop.setLabelFor(xComboTop);
        plotterFrameTop.getControlsPanel().add(xLabelTop);
        plotterFrameTop.getControlsPanel().add(xComboTop);
        yComboTop = new JComboBox(yListTop);
        yComboTop.addActionListener(this);
        yLabelTop = new JLabel("yT");
        yLabelTop.setLabelFor(yComboTop);


        plotterFrameTop.getControlsPanel().add(yLabelTop);
        plotterFrameTop.getControlsPanel().add(yComboTop);
        blankButtonTop = new JButton("Hide histogram (Top)");
        plotterFrameTop.getControlsPanel().add(blankButtonTop);
        blankButtonTop.addActionListener(this);


        // Setup the plotterTop.
        plotterTop = aida.analysisFactory().createPlotterFactory().create();
        plotterTop.setTitle("HPS ECal Amplitude");
        plotterFrameTop.addPlotter(plotterTop);
        plotterTop.createRegions(1, 3);
        plotterTop.setStyle(plotter.style());
        style = plotterTop.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotterTop.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotterTop.region(0).plot(aTSigmaPlot);
        plotterTop.region(1).plot(aTMeanPlot);
        plotterTop.style().dataStyle().fillStyle().setColor("yellow");

        // Setup the plotterTop.
        plotterTop2 = aida.analysisFactory().createPlotterFactory().create();
        plotterTop2.setTitle("HPS ECal Hit Time ");
        plotterFrameTop.addPlotter(plotterTop2);
        plotterTop2.createRegions(1, 3);

        plotterTop2.setStyle(plotter2.style());
        style = plotterTop2.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotterTop2.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotterTop2.region(0).plot(tTSigmaPlot);
        plotterTop2.region(1).plot(tTMeanPlot);


        // Setup the plotter.
        plotterTop3 = aida.analysisFactory().createPlotterFactory().create();
        plotterTop3.setTitle("HPS ECal for Time Outliers ");
        plotterFrameTop.addPlotter(plotterTop3);
        plotterTop3.createRegions(1, 3);

        plotterTop3.setStyle(plotter3.style());
        style = plotterTop3.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotterTop3.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotterTop3.region(0).plot(tTTOutSigmaPlot);
        plotterTop3.region(1).plot(tTTOutMeanPlot);

        // Setup the plotter.
        plotterTop4 = aida.analysisFactory().createPlotterFactory().create();
        plotterTop4.setTitle("HPS ECal Amplitude for Time Outliers ");
        plotterFrameTop.addPlotter(plotterTop4);
        plotterTop4.createRegions(1, 3);

        plotterTop4.setStyle(plotter4.style());
        style = plotterTop4.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotterTop4.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotterTop4.region(0).plot(aTTOutSigmaPlot);
        plotterTop4.region(1).plot(aTTOutMeanPlot);

        plotterTop.region(2).plot(aTPlots[-5 + 23][2 + 5 - 1]);
        plotterTop2.region(2).plot(tTPlots[-5 + 23][2 + 5 - 1]);
        plotterTop3.region(2).plot(tTPlots[-5 + 23][2 + 5 - 1]);
        plotterTop4.region(2).plot(aTPlots[-5 + 23][2 + 5 - 1]);








        plotterFrameBot = new AIDAFrame();
        plotterFrameBot.setTitle("HPS Bottom Trig ECal Crystal Filter Plots");

        xComboBot = new JComboBox(xListBot);
        xComboBot.addActionListener(this);
        xLabelBot = new JLabel("x");
        xLabelBot.setLabelFor(xComboBot);
        plotterFrameBot.getControlsPanel().add(xLabelBot);
        plotterFrameBot.getControlsPanel().add(xComboBot);
        yComboBot = new JComboBox(yListBot);
        yComboBot.addActionListener(this);
        yLabelBot = new JLabel("y");
        yLabelBot.setLabelFor(yComboBot);
        plotterFrameBot.getControlsPanel().add(yLabelBot);
        plotterFrameBot.getControlsPanel().add(yComboBot);
        blankButtonBot = new JButton("Hide histogram");
        plotterFrameBot.getControlsPanel().add(blankButtonBot);
        blankButtonBot.addActionListener(this);

        // Setup the plotterBot.
        plotterBot = aida.analysisFactory().createPlotterFactory().create();
        plotterBot.setTitle("HPS ECal Amplitude");
        plotterFrameBot.addPlotter(plotterBot);
        plotterBot.createRegions(1, 3);
        plotterBot.setStyle(plotter.style());
        style = plotterBot.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotterBot.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotterBot.region(0).plot(aBSigmaPlot);
        plotterBot.region(1).plot(aBMeanPlot);
        plotterBot.style().dataStyle().fillStyle().setColor("yellow");

        // Setup the plotterBot.
        plotterBot2 = aida.analysisFactory().createPlotterFactory().create();
        plotterBot2.setTitle("HPS ECal Hit Time ");
        plotterFrameBot.addPlotter(plotterBot2);
        plotterBot2.createRegions(1, 3);

        plotterBot2.setStyle(plotter2.style());
        style = plotterBot2.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotterBot2.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotterBot2.region(0).plot(tBSigmaPlot);
        plotterBot2.region(1).plot(tBMeanPlot);


        // Setup the plotter.
        plotterBot3 = aida.analysisFactory().createPlotterFactory().create();
        plotterBot3.setTitle("HPS ECal for Time Outliers ");
        plotterFrameBot.addPlotter(plotterBot3);
        plotterBot3.createRegions(1, 3);

        plotterBot3.setStyle(plotter3.style());
        style = plotterBot3.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotterBot3.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotterBot3.region(0).plot(tBTOutSigmaPlot);
        plotterBot3.region(1).plot(tBTOutMeanPlot);

        // Setup the plotter.
        plotterBot4 = aida.analysisFactory().createPlotterFactory().create();
        plotterBot4.setTitle("HPS ECal Amplitude for Time Outliers ");
        plotterFrameBot.addPlotter(plotterBot4);
        plotterBot4.createRegions(1, 3);

        plotterBot4.setStyle(plotter4.style());
        style = plotterBot4.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style = plotterBot4.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotterBot4.region(0).plot(aBTOutSigmaPlot);
        plotterBot4.region(1).plot(aBTOutMeanPlot);

        plotterBot.region(2).plot(aBPlots[-5 + 23][2 + 5 - 1]);
        plotterBot2.region(2).plot(tBPlots[-5 + 23][2 + 5 - 1]);
        plotterBot3.region(2).plot(tBPlots[-5 + 23][2 + 5 - 1]);
        plotterBot4.region(2).plot(aBPlots[-5 + 23][2 + 5 - 1]);
        //xCombo.setSelectedIndex(-5 + 23);
        //yCombo.setSelectedIndex(2 + 5 - 1);





        plotterFrame.pack();
        if (!hide) {
            plotterFrame.setVisible(true);
        }



        plotterFrameTop.pack();
        if (!hide) {
            plotterFrameTop.setVisible(true);
        }




        plotterFrameBot.pack();
        if (!hide) {
            plotterFrameBot.setVisible(true);
        }



        xComboBot.setSelectedIndex(-5 + 23);
        yComboBot.setSelectedIndex(2 + 5 - 1);



        xComboTop.setSelectedIndex(-5 + 23);
        yComboTop.setSelectedIndex(2 + 5 - 1);




        xCombo.setSelectedIndex(-5 + 23);
        yCombo.setSelectedIndex(2 + 5 - 1);




    }

    public void printOutliers() {

        //Outliers in time -- threshold is nr of sigma/rms from the mean
        System.out.printf("Crystals with time RMS more than %.1f times the RMS(<RMS>)=%.1f from <RMS>=%.1f for all crystals\n", tTOutNSigmaThr, tSigmaPlot.rms(), tSigmaPlot.mean());

        for (int iside = 0; iside < 3; ++iside) {
            System.out.println("-- Side " + iside);
            pWriter.printf("# Side %d\n", iside);
            for (int x = -23; x <= 23; x++) { // slot
                for (int y = -5; y <= 5; y++) { // crate   

                    boolean ok = false;
                    if (iside == 0) {
                        if (tTOutSigmaPlot.binEntries(tTOutSigmaPlot.coordToIndexX(x), tTOutSigmaPlot.coordToIndexY(y)) > 0) {
                            ok = true;
                        }
                    } else if (iside == 1) {
                        if (tTTOutSigmaPlot.binEntries(tTTOutSigmaPlot.coordToIndexX(x), tTTOutSigmaPlot.coordToIndexY(y)) > 0) {
                            ok = true;
                        }

                    } else if (iside == 2) {
                        if (tBTOutSigmaPlot.binEntries(tBTOutSigmaPlot.coordToIndexX(x), tBTOutSigmaPlot.coordToIndexY(y)) > 0) {
                            ok = true;
                        }
                    }
                    if (ok) {
                        IIdentifierHelper helper = EcalConditions.getHelper();
                        IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary().getNumberOfFields());
                        //expId.setValue(helper.getFieldIndex("system"), ecal.getSystemID());
                        expId.setValue(helper.getFieldIndex("ix"), x);
                        expId.setValue(helper.getFieldIndex("iy"), y);
                        Long id = helper.pack(expId).getValue();


                        System.out.printf("[%d,%d]\t%d\t%d\t%d\tTime:%f +- %f\tAmp:%f +- %f\n", x, y, EcalConditions.getCrate(id), EcalConditions.getSlot(id), EcalConditions.getChannel(id), tPlots[x + 23][y + 5].mean(), tPlots[x + 23][y + 5].rms(), aPlots[x + 23][y + 5].mean(), aPlots[x + 23][y + 5].rms());

                        pWriter.printf("%d %d\n", x, y);
                    }

                }
            }
        }
    }

    @Override
    public void endOfData() {

        //Redraw one final time and use those values to print out the outlying crystals
        redraw();
        printOutliers();
        //plotterFrame.dispose();
        if (calWindow > 0) {
            for (int crate = 1; crate < 3; crate++) {
                for (short slot = 0; slot < 20; slot++) {
                    for (short ch = 0; ch < 16; ch++) {
                        Long id = EcalConditions.daqToPhysicalID(crate, slot, ch);
                        IIdentifierHelper helper = EcalConditions.getHelper();
                        if (id == null) {
                            continue;
                        }
                        IIdentifier compactId = new Identifier(id);
                        int x = helper.getValue(compactId, "ix");
                        int y = helper.getValue(compactId, "iy");
                        System.out.printf("%d\t%d\t%d\t%f\t%f\n", crate, slot, ch, aPlots[x + 23][y + 5].mean(), aPlots[x + 23][y + 5].rms());
                    }
                }
            }
        }
        try {
            closeFile();
        } catch (IOException ex) {
            Logger.getLogger(EcalCrystalFilter.class.getName()).log(Level.SEVERE, null, ex);
        }


        if (!"".equals(outputPlotFileName)) {
            try {
                aida.saveAs(outputPlotFileName);
            } catch (IOException ex) {
                Logger.getLogger(EcalCrystalFilter.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
            }
        }
        //displayFastTrackingPlots();

    }

    @Override
    public void reset() {
//        if (plotter != null) {
//            plotter.hide();
//            plotter.destroyRegions();
//            for (int x = -23; x <= 23; x++) { // slot
//                for (int y = -5; y <= 5; y++) { // crate                
//                    aPlots[x + 23][y + 5].reset();
//                }
//            }
//        }
    }

    @Override
    public void process(EventHeader event) {

        getTrigger(event);


        boolean isTop, isBot, isAll;
        for (int iside = 0; iside < 3; ++iside) {

            isAll = false;
            isTop = false;
            isBot = false;

            //System.out.println("Side " + iside + " Trigger " + _trigger[0] + "," + _trigger[1]);


            if (iside == 0) {
                isAll = true;
            } else if (iside == 1) {
                if (_trigger[0] > 0) {
                    isTop = true;
                } else {
                    continue;
                }
            } else if (iside == 2) {
                if (_trigger[1] > 0) {
                    isBot = true;
                } else {
                    continue;
                }
            }

            //System.out.println("isTop " + isTop + " isBot " + isBot);


            if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
                List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
                for (RawTrackerHit hit : hits) {


                    int y = hit.getIdentifierFieldValue("iy");
                    int x = hit.getIdentifierFieldValue("ix");


                    //System.out.println("RawTrackerHit: iside " + iside + " trigger " + _trigger[0] + ", " + _trigger[1] + " isBot " + isBot + " isTop " + isTop + " iy " + y);

                    //Only look at the opposite half of what triggered
                    if (isAll) {
                    }
                    if (isTop) {
                        if (y > 0) {
                            continue;
                        }
                    }
                    if (isBot) {
                        if (y < 0) {
                            continue;
                        }
                    }



                    //System.out.println("RawTrackerHit: ===> fill");


                    if (calWindow > 0) {
                        for (int i = 0; i < calWindow; i++) {

                            if (isAll) {
                                aPlots[x + 23][y + 5].fill(hit.getADCValues()[i]);
                            }
                            if (isTop) {
                                aTPlots[x + 23][y + 5].fill(hit.getADCValues()[i]);
                            }
                            if (isBot) {
                                aBPlots[x + 23][y + 5].fill(hit.getADCValues()[i]);
                            }

                        }
                    } else {
                        for (int i = 0; i < hit.getADCValues().length; i++) {
                            if (isAll) {
                                aPlots[x + 23][y + 5].fill(hit.getADCValues()[i]);
                            }
                            if (isTop) {
                                aTPlots[x + 23][y + 5].fill(hit.getADCValues()[i]);
                            }
                            if (isBot) {
                                aBPlots[x + 23][y + 5].fill(hit.getADCValues()[i]);
                            }

                        }
                    }
                }
                if (eventRefreshRate > 0 && ++eventn % eventRefreshRate == 0) {
                    redraw();
                }
            }

            if (event.hasCollection(BaseRawCalorimeterHit.class, inputCollection)) {
                List<BaseRawCalorimeterHit> hits = event.get(BaseRawCalorimeterHit.class, inputCollection);
                for (BaseRawCalorimeterHit hit : hits) {
                    int x = hit.getIdentifierFieldValue("ix");
                    int y = hit.getIdentifierFieldValue("iy");


                    //System.out.println("BaseRawCalorimeterHit: iside " + iside + " trigger " + _trigger[0] + ", " + _trigger[1] + " isBot " + isBot + " isTop " + isTop + " iy " + y );

                    //Only look at the opposite half of what triggered
                    if (isAll) {
                    }
                    if (isTop) {
                        if (y > 0) {
                            continue;
                        }
                    }
                    if (isBot) {
                        if (y < 0) {
                            continue;
                        }
                    }

                    //System.out.println("BaseRawCalorimeterHit: ===> fill");


                    if (isAll) {
                        aPlots[x + 23][y + 5].fill(hit.getAmplitude());
                        tPlots[x + 23][y + 5].fill(hit.getTimeStamp() / 64);
                    }
                    if (isTop) {
                        aTPlots[x + 23][y + 5].fill(hit.getAmplitude());
                        tTPlots[x + 23][y + 5].fill(hit.getTimeStamp() / 64);
                    }
                    if (isBot) {
                        aBPlots[x + 23][y + 5].fill(hit.getAmplitude());
                        tBPlots[x + 23][y + 5].fill(hit.getTimeStamp() / 64);
                    }

                }
                if (eventRefreshRate > 0 && ++eventn % eventRefreshRate == 0) {
                    redraw();
                }
            }

            if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
                List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
                for (CalorimeterHit hit : hits) {
                    int x = hit.getIdentifierFieldValue("ix");
                    int y = hit.getIdentifierFieldValue("iy");

                    //System.out.println("CalorimeterHit: iside " + iside + " trigger " + _trigger[0] + ", " + _trigger[1] + " isBot " + isBot + " isTop " + isTop + " iy " + y );



                    //Only look at the opposite half of what triggered
                    if (isAll) {
                    }
                    if (isTop) {
                        if (y > 0) {
                            continue;
                        }
                    }
                    if (isBot) {
                        if (y < 0) {
                            continue;
                        }
                    }

                    //System.out.println("CalorimeterHit: ===> fill");
                    if (isAll) {
                        aPlots[x + 23][y + 5].fill(hit.getRawEnergy());
                        tPlots[x + 23][y + 5].fill(hit.getTime() / 4.0);
                    }
                    if (isTop) {
                        aTPlots[x + 23][y + 5].fill(hit.getRawEnergy());
                        tTPlots[x + 23][y + 5].fill(hit.getTime() / 4.0);
                    }
                    if (isBot) {
                        aBPlots[x + 23][y + 5].fill(hit.getRawEnergy());
                        tBPlots[x + 23][y + 5].fill(hit.getTime() / 4.0);
                    }
                }
                if (eventRefreshRate > 0 && ++eventn % eventRefreshRate == 0) {
                    redraw();
                }
            }
        }
    }

    private void clearTrigger() {
        _trigger[0] = 0;
        _trigger[1] = 0;
    }

    private void getTrigger(EventHeader event) {

        clearTrigger();
        if (event.hasCollection(TriggerData.class, "TriggerBank")) {
            List<TriggerData> triggerDataList = event.get(TriggerData.class, "TriggerBank");
            if (triggerDataList.isEmpty() == false) {
                TriggerData triggerData = triggerDataList.get(0);
                int topTrig = triggerData.getTopTrig();
                int botTrig = triggerData.getBotTrig();
                _trigger[0] = topTrig > 0 ? 1 : 0;
                _trigger[1] = botTrig > 0 ? 1 : 0;
            } else {
                System.out.println("Event has EMPTY trigger list!!");
                _trigger[0] = 0;
                _trigger[1] = 0;
            }
        } else {
            System.out.println("Event has NO trigger bank!!");
            _trigger[0] = 0;
            _trigger[1] = 0;
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == blankButton) {
            plotter.region(2).clear();
            plotter2.region(2).clear();
            /*
            plotter3.region(2).clear();
            plotter4.region(2).clear();
            
            
             */
        } else if (ae.getSource() == blankButtonTop) {
            plotterTop.region(2).clear();
            plotterTop2.region(2).clear();

        } else if (ae.getSource() == blankButtonBot) {
            plotterBot.region(2).clear();
            plotterBot2.region(2).clear();

        } else {
            Integer x, y;
            x = (Integer) xCombo.getSelectedItem();
            y = (Integer) yCombo.getSelectedItem();
            plotter.region(2).clear();
            plotter.region(2).plot(aPlots[x + 23][y + 5]);
            plotter2.region(2).clear();
            plotter2.region(2).plot(tPlots[x + 23][y + 5]);
//            ((PlotterRegion) plotter.region(2)).getPlot().setAllowUserInteraction(false);
//            ((PlotterRegion) plotter.region(2)).getPlot().setAllowPopupMenus(false);
            plotter3.region(2).clear();
            plotter3.region(2).plot(tPlots[x + 23][y + 5]);
            plotter4.region(2).clear();
            plotter4.region(2).plot(aPlots[x + 23][y + 5]);


            //Integer x, y;
            x = (Integer) xComboTop.getSelectedItem();
            y = (Integer) yComboTop.getSelectedItem();

            plotterTop.region(2).clear();
            plotterTop.region(2).plot(aTPlots[x + 23][y + 5]);
            plotterTop2.region(2).clear();
            plotterTop2.region(2).plot(tTPlots[x + 23][y + 5]);
//            ((PlotterRegion) plotter.region(2)).getPlot().setAllowUserInteraction(false);
//            ((PlotterRegion) plotter.region(2)).getPlot().setAllowPopupMenus(false);
            plotterTop3.region(2).clear();
            plotterTop3.region(2).plot(tTPlots[x + 23][y + 5]);
            plotterTop4.region(2).clear();
            plotterTop4.region(2).plot(aTPlots[x + 23][y + 5]);

            //Integer x, y;
            x = (Integer) xComboBot.getSelectedItem();
            y = (Integer) yComboBot.getSelectedItem();

            plotterBot.region(2).clear();
            plotterBot.region(2).plot(aBPlots[x + 23][y + 5]);
            plotterBot2.region(2).clear();
            plotterBot2.region(2).plot(tBPlots[x + 23][y + 5]);
//            ((PlotterRegion) plotter.region(2)).getPlot().setAllowUserInteraction(false);
//            ((PlotterRegion) plotter.region(2)).getPlot().setAllowPopupMenus(false);
            plotterBot3.region(2).clear();
            plotterBot3.region(2).plot(tBPlots[x + 23][y + 5]);
            plotterBot4.region(2).clear();
            plotterBot4.region(2).plot(aBPlots[x + 23][y + 5]);

        }
    }

    @Override
    public void redraw() {
//        aSigmaPlot.reset();
//        aMeanPlot.reset();
//        tSigmaPlot.reset();
//        tMeanPlot.reset();
        aTOutSigmaPlot.reset();
        aTOutMeanPlot.reset();
        tTOutSigmaPlot.reset();
        tTOutMeanPlot.reset();
        aTTOutSigmaPlot.reset();
        aTTOutMeanPlot.reset();
        tTTOutSigmaPlot.reset();
        tTTOutMeanPlot.reset();
        aBTOutSigmaPlot.reset();
        aBTOutMeanPlot.reset();
        tBTOutSigmaPlot.reset();
        tBTOutMeanPlot.reset();
        for (int x = -23; x <= 23; x++) { // slot
            for (int y = -5; y <= 5; y++) { // crate   
                //System.out.println("redraw x,y " + x + "," + y + " tT " + tTPlots[x + 23][y + 5].entries() + " tB " + tBPlots[x + 23][y + 5].entries());
                if (aPlots[x + 23][y + 5].entries() > 10) {
                    aSigmaPlot.fill(aPlots[x + 23][y + 5].rms());
                    aMeanPlot.fill(aPlots[x + 23][y + 5].mean());

                }
                if (aTPlots[x + 23][y + 5].entries() > 10) {

                    aTSigmaPlot.fill(aTPlots[x + 23][y + 5].rms());
                    aTMeanPlot.fill(aTPlots[x + 23][y + 5].mean());

                }
                if (aBPlots[x + 23][y + 5].entries() > 10) {


                    aBSigmaPlot.fill(aBPlots[x + 23][y + 5].rms());
                    aBMeanPlot.fill(aBPlots[x + 23][y + 5].mean());
                }

                if (tPlots[x + 23][y + 5].entries() > 10) {
                    tSigmaPlot.fill(tPlots[x + 23][y + 5].rms());
                    tMeanPlot.fill(tPlots[x + 23][y + 5].mean());
                }

                if (tTPlots[x + 23][y + 5].entries() > 10) {

                    tTSigmaPlot.fill(tTPlots[x + 23][y + 5].rms());
                    tTMeanPlot.fill(tTPlots[x + 23][y + 5].mean());

                }
                if (tBPlots[x + 23][y + 5].entries() > 10) {


                    tBSigmaPlot.fill(tBPlots[x + 23][y + 5].rms());
                    tBMeanPlot.fill(tBPlots[x + 23][y + 5].mean());
                }

                //Outliers in time -- threshold is nr of sigma/rms from the mean

                if (tPlots[x + 23][y + 5].rms() > (tSigmaPlot.mean() + tSigmaPlot.rms() * tTOutNSigmaThr) && tPlots[x + 23][y + 5].entries() > 10) {
                    tTOutSigmaPlot.fill(x, y, tPlots[x + 23][y + 5].rms());
                    tTOutMeanPlot.fill(x, y, tPlots[x + 23][y + 5].mean());
                    aTOutSigmaPlot.fill(x, y, aPlots[x + 23][y + 5].rms());
                    aTOutMeanPlot.fill(x, y, aPlots[x + 23][y + 5].mean());
                }
                if (tTPlots[x + 23][y + 5].rms() > (tTSigmaPlot.mean() + tTSigmaPlot.rms() * tTOutNSigmaThr) && tTPlots[x + 23][y + 5].entries() > 10) {

                    tTTOutSigmaPlot.fill(x, y, tTPlots[x + 23][y + 5].rms());
                    tTTOutMeanPlot.fill(x, y, tTPlots[x + 23][y + 5].mean());
                    aTTOutSigmaPlot.fill(x, y, aTPlots[x + 23][y + 5].rms());
                    aTTOutMeanPlot.fill(x, y, aTPlots[x + 23][y + 5].mean());
                }
                if (tBPlots[x + 23][y + 5].rms() > (tBSigmaPlot.mean() + tBSigmaPlot.rms() * tTOutNSigmaThr) && tBPlots[x + 23][y + 5].entries() > 10) {

                    tBTOutSigmaPlot.fill(x, y, tBPlots[x + 23][y + 5].rms());
                    tBTOutMeanPlot.fill(x, y, tBPlots[x + 23][y + 5].mean());

                    aBTOutSigmaPlot.fill(x, y, aBPlots[x + 23][y + 5].rms());
                    aBTOutMeanPlot.fill(x, y, aBPlots[x + 23][y + 5].mean());

                }

            }
        }
        //printOutliers();
    }

    @Override
    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }
}
