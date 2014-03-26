
package org.hps.recon.tracking.apv25;

//--- java ---//
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_APV25_CHANNELS;
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES;
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.hps.conditions.deprecated.HPSSVTConstants;
import org.hps.conditions.deprecated.SvtUtils;
import org.hps.readout.ecal.ClockSingleton;
//--- lcsim ---//
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeData;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeDataCollection;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.config.SimTrackerHitReadoutDriver;
import org.lcsim.util.Driver;
//--- Constants ---//
//--- hps-java ---//

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: SvtReadout.java,v 1.12 2013/04/25 22:11:14 meeg Exp $
 */
public class SvtReadout extends Driver {

    private Set<SvtHalfModule> halfModules = new HashSet<SvtHalfModule>();
    private SiSensorSim siSimulation = new CDFSiSensorSim(); 
    SimTrackerHitReadoutDriver readout = null;
    // FIFO queue used to store readout times
    private Queue<Double> fifo = new LinkedList<Double>();
    
    List<String> readouts = new ArrayList<String>();
    Map<SiSensor, List<Integer>> sensorToChannel = new HashMap<SiSensor, List<Integer>>();
    
    // Assuming at 41.6 MHz clock, minimum readout time per sample is 3.36 us
    // For now, a dead time of 250 ns is fine
    double readoutDeadTimePerSample = 250; // ns
    double lastTriggerTime = 0;
    
    static private int nTriggers = 0;
    int nTriggersDropped = 0;
    int triggerLatencyTime = 0; // ns
    int eventNumber = 0;
    
    boolean debug = false;
    boolean pedestalRun = false;
    
    // Collection Names
    String apv25AnalogDataCollectioName = "APV25AnalogData";
    String simTrackerHitCollectionName = "TrackerHits";
    
    /**
     * Default Ctor
     */
    public SvtReadout(){
    }
    
    /**
     * 
     */
    public void setDebug(boolean debug){
    	this.debug = debug;
    }
    
    /**
     * 
     */
    public void setPedestalRun(boolean pedestalRun){
    	this.pedestalRun = pedestalRun;
    }
    
    /**
     * 
     */
    public void setTriggerLatencyTime(int triggerLatencyTime /* ns */){
    	this.triggerLatencyTime = triggerLatencyTime;
    }
    
    /**
     * 
     */
    static public int getNumberOfTriggers(){
    	return nTriggers;
    }
    
    /**
     * 
     */
    public void setReadoutDeadTime(int readoutDeadTimePerSample){
    	this.readoutDeadTimePerSample = readoutDeadTimePerSample;
    }
    
    /**
     * Set the SimTrackerHit collection name
     */
    public void setSimTrackerHitCollectionName(String simTrackerHitCollectionName){
        this.simTrackerHitCollectionName = simTrackerHitCollectionName;
    }
    
    /**
     * 
     */
    @Override
    public void detectorChanged(Detector detector){
        super.detectorChanged(detector);
        
        // Instantiate all SVT Half modules
        for(SiSensor sensor : SvtUtils.getInstance().getSensors()){
            halfModules.add(new SvtHalfModule(sensor));
        }
        
        // Set the trigger latency
        for(SvtHalfModule halfModule : halfModules){
        	for(Apv25Full apv : halfModule.getAllApv25s()){
        		apv.setLatency(triggerLatencyTime);
        	}
        	sensorToChannel.put(halfModule.getSensor(), new ArrayList<Integer>());
        }
        
    	// Load the driver which transfers SimTrackerHits to their 
    	// corresponding sensor readout
        if(readout == null){
        	add(new SimTrackerHitReadoutDriver(readouts));
        }
    }
    
    /**
     * 
     */
    @Override
    public void process(EventHeader event){
        super.process(event);
        
        eventNumber++;
        
        // Increment all trigger pointer and writer positions when necessary
        if((ClockSingleton.getTime() + ClockSingleton.getDt()) % HPSSVTConstants.SAMPLING_INTERVAL == 0){
        	for(SvtHalfModule halfModule : halfModules){
                for(Apv25Full apv : halfModule.getAllApv25s()){
                	apv.incrementPointerPositions();
                }
            }
        }
        
        // Create a list to hold the analog data
        List<Apv25AnalogData> analogData = new ArrayList<Apv25AnalogData>();

        // Loop over all half-modules, perform charge deposition simulation and read them out
        for(SvtHalfModule halfModule : halfModules){
            this.readoutSensor(halfModule);
        }
            
        // If an Ecal trigger is received, readout six samples from each APV25
        if(Apv25Full.readoutBit){

            nTriggers++;
            Apv25Full.readoutBit = false;

            // An APV25 cannot receive a trigger while it's still reading out samples; 
            // drop the trigger 
            if(ClockSingleton.getTime() >= (lastTriggerTime + readoutDeadTimePerSample*TOTAL_NUMBER_OF_SAMPLES)){
            
            	if(debug) System.out.println(this.getClass().getSimpleName() + ": APVs have been triggered on event " + eventNumber);
            	
                lastTriggerTime = ClockSingleton.getTime();
            
                for(int sample = 0; sample < TOTAL_NUMBER_OF_SAMPLES; sample++){
                
                    // Add the time at which each of the six samples should be collected 
                    // the trigger queue
                    fifo.offer(ClockSingleton.getTime() + sample*24);
                }
            } else {
            	if(debug) System.out.println(this.getClass().getSimpleName() + ": Trigger has been dropped!");
                //make an empty hit collection to make the DAQ happy
                //TODO: block the event builder from making an event
                event.put("SVTRawTrackerHits", new ArrayList<RawTrackerHit>(), RawTrackerHit.class, 0);
            	nTriggersDropped++;
            	nTriggers--;
            }
        }

        // Process any triggers in the queue
        if(fifo.peek() != null){
            
            if(fifo.peek() == ClockSingleton.getTime()){

            	// Clear the analog data and readout all APV25's
                analogData.addAll(this.readoutAPV25s());
                fifo.remove();
            }
        }

        if(!analogData.isEmpty())
            event.put(apv25AnalogDataCollectioName, analogData, Apv25AnalogData.class, 0);
    }
    
