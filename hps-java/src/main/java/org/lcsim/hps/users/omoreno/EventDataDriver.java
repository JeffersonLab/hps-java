package org.lcsim.hps.users.omoreno;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.event.LCRelation; 
import org.lcsim.event.base.BaseLCRelation;


public class EventDataDriver extends Driver {

	
	List<EcalHitPosition> ecalHitPositions; 
	List<LCRelation> ecalHitsToPosition; 
	
	// Collection Names 
	String ecalHitsCollectionName = "EcalCalHits";
	String ecalHitsPositionCollectionName = "EcalCalHitPositions"; 
	String ecalHitsRelationCollectionName = "EcalCalHitsRelation";
	
	public EventDataDriver(){}; 
	
	public void process(EventHeader event){ 
		
		// If the event doesn't contain Ecal calorimeter hits, skip the event
		if(!event.hasCollection(CalorimeterHit.class, ecalHitsCollectionName)) return; 
		
		// Get the collections of Ecal calorimeter hits from the event 
		List<CalorimeterHit> ecalHits = event.get(CalorimeterHit.class, ecalHitsCollectionName);
		
		// Instantiate the list to contain all of the Ecal calorimeter hit positions
		ecalHitPositions = new ArrayList<EcalHitPosition>(); 
		ecalHitsToPosition = new ArrayList<LCRelation>(); 
		
		
		// Loop over all of the Ecal calorimeter hits and add the hit position 
		// to the event
		for(CalorimeterHit ecalHit : ecalHits){
			
			double[] position = ecalHit.getPosition(); 
			
			EcalHitPosition ecalHitPosition = new EcalHitPosition(position); 
			ecalHitPositions.add(ecalHitPosition); 
			ecalHitsToPosition.add(new BaseLCRelation(ecalHit, ecalHitPosition)); 
		}
	
		event.put(ecalHitsRelationCollectionName, ecalHitsToPosition, LCRelation.class, 0); 
		event.put(ecalHitsPositionCollectionName, ecalHitPositions, GenericObject.class, 0);
		
	}
	
}
