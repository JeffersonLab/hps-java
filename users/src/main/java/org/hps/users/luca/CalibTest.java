// import the required classes
package org.hps.users.luca;

//import hep.aida.ITupleColumn.String;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.String;
import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import java.util.List;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.event.base.CalorimeterHitImpl;

// the class has to be derived from the driver class
public class CalibTest extends Driver {

  // constructor
  public CalibTest() {
  }
    String ecalName;
    String ecalCollectionName;
    String clusterCollectionName;
    String calorhit;
  //  overwrite the process method
  @Override
  protected void process(EventHeader event) {
    // Get the list of mc particles from the event
    //List<MCParticle> mcParticles = event.getMCParticles();
    // Print out the number of mc particles
    //System.out.println("Event " + event.getEventNumber() + " contains " + mcParticles.size() + " mc particles.");
    // get the list of the lists listed in the event list
    //Set<List> myfirstlist = event.getLists();
   // System.out.println("this is the number of lists: "+ event.getLists().size());
  
    if(event.hasCollection(SimCalorimeterHit.class,calorhit))
    {   List<SimCalorimeterHit> myCalorHit =event.getSimCalorimeterHits(calorhit);
     System.out.println("This file has " + myCalorHit.size() + "SimClaorimeterHits object");}
    
    
   
    

    
    /*for(List  myhit : myCalorHit)
        { for(Object hit : myhit)
        {System.out.println("this event has energy: "+ hit.getRawEnergy() + "\n");} }*/
    
    
    /*for(List lista  : myfirstlist )
    { 
        System.out.println("This event contains this list: " + lista + "\n");}*/
  /*  List<Track>  myTrack = event.getTracks();
    System.out.println("this is what we get printing the tracks: "+ event.getTracks().get(event.getEventNumber()));*/

    //List<Cluster> myCluster = event.getClusters();
    /* System.out.println("this is what we get printing the Clusters: "+ myCluster.get(event.getEventNumber()));*/
    
  /* if (event.hasCollection(SimCalorimeterHit.class,ecalName)){
    List<SimCalorimeterHit> mySimCalHits = event.getSimCalorimeterHits(ecalName);
    System.out.println("this is what we get printing the calorimeter hits: "+ mySimCalHits.size());
   }
   
   
   /* List<SimTrackerHit> mySimTrackerHits = event.getSimTrackerHits(String string);
    System.out.println("this is what we get printing the tracker hits: "+ mySimTrackerHits.size());*/
   /* if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of raw ECal hits.
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

            // Make a hit map for quick lookup by ID.
            Map<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
            
            for (CalorimeterHit hit : hits) {
                hitMap.put(hit.getCellID(), hit);
            }
            
            System.out.println("Number of ECal hits: "+hitMap.size());
           
            
            
        }*/ 
   
  }
}