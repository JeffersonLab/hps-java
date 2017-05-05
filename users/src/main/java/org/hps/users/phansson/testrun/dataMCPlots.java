package org.hps.users.phansson.testrun;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.lcsim.util.aida.AIDA;

public class dataMCPlots {

    
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
        option.addOption("n", true, "Name added to plots");
        return option;
    }
    
    
    public static class DataCont {
        public double n_data;
        public double en_data;
        public double n_pred;
        public double en_pred;
        public DataCont(double data,double edata,double pred,double epred) {
            n_data=data;
            en_data=edata;
            n_pred=pred;
            en_pred=epred;
        }
    }
    
    
    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) {
        // TODO code application logic here
        

        IAnalysisFactory af = IAnalysisFactory.create();
        EcalHitMapPlots ecalhtplots = new EcalHitMapPlots();
            
        
        
        Options opts = createCommandLineOptions();
        if (args.length == 0) {
            System.out.println("dataMCPlots [options]");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", opts);
            System.exit(1);
        }
        CommandLineParser parser = new PosixParser();
        CommandLine cmd=null;
        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException ex) {
            Logger.getLogger(dataMCPlots.class.getName()).log(Level.SEVERE, null, ex);
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
        String outName = "dataMC";
        if(cmd.hasOption("n")) {
            outName = cmd.getOptionValue("n");
        }
        
        
        if(type==1) {
             HashMap<Integer,DataCont> map_dc = new HashMap<Integer,DataCont>();
             String path = "plots/20120723_ecal_dataMC/";
             Integer runs[] = {1351,1354,1353};
             Integer runsBkg[] = {1358,1358,1358};
             String mc[] = {path+"trigratefile_egs5_merged_5000mb_90na_0x016.aida",
                            path+"trigratefile_egs5_merged_20bb_90na_0x0045.aida",
                            path+"trigratefile_egs5_merged_50bb_90na_0x0018.aida"
                            };
             String mcG4[] = {path+"trigratefile_g4_merged_5000mb_90na_0x016.aida",
                            path+"trigratefile_g4_merged_20bb_90na_0x0045.aida",
                            path+"trigratefile_g4_merged_50bb_90na_0x0018.aida"
                            };
             
             double Q_normalization = 90.0; //nC i.e. 90nC is 1s of beam at 90nA
             
        
            SimpleHPSConditions cond = new SimpleHPSConditions();

            AIDA aida = AIDA.defaultInstance();
            //IAnalysisFactory af = aida.analysisFactory();
            IHistogramFactory hf = aida.histogramFactory();
            IDataPointSetFactory dpsf = af.createDataPointSetFactory(null);
    //    ITree tree = aida.tree();//(ITreeFactory) af.createTreeFactory().create();
            IDataPointSet dpsObs = dpsf.create("dpsObs","Data",2);
            IDataPointSet dpsPred = dpsf.create("dpsPred","EGS",2);
            IDataPointSet dpsPredG4 = dpsf.create("dpsPredG4","G4",2);
            //IHistogram1D hdata = aida.histogram1D("Observed",runs.length , 0, runs.length);
            for(int irun=0;irun<runs.length;++irun) {

                    DataCont dc = getDataMC(path+"trigratefile_run"+runs[irun]+".aida",mc[irun], path+"trigratefile_run"+runsBkg[irun]+".aida",outName+"_egs5_"+runs[irun],savePlots,Q_normalization);
                    dpsObs.addPoint();
                    dpsObs.point(irun).coordinate(1).setValue(dc.n_data);
                    dpsObs.point(irun).coordinate(1).setErrorMinus(dc.en_data/2.0);
                    dpsObs.point(irun).coordinate(1).setErrorPlus(dc.en_data/2.0);
                    dpsObs.point(irun).coordinate(0).setValue(cond.getThickness(runs[irun]));
                    dpsObs.point(irun).coordinate(0).setErrorMinus(0.);
                    dpsObs.point(irun).coordinate(0).setErrorPlus(0.);
                    
                    dpsPred.addPoint();
                    dpsPred.point(irun).coordinate(1).setValue(dc.n_pred);
                    dpsPred.point(irun).coordinate(1).setErrorMinus(dc.en_pred/2.0);
                    dpsPred.point(irun).coordinate(1).setErrorPlus(dc.en_pred/2.0);
                    dpsPred.point(irun).coordinate(0).setValue(cond.getThickness(runs[irun]));
                    dpsPred.point(irun).coordinate(0).setErrorMinus(0.5);
                    dpsPred.point(irun).coordinate(0).setErrorPlus(0.5);
                    
                    DataCont dcG4 = getDataMC(path+"trigratefile_run"+runs[irun]+".aida",mcG4[irun], path+"trigratefile_run"+runsBkg[irun]+".aida",outName+"_g4_"+runs[irun],savePlots,Q_normalization);
                    
                    dpsPredG4.addPoint();
                    dpsPredG4.point(irun).coordinate(1).setValue(dcG4.n_pred);
                    dpsPredG4.point(irun).coordinate(1).setErrorMinus(dcG4.en_pred/2.0);
                    dpsPredG4.point(irun).coordinate(1).setErrorPlus(dcG4.en_pred/2.0);
                    dpsPredG4.point(irun).coordinate(0).setValue(cond.getThickness(runs[irun]));
                    dpsPredG4.point(irun).coordinate(0).setErrorMinus(0.5);
                    dpsPredG4.point(irun).coordinate(0).setErrorPlus(0.5);
                    

            }
            

            IPlotter pl_datamc1 = af.createPlotterFactory().create();
            pl_datamc1.setTitle("Data vs MC");
            
            pl_datamc1.style().statisticsBoxStyle().setVisible(false);
            pl_datamc1.createRegions(1,1);
            pl_datamc1.region(0).style().dataStyle().lineStyle().setVisible(false);
            
            IPlotterStyle dataStyle = pl_datamc1.region(0).style();
            dataStyle.dataStyle().lineStyle().setColor("blue");
            
            //pl_datamc1.region(0).style().dataStyle().markerStyle().setShape(strType)
            pl_datamc1.region(0).style().xAxisStyle().setLabel("Target thickness (%r.l.)");
            pl_datamc1.region(0).style().yAxisStyle().setLabel("Counts /"+Q_normalization+"nC");
            pl_datamc1.region(0).plot(dpsObs, dataStyle);
            pl_datamc1.region(0).plot(dpsPred,"mode=overlay");
            pl_datamc1.region(0).plot(dpsPredG4,"mode=overlay");
            pl_datamc1.show();
            if(savePlots) {
                try {
                    pl_datamc1.writeToFile(outName+".png","png");
                } catch (IOException ex) {
                    Logger.getLogger(dataMCPlots.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
    
        }
        
    }

    
    
    public static DataCont getDataMC(String file_data,String file_mc, String file_bkg,String name,boolean savePlots,double Qnorm) {
        
           try {
                String path = "";//plots/20120710_ecal_dataMC/../../";
                
                //String file_mc = "egs_5.5gev_0.016x0_500mb_90na_01_HPS-TestRun-v2_filtered250_multscat.aida";
                //String file_mc = "trigratefile_egs5_merged_5000mb_90na_0x016.aida";
                //String file_mc = "trigratefile_egs5_500mb_90na_01.aida";//trigratefile_egs5_160rl.aida";
                //String file_data = "trigratefile_run1351.aida";
                //String file_bkg = "trigratefile_run1358.aida";
                
                IAnalysisFactory af = IAnalysisFactory.create();
                ITree tree_data = af.createTreeFactory().create(path+ file_data);
                ITree tree_mc = af.createTreeFactory().create(path+ file_mc);
                ITree tree_bkg = af.createTreeFactory().create(path+ file_bkg);
                
                EcalHitMapPlots ecalhtplots = new EcalHitMapPlots();
                
                //String name = "dataMC_clusterE_tophalf_norm";

                
                
                SimpleHPSConditions cond = new SimpleHPSConditions();
                
                int idx = file_data.indexOf("run");
                int run_data = Integer.parseInt(file_data.substring(idx+3,idx+7));
                idx = file_bkg.indexOf("run");
                int run_bkg = Integer.parseInt(file_bkg.substring(idx+3,idx+7));

                //normalize to integrated current of data
                double int_current_mc = getCurrentFromFileName(file_mc);// 90.0; //nC i.e. 1s of beam at 90nA
                double n_bunches_mc = getBunchesFromFileName(file_mc);// e.g. 500mb
                int_current_mc = int_current_mc*n_bunches_mc/500.0;
                
                double k_Q_data = Qnorm/cond.getIntCurrent(run_data);
                
                double k_Q = k_Q_data*cond.getIntCurrent(run_data)/int_current_mc;
                System.out.printf("Run %d: intCurrent %.1fnC intCurrent(MC) %.1fnC Qnorm %.1fnC \n",run_data,cond.getIntCurrent(run_data),int_current_mc,Qnorm);
                System.out.printf("=> k_Q_mc %.1f\n",k_Q );
                System.out.printf("=> k_Q_data %.1f\n",k_Q_data );

                double k_norm_bkg = cond.getIntCurrent(run_data)/cond.getIntCurrent(run_bkg);
                System.out.printf("Run %d: intCurrent %.1fnC \n",run_bkg,cond.getIntCurrent(run_bkg));
                System.out.printf("=> k_norm_bkg %.1f\n",k_norm_bkg );

                double k_rec_data = cond.getRecRate(run_data)/cond.getRate(run_data);
                System.out.printf("Run %d: rate %.1fHz rec rate %.1fHz\n",run_data,cond.getRate(run_data),cond.getRecRate(run_data));
                System.out.printf("=> k_rec_data %.2f\n",k_rec_data );
                
                double k_rec_bkg = cond.getRecRate(run_bkg)/cond.getRate(run_bkg);
                System.out.printf("Run %d: rate %.1fHz rec rate %.1fHz\n",run_data,cond.getRate(run_bkg),cond.getRecRate(run_bkg));
                System.out.printf("=> k_rate_bkg %.2f\n",k_rec_bkg );
                
                
                //Overlay a few histograms
                List<String> histNames = new ArrayList<String>();
                for(int ix=-23;ix<=23;++ix) {
                    for(int iy=-5;iy<=5;++iy) {
                        
                        if(ix<=0) continue;
                        if(iy<=0) continue;
                        
                        histNames.add("Cluster energy x="+ix+" y="+iy);
                        
                    }    
                }
                //System.out.println("Histogram names: "+histNames.toString());
                
                int iadded=0;
                IHistogram1D h_obs=null;
                IHistogram1D h_bkg=null;
                IHistogram1D h_mc=null;
                 
                
                boolean dataToMCNorm = false;
                for(String hname : histNames) {
                    IHistogram1D h_obs_tmp = (IHistogram1D)tree_data.find(hname);
                    IHistogram1D h_bkg_tmp = (IHistogram1D)tree_bkg.find(hname);
                    IHistogram1D h_mc_tmp;
//                    if(file_mc.contains("merged")) {
//                        h_mc_tmp = (IHistogram1D)tree_mc.find(hname+" merged");
//                        System.out.println("adding merged hist \'"+h_mc_tmp.title()+"\'with " + h_mc_tmp.entries());
//                    } else {
                    h_mc_tmp = (IHistogram1D)tree_mc.find(hname);
                    
                    if(iadded==0) {
                        h_obs = ecalhtplots.hf.createCopy(h_obs_tmp.title()+ " sum", h_obs_tmp);
                        h_bkg = ecalhtplots.hf.createCopy(h_bkg_tmp.title()+ " sum", h_bkg_tmp);
                        h_mc = ecalhtplots.hf.createCopy(h_mc_tmp.title()+ " sum", h_mc_tmp);
                    } else {
                    
                        h_obs.add(h_obs_tmp);
                        h_bkg.add(h_bkg_tmp);
                        h_mc.add(h_mc_tmp);
                    }
                    ++iadded;
                    
                }
                
                System.out.println("Added " + iadded + " histograms");
                
                //scale data by dead time efficiency
                h_bkg.scale(1/k_rec_bkg);
                h_obs.scale(1/k_rec_data);
                
                //subtract normalized background from data
                h_bkg.scale(k_norm_bkg);
                IHistogram1D h_data = ecalhtplots.hf.subtract(h_obs.title() + " bkgsubtr", h_obs, h_bkg);
                
                //Normalize data
                h_data.scale(k_Q_data);
                
                if(dataToMCNorm) { //normalize by setting histogram integrals equal
                    h_mc.scale(h_data.sumBinHeights()/h_mc.sumBinHeights());
                } else { //absolute normalization to beam current
                    h_mc.scale(k_Q);
                }
                //System.out.println("mc " + h_mc.entries() + "(" + h_mc.allEntries() + ")" + " obs " + h_obs.entries()+ "(" + h_obs.allEntries() + ")" + " bkg " + h_bkg.entries()+ "(" + h_bkg.allEntries() + ")" + " obs " + h_data.entries()+ "(" + h_data.allEntries() + ")");
                //System.out.println("mc " + h_mc.sumBinHeights() + " obs " + h_obs.sumBinHeights() + " bkg " + h_bkg.sumBinHeights() + " obs " + h_data.sumBinHeights());
                System.out.printf("Run %d: obs %.1f bkg %.1f\t=>\tdata %.1f MC %.1f\t->\tdata/MC=%.2f\n",run_data,h_obs.sumBinHeights(),h_bkg.sumBinHeights(),h_data.sumBinHeights(),h_mc.sumBinHeights(),h_data.sumBinHeights()/h_mc.sumBinHeights());
                h_data.setTitle("Data");
                h_mc.setTitle("MC");
                
                ecalhtplots.plotBasic1D(h_data,h_mc,name, "Cluster energy (MeV)","Counts /"+Qnorm+"nC","data","MC",savePlots);
                
                DataCont dc = new DataCont(h_data.sumBinHeights(),Math.sqrt(h_data.sumBinHeights()),h_mc.sumBinHeights(),Math.sqrt(h_mc.entries())*k_Q);
                
                return dc;
                    
                
            } catch(IOException e) {
                throw new RuntimeException(e);
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
