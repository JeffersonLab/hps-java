package org.hps.users.kmccarty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

public class HodoscopeMultiplaneAnalysis extends Driver {
	private final AIDA aida = AIDA.defaultInstance();
	private IHistogram1D svtHitZPosition = aida.histogram1D("SVT Hit z-Position", 1600, 0.0, 1600.0);
	private IHistogram1D hodoscopeHitZPosition = aida.histogram1D("Hodoscope Hit z-Position", 1600, 0.0, 1600.0);
	private IHistogram2D svtHitZPositionLayer = aida.histogram2D("SVT Hit Layer vs. z-Position", 1600, 0.0, 1600.0, 13, -0.5, 12.5);
	
	private static final int POSITRON = 0;
	private static final int ELECTRON = 1;
	private IHistogram1D[] svtLayersTraversed = new IHistogram1D[2];
	private IHistogram1D[] ecalHitXPosition = new IHistogram1D[2];
	private IHistogram2D[] ecalHitXYPosition = new IHistogram2D[2];
	private IHistogram1D[][] hodoscopeHitΔx = new IHistogram1D[2][6];
	private IHistogram2D[][] hodoscopeHitXEcalX = new IHistogram2D[2][6];
	private IHistogram1D[][] hodoscopeHitXPosition = new IHistogram1D[2][6];
	private IHistogram2D[][] hodoscopeHitXYPosition = new IHistogram2D[2][6];
	
	private IHistogram2D[][][] hodoscopePixelHitXEcalX = new IHistogram2D[2][6][4];
	
	private IHistogram1D[] ecalHitEnergy = new IHistogram1D[2];
	//private IHistogram2D[] ecalHitXPositionEnergy = new IHistogram2D[2];
	private IHistogram2D[] ecalHitYPositionEnergy = new IHistogram2D[2];
	private IHistogram1D[][] hodoscopeHitEnergy = new IHistogram1D[2][6];
	//private IHistogram2D[][] hodoscopeHitXPositionEnergy = new IHistogram2D[2][6];
	//private IHistogram2D[][] hodoscopeHitYPositionEnergy = new IHistogram2D[2][6];
	
	//private IHistogram2D weirdHitOrigins = aida.histogram2D("Weird Hit Origins", 800, -400, 400, 1600, 0, 1600);
	//private IHistogram2D weirdHitEnergy = aida.histogram2D("Weird Hit z vs. |p|", 1300, 0.000, 2.600, 1600, 0, 1600);
	
	private boolean isDualPlane = true;
	
