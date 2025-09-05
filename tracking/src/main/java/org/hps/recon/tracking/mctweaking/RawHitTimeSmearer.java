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
 * 11/17/24
 * add in cluster time smearing as well.  
 * 
 * Strip collection:  
 * "StripClusterer_SiTrackerHitStrip1D" (default)
 * Important Settables:  
 * smear: the  sigma  of smearing gaussian
 * 
 * mg...right now, I smear all hits in all layers by the same amount
 * independent of nHits.  
 */
public class RawHitTimeSmearer extends Driver {


    String smearTimeFile="foobar";
    //IMPORTANT...the layer, top/bottom/stereo/axial/slot/hole are derived from these names!!!
    Set<String> smearTheseSensors = new HashSet<String>();
    // I may eventually want this, so keep it here
    private List<SensorToSmear> _sensorsToSmear = new ArrayList<SensorToSmear>();
    //List of Sensors
    private List<HpsSiSensor> sensors = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    private static Pattern layerPattern = Pattern.compile("L(\\d+)(t|b)");
    private static Pattern allPattern = Pattern.compile("L(\\all)");
    private Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    private boolean _debug = false;

    Random r=new Random();    

    ///



    public void setSmearTimeFile(String smearFile){
	System.out.println("Setting SVT sensor time smearing file = "+smearFile);
	this.smearTimeFile=smearFile;
    }

    public void setDebug(boolean debug) {
        this._debug = debug;
    }

    public RawHitTimeSmearer() {
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
	    System.out.println(this.getClass().getName()+"::  Reading in time smearing file = "+this.smearTimeFile); 
	Map<String,Double> mapOfSmearingTime=readSmearingFile(this.smearTimeFile);        
	if (mapOfSmearingTime.size()==0)
            throw new RuntimeException(this.getClass().getName()+"::  No sensors to smear???");
	    
	for (HpsSiSensor sensor:  sensors){
	    double smearingTime=-666.;
	    if(mapOfSmearingTime.containsKey(sensor.getName())){
		// found a sensor to smear...set up the object
		if(_debug)
		    System.out.println(this.getClass().getName()+":: adding "+sensor.getName()+" with sigma time = "+mapOfSmearingTime.get(sensor.getName()));
		smearingTime=mapOfSmearingTime.get(sensor.getName());
	    }
	    if(smearingTime>0)
		_sensorsToSmear.add(new SensorToSmear(sensor, smearingTime));
	}
	if(_debug)
	    System.out.println(this.getClass().getName()+":: will smear cluster hits on "+_sensorsToSmear.size()+" sensors");
	
    }

    @Override
    public void process(EventHeader event) {

    RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
    if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
        List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
        for (LCRelation relation : trueHitRelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                rawtomc.add(relation.getFrom(), relation.getTo());
                }
            }
    }
    //else {System.out.println("NO TRUTH!!!!");	}
	// get the sensor object of the cluster
	for (SensorToSmear sensorToSmear : _sensorsToSmear){
	    if(sensorToSmear.getSmearTimeSigma()>0){
		HpsSiSensor sensor=(HpsSiSensor)sensorToSmear.getSensor();
		List< LCRelation > fittedHits = sensor.getReadout().getHits(LCRelation.class);
		for (LCRelation fittedHit : fittedHits) {
		    RawTrackerHit rth=FittedRawTrackerHit.getRawTrackerHit(fittedHit);
            Set<SimTrackerHit> simhits = rawtomc.allFrom(rth);
            if (simhits.size() == 0) continue;
            //System.out.println("MC LENGTH: " + simhits.size());

		    double oldTime=FittedRawTrackerHit.getT0(fittedHit);
		    double smearAmount=sensorToSmear.getRandomTimeSmear();
		    double newTime=oldTime+smearAmount;
		    ((FittedRawTrackerHit)fittedHit).getShapeFitParameters().setT0(newTime);
		    if (_debug)
			System.out.println("Smearing this hit Layer = " + sensor.getName()					  
					   +" smearTimeSigma= " + sensorToSmear.getSmearTimeSigma()
					   + "  fitted hit time = " + FittedRawTrackerHit.getT0(fittedHit)
					   + "  old time = " + oldTime
					   + "  new time = " + newTime
					   + "  amount smeared = " + smearAmount); 
		}
	    }
	}
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
        double _smearTimeSigma = -666.0; // units of this is ns

        public SensorToSmear(HpsSiSensor sensor, double smearTime) {
	    _smearTimeSigma=smearTime;
            _sensor = sensor;
        }

        void setSmearTimeSigma(double smear) {
            this._smearTimeSigma=smear;
        }
	
	       
        double getSmearTimeSigma() {
            return this._smearTimeSigma;
        }
	       
	HpsSiSensor getSensor(){
	    return _sensor;
	}
	
        boolean matchSensor(HpsSiSensor sensor) {
            return _sensor == sensor;
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
