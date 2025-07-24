package org.hps.recon.skims;
import static java.lang.Math.abs;

import java.util.List;
import java.util.Set; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.EventHeader;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackType;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;

public class V0Skimmer extends Skimmer {
    //default parameters...ok for 2021 run
    private String _V0CandidateCollectionName = "UnconstrainedV0Candidates_KF";
    private String _V0VertexCollectionName = "UnconstrainedV0Vertices_KF";
    //private double _clusterTimingCut = 20.0; // only used if _tight is true
    private double _posClusterEnergy =  0.2; //GeV
    private double _v0Chi2Cut = 100.0;
    private double _trackChi2Cut = 30.0;
    private double _trackDtCut = 20.0; // the 2-track time difference
    private double _trackPMax = 4.5; //GeV
    private double _elePMax = 9999; //GeV
    private double _v0PMax =  4.5;   //GeV
    private int    _nHitsMin=9;
    private boolean _reqClusterMatch=false; 
    private boolean _debug=false;    
    private int totalV0s=0; 
    private int totalV0sPassing=0; 
    @Override
    public boolean passSelection(EventHeader event){
	if(_debug)
	    System.out.println(this.getClass().getName()+":: in pass selection"); 
	incrementEventProcessed();
	
	if (!event.hasCollection(ReconstructedParticle.class, _V0CandidateCollectionName)) {
	     return false; 
        }	
	if (!event.hasCollection(Vertex.class, _V0VertexCollectionName)) {
	     return false; 
        }


	
        List<ReconstructedParticle> V0Candidates = event.get(ReconstructedParticle.class, _V0CandidateCollectionName);
	List<Vertex> V0Vertexes= event.get(Vertex.class, _V0VertexCollectionName);
	if(V0Candidates.size() != V0Vertexes.size())
	    System.out.println(this.getClass().getName()+":: Number of Vertexes = "+V0Vertexes.size()+
			       "; number of candidates = "+V0Candidates.size());
        int nV0 = 0; // number of good V0
	totalV0s += V0Candidates.size();
        for (ReconstructedParticle v0 : V0Candidates) {
            ReconstructedParticle electron = v0.getParticles().get(ReconParticleDriver.ELECTRON);
            ReconstructedParticle positron = v0.getParticles().get(ReconParticleDriver.POSITRON);

            if (v0.getStartVertex().getChi2() > _v0Chi2Cut) {
		if(_debug)System.out.println(this.getClass().getName()+"::  failed vertex chi2");
                continue;
            }
	    if(electron.getTracks().get(0).getTrackerHits().size()<_nHitsMin
               || positron.getTracks().get(0).getTrackerHits().size()<_nHitsMin){
		if(_debug)System.out.println(this.getClass().getName()+"::  failed nHitsMin "+electron.getTracks().get(0).getTrackerHits().size()+"  "+positron.getTracks().get(0).getTrackerHits().size()+" nHitsMin = "+_nHitsMin);
                continue;
            }
            if ((electron.getTracks().get(0).getChi2()/electron.getTracks().get(0).getNDF()) > _trackChi2Cut
                    || (positron.getTracks().get(0).getChi2()/positron.getTracks().get(0).getNDF()) > _trackChi2Cut) {
		if(_debug)System.out.println(this.getClass().getName()+"::  failed track chi2");
                continue;
            }

	   	    
	    float[] elePTD={TrackData.getTrackData(event, electron.getTracks().get(0)).getFloatVal(1),
		    TrackData.getTrackData(event, electron.getTracks().get(0)).getFloatVal(2),
		    TrackData.getTrackData(event, electron.getTracks().get(0)).getFloatVal(3)}; 
	    float[] posPTD={TrackData.getTrackData(event, positron.getTracks().get(0)).getFloatVal(1),
		    TrackData.getTrackData(event, positron.getTracks().get(0)).getFloatVal(2),
		    TrackData.getTrackData(event, positron.getTracks().get(0)).getFloatVal(3)}; 
	    double elePMag =  (new BasicHep3Vector(elePTD)).magnitude();
	    double posPMag =  (new BasicHep3Vector(posPTD)).magnitude();
	    if (elePMag > _trackPMax || posPMag > _trackPMax) {
		if(_debug)System.out.println(this.getClass().getName()+"::  failed track momentum");
		continue;
	    }
	    if(electron.getMomentum().magnitude() > _elePMax){
		if(_debug)System.out.println(this.getClass().getName()+"::  failed electron momentum");
		continue; 
	    }
	    if ((elePMag+posPMag) > _v0PMax) {
		if(_debug)System.out.println(this.getClass().getName()+"::  failed v0 momentum");
                continue;
            }
            double eleTime = TrackData.getTrackTime(TrackData.getTrackData(event, electron.getTracks().get(0)));
            double posTime = TrackData.getTrackTime(TrackData.getTrackData(event, positron.getTracks().get(0)));
            if (Math.abs(eleTime - posTime) > _trackDtCut) {
		if(_debug)System.out.println(this.getClass().getName()+"::  failed track dt");
                continue;
            }
            if (_reqClusterMatch) { // requires cluster matches and cluster time cut
		//                if (electron.getClusters().isEmpty() || positron.getClusters().isEmpty()) {
                //    continue;
                //}
                if (positron.getClusters().isEmpty()) {
                    continue;
                }
                // calorimeter cluster timing cut
                // first CalorimeterHit in the list is the seed crystal
                //double t1 = ClusterUtilities.getSeedHitTime(electron.getClusters().get(0));
                //double t2 = ClusterUtilities.getSeedHitTime(positron.getClusters().get(0));

                //if (abs(t1 - t2) > _clusterTimingCut) {
                //    continue;
                //}
		if(positron.getClusters().get(0).getEnergy()<_posClusterEnergy)
		    continue; 
            }
            nV0++;
	    totalV0sPassing++; 
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
    public V0Skimmer(String file, Set<String> ignore) {
	super(file, ignore); 
    }

    @Override
    public void setParameters(String parsFileName){
	String infilePreResDir = "/org/hps/recon/skims/"; 
	String infile=infilePreResDir+parsFileName; 
        InputStream inParamStream = this.getClass().getResourceAsStream(infile);
        System.out.println(this.getClass().getName()+"::  reading in V0 skimming cuts from "+infile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inParamStream));
        String line;
        String delims = "[ ]+";// this will split strings between one or more spaces
	try {
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(delims);
		String parName=tokens[0].replaceAll("\\s+","");
                System.out.println(this.getClass().getName()+"::  parameter name = " + parName + "; value = " + tokens[1]);
		putParam(parName,tokens[1]);
		
            }
        } catch (IOException ex) {
	    System.out.println(this.getClass().getName()+":: died while reading parameters");
            return;
        }        
	return;
    }


