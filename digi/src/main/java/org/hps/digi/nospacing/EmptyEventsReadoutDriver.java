package org.hps.digi.nospacing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.geometry.Detector;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.util.Driver;
import org.lcsim.event.EventHeader;

import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.MCParticle;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.ReadoutTimestamp;


/*
 * This driver will create an empty lcsim event 
 * and call super.process() so that all of the registered 
 * drivers run over this empty event. 
 *
 *  
 */


public class EmptyEventsReadoutDriver extends ReadoutDriver{

    private int nEmptyToInsert=250;  //number of events to insert between real MC events
    private int emptyCount=0;  //counter
    //make collections for all needed by readout sim
    EventHeader emptyEvent; 
    boolean gotFirstRealEvent=false;
    //names of collections
    Map baseCollectionMap=new HashMap<String, Class<?>>();

    List<String> baseCollectionNames=Arrays.asList("EcalHits","HodoscopeHits","MCParticle","TrackerHits","TrackerHitsECal");
    List<LCMetaData> mcCollections = null;
    @Override
    public void detectorChanged(Detector det) {

	// in here, make empty collections. 
	// since these are members and don't change
	// should be able just keep adding some one
	// to empty "event"....hopefully this speeds
	// things up a lot.

	System.out.println("EmptyEventsReadoutDriver:: Setting up base map");

	baseCollectionMap.put("EcalHits",SimCalorimeterHit.class);
	baseCollectionMap.put("HodoscopeHits",SimTrackerHit.class);
	baseCollectionMap.put("MCParticle",MCParticle.class);
	baseCollectionMap.put("TrackerHits",SimTrackerHit.class);
	baseCollectionMap.put("TrackerHitsECal",SimTrackerHit.class);		      

	
    }

    
  @Override
    public void process(EventHeader event) {
      System.out.println("EmptyEventsReadoutDriver:: processing event!");
      System.out.println(event.toString());
      printCollections(event); 
      System.out.println("empty count = "+emptyCount); 
      if(!gotFirstRealEvent){
	  System.out.println("EmptyEventsReadoutDriver:: Making the empty bunch");
	  //make an empty lcsim event based on this, real event	  
	  //	  emptyEvent=makeEmptyEventFromMC(event);
	  //just get the metadata from first event
	  getMCMetaData(event);
	  gotFirstRealEvent=true;
      }

      // check if we should add empty or continue

      if(emptyCount<nEmptyToInsert){
	  //	  System.out.println("EmptyEventsReadoutDriver:: sending empty bunch to the readout");
	  //add another empty event
	  //	  clearEvent(emptyEvent);
	  emptyEvent=makeEmptyEvent();
	  System.out.println(emptyEvent.toString());
	  //	  super.process(emptyEvent);
	  processChildren(emptyEvent);
	  emptyCount++;
	  System.out.println("NEW empty count = "+emptyCount); 
	  return;
      }
      System.out.println("EmptyEventsReadoutDriver::  done with this empty bunch stream...should be a real MC+Pulser event next");
      emptyCount=0; 
      return;      
  }    

    @Override
    public void endOfData() {
	

    }

    private EventHeader makeEmptyEventFromMC(EventHeader mcEvent){
	int eventID=666666;
	long time=(long)ReadoutDataManager.getCurrentTime();	
	//this was taken from evio/src/main/java/org/hps/evio/BaseEventBuilder.java
	// Create a new LCSimEvent.
        EventHeader lcsimEvent =
	    new BaseLCSimEvent(
			       ConditionsManager.defaultInstance().getRun(),
			       eventID,
			       ConditionsManager.defaultInstance().getDetector(),
			       time);
	
	List<LCMetaData> mcCollections = new ArrayList<LCMetaData>(mcEvent.getMetaData());
        for (LCMetaData mcCollectionMeta : mcCollections) {	    
            String mcCollectionName = mcCollectionMeta.getName();
	    // check to see if this collection is in the base map
	    // if so, copy collection, clear it, and put it in new event. 
	    if (baseCollectionMap.containsKey(mcCollectionName)){
		List collection =new ArrayList<> ((List) mcEvent.get(mcCollectionName));
		collection.clear();  //remove element
		System.out.println("EmptyEventsReadoutDriver:: inserting collection "+mcCollectionName);

		this.putCollection(mcCollectionMeta, collection, lcsimEvent);	    
	    }
	}
	System.out.println("EmptyEventsReadoutDriver::returning empty event");
	return lcsimEvent;
    }

