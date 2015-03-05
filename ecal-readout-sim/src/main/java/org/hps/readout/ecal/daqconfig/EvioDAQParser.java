package org.hps.readout.ecal.daqconfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;

public class EvioDAQParser {

    /*
     * Read/Parse/Save the DAQ trigger configuration settings.
     * These settings arrive in multiple banks, but they *should* be in the same event.
     *
     * Currently this is set up to read SSP and ECAL configurations,
     * which is all that is currently available in EVIO as of Feb 28, 2015.
     * 
     * GTP settings and Prescale factors will need to be added to this class when added to EVIO.
     *
     * TODO: Error in EVIO format for Crate 39 for 2014 data requires another JEVIO workaround (realized Feb 16).
     *       ** This was fixed in EVIO for data after run 4044.
     *       
     * TODO: Manually put in GTP settings based on run number for 2014 data.
     * TODO: Manually deal with change in format of SSP_HPS_SINGLES_NMIN (at 3312(?)).
     *
     * TODO: Restructure, clean up..
     *  
     *  @author <baltzell@jlab.org>
     */
  
    public int nBanks = 0;
    
    public static final int BANK_TAG = 0xE10E;
  
    // need to know these in order to interpret DAQ strings:
    private static final int[] singlesIOsrc = { 20, 21 };
    private static final int[] pairsIOsrc = { 22, 23 };
  
	// Dump everything read from the DAQ Configuration Bank, minimal interpretation:
	public Map<String,List<String>> configMap = new HashMap<String,List<String>>();
  
	// link ECAL FADC channel settings to EcalChannels:
	Map<EcalChannel,Float> GAIN = new HashMap<EcalChannel,Float>();
	Map<EcalChannel,Float> PEDESTAL = new HashMap<EcalChannel,Float>();
	Map<EcalChannel,Integer> THRESHOLD = new HashMap<EcalChannel,Integer>();
	
//	private boolean debug = true;
	private boolean debug = false;

	// FADC Config:
	int fadcNSA    = 0;
	int fadcNSB    = 0;
	int fadcNPEAK  = 0;
	int fadcMODE   = 0;
	int fadcWIDTH  = 0;
	int fadcOFFSET = 0;

	// GTP Clustering Cut Values:
	int clusterMinSeedEnergy  = 0;
	int clusterMinHitTimeDiff = 0;
	int clusterMaxHitTimeDiff = 0;
	
	// Triggers Enabled:
	boolean[] singlesEn = { false, false };
	boolean[] pairsEn   = { false, false };

	// Singles Cuts Enabled:
	boolean[] singlesNhitsEn     = { false, false };
	boolean[] singlesEnergyMinEn = { false, false };
	boolean[] singlesEnergyMaxEn = { false, false };
	
	// Pairs Cuts Enabled:
	boolean[] pairsEnergySumMaxMinEn = { false, false };
	boolean[] pairsEnergyDiffEn      = { false, false };
	boolean[] pairsCoplanarityEn     = { false, false };
	boolean[] pairsEnergyDistEn      = { false, false };
	
	// Singles Cut Values:
	int[] singlesNhits     = { 0, 0 };
	int[] singlesEnergyMin = { 0, 0 };
	int[] singlesEnergyMax = { 0, 0 };
	
	// Pairs Cut Values:
	int[] pairsNhitsMin       = { 0, 0 };
	int[] pairsEnergyMin      = { 0, 0 };
	int[] pairsEnergyMax      = { 0, 0 };
	int[] pairsEnergySumMin   = { 0, 0 };
	int[] pairsEnergySumMax   = { 0, 0 };
	int[] pairsEnergyDiffMax  = { 0, 0 };
	int[] pairsCoplanarityMax = { 0, 0 };
	int[] pairsTimeDiffMax    = { 0, 0 };
	int[] pairsEnergyDistMin  = { 0, 0 };
	
	// Pairs Cut Parameters:
	float[] pairsEnergyDistSlope = { 0, 0 };

	// Have to remember the previous slot line in order to interpret the data:
	private int thisFadcSlot = 0;

	// Cache local set of EcalChannels:
    private EcalConditions ecalConditions = null;
    private List<EcalChannel> channels = new ArrayList<EcalChannel>();
    
