package org.hps.recon.skims;
import static java.lang.Math.abs;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackType;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

public class V0Skimmer extends Skimmer {
    private String _V0CandidateCollectionName = "UnconstrainedV0Candidates";
    private double _clusterTimingCut = 20.0; // only used if _tight is true
    private double _v0Chi2Cut = 100.0;
    private double _trackChi2Cut = 80.0;
    private double _trackDtCut = 20.0; // the 2-track time difference
    private double _trackPMax = 3.4; //GeV
    private double _v0PMax =  4.5;   //GeV
    private int    _nHitsMin=10;
    private boolean _reqClusterMatch=false; 
    private boolean _debug=false;
    
    @Override
    public boolean passSelection(EventHeader event){
	if(_debug)
	    System.out.println(this.getClass().getName()+":: in pass selection"); 
	incrementEventProcessed();
	
	 if (!event.hasCollection(ReconstructedParticle.class, _V0CandidateCollectionName)) {
	     return false; 
        }
        List<ReconstructedParticle> V0Candidates = event.get(ReconstructedParticle.class, _V0CandidateCollectionName);
        int nV0 = 0; // number of good V0
        for (ReconstructedParticle v0 : V0Candidates) {
            ReconstructedParticle electron = v0.getParticles().get(ReconParticleDriver.ELECTRON);
            ReconstructedParticle positron = v0.getParticles().get(ReconParticleDriver.POSITRON);

            //            if (!TrackType.isGBL(v0.getType())) { // we only care about GBL vertices
            //                continue;
            //}
            
            if (v0.getStartVertex().getChi2() > _v0Chi2Cut) {
                continue;
            }

            if(electron.getTracks().get(0).getTrackerHits().size()<_nHitsMin
               || positron.getTracks().get(0).getTrackerHits().size()<_nHitsMin){
                continue;
            }
            if (electron.getTracks().get(0).getChi2() > _trackChi2Cut
                    || positron.getTracks().get(0).getChi2() > _trackChi2Cut) {
                continue;
            }
            if (electron.getMomentum().magnitude() > _trackPMax || positron.getMomentum().magnitude() > _trackPMax) {
                continue;
            }
            if (v0.getMomentum().magnitude() > _v0PMax) {
                continue;
            }
            double eleTime = TrackData.getTrackTime(TrackData.getTrackData(event, electron.getTracks().get(0)));
            double posTime = TrackData.getTrackTime(TrackData.getTrackData(event, positron.getTracks().get(0)));
            if (Math.abs(eleTime - posTime) > _trackDtCut) {
                continue;
            }
            if (_reqClusterMatch) { // requires cluster matches and cluster time cut
                if (electron.getClusters().isEmpty() || positron.getClusters().isEmpty()) {
                    continue;
                }
                // calorimeter cluster timing cut
                // first CalorimeterHit in the list is the seed crystal
                double t1 = ClusterUtilities.getSeedHitTime(electron.getClusters().get(0));
                double t2 = ClusterUtilities.getSeedHitTime(positron.getClusters().get(0));

                if (abs(t1 - t2) > _clusterTimingCut) {
                    continue;
                }
            }
            nV0++;
        }

	if (nV0>0){
	    incrementEventPassed();
	    return true;
	} else
	    return false;
	
    }    
      
    public V0Skimmer(String file) {
	super(file, null); 
    }
    
    public void setClusterTimeCut(double cutVal){
	this._clusterTimingCut=cutVal; 
    }
    public void setV0Chi2Cut(double cutVal){
	this._v0Chi2Cut=cutVal; 
    }
    public void setTrackChi2Cut(double cutVal){
	this._trackChi2Cut=cutVal; 
    }
    public void setTrackDtCut(double cutVal){
	this._trackDtCut=cutVal; 
    }
    public void setTrackPMax(double cutVal){
	this._trackPMax=cutVal; 
    }
    public void setV0PMax(double cutVal){
	this._v0PMax=cutVal; 
    }
    public void setNHitsMin(int cutVal){
	this._nHitsMin=cutVal; 
    }
}
