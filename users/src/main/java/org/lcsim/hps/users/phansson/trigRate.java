/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.ITree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author phansson
 */
public class trigRate {

    
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
        Options options = new Options();
        
        options.addOption(new Option("d",false, "DAQ dead time file"));
        options.addOption(new Option("f",false, "Beam current file"));
        options.addOption(new Option("w",false, "Save all plots to files"));
        options.addOption(new Option("n",false, "Name that will be added to all plots"));
        options.addOption(new Option("e",true, "Cluster energy cut"));
        options.addOption(new Option("s",true, "Top or bottom half"));
        options.addOption(new Option("h",true, "Hide plots"));
        
        return options;
    }
    
    
    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) {
        // TODO code application logic here
        
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        TrigRateAna ana = new TrigRateAna();

        Options options = createCommandLineOptions();
        if (args.length == 0) {
            System.out.println("trigRate [options]");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }
        CommandLineParser parser = new PosixParser();
        
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }
        
        boolean save = false;
        String curFile = "/Users/phansson/work/HPS/software/reco/run/beamCurrents.txt";
        String deadFile = "/Users/phansson/work/HPS/software/reco/run/deadFile.txt";
        String ttFile = "/Users/phansson/work/HPS/software/reco/run/targetThickness.txt";
        
        
        if(cl.hasOption("f")) {
            curFile = cl.getOptionValue("f");
        }

        if(cl.hasOption("d")) {
            deadFile = cl.getOptionValue("d");
        }
        
        int clEnergy = -1;
        if(cl.hasOption("e")) {
            clEnergy = Integer.parseInt(cl.getOptionValue("e"));
        } else {
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }

        System.out.println("Cluster energy " + clEnergy);

        if(clEnergy<0) {
            System.out.println("Error Cluster energy " + clEnergy);
            System.exit(1);
        }
        
        
        String side = "";
        if(cl.hasOption("s")) {
            side = cl.getOptionValue("s");
        } else {
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }

        System.out.println("Side " + side);

        if(side=="") {
            System.out.println("Error side " + side );
            System.exit(1);
        }
        
        if(cl.hasOption("w")) {
            save = Boolean.parseBoolean(cl.getOptionValue("w"));
            
        } 
        boolean hide = false;
        if(cl.hasOption("h")) {
            hide = Boolean.parseBoolean(cl.getOptionValue("h"));
            
        } 


        
        String name = "";
        if(cl.hasOption("n")) {
            name = cl.getOptionValue("s");
        } else {
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }

        System.out.println("Name " + name);

        if(name=="") {
            System.out.println("Error name " + name );
            System.exit(1);
        }

        
        
        
         
        
        ana.addOn(name);
        ana.saveFiles(save);
        ana.hidePlots(hide);
        ana.loadBeamCurrent(curFile);
        ana.loadDAQDeadTime(deadFile);
        ana.setChargeNormalization(90.0);
        ana.setBackgroundRunNrs(1358, 1359);
        ana.loadTargetThickness(ttFile);
        
        
        
        
        
         
        try {
            ITree tree = analysisFactory.createTreeFactory().create("plots/20120618_trig_rate_gainfix_noFirstRow/trigratedriver_hps_001351_withtracking.evio.0.aida");
            //printObjectsInTree(tree);
            //ana.getGainCalibration(tree,side);
            
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        
        
        
        
        
       
            
        
       
        
       
        
        
        try {

            //String path = "plots/20120615_trig_rate/";
            //String path = "plots/20120615_trig_rate_gainfix/";
            //String path = "plots/20120618_trig_rate_gainfix_noFirstRow/";
            String path = "plots/20120618_trig_rate_gainfix/";
            path = "";
            
            List<String> trees = new ArrayList<String>();
            trees.add(path + "trigratefile_001351.aida");//driver_hps_001351.evio.0.aida");  
            //trees.add(path + "trigratedriver_hps_001354.evio.0.aida");  
            //trees.add(path + "trigratedriver_hps_001359.evio.0.aida");  
            //trees.add(path + "trigratedriver_hps_001362.evio.0.aida");
            //trees.add(path + "trigratedriver_hps_001353.evio.0.aida");  
            //trees.add(path + "trigratedriver_hps_001354.evio.1.aida");  
            //trees.add(path + "trigratedriver_hps_001360.evio.0.aida");  
            //trees.add(path + "trigratedriver_hps_001362.evio.1.aida");
            //trees.add(path + "trigratedriver_hps_001353.evio.1.aida");  
            trees.add(path + "trigratefile_001358.aida");
            //trees.add(path + "trigratedriver_hps_001358.evio.0.aida");  
            //trees.add(path + "trigratedriver_hps_001360.evio.1.aida");  
            //trees.add(path + "trigratedriver_hps_001362.evio.2.aida");
            

/*
            trees.add(path + "trigratedriver_1351.aida");
            trees.add(path + "trigratedriver_1353_0.aida");
            trees.add(path + "trigratedriver_1353_1.aida");
            trees.add(path + "trigratedriver_1354_0.aida");
            trees.add(path + "trigratedriver_1354_1.aida");
            trees.add(path + "trigratedriver_1358.aida");
            trees.add(path + "trigratedriver_1359.aida");
            trees.add(path + "trigratedriver_1360_0.aida");
            trees.add(path + "trigratedriver_1360_1.aida");
            trees.add(path + "trigratedriver_1362_0.aida");
            trees.add(path + "trigratedriver_1362_1.aida");
            trees.add(path + "trigratedriver_1362_2.aida");
*/

            
            String hNameHitMap = "Cluster E>" + clEnergy + "GeV hit map good region " + side;
            String hNameHitY = "Cluster E>" + clEnergy + "GeV hit Y good region " + side;
            
            
            
            
            
            
            
            for(String t : trees) {
                ITree tree = analysisFactory.createTreeFactory().create(t);
                //printObjectsInTree(tree);
            
                System.out.println("\"" + t+ "\":");
                //ana.prettyPrintCount(tree);
                int idx = t.lastIndexOf("13");
                int idxE = t.lastIndexOf(".evio.");
                String str = t.substring(idx, idxE);
                str += "_" + t.substring(idxE+6, idxE+7);
                ana.addCount(tree,hNameHitMap,str);
                ana.addPolarHist(tree,hNameHitY,str);
                
                
                //ana.plotEp(tree,str);
                
                              
            }
            ana.mergeCounts();
            ana.normalize();
            ana.subtractBackground();
            ana.scaleToRndCharge();
            ana.scaleDAQDeadTime();
            ana.plotCount();
            ana.makeRatio(0.18, "ratio_w_018");
            ana.plotRatio("ratio_w_018");
            //ana.prettyPrintCount();
            ana.prettyPrintCountList();
            
//            /Cluster E>0.6GeV hit map good region
//            /Cluster E>1GeV hit map good region
//            /Cluster E_corr>0.6GeV hit map good region
//            /Cluster E_corr>1GeV hit map good region

            
            //ana.getGainCalibration(tree);
            
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        
        
        
        
    }
}
