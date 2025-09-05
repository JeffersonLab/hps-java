package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
public class StripHitSmearer extends Driver {


    String smearPositionFile="foobar";
    //IMPORTANT...the layer, top/bottom/stereo/axial/slot/hole are derived from these names!!!
    Set<String> smearTheseSensors = new HashSet<String>();
    // I may eventually want this, so keep it here
    private List<SensorToSmear> _sensorsToSmear = new ArrayList<SensorToSmear>();
    //List of Sensors
    private List<HpsSiSensor> sensors = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    private static Pattern layerPattern = Pattern.compile("L(\\d+)(t|b)");
    private static Pattern allPattern = Pattern.compile("L(\\all)");
    private String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private boolean _debug = false;

    Random r=new Random();    

    ///

    private List<TrackerHit> siClusters = new ArrayList<TrackerHit>();

    //    private Map<TrackerHit, Boolean> _siClustersAcceptMap = new HashMap<TrackerHit, Boolean>();
    //private    Map<TrackerHit, Boolean> _finalSiClustersAcceptMap = new HashMap<TrackerHit, Boolean>();


    public void setSmearPositionFile(String smearFile){
	System.out.println("Setting SVT sensor position smearing file = "+smearFile);
	this.smearPositionFile=smearFile;
    }
    
    public void setDebug(boolean debug) {
        this._debug = debug;
    }

    public StripHitSmearer() {
    }

    @Override
    public void detectorChanged(Detector detector) {
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                .getDetectorElement().findDescendants(HpsSiSensor.class);
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0)
            throw new RuntimeException(this.getClass().getName()+"::  No sensors were found in this detector.");
	if(_debug)
	    System.out.println(this.getClass().getName()+":: Reading in position smearing file = "+this.smearPositionFile); 
	Map<String,Double> mapOfSmearingPosition=readSmearingFile(this.smearPositionFile);        
	if (mapOfSmearingPosition.size()==0)
            throw new RuntimeException(this.getClass().getName()+"::  No sensors to smear???");
	    
