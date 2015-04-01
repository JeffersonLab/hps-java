/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.ITree;
import hep.aida.ref.plotter.PlotterRegion;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.util.aida.AIDA;



/**
 *
 * @author phansson
 */





public class TrigRateAna {
    
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    IHistogramFactory hf = aida.histogramFactory();
//    ITree tree = aida.tree();//(ITreeFactory) af.createTreeFactory().create();
    
    private int _bkgRunNr1 = -1;
    private int _bkgRunNr2 = -1;
    
    private boolean _saveFiles = false;
    private boolean _hide = false;
    private String _addOn = "";
    
    double _C_ep = 1;
    
    EcalHitMapPlots util = new EcalHitMapPlots();
    
    List<Count> _countList = new ArrayList<Count>();

    HashMap<String,Integer> _counts = new HashMap<String,Integer>();
    HashMap<String,Integer> _counts_merged = new HashMap<String,Integer>();
    HashMap<String,Double> _counts_merged_norm = new HashMap<String,Double>();
    HashMap<String,Double> _counts_merged_norm_subtr = new HashMap<String,Double>();
    HashMap<String,Double> _counts_merged_norm_subtr_qscaled = new HashMap<String,Double>();
    HashMap<String,Double> _counts_merged_norm_subtr_qscaled_daqscaled = new HashMap<String,Double>();
    
    HashMap<String,Double> _ratio = new HashMap<String,Double>();

    List<PolarCount> _polarCountList = new ArrayList<PolarCount>();
    
    List<BeamCurrentData> _beamCurrent = new ArrayList<BeamCurrentData>();
    List<DAQDeadTimeData> _deadTime = new ArrayList<DAQDeadTimeData>();
    HashMap<Integer,Double> _thicknessMap = new HashMap<Integer,Double>();

  
    double _chargeScale = 1.0;
    
    
    public void saveFiles(boolean v) {
        _saveFiles = v;
    }
    
    public void addOn(String s) {
        _addOn = s;
    }
    
    public void hidePlots(boolean h) {
        this._hide = h;
        this.util.hidePlots(this._hide);
    }
    
    public TrigRateAna() {
        
        //System.out.println("Constructor ECalHitMapPlots");
        
    }
    
    public void setChargeNormalization(double q) {
        _chargeScale = q;
    }
   
    public void setBackgroundRunNrs(int nr1, int nr2) {
        _bkgRunNr1 = nr1;
        _bkgRunNr2 = nr2;
        
    }
    
    
    public void getGainCalibration(ITree tree,String side) {
        
        IHistogram1D h = (IHistogram1D)tree.find("Cluster E>600GeV Eoverp good region " + side);
        
        util.plotBasic1D(h,"Eoverp good region E>600MeV", "E/p","Arbitrary Units","green",_saveFiles);
        
        double max = -1;
        int bin_max = -1;
        for(int bin=1;bin<h.axis().bins();++bin) {
            if(h.binEntries(bin)>max) {
                max=h.binEntries(bin);
                bin_max = bin;
            }
        }
        
        System.out.println("Maximum E/p: " + h.axis().binCenter(bin_max));
        _C_ep = h.axis().binCenter(bin_max);
        System.out.println("Gain calibration: " + _C_ep);
        
        return;
        
//        IHistogram2D hm = (IHistogram2D)tree.find("Cluster hit map all");        
//        plotBasic2D(hm,"Cluster Hit Map", "Horizontal Crystal Index","Vertical Crystal Index");
//        
//        IHistogram1D hAmp = (IHistogram1D)tree.find("Cluster size all");
//        plotBasic1D(hAmp,"Cluster Size", "Cluster Size","Arbitrary Units","green");

        
        


    
        
    }
    
    
    private int findCount(List<Count> list, String r,String n) {
        //System.out.println("Looking for run " + r + " for type " + n + " among " + list.size() + "count objects");
        for(int i=0;i<list.size();++i) {
            System.out.println(i +": " + list.get(i).run() + " " + list.get(i).name());
            if(list.get(i).name().equals(n)) {
                if(list.get(i).run().equals(r)) {
                    //System.out.println("Found it! " + i);
                    return i;
                }
            }
        }
        return -1;
    }
    
    private int findPolarCount(List<PolarCount> list, String r, double v, String n) {
        //System.out.println("Looking for run " + r + " for type " + n + " and val " + v + " among " + list.size() + " polar count objects");
        for(int i=0;i<list.size();++i) {
            //System.out.println(i +": " + list.get(i).run() + " " + list.get(i).name() + " " + list.get(i).val());
            if(list.get(i).name().equals(n)) {
                if(list.get(i).run().equals(r)) {
                    if(Double.compare(list.get(i).val(),v)==0) {
                        //System.out.println("Found it! " + i);
                        return i;
                    }
                }
            }
        }
        return -1;
    }
    
    private List<Count> findCount(String name) {
        List<Count> list = new ArrayList<Count>();
        for(Count c :_countList) {
            if(c.name().equals(name)) list.add(c);
        }
        return list;
    }

