package org.hps.users.kmccarty.plots.formatter;

import jas.hist.JASHist1DHistogramStyle;
import jas.hist.JASHist2DHistogramStyle;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;
import hep.aida.ref.plotter.PlotterRegion;

public class InvariantMassPlotsFormatter {
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
        String[] plotFile = {
                rootDir + "temp.aida"
        };
        
        // Define the run numbers for each file.
        String[] runNumber = { "1 Hits", "2 Hits" };
        
        // Define the scaling factors for each plot.
        double scaleFactor = 13.254;
        
        // Get the plots file and open it.
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree[] tree = new ITree[plotFile.length];
        for(int i = 0; i < plotFile.length; i++) {
            tree[i] = af.createTreeFactory().create(plotFile[i]);
            if(tree[i] == null) { throw new IllegalArgumentException("Unable to load plot file."); }
        }
        
        // Get the histograms.
        IHistogram1D[] invariantMassPlots = new IHistogram1D[3];
        invariantMassPlots[0] = (IHistogram1D) tree[0].find("Trident Analysis/Particle Invariant Mass (1 Hit)");
        invariantMassPlots[1] = (IHistogram1D) tree[0].find("Trident Analysis/Particle Invariant Mass (2 Hit)");
        IHistogram1D electronEnergyPlot = (IHistogram1D) tree[0].find("Trident Analysis/Electron Energy");
        IHistogram1D positronEnergyPlot = (IHistogram1D) tree[0].find("Trident Analysis/Positron Energy");
        IHistogram1D energySumPlot = (IHistogram1D) tree[0].find("Trident Analysis/Energy Sum Distribution");
        IHistogram2D energySum2DPlot = (IHistogram2D) tree[0].find("Trident Analysis/2D Energy Distribution");
        IHistogram1D tridentElectronEnergyPlot = (IHistogram1D) tree[0].find("Trident Analysis/Trident Electron Energy");
        IHistogram1D tridentPositronEnergyPlot = (IHistogram1D) tree[0].find("Trident Analysis/Trident Positron Energy");
        
        // Define the plot titles and arrays.
        IHistogram[] plots = { electronEnergyPlot, positronEnergyPlot, energySumPlot, tridentElectronEnergyPlot, tridentPositronEnergyPlot };
        String[] titles = { "Electron Energy", "Positron Energy", "Energy Sum", "Trident Electron Energy", "Trident Positron Energy" };
        String[] xTitles = { "Energy (GeV)", "Energy (GeV)", "Energy Sum (GeV)", "Energy (GeV)", "Energy (GeV)" };
        
        // Re-bin the histograms to have 5-times larger bins. First,
        // get the bin count and upper and lower bounds of the plot.
        int bins = invariantMassPlots[0].axis().bins();
        double low = invariantMassPlots[0].axis().binLowerEdge(0);
        double high = invariantMassPlots[0].axis().binUpperEdge(invariantMassPlots[0].axis().bins() - 1);
        
        // Create new plots with the larger bin sizes.
        AIDA aida = AIDA.defaultInstance();
        IHistogram1D[] newPlot = new IHistogram1D[2];
        newPlot[0] = aida.histogram1D("Particle Invariant Mass (1 Hit)", bins / 5, low, high);
        newPlot[1] = aida.histogram1D("Particle Invariant Mass (2 Hit)", bins / 5, low, high);
        
        // Populate the new plots with the data from the old ones.
        for(int j = 0; j < 2; j++) {
            for(int i = 0; i < bins; i++) {
                int entries = invariantMassPlots[j].binEntries(i);
                double center = invariantMassPlots[j].axis().binCenter(i);
                for(int k = 0; k < entries; k++) {
                    newPlot[j].fill(center);
                }
            }
        }
        
        // Replace the old plots.
        invariantMassPlots = newPlot;
        
        // Create a plotter factory.
        IPlotterFactory plotterFactory = af.createPlotterFactory();
        
        // Format and display the basic histograms.
        for(int i = 0; i < plots.length; i++) {
            // Scale the histogram by the appropriate scaling factor.
            plots[i].scale(1.0 / scaleFactor);
            
            // Create a plotter and plotting region for the plot.
            IPlotter plotter = plotterFactory.create(titles[i]);
            plotter.createRegions(1);
            plotter.region(0).plot(plots[i]);
            
            // Format the axis labels.
            PlotterRegion region = (PlotterRegion) plotter.region(0);
            region.getPlot().setTitle(titles[i]);
            region.getPlot().getXAxis().setLabel(xTitles[i]);
            region.getPlot().getYAxis().setLabel("Rate (Hz)");
            
            // Format the fonts and general plot presentation.
            setDefault1DStyle(region, new ColorStyle[] { ColorStyle.GREY });
            
            // Show the plot.
            plotter.setParameter("plotterWidth", "2000");
            plotter.setParameter("plotterHeight", "1200");
            plotter.show();
        }
        
