package org.hps.digi.nospacing;

import java.util.ArrayList;
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

/*
 * This driver will create an empty lcsim event 
 * and call super.process() so that all of the registered 
 * drivers run over this empty event. 
 *
 *  
 */


public class EmptyEventsDriver extends Driver{

    private int nEmptyToInsert=250;  //number of events to insert between real MC events
    private int emptyCount=0;  //counter
    //make collections for all needed by readout sim
    EventHeader emptyEvent; 
    boolean gotFirstRealEvent=false;
    //names of collections
    Map baseCollectionMap=new HashMap<String, Class<?>>();

    @Override
    public void detectorChanged(Detector det) {

	// in here, make empty collections. 
	// since these are members and don't change
	// should be able just keep adding some one
	// to empty "event"....hopefully this speeds
	// things up a lot.

	System.out.println("EmptyEventsDriver:: Setting up base map");

	baseCollectionMap.put("EcalHits",SimCalorimeterHit.class);
	baseCollectionMap.put("HodoscopeHits",SimTrackerHit.class);
	baseCollectionMap.put("MCParticle",MCParticle.class);
	baseCollectionMap.put("TrackerHits",SimTrackerHit.class);
	baseCollectionMap.put("TrackerHitsECal",SimTrackerHit.class);		      

	
    }

    
  @Override
    public void process(EventHeader event) {
      //      System.out.println("EmptyEventsDriver:: processing event!");

      if(!gotFirstRealEvent){
	  System.out.println("EmptyEventsDriver:: Making the empty bunch");
	  //make an empty lcsim event based on this, real event	  
	  emptyEvent=makeEmptyMCEvent(event); 
	  gotFirstRealEvent=true;
      }

      // check if we should add empty or continue

      if(emptyCount<nEmptyToInsert){
	  //	  System.out.println("EmptyEventsDriver:: sending empty bunch to the readout");
	  //add another empty event
	  clearEvent(emptyEvent);
	  super.process(emptyEvent);
	  emptyCount++;
	  return;
      }
      System.out.println("EmptyEventsDriver::  done with this empty bunch stream...should be a real MC+Pulser event next");
      emptyCount=0; 
      return;      
  }    

    @Override
    public void endOfData() {
	

    }

    private EventHeader makeEmptyMCEvent(EventHeader mcEvent){
	int eventID=666666;
	long time = 0;
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
		System.out.println("EmptyEventsDriver:: inserting collection "+mcCollectionName);

		this.putCollection(mcCollectionMeta, collection, lcsimEvent);	    
	    }
	}
	System.out.println("EmptyEventsDriver::returning empty event");
	return lcsimEvent;
    }

 
       
    protected void putCollection(LCMetaData collection, List entries, EventHeader event) {
        String[] readout = collection.getStringParameters().get("READOUT_NAME");
        if (readout != null) {
            event.put(collection.getName(), entries, collection.getType(), collection.getFlags(), readout[0]);
        } else {
            event.put(collection.getName(), entries, collection.getType(), collection.getFlags());
        }
        if (this.getHistogramLevel() > HLEVEL_NORMAL)
            System.out.println("Putting collection " + collection.getName() + " into event.");
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
    
}