    private List<PolarCount> findPolarCount(String name) {
        List<PolarCount> list = new ArrayList<PolarCount>();
        for(PolarCount c :_polarCountList) {
            if(c.name().equals(name)) list.add(c);
        }
        return list;
    }

    
    public void mergeCounts() {
//        HashMap<String,Integer> m = _counts_merged;
//        for (Map.Entry<String, Integer> e : _counts.entrySet()) {
//            String k = e.getKey();
//            Integer n = e.getValue();
//            if(k.contains("_")) {
//                int idx = k.indexOf("_");
//                String str = k.substring(0, idx);
//                k = str;
//            }
//            if(m.containsKey(k)) {
//                n += m.get(k);
//            }
//            m.put(k, n);
//        }
//        
        List<Count> list = new ArrayList<Count>();
        
        for(Count c : _countList) {
            String run = c.run();
            String name = "merged"; //c.name();
            if(run.contains("_")) {
                int idx = run.indexOf("_");
                String str = run.substring(0, idx);
                run = str;
            }
            int idx = findCount(list,run,name);
            if(idx<0) {
                Count c_new = new Count(name,run,c.n(),c.en());
                list.add(c_new);
            } else {
                list.get(idx).addSimple(c.n(),c.en());
            }
            
        }
        _countList.addAll(list);
        
        
        
        
        List<PolarCount> list2 = new ArrayList<PolarCount>();
        
        for(PolarCount c : _polarCountList) {
            String run = c.run();
            String name = "merged"; //c.name();
            if(run.contains("_")) {
                int idx = run.indexOf("_");
                String str = run.substring(0, idx);
                run = str;
            }
            int idx = findPolarCount(list2,run,c.val(),name);
            if(idx<0) {
                PolarCount c_new = new PolarCount(name,run,c.val(),c.count());
                
                list2.add(c_new);
            } else {
                list2.get(idx).addSimple(c);
            }
            
        }
        _polarCountList.addAll(list2);
        
        
        
        
    }
    
    public boolean isBackgroundRun(int nr) {
        if(_bkgRunNr1==nr) return true;
        if(_bkgRunNr2==nr) return true;
        return false;
        
    }
    
    
    public BeamCurrentData getBCD(String run_str) {
        for(BeamCurrentData b : _beamCurrent) {
            if(b.getRun()==Integer.parseInt(run_str)) return b;
        }
        return null;
    }
    
    
    
    public void normalize() {
        
//        HashMap<String,Double> m = _counts_merged_norm;
//        
//        for (Map.Entry<String, Integer> e : _counts_merged.entrySet()) {
//            String k = e.getKey();
//            Integer n = e.getValue();
//            //get the integrated beam current
//            BeamCurrentData bcd = getBCD(k);
//            if(bcd==null) {
//                System.out.println("Error run " + k + " is not in beam current list!");
//                System.exit(1);
//            }
//            
//            double current = bcd.getIntCurrent();
//            double n_norm = n/current; // unit is [/nC]
//            m.put(k, n_norm);
//            
//        }
//        
        
        List<Count> list = new ArrayList<Count>();
        for(Count c : _countList) {
            if(c.name()!="merged") continue;
            String run = c.run();
            String name = "normalized"; //c.name();
            BeamCurrentData bcd = getBCD(run);
            if(bcd==null) {
                System.out.println("Error run " + run + " is not in beam current list!");
                System.exit(1);
            }
            double current = bcd.getIntCurrent();
            double ecurrent = bcd.getIntCurrentError();
            double n = c.n();
            double en = c.en();
            double n_norm = n/current;
            double en_norm = Math.sqrt((en/current)*(en/current) + (n/(current*current)*ecurrent)*(n/(current*current)*ecurrent));
            
            Count c_new = new Count(name,run,n_norm,en_norm);
            list.add(c_new);
            
        }
        _countList.addAll(list);
        
        
        List<PolarCount> polarlist = new ArrayList<PolarCount>();
        for(PolarCount c : _polarCountList) {
            if(c.name()!="merged") continue;
            String run = c.run();
            String name = "normalized"; //c.name();
            BeamCurrentData bcd = getBCD(run);
            if(bcd==null) {
                System.out.println("Error run " + run + " is not in beam current list!");
                System.exit(1);
            }
            double current = bcd.getIntCurrent();
            double ecurrent = bcd.getIntCurrentError();
            double n = c.count().n();
            double en = c.count().en();
            double n_norm = n/current;
            double en_norm = Math.sqrt((en/current)*(en/current) + (n/(current*current)*ecurrent)*(n/(current*current)*ecurrent));
            
            PolarCount c_new = new PolarCount(name,run,c.val(),new Count(name,run,n_norm,en_norm));
            polarlist.add(c_new);
            
        }
        _polarCountList.addAll(polarlist);
        
        
        
        
    }
    
    
      
