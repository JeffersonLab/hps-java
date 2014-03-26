package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.hps.util.RandomGaussian;
import org.lcsim.util.Driver;

/**
 *
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 */
public class EcalEdepToTriggerConverterDriver extends Driver {

    private String ecalReadoutName = "EcalHits";
    private String inputCollection = "EcalHits";
    private String readoutCollection = "EcalCalHits";
    private String triggerCollection = "EcalTriggerHits";
    private boolean applyBadCrystalMap = true;
    private double tp = 14.0;
    private double readoutPeriod = 4.0;
    private int readoutThreshold = 50;
    private int triggerThreshold = 80;
    private int truncateScale = 128;
    private double pulseIntegral = tp * Math.E / readoutPeriod;
    private double gainScale = 1.0; //gain miscalibration factor
    private double _gain = -1.0; //constant gain, activated if >0
    private boolean addNoise = false;
    private double pePerMeV = 2.0; //photoelectrons per MeV, used to calculate noise

    public EcalEdepToTriggerConverterDriver() {
    }

    public void setTp(double tp) {
        this.tp = tp;
    }

    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    public void setReadoutCollection(String readoutCollection) {
        this.readoutCollection = readoutCollection;
    }

    public void setTriggerCollection(String triggerCollection) {
        this.triggerCollection = triggerCollection;
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setApplyBadCrystalMap(boolean apply) {
        this.applyBadCrystalMap = apply;
    }

    public void setTruncateScale(int truncateScale) {
        this.truncateScale = truncateScale;
    }

    public void setConstantGain(double gain) {
        this._gain = gain;
    }

    @Override
    public void startOfData() {
        if (readoutCollection == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }

    @Override
    public void detectorChanged(Detector detector) {
    }

    public boolean isBadCrystal(CalorimeterHit hit) {
        return EcalConditions.badChannelsLoaded() ? EcalConditions.isBadChannel(hit.getCellID()) : false;
    }

    @Override
    public void process(EventHeader event) {
        ArrayList<CalorimeterHit> triggerHits = new ArrayList<CalorimeterHit>();
        ArrayList<CalorimeterHit> readoutHits = new ArrayList<CalorimeterHit>();

        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);

            for (CalorimeterHit hit : hits) {
                if (applyBadCrystalMap && isBadCrystal(hit)) {
                    continue;
                }
                double amplitude = hitAmplitude(hit);
                CalorimeterHit triggerHit = makeTriggerHit(hit, amplitude);
                if (triggerHit != null) {
                    triggerHits.add(triggerHit);
//                    System.out.format("trigger hit: %f %f\n", amplitude, triggerHit.getRawEnergy());
                }
                CalorimeterHit readoutHit = makeReadoutHit(hit, amplitude);
                if (readoutHit != null) {
                    readoutHits.add(readoutHit);
//                    System.out.format("readout hit: %f %f\n", amplitude, readoutHit.getRawEnergy());
                }
            }
        }
        int flags = 0;
        event.put(triggerCollection, triggerHits, CalorimeterHit.class, flags, ecalReadoutName);
        event.put(readoutCollection, readoutHits, CalorimeterHit.class, flags, ecalReadoutName);
    }

    public CalorimeterHit makeTriggerHit(CalorimeterHit hit, double amplitude) {

//        double time = readoutPeriod * (Math.random() - 1);
        double time = 0 - hit.getTime();
        double triggerIntegral = 0;
        boolean overThreshold = false;
        while (true) {
            double currentValue = amplitude * pulseAmplitude(time);
            if (!overThreshold && currentValue > triggerThreshold) {
                overThreshold = true;
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

//        System.out.format("trigger: %f %f\n", amplitude, triggerIntegral);

        int truncatedIntegral = (int) Math.floor(triggerIntegral / truncateScale);
        if (truncatedIntegral > 0) {
            return new HPSCalorimeterHit(truncatedIntegral, hit.getTime(), hit.getCellID(), 0);
        }
        return null;
    }

    public CalorimeterHit makeReadoutHit(CalorimeterHit hit, double amplitude) {
        if (amplitude < readoutThreshold) {
            return null;
        }

//        double integral = hit.getRawEnergy()/ECalUtils.GeV * gainScale;
        double gain = _gain > 0 ? _gain : EcalConditions.physicalToGain(hit.getCellID());
        double integral = amplitude * gain * pulseIntegral * gainScale * ECalUtils.MeV / ECalUtils.GeV;

//        double thresholdCrossingTime = 0 - hit.getTime();
//        while (true) {
//            double currentValue = amplitude * pulseAmplitude(thresholdCrossingTime);
//            if (currentValue > readoutThreshold) {
//                break;
//            }
//            thresholdCrossingTime += readoutPeriod;
//
//            if (thresholdCrossingTime > 200.0) {
//                break;
//            }
//        }
//
//        double readoutIntegral = 0;
//        for (int i = 0; i < 35; i++) {
//            readoutIntegral += amplitude * pulseAmplitude(thresholdCrossingTime + (i - 5) * readoutPeriod);
//        }
////        double integral = readoutIntegral * HPSEcalConditions.physicalToGain(id);
//        System.out.format("dumb: %f, full: %f\n",hit.getRawEnergy() * 1000.0,readoutIntegral * HPSEcalConditions.physicalToGain(id));

//        System.out.format("readout: %f %f\n", amplitude, integral);
        CalorimeterHit h = new HPSCalorimeterHit(integral, hit.getTime(), hit.getCellID(), 0);
        return h;
    }

    private double hitAmplitude(CalorimeterHit hit) {
        double energyAmplitude = hit.getRawEnergy();
        if (addNoise) {
            //add preamp noise and photoelectron Poisson noise in quadrature
            double noise = Math.sqrt(Math.pow(EcalConditions.physicalToNoise(hit.getCellID()) * EcalConditions.physicalToGain(hit.getCellID()) * ECalUtils.MeV, 2) + hit.getRawEnergy() * ECalUtils.MeV / pePerMeV);
            energyAmplitude += RandomGaussian.getGaussian(0, noise);
        }

        double gain = _gain > 0 ? _gain : EcalConditions.physicalToGain(hit.getCellID());
//        System.out.format("amplitude: %f %f %f %f\n", hit.getRawEnergy(), energyAmplitude, gain, (energyAmplitude / ECalUtils.MeV) / (gain * pulseIntegral));
        return (energyAmplitude / ECalUtils.MeV) / (gain * pulseIntegral);
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
}
