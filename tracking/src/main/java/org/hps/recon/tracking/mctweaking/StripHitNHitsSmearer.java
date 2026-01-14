package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random; 
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import hep.physics.matrix.SymmetricMatrix;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.MCParticle;


/**
 *
 * @author mgraham created 9/20/24
 * This driver will randomize the position of strip clusters
 * within a specified gaussian sigma. 
 *
 * Strip collection:  
 * "StripClusterer_SiTrackerHitStrip1D" (default)
 * Important Settables:  
 * smear: the  sigma  of smearing gaussian
 * 
 * mg...right now, I smear all hits in all layers by the same amount
 * independent of nHits.  
 */
public class StripHitNHitsSmearer extends Driver {


    String smearPositionFile="foobar";
    // I may eventually want this, so keep it here
    //private List<SensorToSmear> _sensorsToSmear = new ArrayList<SensorToSmear>();
    //List of Sensors
    //private List<HpsSiSensor> sensors = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    //private static Pattern layerPattern = Pattern.compile("L(\\d+)(t|b)");
    //private static Pattern allPattern = Pattern.compile("L(\\all)");
    private String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String simHitCollectionName = "SimHits";
    private Map<Integer, Double> smearMap;
    private Map<MCParticle, Set<Integer>> mcParticleHitsByLayer = new HashMap<MCParticle, Set<Integer>>();
    private boolean _debug = false;
    Random r=new Random();    

    ///

    private List<TrackerHit> siClusters = new ArrayList<TrackerHit>();
    private List<SimTrackerHit> simTrackerHits = new ArrayList<SimTrackerHit>();

    //    private Map<TrackerHit, Boolean> _siClustersAcceptMap = new HashMap<TrackerHit, Boolean>();
    //private    Map<TrackerHit, Boolean> _finalSiClustersAcceptMap = new HashMap<TrackerHit, Boolean>();


    public void setSmearPositionFile(String smearFile){
	    System.out.println("Setting SVT sensor position smearing file = "+smearFile);
	    this.smearPositionFile=smearFile;
    }
    
    public void setDebug(boolean debug) {
        this._debug = debug;
    }

    public StripHitNHitsSmearer() {
    }

    @Override
    public void detectorChanged(Detector detector) {
        // Get the HpsSiSensor objects from the tracker detector element
        smearMap = new HashMap<>();
        String infile = "/org/hps/recon/tracking/svtTimeAndPositionSmearing/" + smearPositionFile;
        InputStream inSmearing = this.getClass().getResourceAsStream(infile);
       

        try (BufferedReader br = new BufferedReader( new InputStreamReader(inSmearing))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                System.out.println(line);

                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                System.out.println(parts[0]);
                int hits = Integer.parseInt(parts[0]);
                double smear = Double.parseDouble(parts[1]);
                smearMap.put(hits, smear);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }	
    }

