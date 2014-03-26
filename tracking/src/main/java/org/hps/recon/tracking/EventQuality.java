/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class to hold default cut values
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @version $id: $ 
 */
public class EventQuality {
    
    
    
    private static EventQuality _instance = null;
    public static enum Quality{LOOSE,MEDIUM,TIGHT}
    public static enum Cut{
        UNDEFINED(0),PZ(1),CHI2(2),SHAREDHIT(3),NHITS(4),TOPBOTHIT(5);
        private int value;
        private Cut(int value) {
            this.value = value;
        }
        public int getValue() {
            return this.value;
        }
    }
    private List<CutObject> _cut_list = new ArrayList<CutObject>();
    
    public EventQuality() {
        addCut(new CutObject(Cut.CHI2,"CHI2",100000.,10.,10.));
        addCut(new CutObject(Cut.PZ,"PZ",0.000005,0.4,0.4));
        addCut(new CutObject(Cut.SHAREDHIT,"SHAREDHIT",0,0,0));
        addCut(new CutObject(Cut.NHITS,"NHITS",4,4,5));
        addCut(new CutObject(Cut.TOPBOTHIT,"TOPBOTHIT",0,0,0));
    }
    
    public static EventQuality instance() {
        if(_instance==null) {
            _instance = new EventQuality();
        }
        return _instance;
    }
        
    private void addCut(CutObject c) {
        _cut_list.add(c);
    }
    
    private CutObject find(Cut cut) {
        for(CutObject co : _cut_list) {
            if (co._cut==cut) {
                return co;
            }
        }
        return null;
    }
    
    public double getCutValue(Cut cut,Quality quality) {
        CutObject co = find(cut);
        if(co==null) {
            System.out.printf("Cut \"%d\" didn't exist!?\n",cut);
            System.exit(0);
        }
        return co.get(quality);
    }
    
    @Override
    public String toString() {
        String s = String.format("EventQuality has %d cuts defined:\n",this._cut_list.size());
        for (CutObject c: _cut_list) {
            s += String.format("%s\n",c.toString());
        }
        return s;
    }
    
    public String print(int cuts) {
        String s = String.format("cuts=%d:\n",cuts);
        for (CutObject cut : _cut_list) {
            int tmp = cuts & (1<<cut._cut.getValue());
            s += String.format("Cut %s %s\n",cut._name,tmp==0?"PASSED":"FAILED");
        }
        return s;
    }
    
    private class CutObject {
        String _name = "UNDEFINED";
        Cut _cut = Cut.UNDEFINED;
        Map<Quality,Double> _map = new HashMap<Quality,Double>();
        public CutObject(Cut c, String name, double val1,double val2,double val3) {
            _cut = c;
            _name = name;
            _map.put(Quality.LOOSE, val1);
            _map.put(Quality.MEDIUM, val2);
            _map.put(Quality.TIGHT, val3);
        }
        public void set(Quality q, double val) {
            _map.put(q,val);
        }
        public double get(Quality q) {
            return _map.get(q);
        }
        @Override
        public String toString() {
            return String.format("Name:%s cut:%d val=[%f,%f,%f]",_name,_cut.getValue(),_map.get(Quality.LOOSE),_map.get(Quality.MEDIUM),_map.get(Quality.TIGHT));
        }
    }
    
}
