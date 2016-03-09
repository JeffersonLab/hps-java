package org.hps.users.kmccarty.plots.formatter;

import jas.hist.JASHist1DHistogramStyle;
import jas.hist.JASHist2DHistogramStyle;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.List;

import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;
import hep.aida.ref.plotter.PlotterRegion;

public class MTEPlotFormatter {
    // Define plot fonts.
    private static final Font BASIC_FONT = new Font("Calibri", Font.PLAIN, 20);
    private static final Font AXIS_FONT  = new Font("Calibri", Font.BOLD,  25);
    private static final Font TITLE_FONT = new Font("Calibri", Font.BOLD,  35);
    
    // Defines the color style options for plot data.
    private enum ColorStyle {
         MS_BLUE(new Color( 79, 129, 189), new Color( 36,  64,  97)), MS_ORANGE(new Color(247, 150,  70), new Color(152,  72,   6)),
          MS_RED(new Color(192,  80,  77), new Color( 99,  36,  35)),      GREY(new Color(166, 166, 166), new Color( 89,  89,  89)),
        MS_GREEN(new Color(155, 187,  89), new Color( 79,  98,  40)),   CRIMSON(new Color(161,   0,   0), new Color(104,   0,   0)),
            RUST(new Color(161,  80,   0), new Color(105,  80,   0)),    YELLOW(new Color(161, 161,   0), new Color(122, 109,   8)),
          FOREST(new Color( 65, 102,   0), new Color( 37,  79,   0)),     GREEN(new Color(  7, 132,  70), new Color(  7,  82,  30)),
            TEAL(new Color(  0, 130, 130), new Color(  0,  90, 100)),  CERULEAN(new Color(  0,  86, 130), new Color(  0,  28,  83)),
            BLUE(new Color(  0,  33, 203), new Color(  0,   0, 137)),    INDIGO(new Color( 68,  10, 127), new Color(  0,   0,  61)),
          PURPLE(new Color(106,   0, 106), new Color( 63,   0,  56)),   FUSCHIA(new Color(119,   0,  60), new Color( 60,   0,  60));
        
        private final Color fillColor;
        private final Color lineColor;
        
        private ColorStyle(Color fillColor, Color lineColor) {
            this.fillColor = fillColor;
            this.lineColor = lineColor;
        }
        
        public Color getFillColor() { return fillColor; }
        
        public Color getLineColor() { return lineColor; }
    };
        