        // Format and display the 2D histogram.
        energySum2DPlot.scale(1.0 / scaleFactor);
        IPlotter plotter2D = plotterFactory.create("2D Energy Sum");
        plotter2D.createRegions(1);
        plotter2D.region(0).plot(energySum2DPlot);
        
        // Format the axis labels.
        PlotterRegion region2D = (PlotterRegion) plotter2D.region(0);
        region2D.getPlot().setTitle("2D Energy Sum");
        region2D.getPlot().getXAxis().setLabel("Electron Energy (GeV)");
        region2D.getPlot().getYAxis().setLabel("Positron Energy (GeV)");
        
        // Format the fonts and general plot presentation.
        setDefault2DStyle(region2D, false);
        
        // Show the plot.
        plotter2D.setParameter("plotterWidth", "2000");
        plotter2D.setParameter("plotterHeight", "1200");
        plotter2D.show();
        
        // Format and display the histograms.
        for(int i = 0; i < 2; i++) {
            // Scale the histogram by the appropriate scaling factor.
            invariantMassPlots[i].scale(1.0 / scaleFactor);
            
            // Create a plotter and plotting region for the plot.
            IPlotter plotter = plotterFactory.create("Particle Invariant Mass (" + runNumber[i] + ")");
            plotter.createRegions(1);
            plotter.region(0).plot(invariantMassPlots[i]);
            
            // Format the axis labels.
            PlotterRegion region = (PlotterRegion) plotter.region(0);
            region.getPlot().setTitle("Particle Invariant Mass (" + runNumber[i] + ")");
            region.getPlot().getXAxis().setLabel("Invariant Mass (GeV)");
            region.getPlot().getYAxis().setLabel("Rate (Hz)");
            
            // Format the fonts and general plot presentation.
            setDefault1DStyle(region, new ColorStyle[] { ColorStyle.GREY });
            
            // Show the plot.
            plotter.setParameter("plotterWidth", "2000");
            plotter.setParameter("plotterHeight", "1200");
            plotter.show();
        }
        
        // Note which plot is the numerator and which is the denominator.
        int numerator   = 0;
        int denominator = 1;
        
        // Create a new histogram to display the ratios of the rates.
        IHistogram1D ratioPlot = AIDA.defaultInstance().histogram1D("Invariant Mass Ratio (" + runNumber[numerator] + " / "
                + runNumber[denominator] + ")", invariantMassPlots[0].axis().bins(),
                invariantMassPlots[0].axis().lowerEdge(), invariantMassPlots[0].axis().upperEdge());
        
        // Iterate over each bin.
        for(int bin = 0; bin < invariantMassPlots[0].axis().bins(); bin++) {
            // Calculate the ratio.
            double ratio = invariantMassPlots[numerator].binHeight(bin) / invariantMassPlots[denominator].binHeight(bin);
            
            // If the ratio is either not a number of infinite, skip
            // this bin.
            if(Double.isNaN(ratio) || Double.isInfinite(ratio)) { continue; }
            
            // Populate the ratio plot bin.
            ratioPlot.fill(invariantMassPlots[0].axis().binCenter(bin), ratio);
        }
        
        // Create a plotter and plotting region for the plot.
        IPlotter plotter = plotterFactory.create("Invariant Mass Ratio (5411 / 5554)");
        plotter.createRegions(1);
        plotter.region(0).plot(ratioPlot);
        
        // Format the axis labels.
        PlotterRegion region = (PlotterRegion) plotter.region(0);
        region.getPlot().setTitle("Invariant Mass Ratio (" + runNumber[numerator] + " / " + runNumber[denominator] + ")");
        region.getPlot().getXAxis().setLabel("Invariant Mass (GeV)");
        region.getPlot().getXAxis().setMin(0.010);
        region.getPlot().getXAxis().setMax(0.060);
        region.getPlot().getYAxis().setLabel("Ratio");
        
        // Format the fonts and general plot presentation.
        setDefault1DStyle(region, new ColorStyle[] { ColorStyle.GREY });
        
        // Disable the error bars.
        JASHist1DHistogramStyle fillStyle = (JASHist1DHistogramStyle) region.getDataForName(region.getAllDataNames()[0]).getStyle();
        fillStyle.setShowErrorBars(false);
        
        // Show the plot.
        plotter.setParameter("plotterWidth", "2000");
        plotter.setParameter("plotterHeight", "1200");
        plotter.show();
        
        // Close the trees.
        for(int i = 0; i < plotFile.length; i++) {
            tree[i].close();
        }
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
}