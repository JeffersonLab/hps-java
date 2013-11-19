package org.lcsim.hps.recon.ecal;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;

/**
 *
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 */
public class EcalRawConverter {

    private boolean debug = false;
    private boolean constantGain = false;
    private double gain;

    public EcalRawConverter() {
    }

    public void setGain(double gain) {
        constantGain = true;
        this.gain = gain;
    }

    public short sumADC(RawTrackerHit hit) {
        //Sum all pedestal subtracted ADC values 
        //return scale * (amplitude + 0.5) + pedestal;
        if (debug) {
            System.out.println("Summing ADC for hit: " + hit.toString());
        }
        double pedestal = EcalConditions.physicalToPedestal(hit.getCellID());
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
        double rawEnergy;
        if (constantGain) {
            rawEnergy = gain * sumADC(hit) * ECalUtils.MeV;
        } else {
            rawEnergy = EcalConditions.physicalToGain(id) * sumADC(hit) * ECalUtils.MeV;
        }
//        double[] pos = hit.getDetectorElement().getGeometry().getPosition().v();
        CalorimeterHit h = new HPSCalorimeterHit(rawEnergy + 0.0000001, time, id, 0);
        //+0.0000001 is a horrible hack to ensure rawEnergy!=BaseCalorimeterHit.UNSET_CORRECTED_ENERGY
        return h;
    }

    public CalorimeterHit HitDtoA(RawCalorimeterHit hit, int window) {
        if (hit.getTimeStamp() % 64 != 0) {
            System.out.println("unexpected timestamp " + hit.getTimeStamp());
        }
        double time = hit.getTimeStamp() / 16.0;
        long id = hit.getCellID();
        double rawEnergy;
        if (constantGain) {
            rawEnergy = gain * (hit.getAmplitude() - window * EcalConditions.physicalToPedestal(id)) * ECalUtils.MeV;
        } else {
            rawEnergy = EcalConditions.physicalToGain(id) * (hit.getAmplitude() - window * EcalConditions.physicalToPedestal(id)) * ECalUtils.MeV;
        }
        CalorimeterHit h = new HPSCalorimeterHit(rawEnergy + 0.0000001, time, id, 0);
        //+0.0000001 is a horrible hack to ensure rawEnergy!=BaseCalorimeterHit.UNSET_CORRECTED_ENERGY
        return h;
    }

    public RawCalorimeterHit HitAtoD(CalorimeterHit hit, int window) {
        int time = (int) (Math.round(hit.getTime()/4.0) * 64.0);
        long id = hit.getCellID();
        int amplitude;
        if (constantGain) {
            amplitude = (int) Math.round((hit.getRawEnergy() / ECalUtils.MeV) / gain + window * EcalConditions.physicalToPedestal(id));
        } else {
            amplitude = (int) Math.round((hit.getRawEnergy() / ECalUtils.MeV) / EcalConditions.physicalToGain(id) + window * EcalConditions.physicalToPedestal(id));
        }
        RawCalorimeterHit h = new BaseRawCalorimeterHit(id, amplitude, time);
        return h;
    }

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
}
