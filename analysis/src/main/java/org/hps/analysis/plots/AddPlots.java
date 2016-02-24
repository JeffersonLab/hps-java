package org.hps.analysis.plots;

import hep.aida.IAnalysisFactory;
import hep.aida.IBaseHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogram3D;
import hep.aida.IHistogramFactory;
import hep.aida.IManagedObject;
import hep.aida.ITree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.lcsim.util.aida.AIDA;

/**
 * Command line tool to add together histograms from multiple AIDA files.
 * 
 * @author Jeremy McCormick, SLAC
 *
 */
public class AddPlots {
    
    private static Logger LOGGER = Logger.getLogger(AddPlots.class.getPackage().getName());
        
    private static final Options OPTIONS = new Options();
    static {
        OPTIONS.addOption("o", "output", true, "output file name");
        OPTIONS.addOption("h", "help", false, "print help and exit");
    }
    
    public static void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp("AddPlots [-o outputFile] file1.aida file2.aida [...]", "", OPTIONS, "");
        System.exit(1);
    }
    
    public static void main(String[] args) throws Exception {
        CommandLine cl = new DefaultParser().parse(OPTIONS, args);        
        if (cl.getArgList().isEmpty()) {
            throw new RuntimeException("No input AIDA files to add.");
        }        
        if (cl.getArgList().size() < 2) {
            throw new RuntimeException("Not enough AIDA input files.");
        }
        
        String outputFile = "combined_plots.aida";
        if (cl.hasOption("o")) {
            outputFile = cl.getOptionValue("o");
        }
        
        if (new File(outputFile).exists()) {
            throw new RuntimeException("The output file already exists.");
        }
        
        List<File> inputFiles = new ArrayList<File>();
        for (String arg : cl.getArgList()) {
            File inputFile = new File(arg);
            if (!inputFile.exists()) {
                throw new RuntimeException("The input file " + inputFile.getPath() + " does not exist.");
            }
            inputFiles.add(inputFile);
        }
        
        AIDA aida = AIDA.defaultInstance();
        IAnalysisFactory af = aida.analysisFactory();
        IHistogramFactory hf = aida.histogramFactory();
        ITree tree = af.createTreeFactory().create(inputFiles.get(0).getAbsolutePath());
                        
        List<String> histogramNames = new ArrayList<String>();
        
        String[] objectTypes = tree.listObjectTypes("/", true);
        String[] objectNames = tree.listObjectNames("/", true);
        for (int pathIndex = 0; pathIndex < objectNames.length; pathIndex++) {
            if (objectTypes[pathIndex].startsWith("IHistogram")) {
                histogramNames.add(objectNames[pathIndex]);
            }
            LOGGER.fine(objectNames[pathIndex] + ":" + objectTypes[pathIndex]);
        }
        
        LOGGER.info("found " + histogramNames.size() + " histograms in " + inputFiles.get(0).getPath());

        for (int fileIndex = 1; fileIndex < inputFiles.size(); fileIndex++) {            
            File file = inputFiles.get(fileIndex);
            LOGGER.info("processing " + file.getPath());
            ITree srcTree = af.createTreeFactory().create(file.getAbsolutePath());
            for (String histogramName : histogramNames) {                
                String path = histogramName.substring(0, histogramName.lastIndexOf('/'));
                aida.tree().mkdirs(path);
                IManagedObject object = srcTree.find(histogramName);
                if (object != null) {
                    IBaseHistogram src = (IBaseHistogram) srcTree.find(histogramName);
                    IBaseHistogram target = (IBaseHistogram) tree.find(histogramName);
                    LOGGER.fine("Adding " + histogramName + " from " + file.getPath());
                    add(hf, histogramName, src, target);
                } else {
                    LOGGER.warning("The object " + histogramName + " was not found in " + file.getPath() + ".");
                }
            }
        }

        if (inputFiles.size() == 1) {
            aida.tree().mount("/", tree, "/");
        }
        
        aida.saveAs(outputFile);
        LOGGER.info("wrote plots to " + outputFile);
    }
    
    private static void add(IHistogramFactory factory, String path, IBaseHistogram src, IBaseHistogram target) {
        if (src instanceof IHistogram1D) {
            factory.add(path, (IHistogram1D) src, (IHistogram1D) target);
        } else if (src instanceof IHistogram2D) {
            factory.add(path, (IHistogram2D) src, (IHistogram2D) target);
        } else if (src instanceof IHistogram3D) {
            factory.add(path, (IHistogram3D) src, (IHistogram3D) target);
        }
    }
}