    public void subtractBackground() {
       
//        double bkg1 = _counts_merged_norm.get(String.valueOf(this._bkgRunNr1));
//        double bkg2 = _counts_merged_norm.get(String.valueOf(this._bkgRunNr2));
//        for (Map.Entry<String, Double> e : _counts_merged_norm.entrySet()) {
//            String k = e.getKey();
//            double n = e.getValue();
//            int k_int = Integer.parseInt(k);
//            if(k_int == this._bkgRunNr1) continue;
//            if(k_int == this._bkgRunNr2) continue;
//            
//            double ns = k_int<1360 ? (n-bkg1) : (n-bkg2);
//                
//            _counts_merged_norm_subtr.put(k, ns);
//        }
//        
        
        int idx_bkg1 = findCount(this._countList,String.valueOf(this._bkgRunNr1),"normalized");
        int idx_bkg2 = findCount(this._countList,String.valueOf(this._bkgRunNr2),"normalized");
        
        double bkg1 = _countList.get(idx_bkg1).n();
        double bkg2 = _countList.get(idx_bkg2).n();
        
        System.out.println("bkg1 " + bkg1 + " bkg2 " + bkg2);
        
        double ebkg1 = _countList.get(idx_bkg1).en();
        double ebkg2 = _countList.get(idx_bkg2).en();
        
        List<Count> list = new ArrayList<Count>();
        for(Count c: _countList) {
            if(!c.name().equals("normalized")) continue;
            String name = "bkgsubtr";
            String run = c.run();
            int run_int = Integer.parseInt(run);
            if(this.isBackgroundRun(run_int)) continue;
            double b = run_int<1360 ? bkg1 : bkg2;
            double eb = run_int<1360 ? ebkg1 : ebkg2;
            double n = c.n()-b;
            double en = Math.sqrt(c.en()*c.en() + eb*eb);
            
            Count c_new = new Count(name,run,n,en);
            list.add(c_new);
        }
        _countList.addAll(list);
        
        
        
        List<PolarCount> polarList = new ArrayList<PolarCount>();
        for(PolarCount c: _polarCountList) {
            if(!c.name().equals("normalized")) continue;
            String name = "bkgsubtr";
            String run = c.run();
            int run_int = Integer.parseInt(run);
            if(this.isBackgroundRun(run_int)) continue;
            
            idx_bkg1 = findPolarCount(this._polarCountList,String.valueOf(this._bkgRunNr1),c.val(),"normalized");
            idx_bkg2 = findPolarCount(this._polarCountList,String.valueOf(this._bkgRunNr2),c.val(),"normalized");
            bkg1 = _polarCountList.get(idx_bkg1).count().n();
            bkg2 = _polarCountList.get(idx_bkg2).count().n();
        
            System.out.println("polar " + c.val() + " bkg1 " + bkg1 + " bkg2 " + bkg2);
        
            ebkg1 = _polarCountList.get(idx_bkg1).count().en();
            ebkg2 = _polarCountList.get(idx_bkg2).count().en();
            
            double b = run_int<1360 ? bkg1 : bkg2;
            double eb = run_int<1360 ? ebkg1 : ebkg2;
            double n = c.count().n()-b;
            double en = Math.sqrt(c.count().en()*c.count().en() + eb*eb);
            
            PolarCount c_new = new PolarCount(name,run,c.val(),new Count(name,run,n,en));
            polarList.add(c_new);
        }
        _polarCountList.addAll(polarList);
        
        
    }
    
    
    
        
    public void scaleToRndCharge() {

        System.out.println("Scaling to " + _chargeScale + " nC  from nC");
        
        
//        for (Map.Entry<String, Double> e : this._counts_merged_norm_subtr.entrySet()) {
//            String k = e.getKey();
//            double n = e.getValue();
//            int k_int = Integer.parseInt(k);
//            
//            double ns = n*_chargeScale;
//            
//            _counts_merged_norm_subtr_qscaled.put(k, ns);
//        }
        
        List<Count> list = new ArrayList<Count>();
        for(Count c : _countList) {
            if(!c.name().equals("bkgsubtr")) continue;
            String name = "q-scaled";
            String run  = c.run();
            double ns = c.n()*_chargeScale;
            double ens = c.en()*_chargeScale;
            Count c_new = new Count(name,run,ns,ens);
            list.add(c_new);
            
        }
        _countList.addAll(list);
        
        
        List<PolarCount> polarList = new ArrayList<PolarCount>();
        for(PolarCount c : _polarCountList) {
            if(!c.name().equals("bkgsubtr")) continue;
            String name = "q-scaled";
            String run  = c.run();
            double ns = c.count().n()*_chargeScale;
            double ens = c.count().en()*_chargeScale;
            PolarCount c_new = new PolarCount(name,run,c.val(),new Count(name,run,ns,ens));
            polarList.add(c_new);
            
        }
        _polarCountList.addAll(polarList);
        
        
        
        
    }
    
    
     public DAQDeadTimeData getDTD(String run_str) {
        for(DAQDeadTimeData b : _deadTime) {
            if(b.getRun()==Integer.parseInt(run_str)) return b;
        }
        return null;
    }
    
