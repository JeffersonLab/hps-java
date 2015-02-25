package org.hps.readout.ecal.triggerbank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;

public class TriggerConfig {

    /*
     * Read/Parse/Save the DAQ trigger configuration settings.
     *
     * A hack in progress.
     * 
     * Activate by uncommenting line 61 in LCSimEngRunEventBuilder.java
     *
     * TODO: Error in EVIO format for Crate 39 for 2014 data requires another JEVIO workaround (realized Feb 16).
     * TODO: Manually put in GTP settings based on run number for 2014 data.
     * TODO: Manually deal with change in format of SSP_HPS_SINGLES_NMIN (at 3312(?)).
     *
     * TODO: This should probably be a Driver.
     *
     * TODO: Restructure, clean up..
     *  
     *  NAB 2015/02/16
     */
   
    public static final int BANK_TAG = 0xE10E;
  
    // need to know these in order to interpret DAQ strings:
    private static final int[] singlesIOsrc={20,21};
    private static final int[] pairsIOsrc={22,23};
  
	// Dump everything read from the DAQ Configuration Bank, minimal interpretation:
	public Map<String,List<String>> configMap=new HashMap<String,List<String>>();
  
	// link ECAL FADC channel settings to EcalChannels:
	public Map<EcalChannel,Float> GAIN=new HashMap<EcalChannel,Float>();
	public Map<EcalChannel,Float> PEDESTAL=new HashMap<EcalChannel,Float>();
	public Map<EcalChannel,Integer> THRESHOLD=new HashMap<EcalChannel,Integer>();
	
	private boolean debug=false;//true;

	// FADC Config:
	public int fadcNSA=0;
	public int fadcNSB=0;
	public int fadcNPEAK=0;
	public int fadcMODE=0;
	public int fadcWIDTH=0;
	public int fadcOFFSET=0;

	// GTP Clustering Cut Values:
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
	public int[] pairsEnergyMin={0,0};
	public int[] pairsEnergyMax={0,0};
	public int[] pairsEnergySumMin={0,0};
	public int[] pairsEnergySumMax={0,0};
	public int[] pairsEnergyDiffMax={0,0};
	public int[] pairsCoplanarityMax={0,0};
	public int[] pairsTimeDiffMax={0,0};
	public int[] pairsEnergyDistMin={0,0};
	
	// Pairs Cut Parameters:
	public float[] pairsEnergyDistSlope={0,0};

	// Have to remember the previous slot line in order to interpret the data:
	private int thisFadcSlot=0;

	// Cache local set of EcalChannels:
    private EcalConditions ecalConditions = null;
    private List<EcalChannel> channels=new ArrayList<EcalChannel>();
    
    public TriggerConfig() {
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        for (int ii = 0; ii < 442; ii++) {
            channels.add(findChannel(ii+1));
        } 
	}
    
    public void parse(int crate,int runNumber,String[] dump) {
        loadConfigMap(crate,dump); 
    	if (debug) printMap();
        fixConfigMap2014Run(runNumber);
        parseConfigMap();
        // don't do this here, need to wait on more banks:
        //if (debug) printVars();
    }

