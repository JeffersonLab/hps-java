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

public class MollerSkimmer extends Skimmer {
    private String _MollerCandidateCollectionName = "UnconstrainedMollerCandidates_KF";
    private String _MollerVertexCollectionName = "UnconstrainedMollerVertices_KF";
    private double _vtxChi2Cut = 100.0;
    private double _trackChi2Cut = 30.0;
    private double _trackDtCut = 20.0; 
    private int    _nHitsMin=9;
    private boolean _debug=false;    
    private int totalMollers=0; 
    private int totalMollersPassing=0; 

    @Override
    public boolean passSelection(EventHeader event){

	if(_debug)
	    System.out.println(this.getClass().getName()+":: in pass selection"); 
	incrementEventProcessed();
	
	if (!event.hasCollection(ReconstructedParticle.class, _MollerCandidateCollectionName)) {
	     return false; 
        }	
	if (!event.hasCollection(Vertex.class, _MollerVertexCollectionName)) {
	     return false; 
        }

        List<ReconstructedParticle> V0Candidates = event.get(ReconstructedParticle.class, _MollerCandidateCollectionName);
	List<Vertex> V0Vertexes= event.get(Vertex.class, _MollerVertexCollectionName);

	if(V0Candidates.size() != V0Vertexes.size())
	    System.out.println(this.getClass().getName()+":: Number of Vertexes = "+V0Vertexes.size()+
			       "; number of candidates = "+V0Candidates.size());

        int nMollers = 0; 
	totalMollers += V0Candidates.size();
        for (ReconstructedParticle v0 : V0Candidates) {

            ReconstructedParticle eleTop = v0.getParticles().get(ReconParticleDriver.MOLLER_TOP);
            ReconstructedParticle eleBot = v0.getParticles().get(ReconParticleDriver.MOLLER_BOT);

            if (v0.getStartVertex().getChi2() > _vtxChi2Cut) {
		if(_debug)System.out.println(this.getClass().getName()+"::  failed vertex chi2");
                continue;
            }
	    if(eleTop.getTracks().get(0).getTrackerHits().size()<_nHitsMin
               || eleBot.getTracks().get(0).getTrackerHits().size()<_nHitsMin){
		if(_debug)System.out.println(this.getClass().getName()+"::  failed nHitsMin "+eleTop.getTracks().get(0).getTrackerHits().size()+"  "+eleBot.getTracks().get(0).getTrackerHits().size()+" nHitsMin = "+_nHitsMin);
                continue;
            }
            if ((eleTop.getTracks().get(0).getChi2()/eleTop.getTracks().get(0).getNDF()) > _trackChi2Cut
                    || (eleBot.getTracks().get(0).getChi2()/eleBot.getTracks().get(0).getNDF()) > _trackChi2Cut) {
		if(_debug)System.out.println(this.getClass().getName()+"::  failed track chi2");
                continue;
            }
	   	    
            double eleTime = TrackData.getTrackTime(TrackData.getTrackData(event, eleTop.getTracks().get(0)));
            double posTime = TrackData.getTrackTime(TrackData.getTrackData(event, eleBot.getTracks().get(0)));
            if (Math.abs(eleTime - posTime) > _trackDtCut) {
		if(_debug)System.out.println(this.getClass().getName()+"::  failed track dt");
                continue;
            }
            nMollers++;
	    totalMollersPassing++; 
        }

	if (nMollers>0){
	    incrementEventPassed();
	    return true;
	} else
	    return false;
	
    }    
      
    public MollerSkimmer(String file) {
	super(file, null); 
    }
    public MollerSkimmer(String file, Set<String> ignore) {
	super(file, ignore); 
    }

    @Override
    public void setParameters(String parsFileName){
	String infilePreResDir = "/org/hps/recon/skims/"; 
	String infile=infilePreResDir+parsFileName; 
        InputStream inParamStream = this.getClass().getResourceAsStream(infile);
        System.out.println(this.getClass().getName()+"::  reading in Moller skimming cuts from "+infile);
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
	if(parName.equals("MollerCandidateCollectionName"))
	    _MollerCandidateCollectionName=var;
	else if(parName.equals("vtxChi2Cut"))
	    _vtxChi2Cut=Double.parseDouble(var);
	else if(parName.equals("trackChi2Cut"))
	    _trackChi2Cut=Double.parseDouble(var);
	else if(parName.equals("trackDtCut"))
	    _trackDtCut=Double.parseDouble(var);
	else if(parName.equals("nHitsMin"))
	    _nHitsMin=Integer.parseInt(var);
	else
	    System.out.println(this.getClass().getName()+":: couldn't find "+parName+"!");  
    }

    public int getTotalMollersPassing(){
	return totalMollersPassing; 
    }
    
    public int getTotalMollers(){
	return totalMollers; 
    }
    
    public void setVtxChi2Cut(double cutVal){
	this._vtxChi2Cut=cutVal; 
    }
    public void setTrackChi2Cut(double cutVal){
	this._trackChi2Cut=cutVal; 
    }
    public void setTrackDtCut(double cutVal){
	this._trackDtCut=cutVal; 
    }
    public void setNHitsMin(int cutVal){
	this._nHitsMin=cutVal; 
    }
}
