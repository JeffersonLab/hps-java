/**
 * Template Driver
 */
/**
 * @author mrsolt
 *
 */
package org.hps.analysis.MC;

import java.util.List;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.tracking.gbl.GBLKinkData;

//Change "TemplateDriver" to your dirver name
public class TruthRefitVerticesPlots extends Driver {


    // Use JFreeChart as the default plotting backend
    /*static { 
        hep.aida.jfree.AnalysisFactory.register();
    }*/

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree; 
    IHistogramFactory histogramFactory; 
    
    //Sample histograms
    IHistogram1D TrackChisq_bad;
    IHistogram1D TrackChisq_truth;
    IHistogram1D TrackChisq_diff;
    IHistogram1D TrackBad_Z0;
    IHistogram1D TrackTruth_Z0;
    
    IHistogram1D TrackBad_PhiKink1;
    IHistogram1D TrackBad_PhiKink2;
    IHistogram1D TrackBad_PhiKink3;
    IHistogram1D TrackBad_PhiKink4;
    IHistogram1D TrackBad_PhiKink5;
    IHistogram1D TrackBad_PhiKink6;
    IHistogram1D TrackBad_LambdaKink1;
    IHistogram1D TrackBad_LambdaKink2;
    IHistogram1D TrackBad_LambdaKink3;
    IHistogram1D TrackBad_LambdaKink4;
    IHistogram1D TrackBad_LambdaKink5;
    IHistogram1D TrackBad_LambdaKink6;
    
    IHistogram1D TrackTruth_PhiKink1;
    IHistogram1D TrackTruth_PhiKink2;
    IHistogram1D TrackTruth_PhiKink3;
    IHistogram1D TrackTruth_PhiKink4;
    IHistogram1D TrackTruth_PhiKink5;
    IHistogram1D TrackTruth_PhiKink6;
    IHistogram1D TrackTruth_LambdaKink1;
    IHistogram1D TrackTruth_LambdaKink2;
    IHistogram1D TrackTruth_LambdaKink3;
    IHistogram1D TrackTruth_LambdaKink4;
    IHistogram1D TrackTruth_LambdaKink5;
    IHistogram1D TrackTruth_LambdaKink6;
    
    IHistogram1D TruthScatterYL1;
    IHistogram1D TruthScatterYL2;
    IHistogram1D TruthScatterYL3;
    
    IHistogram1D bscChisq_bad;
    IHistogram1D bscChisq_truth;
    IHistogram1D bscChisq_diff;
    IHistogram1D uncVZ_bad;
    IHistogram1D uncVZ_truth;
    IHistogram1D uncVZ_diff;
    
    IHistogram2D uncVZ_uncM_bad;
    IHistogram2D uncVZ_uncM_truth; 

    
    //Histogram Settings
    double minVZ = -50;
    double maxVZ = 80;
    double maxPhiKink = 0.01;
    double minPhiKink = -maxPhiKink;
    double maxLambdaKink = 0.01;
    double minLambdaKink = -maxLambdaKink;
    double scatterMax = 0.01;
    double scatterMin = -scatterMax;
    int nBins = 100;
    
    private List<HpsSiSensor> sensors = null;
    FieldMap bFieldMap = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    protected static Subdetector trackerSubdet;
    
    //Beam Energy
    double ebeam;
    
