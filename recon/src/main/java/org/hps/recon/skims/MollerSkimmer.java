package org.hps.recon.skims;

import org.lcsim.event.EventHeader;


public class MollerSkimmer extends Skimmer {
    private String _MollerCandidateCollectionName = "UnconstrainedMollerCandidates";
    private double _clusterTimingCut = 20.0; // only used if _tight is true
    private double _v0Chi2Cut = 100.0;
    private double _trackChi2Cut = 80.0;
    private double _trackDtCut = 20.0;
    private double _trackPMax = 0.9;
    private double _v0PMax = 1.4;
    private int    _nHitsMin=10;
    
    @Override
    public boolean passSelection(EventHeader event){
	System.out.println(this.getClass().getName()+":: in pass selection"); 
	boolean pass=true; 
	
	
	return pass; 
    }

    
    public MollerSkimmer(String file) {
	super(file, null); 
	//        this(super.addFileExtension(file), null);
    }

}
