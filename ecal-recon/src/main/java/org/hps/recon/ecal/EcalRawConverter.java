package org.hps.recon.ecal;

import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
//import org.hps.evio.EventConstants;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;

/**
 * This class is used to convert {@link org.lcsim.event.RawCalorimeterHit} objects
 * to {@link org.lcsim.event.CalorimeterHit} objects with energy information.
 * It has methods to convert pedestal subtracted ADC counts to energy.  
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 */
public class EcalRawConverter {

    private boolean constantGain = false;
    private double gain;
    private boolean use2014Gain = true;
    
    private EcalConditions ecalConditions = null;

    public EcalRawConverter() {	
    }

    public void setGain(double gain) {
        constantGain = true;
        this.gain = gain;
    }

    public void setUse2014Gain(boolean use2014Gain) {
        this.use2014Gain = use2014Gain;
    }

    public short sumADC(RawTrackerHit hit) {
        EcalChannelConstants channelData = findChannel(hit.getCellID());        
        double pedestal = channelData.getCalibration().getPedestal();
        short sum = 0;
        short samples[] = hit.getADCValues();
        for (int isample = 0; isample < samples.length; ++isample) {
            sum += (samples[isample] - pedestal);
        }
        return sum;
    }

    public CalorimeterHit HitDtoA(RawTrackerHit hit) {
        double time = hit.getTime();
        long id = hit.getCellID();
        double rawEnergy = adcToEnergy(sumADC(hit), id);
        HPSCalorimeterHit h1 = new HPSCalorimeterHit(rawEnergy, time, id, 0);
        return h1;
    }

    public CalorimeterHit HitDtoA(RawCalorimeterHit hit, int window, double timeOffset) {
        if (hit.getTimeStamp() % 64 != 0) {
            System.out.println("unexpected timestamp " + hit.getTimeStamp());
        }
        double time = hit.getTimeStamp() / 16.0;
        long id = hit.getCellID();
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(id);
        double adcSum = hit.getAmplitude() - window * channelData.getCalibration().getPedestal();
        double rawEnergy = adcToEnergy(adcSum, id);
        HPSCalorimeterHit h2 = new HPSCalorimeterHit(rawEnergy, time + timeOffset, id, 0);
        return h2;
    }
    

    /**
     * A.C. This is the method used to handle both the mode3 and mode7 pulse integrals.
     * 
     * @param hit The raw calorimeter hit
     * @param timeOffset The time offset
     * @return The calibrated calorimeter hit
     * 
     * @TODO Check the pedestal subtraction
     * @TODO A.C. I am not a maven expert, and I can' import org.hps.evio.EventConstants and use the 2 constants  ECAL_PULSE_INTEGRAL_MODE and  ECAL_PULSE_INTEGRAL_HIGHRESTDC_MODE.
     * It seems to me there's a "circular" dependency problem (evio depends on hps-tracking, that depends on ecal-readout-sim, that depends on ecal-recon.
     * Therefore, ecal-recon can't depend on hps-evio, and I can't import org.hps.evio.EventConstants....
     */
    public CalorimeterHit HitDtoA(HPSRawCalorimeterHit hit,double timeOffset) {
        if (hit.mode==3){ // mode 3
        	if (hit.getTimeStamp() % 64 != 0) {
                System.out.println("unexpected timestamp " + hit.getTimeStamp());
            }
            double time = hit.getTimeStamp() / 16.0;
            long id = hit.getCellID();
            // Get the channel data.
            EcalChannelConstants channelData = findChannel(id);
            double adcSum = hit.getAmplitude() - hit.windowSize * channelData.getCalibration().getPedestal();
            double rawEnergy = adcToEnergy(adcSum, id);
            HPSCalorimeterHit h2 = new HPSCalorimeterHit(rawEnergy, time + timeOffset, id, 0);
            return h2;
        }
        else if (hit.mode==4){ // mode 7
        	double time = hit.getTimeStamp() * 62.5 / 1000; //in mode 7 time is in 62.5 ps units!
            long id = hit.getCellID();
            // Get the channel data.
            EcalChannelConstants channelData = findChannel(id);
            double adcSum = hit.getAmplitude() - hit.windowSize * channelData.getCalibration().getPedestal(); //A.C. is this the proper way to pedestal subtract in mode 7?
            //double adcSum = hit.getAmplitude() - hit.windowSize * hit.amplLow;                              //A.C. is this the proper way to pedestal subtract in mode 7?
            double rawEnergy = adcToEnergy(adcSum, id);
            HPSCalorimeterHit h2 = new HPSCalorimeterHit(rawEnergy, time + timeOffset, id, 0);
            return h2;
        }
        else{
        	System.out.println("Unexpected hit type (FADC acq. mode)");
        	long id = hit.getCellID();
        	EcalChannelConstants channelData = findChannel(id);
            double adcSum = hit.getAmplitude() - hit.windowSize * channelData.getCalibration().getPedestal();
        	double rawEnergy = adcToEnergy(adcSum, id);
        	HPSCalorimeterHit h2 = new HPSCalorimeterHit(rawEnergy, 0, id, 0); //Time=0 since I do not know which time to use (mode3 or mode7?)
            return h2;
        }  
    }

    public RawCalorimeterHit HitAtoD(CalorimeterHit hit, int window) {
        int time = (int) (Math.round(hit.getTime() / 4.0) * 64.0);
        long id = hit.getCellID();
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(id);
        int amplitude;
        if (constantGain) {
            amplitude = (int) Math.round((hit.getRawEnergy() / ECalUtils.MeV) / gain + window * channelData.getCalibration().getPedestal());
        } else {
            amplitude = (int) Math.round((hit.getRawEnergy() / ECalUtils.MeV) / channelData.getGain().getGain() + window * channelData.getCalibration().getPedestal());
        }
        RawCalorimeterHit h = new BaseRawCalorimeterHit(id, amplitude, time);
        return h;
    }

    /*
     * return energy (units of GeV) corresponding to the ADC sum and crystal ID
     */
    private double adcToEnergy(double adcSum, long cellID) {
    	
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(cellID);
    	
        if (use2014Gain) {
            if (constantGain) {
                return adcSum * ECalUtils.gainFactor * ECalUtils.ecalReadoutPeriod;
            } else {
                return channelData.getGain().getGain() * adcSum * ECalUtils.gainFactor * ECalUtils.ecalReadoutPeriod; // should not be used for the moment (2014/02)
            }
        } else {
            if (constantGain) {
                return gain * adcSum * ECalUtils.MeV;
            } else {
                return channelData.getGain().getGain() * adcSum * ECalUtils.MeV; //gain is defined as MeV/integrated ADC
            }
        }
    }

    /** 
     * Must be set when an object EcalRawConverter is created.
     * @param detector (long)
     */   
    public void setDetector(Detector detector) {
        // ECAL combined conditions object.
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
    }
    
    /** 
     * Convert physical ID to gain value.
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    public EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }   
}