     public void scaleDAQDeadTime() {

        System.out.println("Scaling DAQ dead time");
        
        
//        for (Map.Entry<String, Double> e : this._counts_merged_norm_subtr_qscaled.entrySet()) {
//            String k = e.getKey();
//            double n = e.getValue();
//            DAQDeadTimeData dtd = getDTD(k);
//            if(dtd==null) {
//                System.out.println("Error run " + k + " is not in daq dead time map");
//                System.exit(1);
//            }
//            
//            double ns = n/dtd.getDAQLiveTimeFraction();
//            
//            _counts_merged_norm_subtr_qscaled_daqscaled.put(k, ns);
//        }
        
        
        List<Count> list = new ArrayList<Count>();
        for(Count c : _countList) {
            if(!c.name().equals("q-scaled")) continue;
            String name = "daq-scaled";
            String run  = c.run();
            DAQDeadTimeData dtd = getDTD(run);
            if(dtd==null) {
                System.out.println("Error run " + run + " is not in daq dead time map");
                System.exit(1);
            }
            double a = c.n();
            double b = dtd.getDAQLiveTimeFraction();
            double ea = c.en();
            double eb = dtd.getDAQLiveTimeFractionError();
            double ns = a/b;
            double ens = Math.sqrt((ea/b)*(ea/b) + (eb*a/(b*b))*(eb*a/(b*b)));
            Count c_new = new Count(name,run,ns,ens);
            list.add(c_new);
            
        }
        
        _countList.addAll(list);
        
        
         List<PolarCount> polarList = new ArrayList<PolarCount>();
        for(PolarCount c : _polarCountList) {
            if(!c.name().equals("q-scaled")) continue;
            String name = "daq-scaled";
            String run  = c.run();
            DAQDeadTimeData dtd = getDTD(run);
            if(dtd==null) {
                System.out.println("Error run " + run + " is not in daq dead time map");
                System.exit(1);
            }
            double a = c.count().n();
            double b = dtd.getDAQLiveTimeFraction();
            double ea = c.count().en();
            double eb = dtd.getDAQLiveTimeFractionError();
            double ns = a/b;
            double ens = Math.sqrt((ea/b)*(ea/b) + (eb*a/(b*b))*(eb*a/(b*b)));
            PolarCount c_new = new PolarCount(name,run,c.val(),new Count(name,run,ns,ens));
            polarList.add(c_new);
            
        }
        
        _polarCountList.addAll(polarList);
        
        
    }
    
    
     public double thickness(String run) {
         if(run.contains("_")) {
             run = run.split("_")[0];
         }
         int run_int = Integer.parseInt(run);
         if(!_thicknessMap.containsKey(run_int)) {
             System.out.println("thickness map do not contain " + run_int);
             System.exit(1);
         }
         return _thicknessMap.get(run_int);
         
     }
     
     
    public void prettyPrintCount() {
        System.out.println("== Raw counts ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        
        for (Map.Entry<String, Integer> e : _counts.entrySet()) {
            System.out.printf("%10s\t%17.2f\t%10d\n", e.getKey(),thickness(e.getKey()),e.getValue());
        }
        
        System.out.println("== Merged ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Map.Entry<String, Integer> e : _counts_merged.entrySet()) {
            System.out.printf("%10s\t%17.2f\t%10d\n", e.getKey(),thickness(e.getKey()),e.getValue());
        }
        System.out.println("== Merged and normalized ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Map.Entry<String, Double> e : _counts_merged_norm.entrySet()) {
            System.out.printf("%10s\t%17.2f\t%10.2f [/nC]\n", e.getKey(),thickness(e.getKey()),e.getValue());
        }
        System.out.println("== Merged, normalized and subtracted ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Map.Entry<String, Double> e : _counts_merged_norm_subtr.entrySet()) {
            System.out.printf("%10s\t%17.2f\t%10.2f [/nC]\n", e.getKey(),thickness(e.getKey()),e.getValue());
        }
        System.out.println("== Merged, normalized, subtracted ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Map.Entry<String, Double> e : _counts_merged_norm_subtr_qscaled.entrySet()) {
            System.out.printf("%10s\t%17.2f\t%10.2f [/%.1fnC]\n", e.getKey(),thickness(e.getKey()),e.getValue(),_chargeScale);
        }
        System.out.println("== Merged, normalized, subtracted and corrected for DAQ dead time ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Map.Entry<String, Double> e : _counts_merged_norm_subtr_qscaled_daqscaled.entrySet()) {
            System.out.printf("%10s\t%17.2f\t%10.2f [/%.1fnC]\n", e.getKey(),thickness(e.getKey()),e.getValue(),_chargeScale);
        }

        System.out.println("== Ratio w.r.t. 0.18% thickness ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","N[thickness]/N[0.18%]");
        for (Map.Entry<String, Double> e : _ratio.entrySet()) {
            System.out.printf("%10s\t%17.2f\t%10.2f\n", e.getKey(),thickness(e.getKey()),e.getValue());
        }

        
        
        System.out.println("\n\n== Integrated beam currents ==");
        System.out.printf("%10s\t%8s\t%8s\t%8s\n","run","start","stop","Int. I [nC]\n");
        for(BeamCurrentData b: _beamCurrent) {
            System.out.printf("%s\n",b.toString());
        }
        
        System.out.println("\n\n== DAQ Dead Time ==");
        for(DAQDeadTimeData b: _deadTime) {
            System.out.printf("%s\n",b.toString());
        }
        
        
        
        
        
    }
    
    
    
    
    public void prettyPrintCountList() {
        System.out.println("== Raw counts ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        List<Count> list = findCount("raw");
        for (Count e : list) {
            System.out.printf("%10s\t%17.2f\t%10.2f+-%.3f\n", e.run(),thickness(e.run()),e.n(),e.en());
        }
        System.out.println("== Merged ==");
        list = findCount("merged");        
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Count e : list) {
            System.out.printf("%10s\t%17.2f\t%10.2f+-%.3f\n", e.run(),thickness(e.run()),e.n(),e.en());
        }

        System.out.println("== Merged and normalized ==");
        list = findCount("normalized");        
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Count e : list) {
            System.out.printf("%10s\t%17.2f\t%10.2f+-%.3f\n", e.run(),thickness(e.run()),e.n(),e.en());
        }

        System.out.println("== Merged, normalized and subtracted ==");
        list = findCount("bkgsubtr");        
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Count e : list) {
            System.out.printf("%10s\t%17.2f\t%10.2f+-%.3f\n", e.run(),thickness(e.run()),e.n(),e.en());
        }
       