    public void parseConfigMap()
    {
//        System.err.println("PARSECONFIGMAP ..................");
       
        fadcNSA=Integer.valueOf(getConfig("FADC250_NSA",0));
        fadcNSB=Integer.valueOf(getConfig("FADC250_NSB",0));
        fadcNPEAK=Integer.valueOf(getConfig("FADC250_NPEAK",0));
        fadcMODE=Integer.valueOf(getConfig("FADC250_MODE",0));
        fadcWIDTH=Integer.valueOf(getConfig("FADC250_W_WIDTH",0));
        fadcOFFSET=Integer.valueOf(getConfig("FADC250_W_OFFSET",0));
        
        clusterMinSeedEnergy=Integer.valueOf(getConfig("GTP_CLUSTER_THRESH",0));
        clusterMinHitTimeDiff=Integer.valueOf(getConfig("GTP_TIMEDIFF",0));
        clusterMaxHitTimeDiff=Integer.valueOf(getConfig("GTP_TIMEDIFF",1));
       
        for (int ii=0; ii<2; ii++) {
            //singlesEn[ii]=getBoolConfigSSP(ii,"SINGLES_EN",0);
            //pairsEn[ii]=getBoolConfigSSP(ii,"PAIRS_EN",0);
            
            singlesNhitsEn[ii]=getBoolConfigSSP(ii,"SINGLES_NMIN",1);
            singlesEnergyMinEn[ii]=getBoolConfigSSP(ii,"SINGLES_EMIN",1);
            singlesEnergyMaxEn[ii]=getBoolConfigSSP(ii,"SINGLES_EMAX",1);

            pairsEnergySumMinEn[ii]=getBoolConfigSSP(ii,"PAIRS_SUMMAX_MIN",2);
            pairsEnergySumMaxEn[ii]=getBoolConfigSSP(ii,"PAIRS_SUMMAX_MIN",2);
            pairsEnergyDiffEn[ii]=getBoolConfigSSP(ii,"PAIRS_DIFFMAX",1);
            pairsCoplanarityEn[ii]=getBoolConfigSSP(ii,"PAIRS_COPLANARITY",1);
            pairsEnergyDistEn[ii]=getBoolConfigSSP(ii,"PAIRS_ENERGYDIST",1);
            //pairsTimeDiffEn[ii]=getBoolConfigSSP(ii,"PAIRS_TIMECOINCIDENCE",0);
            //pairsEnergyMin[ii]=getIntConfigSSP(ii,"PAIRS_EMIN",0);
            //pairsEnergyMax[ii]=getIntConfigSSP(ii,"PAIRS_EMAX",0);

            singlesNhits[ii]=getIntConfigSSP(ii,"SINGLES_NMIN",0);
            singlesEnergyMin[ii]=getIntConfigSSP(ii,"SINGLES_EMIN",0);
            singlesEnergyMax[ii]=getIntConfigSSP(ii,"SINGLES_EMAX",0);

            pairsEnergyMin[ii]=getIntConfigSSP(ii,"PAIRS_EMIN",0);
            pairsEnergyMax[ii]=getIntConfigSSP(ii,"PAIRS_EMAX",0);
            pairsEnergySumMin[ii]=getIntConfigSSP(ii,"PAIRS_SUMMAX_MIN",1);
            pairsEnergySumMax[ii]=getIntConfigSSP(ii,"PAIRS_SUMMAX_MIN",0);
            pairsEnergyDiffMax[ii]=getIntConfigSSP(ii,"PAIRS_DIFFMAX",0);
            pairsCoplanarityMax[ii]=getIntConfigSSP(ii,"PAIRS_COPLANARITY",0);
            pairsTimeDiffMax[ii]=getIntConfigSSP(ii,"PAIRS_TIMECOINCIDENCE",0);
            pairsEnergyDistSlope[ii]=getFloatConfigSSP(ii,"PAIRS_ENERGYDIST",0);
            pairsEnergyDistMin[ii]=getIntConfigSSP(ii,"PAIRS_ENERGYDIST",1);
        }
//        System.err.println("DONE PARSECONFIGMAP.");
    }
    
   
      
    private void fixConfigMap2014Run(int runNumber) {
        if (runNumber>3470 || runNumber < 3100) return;
     	// TODO: port datacat/python/engrun/engrun_metadata.py
        // 1. SET GTP SETTINGS MANUALLY BASED ON RUN NUMBER
        // 2. FIX SINGLES_NMIN prior to 3312
    	for (String key : configMap.keySet()) {
    	}
    	List<String> tmp=new ArrayList<String>();
    	tmp.add("0");
    	tmp.add("0");
    	tmp.add("0");
    	tmp.add("0");
    	configMap.put("GTP_CLUSTER_THRESH",tmp);
    	configMap.put("GTP_TIMEDIFF",tmp);
    }
    