	@Override
	public void startOfData() {
		String[] layer;
		if(isDualPlane) {
			layer = new String[] {
					" (z = 1090 mm)", " (z = 1100 mm)"
			};
		} else {
			layer = new String[] {
					" (z = 1000 mm)", " (z = 1050 mm)", " (z = 1100 mm)", " (z = 1150 mm)", " (z = 1200 mm)", " (z = 1250 mm)"
			};
		}
		String[] type = { "Positron", "Electron" };
		for(int i = 0; i < 2; i++) {
			ecalHitEnergy[i] = aida.histogram1D("Calorimeter Hit " + type[i] + " Momentum", 1300, 0.000, 2.600);
			ecalHitXPosition[i] = aida.histogram1D("Calorimeter Hit " + type[i] + " x-Position", 800, -400, 400);
			//ecalHitXPositionEnergy[i] = aida.histogram2D("Calorimeter Hit " + type[i] + " x-Position vs. Momentum",
			//		1300, 0.000, 2.600, 500, -250, 250);
			ecalHitYPositionEnergy[i] = aida.histogram2D("Calorimeter Hit " + type[i] + " y-Position vs. Momentum",
					1300, 0.000, 2.600, 500, -250, 250);
			ecalHitXYPosition[i] = aida.histogram2D("Calorimeter Hit " + type[i] + " xy-Position", 800, -400, 400, 500, -250, 250);
			svtLayersTraversed[i] = aida.histogram1D(type[i] + " SVT Layers Traversed", 7, -0.5, 6.5);
			
			for(int j = 0; j < layer.length; j++) {
				hodoscopeHitEnergy[i][j] = aida.histogram1D("Hodoscope Hit " + type[i] + " Momentum" + layer[j], 1300, 0, 2.600);
				hodoscopeHitΔx[i][j] = aida.histogram1D(type[i] + " Hodoscope Hit #Deltax" + layer[j], 800, -400, 400);
				//hodoscopeHitXPositionEnergy[i][j] = aida.histogram2D("Hodoscope Hit " + type[i] + " x-Position vs. Momentum" + layer[j],
				//		1300, 0.000, 2.600, 500, -250, 250);
				//hodoscopeHitYPositionEnergy[i][j] = aida.histogram2D("Hodoscope Hit " + type[i] + " y-Position vs. Momentum" + layer[j],
				//		1300, 0.000, 2.600, 500, -250, 250);
				hodoscopeHitXPosition[i][j] = aida.histogram1D("Hodoscope Hit " + type[i] + " x-Position" + layer[j], 310, 0, 310);
				hodoscopeHitXYPosition[i][j] = aida.histogram2D("Hodoscope Hit " + type[i] + " xy-Position" + layer[j],
						310, 0, 310, 400, -200, 200);
				hodoscopeHitXEcalX[i][j] = aida.histogram2D(type[i] + " Hodoscope Hit x vs. Calorimeter Hit x" + layer[j],
						800, -400, 400, 310, 0, 310);
				
				hodoscopePixelHitXEcalX[i][j][0] = aida.histogram2D(
						type[i] + " Pixelized Hodoscope x vs. Calorimeter x (10x10 mm)" + layer[j],
						62, -403, 403, 31, 0, 310);
				hodoscopePixelHitXEcalX[i][j][1] = aida.histogram2D(
						type[i] + " Pixelized Hodoscope x vs. Calorimeter x (14x14 mm)" + layer[j],
						62, -403, 403, 23, 0, 322);
				hodoscopePixelHitXEcalX[i][j][2] = aida.histogram2D(
						type[i] + " Pixelized Hodoscope x vs. Calorimeter x (18x18 mm)" + layer[j],
						62, -403, 403, 18, 0, 324);
				hodoscopePixelHitXEcalX[i][j][3] = aida.histogram2D(
						type[i] + " Pixelized Hodoscope x vs. Calorimeter x (22x22 mm)" + layer[j],
						62, -403, 403, 15, 0, 330);
			}
		}
	}
	
