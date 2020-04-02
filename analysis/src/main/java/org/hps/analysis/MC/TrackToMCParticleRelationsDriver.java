package org.hps.analysis.MC;


import hep.physics.vec.Hep3Vector;
//import hep.physics.vec.BasicHep3Vector;
import hep.physics.matrix.SymmetricMatrix;

import java.util.List;
import java.util.ArrayList;

import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRelationalTable;


import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;

//import org.lcsim.event.TrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.geometry.Detector;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOConstants;


/**
 * @author PF
 * This driver creates an MCParticle relation to be persisted for each track collection
 * It also saves a TruthTrack
 */


public class TrackToMCParticleRelationsDriver extends Driver {
    
    //Collection Names
    private String trackCollectionName = "GBLTracks";
    
    //If the tracks are kalman tracks
    private boolean kalmanTracks     = true;

    private double bfield;
    private double bfield_y;

    private boolean debug = false;
    private boolean saveTruthTracks = true;
    
    
    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setKalmanTracks(boolean val) {
        kalmanTracks = val;
    }

    public void setDebug(boolean val) {
        debug = val;
    }
    
    public void setSaveTruthTracks(boolean val) {
        saveTruthTracks = val;
    }
    
    /*
      public void setTrackCollectionNames(String [] trackCollectionNames) {
      this.trackCollectionNames = trackCollectionNames;
      }
    */
    
    @Override
    protected void detectorChanged(Detector detector) {
        Hep3Vector fieldInTracker = TrackUtils.getBField(detector);
        bfield = Math.abs(fieldInTracker.y());
        bfield_y = fieldInTracker.y();
    }
    
    @Override 
    protected void process(EventHeader event) {
        
        //Retrieve track collection
        List<Track> trackCollection = new ArrayList<Track>();
        if (trackCollectionName != null) {
            if (event.hasCollection(Track.class, trackCollectionName)) {
                trackCollection = event.get(Track.class, trackCollectionName);
            }
        }
        
        //Retrieve rawhits to mc
        
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
        }
        else
            return;

        //Retrieve all simulated hits
        String MCHitInputCollectionName = "TrackerHits";
        List<SimTrackerHit> allsimhits = event.get(SimTrackerHit.class, MCHitInputCollectionName);


        //MCParticleRelations
        
        List<LCRelation> trackToMCParticleRelations    =  new ArrayList<LCRelation>();
        
        //Truth Tracks and Relations
        List<LCRelation> trackToTruthTrackRelations    =  new ArrayList<LCRelation>();
        List<Track>      truthTrackCollection          =  new ArrayList<Track>();
        
        for (Track track : trackCollection) {
            
            //Truth Matching tool
            TrackTruthMatching ttm = new TrackTruthMatching(track, rawtomc, allsimhits, kalmanTracks);
            if (ttm != null) {
                MCParticle mcp = ttm.getMCParticle();
                
                if (mcp != null) {
                    trackToMCParticleRelations.add(new BaseLCRelation(track,mcp));
                    
                    //Hep3Vector origin = new BasicHep3Vector(0.,0.,0.);
                    HelicalTrackFit mcp_htf  = TrackUtils.getHTF(mcp,bfield);
                    HelicalTrackFit trk_htf  = TrackUtils.getHTF(track);
                
                    if (debug) {
                        System.out.println("--------------------");
                        System.out.println(trackCollectionName+" Track:");
                        System.out.printf("d0 %f z0 %f R %f phi %f lambda %s\n", trk_htf.dca(), trk_htf.z0(), trk_htf.R(), trk_htf.phi0(), trk_htf.slope());
                        System.out.printf("Nhits = %d \n",ttm.getNHits());
                        System.out.printf("NGoodHits = %d  purity = %f\n",ttm.getNGoodHits(),ttm.getPurity());
                        
                    }
                    BaseTrack truth_trk  = new BaseTrack();
                    truth_trk.setTrackParameters(mcp_htf.parameters(),bfield);
                    truth_trk.getTrackStates().clear();
                    double[] ref = new double[] { 0., 0., 0. };
                    SymmetricMatrix cov = new SymmetricMatrix(5);
                    TrackState stateIP = new BaseTrackState(mcp_htf.parameters(),ref,cov.asPackedArray(true),TrackState.AtIP,bfield);
                    truth_trk.getTrackStates().add(stateIP);
                    truth_trk.setChisq(-1);
                    truth_trk.setNDF(-1);
                    truth_trk.setFitSuccess(false);
                    truth_trk.setRefPointIsDCA(true);
                    truth_trk.setTrackType(-1);
                    truthTrackCollection.add(truth_trk);
                    trackToTruthTrackRelations.add(new BaseLCRelation(track,truth_trk));
                    
                    
                    if (debug) {
                        double d0    = truth_trk.getTrackStates().get(0).getD0();
                        double z0    = truth_trk.getTrackStates().get(0).getZ0();
                        double C     = truth_trk.getTrackStates().get(0).getOmega();
                        double phi   = truth_trk.getTrackStates().get(0).getPhi();
                        double slope = truth_trk.getTrackStates().get(0).getTanLambda();
                        System.out.printf("TruthTrack \n");
                        System.out.printf("d0 %f z0 %f R %f phi %f lambda %s\n", d0, z0, 1./C, phi, slope);
                        System.out.printf("MCParticle  \n");
                        Hep3Vector pVec = mcp.getMomentum();
                        System.out.printf("ptTrue = %f \n", Math.sqrt(pVec.x()*pVec.x() + pVec.z()*pVec.z()));
                        double mom_param = 2.99792458e-04;
                        double trkMom = trk_htf.R() * bfield * mom_param;
                        System.out.printf("pt = %f \n", trkMom);
                        
                        System.out.println("--------------------");
                    }
                }//mcp not null
                else {
                    System.out.printf("PF::FakeTrack");
                }
            } //ttm not null
            else {
                System.out.printf("Error::TrackTruthMatching returns null \n");
            }
        }
        
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(trackCollectionName+"Truth", truthTrackCollection, Track.class, flag);
        event.put(trackCollectionName+"ToTruthTrackRelations", trackToTruthTrackRelations, LCRelation.class, 0);
        event.put(trackCollectionName+"ToMCParticleRelations", trackToMCParticleRelations, LCRelation.class, 0);
    }//closes process
}
