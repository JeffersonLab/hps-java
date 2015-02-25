package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 */
public class EcalReadoutToTriggerConverterDriver extends Driver {

    // To import database conditions
    private EcalConditions ecalConditions = null;

    private String rawCollectionName = "EcalReadoutHits";
    private final String ecalReadoutName = "EcalHits";
    private String ecalCollectionName = "EcalCalHits";
    private int integralWindow = 30;
    private double threshold = Double.NEGATIVE_INFINITY;
    private boolean applyBadCrystalMap = true;
    private boolean dropBadFADC = false;
    private double tp = 14.0;
    private final double readoutPeriod = 4.0;
    private final int readoutThreshold = 50;
    private final int triggerThreshold = 80;
    private double timeShift = 0;
    private int truncateScale = 128;
    private static final boolean isBadChannelLoaded = true;

    public EcalReadoutToTriggerConverterDriver() {
    }

    public void setTp(double tp) {
        this.tp = tp;
    }

    public void setDropBadFADC(boolean dropBadFADC) {
        this.dropBadFADC = dropBadFADC;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
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

    public void setTruncateScale(int truncateScale) {
        this.truncateScale = truncateScale;
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

        System.out.println("You are now using the database conditions for EcalReadoutToTriggerConverterDriver.");
    }

    public boolean isBadCrystal(CalorimeterHit hit) {
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(hit.getCellID());

        return isBadChannelLoaded ? channelData.isBadChannel() : false;
    }

    public boolean isBadFADC(CalorimeterHit hit) {
        return (getCrate(hit.getCellID()) == 1 && getSlot(hit.getCellID()) == 3);
    }

    @Override
    public void process(EventHeader event) {
        ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

        if (event.hasCollection(BaseRawCalorimeterHit.class, rawCollectionName)) {
            List<BaseRawCalorimeterHit> hits = event.get(BaseRawCalorimeterHit.class, rawCollectionName);

            for (BaseRawCalorimeterHit hit : hits) {
                CalorimeterHit newHit = HitDtoA(hit, integralWindow);
                if (newHit != null && newHit.getRawEnergy() > threshold) {
                    if (applyBadCrystalMap && isBadCrystal(newHit)) {
                        continue;
                    }
                    if (dropBadFADC && isBadFADC(newHit)) {
                        continue;
                    }
                    newHits.add(newHit);
                }
            }
        }
        int flags = 0;
        event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
    }

    public CalorimeterHit HitDtoA(BaseRawCalorimeterHit hit, int window) {

        // Get the channel data.
        EcalChannelConstants channelData = findChannel(hit.getCellID());

        double integral = tp * Math.E / readoutPeriod;
        double readoutIntegral = (hit.getAmplitude() - window * channelData.getCalibration().getPedestal());
        double amplitude = readoutIntegral / integral;

//        double time = readoutPeriod * (Math.random() - 1);
        double time = 0 - timeShift;
        timeShift += 0.01;
        if (timeShift > readoutPeriod) {
            timeShift = 0;
        }
        double triggerIntegral = 0;
        boolean overThreshold = false;
//        double readoutTime = -1;
//        double triggerTime = -1;
        while (true) {
            double currentValue = amplitude * pulseAmplitude(time);
//            if (readoutTime < 0 && currentValue > readoutThreshold) {
//                readoutTime = time;
//            }
            if (!overThreshold && currentValue > triggerThreshold) {
                overThreshold = true;
//                triggerTime = time;
            }
            if (overThreshold) {
                triggerIntegral += amplitude * pulseAmplitude(time);
                if (currentValue < triggerThreshold) {
                    break;
                }
            }
            time += readoutPeriod;

            if (time > 200.0) {
                break;
            }
        }

//        System.out.format("%f %f %f\n", readoutIntegral, amplitude, triggerIntegral);
        if (hit.getTimeStamp() % 64 != 0) {
            System.out.println("unexpected timestamp " + hit.getTimeStamp());
        }
        int truncatedIntegral = (int) Math.floor(triggerIntegral / truncateScale) * truncateScale;
        double hitTime = hit.getTimeStamp() / 16.0;
//        if (readoutTime >= 0 && triggerTime >= 0) {
//            hitTime += triggerTime - readoutTime;
//        }
        long id = hit.getCellID();
//                Hep3Vector pvec = hit.getDetectorElement().getGeometry().getPosition();
//                double [] pos = new double[3];
//                pos[0] = pvec.x();
//                pos[1] = pvec.y();
//                pos[2] = pvec.z();
//        if (truncatedIntegral<=0) return null;
        if (truncatedIntegral <= 0) {
            truncatedIntegral = 0;
        }
        return CalorimeterHitUtilities.create(truncatedIntegral, hitTime, id);
    }

    private double pulseAmplitude(double time) {
        if (time <= 0.0) {
            return 0.0;
        }
        if (tp > 0.0) {
            return (time / tp) * Math.exp(1.0 - time / tp);
        } else {
            if (time < -tp) {
                return 1.0;
            } else {
                return 0.0;
            }
        }
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

    /**
     * Return crate number from cellID
     *
     * @param cellID (long)
     * @return Crate number (int)
     */
    private int getCrate(long cellID) {
        // Find the ECAL channel and return the crate number.
        return ecalConditions.getChannelCollection().findGeometric(cellID).getCrate();
    }

    /**
     * Return slot number from cellID
     *
     * @param cellID (long)
     * @return Slot number (int)
     */
    private int getSlot(long cellID) {
        // Find the ECAL channel and return the slot number.
        return ecalConditions.getChannelCollection().findGeometric(cellID).getSlot();
    }
}
