package org.hps.users.kmccarty;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hps.readout.ecal.ClockSingleton;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseSimCalorimeterHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

public class HodoscopeHitDriver extends Driver {
	private String inputHitCollection = "HodoscopeHits";
	private String outputHitCollection = "SimHodoscopeHits";
	
	private final List<Object> particles = new ArrayList<Object>();
	private final List<Float> energies = new ArrayList<Float>();
	private final List<Float> times = new ArrayList<Float>();
	private final List<Integer> pids = new ArrayList<Integer>();
	private final Map<Point, List<SimTrackerHit>> energyDepMap = new HashMap<Point, List<SimTrackerHit>>();
	
	private final double[][][] crystalCenters = {
			{
				new double[] {  46.5, 26.7 }, new double[] {  61.5, 26.7 }, new double[] {  76.5, 26.7 }, new double[] {  94,   26.7 },
				new double[] { 114,   26.7 }, new double[] { 134,   26.7 }, new double[] { 159,   26.7 }, new double[] { 192.5, 39.2 }
			},
			{
				new double[] {  46.5, 59.1 }, new double[] {  61.5, 59.1 }, new double[] {  76.5, 59.1 }, new double[] {  94,   59.1 },
				new double[] { 114,   59.1 }, new double[] { 134,   59.1 }, new double[] { 159,   59.1 }, new double[] { 192.5, 59.1 }
			}
	};
	
	@Override
	public void process(EventHeader event) {
		// Add an empty list for hodoscope hits.
		// TODO:  Re-enable this later.
		//event.put(outputHitCollection, new ArrayList<SimCalorimeterHit>(), SimCalorimeterHit.class, 1);
		
		// Get the hodoscope hits.
		List<SimTrackerHit> hodoscopeHits = null;
		if(event.hasCollection(SimTrackerHit.class, inputHitCollection)) {
			hodoscopeHits = event.get(SimTrackerHit.class, inputHitCollection);
		} else {
			System.err.println("No hodoscope hit collection found!");
			return;
		}
		
		// Iterate over each hodoscope hit. Each crystal should have
		// and energy deposition assigned to it based on the total
		// energy from charged particles that pass through that
		// particular crystal. Positions should be defined as the
		// center of the crystal.
		energyDepMap.clear();
		for(SimTrackerHit hodoHit : hodoscopeHits) {
			// Particles without charge do not contribute hits to the
			// hodoscope and should be skipped.
			//if(hodoHit.getMCParticle().getCharge() == 0) {
			//	continue;
			//}
			
			// Get the index for the hodoscope hit and its energy
			// deposition.
			Point index = getIndex(hodoHit);
			
			// Add the energy to the total energy for this hodoscope
			// crystal in the map.
			if(energyDepMap.containsKey(index)) {
				energyDepMap.get(index).add(hodoHit);
			} else {
				energyDepMap.put(index, new ArrayList<SimTrackerHit>());
				energyDepMap.get(index).add(hodoHit);
			}
		}
		
		// Create a list to store simulated hodoscope hits.
		// TODO: Remove the temporary object array.
		List<TempHodoscopeHit> tempHits = new ArrayList<TempHodoscopeHit>();
		List<SimCalorimeterHit> processedHits = new ArrayList<SimCalorimeterHit>();
		
		// Lastly, form proper LCIO hits from the compiled energy
		// depositions in the map.
		double eventTime = ClockSingleton.getTime();
		for(Entry<Point, List<SimTrackerHit>> entry : energyDepMap.entrySet()) {
			// Store data applicable to the entire hit.
			double energy = 0;
			double[] position = new double[3];
			position[0] = crystalCenters[Math.abs(entry.getKey().y) - 1][entry.getKey().x - 1][0];
			position[1] = crystalCenters[Math.abs(entry.getKey().y) - 1][entry.getKey().x - 1][1];
			position[2] = 1100;
			position[0] = position[0] + 21.17;
			if(entry.getKey().y < 0) { position[1] = -position[1]; }
			
			// Iterate over all contributing particles and collect
			// their data.
			for(SimTrackerHit contHit : entry.getValue()) {
				energy += contHit.getdEdx();
				particles.add(contHit.getMCParticle());
				energies.add((float) contHit.getdEdx());
				times.add((float) contHit.getTime());
				pids.add(contHit.getMCParticle().getPDGID());
			}
			
			// Compile the objects needed for the hit object.
			int[] pidArray = getIntArray(pids);
			float[] energyArray = getFloatArray(energies);
			float[] timeArray = getFloatArray(times);
			Object[] particleArray = particles.toArray(new Object[particles.size()]);
			// TODO: Handle id field.
			// TODO: Handle LCMetaData field. Seems to need "Readout" to work.
			// TODO: Store crystal indices and positional data.
			
			// TODO: Re-enable when this works.
			// Create the new hit object.
			//SimCalorimeterHit hodoscopeHit = new BaseSimCalorimeterHit(0, energy, eventTime,
			//		particleArray, energyArray, timeArray, pidArray, null);
			//processedHits.add(hodoscopeHit);
			
			// TODO: Remove the temporary object additions.
			tempHits.add(new TempHodoscopeHit(entry.getKey(), energy, eventTime, position[0], position[1], position[2]));
			
			// Clear the arrays.
			pids.clear();
			times.clear();
			energies.clear();
			particles.clear();
		}
		
		// Add the simulated hits to the event.
		// TODO:  Switch back to the correct class.
		//event.get(SimCalorimeterHit.class, outputHitCollection).addAll(processedHits);
		event.put(outputHitCollection, tempHits, TempHodoscopeHit.class, 1 << LCIOConstants.BITTransient);
		if(!processedHits.isEmpty()) {
			System.out.println("Wrote " + processedHits.size() + " hodoscope hits.");
		}
	}
	