    /**
     * Loads all plots in a file and formats them according to the
     * indicated style.
     * @param args - Unused default executable parameter.
     * @throws IOException Occurs if there is an issue opening the file.
     */
    public static void main(String[] args) throws IOException {
        // Define the root directory for the plots.
        String rootDir = "D:\\cygwin64\\home\\Kyle\\";
        
        // Define the new name of the file containing the trigger plots.
        String plotFile = rootDir + "temp.aida";
        
        // Define the scaling factors for each plot.
        double scaleFactor = 1;
        
        // Get the plots file and open it.
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create(plotFile);
        if(tree == null) { throw new IllegalArgumentException("Unable to load plot file."); }
        
        // Define index references for each event type.
        int MOLLER  = 0;
        int TRIDENT = 1;
        int ELASTIC = 2;
        
        // Get the histograms.
        IHistogram1D[] trackCountPlots = new IHistogram1D[3];
        trackCountPlots[MOLLER]  = (IHistogram1D) tree.find("MTE Analysis/Møller Event Tracks");
        trackCountPlots[TRIDENT] = (IHistogram1D) tree.find("MTE Analysis/Trident Event Tracks");
        trackCountPlots[ELASTIC] = (IHistogram1D) tree.find("MTE Analysis/Elastic Event Tracks");
        
        IHistogram1D[] energyPlots = new IHistogram1D[3];
        energyPlots[MOLLER]  = (IHistogram1D) tree.find("MTE Analysis/Møller Electron Energy Distribution");
        energyPlots[TRIDENT] = (IHistogram1D) tree.find("MTE Analysis/Trident Electron Energy Distribution");
        energyPlots[ELASTIC] = (IHistogram1D) tree.find("MTE Analysis/Elastic Energy Distribution");
        
        IHistogram1D[] energySumPlots = new IHistogram1D[2];
        energySumPlots[MOLLER]  = (IHistogram1D) tree.find("MTE Analysis/Møller Energy Sum Distribution");
        energySumPlots[TRIDENT] = (IHistogram1D) tree.find("MTE Analysis/Trident Energy Sum Distribution");
        
        IHistogram2D[] energy2DPlots = new IHistogram2D[2];
        energy2DPlots[MOLLER]  = (IHistogram2D) tree.find("MTE Analysis/Møller 2D Energy Distribution");
        energy2DPlots[TRIDENT] = (IHistogram2D) tree.find("MTE Analysis/Trident 2D Energy Distribution");
        
        // Create a plotter factory.
        IPlotterFactory plotterFactory = af.createPlotterFactory();
        
        // Format the track count plots.
        for(IHistogram1D trackCountPlot : trackCountPlots) {
            // Create a plotter and plotting region for the plot.
            IPlotter plotter = plotterFactory.create(trackCountPlot.title());
            plotter.createRegions(1);
            plotter.region(0).plot(trackCountPlot);
            
            // Format the axis labels.
            PlotterRegion region = (PlotterRegion) plotter.region(0);
            region.getPlot().setTitle(trackCountPlot.title());
            region.getPlot().getXAxis().setLabel("Number of Tracks");
            region.getPlot().getYAxis().setLabel("Count");
            
            // Format the fonts and general plot presentation.
            setDefault1DStyle(region, new ColorStyle[] { ColorStyle.GREY });
            
            // Show the plot.
            plotter.setParameter("plotterWidth", "2000");
            plotter.setParameter("plotterHeight", "1200");
            plotter.show();
        }
        
        // Format the electron energy plots.
        for(IHistogram1D energyPlot : energyPlots) {
            // Create a plotter and plotting region for the plot.
            IPlotter plotter = plotterFactory.create(energyPlot.title());
            plotter.createRegions(1);
            plotter.region(0).plot(energyPlot);
            
            // Format the axis labels.
            PlotterRegion region = (PlotterRegion) plotter.region(0);
            region.getPlot().setTitle(energyPlot.title());
            region.getPlot().getXAxis().setLabel("Track Energy (GeV)");
            region.getPlot().getYAxis().setLabel("Count");
            
            // Format the fonts and general plot presentation.
            setDefault1DStyle(region, new ColorStyle[] { ColorStyle.GREY });
            
            // Show the plot.
            plotter.setParameter("plotterWidth", "2000");
            plotter.setParameter("plotterHeight", "1200");
            plotter.show();
        }
        
        // Format the energy sum plots.
        for(IHistogram1D energySumPlot : energySumPlots) {
            // Create a plotter and plotting region for the plot.
            IPlotter plotter = plotterFactory.create(energySumPlot.title());
            plotter.createRegions(1);
            plotter.region(0).plot(energySumPlot);
            
            // Format the axis labels.
            PlotterRegion region = (PlotterRegion) plotter.region(0);
            region.getPlot().setTitle(energySumPlot.title());
            region.getPlot().getXAxis().setLabel("Track Energy (GeV)");
            region.getPlot().getYAxis().setLabel("Count");
            
            // Format the fonts and general plot presentation.
            setDefault1DStyle(region, new ColorStyle[] { ColorStyle.GREY });
            
            // Show the plot.
            plotter.setParameter("plotterWidth", "2000");
            plotter.setParameter("plotterHeight", "1200");
            plotter.show();
        }
        
        // Format the 2D energy sum plots.
        for(IHistogram2D energy2DPlot : energy2DPlots) {
            // Create a plotter and plotting region for the plot.
            IPlotter plotter = plotterFactory.create(energy2DPlot.title());
            plotter.createRegions(1);
            plotter.region(0).plot(energy2DPlot);
            
            // Format the axis labels.
            PlotterRegion region = (PlotterRegion) plotter.region(0);
            region.getPlot().setTitle(energy2DPlot.title());
            region.getPlot().getXAxis().setLabel("First Track Energy (GeV)");
            region.getPlot().getYAxis().setLabel("Second Track Energy (GeV)");
            
            
            // Format the fonts and general plot presentation.
            setDefault2DStyle(region, false);
            
            // Show the plot.
            plotter.setParameter("plotterWidth", "2000");
            plotter.setParameter("plotterHeight", "1200");
            plotter.show();
        }
        
        // Disable the error bars.
        //JASHist1DHistogramStyle fillStyle = (JASHist1DHistogramStyle) region.getDataForName(region.getAllDataNames()[0]).getStyle();
        //fillStyle.setShowErrorBars(false);
        
        // Close the tree.
        tree.close();
    }
    
