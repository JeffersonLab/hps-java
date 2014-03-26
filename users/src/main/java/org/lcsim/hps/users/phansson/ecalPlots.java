/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author phansson
 */
public class ecalPlots {

    
//    private AIDA aida = AIDA.defaultInstance();
//    private IAnalysisFactory af = aida.analysisFactory();
//    IHistogramFactory hf = aida.histogramFactory();
//    ITree tree = aida.tree();//(ITreeFactory) af.createTreeFactory().create();

    
    private static void printObjectsInTree(ITree tree) {
        System.out.println("-----\nObject names in tree " + tree.name() + ":");
        for( String str : tree.listObjectNames()) {
            System.out.println(str);
        }
        System.out.println("-----");
    }
    
    
    private static Options createCommandLineOptions() {
        Options option = new Options();
        option.addOption("s", false, "Save to file");
        option.addOption("t", true, "Select the type of analysis to run");
        return option;
    }
    
    
    
    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) {
        // TODO code application logic here
        

        IAnalysisFactory af = IAnalysisFactory.create();
        ECalHitMapPlots ecalhtplots = new ECalHitMapPlots();
            
        
        
        Options opts = createCommandLineOptions();
        if (args.length == 0) {
            System.out.println("ecalPlots [options]");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", opts);
            System.exit(1);
        }
        CommandLineParser parser = new PosixParser();
        CommandLine cmd=null;
        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException ex) {
            Logger.getLogger(ecalPlots.class.getName()).log(Level.SEVERE, null, ex);
        }

        
        
        int type=0;
        String strType = cmd.getOptionValue("t");
        if(strType==null) {
            System.out.println("using default analysis " + type);
        } else {
            type = Integer.parseInt(strType);
        }
        boolean savePlots = false;
        if(cmd.hasOption("s")) {
            savePlots = true;
        }
        
 
               
        if(type==999) {
        
            ITree tree=null;
            try {
                String[] list = cmd.getArgs();
                String file  = list[0];
                tree = af.createTreeFactory().create(file);
                printObjectsInTree(tree);
                IHistogram1D h = (IHistogram1D)tree.find(list[1]);
                ecalhtplots.plotBasic1D(h,list[1],"","","",false);
                
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ecalPlots.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(ecalPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return;
        }
        
        if(type==0) {
            
            try {
                String path = "plots/20120710_ecal_dataMC/";
                String file_mc = "../../multscatana.aida";
                //String file_mc = "multscatana_egs5_500empty.aida";
                String file_data = "multscatana_run1351.aida";
                String file_bkg = "multscatana_run1358.aida";
                
                //ITree tree_mc = af.createTreeFactory().create(path+"multscatana_egs5_ecalbadchapplied.aida");
            //if(1==1) return;
            ITree tree_mc = af.createTreeFactory().create(path+ file_mc);
            printObjectsInTree(tree_mc);
            if(1==1) return;
            
            ITree tree_data = af.createTreeFactory().create(path+file_data);
            ITree tree_bkg = af.createTreeFactory().create(path+file_bkg);
            String name = "dataMC_500empty_run1351_egs5_currentNorm";
            
            //Overlay a few histograms
            List<String> histNames = new ArrayList<String>();
            
            histNames.add("Cluster energy all");
            histNames.add("Cluster energy bottom");
            histNames.add("Cluster energy top");
            histNames.add("Cluster size");
            histNames.add("Cluster size bottom");
            histNames.add("Cluster size top");
            histNames.add("Crystal amplitude all");
            histNames.add("Crystal amplitude bottom");
            histNames.add("Crystal amplitude top");
            for(int icol=-5;icol<=5;++icol) {
                for(int irow=-23;irow<=23;++irow) {
                    if(Math.abs(irow)==1) {
                        histNames.add("Cluster energy x=" + icol + " y=" + irow);
                    }
                }
            }


            SimpleHPSConditions cond = new SimpleHPSConditions();
            
            int idx = file_data.indexOf("run");
            
            int run_data = Integer.parseInt(file_data.substring(idx+3,idx+7));
            idx = file_bkg.indexOf("run");
            int run_bkg = Integer.parseInt(file_bkg.substring(idx+3,idx+7));
            
            //normalize to integrated current of data
            double int_current_mc = 90.0; //nC i.e. 1s of beam at 90nA
            double k_Q = cond.getIntCurrent(run_data)/int_current_mc;
            System.out.printf("Run %d: intCurrent %.1fnC MC<->%.1fnC  => k_current %.1f\n",run_data,cond.getIntCurrent(run_data),int_current_mc,k_Q );
            double k_rec = cond.getRecRate(run_data)/cond.getRate(run_data);
            System.out.printf("Run %d: rate %.1fHz rec rate %.1fHz  => k_rate %.1f\n",run_data,cond.getRate(run_data),cond.getRecRate(run_data),k_rec );
            
                boolean normDataToMC = true;
                for(String hname : histNames) {
                    IHistogram1D h_mc = (IHistogram1D)tree_mc.find(hname);
                    IHistogram1D h_obs = (IHistogram1D)tree_data.find(hname);
                    IHistogram1D h_bkg = (IHistogram1D)tree_bkg.find(hname);
                    double rate_obs = cond.getRecRate(run_data);//1933.479;
                    double rate_bkg = cond.getRecRate(run_bkg);//309.785;
                    double c_rate = rate_bkg/rate_obs;
                    h_bkg.scale((h_obs.entries()/h_bkg.entries())*c_rate);
                    IHistogram1D h_data = ecalhtplots.hf.subtract(hname + " bkgsubtr", h_obs, h_bkg);
                    if(normDataToMC) {
                        h_mc.scale(h_data.sumBinHeights()/h_mc.sumBinHeights());
                    } else {
                        h_mc.scale(k_Q*k_rec);
                    }
                    //System.out.println("mc " + h_mc.entries() + " obs " + h_obs.entries() + " bkg " + h_bkg.entries() + " obs " + h_data.entries());
                    //System.out.println("mc " + h_mc.sumBinHeights() + " obs " + h_obs.sumBinHeights() + " bkg " + h_bkg.sumBinHeights() + " obs " + h_data.sumBinHeights());
                    System.out.printf("Run %d: obs %.1f bkg %.1f\t=>\tdata %.1f MC %.1f\t->\tdata/MC=%.2f\n",run_data,h_obs.sumBinHeights(),h_bkg.sumBinHeights(),h_data.sumBinHeights(),h_mc.sumBinHeights(),h_data.sumBinHeights()/h_mc.sumBinHeights());

                    ecalhtplots.plotBasic1D(h_data,h_mc,name+"_"+hname, "","","Data","MC",savePlots);

                }
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            
        }
        
        
        
        
        
         if(type==2) {
            
            try {
                String path = "";//plots/20120710_ecal_dataMC/../../";
                String file_dead = "multscatana_egs5_160rl.aida";//multscatana_egs5_500empty_nodeadchannelfilter.aida";
                String file = "multscatana_egs5_160rl.aida";//"multscatana_egs5_500empty.aida";
                 ITree tree_dead = af.createTreeFactory().create(path+ file_dead);
                 ITree tree = af.createTreeFactory().create(path+ file);
                String name = "deadChEffect_egs5";

                //Overlay a few histograms
                List<String> histNames = new ArrayList<String>();
                
                histNames.add("Cluster energy x=4 y=1");
                histNames.add("Cluster energy x=5 y=1");
                
                
                for(String hname : histNames) {
                    IHistogram1D h = (IHistogram1D)tree.find(hname);
                    IHistogram1D h_dead = (IHistogram1D)tree_dead.find(hname);
                    
                    //h_bkg.scale((h.entries()/h_dead.entries())*c_rate);
                    //IHistogram1D h_data = ecalhtplots.hf.subtract(hname + " bkgsubtr", h_obs, h_bkg);
                    //h_mc.scale(h_data.sumBinHeights()/h_mc.sumBinHeights());
                    
                    //System.out.println("mc " + h_mc.entries() + " obs " + h_obs.entries() + " bkg " + h_bkg.entries() + " obs " + h_data.entries());
                    //System.out.println("mc " + h_mc.sumBinHeights() + " obs " + h_obs.sumBinHeights() + " bkg " + h_bkg.sumBinHeights() + " obs " + h_data.sumBinHeights());
                    //System.out.printf("Run %d: obs %.1f bkg %.1f\t=>\tdata %.1f MC %.1f\t->\tdata/MC=%.2f\n",run_data,h_obs.sumBinHeights(),h_bkg.sumBinHeights(),h_data.sumBinHeights(),h_mc.sumBinHeights(),h_data.sumBinHeights(),h_mc.sumBinHeights());

                    ecalhtplots.plotBasic1D(h,h_dead,name+"_"+hname, "","","Fixed","Bad Ch. incl.",savePlots);

                }
                
                

            } catch(IOException e) {
                throw new RuntimeException(e);
            }
         }
        
        
        
          
        
         
        
        /*
        ITree tree_multscatana = null;
        try {
            tree_multscatana = analysisFactory.createTreeFactory().create("plots/PAC/20120610/noEcalChFilter_1351/multscatana_1351_noEcalChFilter.aida");
            //tree_multscatana = analysisFactory.createTreeFactory().create("plots/PAC/20120610/EcalChFilter_1351/multscatana_1351_EcalChFilter.aida");
            
            

            printObjectsInTree(tree_multscatana);
            
            //ecalhtplots.plotMultScatAna(tree_multscatana);
            
            
            
            
            
            //tree_multscatana = analysisFactory.createTreeFactory().create("multscatana_1351.aida");
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        
        
        ITree tree_filter = null;
        try {
            tree_filter = analysisFactory.createTreeFactory().create("plots/PAC/20120610/noEcalChFilter_1351/ecalCrystalFilter_1351_noEcalChFilter.aida");
            //tree_filter = analysisFactory.createTreeFactory().create("plots/PAC/20120610/EcalChFilter_1351/ecalCrystalFilter_1351_EcalChFilter.aida");
        
            printObjectsInTree(tree_filter);
        
            //ecalhtplots.plotEcalFilter(tree_filter);
            
        
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        
        */
        
        if(type==1) {
            ITree tree_empty_multscatana = null;
            ITree tree_1351_multscatana = null;
            try {
                tree_empty_multscatana = af.createTreeFactory().create("multscatana_1358_EcalChFilter_100k.aida");
                tree_1351_multscatana = af.createTreeFactory().create("multscatana_1351_EcalChFilter.aida");
                //tree_1351_multscatana = analysisFactory.createTreeFactory().create("plots/PAC/20120610/EcalChFilter_1351/multscatana_1351_EcalChFilter.aida");


                ecalhtplots.overLayUpStrBkg(tree_empty_multscatana, tree_1351_multscatana);



            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        
        
    }

    public static double getBunchesFromFileName(String filename) {
        //trigratefile_egs5_merged_500mb_90na_0x016.aida
        String[] parts = filename.split("_");
        for(String part : parts) {
            int idx = part.indexOf("mb");
            if(idx!=-1) {
                return Double.valueOf(part.substring(0, idx));
            }
            idx = part.indexOf("bb");
            if(idx!=-1) {
                return 1000.0*Double.valueOf(part.substring(0, idx));
            }
            
        }
        return -1.0;
        
    }

    public static double getCurrentFromFileName(String filename) {
        //trigratefile_egs5_merged_500mb_90na_0x016.aida
        String[] parts = filename.split("_");
        for(String part : parts) {
            int idx = part.indexOf("na");
            if(idx==-1) {
                continue;
            }
            return Double.valueOf(part.substring(0, idx));
        }
        return -1.0;
        
    }

    
}
