package org.hps.recon.skims;

import java.util.Set;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TSData2019;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * Skimmer for full-energy-electron (FEE) events.
 *
 * The selection is applied in steps; the first (and, for now, only) step is the
 * trigger skim, which requires that the event was taken with an FEE trigger.
 * The trigger bits are read from the 2019+ TS bank, so this is applicable to the
 * 2019 and 2021 running periods.
 */
public class FEESkimmer extends Skimmer {
    //default parameters...ok for 2021 run
    private String _tsBankCollectionName = "TSBank";
    private boolean _requireFEETrigger = true;
    private boolean _useFEETopTrigger = true;
    private boolean _useFEEBotTrigger = true;
    private boolean _debug = false;
    private int totalFEETopTriggers = 0;
    private int totalFEEBotTriggers = 0;

    @Override
    public boolean passSelection(EventHeader event){
	if(_debug)
	    System.out.println(this.getClass().getName()+":: in pass selection");
	incrementEventProcessed();

	//step 1:  trigger skim
	if(!passTriggerSelection(event))
	    return false;

	incrementEventPassed();
	return true;
    }

    /**
     * Require an FEE trigger bit in the TS bank.  If neither the top nor the
     * bottom FEE trigger is requested, or if the trigger requirement is turned
     * off altogether, every event passes this step.
     */
    private boolean passTriggerSelection(EventHeader event){
	if(!_requireFEETrigger)
	    return true;

	if (!event.hasCollection(GenericObject.class, _tsBankCollectionName)) {
	    if(_debug)System.out.println(this.getClass().getName()+"::  no "+_tsBankCollectionName+" collection in event");
	    return false;
	}

	boolean pass=false;
	for (GenericObject tsBank : event.get(GenericObject.class, _tsBankCollectionName)) {
	    if (AbstractIntData.getTag(tsBank) != TSData2019.BANK_TAG)
		continue;
	    TSData2019 triggerData = new TSData2019(tsBank);
	    if (_useFEETopTrigger && triggerData.isFEETopTrigger()) {
		totalFEETopTriggers++;
		pass=true;
	    }
	    if (_useFEEBotTrigger && triggerData.isFEEBotTrigger()) {
		totalFEEBotTriggers++;
		pass=true;
	    }
	}

	if(_debug && !pass)
	    System.out.println(this.getClass().getName()+"::  failed FEE trigger");

	return pass;
    }

    public FEESkimmer(String file) {
	super(file, null);
    }
    public FEESkimmer(String file, Set<String> ignore) {
	super(file, ignore);
    }

    @Override
    public void setParameters(String parsFileName){
	String infilePreResDir = "/org/hps/recon/skims/";
	String infile=infilePreResDir+parsFileName;
        InputStream inParamStream = this.getClass().getResourceAsStream(infile);
        System.out.println(this.getClass().getName()+"::  reading in FEE skimming cuts from "+infile);
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
	if(parName.equals("tsBankCollectionName"))
	    _tsBankCollectionName=var;
	else if(parName.equals("requireFEETrigger"))
	    _requireFEETrigger=Boolean.parseBoolean(var);
	else if(parName.equals("useFEETopTrigger"))
	    _useFEETopTrigger=Boolean.parseBoolean(var);
	else if(parName.equals("useFEEBotTrigger"))
	    _useFEEBotTrigger=Boolean.parseBoolean(var);
	else
	    System.out.println(this.getClass().getName()+":: couldn't find "+parName+"!");
    }

    public int getTotalFEETopTriggers(){
	return totalFEETopTriggers;
    }

    public int getTotalFEEBotTriggers(){
	return totalFEEBotTriggers;
    }

    public void setRequireFEETrigger(boolean require){
	this._requireFEETrigger=require;
    }
    public void setUseFEETopTrigger(boolean use){
	this._useFEETopTrigger=use;
    }
    public void setUseFEEBotTrigger(boolean use){
	this._useFEEBotTrigger=use;
    }
    public void setTsBankCollectionName(String name){
	this._tsBankCollectionName=name;
    }
}
