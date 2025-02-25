package org.hps.recon.skims;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List; 
import java.util.ArrayList;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;

import org.lcsim.util.Driver;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;

public class MultiSkimDriver extends Driver {

    private static final Logger LOGGER = Logger.getLogger(MultiSkimDriver.class.getPackage().getName());

    //if this is true, an event will be written to each stream that it passes
    //if false, it will only go into the first stream it passes... so the ordering of the skim list matters.  
    private boolean allowEventsInMultipleStreams=false;  
    //   this is not super smart...just hard-coding the possible skims
    private boolean skimV0=false;
    private boolean skimThreeBody=false;
    private boolean skimFEE=false;
    private boolean skimMoller=false;

    private String ouputDir="skimData"; 

    private String v0OutputFile="v0Skim"; 
    private String threeBodyOutputFile="threeBodySkim"; 
    private String FEEOutputFile="FEESkim"; 
    private String mollerOutputFile="mollerSkim"; 

    private Skimmer v0Skimmer; 
    private Skimmer threeBodySkimmer; 
    private Skimmer FEESkimmer; 
    private Skimmer mollerSkimmer;     
    
    protected Double beamEnergy;

    private int nprocessed = 0;
    private int npassed = 0;
    List<Skimmer> writeSkimList=new ArrayList<Skimmer>(); 

    
    public void endOfData() {
        System.out.println(this.getClass().getSimpleName() + " Summary: ");
        System.out.println("events processed = " + nprocessed);
        System.out.println("events passed    = " + npassed);
        System.out.println("       rejection = " + ((double) npassed) / nprocessed);

    }

    @Override
    protected void process(EventHeader event) {
	writeSkimList.clear();
	//  check each skim and see if event passes
	if(skimV0 &&
	   v0Skimmer.passSelection(event))
	    writeSkimList.add(v0Skimmer);
	
	if(skimThreeBody &&
	   threeBodySkimmer.passSelection(event))
	    writeSkimList.add(threeBodySkimmer); 

	if(skimFEE &&
	   FEESkimmer.passSelection(event))
	    writeSkimList.add(FEESkimmer);
	if(skimMoller &&
	   mollerSkimmer.passSelection(event))
	    writeSkimList.add(mollerSkimmer);

	
	if(writeSkimList.size()>0)
	    writeEventToStreams(event,writeSkimList);	
    }


    public void writeEventToStreams(EventHeader event, List<Skimmer> skimsToWrite){
	for(Skimmer skim: skimsToWrite)
	    skim.writeEvent(event); 	
    }

    
    public void incrementEventProcessed() {
        nprocessed++;
    }

    public void incrementEventPassed() {
        npassed++;
    }

    public void skipEvent() {
        throw new Driver.NextEventException();
    }

    public void setBeamEnergy(double e) {
        this.beamEnergy = e;
    }

    public double getBeamEnergy() {
        return this.beamEnergy;
    }

    public void setV0OutputFile(String outputFile){
	this.v0OutputFile=outputFile; 
    }
    
    @Override
    protected void detectorChanged(Detector detector) {
        BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                .getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();
        if (beamEnergy == null && beamEnergyCollection != null && beamEnergyCollection.size() != 0)
            beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
        else {
            LOGGER.log(Level.WARNING, "warning:  beam energy not found.  Using  3.74 GeV as the default energy");
            beamEnergy = 3.74;
        }	
	
	//set up skims
	if(skimV0)
	    v0Skimmer=new V0Skimmer(ouputDir+"/"+v0OutputFile); 
	if(skimThreeBody)
	    threeBodySkimmer=new ThreeBodySkimmer(ouputDir+"/"+threeBodyOutputFile); 
	if(skimFEE)
	    FEESkimmer=new FEESkimmer(ouputDir+"/"+FEEOutputFile); 
	if(skimMoller)
	    mollerSkimmer=new MollerSkimmer(ouputDir+"/"+mollerOutputFile); 

    }

    public void setSkimV0(boolean doSkim){
	this.skimV0=doSkim; 
    }

    public void setSkimThreeBody(boolean doSkim){
	this.skimThreeBody=doSkim; 
    }
     public void setSkimFEE(boolean doSkim){
	this.skimFEE=doSkim; 
    }
     public void setSkimMoller(boolean doSkim){
	this.skimMoller=doSkim; 
    }

}
