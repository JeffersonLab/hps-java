package org.hps.recon.ecal;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.hps.recon.ecal.daqconfig.ConfigurationManager;
import org.hps.recon.ecal.daqconfig.FADCConfig;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;

/**
 * 
 * This is a EcalRawConverter used only for pure firmware emulation for
 * studying trigger efficiency from real data.  It requires the DAQ
 * configuration to be read from EVIO in order to set the parameters.
 * 
 */
public class EcalOnlineRawConverter {

    private FADCConfig config = null;
    private static final int nsPerSample = 4;
    private int nPeak = 3;
    
    public EcalOnlineRawConverter() {
    	// Track changes in the DAQ configuration.
    	ConfigurationManager.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Get the FADC configuration.
				config = ConfigurationManager.getInstance().getFADCConfig();
				// Get the number of peaks.
				if(config.getMode() == 1) nPeak = Integer.MAX_VALUE;
				else                      nPeak = config.getMaxPulses();
				// Print the FADC configuration.
				System.out.println();
				System.out.println();
				System.out.printf("NSA            :: %d ns%n", config.getNSA());
				System.out.printf("NSB            :: %d ns%n", config.getNSB());
				System.out.printf("Window Samples :: %d clock-cycles%n", config.getWindowWidth());
				System.out.printf("Max Peaks      :: %d peaks%n", nPeak);
				System.out.println("======================================================================");
				System.out.println("=== FADC Pulse-Processing Settings ===================================");
				System.out.println("======================================================================");
				config.printConfig();
			}
    	});
    }

    /**
     * Get pedestal for entire pulse integral.  Account for clipping if
     * windowSamples is greater than zero.
     */
    public double getPulsePedestal(EventHeader event,long cellID,int windowSamples,int thresholdCrossing) {
        int firstSample,lastSample;
        if ( windowSamples>0 && (config.getNSA()+config.getNSB())/nsPerSample >= windowSamples ) {
            // special case where firmware always integrates entire window
            firstSample = 0;
            lastSample = windowSamples-1;
        } else {
            firstSample = thresholdCrossing - config.getNSB()/nsPerSample;
            lastSample  = thresholdCrossing + config.getNSA()/nsPerSample-1;
            if (windowSamples > 0) {
                // properly pedestal subtract pulses clipped by edge(s) of readout window:
                if (firstSample < 0) firstSample=0;
                if (lastSample >= windowSamples) lastSample=windowSamples-1;
            }
        }
        return (lastSample-firstSample+1)*config.getPedestal(cellID);
    }
   
    
    /**
     * Emulate the FADC250 firmware in conversion of Mode-1 waveform to a Mode-3/7 pulse,
     * given a time for threshold crossing.
     */
    public double[] convertWaveformToPulse(RawTrackerHit hit,int thresholdCrossing,boolean mode7) {
       
        short samples[] = hit.getADCValues();
        // choose integration range:
        int firstSample,lastSample;
        if ((config.getNSA()+config.getNSB())/nsPerSample >= samples.length) {
            // firmware treats this case specially:
            firstSample = 0;
            lastSample = samples.length-1;
        } else {
            firstSample = thresholdCrossing - config.getNSB()/nsPerSample;
            lastSample  = thresholdCrossing + config.getNSA()/nsPerSample - 1;
        }
        
        // pulse integral:
        double sumADC = 0;
        for (int jj=firstSample; jj<=lastSample; jj++) {
            if (jj<0) continue;
            if (jj>=samples.length) break;
            sumADC += samples[jj];
        }

        // pulse time with 4ns resolution:
        double pulseTime=thresholdCrossing*nsPerSample;
        return new double []{pulseTime,sumADC};
    }
   
    
    /**
     *
     */
    public ArrayList <CalorimeterHit> HitDtoA(EventHeader event, RawTrackerHit hit) {
        final long cellID = hit.getCellID();
        final short samples[] = hit.getADCValues();
        if(samples.length == 0) return null;
        
        // threshold is pedestal plus threshold configuration parameter:
        final int absoluteThreshold;
        int leadingEdgeThreshold = config.getThreshold(cellID);
        absoluteThreshold = (int) (config.getPedestal(cellID) + leadingEdgeThreshold);
        
        ArrayList <Integer> thresholdCrossings = new ArrayList<Integer>();
        
        // special case, first sample is above threshold:
        if (samples[0] > absoluteThreshold) {
            thresholdCrossings.add(0);
        } 
        
        // search for threshold crossings:
        for(int ii = 1; ii < samples.length; ++ii) {
            if ( samples[ii]   >  absoluteThreshold && 
                 samples[ii-1] <= absoluteThreshold) {
                
                // found one:
                thresholdCrossings.add(ii);

                // search for next threshold crossing begins at end of this pulse:
                if (ConfigurationManager.getInstance().getFADCConfig().getMode() == 1) {
                    // special case, emulating SSP:
                	ii += 8;
                } else {
                    // "normal" case, emulating FADC250:
                	ii += config.getNSA()/nsPerSample - 1;
                }

                // firmware limit on # of peaks:
                if (thresholdCrossings.size() >= nPeak) break;
            }
        }
        
        // make hits
        ArrayList <CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
        for(int thresholdCrossing : thresholdCrossings) {
            // do pulse integral:
            final double[] data = convertWaveformToPulse(hit, thresholdCrossing, false);
            double time = data[0];
            double sum = data[1];
            
            // do pedestal subtraction:
            sum -= getPulsePedestal(event, cellID, samples.length, thresholdCrossing);
          
            // do gain scaling:
            double energy = adcToEnergy(sum, cellID);
            
            newHits.add(CalorimeterHitUtilities.create(energy,time,cellID));
        }
        
        return newHits;
    }

    /**
     * This HitDtoA is for Mode-3 data.
     */
    public CalorimeterHit HitDtoA(EventHeader event,RawCalorimeterHit hit, double timeOffset) {
        if (hit.getTimeStamp() % 64 != 0) {
            System.out.println("unexpected timestamp " + hit.getTimeStamp());
        }
        double time = hit.getTimeStamp() / 16.0;
        long id = hit.getCellID();
        double pedestal = getPulsePedestal(event,id,config.getWindowWidth(),(int)time/nsPerSample);
        double adcSum = hit.getAmplitude() - pedestal;
        double rawEnergy = adcToEnergy(adcSum, id);
        return CalorimeterHitUtilities.create(rawEnergy, time + timeOffset, id);
    }

    /**
     * This HitDtoA is exclusively for Mode-7 data, hence the GenericObject parameter.
     */
    public CalorimeterHit HitDtoA(EventHeader event,RawCalorimeterHit hit, GenericObject mode7Data, double timeOffset) {
        double time = hit.getTimeStamp() / 16.0; //timestamps use the full 62.5 ps resolution
        long id = hit.getCellID();
        double pedestal = getPulsePedestal(event,id,config.getWindowWidth(),(int)time/nsPerSample);
        double adcSum = hit.getAmplitude() - pedestal;
        double rawEnergy = adcToEnergy(adcSum, id);       
        return CalorimeterHitUtilities.create(rawEnergy, time + timeOffset, id);
    }


    /**
     * return energy (units of GeV) corresponding to the ADC sum and crystal ID
     */
    private double adcToEnergy(double adcSum, long cellID) {
        return config.getGain(cellID) * adcSum * EcalUtils.MeV;
    }

}
