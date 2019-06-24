package org.hps.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

abstract public class TupleDriverTest extends TestCase {
    protected String testURLBase = null;
            //"http://www.lcsim.org/test/hps-java/";
    protected String txtRefFileName = "ntuple_005772_fee_Ref.txt";
    protected String lcioInputFileName = "hps_005772.0_recon_Rv4657-0-10000.slcio";
    protected String txtOutputFileName = "target/test-output/out_fee.txt";
    protected Driver testTupleDriver = null;
    protected boolean useEventFlagFilter = true;

    public void testIt() throws Exception {

        File lcioInputFile = null;
        if (testURLBase == null) {
            lcioInputFile = new File(lcioInputFileName);
        } else {
            URL lcioURL = new URL(testURLBase + "/" + lcioInputFileName);
            FileCache cache = new FileCache();
            lcioInputFile = cache.getCachedFile(lcioURL);
        }
        
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.addConditionsListener(new SvtDetectorSetup());
        
        loop.add(new org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver());
        if (useEventFlagFilter)
            loop.add(new org.hps.recon.filtering.EventFlagFilter());
        
        org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup rthss = new org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup();
        String[] readoutColl = { "SVTRawTrackerHits" };
        rthss.setReadoutCollections(readoutColl);
        loop.add(rthss);

        loop.add(testTupleDriver);
        
        loop.loop(100);
        loop.dispose();
        
        CompareTextFiles();
    }
    
    private void CompareTextFiles() throws Exception {
        File txtRefFile = null;
        if (testURLBase == null) {
            txtRefFile = new File(txtRefFileName);
        } else {
            URL txtURL = new URL(testURLBase + "/" + txtRefFileName);
            FileCache cache = new FileCache();
            txtRefFile = cache.getCachedFile(txtURL);
        }
        File txtNewFile = new File(txtOutputFileName); 
        
        Map<String, List<String>> refMap = textToMap(txtRefFile); 
        Map<String, List<String>> newMap = textToMap(txtNewFile); 
        
        Set<String> refMapKeys = refMap.keySet();
        for (String branchName : refMapKeys) {
            assertTrue("new text file is missing branch " + branchName, newMap.containsKey(branchName));
            List<String> branchValsRef = refMap.get(branchName);
            List<String> branchValsNew = newMap.get(branchName);
            assertTrue("wrong number of entries in branch " + branchName, branchValsRef.size() == branchValsNew.size());
            for (int i=0; i<branchValsRef.size(); i++) {
                assertTrue("there is a wrong entry value in branch " + branchName, branchValsRef.get(i).equals(branchValsNew.get(i)));
            }
        }
        Set<String> newMapKeys = newMap.keySet();
        for (String branchName : newMapKeys) {
            if (!refMapKeys.contains(branchName))
                System.out.println("Note: you have added branch " + branchName);
        }
        
        assertTrue("new text file is missing branches", newMap.size() >= refMap.size());
    }
    
    private Map<String, List<String>> textToMap(File input) {
        Map<String, List<String>> treeMap = new HashMap<String, List<String>>();
        String[] mapStrings = null;
        Scanner scFile = null;
        String mapString = null;
        
        try {
            scFile = new Scanner(input);
            mapString = scFile.nextLine(); 
            mapStrings = mapString.split(":");
            int numEntries = mapStrings.length;
            int counter=0;
            while (scFile.hasNextDouble()) {
                String addMe = String.format("%f", scFile.nextDouble());
                if (counter < numEntries) {
                    List<String> toAdd = new ArrayList<String>();
                    toAdd.add(addMe);
                    treeMap.put(mapStrings[counter], toAdd);
                }
                else {
                    treeMap.get(mapStrings[counter % numEntries]).add(addMe);
                }
                counter++;
            }

        }
        catch(FileNotFoundException ex) {
            System.out.println("Unable to open txt file");                
        }
        
        scFile.close();
        
        return treeMap;
    }
}