    private void putParam(String parName, String var){
	if(parName.equals("V0CandidateCollectionName"))
	    _V0CandidateCollectionName=var;
	//	else if(parName.equals("clusterTimingCut"))
	//	    _clusterTimingCut=Double.parseDouble(var); 
	else if(parName.equals("v0Chi2Cut"))
	    _v0Chi2Cut=Double.parseDouble(var);
	else if(parName.equals("trackChi2Cut"))
	    _trackChi2Cut=Double.parseDouble(var);
	else if(parName.equals("trackDtCut"))
	    _trackDtCut=Double.parseDouble(var);
	else if(parName.equals("trackPMax"))
	    _trackPMax=Double.parseDouble(var);
	else if(parName.equals("elePMax"))
	    _elePMax=Double.parseDouble(var);
	else if(parName.equals("v0PMax"))
	    _v0PMax=Double.parseDouble(var);
	else if(parName.equals("nHitsMin"))
	    _nHitsMin=Integer.parseInt(var);
	else if(parName.equals("reqClusterMatch"))
	    _reqClusterMatch=Boolean.parseBoolean(var);
	else if(parName.equals("posClusterEnergy"))
	    _posClusterEnergy=Double.parseDouble(var);
	else
	    System.out.println(this.getClass().getName()+":: couldn't find "+parName+"!");  
    }

    public int getTotalV0sPassing(){
	return totalV0sPassing; 
    }
    
    public int getTotalV0s(){
	return totalV0s; 
    }
    
    //    public void setClusterTimeCut(double cutVal){
    //	this._clusterTimingCut=cutVal; 
    //    }
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
