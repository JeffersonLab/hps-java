package org.hps.readout.ecal.triggerbank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerConfig {

    public Map<String,List<String>> configMap=new HashMap<String,List<String>>();
    
    public TriggerConfig(int runNumber,String[] dump) {
        loadConfigMap(runNumber,dump);
        print();
    }
    
    private void loadConfigMap(int runNumber,String[] dump) {

        // have to do some corrections for certain run number ranges
       
        System.err.println("loadConfigMap:  "+dump.length);
        
        for (String dump1 : dump) {
            for (String line : dump1.split("\n")) {

                System.err.println("LLAMAS:  "+line);
                
                String[] cols=line.trim().split(" +",2);
                if (cols.length < 2) continue;

                String key=cols[0];
                List<String> vals=new ArrayList<String>
                    (Arrays.asList(cols[1].trim().split(" +")));

                if (vals.size() < 1) continue;
               
                if (vals.size() > 4) {
                    // put threshold trick here
                    continue;
                }
               
                // deal with error on SSP_HPS_SINGLES_NMIN
                
                if (vals.size() > 1)
                {
                    System.err.println("A PROPOISE:  "+key+"-"+vals);
                    key += "_"+vals.remove(0);
                    System.err.println("B PORPOISE:  "+key+"-"+vals);
                }

                List<String> svals=new ArrayList<String>();
                for (String val : vals) {
                    System.err.println("DOGGEIES:  "+key+" "+val.length()+"  **"+val+"**");
                    svals.add(val);
                    System.err.println("KITTIES");
                }

                configMap.put(key,svals);
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
    
    public Double getEnergySlope(int itrig) {
        return Double.valueOf(configMap.get("SSP_HPS_ENERGYDIST_"+itrig).get(1));
    }
}