    public void detectorChanged(Detector detector){
        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
    
        //Set Beam Energy
        BeamEnergyCollection beamEnergyCollection = 
                this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        ebeam = beamEnergyCollection.get(0).getBeamEnergy();      
        
        bFieldMap = detector.getFieldMap();
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
        
        trackerSubdet = detector.getSubdetector(SUBDETECTOR_NAME);
        
        //Setup the Histograms
        TrackChisq_bad = aida.histogram1D("TrackChisq Bad", nBins, 0, 36);
        TrackChisq_truth = aida.histogram1D("TrackChisq Truth", nBins, 0, 36);
        TrackChisq_diff = aida.histogram1D("TrackChisq Difference", nBins, -36, 36);
        TrackBad_Z0 = aida.histogram1D("Track Z0 Bad", nBins, -3, 3);
        TrackTruth_Z0 = aida.histogram1D("Track Z0 Truth", nBins, -3, 3);
        
        TrackBad_PhiKink1 = aida.histogram1D("Track Bad Phi Kink1", nBins, minPhiKink, maxPhiKink);
        TrackBad_PhiKink2 = aida.histogram1D("Track Bad Phi Kink2", nBins, minPhiKink, maxPhiKink);
        TrackBad_PhiKink3 = aida.histogram1D("Track Bad Phi Kink3", nBins, minPhiKink, maxPhiKink);
        TrackBad_PhiKink4 = aida.histogram1D("Track Bad Phi Kink4", nBins, minPhiKink, maxPhiKink);
        TrackBad_PhiKink5 = aida.histogram1D("Track Bad Phi Kink5", nBins, minPhiKink, maxPhiKink);
        TrackBad_PhiKink6 = aida.histogram1D("Track Bad Phi Kink6", nBins, minPhiKink, maxPhiKink);
        TrackBad_LambdaKink1 = aida.histogram1D("Track Bad Lambda Kink1", nBins, minLambdaKink, maxLambdaKink);
        TrackBad_LambdaKink2 = aida.histogram1D("Track Bad Lambda Kink2", nBins, minLambdaKink, maxLambdaKink);
        TrackBad_LambdaKink3 = aida.histogram1D("Track Bad Lambda Kink3", nBins, minLambdaKink, maxLambdaKink);
        TrackBad_LambdaKink4 = aida.histogram1D("Track Bad Lambda Kink4", nBins, minLambdaKink, maxLambdaKink);
        TrackBad_LambdaKink5 = aida.histogram1D("Track Bad Lambda Kink5", nBins, minLambdaKink, maxLambdaKink);
        TrackBad_LambdaKink6 = aida.histogram1D("Track Bad Lambda Kink6", nBins, minLambdaKink, maxLambdaKink);
        
        TrackTruth_PhiKink1 = aida.histogram1D("Track Truth Phi Kink1", nBins, minPhiKink, maxPhiKink);
        TrackTruth_PhiKink2 = aida.histogram1D("Track Truth Phi Kink2", nBins, minPhiKink, maxPhiKink);
        TrackTruth_PhiKink3 = aida.histogram1D("Track Truth Phi Kink3", nBins, minPhiKink, maxPhiKink);
        TrackTruth_PhiKink4 = aida.histogram1D("Track Truth Phi Kink4", nBins, minPhiKink, maxPhiKink);
        TrackTruth_PhiKink5 = aida.histogram1D("Track Truth Phi Kink5", nBins, minPhiKink, maxPhiKink);
        TrackTruth_PhiKink6 = aida.histogram1D("Track Truth Phi Kink6", nBins, minPhiKink, maxPhiKink);
        TrackTruth_LambdaKink1 = aida.histogram1D("Track Truth Lambda Kink1", nBins, minLambdaKink, maxLambdaKink);
        TrackTruth_LambdaKink2 = aida.histogram1D("Track Truth Lambda Kink2", nBins, minLambdaKink, maxLambdaKink);
        TrackTruth_LambdaKink3 = aida.histogram1D("Track Truth Lambda Kink3", nBins, minLambdaKink, maxLambdaKink);
        TrackTruth_LambdaKink4 = aida.histogram1D("Track Truth Lambda Kink4", nBins, minLambdaKink, maxLambdaKink);
        TrackTruth_LambdaKink5 = aida.histogram1D("Track Truth Lambda Kink5", nBins, minLambdaKink, maxLambdaKink);
        TrackTruth_LambdaKink6 = aida.histogram1D("Track Truth Lambda Kink6", nBins, minLambdaKink, maxLambdaKink);
        
        TruthScatterYL1 = aida.histogram1D("Truth Scatter Y Away From Beam L1", nBins, scatterMin, scatterMax);
        TruthScatterYL2 = aida.histogram1D("Truth Scatter Y Away From Beam L2", nBins, scatterMin, scatterMax);
        TruthScatterYL3 = aida.histogram1D("Truth Scatter Y Away From Beam L3", nBins, scatterMin, scatterMax);
        
        bscChisq_bad = aida.histogram1D("BscChisq Bad", nBins, 0, 20);
        bscChisq_truth = aida.histogram1D("BscChisq Truth", nBins, 0, 20);
        bscChisq_diff = aida.histogram1D("BscChisq Difference", nBins, -10, 10);
        uncVZ_bad = aida.histogram1D("UncVZ Bad", nBins, minVZ, maxVZ);
        uncVZ_truth = aida.histogram1D("UncVZ Truth", nBins, minVZ, maxVZ);
        uncVZ_diff = aida.histogram1D("UncVZ Difference", nBins, -50, 50);
        uncVZ_uncM_bad = aida.histogram2D("uncVZ vs uncM Bad", nBins, 0, 0.1*ebeam, nBins, minVZ, maxVZ);
        uncVZ_uncM_truth = aida.histogram2D("uncVZ vs uncM Truth", nBins, 0, 0.1*ebeam, nBins, minVZ, maxVZ);

    }

