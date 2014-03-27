package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.deprecated.EcalConditions;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 *
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 */
public class EcalRawConverterDriver extends Driver {

    EcalRawConverter converter = null;
    String rawCollectionName = "EcalReadoutHits";
    String ecalReadoutName = "EcalHits";
    String ecalCollectionName = "EcalCalHits";
    int integralWindow = 35;
    boolean debug = false;
    double threshold = Double.NEGATIVE_INFINITY;
    boolean applyBadCrystalMap = true;
    boolean dropBadFADC = false;
    private boolean runBackwards = false;

    public EcalRawConverterDriver() {
        converter = new EcalRawConverter();
    }

    public void setUse2014Gain(boolean use2014Gain) {
        converter.setUse2014Gain(use2014Gain);
    }

    public void setRunBackwards(boolean runBackwards) {
        this.runBackwards = runBackwards;
    }

    public void setDropBadFADC(boolean dropBadFADC) {
        this.dropBadFADC = dropBadFADC;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setGain(double gain) {
        converter.setGain(gain);
    }

    public void setIntegralWindow(int integralWindow) {
        this.integralWindow = integralWindow;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }

    public void setApplyBadCrystalMap(boolean apply) {
        this.applyBadCrystalMap = apply;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }

    @Override
    public void detectorChanged(Detector detector) {
    }

    public static boolean isBadCrystal(CalorimeterHit hit) {
        return EcalConditions.badChannelsLoaded() ? EcalConditions.isBadChannel(hit.getCellID()) : false;
    }

    public static boolean isBadFADC(CalorimeterHit hit) {
        long daqID = EcalConditions.physicalToDaqID(hit.getCellID());
        return (EcalConditions.getCrate(daqID) == 1 && EcalConditions.getSlot(daqID) == 3);
    }

    @Override
    public void process(EventHeader event) {
        int flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME; //store cell ID

        if (!runBackwards) {
            ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

            // Get the list of ECal hits.
            if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
                List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

                for (RawTrackerHit hit : hits) {
                    CalorimeterHit newHit = converter.HitDtoA(hit);
                    if (applyBadCrystalMap && isBadCrystal(newHit)) {
                        continue;
                    }
                    if (dropBadFADC && isBadFADC(newHit)) {
                        continue;
                    }
                    if (newHit.getRawEnergy() > threshold) {
                        newHits.add(newHit);
                    }
                }
                event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
            }
            if (event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) {
                List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, rawCollectionName);

                for (RawCalorimeterHit hit : hits) {
                    if (debug) {
                        System.out.format("old hit energy %d\n", hit.getAmplitude());
                    }
                    CalorimeterHit newHit = converter.HitDtoA(hit, integralWindow);
                    if (newHit.getRawEnergy() > threshold) {
                        if (applyBadCrystalMap && isBadCrystal(newHit)) {
                            continue;
                        }
                        if (dropBadFADC && isBadFADC(newHit)) {
                            continue;
                        }
                        if (debug) {
                            System.out.format("new hit energy %f\n", newHit.getRawEnergy());
                        }
                        newHits.add(newHit);
                    }
                }
                event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
            }
        } else {
            ArrayList<RawCalorimeterHit> newHits = new ArrayList<RawCalorimeterHit>();
            if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
                List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

                for (CalorimeterHit hit : hits) {
                    if (debug) {
                        System.out.format("old hit energy %f\n", hit.getRawEnergy());
                    }
                    RawCalorimeterHit newHit = converter.HitAtoD(hit, integralWindow);
                    if (newHit.getAmplitude() > 0) {
                        if (debug) {
                            System.out.format("new hit energy %d\n", newHit.getAmplitude());
                        }
                        newHits.add(newHit);
                    }
                }
                event.put(rawCollectionName, newHits, RawCalorimeterHit.class, flags, ecalReadoutName);
            }
        }
    }
}