	for (HpsSiSensor sensor:  sensors){
	    double smearingPosition=-666.;
        System.out.println("Sensor Name: " + sensor.getName());
	    if(mapOfSmearingPosition.containsKey(sensor.getName())){
		// found a sensor to smear...set up the object
		if(_debug)
		    System.out.println(this.getClass().getName()+":: adding "+sensor.getName()+" with sigma = "+mapOfSmearingPosition.get(sensor.getName()));
		smearingPosition=mapOfSmearingPosition.get(sensor.getName());
		//		_sensorsToSmear.add(new SensorToSmear(sensor,mapOfSmearingPosition.get(sensor.getName())));		
	    }
	    if(smearingPosition>0)
		_sensorsToSmear.add(new SensorToSmear(sensor,smearingPosition));
	}
	if(_debug)
	    System.out.println(this.getClass().getName()+":: will smear cluster hits on "+_sensorsToSmear.size()+" sensors");
	
    }

    @Override
    public void process(EventHeader event) {
        List<TrackerHit> smearedSiClusters=new ArrayList<TrackerHit>();
        if (event.hasItem(stripHitInputCollectionName))
            siClusters = (List<TrackerHit>) event.get(stripHitInputCollectionName);
        else {
            System.out.println("StripHitSmearer::process No Input Collection Found?? " + stripHitInputCollectionName);
            return;
        }
        if (_debug)
            System.out.println("Number of SiClusters Found = " + siClusters.size());
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
            for (RawTrackerHit rth:rawHits) {
                Set<SimTrackerHit> simhits = rawtomc.allFrom(rth);
                //System.out.println("MC LENGTH: " + simhits.size());
                if (simhits.size() > 0) total_simhits++;
            }
            if (total_simhits == 0) {isMC = false;}


            boolean smearedHit = false;
	    // get the sensor object of the cluster
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) siCluster.getRawHits().get(0)).getDetectorElement();			
	    Hep3Vector newPos=toHep3(siCluster.getPosition());//set the "new" variables to the original cluster position
            for (SensorToSmear sensorToSmear : _sensorsToSmear)
		//get the sensorToSmear object in order to look up smearing size
                if (sensorToSmear.matchSensor(sensor)) {  
		    if((sensorToSmear.getSmearPositionSigma()>0) & (isMC)){
			//get the local u position
			Hep3Vector pos = globalToSensor(toHep3(siCluster.getPosition()), sensor);
			
			//this gives the amount to move the cluster based on the sigma assigned to the sensor 
			double smearAmount=sensorToSmear.getRandomPositionSmear();
			double uPosNew=pos.x()+smearAmount;
			//get the new global position
		        if (isMC) {newPos=sensorToGlobal(toHep3(new double[]{uPosNew,pos.y(),pos.z()}),sensor);}
			//make a copy of cluster and set the new position
			//			SiTrackerHitStrip1D smearedTrackerHit=new SiTrackerHitStrip1D(newPos, new SymmetricMatrix(3,siCluster.getCovMatrix(),true),
			//siCluster.getdEdx(),siCluster.getTime(),(List<RawTrackerHit>)siCluster.getRawHits(),TrackerHitType.decoded(siCluster.getType()));
			
			if (_debug)
			    System.out.println("Smearing this hit Layer = " + sensor.getName()					  
					       +" smearPositionSigma= " + sensorToSmear.getSmearPositionSigma()
					       + "  old u = " + pos.x()
					       + "  new u = " + uPosNew
					       + "  amount smeared = " + smearAmount
                            + "  Smeared?? = " + isMC); 
			if(_debug){
			    System.out.println("original  position = "+toHep3(siCluster.getPosition()).toString());
			    System.out.println("global og position = "+pos.toString());
			}		   
			// set the new position
			//			smearedTrackerHit.setPosition(new double[]{newPos.x(),newPos.y(),newPos.z()});
			
			// add the smeared hit to list and mark it
			//			smearedSiClusters.add(smearedTrackerHit);
			smearedHit=true;
		    }		  
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

    public Map<String,Double> readSmearingFile(String smearFile){
	Map<String,Double> sensorNameSmearingMap = new HashMap<String, Double>();
	String infile = "/org/hps/recon/tracking/svtTimeAndPositionSmearing/" +smearFile;
	if (_debug)System.out.println(this.getClass().getName()+"::Reading sensor smearing file " + infile);
	InputStream inSensors = this.getClass().getResourceAsStream(infile);
	BufferedReader reader = new BufferedReader(new InputStreamReader(inSensors));
	String line;
	String delims = "\\s+";// this will split strings between one or more spaces
	try {
	    while ((line = reader.readLine()) != null) {
		String[] tokens = line.split(delims);
		if (_debug) System.out.println("sensor name = " + tokens[0] + "; smearing = " + tokens[1]+" mm");
		String sensorName=tokens[0];
		Double smearingSigma=Double.parseDouble(tokens[1]);
		sensorNameSmearingMap.put(sensorName,smearingSigma); 
	    }
	} catch (IOException ex) {
	    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
	}
	return sensorNameSmearingMap; 
	
    }  
   
    public class SensorToSmear {

        int _layer = 1;
        HpsSiSensor _sensor = null;
        double _smearPositionSigma = -666.0; // units of this is mm
        double _smearTimeSigma = -666.0; // units of this is ns

        public SensorToSmear(HpsSiSensor sensor, double smearPos,double smearTime) {
	    _smearPositionSigma=smearPos;
	    _smearTimeSigma=smearTime;
            _sensor = sensor;
        }
	public SensorToSmear(HpsSiSensor sensor, double smearPos) {
	    _smearPositionSigma=smearPos;
            _sensor = sensor;
        }
        void setSmearPositionSigma(double smear) {
            this._smearPositionSigma=smear;
        }
	
        void setSmearTimeSigma(double smear) {
            this._smearTimeSigma=smear;
        }
	
        double getSmearPositionSigma() {
            return this._smearPositionSigma;
        }
	       
        double getSmearTimeSigma() {
            return this._smearTimeSigma;
        }
	       
        boolean matchSensor(HpsSiSensor sensor) {
            return _sensor == sensor;
        }

	double getRandomPositionSmear(){
	    if(this._smearPositionSigma>0)		
		return r.nextGaussian()*this._smearPositionSigma;
	    else
		return 0.0;
	}
	double getRandomTimeSmear(){
	    if(this._smearTimeSigma>0)		
		return r.nextGaussian()*this._smearTimeSigma;
	    else
		return 0.0;
	}
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
