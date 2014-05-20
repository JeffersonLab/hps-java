package org.hps.users.omoreno;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

public class SvtDataRates extends Driver {

	//Map<VOLUMES, double[]> rawHitsPerLayer = new HashMap<VOLUMES, double[]>(); 
	double[][] rawHitsPerLayer = new double[12][4];
	
	//public enum VOLUMES { TOP, BOTTOM };
	
	// Collection Names
	String rawTrackerHitCollectionName = "SVTRawTrackerHits";

	double totalEvents = 0; 
	int totalLayersPerVolume = 0; 
	
	public SvtDataRates(){}
	
	//static { 
	//	hep.aida.jfree.AnalysisFactory.register();
	//}
	
	protected void detectorChanged(Detector detector){
	
		List<HpsSiSensor> sensors = detector.getDetectorElement().findDescendants(HpsSiSensor.class);
		for(HpsSiSensor sensor : sensors){
			this.printDebug("Layer: " + sensor.getLayerNumber() + " Module: " + sensor.getModuleNumber());
		}
		totalLayersPerVolume = sensors.size()/2;
		//for(VOLUMES volume : VOLUMES.values()){
		//	rawHitsPerLayer.put(volume, new double[totalLayersPerVolume]);
		//}
	}
	
	protected void process(EventHeader event){
		
		if(!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)){
			return;
		}
		
		totalEvents++; 
		
		List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
		for(RawTrackerHit rawHit : rawHits){
			
			HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement(); 
			int layer = sensor.getLayerNumber();
			int module = sensor.getModuleNumber();
			//if(sensor.isTopLayer()){
			//	rawHitsPerLayer.get(VOLUMES.TOP)[layer]++;
			//} else { 
			//	rawHitsPerLayer.get(VOLUMES.BOTTOM)[layer]++;
			//}
			
			rawHitsPerLayer[layer-1][module]++;
		}
	}
	
	protected void endOfData(){
	
		//for(VOLUMES volume : VOLUMES.values()){
			//System.out.println("Volume: " + volume);
			//System.out.println("Hits per layer per event: ");
			//for(int layer = 0; layer < totalLayersPerVolume; layer++){
			//	System.out.println("Layer: " + (layer+1) + ": " + rawHitsPerLayer.get(volume)[layer]/totalEvents); 
			//}
		//}
		
		for(int layer = 0; layer < 12; layer++){
			
			for(int module = 0; module < 4; module++){
				System.out.println("Layer: " + layer + 
									" Module: " + module + 
									" Hits Per Layer: " + rawHitsPerLayer[layer][module]/totalEvents); 
			}
		}
	}

	private void printDebug(String debugMessage){
		System.out.println(this.getClass().getSimpleName() + ": " + debugMessage);
	}
	
}