    public EvioDAQParser() {
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        for (int ii = 0; ii < 442; ii++) {
            channels.add(findChannel(ii+1));
        } 
	}
    
    public void parse(int crate,int runNumber,String[] dump) {
        
        nBanks++;
        
        loadConfigMap(crate,dump); 
    	if (debug) printMap();
        fixConfigMap2014Run(runNumber);
        parseConfigMap();
        
        if (nBanks>2 && debug) printVars();
    }

    /*
     * The first parsing routine.  Just dumps the config strings
     * into a map whose keys are the first column in the config file.
     * Also treats some special cases.
     */
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
               
                // SPECIAL CASE:
                // parse the 16+1 column slot configurations. 
                if (key.startsWith("FADC250")) {
                    parseFADC(crate,key.trim(),vals);
                }
                
                // SPECIAL CASE:
                // figure out which triggers are enabled:
                else if (key.startsWith("SSP_HPS_SET_IO_SRC")) {
                    int trig = Integer.valueOf(vals.get(1));
                    for (int ii=0; ii<pairsIOsrc.length; ii++)
                    {
                        if (trig == singlesIOsrc[ii]) {
                            singlesEn[ii]=true;
                        }
                        else if (trig == pairsIOsrc[ii]) {
                            pairsEn[ii]=true;
                        }
                    }
                }
               
                // GENERAL CASE:
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
    /*
     * This function parses the config map for the cases where the
     * config string has a simple format:
     * TAG VALUE
     * TAG TRIGGER VALUES
     */
    public void parseConfigMap()
    {
//        System.out.println("PARSECONFIGMAP ..................");
       
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
            
            singlesNhitsEn[ii]=getBoolConfigSSP(ii,"SINGLES_NMIN",1);
            singlesEnergyMinEn[ii]=getBoolConfigSSP(ii,"SINGLES_EMIN",1);
            singlesEnergyMaxEn[ii]=getBoolConfigSSP(ii,"SINGLES_EMAX",1);

            pairsEnergySumMaxMinEn[ii]=getBoolConfigSSP(ii,"PAIRS_SUMMAX_MIN",2);
            pairsEnergyDiffEn[ii]=getBoolConfigSSP(ii,"PAIRS_DIFFMAX",1);
            pairsCoplanarityEn[ii]=getBoolConfigSSP(ii,"PAIRS_COPLANARITY",1);
            pairsEnergyDistEn[ii]=getBoolConfigSSP(ii,"PAIRS_ENERGYDIST",1);

            singlesNhits[ii]=getIntConfigSSP(ii,"SINGLES_NMIN",0);
            singlesEnergyMin[ii]=getIntConfigSSP(ii,"SINGLES_EMIN",0);
            singlesEnergyMax[ii]=getIntConfigSSP(ii,"SINGLES_EMAX",0);

            pairsNhitsMin[ii]=getIntConfigSSP(ii,"PAIRS_NMIN",0);
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
//        System.out.println("DONE PARSECONFIGMAP.");
    }
    
   
     
    /*
     * UNFINISHED.
     * This is a fixer-upper for before we had the full config in EVIO
     * or when there was a bug in it.
     */
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
    
    
    