    /**
     * Sets the plot display formatting for 1D plots.
     * @param region - The plotter region to format.
     * @param color - The data color settings to use.
     */
    private static final void setDefault1DStyle(PlotterRegion region, ColorStyle[] color) {
        // Get the names of each plot on in the region.
        String[] dataNames = region.getAllDataNames();
        
        // Check whether this is an overlay plot. Overlay plots contain
        // more than one data name.
        boolean overlay = (dataNames.length > 1 ? true : false);
        
        // Iterate over each plot in the region.
        for(int i = 0; i < dataNames.length; i++) {
            // Set the overlay style if needed.
            if(overlay) {
                // Get the fill style for the current data type.
                JASHist1DHistogramStyle fillStyle = (JASHist1DHistogramStyle) region.getDataForName(dataNames[i]).getStyle();
                
                // Set the histogram style to display thick-lined bars
                // with no fill. The color is set by the "color" argument.
                fillStyle.setHistogramFill(false);
                fillStyle.setHistogramBarLineWidth(3);
                fillStyle.setHistogramBarLineColor(color[i].getFillColor());
                
                // Set the legend text style.
                region.getPlot().getLegend().setFont(new Font("Calibri", Font.PLAIN, 20));
            }
            
            // Otherwise, set the fill style for a single plot.
            else {
                // Get the fill style for the current data type.
                JASHist1DHistogramStyle fillStyle = (JASHist1DHistogramStyle) region.getDataForName(dataNames[i]).getStyle();
                
                // Set the histogram style to display thick-lined bars
                // with a fill color. The colors are defined by the
                // "color" argument.
                fillStyle.setHistogramBarLineWidth(3);
                fillStyle.setHistogramBarColor(color[i].getFillColor());
                fillStyle.setHistogramBarLineColor(color[i].getLineColor());
            }
            
            // Set the statistics box style.
            region.getPlot().getStats().setVisible(true);
            region.getPlot().getStats().setFont(BASIC_FONT);
            
            // Set the title font.
            region.getPlot().getTitleObject().setFont(TITLE_FONT);
            
            // Set the axis tick-mark fonts.
            region.getPlot().getXAxis().setFont(BASIC_FONT);
            region.getPlot().getYAxis().setFont(BASIC_FONT);
            region.getPlot().getXAxis().getLabelObject().setFont(AXIS_FONT);
            region.getPlot().getYAxis().getLabelObject().setFont(AXIS_FONT);
        }
    }
    
    /**
     * Sets the plot display formatting for 1D plots.
     * @param region - The plotter region to format.
     */
    private static final void setDefault2DStyle(PlotterRegion region, boolean logarithmic) {
        // Get the fill style object. 2D plots should never be overlay
        // plots, so there should only ever be one data name.
        JASHist2DHistogramStyle fillStyle = (JASHist2DHistogramStyle) region.getDataForName(region.getAllDataNames()[0]).getStyle();
        
        // Set the fill style for a two-dimensional plot.
        if(logarithmic) { fillStyle.setLogZ(true); }
        fillStyle.setHistStyle(JASHist2DHistogramStyle.STYLE_COLORMAP);
        fillStyle.setColorMapScheme(JASHist2DHistogramStyle.COLORMAP_RAINBOW);
        
        // Make the statistics box invisible.
        region.getPlot().getStats().setVisible(false);
        
        // Set the general plot font (which is also the z-axis font).
        region.getPlot().setFont(BASIC_FONT);
        
        // Set the title font.
        region.getPlot().getTitleObject().setFont(TITLE_FONT);
        
        // Set the axis tick-mark fonts.
        region.getPlot().getXAxis().setFont(BASIC_FONT);
        region.getPlot().getYAxis().setFont(BASIC_FONT);
        region.getPlot().getXAxis().getLabelObject().setFont(AXIS_FONT);
        region.getPlot().getYAxis().getLabelObject().setFont(AXIS_FONT);
    }
    
    /**
     * Recursive method that gets all object names from a tree that
     * are not directories. Method should not be called directly, but
     * rather called only through the <code>getHistograms(ITree)</code>
     * method.
     * @param tree - The tree from which to obtain the object names.
     * @param directory - The directory in which to search for objects.
     * @param list - The list in which to place the objects.
     * @return Returns the <code>List</code> collection that was given
     * as an argument.
     */
    private static final List<String> getHistograms(ITree tree, String directory, List<String> list) {
        // Get the list of objects in the directory.
        String[] treeObjects = tree.listObjectNames(directory);
        
        // Print the objects.
        for(String objectName : treeObjects) {
            // Check if the object is a directory.
            boolean isDirectory = isDirectory(objectName);
            
            // If the object is a directory, get the histograms from it.
            if(isDirectory) {
                getHistograms(tree, objectName, list);
            }
            
            // If the object is a plot, add it to the list.
            else { list.add(objectName); }
        }
        
        // Return the list.
        return list;
    }
    
    /**
     * Checks whether a tree object is a directory.
     * @param object - The object to check.
     * @return Returns <code>true</code> if the object is a directory
     * and <code>false</code> otherwise.
     */
    private static final boolean isDirectory(String object) {
        return (object.toCharArray()[object.length() - 1] == '/');
    }
}