    private void parseFADC(int crate,String key,List<String> vals)
    {
//        System.err.println(crate);
        if (key.equals("FADC250_SLOT")) {
            thisFadcSlot=Integer.valueOf(vals.get(0));
        }
        else if (key.equals("FADC250_ALLCH_TET")) {
            setChannelParsInt(crate,thisFadcSlot,THRESHOLD,vals);
        }
        else if (key.equals("FADC250_ALLCH_PED")) {
            setChannelParsFloat(crate,thisFadcSlot,PEDESTAL,vals);    
        }
        else if (key.equals("FADC250_ALLCH_GAIN")) {
            setChannelParsFloat(crate,thisFadcSlot,GAIN,vals);
        }
    }
   
    private void setChannelParsFloat(int crate,int slot,Map<EcalChannel,Float>map, List<String> vals)
    {
        for (int ii=0; ii<16; ii++) {
            map.put(findChannel(crate,slot,ii),Float.valueOf(vals.get(ii)));
        }
    }
    private void setChannelParsInt(int crate,int slot,Map<EcalChannel,Integer>map, List<String> vals)
    {
        for (int ii=0; ii<16; ii++) {
            map.put(findChannel(crate,slot,ii),Integer.valueOf(vals.get(ii)));
        }
    }
    
    private void loadConfigMap(int crate,String[] dump) {
  
        for (String dump1 : dump) {
            for (String line : dump1.trim().split("\n")) {

                String[] cols=line.trim().split(" +",2);
                if (cols.length < 2) continue;

                String key=cols[0];
                List<String> vals=new ArrayList<String>
                    (Arrays.asList(cols[1].trim().split(" +")));

                if (vals.size() < 1) {
                	continue;
                }
                
                if (key.startsWith("FADC250")) {
                    parseFADC(crate,key.trim(),vals);
                }
                else if (key.startsWith("SSP_HPS_SET_IO_SRC")) {
                    for (int ii=0; ii<pairsIOsrc.length; ii++)
                    {
                        int trig = Integer.valueOf(vals.get(1));
                        if (trig == singlesIOsrc[ii]) {
                            singlesEn[ii]=true;
                        }
                        else if (trig == pairsIOsrc[ii]) {
                            pairsEn[ii]=true;
                        }
                    }
                }
                
                // Append trigger# onto key:
                if (vals.size() > 1 && key.startsWith("SSP"))
                {
                    key += "_"+vals.remove(0);
                }
               
                // dump it into the map:
                configMap.put(key,vals);
            }
        }
    }
    
    public void printMap() {
        System.err.print("\nTriggerConfigMap::::::::::::::::::::::::::::\n");
        for (String key : configMap.keySet()) {
            System.err.printf("%s: ",key);
            for (String val : configMap.get(key)) {
                System.err.printf("%s ",val);
            }
            System.err.printf("\n");
        }
        System.err.println("::::::::::::::::::::::::::::::::::::::::::::");
    }