    /*
     * These treat the FADC config lines with 16+1 columns.
     * Must keep track of most recent FADC250_SLOT tag, since it's
     * not on the line with the data. 
     */
    private void parseFADC(int crate,String key,List<String> vals)
    {
//        System.out.println(crate);
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
   
    
    
    
    public void printMap() {
        System.out.print("\nTriggerConfigMap::::::::::::::::::::::::::::\n");
        for (String key : configMap.keySet()) {
            System.out.printf("%s: ",key);
            for (String val : configMap.get(key)) {
                System.out.printf("%s ",val);
            }
            System.out.printf("\n");
        }
        System.out.println("::::::::::::::::::::::::::::::::::::::::::::");
    }

    public void printVars()
	{
        System.out.println("\nTriggerConfigVars%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.println();
	    System.out.println(String.format("GTPMINSEED: %d",clusterMinSeedEnergy));
	    System.out.println(String.format("GTPMINHITDT: %d",clusterMinHitTimeDiff));
	    System.out.println(String.format("GTPMAXHITDT: %d",clusterMaxHitTimeDiff));
	    System.out.println();
	    System.out.println(String.format("FADC250_NSA: %d",fadcNSA));
	    System.out.println(String.format("FADC250_NSB: %d",fadcNSB));
	    System.out.println(String.format("FADC250_NPEAK: %d",fadcNPEAK));
	    System.out.println(String.format("FADC250_MODE: %d",fadcMODE));
	    System.out.println(String.format("FADC250_WIDTH: %d",fadcWIDTH));
	    System.out.println(String.format("FADC250_OFFSET: %d",fadcOFFSET));
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            //System.out.print(String.format("SLOT%d CHAN%d --",cc.getSlot(),cc.getChannel()));
            if (!PEDESTAL.containsKey(cc)) {
                System.out.println("\nP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            if (!THRESHOLD.containsKey(cc)) {
                System.out.println("\nT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            if (!GAIN.containsKey(cc)) {
                System.out.println("\nG !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            //System.out.println(String.format(" %f %d %f",
            //        PEDESTAL.get(cc),THRESHOLD.get(cc),GAIN.get(cc)));
        }
	    System.out.println();
	    for (int ii=0; ii<2; ii++)
	    {
	        System.out.println(String.format("SINGLES_EN %d %b ",ii,singlesEn[ii]));
	        System.out.println(String.format("PAIRS_EN %d %b ",ii,pairsEn[ii]));
	        
	        System.out.println(String.format("SINGLES_NHITS_EN %d %b:  ",ii,singlesNhitsEn[ii]));
	        System.out.println(String.format("SINGLES_EMIN_EN %d %b",ii,singlesEnergyMinEn[ii]));
	        System.out.println(String.format("SINGLES_EMAX_EN %d %b",ii,singlesEnergyMaxEn[ii]));
	        
	        System.out.println(String.format("PAIRS_SUMMAXMIN_EN %d %b",ii,pairsEnergySumMaxMinEn[ii]));
	        System.out.println(String.format("PAIRS_ENERGYDIFF_EN %d %b",ii,pairsEnergyDiffEn[ii]));
	        System.out.println(String.format("PAIRS_COP_EN %d %b",ii,pairsCoplanarityEn[ii]));
	        System.out.println(String.format("PAIRS_EDIST_EN %d %b",ii,pairsEnergyDistEn[ii]));
	        
	        System.out.println(String.format("SINGLES_NHTIS %d %d",ii,singlesNhits[ii]));
	        System.out.println(String.format("SINGLES_EMIN %d %d",ii,singlesEnergyMin[ii]));
	        System.out.println(String.format("SINGLES_EMAX %d %d",ii,singlesEnergyMax[ii]));
	        
	        System.out.println(String.format("PAIRS_NHITS %d %d",ii,pairsNhitsMin[ii]));
	        System.out.println(String.format("PAIRS_SUMMIN %d %d",ii,pairsEnergySumMin[ii]));
	        System.out.println(String.format("PAIRS_SUMMAX %d %d",ii,pairsEnergySumMax[ii]));
	        System.out.println(String.format("PAIRS_ENERGYDIFF %d %d",ii,pairsEnergyDiffMax[ii]));
	        System.out.println(String.format("PAIRS_COPMAX %d %d",ii,pairsCoplanarityMax[ii]));
	        System.out.println(String.format("PAIRS_TDIFFMAAX %d %d",ii,pairsTimeDiffMax[ii]));
	        System.out.println(String.format("PAIRS_EDISTMIN %d %d",ii,pairsEnergyDistMin[ii]));
	        System.out.println(String.format("PAIRS_EDISTSLOP %d %f",ii,pairsEnergyDistSlope[ii]));
	    }
	    System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}
  
    
    
    /*
     * Parsing wrappers to make rest of code easier.
     */
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
        	    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
        	            "ConfigMap TOO SHORT:   "+ival+" "+configMap.get(key));
        		return "0";
        	}
        } else {
            // this is only necessarily an error if we've read 3 banks:
            if (nBanks>2) {
            	Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
        	         "ConfigMap MISSING KEY:   "+key);
            }
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