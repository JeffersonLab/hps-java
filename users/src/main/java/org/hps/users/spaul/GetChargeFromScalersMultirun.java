package org.hps.users.spaul;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
/**
 * sums up the charge from multiple runs.
 */
public class GetChargeFromScalersMultirun {
    private static ArrayList<String> runs;
    private static ArrayList<Date> starts;
    private static ArrayList<Date> ends;

    public static void main(String[] arg) throws FileNotFoundException, ParseException{
        String inputFile = arg[0];
        String timingInfoFile = arg[1];
        String outputFile = arg[2];
        
        
        readTimingInfoFile(timingInfoFile);
        
        Map map = getCharges(runs, starts, ends, inputFile);
        mergeBiasIntervals(map);
        
        ArrayList<String> keys = new ArrayList(map.keySet());
        Collections.sort(keys);
        
        
        PrintWriter pw = new PrintWriter(new File(outputFile));
        for(String s : keys){
            pw.println(s + "\t" + map.get(s));
            System.out.println(s + "\t" + map.get(s));
        }
        pw.close();
    }

    static void readTimingInfoFile(String s) throws FileNotFoundException, ParseException{
        Scanner scanner = new Scanner(new File(s));
        scanner.useDelimiter("[\n\t]");
        runs = new ArrayList();
        starts = new ArrayList();
        ends = new ArrayList();
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");
        
        while(scanner.hasNext()){
            runs.add(scanner.next());
            String n = scanner.next();
            if(n.matches("\\d+"))
                starts.add(new Date(Long.parseLong(n)));
            else 
                starts.add(df.parse(n));
            n = scanner.next();
            if(n.matches("\\d+"))
                ends.add(new Date(Long.parseLong(n)));
            else 
                ends.add(df.parse(n));
            
        }
        scanner.close();
    }
    
    
    
    
    
    /**
     * returns charge in microCoulombs
     * @param runs names of the runs   
     * @param starts starting times of the runs
     * @param ends ending times of the runs
     * @param file the mya file that we need to use
     * @return a map relating the runs to the charges (uC).  
     * @throws FileNotFoundException
     */
    
    static Map<String, Double> getCharges(ArrayList<String> runs, ArrayList<Date> starts, ArrayList<Date> ends, String file) throws FileNotFoundException{
        Scanner s = new Scanner(new File(file));
        HashMap<String, Double> map = new HashMap();
        long prev = 0;
        long time = 0;
        for(int i = 0; i< runs.size(); i++){
            long endt = ends.get(i).getTime();
            long startt = starts.get(i).getTime();
            
            double charge = 0;
            boolean started = false;
            double prevval = 0;
            if(time > endt){
                s.close();
                s = new Scanner(new File(file));
            }
            inner : while(s.hasNext()){
                String var = s.next();
                prev = time;
                time = s.nextLong()*1000; //convert from s to ms

                double val = s.nextDouble();
                if(!var.equals("scaler_calc1"))
                    continue;
                
                if(!started && time> startt){ //first sample in the run
                    charge += (val)/2.*(time-startt);
                    started= true;
                }
                
                else if(time > startt && endt > time){ //middle samples in the run
                    charge += (val/*+prevval*/)/*/2.*/*(time-prev);
                    
                }
                
                if(endt < time){ //last sample that is in the run
                    charge += (/*prev*/val)/2.*(endt-prev);
                    break inner;
                }
                prevval = val;
            }
            charge/=1e6;
            map.put(runs.get(i), charge);
        }
        s.close();
        return map;

    }
    
    /**
     * If the subsections of the runs in which the bias is on are labeled according to a scheme,
    they will be added together.  
    for instance, 5779a, 5779b, 5779c, etc. will be added up as 5779bias.   
     * @param map the map of run names (and portions of runs that have bias labeled as [run number][a,b,c,d...],
     * corresponding to the total charge in that run (or piece of a run).  
     */
    static void mergeBiasIntervals(Map<String, Double> map){
        Map<String, Double> map2 = new HashMap();
        for(Map.Entry<String, Double> entry : map.entrySet()){
            String key1 = entry.getKey();
            if(!entry.getKey().matches("\\d+a"))
                    continue;
            double charge = entry.getValue();
            for(Map.Entry<String, Double> entry2 : map.entrySet()){
                if(entry2.getKey().matches(key1.substring(0, 4) + "[b-z]"))
                        charge += entry2.getValue();
            }
            map2.put(key1.substring(0, 4) + "bias", charge);
            
        }
        for(Map.Entry<String, Double> e : map2.entrySet()){
            map.put(e.getKey(), e.getValue());
        }
    }
}
