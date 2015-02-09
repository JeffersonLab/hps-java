package org.hps.readout.ecal.triggerbank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerConfig {

	private boolean debug=true;

	// These are the configs we need to know for trigger studies:
	
	// General Clustering Cut Values:
	private int clusterMinSeedEnergy=0;
	private int clusterMinHitTimeDiff=0;
	private int clusterMaxHitTimeDiff=0;
	
	// Triggers Enabled:
	private boolean[] singlesEn={false,false};
	private boolean[] pairsEn={false,false};

	// Singles Cuts Enabled:
	private boolean[] singlesNHitsEn={false,false};
	private boolean[] singlesEnergyEn={false,false};
	
	// Pairs Cuts Enabled:
	private boolean[] pairsEnergySumMinEn={false,false};
	private boolean[] pairsEnergySumMaxEn={false,false};
	private boolean[] pairsEnergyDiffEn={false,false};
	private boolean[] pairsCoplanarityEn={false,false};
	private boolean[] pairsEnergyDistEn={false,false};
	private boolean[] pairsTimeDiffEn={false,false};
	
	// Singles Cut Values:
	private int[] singlesNhits={0,0};
	private int[] singlesEnergyMin={0,0};
	private int[] singlesEnergyMax={0,0};
	
	// Pairs Cut Values:
	private int[] pairsEnergySumMin={0,0};
	private int[] pairsEnergySumMax={0,0};
	private int[] pairsEnergyDiffMax={0,0};
	private int[] pairsCoplanarityMax={0,0};
	private int[] pairsTimeDiffMax={0,0};
	private int[] pairsEnergyDistMin={0,0};
	
	// Pairs Cut Parameters:
	private float[] pairsEnergyDistSlope={0,0};
   
   
      
	// Dump everything read from the DAQ Configuration Bank, minimal interpretation:
	public Map<String,List<String>> configMap=new HashMap<String,List<String>>();
	
    
    public TriggerConfig(int runNumber,String[] dump) {

    	loadConfigMap(dump);
        
    	if (runNumber < 3200) {
        	// dne
        } else if (runNumber < 3470 ) {
        	parseConfigMap2014Run(runNumber);
        } else {
            parseConfigMap();
        }
        if (debug) print();
    }
  
    public void parseConfigMap2014Run(int runNumber) {
     	// TODO: port datacat/python/engrun/engrun_metadata.py
    	for (String key : configMap.keySet()) {
    	}
    }
    
    public void parseConfigMap()
    {
    	
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
                
                // This should happen, but is only useful for FADC thresholds/pedestals:
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
    
    public void print() {
        System.out.print("TriggerConfig:  ");
        for (String key : configMap.keySet()) {
            System.out.printf("%s ",key);
            for (String val : configMap.get(key)) {
                System.out.printf("%s ",val);
            }
            System.out.printf("\n");
        }
    }
    
    
/*
    // Should parse the map first to check for errors.
 
    public boolean getPairsEnergyDistEn(int itrig) {
    	return Boolean.valueOf(configMap.get("SSP_HPS_ENERGYDIST_"+itrig).get(0));
    }
    public double getPairsEnergyDistSlope(int itrig) {
        return Double.valueOf(configMap.get("SSP_HPS_ENERGYDIST_"+itrig).get(1));
    }
    public int getPairsEnergyDistCut(int itrig) {
        return Integer.valueOf(configMap.get("SSP_HPS_ENERGYDIST_"+itrig).get(2));
    }
    
    public boolean getPairsEnergySumMinEn(int itrig) {
    	return Boolean.valueOf(configMap.get("SSP_HPS_EMIN_"+itrig).get(0));
    }
    public int getPairsEnergySumMin(int itrig) {
    	return Integer.valueOf(configMap.get("SSP_HPS_EMIN_"+itrig).get(1));
    }

    public boolean getPairsEnergySumMaxEn(int itrig) {
    	return Boolean.valueOf(configMap.get("SSP_HPS_EMAX_"+itrig).get(0));
    }
    public int getPairsEnergySumMax(int itrig) {
    	return Integer.valueOf(configMap.get("SSP_HPS_EMAX_"+itrig).get(1));
    }
    
    public boolean getPairsEnergyDiffEn(int itrig) {
    	return Boolean.valueOf(configMap.get("SSP_HPS_DIFFMAX_"+itrig).get(0));
    }
    public int getPairsEnergyDiffMax(int itrig) {
    	return Integer.valueOf(configMap.get("SSP_HPS_DIFFMAX_"+itrig).get(1));
    }

    public boolean getPairsCoplanarityEn(int itrig) {
    	return Boolean.valueOf(configMap.get("SSP_HPS_COPLANARITY_"+itrig).get(0));
    }
    public int getPairsCoplanarity(int itrig) {
    	return Integer.valueOf(configMap.get("SSP_HPS_COPLANARITY_"+itrig).get(1));
    }
*/
}