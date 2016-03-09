package org.hps.users.kmccarty.plots;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IManagedObject;
import hep.aida.ITree;

public class AddPlots {
    
    public static void main(String[] args) throws IllegalArgumentException, IOException {
        // Define the root directory for the plots.
        String rootDir = null;
        
        // Get the option identifier from the command arguments.
        boolean isHelp = false;
        boolean isFileList = false;
        boolean isDirectory = false;
        if(args.length > 0) {
            if(args[0].compareTo("-h") == 0) { isHelp = true; }
            else if(args[0].compareTo("-f") == 0) { isFileList = true; }
            else if(args[0].compareTo("-d") == 0) { isDirectory = true; }
        } else {
            System.err.println("Insufficient arguments. See \"AddPlots -h\"");
            System.exit(1);
        }
        
        // Process the command line argument.
        List<File> plotFiles = new ArrayList<File>();
        if(isHelp) {
            System.out.println("Usage:");
            System.out.println("\tAddPlots -d [PLOT_DIRECTORY]");
            System.out.println("\tAddPlots -f [PLOT_FILE] [PLOT_FILE] ...");
            System.exit(0);
        } else if(isDirectory) {
            // Make sure that a directory is specified.
            if(args.length < 2) {
                System.err.println("Insufficient arguments. Must specify at least two files.");
                System.exit(1);
            }
            
            // Get the plot directory from the second argument.
            File plotDirectory = new File(args[1]);
            
            // Verify that it exists and is a directory.
            if(!plotDirectory.exists()) {
                System.err.println("File path does not exist.");
                System.exit(1);
            } if(!plotDirectory.isDirectory()) {
                System.err.println("Indicated path must be a directory.");
                System.exit(1);
            }
            
            // Store the root directory.
            rootDir = plotDirectory.getAbsolutePath() + "/";
            
            // Extract the AIDA files from the directory.
            for(File file : plotDirectory.listFiles()) {
                System.out.println(file.getName());
                int indexOfExtension = file.getName().lastIndexOf('.');
                if(indexOfExtension == -1) { continue; }
                if(file.getName().substring(indexOfExtension).compareToIgnoreCase(".aida") == 0) {
                    plotFiles.add(file);
                }
            }
            
            // Debug status print.
            System.out.println("Processing plots in directory \"" + plotDirectory.getAbsolutePath() + "\"");
        } else if(isFileList) {
            // Make sure that at least one file was specified.
            if(args.length < 3) {
                System.err.println("Insufficient arguments. Must specify at least two files.");
                System.exit(1);
            }
            
            // Get the root directory.
            rootDir = System.getProperty("user.dir") + "/";
            
            // Create and verify the specified files.
            for(int i = 1; i < args.length; i++) {
                // Create the file object and make sure that it exists.
                File file = new File(args[i]);
                if(!file.exists()) {
                    System.err.println("Specified file does not exist: " + args[i]);
                    System.exit(1);
                }
                
                // Add it to the file list.
                plotFiles.add(file);
            }
        } else {
            System.err.println("Option \"" + args[0] + "\" is not recognized.");
            System.exit(1);
        }
        
        // Make sure that there are actually files.
        if(plotFiles.isEmpty()) {
            System.err.println("No AIDA files found!");
            System.exit(1);
        }
        
        // Get the plots file and open it.
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create(plotFiles.get(0).getAbsolutePath());
        if(tree == null) { throw new IllegalArgumentException("Unable to load plot file."); }
        
        // Get the histograms names.
        List<String> objectNameList = getTreeFiles(tree);
        
        // Separate the plots into 1D and 2D plots and extract their
        // bin sizes and other properties.
        List<Integer> xBins1D = new ArrayList<Integer>();
        List<Double> xBins1DMin = new ArrayList<Double>();
        List<Double> xBins1DMax = new ArrayList<Double>();
        List<Integer> xBins2D = new ArrayList<Integer>();
        List<Double> xBins2DMin = new ArrayList<Double>();
        List<Double> xBins2DMax = new ArrayList<Double>();
        List<Integer> yBins2D = new ArrayList<Integer>();
        List<Double> yBins2DMin = new ArrayList<Double>();
        List<Double> yBins2DMax = new ArrayList<Double>();
        List<String> histogramNames1D = new ArrayList<String>();
        List<String> histogramNames2D = new ArrayList<String>();
        for(String objectName : objectNameList) {
            // Get the object.
            IManagedObject object = tree.find(objectName);
            
            // If it is a 1D histogram, process it.
            if(object instanceof IHistogram1D) {
                // Add the object to the 1D histogram list.
                histogramNames1D.add(objectName);
                
                // Get the bin size.
                IHistogram1D plot = (IHistogram1D) object;
                xBins1D.add(plot.axis().bins());
                xBins1DMin.add(plot.axis().lowerEdge());
                xBins1DMax.add(plot.axis().upperEdge());
            }
            
            // If it is a 1D histogram, process it.
            else if(object instanceof IHistogram2D) {
                // Add the object to the 2D histogram list.
                histogramNames2D.add(objectName);
                
                // Get the bin size.
                IHistogram2D plot = (IHistogram2D) object;
                xBins2D.add(plot.xAxis().bins());
                xBins2DMin.add(plot.xAxis().lowerEdge());
                xBins2DMax.add(plot.xAxis().upperEdge());
                yBins2D.add(plot.yAxis().bins());
                yBins2DMin.add(plot.yAxis().lowerEdge());
                yBins2DMax.add(plot.yAxis().upperEdge());
            }
        }
        
        // Create plots corresponding to each of the plot objects.
        AIDA aida = AIDA.defaultInstance();
        List<IHistogram1D> histograms1D = new ArrayList<IHistogram1D>(histogramNames1D.size());
        List<IHistogram2D> histograms2D = new ArrayList<IHistogram2D>(histogramNames2D.size());
        for(int i = 0; i < histogramNames1D.size(); i++) {
            IHistogram1D histogram = aida.histogram1D(histogramNames1D.get(i), xBins1D.get(i), xBins1DMin.get(i), xBins1DMax.get(i));
            histograms1D.add(histogram);
        }
        for(int i = 0; i < histogramNames2D.size(); i++) {
            IHistogram2D histogram = aida.histogram2D(histogramNames2D.get(i), xBins2D.get(i), xBins2DMin.get(i), xBins2DMax.get(i), yBins2D.get(i), yBins2DMin.get(i), yBins2DMax.get(i));
            histograms2D.add(histogram);
        }
        
        // Iterate over each file and add their entries to the compiled
        // plots.
        for(File file : plotFiles) {
            // Open the file.
            ITree fileTree = af.createTreeFactory().create(file.getAbsolutePath());
            
            // For each plot, get the equivalent plot from the file
            // and add each bin entry to the compiled plot.
            for(int i = 0; i < histogramNames1D.size(); i++) {
                // Get the histogram object.
                IHistogram1D histogram = (IHistogram1D) fileTree.find(histogramNames1D.get(i));
                
                // Iterate over the bins.
                for(int x = 0; x < xBins1D.get(i); x++) {
                    // Get the entries in this bin and the bin average.
                    int entries = histogram.binEntries(x);
                    double average = histogram.binMean(x);
                    
                    // Add the entries to the compiled plot.
                    for(int j = 0; j < entries; j++) {
                        histograms1D.get(i).fill(average);
                    }
                }
            }
            for(int i = 0; i < histogramNames2D.size(); i++) {
                // Get the histogram object.
                IHistogram2D histogram = (IHistogram2D) fileTree.find(histogramNames2D.get(i));
                
                // Iterate over the bins.
                for(int x = 0; x < xBins2D.get(i); x++) {
                    for(int y = 0; y < yBins2D.get(i); y++) {
                        // Get the entries in this bin and the bin average.
                        int entries = histogram.binEntries(x, y);
                        double averageX = histogram.binMeanX(x, y);
                        double averageY = histogram.binMeanY(x, y);
                        
                        // Add the entries to the compiled plot.
                        for(int j = 0; j < entries; j++) {
                            histograms2D.get(i).fill(averageX, averageY);
                        }
                    }
                }
            }
        }
        
        // Save the compiled plots to a new file.
        aida.saveAs(rootDir + "compiled-plots.aida");
        System.out.println("Plots written to path " + rootDir + "compiled-plots.aida");
    }
    
    private static final List<String> getTreeFiles(ITree tree) {
        return getTreeFiles(tree, "/");
    }
    
    private static final List<String> getTreeFiles(ITree tree, String rootDir) {
        // Make a list to contain the plot names.
        List<String> list = new ArrayList<String>();
        
        // Iterate over the objects at the indicated directory of the tree.
        String objectNames[] = tree.listObjectNames(rootDir);
        for(String objectName : objectNames) {
            // Convert the object name to a char array and check the
            // last character. Directories end in '/'.
            char[] plotChars = objectName.toCharArray();
            
            // If the object is a directory, process any objects inside
            // of it as well.
            if(plotChars[plotChars.length - 1] == '/') {
                List<String> dirList = getTreeFiles(tree, objectName);
                list.addAll(dirList);
            }
            
            // Otherwise, just add the object to the list.
            else { list.add(objectName); }
        }
        
        // Return the compiled list.
        return list;
    }
}