	private static final float[] getFloatArray(List<Float> list) {
		float[] array = new float[list.size()];
		for(int i = 0; i < list.size(); i++) {
			array[i] = list.get(i).floatValue();
		}
		return array;
	}
	
	private static final int[] getIntArray(List<Integer> list) {
		int[] array = new int[list.size()];
		for(int i = 0; i < list.size(); i++) {
			array[i] = list.get(i).intValue();
		}
		return array;
	}
	
	private static final Point getIndex(SimTrackerHit hodoscopeHit) {
		// Store the indices of the hodoscope hit.
		int ix;
		int iy;
		
		// Determine the x-index of the hodoscope hit.
		double x = hodoscopeHit.getPosition()[0];
		if(x < 21.17 + 39) {
			ix = 0;
		} else if(x < 21.17 + 54) {
			ix = 1;
		} else if(x < 21.17 + 69) {
			ix = 2;
		} else if(x < 21.17 + 84) {
			ix = 3;
		} else if(x < 21.17 + 104) {
			ix = 4;
		} else if(x < 21.17 + 124) {
			ix = 5;
		} else if(x < 21.17 + 144) {
			ix = 6;
		} else if(x < 21.17 + 174) {
			ix = 7;
		} else if(x < 21.17 + 211) {
			ix = 8;
		} else {
			ix = 9;
		}
		
		// Determine the y-index of the hodoscope hit.
		double y = Math.abs(hodoscopeHit.getPosition()[1]);
		if(y < 14.2) {
			iy = 0;
		} else if(y < 39.2) {
			iy = 1;
		} else if(y < 79.1) {
			iy = 2;
		} else {
			iy = 3;
		}
		if(Math.signum(hodoscopeHit.getPosition()[1]) == -1) {
			iy = -iy;
		}
		
		// Return the result.
		return new Point(ix, iy);
	}
	
	public void setInputHitCollectionName(String collection) {
		inputHitCollection = collection;
	}
	
	public void setOutputHitCollectionName(String collection) {
		outputHitCollection = collection;
	}
	
	public class TempHodoscopeHit {
		public final double time;
		public final Point index;
		public final double energyDep;
		public final double[] position;
		
		public TempHodoscopeHit(Point index, double energy, double time, double x, double y, double z) {
			this.time = time;
			this.index = index;
			this.energyDep = energy;
			this.position = new double[] { x, y, z };
		}
	}
}