package org.hps.users.spaul;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.ecal.tdeg.TdegTweakDriver;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.hps.rundb.RunManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.Track;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;


public class EcalPeaksTupleMaker extends Driver {
	private static final Logger LOGGER = Logger.getLogger(EcalPeaksTupleMaker.class.getPackage().getName());

	private String clusterCollection = "EcalClustersCorr";
	private String particleCollection = "FinalStateParticles";
	
	protected Double beamEnergy;


    public void setBeamEnergy(double e){
    this.beamEnergy = e;
    }
    public double getBeamEnergy(){
    return this.beamEnergy;
    }
    
    
    @Override
    protected void detectorChanged(Detector detector){
        BeamEnergyCollection beamEnergyCollection = 
            this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        if(beamEnergy== null && beamEnergyCollection != null && beamEnergyCollection.size() != 0)
            beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
        else{
            LOGGER.log(Level.WARNING, "warning:  beam energy not found.  Using a 6.6 GeV as the default energy");
            beamEnergy = 6.6;
        }
        
       
       
    }
    
    
    protected void endOfData(){
        out.close();
    }
    
    
    PrintStream out;
    
    public void setOutputFile(String filename){
        try {
            out = new PrintStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    


	
	//private long firsttimestamp = 0;
	Long tiTimeOffset;


    private final long timestampCycle = 24 * 6 * 35;
    private Long getTiTimeOffset(int run) {
        Long currentTiTimeOffset = null;
        RunManager runManager = RunManager.getRunManager();
       // runManager.setRun(runNumber);
        if (runManager.getRun() != null) {
            if (runManager.runExists()) {
                currentTiTimeOffset = runManager.getRunSummary().getTiTimeOffset();
                tiTimeOffset = (currentTiTimeOffset / timestampCycle) * timestampCycle;
                LOGGER.info("TI time offset set to " + currentTiTimeOffset + " for run "
                        + run + " from database");
            } else {
                LOGGER.warning("Run " + run 
                        + " does not exist in the run database.");
            }
        } else {
            LOGGER.info("Run manager is not initialized; TI time offset not available.");
        }
        /* Make sure connection is closed immediately. --JM */
        try {
            LOGGER.info("Closing run manager db connection ...");
            RunManager.getRunManager().closeConnection();
            LOGGER.info("Run manager db connection was closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
       return tiTimeOffset;
    }
    
    
	@Override
	public void process(EventHeader event){
		int runNumber = event.getRunNumber();
		
		long timestamp = event.getTimeStamp();
		if(timestamp < 1e18)
		{
		    if(tiTimeOffset == null){
		        tiTimeOffset = getTiTimeOffset(runNumber);
		    }
		    timestamp += tiTimeOffset;
		}
		//System.out.println(timestamp);
		
		
		boolean isSingle1 = false;
		boolean isPair1 = false;
        boolean isPair0 = false;
        boolean isSingle0 = false;
		for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
		{
			if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
			TIData tid = new TIData(gob);
			if (tid.isPair0Trigger()) isPair0 = true;
            if (tid.isPair1Trigger()) isPair1 = true;
			if (tid.isSingle1Trigger()) isSingle1 = true;
            if (tid.isSingle0Trigger()) isSingle0 = true;
		}
		
		if(!isSingle1 && !isSingle0 && !isPair1)
			return;

		List<Cluster> clusters = event.get(Cluster.class, clusterCollection);
		List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class, particleCollection);
		//if(isSingle1 || isSingle0)
			
		//first filter out any events that don't have a track with at least 90\% of the beam energy.  
		//This will significantly reduce the size of the left-side tail of the Ecal measured energy distribution.
		ReconstructedParticle feeFound = null;
		List<Cluster> matchedClusters = new ArrayList();
		for(ReconstructedParticle p: particles){
			if(p.getMomentum().z() > .9*beamEnergy && p.getTracks().size() != 0 && p.getGoodnessOfPID() < 5 && p.getClusters().size() ==1 && p.getType() > 31){
				feeFound = p;
				matchedClusters.addAll(p.getClusters());
				//break;
			}
		}
		if(feeFound == null)
			return;
		for(Cluster c1 : clusters){
			if(c1.getSize() < 3)
				continue;
			if(isEdge(c1))
				continue;
			
			int hasTrack = matchedClusters.contains(c1) ? 1:0;
			
			//ix, iy, clusterEnergy, seedEnergy, time, timestamp, hasTrack, nhits
			out.printf("%d,%d,%f,%f,%f,%d,%d,%d\n",
			            c1.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
                        c1.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"),
                        c1.getEnergy(),
                        c1.getCalorimeterHits().get(0).getCorrectedEnergy(),
                        c1.getCalorimeterHits().get(0).getTime(),
                        timestamp,
                        hasTrack,
                        c1.getCalorimeterHits().size()
			            
			    );
				
		}
	}
	/*private boolean isEdge(Cluster c){
		CalorimeterHit seed = c.getCalorimeterHits().get(0);
		int ix = seed.getIdentifierFieldValue("ix");
		int iy = seed.getIdentifierFieldValue("iy");
		return isEdge(ix, iy);
			
	}*/
	
	
	boolean isEdge(Cluster c){
        return !(c.getPosition()[0] > -262.74 && c.getPosition()[0] < 347.7 && Math.abs(c.getPosition()[1])>33.54 
                && Math.abs(c.getPosition()[1])<75.18 
                && !(c.getPosition()[0]>-106.66 && c.getPosition()[0] < 42.17 && Math.abs(c.getPosition()[1])<47.17));
    }
	
	
	/*private boolean isEdge(int ix, int iy){
        if(iy == 5 || iy == 1 || iy == -1 || iy == -5)
            return true;
        if(ix == -23 || ix == 23)
            return true;
        if((iy == 2 || iy == -2) && (ix >=-11 && ix <= -1))
            return true;
        return false;
    }*/
	
	

	
	
}
