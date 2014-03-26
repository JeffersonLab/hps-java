/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.phansson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author phansson
 */
public class SimpleHPSConditions {
    
    private class SimpleHPSCondition {
        public double _thickness;
        public double _rate;
        public double _rate_rec;
        public double _int_current;
        public int _start_time;
        public int _stop_time;
        public int _run_nr;

        public SimpleHPSCondition() {}
        
        @Override
        public String toString() {
           return String.format("%d %.1f nC %.2f r.l. %.2f %.2f Hz \n",_run_nr,_int_current,_thickness,_rate,_rate_rec);
        }
    
    }
    
    public List<SimpleHPSCondition> _conditionsList = new ArrayList<SimpleHPSCondition>();
    
    public SimpleHPSConditions() {
     
        this.loadCond();
        
    }
    
    public void loadCond() {
//        System.out.println("Reading beam currents from " + pathToFile);
        InputStreamReader fReader;
        BufferedReader bufReader;
        try {
            InputStream stream = this.getClass().getResourceAsStream("/org/lcsim/hps/steering/runConditions.txt");
            fReader = new InputStreamReader(stream);
            bufReader = new BufferedReader(fReader);

            String line;
            while( (line = bufReader.readLine()) != null) {
                //System.out.println("Line: " + line);
                if(line.contains("run") || line.contains("#")) continue;
                
                String[] vec_line = line.split("\\s+");
                //for(int i=0;i<5;++i) System.out.println(vec_line[i] +" ," );
                String s = vec_line[0];
                //System.out.println("s \"" + s + "\"");
                Integer run = Integer.parseInt(s);
                //System.out.println(run);
                int start = Integer.parseInt(vec_line[1]);
                int stop = Integer.parseInt(vec_line[2]);
                double cur = Double.parseDouble(vec_line[4]);
                double t = Double.parseDouble(vec_line[5]);
                double rate = Double.parseDouble(vec_line[6]);
                double rate_rec = Double.parseDouble(vec_line[7]);

                SimpleHPSCondition c = new SimpleHPSCondition();
                c._run_nr = run;
                c._int_current = cur;
                c._start_time = start;
                c._stop_time = stop;
                c._rate = rate;
                c._rate_rec = rate_rec;
                c._thickness = t;
                this._conditionsList.add(c);
                System.out.println("Add conditions for run " + c.toString());
                
            }
            bufReader.close();
            fReader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE,null,e);
        } 
        
    }
    
    private SimpleHPSCondition getCond(int run) {
        for(SimpleHPSCondition c: this._conditionsList) {
            if(c._run_nr==run) {
                return c;
            }
        }
        return null;
    }
    public int getStartTime(int run) {
        return this.getCond(run)._start_time;
    }
    public int getStopTime(int run) {
        return this.getCond(run)._stop_time;
    }
    public double getIntCurrent(int run) {
        return this.getCond(run)._int_current;
    }
    public double getRate(int run) {
        return this.getCond(run)._rate;
    }
    public double getRecRate(int run) {
        return this.getCond(run)._rate_rec;
    }
    public double getThickness(int run) {
        return this.getCond(run)._thickness;
    }
    
    public String toString() {
         //String str = String.format("%10s\t%8d\t%8d\t%8.2f+-%.2f",getRun(),getStartTime(),getStopTime(),getIntCurrent(),getIntCurrentError());
         String str = " yeah need to add something";
         return str;
    }
    
    
    
}