	@Override
	public void process(EventHeader event) {
		// Get the SVT hits.
		List<SimTrackerHit> svtHits = getCollection(event, "TrackerHits", SimTrackerHit.class);
		
		// Plot the z-position of each SVT hit.
		for(SimTrackerHit svtHit : svtHits) {
			svtHitZPosition.fill(svtHit.getPosition()[2]);
			svtHitZPositionLayer.fill(svtHit.getPosition()[2], svtHit.getLayer());
		}
		
		// Iterate over particles that pass through the calorimeter
		// plane and map them to their calorimeter scoring plane hit
		// object. This is necessary to establish that a hodoscope
		// hit also crossed the calorimeter scoring plane.
		List<SimTrackerHit> ecalHits = getCollection(event, "TrackerHitsECal", SimTrackerHit.class);
		Map<MCParticle, SimTrackerHit> ecalHitMap = new HashMap<MCParticle, SimTrackerHit>();
		for(SimTrackerHit ecalHit : ecalHits) {
			// If a calorimeter hit associated with this particle is
			// already present, keep only the earliest of the two.
			if(ecalHitMap.containsKey(ecalHit.getMCParticle())) {
				SimTrackerHit oldHit = ecalHitMap.get(ecalHit.getMCParticle());
				if(oldHit.getTime() > ecalHit.getTime()) {
					ecalHitMap.put(ecalHit.getMCParticle(), ecalHit);
				}
			} else {
				ecalHitMap.put(ecalHit.getMCParticle(), ecalHit);
			}
		}
		
		// Iterate over the Monte Carlo particles and track how many
		// SVT layers they go through. Map the particle to this value.
		List<MCParticle> particles = getCollection(event, "MCParticle", MCParticle.class);
		Map<MCParticle, Integer> particleLayerCountMap = new HashMap<MCParticle, Integer>();
		for(MCParticle particle : particles) {
			int layers = 0;
			boolean[] traversedLayer = new boolean[6];
			for(SimTrackerHit svtHit : svtHits) {
				if(svtHit.getMCParticle() == particle) {
					traversedLayer[getSVTLayer(svtHit) - 1] = true;
				}
			}
			for(boolean verified : traversedLayer) {
				if(verified) { layers++; }
			}
			if(particle.getProductionTime() == 0) {
				if(particle.getPDGID() == 11) {
					svtLayersTraversed[ELECTRON].fill(layers);
				} else if(particle.getPDGID() == -11) {
					svtLayersTraversed[POSITRON].fill(layers);
				}
			}
			particleLayerCountMap.put(particle, layers);
		}
		
		// Iterate over the hodoscope scoring planes and plot values
		// for only those with at least five SVT layers traversed and
		// which also cross the calorimeter scoring plane.
		Set<MCParticle> plottedEcalParticles = new HashSet<MCParticle>();
		List<SimTrackerHit> hodoscopeHits = getCollection(event, "HodoscopeHits", SimTrackerHit.class);
		hodoscopeHitLoop:
		for(SimTrackerHit hodoscopeHit : hodoscopeHits) {
			hodoscopeHitZPosition.fill(hodoscopeHit.getPosition()[2]);
			
			// Only plot positrons or electrons.
			if(Math.abs(hodoscopeHit.getMCParticle().getPDGID()) != 11) {
				continue hodoscopeHitLoop;
			}
			int type = -1;
			if(hodoscopeHit.getMCParticle().getPDGID() > 0) { type = ELECTRON; }
			else  { type = POSITRON; }
			
			// Only plot particles that traverse at least 5 SVT layers.
			int layersTraversed = particleLayerCountMap.containsKey(hodoscopeHit.getMCParticle())
					? particleLayerCountMap.get(hodoscopeHit.getMCParticle()) : 0;
					
			if(layersTraversed < 5) {
				continue hodoscopeHitLoop;
			}
			
			// Only plot particles that also traverse the calorimeter
			// scoring plane. Additionally, hodoscope hits must occur
			// before the calorimeter hit. Otherwise, the hodoscope
			// hit is in fact a back-scattered particle and does not
			// represent valid data.
			SimTrackerHit ecalHit = ecalHitMap.get(hodoscopeHit.getMCParticle());
			
			if(ecalHit == null || ecalHit.getTime() < hodoscopeHit.getTime() || getMagnitude(ecalHit.getMomentum()) < 0.150) {
				continue hodoscopeHitLoop;
			}
			
			// Require that the particle fall within an allowed range
			// with respect to momentum.
			double p = getMagnitude(hodoscopeHit.getMomentum());
			if(p < 0.150 || p > 1.955) {
				continue hodoscopeHitLoop;
			}
			
			// Restrict particles to only those which were created at
			// simulation initialization.
			if(hodoscopeHit.getMCParticle().getProductionTime() != 0) {
				continue hodoscopeHitLoop;
			}
			
			// Populate the calorimeter plots, if this particle has
			// not already been plotted.
			if(!plottedEcalParticles.contains(hodoscopeHit.getMCParticle())) {
				plottedEcalParticles.add(hodoscopeHit.getMCParticle());
				
				double hitP = getMagnitude(ecalHit.getMomentum());
				ecalHitEnergy[type].fill(hitP);
				//ecalHitXPositionEnergy[type].fill(hitP, ecalHit.getPosition()[0]);
				ecalHitYPositionEnergy[type].fill(hitP, ecalHit.getPosition()[1]);
				ecalHitXPosition[type].fill(ecalHit.getPosition()[0]);
				ecalHitXYPosition[type].fill(ecalHit.getPosition()[0], ecalHit.getPosition()[1]);
			}
			
			// Populate the hodoscope plots for the appropriate layer.
			int layer = isDualPlane ? getHodoscopeDualplaneLayer(hodoscopeHit) : getHodoscopeMultiplaneLayer(hodoscopeHit);
			//System.out.printf("Type: %d (PID = %d);  Layer: %d (z = %f)%n",
			//		type, hodoscopeHit.getMCParticle().getPDGID(), layer, hodoscopeHit.getPosition()[2]);
			hodoscopeHitEnergy[type][layer].fill(p);
			//hodoscopeHitXPositionEnergy[type][layer].fill(p, ecalHit.getPosition()[0]);
			//hodoscopeHitYPositionEnergy[type][layer].fill(p, ecalHit.getPosition()[1]);
			hodoscopeHitΔx[type][layer].fill(ecalHit.getPosition()[0] - hodoscopeHit.getPosition()[0]);
			hodoscopeHitXEcalX[type][layer].fill(ecalHit.getPosition()[0], hodoscopeHit.getPosition()[0]);
			hodoscopeHitXPosition[type][layer].fill(hodoscopeHit.getPosition()[0]);
			hodoscopeHitXYPosition[type][layer].fill(hodoscopeHit.getPosition()[0], hodoscopeHit.getPosition()[1]);
			for(int i = 0; i < 4; i++) {
				hodoscopePixelHitXEcalX[type][layer][i].fill(ecalHit.getPosition()[0], hodoscopeHit.getPosition()[0]);
			}
		}
		
		
		
		/*
		// Investigate the origins of the weird hits.
		for(SimTrackerHit ecalHit : ecalHits) {
			if(ecalHit.getPosition()[0] >= 100 && ecalHit.getPosition()[0] <= 150) {
				// Only plot positrons.
				if(ecalHit.getMCParticle().getPDGID() != -11) {
					continue;
				}
				
				// Only plot particles that traverse at least 5 SVT layers.
				int layersTraversed = particleLayerCountMap.containsKey(ecalHit.getMCParticle())
						? particleLayerCountMap.get(ecalHit.getMCParticle()) : 0;
				if(layersTraversed < 5) {
					continue;
				}
				
				// Require that the particle fall within an allowed range
				// with respect to momentum.
				double p = getMagnitude(ecalHit.getMomentum());
				if(p < 0.150 || p > 1.955) {
					continue;
				}
				
				// Restrict particles to only those which were created at
				// simulation initialization.
				if(ecalHit.getMCParticle().getProductionTime() != 0) {
					continue;
				}
				
				// Get the corresponding hodoscope and SVT hits in
				// all existing planes for this particle.
				boolean hodoHitExists = false;
				SimTrackerHit[] weirdSVTHits = new SimTrackerHit[6];
				SimTrackerHit[] weirdHodoHits = new SimTrackerHit[6];
				for(SimTrackerHit hodoHit : hodoscopeHits) {
					// Look for a hodoscope hit with the same Monte
					// Carlo particle.
					if(hodoHit.getMCParticle() != ecalHit.getMCParticle()) {
						continue;
					}
					
					// Require that the particle have at least 150 MeV.
					if(getMagnitude(hodoHit.getMomentum()) < 0.150) {
						continue;
					}
					
					hodoHitExists = true;
					if(weirdHodoHits[getHodoscopeLayer(hodoHit)] == null
							|| weirdHodoHits[getHodoscopeLayer(hodoHit)].getTime() > hodoHit.getTime()) {
						weirdHodoHits[getHodoscopeLayer(hodoHit)] = hodoHit;
					}
				}
				for(SimTrackerHit svtHit : svtHits) {
					// Look for a hodoscope hit with the same Monte
					// Carlo particle.
					if(svtHit.getMCParticle() != ecalHit.getMCParticle()) {
						continue;
					}
					
					// Require that the particle have at least 150 MeV.
					if(getMagnitude(svtHit.getMomentum()) < 0.150) {
						continue;
					}
					
					if(weirdSVTHits[getSVTLayer(svtHit) - 1] == null
							|| weirdSVTHits[getSVTLayer(svtHit) - 1].getTime() > svtHit.getTime()) {
						weirdSVTHits[getSVTLayer(svtHit) - 1] = svtHit;
					}
				}
				
				// If at least one hodoscope hit exists, plot the
				// particle x-position for each of its hits.
				if(hodoHitExists) {
					weirdHitOrigins.fill(ecalHit.getPosition()[0], ecalHit.getPosition()[2]);
					weirdHitEnergy.fill(getMagnitude(ecalHit.getMomentum()), ecalHit.getPosition()[2]);
					weirdHitOrigins.fill(ecalHit.getMCParticle().getOriginX(), ecalHit.getMCParticle().getOriginZ());
					weirdHitEnergy.fill(ecalHit.getMCParticle().getMomentum().magnitude(), ecalHit.getMCParticle().getOriginZ());
					for(SimTrackerHit svtHit : weirdSVTHits) {
						if(svtHit != null) {
							weirdHitOrigins.fill(svtHit.getPosition()[0], svtHit.getPosition()[2]);
							weirdHitEnergy.fill(getMagnitude(svtHit.getMomentum()), svtHit.getPosition()[2]);
						}
					}
					for(SimTrackerHit hodoHit : weirdHodoHits) {
						if(hodoHit != null) {
							weirdHitOrigins.fill(hodoHit.getPosition()[0], hodoHit.getPosition()[2]);
							weirdHitEnergy.fill(getMagnitude(hodoHit.getMomentum()), hodoHit.getPosition()[2]);
						}
					}
				}
			}
		}
		*/
	}
	
