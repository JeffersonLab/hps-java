package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.util.RandomGaussian;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
//import org.hps.conditions.deprecated.EcalConditions;


/**
 * This file can be used on Monte Carlo and excludes pile up. This makes
 * it useful to study sampling fractions and energy resolution when firing single
 * energy particles at the detector. This file takes the geant4 energy deposition,
 * and converts it to uits of FADC using the pulse integral value and gain (amplitude).
 * The amplitude can include noise if desired. This value is then compared to the 
 * readout/trigger thresholds in FADC. Finally,this value is converted back into GeV 
 * energy output which can be used for clustering, etc. 
 * 
 * Original author for test run:
 * @author phansson 
 * Modified from test run to Spring 2015 running: 
 * @author Holly Szumila <hvanc001@odu.edu>
 */
public class EcalEdepToTriggerConverterDriver extends Driver {
	
    private EcalConditions ecalConditions = null;
    
    private static final boolean isBadChannelLoaded = true;
	
    private final String ecalReadoutName = "EcalHits";
    private String inputCollection = "EcalHits";
    private String readoutCollection = "EcalCalHits";
    private String triggerCollection = "EcalTriggerHits";
    private boolean applyBadCrystalMap = true;
    private double tp = 6.95; //14.0 for test run;
    private final double readoutPeriod = 4.0;
    private final int readoutThreshold = 12;//FADC units; 50 for test run;
    private final int triggerThreshold = 12;//FADC units; 80;
    private int truncateScale = 128;
    private final double pulseIntegral = tp * Math.E * Math.E /(2*readoutPeriod);//tp * Math.E / readoutPeriod (test run);
    private final double gainScale = 1.0; //gain miscalibration factor
    private double _gain = -1.0; //constant gain, activated if >0
    private boolean addNoise = false;
    private final double pePerMeV = 32.8; //photoelectrons per MeV, used to calculate noise

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
    	
        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
                
        System.out.println("You are now using the database conditions for EcalEdepToTriggerConverterDriver.");
    }

    public boolean isBadCrystal(CalorimeterHit hit) {
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(hit.getCellID());
    	
        return isBadChannelLoaded ? channelData.isBadChannel() : false;
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
        	return CalorimeterHitUtilities.create(truncatedIntegral, hit.getTime(), hit.getCellID());
        }
        return null;
    }

    public CalorimeterHit makeReadoutHit(CalorimeterHit hit, double amplitude) {
        if (amplitude < readoutThreshold) {
            return null;
        }

        
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(hit.getCellID());
        
//        double integral = hit.getRawEnergy()/ECalUtils.GeV * gainScale;
        double gain = _gain > 0 ? _gain : channelData.getGain().getGain();
        double integral = amplitude * gain * pulseIntegral * gainScale * EcalUtils.MeV / EcalUtils.GeV;

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
        return CalorimeterHitUtilities.create(integral, hit.getTime(), hit.getCellID());
    }

    private double hitAmplitude(CalorimeterHit hit) {
        double energyAmplitude = hit.getRawEnergy();
        
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(hit.getCellID());
        
        if (addNoise) {
            //add preamp noise and photoelectron Poisson noise in quadrature
            double noise = Math.sqrt(Math.pow(channelData.getCalibration().getNoise() * channelData.getGain().getGain() * EcalUtils.MeV, 2) + hit.getRawEnergy() * EcalUtils.MeV / pePerMeV);
            energyAmplitude += RandomGaussian.getGaussian(0, noise);
        }

        double gain = _gain > 0 ? _gain : channelData.getGain().getGain();
//        System.out.format("amplitude: %f %f %f %f\n", hit.getRawEnergy(), energyAmplitude, gain, (energyAmplitude / ECalUtils.MeV) / (gain * pulseIntegral));
        return (energyAmplitude / EcalUtils.MeV) / (gain * pulseIntegral);
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
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }
}
