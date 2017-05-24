package org.hps.readout.ecal;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOConstants;

/**
 * Performs readout of ECal hits.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: EcalReadoutDriver.java,v 1.4 2013/03/20 01:03:32 meeg Exp $
 */
public abstract class EcalReadoutDriver<T> extends TriggerableDriver {
    String ecalCollectionName;
    String ecalRawCollectionName = "EcalRawHits";
    String ecalReadoutName = "EcalHits";
    Class<?> hitClass;
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
    protected boolean debug = false;
    
    public EcalReadoutDriver() {
        flags += 1 << LCIOConstants.CHBIT_LONG; //store position
        flags += 1 << LCIOConstants.RCHBIT_ID1; //store cell ID
        triggerDelay = 100.0;
    }
    
    @Override
    public void startOfData() {
        // Initialize the superclass (TriggerabelDriver).
        super.startOfData();
        
        // There must be a defined value for the base calorimeter hit
        // objects. If there is not, produce an error.
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
        
        // TODO: What is the readout counter? What does it do?
        readoutCounter = 0;
        
        // TODO: What is initReadout? What does it do?
        initReadout();
    }
    
    @Override
    public void process(EventHeader event) {
        // DEBUG :: Indicate that an event is being processed and
        //          specify the current variable states.
        if(debug) {
            System.out.println("====================================================================");
            System.out.println("=== Initiating EcalReadoutDriver Method process(EventHeader) =======");
            System.out.println("====================================================================");
            System.out.println("System Variables:");
            System.out.println("\tecalCollectionName    :: " + ecalCollectionName);
            System.out.println("\tecalRawCollectionName :: " + ecalRawCollectionName);
            System.out.println("\tecalReadoutName       :: " + ecalReadoutName);
            System.out.println("\thitClass              :: " + hitClass.getSimpleName());
            System.out.println("\thitType               :: " + hitType);
            System.out.println("\treadoutCycle          :: " + readoutCycle);
            System.out.println("\tthreshold             :: " + threshold);
            System.out.println("\tflags                 :: " + flags);
            System.out.println("\treadoutPeriod         :: " + readoutPeriod);
            System.out.println("\treadoutOffset         :: " + readoutOffset);
            System.out.println("\treadoutCounter        :: " + readoutCounter);
            System.out.println("\treadoutBit            :: " + readoutBit);
            System.out.println("\tdebug                 :: " + debug);
            System.out.println();
        }
        
        // Get the base calorimeter hits collection. This contains
        // the hits produced by SLiC.
        List<CalorimeterHit> hits;
        if(event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            hits = event.get(CalorimeterHit.class, ecalCollectionName);
        } else {
            hits = new ArrayList<CalorimeterHit>();
        }
        
        // DEBUG :: Output the observed SLiC calorimeter hits.
        if(debug) {
            System.out.println("Registering SLiC hits...");
            if(hits.isEmpty()) {
                System.out.println("No hits!");
            } else {
                for(CalorimeterHit hit : hits) {
                    System.out.printf("\tHit at (%3d, %3d) with energy %5.3f GeV at time t = %f%n",
                            hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getCorrectedEnergy(),
                            hit.getTime());
                }
            }
            System.out.println();
        }
        
        // Write the hits into the hit buffer for processing. The hit
        // buffer processing is handled by the specific implementing
        // class.
        putHits(hits);
        
        // Create a new list in which to store the processed hits.
        ArrayList<T> newHits = null;
        
        // Check to see if a sufficiently large number of hits have
        // been buffered to allow for processing. If so, process the
        // hit buffer and create proper hit objects.
        if(readoutCycle > 0) {
            if((ClockSingleton.getClock() + 1) % readoutCycle == 0) {
                // TODO: Is it possible for this to NOT be null?
                if(newHits == null) {
                    newHits = new ArrayList<T>();
                }
                
                // Process the hits into the processed hits list.
                readHits(newHits);
                
                // TODO: What does this do? It does not appear to
                // serve any purpose outside of the "else" negative
                // condition. Is this also related to 2014 data?
                readoutCounter++;
            }
        }
        
        // TODO: The "readoutCycle" is set to 1 by default. In what
        // circumstances would it be set to negative? What does this
        // even represent? Is this more 2014 handling?
        else {
            while(ClockSingleton.getTime() - readoutTime() + ClockSingleton.getDt() >= readoutPeriod) {
                // TODO: Is it possible for this to NOT be null?
                if(newHits == null) {
                    newHits = new ArrayList<T>();
                }
                readHits(newHits);
                readoutCounter++;
            }
        }
        
        // If hits were processed, they must be written to the data
        // stream for access in subsequent drivers.
        if(newHits != null) {
            // Add the new hits to the data stream.
            event.put(ecalRawCollectionName, newHits, hitClass, flags, ecalReadoutName);
            
            // DEBUG :: Note that the new hits were written.
            System.out.printf("Wrote new hits of class %s to collection %s.%n", hitClass.getSimpleName(), ecalReadoutName);
        }
        
        // TODO: What does this do? Why is this event a trigger driver?
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
    protected void processTrigger(EventHeader event) { }
    
    //initialize buffers
    protected abstract void initReadout();
    
    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_ECAL;
    }
    
    // TODO: This does nothing as of now.
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
}