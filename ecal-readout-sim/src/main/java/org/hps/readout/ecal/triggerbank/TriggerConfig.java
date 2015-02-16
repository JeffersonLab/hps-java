package org.hps.readout.ecal.triggerbank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerConfig {

    /*
     * Read/Parse/Save the DAQ trigger configuration settings.
     * 
     * This is a work in progress, activate by uncommenting
     * line 61 in LCSimEngRunEventBuilder.java
     *
     * Maybe should store all the parameters in another
     * map after parsing for direct access to minimize code,
     * instead of all these variables. 
     *
     * TODO: convert SET_IO_SRC to trigger enabled bits
     * TODO: Test and Debug
     * TODO: port datacat/python/engrun/engrun_metadata.py in fixConfigMap2014Run
     * TODO: Parse and save threhsolds.
     * TODO: Get NSA and NSB to remove hardcoded window in EcalRawConverter.
     *  
     *  NAB 2015/02/15
     */
   
    public static final int bankTag = 0xE10E;
    
	// Dump everything read from the DAQ Configuration Bank, minimal interpretation:
	public Map<String,List<String>> configMap=new HashMap<String,List<String>>();
    
	private boolean debug=true;

	// These are not all the parameters available in the run configuration bank,
	// just *most* of the ones needed for trigger studies:

	// General Clustering Cut Values:
	public int clusterMinSeedEnergy=0;
	public int clusterMinHitTimeDiff=0;
	public int clusterMaxHitTimeDiff=0;
	
	// Triggers Enabled:
	public boolean[] singlesEn={false,false};
	public boolean[] pairsEn={false,false};

	// Singles Cuts Enabled:
	public boolean[] singlesNhitsEn={false,false};
	public boolean[] singlesEnergyMinEn={false,false};
	public boolean[] singlesEnergyMaxEn={false,false};
	
	// Pairs Cuts Enabled:
	public boolean[] pairsEnergySumMinEn={false,false};
	public boolean[] pairsEnergySumMaxEn={false,false};
	public boolean[] pairsEnergyDiffEn={false,false};
	public boolean[] pairsCoplanarityEn={false,false};
	public boolean[] pairsEnergyDistEn={false,false};
	public boolean[] pairsTimeDiffEn={false,false};
	
	// Singles Cut Values:
	public int[] singlesNhits={0,0};
	public int[] singlesEnergyMin={0,0};
	public int[] singlesEnergyMax={0,0};
	
	// Pairs Cut Values:
	public int[] pairsEnergySumMin={0,0};
	public int[] pairsEnergySumMax={0,0};
	public int[] pairsEnergyDiffMax={0,0};
	public int[] pairsCoplanarityMax={0,0};
	public int[] pairsTimeDiffMax={0,0};
	public int[] pairsEnergyDistMin={0,0};
	
	// Pairs Cut Parameters:
	public float[] pairsEnergyDistSlope={0,0};

    public TriggerConfig(int runNumber,String[] dump) {

    	loadConfigMap(dump);
    	
    	if (debug) printMap();
        
        fixConfigMap2014Run(runNumber);
        
        parseConfigMap();
        
        if (debug) printVars();
    }
  

    public void parseConfigMap()
    {
        clusterMinSeedEnergy=Integer.valueOf(getConfig("GTP_CLUSTER_THRESH",0));
        clusterMinHitTimeDiff=Integer.valueOf(getConfig("GTP_TIMEDIFF",0));
        clusterMaxHitTimeDiff=Integer.valueOf(getConfig("GTP_TIMEDIFF",1));
        
        for (int ii=0; ii<2; ii++) {
            singlesEn[ii]=getBoolConfigSSP(ii,"SINGLES_EN",0);
            pairsEn[ii]=getBoolConfigSSP(ii,"PAIRS_EN",0);
            
            singlesNhitsEn[ii]=getBoolConfigSSP(ii,"SINGLES_NHITS",0);
            singlesEnergyMinEn[ii]=getBoolConfigSSP(ii,"SINGLES_EMIN",0);
            singlesEnergyMaxEn[ii]=getBoolConfigSSP(ii,"SINGLES_EMAX",0);

            pairsEnergySumMinEn[ii]=getBoolConfigSSP(ii,"PAIRS_SUMMAX_MIN",0);
            pairsEnergySumMaxEn[ii]=getBoolConfigSSP(ii,"PAIRS_SUMMAX_MIN",0);
            pairsEnergyDiffEn[ii]=getBoolConfigSSP(ii,"PAIRS_DIFFMAX",0);
            pairsCoplanarityEn[ii]=getBoolConfigSSP(ii,"PAIRS_COPLANARITY",0);
            pairsEnergyDistEn[ii]=getBoolConfigSSP(ii,"PAIRS_ENERGYDIST",0);
            pairsTimeDiffEn[ii]=getBoolConfigSSP(ii,"PAIRS_TIMEDIFF",0);

            singlesNhits[ii]=getIntConfigSSP(ii,"SINGLES_NHITS",1);
            singlesEnergyMin[ii]=getIntConfigSSP(ii,"SINGLES_EMIN",1);
            singlesEnergyMax[ii]=getIntConfigSSP(ii,"SINGLES_EMAX",1);

            pairsEnergySumMin[ii]=getIntConfigSSP(ii,"PAIRS_SUMMAX_MIN",1);
            pairsEnergySumMax[ii]=getIntConfigSSP(ii,"PAIRS_SUMMAX_MIN",2);
            pairsEnergyDiffMax[ii]=getIntConfigSSP(ii,"PAIRS_DIFFMAX",1);
            pairsCoplanarityMax[ii]=getIntConfigSSP(ii,"PAIRS_COPLANARITY",1);
            pairsTimeDiffMax[ii]=getIntConfigSSP(ii,"PAIRS_TIMEDIFF",1);
            pairsEnergyDistMin[ii]=getIntConfigSSP(ii,"PAIRS_ENERGYDIST",2);     // <!--- CHECK ME
            pairsEnergyDistSlope[ii]=getFloatConfigSSP(ii,"PAIRS_ENERGYDIST",1); // <!--- CHECK ME
        }
    }
    
   
      
    public void fixConfigMap2014Run(int runNumber) {
        if (runNumber>3470 || runNumber < 3100) return;
     	// TODO: port datacat/python/engrun/engrun_metadata.py
    	for (String key : configMap.keySet()) {
    	}
    	List<String> tmp=new ArrayList<String>();
    	tmp.add("0");
    	configMap.put("GTP_CLUSTER_THRESH",tmp);
    	tmp.add("0");
    	configMap.put("GTP_TIMEDIFF",tmp);
    }
    
    private void loadConfigMap(String[] dump) {
    
        for (String dump1 : dump) {
            for (String line : dump1.trim().split("\n")) {

                String[] cols=line.trim().split(" +",2);
                if (cols.length < 2) continue;

                String key=cols[0];
                List<String> vals=new ArrayList<String>
                    (Arrays.asList(cols[1].trim().split(" +")));

                // This should never happen:
                if (vals.size() < 1) {
                	continue;
                }
                
                // Pickup GTP case:
                
                // This should happen, but is only useful for FADC thresholds/pedestals:
                // NOPE, Trigger studies are going to want to know all these.
                if (vals.size() > 4) {
                    // TODO: put global threshold trick here
                    continue;
                }
                
                // Append trigger# onto key:
                if (vals.size() > 1)
                {
                    key += "_"+vals.remove(0);
                }
               
                // dump it into the map:
                configMap.put(key,vals);
            }
        }
    }
    
    public void printMap() {
        System.out.print("TriggerConfig:  ");
        for (String key : configMap.keySet()) {
            System.out.printf("%s ",key);
            for (String val : configMap.get(key)) {
                System.out.printf("%s ",val);
            }
            System.out.printf("\n");
        }
    }

    public void printVars()
	{
	    System.out.println(String.format("GTPMINSEED: %d",clusterMinSeedEnergy));
	    System.out.println(String.format("GTPMINHITDT: %d",clusterMinHitTimeDiff));
	    System.out.println(String.format("GTPMAXHITDT: %d",clusterMaxHitTimeDiff));
	    for (int ii=0; ii<2; ii++)
	    {
	        System.out.println(String.format("SSPSINGLESEN %d %b: ",ii,singlesEn[ii]));
	        System.out.println(String.format("SSPSINGLESEN %d %b: ",ii,pairsEn[ii]));
	        
	        System.out.println(String.format("SSPSINGLESNHITSEN %d %b:  ",ii,singlesNhitsEn[ii]));
	        System.out.println(String.format("",ii,singlesEnergyMinEn[ii]));
	        System.out.println(String.format("",ii,singlesEnergyMaxEn[ii]));
	        
	        System.out.println(String.format("",ii,pairsEnergySumMinEn[ii]));
	        System.out.println(String.format("",ii,pairsEnergySumMaxEn[ii]));
	        System.out.println(String.format("",ii,pairsEnergyDiffEn[ii]));
	        System.out.println(String.format("",ii,pairsCoplanarityEn[ii]));
	        System.out.println(String.format("",ii,pairsEnergyDistEn[ii]));
	        System.out.println(String.format("",ii,pairsTimeDiffEn[ii]));
	        
	        System.out.println(String.format("",ii,singlesNhits[ii]));
	        System.out.println(String.format("",ii,singlesEnergyMin[ii]));
	        System.out.println(String.format("",ii,singlesEnergyMax[ii]));
	        
	        System.out.println(String.format("",ii,pairsEnergySumMin[ii]));
	        System.out.println(String.format("",ii,pairsEnergySumMax[ii]));
	        System.out.println(String.format("",ii,pairsEnergyDiffMax[ii]));
	        System.out.println(String.format("",ii,pairsCoplanarityMax[ii]));
	        System.out.println(String.format("",ii,pairsTimeDiffMax[ii]));
	        System.out.println(String.format("",ii,pairsEnergyDistMin[ii]));
	        System.out.println(String.format("",ii,pairsEnergyDistSlope[ii]));
	    }
	}
  
    public float getFloatConfigSSP(int itrig,String stub,int ival) {
        return Float.valueOf(getConfigSSP(itrig,stub,ival));
    }

    public int getIntConfigSSP(int itrig,String stub,int ival) {
        return Integer.valueOf(getConfigSSP(itrig,stub,ival));
    }

    public boolean getBoolConfigSSP(int itrig,String stub,int ival) {
        return Boolean.valueOf(getConfigSSP(itrig,stub,ival));
    }

    public String getConfigSSP(int itrig,String stub,int ival) {
    	String key="SSP_HPS_"+stub+"_"+itrig;
    	return getConfig(key,ival);
    }
    
    public String getConfig(String key, int ival) {
        if (configMap.containsKey(key)) {
        	List<String> vals=configMap.get(key);
        	if (ival<vals.size()) {
            	return configMap.get(key).get(ival);
        	} else {
        		System.err.println("configMap too short:  "+ival+configMap.get(key));
        		return null;
        	}
        } else {
        	System.err.println("configMap missing key:  "+key);
        	return null;
        }
    }
}