        System.out.println("== Merged, normalized, subtracted, q-scaled ==");
        list = findCount("q-scaled");        
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Count e : list) {
            System.out.printf("%10s\t%17.2f\t%10.2f+-%.3f [/%.1fnC]\n", e.run(),thickness(e.run()),e.n(),e.en(),_chargeScale);
        }
        
        System.out.println("== Merged, normalized, subtracted and corrected for DAQ dead time ==");
        list = findCount("daq-scaled");        
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","Counts");
        for (Count e : list) {
            System.out.printf("%10s\t%17.2f\t%10.2f+-%.3f [/%.1fnC]\n", e.run(),thickness(e.run()),e.n(),e.en(),_chargeScale);
        }

        System.out.println("== Ratio w.r.t. 0.18% thickness ==");
        System.out.printf("%10s\t%17s\t%10s\n","run","Target thickn. [% r.l.]","N[thickness]/N[0.18%]");
        list = findCount("ratio0.18");        
        for (Count e : list) {
            System.out.printf("%10s\t%17.2f\t%10.2f+-%.3f [/%.1fnC]\n", e.run(),thickness(e.run()),e.n(),e.en(),_chargeScale);
        }

        
       
        System.out.println("== Polar: Merged, normalized, subtracted and corrected for DAQ dead time ==");
        List<PolarCount> polarList = findPolarCount("daq-scaled");        
        List<String> listRun = new ArrayList<String>();
        for(PolarCount c : polarList) {
            if(listRun.contains(c.run())) continue;
            listRun.add(c.run());
        }
        System.out.printf("%10s\t%17s\t%10s\t%10s\n","run","Target thickn. [% r.l.]","Polar \"Y\"","Counts");
        for(String r : listRun) {
            for (PolarCount e : polarList) {
                if(!e.run().equals(r)) continue;
                System.out.printf("%10s\t%17.2f\t%10.1f\t%10.2f+-%.3f [/%.1fnC]\n", e.run(),thickness(e.run()),e.val(),e.count().n(),e.count().en(),_chargeScale);
            }
        }
        
        
        
        
        
        
        
        System.out.println("\n\n== Integrated beam currents ==");
        System.out.printf("%10s\t%8s\t%8s\t%8s\n","run","start","stop","Int. I [nC]\n");
        for(BeamCurrentData b: _beamCurrent) {
            System.out.printf("%s\n",b.toString());
        }
        
        System.out.println("\n\n== DAQ Dead Time ==");
        for(DAQDeadTimeData b: _deadTime) {
            System.out.printf("%s\n",b.toString());
        }
        
        
        
        
        
    }
    
    
    
    
    
    public void plotCount() {
        plotCount("raw");
        plotCount("merged");
        plotCount("normalized");
        plotCount("bkgsubtr");
        plotCount("q-scaled");
        plotCount("daq-scaled");
        
        plotPolarCount("raw");
        plotPolarCount("merged");
        plotPolarCount("normalized");
        plotPolarCount("bkgsubtr");
        plotPolarCount("q-scaled");
        plotPolarCount("daq-scaled");

/*
        plotCount(this._counts,"Counts raw");
        plotCount(this._counts_merged,"Counts merged");
        plotCountD(this._counts_merged_norm,"Counts merged normalized");
        plotCountD(this._counts_merged_norm,"Counts merged background subtracted");
        plotCountD(this._counts_merged_norm_subtr_qscaled,"Counts merged normalized q-norm");
        plotCountD(this._counts_merged_norm_subtr_qscaled_daqscaled,"Counts merged normalized q-norm");
        */
    }

