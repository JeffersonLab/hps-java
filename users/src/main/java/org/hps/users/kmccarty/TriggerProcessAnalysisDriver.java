package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.analysis.trigger.SimTriggerData;
import org.hps.analysis.trigger.data.TriggerDiagStats;
import org.hps.analysis.trigger.util.OutputLogger;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TIData;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TriggerProcessAnalysisDriver extends Driver {
	private int eventsProcessed = 0;
	private int møllersProcessed = 0;
	private boolean checkSVT = false;
	private int tridentsProcessed = 0;
	private int gblMøllersProcessed = 0;
	private int gblTridentsProcessed = 0;
	private int vertexedMøllersProcessed = 0;
	private int vertexedTridentsProcessed = 0;
	private double timeCoincidence = 2.5;
	private double elasticThreshold = 0.900;
	private double møllerLowerRange = 0.800;
	private double møllerUpperRange = 1.300;
	private AIDA aida = AIDA.defaultInstance();
	private boolean checkTriggerTimeWindow = false;
	private String bankCollectionName = "TriggerBank";
	private String clusterCollectionName = "EcalClustersCorr";
	private String particleCollectionName = "FinalStateParticles";
	private String møllerCollectionName = "UnconstrainedMollerCandidates";
	private String tridentCollectionName = "TargetConstrainedV0Candidates";
	
	// Track how many events pass each trigger configuration.
	private int triggerActiveEvents = 0;
	private int[][][][][] ctmTriggerMøllers = new int[2][2][2][2][2];
	private int[][][][][] vtxTriggerMøllers = new int[2][2][2][2][2];
	private int[][][][][] ctmTriggerTridents = new int[2][2][2][2][2];
	private int[][][][][] vtxTriggerTridents = new int[2][2][2][2][2];
	
	// Define Møller cut constants.
	/*
	private static final int LOW = 0;
	private static final int HIGH = 1;
	private static final double[] MØLLER_ANGLE_THRESHOLD = {
			(1 - 0.40) * 0.5109989 / (2 * 1056),
			(1 + 0.40) * 0.5109989 / (2 * 1056)
	};
	private static final double[] MØLLER_ENERGY_THRESHOLD = {
			(1 - 0.15) * 1.056,
			(1 + 0.15) * 1.056
	};
	private static final double BEAM_ROTATION = -0.0305;
	private static final double ELECTRON_MASS_2 = 0.0005109989 * 0.0005109989;
	*/
	
	// Define trident cluster-track matched condition plots.
	private IHistogram1D trctmInvariantMass = aida.histogram1D("Tridents CTMatched/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D trctmInstancesInEvent = aida.histogram1D("Tridents CTMatched/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D trctmEnergySum1D = aida.histogram1D("Tridents CTMatched/Cluster Energy Sum", 150, 0.000, 1.500);
	private IHistogram1D trctmMomentumSum1D = aida.histogram1D("Tridents CTMatched/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D trctmElectronEnergy = aida.histogram1D("Tridents CTMatched/Electron Cluster Energy", 150, 0.000, 1.500);
	private IHistogram1D trctmElectronMomentum = aida.histogram1D("Tridents CTMatched/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trctmPositronEnergy = aida.histogram1D("Tridents CTMatched/Positron Cluster Energy", 150, 0.000, 1.500);
	private IHistogram1D trctmPositronMomentum = aida.histogram1D("Tridents CTMatched/Positron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trctmTimeCoincidence = aida.histogram1D("Tridents CTMatched/Time Coincidence", 100, -4, 4);
	private IHistogram2D trctmClusterPosition = aida.histogram2D("Tridents CTMatched/Cluster Seed Position", 46, -23, 23, 11, -5.5, 5.5);
	private IHistogram2D trctmEnergySum2D = aida.histogram2D("Tridents CTMatched/Cluster Energy Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D trctmTrackPosition = aida.histogram2D("Tridents CTMatched/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
	private IHistogram2D trctmMomentumSum2D = aida.histogram2D("Tridents CTMatched/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D trctmESumCoplanarity = aida.histogram2D("Tridents CTMatched/Cluster Energy Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	private IHistogram2D trctmPSumCoplanarity = aida.histogram2D("Tridents CTMatched/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	// Define trident vertexed condition plots.
	private IHistogram1D trvtxInvariantMass = aida.histogram1D("Tridents Vertexed/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D trvtxInstancesInEvent = aida.histogram1D("Tridents Vertexed/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D trvtxEnergySum1D = aida.histogram1D("Tridents Vertexed/Cluster Energy Sum", 150, 0.000, 1.500);
	private IHistogram1D trvtxMomentumSum1D = aida.histogram1D("Tridents Vertexed/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D trvtxElectronEnergy = aida.histogram1D("Tridents Vertexed/Electron Cluster Energy", 150, 0.000, 1.500);
	private IHistogram1D trvtxElectronMomentum = aida.histogram1D("Tridents Vertexed/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trvtxPositronEnergy = aida.histogram1D("Tridents Vertexed/Positron Cluster Energy", 150, 0.000, 1.500);
	private IHistogram1D trvtxPositronMomentum = aida.histogram1D("Tridents Vertexed/Positron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trvtxTimeCoincidence = aida.histogram1D("Tridents Vertexed/Time Coincidence", 100, -4, 4);
	private IHistogram2D trvtxClusterPosition = aida.histogram2D("Tridents Vertexed/Cluster Seed Position", 46, -23, 23, 11, -5.5, 5.5);
	private IHistogram2D trvtxEnergySum2D = aida.histogram2D("Tridents Vertexed/Cluster Energy Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D trvtxTrackPosition = aida.histogram2D("Tridents Vertexed/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
	private IHistogram2D trvtxMomentumSum2D = aida.histogram2D("Tridents Vertexed/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D trvtxESumCoplanarity = aida.histogram2D("Tridents Vertexed/Cluster Energy Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	private IHistogram2D trvtxPSumCoplanarity = aida.histogram2D("Tridents Vertexed/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	private IHistogram1D trvtxChiSquared = aida.histogram1D("Tridents Vertexed/Chi Squared", 1000, 0.0, 1000.0);
	
	// Define the Møller cluster-track matched condition plots.
	private IHistogram1D møctmInvariantMass = aida.histogram1D("Møller CTMatched/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D møctmInstancesInEvent = aida.histogram1D("Møller CTMatched/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D møctmEnergySum1D = aida.histogram1D("Møller CTMatched/Cluster Energy Sum", 150, 0.000, 1.500);
	private IHistogram1D møctmMomentumSum1D = aida.histogram1D("Møller CTMatched/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D møctmElectronEnergy = aida.histogram1D("Møller CTMatched/Electron Cluster Energy", 150, 0.000, 1.500);
	private IHistogram1D møctmElectronMomentum = aida.histogram1D("Møller CTMatched/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D møctmTimeCoincidence = aida.histogram1D("Møller CTMatched/Time Coincidence", 100, -4, 4);
	private IHistogram2D møctmClusterPosition = aida.histogram2D("Møller CTMatched/Cluster Seed Position", 46, -23, 23, 11, -5.5, 5.5);
	private IHistogram2D møctmEnergySum2D = aida.histogram2D("Møller CTMatched/Cluster Energy Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D møctmTrackPosition = aida.histogram2D("Møller CTMatched/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
	private IHistogram2D møctmMomentumSum2D = aida.histogram2D("Møller CTMatched/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D møctmESumCoplanarity = aida.histogram2D("Møller CTMatched/Cluster Energy Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	private IHistogram2D møctmPSumCoplanarity = aida.histogram2D("Møller CTMatched/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	// Define the Møller track-only condition plots.
	private IHistogram1D møgblTimeCoincidence = aida.histogram1D("Møller Track-Only/Time Coincidence", 100, -4, 4);
	private IHistogram1D møgblInvariantMass = aida.histogram1D("Møller Track-Only/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D møgblInstancesInEvent = aida.histogram1D("Møller Track-Only/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D møgblMomentumSum1D = aida.histogram1D("Møller Track-Only/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D møgblElectronMomentum = aida.histogram1D("Møller Track-Only/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram2D møgblTrackPosition = aida.histogram2D("Møller Track-Only/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
	private IHistogram2D møgblMomentumSum2D = aida.histogram2D("Møller Track-Only/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D møgblPSumCoplanarity = aida.histogram2D("Møller Track-Only/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	// Define the Møller vertexed condition plots.
	private IHistogram1D møvtxTimeCoincidence = aida.histogram1D("Møller Vertexed/Time Coincidence", 100, -4, 4);
	private IHistogram1D møvtxInvariantMass = aida.histogram1D("Møller Vertexed/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D møvtxInstancesInEvent = aida.histogram1D("Møller Vertexed/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D møvtxMomentumSum1D = aida.histogram1D("Møller Vertexed/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D møvtxElectronMomentum = aida.histogram1D("Møller Vertexed/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram2D møvtxTrackPosition = aida.histogram2D("Møller Vertexed/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
	private IHistogram2D møvtxMomentumSum2D = aida.histogram2D("Møller Vertexed/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D møvtxPSumCoplanarity = aida.histogram2D("Møller Vertexed/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	// Define the GBL trident condition plots.
	private IHistogram1D trgblInvariantMass = aida.histogram1D("Tridents Track-Only/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D trgblInstancesInEvent = aida.histogram1D("Tridents Track-Only/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D trgblMomentumSum1D = aida.histogram1D("Tridents Track-Only/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D trgblElectronMomentum = aida.histogram1D("Tridents Track-Only/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trgblPositronMomentum = aida.histogram1D("Tridents Track-Only/Positron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trgblTimeCoincidence = aida.histogram1D("Tridents Track-Only/Time Coincidence", 100, -4, 4);
	private IHistogram2D trgblTrackPosition = aida.histogram2D("Tridents Track-Only/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
	private IHistogram2D trgblMomentumSum2D = aida.histogram2D("Tridents Track-Only/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D trgblPSumCoplanarity = aida.histogram2D("Tridents Track-Only/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	@Override
	public void endOfData() {
		// Calculate the scaling factor for Hertz.
		double scale = 19000.0 / eventsProcessed;
		
		System.out.println("Processed " + eventsProcessed + " events.");
		System.out.println("Processed " + møllersProcessed + " Møller events");
		System.out.println("\tAcceptance :: " + (100.0 * møllersProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (møllersProcessed * scale) + " Hz");
		
		System.out.println("Processed " + tridentsProcessed + " trident events");
		System.out.println("\tAcceptance :: " + (100.0 * tridentsProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (tridentsProcessed * scale) + " Hz");
		
		/*
		System.out.println("Processed " + gblMøllersProcessed + " track-only Møller events");
		System.out.println("\tAcceptance :: " + (100.0 * gblMøllersProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (gblMøllersProcessed * scale) + " Hz");
		
		System.out.println("Processed " + gblTridentsProcessed + " track-only trident events");
		System.out.println("\tAcceptance :: " + (100.0 * gblTridentsProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (gblTridentsProcessed * scale) + " Hz");
		*/
		
		System.out.println("Processed " + vertexedMøllersProcessed + " vertexed Møller events");
		System.out.println("\tAcceptance :: " + (100.0 * vertexedMøllersProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (vertexedMøllersProcessed * scale) + " Hz");
		
		System.out.println("Processed " + vertexedTridentsProcessed + " vertexed trident events");
		System.out.println("\tAcceptance :: " + (100.0 * vertexedTridentsProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (vertexedTridentsProcessed * scale) + " Hz");
		
		
		System.out.println("CTM Møllers:");
		for(int pulser = 0; pulser < 2; pulser++) {
			for(int singles0 = 0; singles0 < 2; singles0++) {
				for(int singles1 = 0; singles1 < 2; singles1++) {
					for(int pair0 = 0; pair0 < 2; pair0++) {
						for(int pair1 = 0; pair1 < 2; pair1++) {
							System.out.printf("\t%5b  %5b  %5b  %5b  %5b  %d%n",
									pulser == 1 ? true : false, singles0 == 1 ? true : false,
									singles1 == 1 ? true : false, pair0 == 1 ? true : false,
									pair1 == 1 ? true : false, ctmTriggerMøllers[pulser][singles0][singles1][pair0][pair1]);
						}
					}
				}
			}
		}
		
		System.out.println("CTM Tridents:");
		for(int pulser = 0; pulser < 2; pulser++) {
			for(int singles0 = 0; singles0 < 2; singles0++) {
				for(int singles1 = 0; singles1 < 2; singles1++) {
					for(int pair0 = 0; pair0 < 2; pair0++) {
						for(int pair1 = 0; pair1 < 2; pair1++) {
							System.out.printf("\t%5b  %5b  %5b  %5b  %5b  %d%n",
									pulser == 1 ? true : false, singles0 == 1 ? true : false,
									singles1 == 1 ? true : false, pair0 == 1 ? true : false,
									pair1 == 1 ? true : false, ctmTriggerTridents[pulser][singles0][singles1][pair0][pair1]);
						}
					}
				}
			}
		}
		
		System.out.println("VTX Møllers:");
		for(int pulser = 0; pulser < 2; pulser++) {
			for(int singles0 = 0; singles0 < 2; singles0++) {
				for(int singles1 = 0; singles1 < 2; singles1++) {
					for(int pair0 = 0; pair0 < 2; pair0++) {
						for(int pair1 = 0; pair1 < 2; pair1++) {
							System.out.printf("\t%5b  %5b  %5b  %5b  %5b  %d%n",
									pulser == 1 ? true : false, singles0 == 1 ? true : false,
									singles1 == 1 ? true : false, pair0 == 1 ? true : false,
									pair1 == 1 ? true : false, vtxTriggerMøllers[pulser][singles0][singles1][pair0][pair1]);
						}
					}
				}
			}
		}
		
		System.out.println("VTX Tridents:");
		for(int pulser = 0; pulser < 2; pulser++) {
			for(int singles0 = 0; singles0 < 2; singles0++) {
				for(int singles1 = 0; singles1 < 2; singles1++) {
					for(int pair0 = 0; pair0 < 2; pair0++) {
						for(int pair1 = 0; pair1 < 2; pair1++) {
							System.out.printf("\t%5b  %5b  %5b  %5b  %5b  %d%n",
									pulser == 1 ? true : false, singles0 == 1 ? true : false,
									singles1 == 1 ? true : false, pair0 == 1 ? true : false,
									pair1 == 1 ? true : false, vtxTriggerTridents[pulser][singles0][singles1][pair0][pair1]);
						}
					}
				}
			}
		}
	}
	
	/**
	@Override
	public void process(EventHeader event) {
		// Check whether the SVT was active in this event and, if so,
		// skip it. This can be disabled through the steering file for
		// Monte Carlo data, where the "SVT" is always active.
		if(checkSVT) {
			final String[] flagNames = { "svt_bias_good", "svt_burstmode_noise_good", "svt_position_good" };
			boolean svtGood = true;
	        for(int i = 0; i < flagNames.length; i++) {
	            int[] flag = event.getIntegerParameters().get(flagNames[i]);
	            if(flag == null || flag[0] == 0) {
	                svtGood = false;
	            }
	        }
	        if(!svtGood) { return; }
		}
        
        System.out.println("Processed " + eventsProcessed + " events.");
        System.out.println("Processed " + mollersProcessed + " Moller events");
        System.out.println("\tAcceptance :: " + (100.0 * mollersProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (mollersProcessed * scale) + " Hz");
        
        System.out.println("Processed " + tridentsProcessed + " trident events");
        System.out.println("\tAcceptance :: " + (100.0 * tridentsProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (tridentsProcessed * scale) + " Hz");
        
        System.out.println("Processed " + gblMollersProcessed + " track-only Moller events");
        System.out.println("\tAcceptance :: " + (100.0 * gblMollersProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (gblMollersProcessed * scale) + " Hz");
        
        System.out.println("Processed " + gblTridentsProcessed + " Rafo trident events");
        System.out.println("\tAcceptance :: " + (100.0 * gblTridentsProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (gblTridentsProcessed * scale) + " Hz");
    }**/
    
/*
    @Override
    public void process(EventHeader event) {
        // Check whether the SVT was active in this event and, if so,
        // skip it. This can be disabled through the steering file for
        // Monte Carlo data, where the "SVT" is always active.
        if(checkSVT) {
            final String[] flagNames = { "svt_bias_good", "svt_burstmode_noise_good", "svt_position_good" };
            boolean svtGood = true;
            for(int i = 0; i < flagNames.length; i++) {
                int[] flag = event.getIntegerParameters().get(flagNames[i]);
                if(flag == null || flag[0] == 0) {
                    svtGood = false;
                }
            }
            if(!svtGood) { return; }
        }
        
        System.out.println("Processed " + eventsProcessed + " events.");
        System.out.println("Processed " + mollersProcessed + " Moller events");
        System.out.println("\tAcceptance :: " + (100.0 * mollersProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (mollersProcessed * scale) + " Hz");
        
        System.out.println("Processed " + tridentsProcessed + " trident events");
        System.out.println("\tAcceptance :: " + (100.0 * tridentsProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (tridentsProcessed * scale) + " Hz");
        
        System.out.println("Processed " + gblMollersProcessed + " track-only Moller events");
        System.out.println("\tAcceptance :: " + (100.0 * gblMollersProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (gblMollersProcessed * scale) + " Hz");
        
        System.out.println("Processed " + gblTridentsProcessed + " Rafo trident events");
        System.out.println("\tAcceptance :: " + (100.0 * gblTridentsProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (gblTridentsProcessed * scale) + " Hz");
    }
    */
    
    @Override
    public void process(EventHeader event) {
        // Check whether the SVT was active in this event and, if so,
        // skip it. This can be disabled through the steering file for
        // Monte Carlo data, where the "SVT" is always active.
        if(checkSVT) {
            final String[] flagNames = { "svt_bias_good", "svt_burstmode_noise_good", "svt_position_good" };
            boolean svtGood = true;
            for(int i = 0; i < flagNames.length; i++) {
                int[] flag = event.getIntegerParameters().get(flagNames[i]);
                if(flag == null || flag[0] == 0) {
                    svtGood = false;
                }
            }
            if(!svtGood) { return; }
        }
        
        // Track the number of events with good SVT.
        eventsProcessed++;
        
        // Check if the event has a collection of tracks. If it exists,
        // extract it. Otherwise, skip the event.
		if(!event.hasCollection(ReconstructedParticle.class, particleCollectionName)) {
			return;
		}
		List<ReconstructedParticle> trackList = event.get(ReconstructedParticle.class, particleCollectionName);
		
		// Check if the event has a collection of clusters. If it
		// exists, extract it. Otherwise, skip the event.
		if(!event.hasCollection(Cluster.class, clusterCollectionName)) {
			return;
		}
		List<Cluster> clusterList = event.get(Cluster.class, clusterCollectionName);
		
		// Check if a trident candidate collection exists, and obtain
		// it if so. Continue with the event if it does not.
		List<ReconstructedParticle> tridentCandidates = null;
		if(event.hasCollection(ReconstructedParticle.class, tridentCollectionName)) {
			tridentCandidates = event.get(ReconstructedParticle.class, tridentCollectionName);
		} else {
			tridentCandidates = new ArrayList<ReconstructedParticle>(0);
		}
		
		// Check if a Møller candidate collection exists, and obtain
		// it if so. Continue with the event if it does not.
		List<ReconstructedParticle> møllerCandidates = null;
		if(event.hasCollection(ReconstructedParticle.class, møllerCollectionName)) {
			møllerCandidates = event.get(ReconstructedParticle.class, møllerCollectionName);
		} else {
			møllerCandidates = new ArrayList<ReconstructedParticle>(0);
		}
		
		// Get cluster-track matched top/bottom pairs.
		List<ReconstructedParticle[]> gblMatchedPairs = getTopBottomTracksGBL(trackList);
		List<ReconstructedParticle[]> ctMatchedPairs  = getTopBottomTracksCTMatched(trackList);
		
		// Get the trident and Møller tracks for the matched track
		// and cluster pair condition sets.
		List<ReconstructedParticle[]> møllers     = getMøllerTracksCTMatched(ctMatchedPairs);
		List<ReconstructedParticle[]> møllersGBL  = getMøllerTracksGBL(gblMatchedPairs, event);
		List<ReconstructedParticle[]> tridents    = getTridentTracksCTMatched(ctMatchedPairs);
		List<ReconstructedParticle[]> tridentsVTX = getTridentTracksVertexed(tridentCandidates);
		List<ReconstructedParticle[]> møllersVTX  = getMøllerTracksVertexed(møllerCandidates);
		List<ReconstructedParticle[]> tridentsGBL = getTridentClustersGBL(gblMatchedPairs, TriggerModule.getTopBottomPairs(clusterList, Cluster.class), event);
		
		// Track how many events had tridents and Møllers.
		if(!møllers.isEmpty()) { møllersProcessed++; }
		if(!tridents.isEmpty()) { tridentsProcessed++; }
		if(!møllersGBL.isEmpty()) { gblMøllersProcessed++; }
		if(!tridentsGBL.isEmpty()) { gblTridentsProcessed++; }
		if(!møllersVTX.isEmpty()) { vertexedMøllersProcessed++; }
		if(!tridentsVTX.isEmpty()) { vertexedTridentsProcessed++; }
		
		// Get the SSP clusters.
		if(event.hasCollection(GenericObject.class, bankCollectionName)) {
			// Get the bank list.
			List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
			
			// Check for simulated triggers. If they exist, populate the
			// trigger tracking variables.
			for(GenericObject obj : bankList) {
				if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
					TIData tiBank = new TIData(obj);
					
					// Establish which triggers were active.
					boolean isPulser   = tiBank.isPulserTrigger();
					boolean isPair0    = tiBank.isPair0Trigger();
					boolean isPair1    = tiBank.isPair1Trigger();
					boolean isSingles0 = tiBank.isSingle0Trigger();
					boolean isSingles1 = tiBank.isSingle1Trigger();
					
					// Populate the appropriate trigger tracking variables.
					triggerActiveEvents++;
					if(!møllers.isEmpty()) {
						ctmTriggerMøllers[isPulser ? 1 : 0][isSingles0 ? 1 : 0][isSingles1 ? 1 : 0][isPair0 ? 1 : 0][isPair1 ? 1 : 0]++;
					} if(!tridents.isEmpty()) {
						ctmTriggerTridents[isPulser ? 1 : 0][isSingles0 ? 1 : 0][isSingles1 ? 1 : 0][isPair0 ? 1 : 0][isPair1 ? 1 : 0]++;
					} if(!møllersVTX.isEmpty()) {
						vtxTriggerMøllers[isPulser ? 1 : 0][isSingles0 ? 1 : 0][isSingles1 ? 1 : 0][isPair0 ? 1 : 0][isPair1 ? 1 : 0]++;
					} if(!tridentsVTX.isEmpty()) {
						vtxTriggerTridents[isPulser ? 1 : 0][isSingles0 ? 1 : 0][isSingles1 ? 1 : 0][isPair0 ? 1 : 0][isPair1 ? 1 : 0]++;
					}
				}
			}
		}
		
		// Produce Møller cluster-track matched plots.
		møctmInstancesInEvent.fill(møllers.size());
		for(ReconstructedParticle[] pair : møllers) {
			// Get the track clusters.
			Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			
			// Populate the cluster plots.
			møctmElectronEnergy.fill(trackClusters[0].getEnergy());
			møctmElectronEnergy.fill(trackClusters[1].getEnergy());
			møctmEnergySum1D.fill(TriggerModule.getValueEnergySum(trackClusters));
			møctmEnergySum2D.fill(trackClusters[0].getEnergy(), trackClusters[1].getEnergy());
			møctmESumCoplanarity.fill(TriggerModule.getValueEnergySum(trackClusters), getCalculatedCoplanarity(trackClusters));
			møctmTimeCoincidence.fill(TriggerModule.getClusterTime(trackClusters[0]) - TriggerModule.getClusterTime(trackClusters[1]));
			møctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[0]), TriggerModule.getClusterYIndex(trackClusters[0]));
			møctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[1]), TriggerModule.getClusterYIndex(trackClusters[1]));
			
			// Populate the momentum plots.
			møctmInvariantMass.fill(getInvariantMass(pair));
			møctmElectronMomentum.fill(pair[0].getMomentum().magnitude());
			møctmElectronMomentum.fill(pair[1].getMomentum().magnitude());
			møctmMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			møctmMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			møctmTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
			møctmTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
			møctmPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
		
		// Produce trident cluster-track matched plots.
		trctmInstancesInEvent.fill(tridents.size());
		for(ReconstructedParticle[] pair : tridents) {
			// Get the electron and positron tracks.
			ReconstructedParticle electronTrack = pair[pair[0].getCharge() < 0 ? 0 : 1];
			ReconstructedParticle positronTrack = pair[pair[0].getCharge() > 0 ? 0 : 1];
			
			// Get the track clusters.
			Cluster electronCluster = electronTrack.getClusters().get(0);
			Cluster positronCluster = positronTrack.getClusters().get(0);
			Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			
			// Populate the cluster plots.
			trctmElectronEnergy.fill(electronCluster.getEnergy());
			trctmPositronEnergy.fill(positronCluster.getEnergy());
			trctmEnergySum2D.fill(pair[0].getEnergy(), pair[1].getEnergy());
			trctmEnergySum1D.fill(TriggerModule.getValueEnergySum(trackClusters));
			trctmESumCoplanarity.fill(TriggerModule.getValueEnergySum(trackClusters), getCalculatedCoplanarity(trackClusters));
			trctmTimeCoincidence.fill(TriggerModule.getClusterTime(trackClusters[0]) - TriggerModule.getClusterTime(trackClusters[1]));
			trctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[0]), TriggerModule.getClusterYIndex(trackClusters[0]));
			trctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[1]), TriggerModule.getClusterYIndex(trackClusters[1]));
			
			// Populate the momentum plots.
			trctmInvariantMass.fill(getInvariantMass(pair));
			trctmElectronMomentum.fill(electronTrack.getMomentum().magnitude());
			trctmPositronMomentum.fill(positronTrack.getMomentum().magnitude());
			trctmMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			trctmMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			trctmTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
			trctmTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
			trctmPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
		
		// Produce trident vertexed plots.
		trvtxInstancesInEvent.fill(tridentsVTX.size());
		for(ReconstructedParticle[] pair : tridentsVTX) {
			// Get the electron and positron tracks.
			ReconstructedParticle electronTrack = pair[pair[0].getCharge() < 0 ? 0 : 1];
			ReconstructedParticle positronTrack = pair[pair[0].getCharge() > 0 ? 0 : 1];
			
			// Get the track clusters.
			Cluster electronCluster = electronTrack.getClusters().get(0);
			Cluster positronCluster = positronTrack.getClusters().get(0);
			Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			
			// Populate the cluster plots.
			trvtxElectronEnergy.fill(electronCluster.getEnergy());
			trvtxPositronEnergy.fill(positronCluster.getEnergy());
			trvtxEnergySum2D.fill(pair[0].getEnergy(), pair[1].getEnergy());
			trvtxEnergySum1D.fill(TriggerModule.getValueEnergySum(trackClusters));
			trvtxESumCoplanarity.fill(TriggerModule.getValueEnergySum(trackClusters), getCalculatedCoplanarity(trackClusters));
			trvtxTimeCoincidence.fill(TriggerModule.getClusterTime(trackClusters[0]) - TriggerModule.getClusterTime(trackClusters[1]));
			trvtxClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[0]), TriggerModule.getClusterYIndex(trackClusters[0]));
			trvtxClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[1]), TriggerModule.getClusterYIndex(trackClusters[1]));
			
			// Populate the momentum plots.
			trvtxInvariantMass.fill(getInvariantMass(pair));
			trvtxElectronMomentum.fill(electronTrack.getMomentum().magnitude());
			trvtxPositronMomentum.fill(positronTrack.getMomentum().magnitude());
			trvtxMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			trvtxMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			trvtxTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
			trvtxTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
			trvtxPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
		
		// Produce the Møller vertexed plots.
		møvtxInstancesInEvent.fill(møllersVTX.size());
		RelationalTable<?, ?> hitToStrips = TrackUtils.getHitToStripsTable(event);
		RelationalTable<?, ?> hitToRotated = TrackUtils.getHitToRotatedTable(event);
		for(ReconstructedParticle pair[] : møllersVTX) {
			// Get the tracks and track times.
			Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
			double times[] = {
					TrackUtils.getTrackTime(tracks[0], hitToStrips, hitToRotated),
					TrackUtils.getTrackTime(tracks[1], hitToStrips, hitToRotated)	
			};
			
			// Fill the plots.
			møvtxTimeCoincidence.fill(times[0] - times[1]);
			møvtxInvariantMass.fill(getInvariantMass(pair));
			møvtxElectronMomentum.fill(pair[0].getMomentum().magnitude());
			møvtxElectronMomentum.fill(pair[1].getMomentum().magnitude());
			møvtxMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			møvtxMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			møvtxTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
			møvtxTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
			møvtxPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
		
		// Produce the Møller track-only plots.
		møgblInstancesInEvent.fill(møllersGBL.size());
		for(ReconstructedParticle pair[] : møllersGBL) {
			// Get the tracks and track times.
			Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
			double times[] = {
					TrackUtils.getTrackTime(tracks[0], hitToStrips, hitToRotated),
					TrackUtils.getTrackTime(tracks[1], hitToStrips, hitToRotated)	
			};
			
			// Fill the plots.
			møgblTimeCoincidence.fill(times[0] - times[1]);
			møgblInvariantMass.fill(getInvariantMass(pair));
			møgblElectronMomentum.fill(pair[0].getMomentum().magnitude());
			møgblElectronMomentum.fill(pair[1].getMomentum().magnitude());
			møgblMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			møgblMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			møgblTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
			møgblTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
			møgblPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
		
		// Produce track-only trident plots.
		trgblInstancesInEvent.fill(tridentsGBL.size());
		for(ReconstructedParticle[] pair : tridentsGBL) {
			// Get the tracks and track times.
			Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
			double times[] = {
					TrackUtils.getTrackTime(tracks[0], hitToStrips, hitToRotated),
					TrackUtils.getTrackTime(tracks[1], hitToStrips, hitToRotated)	
			};
			
			// Get the positron and the electron.
			ReconstructedParticle positron = pair[0].getCharge() > 0 ? pair[0] : pair[1];
			ReconstructedParticle electron = pair[0].getCharge() < 0 ? pair[0] : pair[1];
			
			// Fill the plots.
			trgblTimeCoincidence.fill(times[0] - times[1]);
			trgblInvariantMass.fill(getInvariantMass(pair));
			trgblElectronMomentum.fill(electron.getMomentum().magnitude());
			trgblPositronMomentum.fill(positron.getMomentum().magnitude());
			trgblMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			trgblMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			trgblTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
			trgblTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
			trgblPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
	}
	
	public void setCheckSVT(boolean state) {
		checkSVT = state;
	}
	
	public void setCheckTriggerTimeWindow(boolean state) {
		checkTriggerTimeWindow = state;
	}
	
	/**
	 * Gets a list of all possible GBL top/bottom track pairs. These
	 * tracks are not guaranteed to have a matched cluster.
	 * @param trackList - A list of all possible tracks.
	 * @return Returns a list of track pairs.
	 */
	private static final List<ReconstructedParticle[]> getTopBottomTracksGBL(List<ReconstructedParticle> trackList) {
		// Separate the tracks into top and bottom tracks based on
		// the value of tan(Λ). Use only GBL tracks to avoid track
		// duplication.
		List<ReconstructedParticle> topTracks = new ArrayList<ReconstructedParticle>();
		List<ReconstructedParticle> botTracks = new ArrayList<ReconstructedParticle>();
		trackLoop:
		for(ReconstructedParticle track : trackList) {
			// Require that the ReconstructedParticle contain an actual
			// Track object.
			if(track.getTracks().isEmpty()) {
				continue trackLoop;
			}
			
			// Ignore tracks that are not GBL tracks.
			if(!TrackType.isGBL(track.getType())) {
				continue trackLoop;
			}
			
			// If the above tests pass, the ReconstructedParticle has
			// a track and is also a GBL track. Separate it into either
			// a top or a bottom track based on its tan(Λ) value.
			if(track.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0) {
				topTracks.add(track);
			} else {
				botTracks.add(track);
			}
		}
		
		// Form all top/bottom pairs with the unique tracks.
		List<ReconstructedParticle[]> pairList = new ArrayList<ReconstructedParticle[]>();
		for(ReconstructedParticle topTrack : topTracks) {
			for(ReconstructedParticle botTrack : botTracks) {
				pairList.add(new ReconstructedParticle[] { topTrack, botTrack });
			}
		}
		
		// Return the result.
		return pairList;
	}
	
	/**
	 * Produces pairs of tracks. The track pairs are required to be
	 * matched to a cluster and the associated clusters must form a
	 * top/bottom pair. If more than one track points to the same
	 * cluster, only the first track is retained.
	 * @param trackList - A list of all tracks.
	 * @return Returns a list of track pairs meeting the aforementioned
	 * conditions.
	 */
	private static final List<ReconstructedParticle[]> getTopBottomTracksCTMatched(List<ReconstructedParticle> trackList) {
		// Track clusters that have already been seen to prevent clusters
		// that have duplicate tracks from reappearing.
		Set<Cluster> clusterSet = new HashSet<Cluster>();
		
		// Separate the tracks into top and bottom tracks based on
		// the track cluster. Filter out tracks with no clusters.
		List<ReconstructedParticle> topTracks = new ArrayList<ReconstructedParticle>();
		List<ReconstructedParticle> botTracks = new ArrayList<ReconstructedParticle>();
		trackLoop:
		for(ReconstructedParticle track : trackList) {
			// Check if the track has a cluster. If not, skip it.
			if(track.getClusters().isEmpty()) {
				continue trackLoop;
			}
			
			// If the track doesn't have actual tracks, skip it.
			if(track.getTracks().isEmpty()) {
				continue trackLoop;
			}
			
			// Check if the track cluster has already seen.
			Cluster trackCluster = track.getClusters().get(0);
			if(clusterSet.contains(trackCluster)) {
				continue trackLoop;
			}
			
			// If the track has a unique cluster, add it to the proper
			// list based on the cluster y-index.
			clusterSet.add(trackCluster);
			if(TriggerModule.getClusterYIndex(trackCluster) > 0) {
				topTracks.add(track);
			} else {
				botTracks.add(track);
			}
		}
		
		// Form all top/bottom pairs with the unique tracks.
		List<ReconstructedParticle[]> pairList = new ArrayList<ReconstructedParticle[]>();
		for(ReconstructedParticle topTrack : topTracks) {
			for(ReconstructedParticle botTrack : botTracks) {
				pairList.add(new ReconstructedParticle[] { topTrack, botTrack });
			}
		}
		
		// Return the result.
		return pairList;
	}
	
	private final List<ReconstructedParticle[]> getTridentTracksVertexed(List<ReconstructedParticle> candidateList) {
		// Store the set of track pairs that meet the trident condition.
		List<ReconstructedParticle[]> tridentTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Loop over the filtered pair list and apply the trident
		// condition test.
		tridentLoop:
		for(ReconstructedParticle candidate : candidateList) {
			// Require that the vertex fit have a X^2 value of less
			// than 25.
			trvtxChiSquared.fill(candidate.getStartVertex().getChi2());
			if(candidate.getStartVertex().getChi2() > 10) {
				continue tridentLoop;
			}
			
			// Make sure that each particle is track/cluster matched.
			ReconstructedParticle[] pair = {
					candidate.getParticles().get(0),
					candidate.getParticles().get(1)
			};
			if(pair[0].getTracks().isEmpty() || pair[0].getClusters().isEmpty()) {
				continue tridentLoop;
			}
			if(pair[1].getTracks().isEmpty() || pair[1].getClusters().isEmpty()) {
				continue tridentLoop;
			}
			
			// Make sure there is a top/bottom cluster pair.
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			boolean hasTop = TriggerModule.getClusterYIndex(trackClusters[0]) > 0 || TriggerModule.getClusterYIndex(trackClusters[1]) > 0;
			boolean hasBot = TriggerModule.getClusterYIndex(trackClusters[0]) < 0 || TriggerModule.getClusterYIndex(trackClusters[1]) < 0;
			if(!hasTop || !hasBot) {
				continue tridentLoop;
			}
			
			// There must be one positive and one negative track.
			ReconstructedParticle electron = null;
			ReconstructedParticle positron = null;
			if(pair[0].getCharge() > 0) { positron = pair[0]; }
			else if(pair[1].getCharge() > 0) { positron = pair[1]; }
			if(pair[0].getCharge() < 0) { electron = pair[0]; }
			else if(pair[1].getCharge() < 0) { electron = pair[1]; }
			if(electron == null || positron == null) {
				continue tridentLoop;
			}
			
			// Make sure that the clusters are not the same. This should
			// not actually ever be possible...
			if(pair[0].getClusters().get(0) == pair[1].getClusters().get(0)) {
				continue tridentLoop;
			}
			
			// The clusters must within a limited time window.
			if(TriggerModule.getValueTimeCoincidence(trackClusters) > timeCoincidence) {
				continue tridentLoop;
			}
			
			// Require that the electron in the pair have an energy
			// below the elastic threshold to exclude elastic electrons.
			if(electron.getMomentum().magnitude() >= elasticThreshold) {
				continue tridentLoop;
			}
			
			// Require that all clusters occur within the trigger time
			// window to exclude accidentals.
			if(checkTriggerTimeWindow) {
				if(!inTriggerWindow(trackClusters[0]) || !inTriggerWindow(trackClusters[1])) {
					continue tridentLoop;
				}
			}
			
			// If all the above conditions are met, the pair is to be
			// considered a trident pair. Add it to the list.
			tridentTracks.add(pair);
		}
		
		// Return the list of pairs that passed the condition.
		return tridentTracks;
	}
	
	private final List<ReconstructedParticle[]> getTridentClustersGBL(List<ReconstructedParticle[]> pairList, List<Cluster[]> clusterList, EventHeader event) {
		// Store the set of track pairs that meet the trident condition.
		List<ReconstructedParticle[]> tridentTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Extract track relational tables from the event object.
		RelationalTable<?, ?> hitToStrips = TrackUtils.getHitToStripsTable(event);
		RelationalTable<?, ?> hitToRotated = TrackUtils.getHitToRotatedTable(event);
		
		// Tracks will not be considered for trident analysis unless there
		// is at least one top/bottom cluster pair within the time window.
		boolean passesClusterCondition = false;
		tridentClusterLoop:
		for(Cluster[] pair : clusterList) {
			// Ignore clusters that are too far apart temporally.
			if(TriggerModule.getValueTimeCoincidence(pair) > timeCoincidence) {
				continue tridentClusterLoop;
			}
			
			// Require that the cluster pair be top/bottom.
			boolean hasTop = TriggerModule.getClusterYIndex(pair[0]) > 0 || TriggerModule.getClusterYIndex(pair[1]) > 0;
			boolean hasBot = TriggerModule.getClusterYIndex(pair[0]) < 0 || TriggerModule.getClusterYIndex(pair[1]) < 0;
			if(!hasTop || !hasBot) {
				continue tridentClusterLoop;
			}
			
			// If the cluster passes, mark that it has done so and skip
			// the rest. Only one pair need pass.
			passesClusterCondition = true;
			break tridentClusterLoop;
		}
		
		// If no cluster pair passed the cluster condition, no tracks
		// are allowed to pass either.
		if(!passesClusterCondition) {
			return tridentTracks;
		}
		
		// Next, check the track pair list. A track pair must have a
		// positive and a negative track and must also be within the
		// time coincidence window.
		tridentTrackLoop:
		for(ReconstructedParticle[] pair : pairList) {
			// Check that there is at least one positive and one negative
			// track in the pair.
			boolean hasPositive = pair[0].getCharge() > 0 || pair[1].getCharge() > 0;
			boolean hasNegative = pair[0].getCharge() < 0 || pair[1].getCharge() < 0;
			if(!hasPositive || !hasNegative) {
				break tridentTrackLoop;
			}
			
			// Check that the track pair passes the time cut.
			double times[] = {
				TrackUtils.getTrackTime(pair[0].getTracks().get(0), hitToStrips, hitToRotated),
				TrackUtils.getTrackTime(pair[1].getTracks().get(0), hitToStrips, hitToRotated)	
			};
			
			if(Math.abs(times[0] - times[1]) > timeCoincidence) {
				continue tridentTrackLoop;
			}
			
			// Require that the negative track have less than the
			// elastic threshold momentum to exclude elastic electrons.
			if(pair[0].getCharge() < 0 && pair[0].getMomentum().magnitude() > elasticThreshold
					|| pair[1].getCharge() < 0 && pair[1].getMomentum().magnitude() > elasticThreshold) {
				continue tridentTrackLoop;
			}
			
			// If the track passes both, it is considered a trident pair.
			tridentTracks.add(pair);
		}
		
		// Return the resultant pairs.
		return tridentTracks;
	}
	
	/**
	 * Gets a list track pairs that meet the trident condition defined
	 * using tracks with matched calorimeter clusters. A pair meets the
	 * cluster/track matched trident condition is it meets the following:
	 * <ul><li>Both tracks have matched clusters.</li>
	 * <li>Has one positive track.</li>
	 * <li>Has one negative track.</li>
	 * <li>Clusters have a time coincidence of 2.5 ns or less.</li>
	 * <li>The electron momentum is below 900 MeV.</li></ul>
	 * @param pairList - A <code>List</code> collection of parameterized
	 * type <code>ReconstructedParticle[]</code> containing all valid
	 * top/bottom pairs of tracks with matched clusters. These will be
	 * tested to see if they meet the process criteria.
	 * @return Returns a list containing pairs of tracks that meet the
	 * trident condition.
	 */
	private final List<ReconstructedParticle[]> getTridentTracksCTMatched(List<ReconstructedParticle[]> pairList) {
		// Store the set of track pairs that meet the trident condition.
		List<ReconstructedParticle[]> tridentTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Loop over the filtered pair list and apply the trident
		// condition test.
		tridentLoop:
		for(ReconstructedParticle[] pair : pairList) {
			// There must be one positive and one negative track.
			ReconstructedParticle electron = null;
			ReconstructedParticle positron = null;
			if(pair[0].getCharge() > 0) { positron = pair[0]; }
			else if(pair[1].getCharge() > 0) { positron = pair[1]; }
			if(pair[0].getCharge() < 0) { electron = pair[0]; }
			else if(pair[1].getCharge() < 0) { electron = pair[1]; }
			if(electron == null || positron == null) {
				continue tridentLoop;
			}
			
			// Make sure that the clusters are not the same. This should
			// not actually ever be possible...
			if(pair[0].getClusters().get(0) == pair[1].getClusters().get(0)) {
				continue tridentLoop;
			}
			
			// The clusters must within a limited time window.
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			if(TriggerModule.getValueTimeCoincidence(trackClusters) > timeCoincidence) {
				continue tridentLoop;
			}
			
			// Require that the electron in the pair have an energy
			// below the elastic threshold to exclude elastic electrons.
			if(electron.getMomentum().magnitude() >= elasticThreshold) {
				continue tridentLoop;
			}
			
			// Require that all clusters occur within the trigger time
			// window to exclude accidentals.
			if(checkTriggerTimeWindow) {
				if(!inTriggerWindow(trackClusters[0]) || !inTriggerWindow(trackClusters[1])) {
					continue tridentLoop;
				}
			}
			
			// If all the above conditions are met, the pair is to be
			// considered a trident pair. Add it to the list.
			tridentTracks.add(pair);
		}
		
		// Return the list of pairs that passed the condition.
		return tridentTracks;
	}
	
	private final List<ReconstructedParticle[]> getMøllerTracksGBL(List<ReconstructedParticle[]> pairList, EventHeader event) {
		// Store the set of track pairs that meet the Møller condition.
		List<ReconstructedParticle[]> møllerTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Extract track relational tables from the event object.
		RelationalTable<?, ?> hitToStrips = TrackUtils.getHitToStripsTable(event);
		RelationalTable<?, ?> hitToRotated = TrackUtils.getHitToRotatedTable(event);
		
		// Loop over the filtered pair list and apply the Møller
		// condition test.
		møllerLoop:
		for(ReconstructedParticle[] pair : pairList) {
			// Both tracks must be negatively charged.
			if(pair[0].getCharge() > 0 || pair[1].getCharge() > 0) {
				continue møllerLoop;
			}
			
			// The clusters must within a limited time window.
			double times[] = {
				TrackUtils.getTrackTime(pair[0].getTracks().get(0), hitToStrips, hitToRotated),
				TrackUtils.getTrackTime(pair[1].getTracks().get(0), hitToStrips, hitToRotated)	
			};
			
			if(Math.abs(times[0] - times[1]) > timeCoincidence) {
				continue møllerLoop;
			}
			
			// Require that the electrons in the pair have energies
			// below the elastic threshold to exclude said electrons.
			if(pair[0].getMomentum().magnitude() > elasticThreshold || pair[1].getMomentum().magnitude() > elasticThreshold) {
				continue møllerLoop;
			}
			
			// Require that the energy of the pair be within a range
			// that is sufficiently "Møller-like."
			double momentumSum = VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude();
			if(momentumSum < møllerLowerRange || momentumSum > møllerUpperRange) {
				continue møllerLoop;
			}
			
			// If all the above conditions are met, the pair is to be
			// considered a trident pair. Add it to the list.
			møllerTracks.add(pair);
		}
		
		// Return the list of pairs that passed the condition.
		return møllerTracks;
	}
	
	private final List<ReconstructedParticle[]> getMøllerTracksVertexed(List<ReconstructedParticle> candidateList) {
		// Store the set of track pairs that meet the Møller condition.
		List<ReconstructedParticle[]> møllerTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Loop over the candidate list and apply the Møller cuts.
		møllerLoop:
		for(ReconstructedParticle candidate : candidateList) {
			// Require that the vertex fit have a X^2 value of less
			// than 25.
			if(candidate.getStartVertex().getChi2() > 10) {
				continue møllerLoop;
			}
			
			// Make sure that each particle is track/cluster matched.
			ReconstructedParticle[] pair = {
					candidate.getParticles().get(0),
					candidate.getParticles().get(1)
			};
			if(pair[0].getTracks().isEmpty() || pair[0].getClusters().isEmpty()) {
				continue møllerLoop;
			}
			if(pair[1].getTracks().isEmpty() || pair[1].getClusters().isEmpty()) {
				continue møllerLoop;
			}
			
			// Make sure there is a top/bottom cluster pair.
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			boolean hasTop = TriggerModule.getClusterYIndex(trackClusters[0]) > 0 || TriggerModule.getClusterYIndex(trackClusters[1]) > 0;
			boolean hasBot = TriggerModule.getClusterYIndex(trackClusters[0]) < 0 || TriggerModule.getClusterYIndex(trackClusters[1]) < 0;
			if(!hasTop || !hasBot) {
				continue møllerLoop;
			}
			
			// Require that the clusters be within the time coincidence.
			if(TriggerModule.getValueTimeCoincidence(trackClusters) > timeCoincidence) {
				continue møllerLoop;
			}
			
			// Require that both tracks have less than the elastic
			// threshold in momentum.
			if(pair[0].getMomentum().magnitude() > elasticThreshold || pair[1].getMomentum().magnitude() > elasticThreshold) {
				continue møllerLoop;
			}
			
			
			
			
			// Variable definitions
			double[] mom1 = pair[0].getMomentum().v();
			double px1 = mom1[0];
			double py1 = mom1[1];
			double pz1 = mom1[2];
			
			double unrot_px1 = px1*Math.cos(-0.0305) + pz1*Math.sin(-0.0305);
			double unrot_pz1 = pz1*Math.cos(-0.0305) - px1*Math.sin(-0.0305);
			
			double unrot_theta1 = Math.atan2(Math.sqrt(unrot_px1*unrot_px1 + py1*py1),unrot_pz1);
			double TrackE1 = Math.sqrt(Math.sqrt(px1*px1 + py1*py1 + pz1*pz1)*Math.sqrt(px1*px1 + py1*py1 + pz1*pz1) + 0.0005109989*0.0005109989);
			
			
			double[] mom2 = pair[1].getMomentum().v();
			double px2 = mom2[0];
			double py2 = mom2[1];
			double pz2 = mom2[2];
			
			double unrot_px2 = px2*Math.cos(-0.0305) + pz2*Math.sin(-0.0305);
			double unrot_pz2 = pz2*Math.cos(-0.0305) - px2*Math.sin(-0.0305);
			
			double unrot_theta2 = Math.atan2(Math.sqrt(unrot_px2*unrot_px2 + py2*py2),unrot_pz2);
			double TrackE2 = Math.sqrt(Math.sqrt(px2*px2 + py2*py2 + pz2*pz2)*Math.sqrt(px2*px2 + py2*py2 + pz2*pz2) + 0.0005109989*0.0005109989);
			
			
			// Ultimate Moller Cut
			if((Math.sin(unrot_theta1/2)*Math.sin(unrot_theta2/2)<=(1+0.20)*0.5109989/(2*1056) && Math.sin(unrot_theta1/2)*Math.sin(unrot_theta2/2)>=(1-0.20)*0.5109989/(2*1056)) ) {
				if( (TrackE1+TrackE2<=(1+0.10)*1.056 && TrackE1+TrackE2>=(1-0.10)*1.056) ) {
					
					
					
					
					møllerTracks.add(pair);
				}
			}
			
			
			
			
			/*
			double[] trackE = new double[2];
			double[] unrotTheta = new double[2];
			double[][] unrotP = new double[2][3];
			for(int i = 0; i < pair.length; i++) {
				unrotP[i][0] = pair[i].getMomentum().x() * Math.cos(BEAM_ROTATION) + pair[i].getMomentum().z() * Math.sin(BEAM_ROTATION);
				unrotP[i][2] = pair[i].getMomentum().z() * Math.cos(BEAM_ROTATION) - pair[i].getMomentum().x() * Math.sin(BEAM_ROTATION);
				unrotTheta[i] = Math.atan2(Math.sqrt(unrotP[i][0] * unrotP[i][0] + pair[0].getMomentum().y() * pair[i].getMomentum().y()), unrotP[0][2]);
				trackE[i] = Math.sqrt(pair[i].getMomentum().magnitudeSquared() + ELECTRON_MASS_2);
			}
			
			// Perform the track angle threshold cuts.
			double unrotatedTheta = Math.sin(unrotTheta[0] / 2) * Math.sin(unrotTheta[1] / 2);
			if((unrotatedTheta <= MØLLER_ANGLE_THRESHOLD[HIGH] && unrotatedTheta >= MØLLER_ANGLE_THRESHOLD[LOW])) {
				// Perform the track energy sum threshold cuts.
				double trackESum = trackE[0] + trackE[1];
				if((trackESum <= MØLLER_ENERGY_THRESHOLD[HIGH] && trackESum >= MØLLER_ENERGY_THRESHOLD[LOW])) {
					// If a pair passes all the Møller cuts, then it
					// is a Møller pair.
					møllerTracks.add(pair);
				}
			}
			*/
		}
		
		// Return the Møller list.
		return møllerTracks;
	}
	
	/**
	 * Gets a list track pairs that meet the Møller condition defined
	 * using tracks with matched calorimeter clusters. A pair meets the
	 * cluster/track matched Møller condition is it meets the following:
	 * <ul><li>Both tracks have matched clusters.</li>
	 * <li>Both tracks are negative.</li>
	 * <li>Clusters have a time coincidence of 2.5 ns or less.</li>
	 * <li>The electron momenta are below 900 MeV.</li>
	 * <li>The momentum sum of the tracks is in the range <code>800 MeV
	 * ≤ p1 + p2 ≤ 1500 MeV</li></ul>
	 * @param pairList - A <code>List</code> collection of parameterized
	 * type <code>ReconstructedParticle[]</code> containing all valid
	 * top/bottom pairs of tracks with matched clusters. These will be
	 * tested to see if they meet the process criteria.
	 * @return Returns a list containing pairs of tracks that meet the
	 * Møller condition.
	 */
	private final List<ReconstructedParticle[]> getMøllerTracksCTMatched(List<ReconstructedParticle[]> pairList) {
		// Store the set of track pairs that meet the Møller condition.
		List<ReconstructedParticle[]> møllerTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Loop over the filtered pair list and apply the Møller
		// condition test.
		møllerLoop:
		for(ReconstructedParticle[] pair : pairList) {
			// Both tracks must be negatively charged.
			if(pair[0].getCharge() > 0 || pair[1].getCharge() > 0) {
				continue møllerLoop;
			}
			
			// The clusters must within a limited time window.
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			if(TriggerModule.getValueTimeCoincidence(trackClusters) > timeCoincidence) {
				continue møllerLoop;
			}
			
			// Require that the electrons in the pair have energies
			// below the elastic threshold to exclude said electrons.
			if(pair[0].getMomentum().magnitude() > elasticThreshold || pair[1].getMomentum().magnitude() > elasticThreshold) {
				continue møllerLoop;
			}
			
			// Require that the energy of the pair be within a range
			// that is sufficiently "Møller-like."
			double momentumSum = VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude();
			if(momentumSum < møllerLowerRange || momentumSum > møllerUpperRange) {
				continue møllerLoop;
			}
			
			// Require that all clusters occur within the trigger time
			// window to exclude accidentals.
			if(checkTriggerTimeWindow) {
				if(!inTriggerWindow(trackClusters[0]) || !inTriggerWindow(trackClusters[1])) {
					continue møllerLoop;
				}
			}
			
			// If all the above conditions are met, the pair is to be
			// considered a trident pair. Add it to the list.
			møllerTracks.add(pair);
		}
		
		// Return the list of pairs that passed the condition.
		return møllerTracks;
	}
	
	/**
	 * Calculates the approximate invariant mass for a pair of tracks
	 * from their momentum. This assumes that the particles are either
	 * electrons or positrons, and thusly have a sufficiently small
	 * mass term that it can be safely excluded.
	 * @param pair - The track pair for which to calculate the invariant
	 * mass.
	 * @return Returns the approximate invariant mass in units of GeV.
	 */
	private static final double getInvariantMass(ReconstructedParticle[] pair) {
		// Get the momentum squared.
		double p2 = Math.pow(pair[0].getMomentum().magnitude() + pair[1].getMomentum().magnitude(), 2);
		
		// Get the remaining terms.
		double xPro = pair[0].getMomentum().x() + pair[1].getMomentum().x();
		double yPro = pair[0].getMomentum().y() + pair[1].getMomentum().y();
		double zPro = pair[0].getMomentum().z() + pair[1].getMomentum().z();
		
		// Calculate the invariant mass.
		return Math.sqrt(p2 - Math.pow(xPro, 2) - Math.pow(yPro, 2) - Math.pow(zPro, 2));
	}
	
	/**
	 * Calculates the coplanarity angle between two points, specified
	 * by a double array. The array must be of the format (x, y, z).
	 * @param position - The first position array.
	 * @param otherPosition - The second position array.
	 * @return Returns the coplanarity angle between the points in units
	 * of degrees.
	 */
	private static final double getCalculatedCoplanarity(double[] position, double[] otherPosition) {
		// Define the x- and y-coordinates of the clusters as well as
		// calorimeter center.
		final double ORIGIN_X = 42.52;
		double x[] = { position[0], otherPosition[0] };
		double y[] = { position[1], otherPosition[1] };
		
        // Get the cluster angles.
        double[] clusterAngle = new double[2];
        for(int i = 0; i < 2; i++) {
            clusterAngle[i] = Math.atan2(y[i], x[i] - ORIGIN_X) * 180 / Math.PI;
            if(clusterAngle[i] <= 0) { clusterAngle[i] += 360; }
        }
        
        // Calculate the coplanarity cut value.
        double clusterDiff = clusterAngle[0] - clusterAngle[1];
        return clusterDiff > 0 ? clusterDiff : clusterDiff + 360;
    }
    
    /**
     * Calculates the coplanarity angle of a pair of clusters.
     * @param pair - The pair of clusters for which to calculate the
     * coplanarity angle.
     * @return Returns the coplanarity angle between the two clusters
     * in degrees.
     */
    private static final double getCalculatedCoplanarity(Cluster[] pair) {
        return getCalculatedCoplanarity(pair[0].getPosition(), pair[1].getPosition());
    }
    
    /**
     * Calculates the coplanarity angle of a pair of tracks. The track
     * is extrapolated to the calorimeter face and its position there
     * used for the arguments in the calculation.
     * @param pair - The pair of tracks for which to calculate the
     * coplanarity angle.
     * @return Returns the coplanarity angle between the two tracks
     * in degrees.
     */
    private static final double getCalculatedCoplanarity(Track[] pair) {
        return getCalculatedCoplanarity(TrackUtils.getTrackPositionAtEcal(pair[0]).v(), TrackUtils.getTrackPositionAtEcal(pair[1]).v());
    }
    
    private static final boolean inTriggerWindow(Cluster cluster) {
        // Get the cluster time.
        double clusterTime = TriggerModule.getClusterTime(cluster);
        
        // Check that it is within the allowed bounds.
        return (35 <= clusterTime && clusterTime <= 50);
    }
    
    private static final boolean isCoincidental(Cluster[] pair) {
        // Get the energy sum and the time coincidence.
        double energySum = pair[0].getEnergy() + pair[1].getEnergy();
        double timeCoincidence = TriggerModule.getValueTimeCoincidence(pair);
        
        // Get the upper and lower bounds of the allowed range.
        double mean = getTimeDependenceMean(energySum);
        double threeSigma = 3.0 * getTimeDependenceSigma(energySum);
        double lowerBound = mean - threeSigma;
        double upperBound = mean + threeSigma;
        
        // Perform the time coincidence check.
        return (lowerBound <= timeCoincidence && timeCoincidence <= upperBound);
    }
    
    private static final double getTimeDependenceMean(double energySum) {
        // Define the fit parameters.
        double[] param = { 0.289337, -2.81998, 9.03475, -12.93, 8.71476, -2.26969 };
        
        // Calculate the mean.
        return param[0] + energySum * (param[1] + energySum * (param[2] + energySum * (param[3] + energySum * (param[4] + energySum * (param[5])))));
    }
    
    private static final double getTimeDependenceSigma(double energySum) {
        // Define the fit parameters.
        double[] param = { 4.3987, -24.2371, 68.9567, -98.2586, 67.562, -17.8987 };
        
        // Calculate the standard deviation.
        return param[0] + energySum * (param[1] + energySum * (param[2] + energySum * (param[3] + energySum * (param[4] + energySum * (param[5])))));
    }
}