    /**
     * Readout the electrodes of an HPS Si sensor and inject the charge into 
     * the APV25 readout chip.
     * 
     * @param halfModule : SVT Half Module
     */
    public void readoutSensor(SvtHalfModule halfModule){
        
        // Set the sensor to be used in the charge deposition simulation
        siSimulation.setSensor(halfModule.getSensor());
        
        // Perform the charge deposition simulation
        Map<ChargeCarrier, SiElectrodeDataCollection> electrodeDataMap = siSimulation.computeElectrodeData();
        
        for (ChargeCarrier carrier : ChargeCarrier.values()) {
            
            // If the sensor is capable of collecting the given charge carrier
            // then obtain the electrode data for the sensor
            if (halfModule.getSensor().hasElectrodesOnSide(carrier)) {
                
                SiElectrodeDataCollection electrodeDataCol = electrodeDataMap.get(carrier);
                
                // If there is no electrode data available create a new instance of electrode data
                if (electrodeDataCol == null) {
                    electrodeDataCol = new SiElectrodeDataCollection();
                }
                
                // Loop over all sensor channels
                for(Integer physicalChannel : electrodeDataCol.keySet()){
                    
                    // find the APV channel number from the physical channel
                    int channel = physicalChannel - TOTAL_STRIPS_PER_SENSOR
                            + halfModule.getAPV25Number(physicalChannel)*TOTAL_APV25_CHANNELS + (TOTAL_APV25_CHANNELS - 1); 
                    
                    // Only inject charge if the channels isn't considered bad
                    if(halfModule.getAPV25(physicalChannel).getChannel(channel).isBadChannel()) continue;
                    
                    // Get the electrode data for this channel
                    SiElectrodeData electrodeData = electrodeDataCol.get(physicalChannel);
                    
                    // Get the charge in units of electrons
                    double charge = pedestalRun ? 0 : electrodeData.getCharge();
                    
                    if(debug){
                        if(charge > 0){ 
                        	System.out.println(this.getClass().getSimpleName() 
                        		+ ": Sensor: " + SvtUtils.getInstance().getDescription(halfModule.getSensor()) 
                        		+ ": Injecting charge " + charge + " into channel " + physicalChannel);
                        	sensorToChannel.get(halfModule.getSensor()).add(physicalChannel);
                        }
                    }
                    
                    // Inject the charge into the APV25 amplifier chain
                    halfModule.getAPV25(physicalChannel).injectCharge(channel, charge);
                }
            }
        }
        
        // Clear the sensors of all deposited charge
        siSimulation.clearReadout();
    }
    
    /**
     * 
     */
    public List<Apv25AnalogData> readoutAPV25s(){
        
        // Create a list to hold the analog data
        List<Apv25AnalogData> analogData = new ArrayList<Apv25AnalogData>();

        for(SvtHalfModule halfModule : halfModules){
            
            // Get the sensor associated with this half-module
            SiSensor sensor = halfModule.getSensor();
            
            // Get all of the APVs associated with the sensor
            Apv25Full[] apv25 = halfModule.getAllApv25s();
        
            if(debug){
            	for(int physicalChannel = 0; physicalChannel < TOTAL_APV25_CHANNELS; physicalChannel++){
            		if(sensorToChannel.get(halfModule.getSensor()).contains(physicalChannel)){
            			int channel = physicalChannel - TOTAL_STRIPS_PER_SENSOR
                                + halfModule.getAPV25Number(physicalChannel)*TOTAL_APV25_CHANNELS + (TOTAL_APV25_CHANNELS - 1); 
            			System.out.println("\nPhysical Channel: " + physicalChannel 
            					+ " Sensor: " + SvtUtils.getInstance().getDescription(halfModule.getSensor())
            					+ apv25[halfModule.getAPV25Number(physicalChannel)].getChannel(channel).getPipeline().toString() + "\n");
            		}
            	}
            }

            // Readout all APV25's 
            for(int apvN = 0; apvN < apv25.length; apvN++){
                Apv25AnalogData analogDatum = apv25[apvN].readOut();
                analogDatum.setSensor(sensor);
                analogDatum.setApv(apvN);
                analogData.add(analogDatum);

            }
            sensorToChannel.get(halfModule.getSensor()).clear();
        }
        
        return analogData;
    }
}
