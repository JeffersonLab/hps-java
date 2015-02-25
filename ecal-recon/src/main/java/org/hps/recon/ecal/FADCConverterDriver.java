package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @version $Id: FADCConverterDriver.java,v 1.4 2013/02/25 22:39:24 meeg Exp $
 */
public class FADCConverterDriver extends Driver {

    private EcalConditions ecalConditions = null;
    private String rawCollectionName = "EcalReadoutHits";
    private String ecalReadoutName = "EcalHits";
    private String ecalCollectionName = "EcalIntegralHits";
    private int numSamplesBefore = 5;
    private int numSamplesAfter = 30;
    private int threshold = 50;

    public FADCConverterDriver() {
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setEcalReadoutName(String ecalReadoutName) {
        this.ecalReadoutName = ecalReadoutName;
    }

    public void setNumSamplesAfter(int numSamplesAfter) {
        this.numSamplesAfter = numSamplesAfter;
    }

    public void setNumSamplesBefore(int numSamplesBefore) {
        this.numSamplesBefore = numSamplesBefore;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }

    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }

    @Override
    public void detectorChanged(Detector detector) {
        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();

        System.out.println("You are now using the database conditions for FADCConverterDriver");
    }

    @Override
    public void process(EventHeader event) {
        ArrayList<BaseRawCalorimeterHit> readoutHits = new ArrayList<BaseRawCalorimeterHit>();

        // Get the list of ECal hits.
        if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

            for (RawTrackerHit hit : hits) {
                short[] window = hit.getADCValues();
                long id = hit.getCellID();

                // Get the channel data.
                EcalChannelConstants channelData = findChannel(id);

                //do DAQ readout
                double crystalThreshold = channelData.getCalibration().getPedestal() + threshold;
                int adcSum = 0;
                int pointerOffset = 0;
                int numSamplesToRead = 0;
                int thresholdCrossing = 0;
                for (int i = 0; i < window.length; i++) {
                    if (numSamplesToRead != 0) {
                        adcSum += window[i - pointerOffset];
                        numSamplesToRead--;
                        if (numSamplesToRead == 0) {
                            readoutHits.add(new BaseRawCalorimeterHit(id, adcSum, 64 * thresholdCrossing));
                        }
                    } else if ((i == 0 || window[i - 1] <= crystalThreshold) && window[i] > crystalThreshold) {
                        thresholdCrossing = i;
                        pointerOffset = Math.min(numSamplesBefore, i);
                        numSamplesToRead = pointerOffset + Math.min(numSamplesAfter, window.length - i - pointerOffset - 1);
                        adcSum = 0;
                    }
                }
                //do trigger readout
            }
        }
        int flags = 0;
        event.put(ecalCollectionName, readoutHits, BaseRawCalorimeterHit.class, flags, ecalReadoutName);
    }

    /**
     * Convert physical ID to gain value.
     *
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }

}
