package org.hps.readout.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.readout.TempOutputWriter;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.lcio.LCIOConstants;

/**
 * Performs readout of ECal hits.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: EcalReadoutDriver.java,v 1.4 2013/03/20 01:03:32 meeg Exp $
 */
public abstract class EcalReadoutDriver<T> extends TriggerableDriver {
	private final TempOutputWriter inputWriter = new TempOutputWriter("raw_hits_input_old.log");
	private final TempOutputWriter outputWriter = new TempOutputWriter("raw_hits_output_old.log");
	protected final TempOutputWriter verboseWriter = new TempOutputWriter("raw_hits_verbose_old.log");

    String ecalCollectionName;
    String ecalRawCollectionName = "EcalRawHits";
    String ecalReadoutName = "EcalHits";
    Class hitClass;
    //hit type as in org.lcsim.recon.calorimetry.CalorimeterHitType
    int hitType = 0;
    //number of bunches in readout cycle
    int readoutCycle = 1;
    //minimum readout value to write a hit
    double threshold = 0.0;
    //LCIO flags
    int flags = 0;
    //readout period in ns
    double readoutPeriod = 2.0;
    //readout period time offset in ns
    double readoutOffset = 0.0;
    //readout period counter
    int readoutCounter;
    public static boolean readoutBit = false;
    protected boolean debug = true;

    public EcalReadoutDriver() {
        flags += 1 << LCIOConstants.CHBIT_LONG; //store position
        flags += 1 << LCIOConstants.RCHBIT_ID1; //store cell ID
        triggerDelay = 100.0;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setEcalReadoutName(String ecalReadoutName) {
        this.ecalReadoutName = ecalReadoutName;
    }

    public void setEcalRawCollectionName(String ecalRawCollectionName) {
        this.ecalRawCollectionName = ecalRawCollectionName;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setReadoutCycle(int readoutCycle) {
        this.readoutCycle = readoutCycle;
        if (readoutCycle > 0) {
            this.readoutPeriod = readoutCycle * ClockSingleton.getDt();
        }
    }

    public void setReadoutOffset(double readoutOffset) {
        this.readoutOffset = readoutOffset;
    }

    public void setReadoutPeriod(double readoutPeriod) {
        this.readoutPeriod = readoutPeriod;
        this.readoutCycle = -1;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public void startOfData() {
    	if(debug) {
			inputWriter.initialize();
			outputWriter.initialize();
			verboseWriter.initialize();
    	}
    	
        super.startOfData();
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }

        readoutCounter = 0;

        initReadout();
    }
	
	@Override
	public void endOfData() {
		if(debug) {
			inputWriter.close();
			outputWriter.close();
			verboseWriter.close();
		}
	}

    @Override
    public void process(EventHeader event) {
        //System.out.println(this.getClass().getCanonicalName() + " - process");
        // Get the list of ECal hits.        
        List<CalorimeterHit> hits;
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            hits = event.get(CalorimeterHit.class, ecalCollectionName);
        } else {
            hits = new ArrayList<CalorimeterHit>();
        }
        
		// DEBUG :: Write the truth hits seen.
        inputWriter.write(">" + ClockSingleton.getTime());
        outputWriter.write(">" + ClockSingleton.getTime());
		inputWriter.write("Input");
		for(CalorimeterHit hit : hits) {
			inputWriter.write(String.format("%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
		}
		
		// DEBUG :: Write the event header information and truth hit
		//          data to the verbose writer.
		verboseWriter.write("\n\n\nEvent " + event.getEventNumber() + " at time t = " + ClockSingleton.getTime());
		verboseWriter.write("Saw Input Truth Hits:");
		if(hits.isEmpty()) {
			verboseWriter.write("\tNone!");
		} else {
			for(CalorimeterHit hit : hits) {
				verboseWriter.write(String.format("\tHit with %f GeV at time %f in cell %d.", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
			}
		}
		
        //write hits into buffers
        putHits(hits);

        ArrayList<T> newHits = null;

        //if at the end of a readout cycle, write buffers to hits
        if (readoutCycle > 0) {
            if ((ClockSingleton.getClock() + 1) % readoutCycle == 0) {
                if (newHits == null) {
                    newHits = new ArrayList<T>();
                }
                readHits(newHits);
                readoutCounter++;
            }
        } else {
            while (ClockSingleton.getTime() - readoutTime() + ClockSingleton.getDt() >= readoutPeriod) {
                if (newHits == null) {
                    newHits = new ArrayList<T>();
                }
                readHits(newHits);
                readoutCounter++;
            }
        }

        if (newHits != null) {
        	outputWriter.write("Output");
			for(T hit : newHits) {
				if(hit instanceof RawCalorimeterHit) {
					outputWriter.write(String.format("%d;%d;%d", ((RawCalorimeterHit) hit).getAmplitude(),
							((RawCalorimeterHit) hit).getTimeStamp(), ((RawCalorimeterHit) hit).getCellID()));
				}
			}
            event.put(ecalRawCollectionName, newHits, hitClass, flags, ecalReadoutName);
        }

        checkTrigger(event);
    }

    protected double readoutTime() {
        return readoutCounter * readoutPeriod + readoutOffset;
    }

    //read analog signal out of buffers and make hits; reset buffers
    protected abstract void readHits(List<T> hits);

    //add deposited energy to buffers
    //must be run every event, even if the list is empty
    protected abstract void putHits(List<CalorimeterHit> hits);

    @Override
    protected void processTrigger(EventHeader event) {
    }

    //initialize buffers
    protected abstract void initReadout();

    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_ECAL;
    }
}