    public void printVars()
	{
        System.err.println("\nTriggerConfigVars%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.err.println();
	    System.err.println(String.format("GTPMINSEED: %d",clusterMinSeedEnergy));
	    System.err.println(String.format("GTPMINHITDT: %d",clusterMinHitTimeDiff));
	    System.err.println(String.format("GTPMAXHITDT: %d",clusterMaxHitTimeDiff));
	    System.err.println();
	    System.err.println(String.format("FADC250_NSA: %d",fadcNSA));
	    System.err.println(String.format("FADC250_NSB: %d",fadcNSB));
	    System.err.println(String.format("FADC250_NPEAK: %d",fadcNPEAK));
	    System.err.println(String.format("FADC250_MODE: %d",fadcMODE));
	    System.err.println(String.format("FADC250_WIDTH: %d",fadcWIDTH));
	    System.err.println(String.format("FADC250_OFFSET: %d",fadcOFFSET));
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            System.err.print(String.format("SLOT%d CHAN%d --",cc.getSlot(),cc.getChannel()));
            if (!PEDESTAL.containsKey(cc)) {
                System.err.println("\nP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            if (!THRESHOLD.containsKey(cc)) {
                System.err.println("\nT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            if (!GAIN.containsKey(cc)) {
                System.err.println("\nG !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            System.err.println(String.format(" %f %d %f",
                    PEDESTAL.get(cc),THRESHOLD.get(cc),GAIN.get(cc)));
        }
	    System.err.println();
	    for (int ii=0; ii<2; ii++)
	    {
	        System.err.println(String.format("SINGLESEN-%d: %b ",ii,singlesEn[ii]));
	        System.err.println(String.format("PAIRSEN-%d: %b: ",ii,pairsEn[ii]));
	        
	        System.err.println(String.format("SINGLESNHITSEN %d %b:  ",ii,singlesNhitsEn[ii]));
	        System.err.println(String.format("SINGLESEMINEN %d %b",ii,singlesEnergyMinEn[ii]));
	        System.err.println(String.format("SINGLESEMAXEN %d %b",ii,singlesEnergyMaxEn[ii]));
	        
	        System.err.println(String.format("PAIRSSUMMINEN %d %b",ii,pairsEnergySumMinEn[ii]));
	        System.err.println(String.format("PAIRSSUMMAXEN %d %b",ii,pairsEnergySumMaxEn[ii]));
	        System.err.println(String.format("PAIRSENERGYDIFFEN %d %b",ii,pairsEnergyDiffEn[ii]));
	        System.err.println(String.format("PAIRSCOPEN %d %b",ii,pairsCoplanarityEn[ii]));
	        System.err.println(String.format("PAIRSEDISTEN %d %b",ii,pairsEnergyDistEn[ii]));
	        System.err.println(String.format("PAIRSTIMEDIFFEN %d %b",ii,pairsTimeDiffEn[ii]));
	        
	        System.err.println(String.format("SINGLESNHTIS %d %d",ii,singlesNhits[ii]));
	        System.err.println(String.format("SINGLESEMIN %d %d",ii,singlesEnergyMin[ii]));
	        System.err.println(String.format("SINGLESEMAX %d %d",ii,singlesEnergyMax[ii]));
	        
	        System.err.println(String.format("PAIRSSUMMIN %d %d",ii,pairsEnergySumMin[ii]));
	        System.err.println(String.format("PRISSUMMAX %d %d",ii,pairsEnergySumMax[ii]));
	        System.err.println(String.format("PAIRSENERGYDIFF %d %d",ii,pairsEnergyDiffMax[ii]));
	        System.err.println(String.format("PAIRSCOPMAX %d %d",ii,pairsCoplanarityMax[ii]));
	        System.err.println(String.format("PAIRSTDIFFMAAX %d %d",ii,pairsTimeDiffMax[ii]));
	        System.err.println(String.format("PAIRSEDISTMIN %d %d",ii,pairsEnergyDistMin[ii]));
	        System.err.println(String.format("PAIRSEDISTSLOP %d %f",ii,pairsEnergyDistSlope[ii]));
	    }
	    System.err.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}
  
    public float getFloatConfigSSP(int itrig,String stub,int ival) {
        return Float.valueOf(getConfigSSP(itrig,stub,ival));
    }

    public int getIntConfigSSP(int itrig,String stub,int ival) {
        return Integer.valueOf(getConfigSSP(itrig,stub,ival));
    }

    public boolean getBoolConfigSSP(int itrig,String stub,int ival) {
        return "1".equals(getConfigSSP(itrig,stub,ival));
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
        		return "0";
        	}
        } else {
            // this is not an error, we have to wait on 3 banks.
            // leave here for now.
        	System.err.println("configMap missing key:  "+key);
        	return "0";
        }
    }

    public EcalChannel findChannel(int crate,int fadcSlot,int fadcChan)
    {
        for (EcalChannel cc : channels) {
            // EcalChannel follows different convention on crate numbering:
            if ((cc.getCrate()-1)*2 == crate-37 && 
                cc.getSlot() == fadcSlot && cc.getChannel() == fadcChan)
            {
                return cc;
            }
        }
        return null;
    }
    public EcalChannel findChannel(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }
} 