    @Override
    public void process(EventHeader event) {
        List<TrackerHit> smearedSiClusters=new ArrayList<TrackerHit>();
        if (event.hasItem(stripHitInputCollectionName))
            siClusters = (List<TrackerHit>) event.get(stripHitInputCollectionName);
        else {
            System.out.println("StripHitNHitsSmearer::process No Input Collection Found?? " + stripHitInputCollectionName);
            return;
        }
        if (_debug)
            System.out.println("Number of SiClusters Found = " + siClusters.size());

        if (event.hasItem("TrackerHits"))
            simTrackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        else {
            System.out.println("StripHitNHitsSmearer::process No Input Collection Found?? " + "TrackerHits");
            return;
        }
        
        int oldClusterListSize = siClusters.size();

        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                rawtomc.add(relation.getFrom(), relation.getTo());
                }
            }
        }

        for (TrackerHit siCluster : siClusters) {  // Looping over all clusters

            boolean isMC = true;
            // Check if hits in cluster have matching MC particle
            List<RawTrackerHit> rawHits = siCluster.getRawHits();
            int total_simhits = 0;
            MCParticle smearedParticle = null;
            for (RawTrackerHit rth:rawHits) {
                Set<SimTrackerHit> simhits = rawtomc.allFrom(rth);

                for(SimTrackerHit simhit : simhits){
                    //get mcp that left simhit
                    MCParticle particle = simhit.getMCParticle();
                    smearedParticle = particle;
                    if(!mcParticleHitsByLayer.containsKey(particle)){
                            Set<Integer> hitLayers = getLayersHitByMCP(particle, simTrackerHits);
                            mcParticleHitsByLayer.put(particle, hitLayers);
                    }
                }

                //System.out.println("MC LENGTH: " + simhits.size());
                if (simhits.size() > 0) total_simhits++;
            }
            if (total_simhits == 0) {isMC = false;}
            


            boolean smearedHit = false;
	        // get the sensor object of the cluster
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) siCluster.getRawHits().get(0)).getDetectorElement();			
	        Hep3Vector newPos=toHep3(siCluster.getPosition());//set the "new" variables to the original cluster position
             
		    if (isMC) {
                int nhits = mcParticleHitsByLayer.get(smearedParticle).size();
                if (_debug) {
                    System.out.println("Found MCParticle with " + nhits + "hits");
                }
                Double smearingValue = smearMap.get(nhits);
			    //get the local u position
			    Hep3Vector pos = globalToSensor(toHep3(siCluster.getPosition()), sensor);
                double smearAmount = r.nextGaussian() * smearingValue;
			    double uPosNew=pos.x()+smearAmount;
			    //get the new global position
		        if (smearingValue > 0) {newPos=sensorToGlobal(toHep3(new double[]{uPosNew,pos.y(),pos.z()}),sensor);}
			    
			    if (_debug)
			        System.out.println("Smearing this hit Layer = " + sensor.getName()					  
					       +" smearPositionSigma= " + smearingValue
					       + "  old u = " + pos.x()
					       + "  new u = " + uPosNew
					       + "  amount smeared = " + smearAmount
                           + "  Smeared?? = " + isMC); 
			    if (_debug) {
			        System.out.println("original  position = "+toHep3(siCluster.getPosition()).toString());
			        System.out.println("global og position = "+pos.toString());
			    }		   
			// set the new position
			//			smearedTrackerHit.setPosition(new double[]{newPos.x(),newPos.y(),newPos.z()});
			
			// add the smeared hit to list and mark it
			//			smearedSiClusters.add(smearedTrackerHit);
			    smearedHit=true;		  
		    }
	    //make a smeared hit...even if the sensor hit was not smeared, use newPos/newTime since they were filled with original pos/time above
	    SiTrackerHitStrip1D smearedTrackerHit=
		new SiTrackerHitStrip1D(newPos, new SymmetricMatrix(3,siCluster.getCovMatrix(),true),
					siCluster.getdEdx(),siCluster.getTime(),(List<RawTrackerHit>)siCluster.getRawHits(),
					TrackerHitType.decoded(siCluster.getType()));
	    
	    smearedSiClusters.add(smearedTrackerHit); 
	    
        }

        if (_debug)System.out.println("New Cluster List Has " + smearedSiClusters.size() + "; old List had " + oldClusterListSize);
        // TODO flag not used
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.        
        event.remove(this.stripHitInputCollectionName);
        event.put(this.stripHitInputCollectionName, smearedSiClusters, SiTrackerHitStrip1D.class, 0, toString());
    }

    @Override
    public void endOfData() {
    }
   
   private Set<Integer> getLayersHitByMCP(MCParticle mcp, List<SimTrackerHit> simhits){
        Set<Integer> layerhitsMap = new HashSet<Integer>();
	    for(SimTrackerHit simhit : simhits){
	        MCParticle simhitmcp = simhit.getMCParticle();
	        if(simhitmcp == mcp){
		    int layer = simhit.getLayer();
		    if(!layerhitsMap.contains(layer))
		        layerhitsMap.add(layer); 	       
	        }
	    }		
	    return  layerhitsMap;
    }
    
    //Converts double array into Hep3Vector
    private Hep3Vector toHep3(double[] arr) {
        return new BasicHep3Vector(arr[0], arr[1], arr[2]);
    }
  
    //Converts position into sensor frame
    private Hep3Vector globalToSensor(Hep3Vector hitpos, HpsSiSensor sensor) {
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (electrodes == null) {
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        return electrodes.getGlobalToLocal().transformed(hitpos);
    }

    //Converts position from local (sensor) to global frame
    private Hep3Vector sensorToGlobal(Hep3Vector hitpos, HpsSiSensor sensor) {
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (electrodes == null) {
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        return electrodes.getLocalToGlobal().transformed(hitpos);
    }
}