	private static final int getHodoscopeDualplaneLayer(SimTrackerHit hodoscopeHit) {
		double z = hodoscopeHit.getPosition()[2];
		if(z >= 1089 && z < 1091) {
			return 0;
		} else if(z >= 1099 && z < 1101) {
			return 1;
		} else {
			return -1;
		}
	}
	
	private static final int getHodoscopeMultiplaneLayer(SimTrackerHit hodoscopeHit) {
		double z = hodoscopeHit.getPosition()[2];
		if(z >= 999 && z < 1001) {
			return 0;
		} else if(z >= 1049 && z < 1051) {
			return 1;
		} else if(z >= 1099 && z < 1101) {
			return 2;
		} else if(z >= 1149 && z < 1151) {
			return 3;
		} else if(z >= 1199 && z < 1201) {
			return 4;
		} else if(z >= 1249 && z < 1251) {
			return 5;
		} else {
			return -1;
		}
	}
	
	private static final int getSVTLayer(SimTrackerHit svtHit) {
		if(svtHit.getLayer() == 1 || svtHit.getLayer() == 2) {
			return 1;
		} else if(svtHit.getLayer() == 3 || svtHit.getLayer() == 4) {
			return 2;
		} else if(svtHit.getLayer() == 5 || svtHit.getLayer() == 6) {
			return 3;
		} else if(svtHit.getLayer() == 7 || svtHit.getLayer() == 8) {
			return 4;
		} else if(svtHit.getLayer() == 9 || svtHit.getLayer() == 10) {
			return 5;
		} else if(svtHit.getLayer() == 11 || svtHit.getLayer() == 12) {
			return 6;
		} else {
			return -1;
		}
	}
	
	private static final <E> List<E> getCollection(EventHeader event, String collectionName, Class<E> type) {
		List<E> collection;
		if(event.hasCollection(type, collectionName)) {
			collection = event.get(type, collectionName);
		} else {
			collection = new java.util.ArrayList<E>(0);
		}
		return collection;
	}
	
	private static final double getMagnitude(double[] v) {
		double squareSum = 0;
		for(double vi : v) {
			squareSum += Math.pow(vi, 2);
		}
		return Math.sqrt(squareSum);
	}
	
	public void setIsMultiPlane(boolean state) {
		isDualPlane = !state;
	}
}