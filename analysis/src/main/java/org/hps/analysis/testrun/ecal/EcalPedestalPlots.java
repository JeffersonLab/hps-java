package org.hps.analysis.testrun.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.hps.conditions.deprecated.EcalConditions;
import org.hps.recon.ecal.ECalUtils;
import org.hps.util.Redrawable;
import org.hps.util.Resettable;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalPedestalPlots extends Driver implements Resettable, ActionListener, Redrawable {

    private String inputCollection = "EcalReadoutHits";
    private IPlotter plotter;
    private IPlotter plotter2;
    private AIDA aida = AIDA.defaultInstance();
    //private AIDAFrame plotterFrame;
    private IHistogram2D aMeanPlot;
    private IHistogram2D aSigmaPlot;
    private IHistogram2D tMeanPlot;
    private IHistogram2D tSigmaPlot;
    private IHistogram1D[][] aPlots = new IHistogram1D[47][11];
    private IHistogram1D[][] tPlots = new IHistogram1D[47][11];
    private JLabel xLabel, yLabel;
    private JComboBox xCombo;
    private JComboBox yCombo;
    private JButton blankButton;
    private static final Integer[] xList = new Integer[46];
    private static final Integer[] yList = new Integer[10];
    int eventRefreshRate = 1;
    int eventn = 0;
    boolean hide = false;
    int calWindow = 0;
    double maxE = 1000 * ECalUtils.MeV;

    public EcalPedestalPlots() {
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
        aSigmaPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Sigma (Amplitude)", 47, -23.5, 23.5, 11, -5.5, 5.5);
        aMeanPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Mean (Amplitude)", 47, -23.5, 23.5, 11, -5.5, 5.5);
        tSigmaPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Sigma (Time)", 47, -23.5, 23.5, 11, -5.5, 5.5);
        tMeanPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Mean (Time)", 47, -23.5, 23.5, 11, -5.5, 5.5);

        for (int x = -23; x <= 23; x++) { // slot
            for (int y = -5; y <= 5; y++) { // crate                
                //System.out.println("creating plot: " + "ECAL: Crate " + j + "; Slot " + i + " in region " + region);
//                IHistogram1D hist = aida.histogram1D("ECAL: x=" + i + "; y=" + j, 1000, 0, 16);
//                plots[x + 23][y + 5] = aida.cloud1D("ECAL: x=" + x + "; y=" + y);
                if (calWindow == 0) {
                    aPlots[x + 23][y + 5] = aida.histogram1D("ECAL Amplitudes: x=" + x + "; y=" + y, 500, -0.1, maxE);
                } else {
                    aPlots[x + 23][y + 5] = aida.histogram1D("ECAL Amplitudes: x=" + x + "; y=" + y, 1024, -0.5, 1023.5);
                }
                tPlots[x + 23][y + 5] = aida.histogram1D("ECAL Times: x=" + x + "; y=" + y, 100, 0, 100);
            }
        }

        //plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS ECal Crystal Plots");

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

        // Setup the plotter.
        plotter = aida.analysisFactory().createPlotterFactory().create();
        plotter.setTitle("HPS ECal Hit Amplitude Plots");
        //plotterFrame.addPlotter(plotter);
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

        // Setup the plotter.
        plotter2 = aida.analysisFactory().createPlotterFactory().create();
        plotter2.setTitle("HPS ECal Hit Time Plots");
        //plotterFrame.addPlotter(plotter2);
        plotter2.createRegions(1, 3);

        plotter2.style().statisticsBoxStyle().setVisible(false);
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

        plotter.region(2).plot(aPlots[-5 + 23][2 + 5 - 1]);
        plotter2.region(2).plot(tPlots[-5 + 23][2 + 5 - 1]);
        xCombo.setSelectedIndex(-5 + 23);
        yCombo.setSelectedIndex(2 + 5 - 1);

        //plotterFrame.pack();
        //if (!hide) {
        //    plotterFrame.setVisible(true);
        //}
    }

    @Override
    public void endOfData() {
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
    }

    @Override
    public void reset() {
        if (plotter != null) {
            plotter.hide();
            plotter.destroyRegions();
            for (int x = -23; x <= 23; x++) { // slot
                for (int y = -5; y <= 5; y++) { // crate                
                    aPlots[x + 23][y + 5].reset();
                }
            }
        }
    }

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
            for (RawTrackerHit hit : hits) {
                int x = hit.getIdentifierFieldValue("ix");
                int y = hit.getIdentifierFieldValue("iy");
                if (calWindow > 0) {
                    for (int i = 0; i < calWindow; i++) {
                        aPlots[x + 23][y + 5].fill(hit.getADCValues()[i]);
                    }
                } else {
                    for (int i = 0; i < hit.getADCValues().length; i++) {
                        aPlots[x + 23][y + 5].fill(hit.getADCValues()[i]);
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
                aPlots[x + 23][y + 5].fill(hit.getAmplitude());
                tPlots[x + 23][y + 5].fill(hit.getTimeStamp() / 64);
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
                aPlots[x + 23][y + 5].fill(hit.getRawEnergy());
                tPlots[x + 23][y + 5].fill(hit.getTime() / 4.0);
            }
            if (eventRefreshRate > 0 && ++eventn % eventRefreshRate == 0) {
                redraw();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == blankButton) {
            plotter.region(2).clear();
            plotter2.region(2).clear();
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
        }
    }

    @Override
    public void redraw() {
        aSigmaPlot.reset();
        aMeanPlot.reset();
        tSigmaPlot.reset();
        tMeanPlot.reset();
        for (int x = -23; x <= 23; x++) { // slot
            for (int y = -5; y <= 5; y++) { // crate   
                if (aPlots[x + 23][y + 5].entries() > 10) {
                    aSigmaPlot.fill(x, y, aPlots[x + 23][y + 5].rms());
                    aMeanPlot.fill(x, y, aPlots[x + 23][y + 5].mean());
                }
                if (tPlots[x + 23][y + 5].entries() > 10) {
                    tSigmaPlot.fill(x, y, tPlots[x + 23][y + 5].rms());
                    tMeanPlot.fill(x, y, tPlots[x + 23][y + 5].mean());
                }
            }
        }
    }

    @Override
    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }
}