    public void process(EventHeader event){
        aida.tree().cd("/");
        
        List<Track> tracksBad = event.get(Track.class,"GBLTracks_bad");
        List<Track> tracksTruth = event.get(Track.class,"GBLTracks_truth");
        List<ReconstructedParticle> uncBad = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_bad");
        List<ReconstructedParticle> uncBad2 = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_bad2");
        List<ReconstructedParticle> uncTruth = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_truth");
        List<ReconstructedParticle> uncTruth2 = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_truth2");
        List<ReconstructedParticle> bscBad = event.get(ReconstructedParticle.class,"BeamspotConstrainedV0Candidates_bad");
        List<ReconstructedParticle> bscBad2 = event.get(ReconstructedParticle.class,"BeamspotConstrainedV0Candidates_bad2");
        List<ReconstructedParticle> bscTruth = event.get(ReconstructedParticle.class,"BeamspotConstrainedV0Candidates_truth");
        List<ReconstructedParticle> bscTruth2 = event.get(ReconstructedParticle.class,"BeamspotConstrainedV0Candidates_truth2");
        
        if(!tracksBad.isEmpty() && !tracksTruth.isEmpty()){
            double tracksBad_chisq = tracksBad.get(0).getChi2();
            double tracksTruth_chisq = tracksTruth.get(0).getChi2();
            GenericObject kinksBad = getKinkData(event, tracksBad.get(0),"GBLKinkDataRelations");
            GenericObject kinksTruth = getKinkData(event, tracksTruth.get(0),"GBLKinkDataRelations_truth");
        
            TrackChisq_bad.fill(tracksBad_chisq);
            TrackChisq_truth.fill(tracksTruth_chisq);
            TrackChisq_diff.fill(tracksBad_chisq - tracksTruth_chisq);
            TrackBad_Z0.fill(tracksBad.get(0).getTrackStates().get(0).getZ0());
            TrackTruth_Z0.fill(tracksTruth.get(0).getTrackStates().get(0).getZ0());
            
            if(kinksBad != null){
                TrackBad_PhiKink1.fill(GBLKinkData.getPhiKink(kinksBad, 0));
                TrackBad_PhiKink2.fill(GBLKinkData.getPhiKink(kinksBad, 1));
                TrackBad_PhiKink3.fill(GBLKinkData.getPhiKink(kinksBad, 2));
                TrackBad_PhiKink4.fill(GBLKinkData.getPhiKink(kinksBad, 3));
                TrackBad_PhiKink5.fill(GBLKinkData.getPhiKink(kinksBad, 4));
                TrackBad_PhiKink6.fill(GBLKinkData.getPhiKink(kinksBad, 5));
                TrackBad_LambdaKink1.fill(GBLKinkData.getLambdaKink(kinksBad, 0));
                TrackBad_LambdaKink2.fill(GBLKinkData.getLambdaKink(kinksBad, 1));
                TrackBad_LambdaKink3.fill(GBLKinkData.getLambdaKink(kinksBad, 2));
                TrackBad_LambdaKink4.fill(GBLKinkData.getLambdaKink(kinksBad, 3));
                TrackBad_LambdaKink5.fill(GBLKinkData.getLambdaKink(kinksBad, 4));
                TrackBad_LambdaKink6.fill(GBLKinkData.getLambdaKink(kinksBad, 5));
            }
            
            if(kinksTruth != null){
                TrackTruth_PhiKink1.fill(GBLKinkData.getPhiKink(kinksTruth, 0));
                TrackTruth_PhiKink2.fill(GBLKinkData.getPhiKink(kinksTruth, 1));
                TrackTruth_PhiKink3.fill(GBLKinkData.getPhiKink(kinksTruth, 2));
                TrackTruth_PhiKink4.fill(GBLKinkData.getPhiKink(kinksTruth, 3));
                TrackTruth_PhiKink5.fill(GBLKinkData.getPhiKink(kinksTruth, 4));
                TrackTruth_PhiKink6.fill(GBLKinkData.getPhiKink(kinksTruth, 5));
                TrackTruth_LambdaKink1.fill(GBLKinkData.getLambdaKink(kinksTruth, 0));
                TrackTruth_LambdaKink2.fill(GBLKinkData.getLambdaKink(kinksTruth, 1));
                TrackTruth_LambdaKink3.fill(GBLKinkData.getLambdaKink(kinksTruth, 2));
                TrackTruth_LambdaKink4.fill(GBLKinkData.getLambdaKink(kinksTruth, 3));
                TrackTruth_LambdaKink5.fill(GBLKinkData.getLambdaKink(kinksTruth, 4));
                TrackTruth_LambdaKink6.fill(GBLKinkData.getLambdaKink(kinksTruth, 5));
            }
            
            MCFullDetectorTruth truthMatch = new MCFullDetectorTruth(event, tracksBad.get(0), bFieldMap, sensors, trackerSubdet);
            if(truthMatch != null){
                double L1 = -9999;
                double L2 = -9999;
                double L3 = -9999;
                double L4 = -9999;
                double L5 = -9999;
                double L6 = -9999;
                if(truthMatch.getActiveHitScatter(1) != null){
                    L1 = truthMatch.getActiveHitScatter(1)[1];
                    if(tracksBad.get(0).getTrackStates().get(0).getTanLambda() < 0){
                        L1 = -L1;
                    }
                }
                if(truthMatch.getActiveHitScatter(2) != null){
                    L2 = truthMatch.getActiveHitScatter(2)[1];
                    if(tracksBad.get(0).getTrackStates().get(0).getTanLambda() < 0){
                        L2 = -L2;
                    }
                }
                if(truthMatch.getActiveHitScatter(3) != null){
                    L3 = truthMatch.getActiveHitScatter(3)[1];
                    if(tracksBad.get(0).getTrackStates().get(0).getTanLambda() < 0){
                        L3 = -L3;
                    }
                }
                if(truthMatch.getActiveHitScatter(4) != null){
                    L4 = truthMatch.getActiveHitScatter(4)[1];
                    if(tracksBad.get(0).getTrackStates().get(0).getTanLambda() < 0){
                        L4 = -L4;
                    }
                }
                if(truthMatch.getActiveHitScatter(5) != null){
                    L5 = truthMatch.getActiveHitScatter(5)[1];
                    if(tracksBad.get(0).getTrackStates().get(0).getTanLambda() < 0){
                        L5 = -L5;
                    }
                }
                if(truthMatch.getActiveHitScatter(6) != null){
                    L6 = truthMatch.getActiveHitScatter(6)[1];
                    if(tracksBad.get(0).getTrackStates().get(0).getTanLambda() < 0){
                        L6 = -L6;
                    }
                }
                TruthScatterYL1.fill(L1+L2);
                TruthScatterYL2.fill(L3+L4);
                TruthScatterYL3.fill(L5+L6);
            }
        }
        
        if(!bscBad.isEmpty() && !bscTruth.isEmpty()){
            double BscChisq_bad = bscBad.get(0).getStartVertex().getChi2();
            double BscChisq_truth = bscTruth.get(0).getStartVertex().getChi2();
        
            bscChisq_bad.fill(BscChisq_bad);
            bscChisq_truth.fill(BscChisq_truth);
            bscChisq_diff.fill(BscChisq_bad - BscChisq_truth);
        }
        
        if(!uncBad.isEmpty() && !uncTruth.isEmpty()){
            double UncVZ_bad = uncBad.get(0).getStartVertex().getPosition().z();
            double UncVZ_truth = uncTruth.get(0).getStartVertex().getPosition().z();
            double UncM_bad = uncBad.get(0).getMass();
            double UncM_truth = uncTruth.get(0).getMass();
        
            uncVZ_bad.fill(UncVZ_bad);
            uncVZ_truth.fill(UncVZ_truth);
            uncVZ_diff.fill(UncVZ_bad - UncVZ_truth);
        
            uncVZ_uncM_bad.fill(UncM_bad,UncVZ_bad);
            uncVZ_uncM_truth.fill(UncM_truth,UncVZ_truth);
        }
    }
    
    public static RelationalTable getKinkDataToTrackTable(EventHeader event, String DataRelationConnection) {
        RelationalTable kinkDataToTrack = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY,
                RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, DataRelationConnection)) {
            List<LCRelation> relations = event.get(LCRelation.class, DataRelationConnection);
            for (LCRelation relation : relations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    kinkDataToTrack.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        return kinkDataToTrack;
    }

    public static GenericObject getKinkData(EventHeader event, Track track, String DataRelationConnection) {
        return (GenericObject) getKinkDataToTrackTable(event,DataRelationConnection).from(track);
    }
}