//    public void plotCount(Map<String,Integer> map, String name) {
//        HashMap<String,Double> m = new HashMap<String,Double>();
//        for(Map.Entry<String,Integer> e : map.entrySet()) {
//            m.put(e.getKey(), e.getValue().doubleValue());
//        }
//        plotCountD(m,name);
//    }
//    
    public void plotCount(String name) {

      //IDataPointSetFactory dpsf = af.createDataPointSetFactory();
      
      // Create a one dimensional IDataPointSet.
      IDataPointSet dps = af.createDataPointSetFactory(af.createTreeFactory().createTree()).create("dps1D",name,2);
      String xtitle = "Target thickness [% rad. len.]";
      for (Count e : _countList) {
          if(!e.name().equals(name)) continue;
            dps.addPoint();
            dps.point(dps.size()-1).coordinate(0).setValue(this.thickness(e.run()));
            dps.point(dps.size()-1).coordinate(0).setErrorPlus(0);
            dps.point(dps.size()-1).coordinate(1).setValue(e.n());
            dps.point(dps.size()-1).coordinate(1).setErrorPlus(e.en());
            //xtitle += " " + e.getKey();
            
      }
      IPlotter plotter_hm = af.createPlotterFactory().create();
      plotter_hm.setTitle("Counts");
      plotter_hm.currentRegion().plot(dps);
        
      ((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xtitle);
        
        
        //if(fillColor=="") fillColor="yellow";
        //plotter_hm.createRegion(10.0,20.0, 460.0,100.0);
        //plotter_hm.createRegion(d, d1, d2, d3)
//        plotter_hm.createRegions(1,1);//.plot(hm);
//        plotter_hm.region(0).plot(h);
//        plotter_hm.region(1).plot(h2);
//        plotter_hm.region(2).plot(h);
//        plotter_hm.region(2).plot(h2,"mode=overlay");
        plotter_hm.style().statisticsBoxStyle().setVisible(false);
        //plotter_hm.style().dataStyle().fillStyle().setColor(fillColor);
        //((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xTitle);
        //((PlotterRegion) plotter_hm.region(0)).getPlot().getYAxis().setLabel(yTitle);          
        if(!this._hide) plotter_hm.show();
        
        
        if(_saveFiles) {
            try {
                plotter_hm.writeToFile(name + "_" + _addOn +".png", "png");
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        
        
    }
    
    public void plotPolarCount(String name) {

      IPlotter plotter_hm = af.createPlotterFactory().create();
      plotter_hm.setTitle("Polar Angle Counts");
      
      int nregions;
      List<String> runs = new ArrayList<String>();
      for (PolarCount e : _polarCountList) {
          if(!e.name().equals(name)) continue;
          if(runs.contains(e.run())) continue;
          runs.add(e.run());
      }
      
      System.out.println("Found " + runs.size() + " runs for " + name);
      boolean first = true;
      for(String run : runs) {
        //IDataPointSetFactory dpsf = af.createDataPointSetFactory();
      
        // Create a one dimensional IDataPointSet.
        IDataPointSet dps = af.createDataPointSetFactory(af.createTreeFactory().createTree()).create("dps1D",name,2);
        String xtitle = "Polar Hit Count";//Target thickness [% rad. len.]";
        for (PolarCount e : _polarCountList) {
            if(!e.name().equals(name)) continue;
            if(!e.run().equals(run)) continue;
                dps.addPoint();
//                dps.point(dps.size()-1).coordinate(0).setValue(this.thickness(e.run()));
                dps.point(dps.size()-1).coordinate(0).setValue(e.val());
                dps.point(dps.size()-1).coordinate(0).setErrorPlus(0);
                dps.point(dps.size()-1).coordinate(1).setValue(e.count().n());
                dps.point(dps.size()-1).coordinate(1).setErrorPlus(e.count().en());
                //xtitle += " " + e.getKey();
                
        }
      
        if(first) plotter_hm.currentRegion().plot(dps);
        else {
           plotter_hm.currentRegion().plot(dps,"mode=overlay"); 
        }
        
        ((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xtitle);
      }
        
        //if(fillColor=="") fillColor="yellow";
        //plotter_hm.createRegion(10.0,20.0, 460.0,100.0);
        //plotter_hm.createRegion(d, d1, d2, d3)
//        plotter_hm.createRegions(1,1);//.plot(hm);
//        plotter_hm.region(0).plot(h);
//        plotter_hm.region(1).plot(h2);
//        plotter_hm.region(2).plot(h);
//        plotter_hm.region(2).plot(h2,"mode=overlay");
        plotter_hm.style().statisticsBoxStyle().setVisible(false);
        //plotter_hm.style().dataStyle().fillStyle().setColor(fillColor);
        //((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xTitle);
        //((PlotterRegion) plotter_hm.region(0)).getPlot().getYAxis().setLabel(yTitle);          
        if(!this._hide) plotter_hm.show();
        
        
        if(_saveFiles) {
            try {
                plotter_hm.writeToFile(name + "_" + _addOn +".png", "png");
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        
        
    }

    
      public void makeRatio(double denominator_name, String name) {

           //Find the denominator
          double den = -1;
          double eden = 0;
//          for (Map.Entry<String, Double> e : this._counts_merged_norm_subtr.entrySet()) {
//              if(isBackgroundRun(Integer.parseInt(e.getKey()))) continue;
//              double t = this.thickness(e.getKey());
//              if(t == denominator_name) {
//                  den = e.getValue();
//                  break;
//              }
//          }
//          
//          System.out.println("Den " + den + " (" + denominator_name + ")");
//          if(den<0) {
//              System.exit(1);
//          }
//          
//        
//        for (Map.Entry<String, Double> e : this._counts_merged_norm_subtr.entrySet()) {
//        
//          double n =  e.getValue();
//          double ratio = n/den;
//          double t = this.thickness(e.getKey());
//          _ratio.put(e.getKey(), ratio);
//          //System.out.println(e.getKey() + " t " + t + " n " + n + " ratio " + ratio);
//
//            
//        }
//        
        
        
        for(Count c : _countList) {
            if(c.name()!="bkgsubtr") continue;
            if(isBackgroundRun(Integer.parseInt(c.run()))) continue;
            double t = this.thickness(c.run());
              if(t == denominator_name) {
                  den = c.n();
                  eden = c.en();
                  break;
              }
        }
        List<Count> list = new ArrayList<Count>();
        for(Count c : _countList) {
            if(c.name()!="bkgsubtr") continue;
            String name_new = "ratio0.18";
            double a = c.n();
            double ea = c.en();
            double b = den;
            double eb = eden;
            double r = a/b;
            double er = Math.sqrt((ea/b)*(ea/b) + (eb*a/(b*b))*(eb*a/(b*b)));
            Count c_new = new Count(name_new,c.run(),r,er);
            list.add(c_new);
            
            
        }
        _countList.addAll(list);
        
        
        
        
      }
    
    
    
    
     public void plotRatio(String name) {
     
     
         
          
      // Create a one dimensional IDataPointSet.
      IDataPointSet dps = af.createDataPointSetFactory(af.createTreeFactory().createTree()).create("dps2D",name,2);
      String xtitle = " Converter thickness (% r.l.)";
      //for (Map.Entry<String, Double> e : this._ratio.entrySet()) {
      for (Count e : this._countList) {
          if(!e.name().equals("ratio0.18")) continue;
          dps.addPoint();
          double t = this.thickness(e.run());
          dps.point(dps.size()-1).coordinate(0).setValue(t);
          dps.point(dps.size()-1).coordinate(0).setErrorPlus(0);
          dps.point(dps.size()-1).coordinate(1).setValue(e.n());
          dps.point(dps.size()-1).coordinate(1).setErrorPlus(e.en());
          //xtitle += " " + e.getKey();
          
            
      }
      
        IPlotter plotter_hm = af.createPlotterFactory().create();
        plotter_hm.style().dataStyle().lineStyle().setVisible(false);
        plotter_hm.setTitle("Ratio " + name);
        plotter_hm.currentRegion().plot(dps);
        ((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xtitle);
        
        
        //if(fillColor=="") fillColor="yellow";
        //plotter_hm.createRegion(10.0,20.0, 460.0,100.0);
        //plotter_hm.createRegion(d, d1, d2, d3)
//        plotter_hm.createRegions(1,1);//.plot(hm);
//        plotter_hm.region(0).plot(h);
//        plotter_hm.region(1).plot(h2);
//        plotter_hm.region(2).plot(h);
//        plotter_hm.region(2).plot(h2,"mode=overlay");
        plotter_hm.style().statisticsBoxStyle().setVisible(false);
        //plotter_hm.style().dataStyle().fillStyle().setColor(fillColor);
        //((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xTitle);
        //((PlotterRegion) plotter_hm.region(0)).getPlot().getYAxis().setLabel(yTitle);          
        if(!this._hide) plotter_hm.show();
        if(_saveFiles) {
            try {
                plotter_hm.writeToFile(name + "_" + _addOn + ".png", "png");
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        
        
    }
    
    
    public void addCount(ITree tree,String hname, String name) {
        
        IHistogram2D h = (IHistogram2D)tree.find(hname);
        
        if(h==null) {
            System.out.println("COuldn't find " + hname + "!!!!");
            return;
        }
        //System.out.println("save " + _saveFiles);
        util.plotBasic2DMap(h, hname + " " + name + " " + _addOn, "Horizontal Cluster Index", "Vertical Cluster Index", _saveFiles);
        //_counts.put(name, h.allEntries());
        
        Count c = new Count("raw",name,(double)h.allEntries(),Math.sqrt((double)h.allEntries()));
        _countList.add(c);
        
        
        
    }
    

    public void addPolarHist(ITree tree,String hname, String name) {
        
        IHistogram1D h = (IHistogram1D)tree.find(hname);
        
        if(h==null) {
            System.out.println("COuldn't find " + hname + "!!!!");
            return;
        }
        //System.out.println("save " + _saveFiles);
        util.plotBasic1D(h, hname + " " + name + " " + _addOn, "Horizontal Cluster Index", "Vertical Cluster Index", "yellow", _saveFiles);
        
        for(int i=1;i<h.axis().bins()+1;++i) {
            Count c = new Count("raw",name,(double)h.binEntries(i),h.binError(i));
            PolarCount pc = new PolarCount("raw",name,h.axis().binCenter(i),c);
            _polarCountList.add(pc);
        }
        
        
    }

    
    
    public void plotEp(ITree tree,String name) {
        String hname = "EoverP cl X<0 Pz>0.6GeV Y>1 top";
        IHistogram1D h = (IHistogram1D)tree.find(hname);
        
        if(h==null) {
            System.out.println("COuldn't find " + hname + "!!!!");
            return;
        }
        
        util.plotBasic1D(h,hname + " " + name+ " " + _addOn, "E/p","Arbitrary Units","green",_saveFiles);
        
        
        
        
        
    }
    
    
    
    public void loadBeamCurrent(String pathToFile) {
        System.out.println("Reading beam currents from " + pathToFile);
        FileReader fReader;
        BufferedReader bufReader;
        try {
            fReader = new FileReader(pathToFile);
            bufReader = new BufferedReader(fReader);
       
            String line;
            while( (line = bufReader.readLine()) != null) {
                //System.out.println("Line: " + line);
                if(line.contains("run")) continue;
                
                String[] vec_line = line.split("\\s+");
                //for(int i=0;i<5;++i) System.out.println(vec_line[i] +" ," );
                String s = vec_line[0];
                //System.out.println("s \"" + s + "\"");
                Integer run = Integer.parseInt(s);
                //System.out.println(run);
                int start = Integer.parseInt(vec_line[1]);
                int stop = Integer.parseInt(vec_line[2]);
                double cur = Double.parseDouble(vec_line[4]);

                BeamCurrentData bcd = new BeamCurrentData(run,start,stop,cur);
                _beamCurrent.add(bcd);
                
            }
            bufReader.close();
            fReader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE,null,e);
        } 
        
        
        
       
        
        
    }
    
    
    public void loadDAQDeadTime(String pathToFile) {
        System.out.println("Reading DAQ dead times from " + pathToFile);
        
        
        FileReader fReader;
        BufferedReader bufReader;
        try {
            fReader = new FileReader(pathToFile);
            bufReader = new BufferedReader(fReader);
       
            String line;
            while( (line = bufReader.readLine()) != null) {
                //System.out.println("Line: " + line);
                if(line.contains("run")) continue;
                
                String[] vec_line = line.split("\\s+");
                //for(int i=0;i<3;++i) System.out.println(vec_line[i] +" ," );
                
                int run = Integer.parseInt(vec_line[0]);
                double trig = Double.parseDouble(vec_line[1]);
                double daq = Double.parseDouble(vec_line[2]);

                DAQDeadTimeData d = new DAQDeadTimeData(run,trig,daq);
                _deadTime.add(d);
                
            }
            bufReader.close();
            fReader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE,null,e);
        } 
        
        
        
       
        
        
    }
    
     public void loadTargetThickness(String pathToFile) {
        System.out.println("Reading thickness from " + pathToFile);
        
        
        FileReader fReader;
        BufferedReader bufReader;
        try {
            fReader = new FileReader(pathToFile);
            bufReader = new BufferedReader(fReader);
       
            String line;
            while( (line = bufReader.readLine()) != null) {
                //System.out.println("Line: " + line);
                if(line.contains("run")) continue;
                
                String[] vec_line = line.split("\\s+");
                //for(int i=0;i<2;++i) System.out.println(vec_line[i] +" ," );
                
                int run = Integer.parseInt(vec_line[0]);
                double t = Double.parseDouble(vec_line[1]);
                _thicknessMap.put(run,t);

            }
            bufReader.close();
            fReader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE,null,e);
        } 
        
        
        
       
        
        
    }
    
    
    
    
    
    
   /*
    
    public void overLayUpStrBkg(ITree tree_empty, ITree tree) {
        
        IHistogram1D hYp_t = (IHistogram1D)tree.find("Top track q>0 Y @ -67cm");        
        IHistogram1D hYn_t = (IHistogram1D)tree.find("Top track q<0 Y @ -67cm");                
        IHistogram1D heYp_t = (IHistogram1D)tree_empty.find("Top track q>0 Y @ -67cm");
        IHistogram1D heYn_t = (IHistogram1D)tree_empty.find("Top track q<0 Y @ -67cm");        

        IHistogram1D hYp_b = (IHistogram1D)tree.find("Bottom track q>0 Y @ -67cm");        
        IHistogram1D hYn_b = (IHistogram1D)tree.find("Bottom track q<0 Y @ -67cm");                
        IHistogram1D heYp_b = (IHistogram1D)tree_empty.find("Bottom track q>0 Y @ -67cm");        
        IHistogram1D heYn_b = (IHistogram1D)tree_empty.find("Bottom track q<0 Y @ -67cm");        

        
        double r_t = 1933.479;
        double r_b = 1933.479;
        double re_t = 309.785;
        double re_b = 309.785;
        
        double c_t = re_t/r_t;
        double c_b = re_b/r_b;

        //plotBasic1D(hYp_t,"Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        //plotBasic1D(heYp_t,"Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","red");
        //plotBasic1D(hYp_t,heYp_t,"Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        //plotBasic1D(hYn_t,heYn_t,"Top track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");

        //plotBasic1D(hYp_b,heYp_b,"Bottom track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        //plotBasic1D(hYn_b,heYn_b,"Bottom track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");

        
        heYp_t.scale(hYp_t.entries()/heYp_t.entries()*c_t);
        heYn_t.scale(hYn_t.entries()/heYn_t.entries()*c_t);

        heYp_b.scale(hYp_b.entries()/heYp_b.entries()*c_b);
        heYn_b.scale(hYn_b.entries()/heYn_b.entries()*c_b);

        plotBasic1D(hYp_t,heYp_t,"(norm) Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        plotBasic1D(hYn_t,heYn_t,"(norm) Top track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");

        plotBasic1D(hYp_b,heYp_b,"(norm) Bottom track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        plotBasic1D(hYn_b,heYn_b,"(norm) Bottom track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");

        
        IHistogram1D hsYn_t = hf.subtract("(subtr) Top track q<0 Y @ -67cm", hYn_t, heYn_t);
        IHistogram1D hsYp_t = hf.subtract("(subtr) Top track q>0 Y @ -67cm", hYp_t, heYp_t);

        IHistogram1D hsYn_b = hf.subtract("(subtr) Bottom track q<0 Y @ -67cm", hYn_b, heYn_b);
        IHistogram1D hsYp_b = hf.subtract("(subtr) Bottom track q>0 Y @ -67cm", hYp_b, heYp_b);

        plotBasic1D(hsYp_t,"(subtr) Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        plotBasic1D(hsYn_t,"(subtr) Top track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        plotBasic1D(hsYp_b,"(subtr) Bottom track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        plotBasic1D(hsYn_b,"(subtr) Bottom track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        
        
        
        
    }
    
    
    */
    

/*    
    public void plotAveAmpMap(ITree tree) {

        System.out.println("plotAveAmp with tree " + tree.name());

        IHistogram2D hm = (IHistogram2D)tree.find("Cluster hit map all");
        
        
        IPlotter plotter_hm = af.createPlotterFactory().create();
        plotter_hm.setTitle("Cluster Hit Map");
        plotter_hm.createRegion().plot(hm);
        plotter_hm.show();
        default2DStyle(plotter_hm.region(0).style());
        plotter_hm.style().statisticsBoxStyle().setVisible(false);
        plotter_hm.style().setParameter("hist2DStyle","colorMap");
        plotter_hm.style().dataStyle().fillStyle().setParameter("colorMapScheme","rainbow");
              //((PlotterRegion) plotter_trig_other.region(i)).getPlot().setAllowPopupMenus(true);
        
        
        
        
    }

    */
    
    
}
