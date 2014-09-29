package org.hps.recon.ecal;

import org.hps.conditions.ConditionsDriver;
import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalConditionsUtil;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;

/**
 *
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 */
public class EcalRawConverter {

    private boolean debug = false;
    private boolean constantGain = false;
    private double gain;
    private boolean use2014Gain = true;
    
    //get the database condition manager
    
//    Detector detector = DatabaseConditionsManager.getInstance().getDetectorObject();
    Detector detector = null;
    static EcalConditions ecalConditions = null;
    static IIdentifierHelper helper = null;
    static EcalChannelCollection channels = null; 


    public EcalRawConverter() {	
    }

    public void setGain(double gain) {
        constantGain = true;
        this.gain = gain;
    }

    public void setUse2014Gain(boolean use2014Gain) {
        this.use2014Gain = use2014Gain;
    }

    private short sumADC(RawTrackerHit hit) {
        //Sum all pedestal subtracted ADC values 
        //return scale * (amplitude + 0.5) + pedestal;
        if (debug) {
            System.out.println("Summing ADC for hit: " + hit.toString());
        }
        
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(hit.getCellID());
        
        double pedestal = channelData.getCalibration().getPedestal();
        short sum = 0;
        short samples[] = hit.getADCValues();
        for (int isample = 0; isample < samples.length; ++isample) {
            sum += (samples[isample] - pedestal);
            if (debug) {
                System.out.println("Sample " + isample + " " + samples[isample] + " pedestal " + pedestal + " (" + sum + ")");
            }
        }
        return sum;
    }

    public CalorimeterHit HitDtoA(RawTrackerHit hit) {
        double time = hit.getTime();
        long id = hit.getCellID();
        double rawEnergy = adcToEnergy(sumADC(hit), id);
        HPSCalorimeterHit h1 = new HPSCalorimeterHit(rawEnergy + 0.0000001, time, id, 0);
        h1.setDetector(detector);
//        double[] pos = hit.getDetectorElement().getGeometry().getPosition().v();
        //+0.0000001 is a horrible hack to ensure rawEnergy!=BaseCalorimeterHit.UNSET_CORRECTED_ENERGY
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
        HPSCalorimeterHit h2 = new HPSCalorimeterHit(rawEnergy + 0.0000001, time + timeOffset, id, 0);
        h2.setDetector(detector);
        //+0.0000001 is a horrible hack to ensure rawEnergy!=BaseCalorimeterHit.UNSET_CORRECTED_ENERGY
        return h2;
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
    /*
     public static CalorimeterHit HitDtoA(RawCalorimeterHit hit, int window, double g) {
     if (hit.getTimeStamp() % 64 != 0) {
     System.out.println("unexpected timestamp " + hit.getTimeStamp());
     }
     double time = hit.getTimeStamp() / 16.0;
     long id = hit.getCellID();
     double rawEnergy = g * (hit.getAmplitude() - window * EcalConditions.physicalToPedestal(id)) * ECalUtils.MeV;
     CalorimeterHit h = new HPSCalorimeterHit(rawEnergy + 0.0000001, time, id, 0);
     //+0.0000001 is a horrible hack to ensure rawEnergy!=BaseCalorimeterHit.UNSET_CORRECTED_ENERGY
     return h;
     }
     */
    /** 
     * Must be set when an object EcalRawConverter is created.
     * @param detector (long)
     */   
    void setDetector(Detector detector) {
    	
//    	h1.setDetector(detector);
//    	h2.setDetector(detector);
    	
        this.detector = detector;
        
        // ECAL combined conditions object.
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        
        // List of channels.
        channels = ecalConditions.getChannelCollection();
        
        // ID helper.
        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
        
        System.out.println("You are now using the database conditions for EcalRawConverter.");
    }
    
    /** 
     * Convert physical ID to gain value.
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    private static EcalChannelConstants findChannel(long cellID) {
        // Make an ID object from raw hit ID.
        IIdentifier id = new Identifier(cellID);
        
        // Get physical field values.
        int system = helper.getValue(id, "system");
        int x = helper.getValue(id, "ix");
        int y = helper.getValue(id, "iy");
        
        // Create an ID to search for in channel collection.
        GeometryId geometryId = new GeometryId(helper, new int[] { system, x, y });
                
        // Get the channel data.
        return ecalConditions.getChannelConstants(channels.findChannel(geometryId));    
    }   
    
}