    private EventHeader makeEmptyEvent(){
	int eventID=666666;
	long time=(long)ReadoutDataManager.getCurrentTime();
	System.out.println("making an empty bunch with time = "+time);
	//this was taken from evio/src/main/java/org/hps/evio/BaseEventBuilder.java
	// Create a new LCSimEvent.
        EventHeader lcsimEvent =
	    new BaseLCSimEvent(
			       ConditionsManager.defaultInstance().getRun(),
			       eventID,
			       ConditionsManager.defaultInstance().getDetector(),
			       time);

	//	for (Map.Entry<String, Class<?>> thisEntry : baseCollectionMap.entrySet()) {
	for (String name : baseCollectionNames) {
	    //	    String name = entry.getKey();	    
	    //  use the already obtained Metadata from the first MC event
	    //  in order to get the flags right
	    System.out.println("EmptyEventsReadoutDriver:: inserting collection "+name);
	    for(LCMetaData mcCollectionMeta : mcCollections) {
		//		System.out.println("looping over collections from mcMetaData:  "+mcCollectionMeta.getName());
		if (mcCollectionMeta.getName().equals(name)){
		    List collection = new ArrayList<> ();
		    //  System.out.println("EmptyEventsReadoutDriver:: inserting collection "+name);
		    this.putCollection(mcCollectionMeta, collection, lcsimEvent);	    
		}
	    }
		
	}

	System.out.println("#######################    this should be an empty event  ###################");
	printCollections(lcsimEvent);
	System.out.println("#############################################################################");
	return lcsimEvent;

    }
       
    protected void putCollection(LCMetaData meta, List entries, EventHeader event) {
        String[] readout = meta.getStringParameters().get("READOUT_NAME");
        if (readout != null) {
            event.put(meta.getName(), entries, meta.getType(), meta.getFlags(), readout[0]);
        } else {
            event.put(meta.getName(), entries, meta.getType(), meta.getFlags());
        }
        if (this.getHistogramLevel() > HLEVEL_NORMAL)
            System.out.println("Putting collection" + meta.getName() + " into event.");
    }

    private void getMCMetaData(EventHeader mcEvent){
	mcCollections = new ArrayList<LCMetaData>(mcEvent.getMetaData());	
    }
    
    private void clearEvent(EventHeader event){
	List<LCMetaData> evtCollections = new ArrayList<LCMetaData>(event.getMetaData());
        for (LCMetaData evtCollectionMeta : evtCollections) {	    
	    String colName=evtCollectionMeta.getName();
	    List col=(List)event.get(colName);
	    if(col.size()>0){
		System.out.println("clearing collection "+colName+" of size = "+col.size());
		((List)event.get(colName)).clear();
		System.out.println(".....new size = "+col.size());
	    }
	}
    }

    private void printCollections(EventHeader event){
	List<LCMetaData> Collections = new ArrayList<LCMetaData>(event.getMetaData());
        for (LCMetaData CollectionMeta : Collections) {	    
            String CollectionName = CollectionMeta.getName();
	    // check to see if this collection is in the base map
	    // if so, copy collection, clear it, and put it in new event. 
	    List collection =new ArrayList<> ((List) event.get(CollectionName));
	    System.out.println("EmptyEventsReadoutDriver::printCollections:: "+CollectionName+" has "+collection.size()+" entries");
	}       	
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        // TODO: Probably should have some defined value - buffer seems to be filled enough from the ecal delay alone, though.
        return 0;
    }    
}
