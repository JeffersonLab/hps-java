package org.hps.users.celentan;

import hep.aida.IHistogram1D;

import java.util.ArrayList;
import java.util.List;

import org.hps.monitoring.ecal.plots.EcalMonitoringUtilities;
import org.hps.recon.ecal.ECalUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * The driver <code>EcalChannelAmplitude</code> implements the histogram shown to the user in the 
 * fifth tab of the Monitoring Application, when using the Ecal monitoring lcsim file. The implementation
 * is as follows: - The event display is opened in a separate window - It is updated regularly, according 
 * to the event refresh rate - If the user clicks on a crystal, the corresponding energy and time
 * distributions (both Histogram1D) are shown in the last panel of the MonitoringApplication, as well 
 * as a 2D histogram (hit time vs hit energy). Finally, if available, the raw waveshape (in mV) is
 * displayed.
 * 
 * @author Andrea Celentano 
 *
 */
public class EcalChannelsAmplitude extends Driver {

    String inputCollection = "EcalCalHits";
    String inputCollectionRaw = "EcalReadoutHits";
    String clusterCollection = "EcalClusters";

    private AIDA aida = AIDA.defaultInstance();
    private Detector detector;

    int eventn = 0;
    int ix, iy, id;
    int pedSamples = 10;

    double amp, ped, sigma;
    double hitE;
    int[] windowRaw = new int[47 * 11];// in case we have the raw waveform, this is the window lenght (in samples)
    boolean[] isFirstRaw = new boolean[47 * 11];

    boolean enableAllFadc = false;

    ArrayList<IHistogram1D> channelAmplitudePlot;
    ArrayList<IHistogram1D> channelRawWaveform;

    int itmpx, itmpy;

    public EcalChannelsAmplitude() {

    }

    public void setPedSamples(int pedSamples) {
        this.pedSamples = pedSamples;
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setInputCollectionRaw(String inputCollectionRaw) {
        this.inputCollectionRaw = inputCollectionRaw;
    }

    public void setInputClusterCollection(String inputClusterCollection) {
        this.clusterCollection = inputClusterCollection;
    }

    @Override
    public void detectorChanged(Detector detector) {
        System.out.println("Ecal event display detector changed");
        this.detector = detector;

        aida.tree().cd("/");

        channelAmplitudePlot = new ArrayList<IHistogram1D>();
        channelRawWaveform = new ArrayList<IHistogram1D>();
        // create the histograms for single channel energy and time distribution.
        for (int ii = 0; ii < (47 * 11); ii = ii + 1) {
            int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
            int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);
            channelAmplitudePlot.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Amplitude : " + (column) + " " + (row) + ": " + ii, 100, -.2, 2100.));
            channelRawWaveform.add(aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Hit Amplitude : " + (column) + " " + (row) + ": " + ii));
            // the above instruction is a terrible hack, just to fill the arrayList with all the elements. They'll be initialized properly during the event readout,
            // since we want to account for possibly different raw waveform dimensions!

            isFirstRaw[ii] = true;
            windowRaw[ii] = 1;
        }
        id = 0;
        iy = EcalMonitoringUtilities.getRowFromHistoID(id);
        ix = EcalMonitoringUtilities.getColumnFromHistoID(id);
    }

    @Override
    public void endOfData() {
        System.out.println("END OF DATA");
    }

    @Override
    public void process(EventHeader event) {

        int ii;
        int row = 0;
        int column = 0;
        double[] result;

        if (event.hasCollection(RawTrackerHit.class, inputCollectionRaw)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollectionRaw);
            for (RawTrackerHit hit : hits) {
                row = hit.getIdentifierFieldValue("iy");
                column = hit.getIdentifierFieldValue("ix");
                ii = EcalMonitoringUtilities.getHistoIDFromRowColumn(row, column);
                if ((row != 0) && (column != 0) && (!(EcalMonitoringUtilities.isInHole(row, column)))) {
                    if (isFirstRaw[ii]) { // at the very first hit we read for this channel, we need to read the window length and save it
                        isFirstRaw[ii] = false;
                        windowRaw[ii] = hit.getADCValues().length;
                        channelRawWaveform.set(ii, aida.histogram1D(detector.getDetectorName() + " : " + inputCollectionRaw + " : Raw Waveform : " + (column) + " " + (row) + ": " + ii, windowRaw[ii], -0.5 * ECalUtils.ecalReadoutPeriod, (-0.5 + windowRaw[ii]) * ECalUtils.ecalReadoutPeriod));
                    }
                    result = ECalUtils.computeAmplitude(hit.getADCValues(), windowRaw[ii], pedSamples);
                    channelAmplitudePlot.get(ii).fill(result[0]);
                }
            }
        }

    }

    /*
     * @Override public void reset(){ for(int ii = 0; ii < (47*11); ii = ii +1){ channelEnergyPlot.get(ii).reset(); channelTimePlot.get(ii).reset(); channelTimeVsEnergyPlot.get(ii).reset(); } }
     */
}
