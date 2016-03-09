package org.hps.users.kmccarty.plots.formatter;

import java.io.IOException;

import org.hps.users.kmccarty.plots.FormattedPlot1D;
import org.hps.users.kmccarty.plots.FormattedPlot2D;
import org.hps.users.kmccarty.plots.PlotFormatModule;
import org.hps.users.kmccarty.plots.PlotsFormatter;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;

public class SingleTriggerPlotsFormatter {
    
    public static void main(String[] args) throws IllegalArgumentException, IOException {
        // Define the root directory for the plots.
        String rootDir = "D:\\cygwin64\\home\\Kyle\\";
        
        // Define the new name of the file containing the trigger plots.
        String plotFile = rootDir + "trident-readout-full.aida";
        
        // Get the plots file and open it.
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create(plotFile);
        if(tree == null) { throw new IllegalArgumentException("Unable to load plot file."); }
        
        // Define plots variables.
        int UNCUT     = 0;
        int TRIGGERED = 1;
        String[] plotsDir = { "NoCuts/", "PassedAll/" };
        int PLOT_HIT_COUNT      = 0;
        int PLOT_SEED_ENERGY    = 1;
        int PLOT_CLUSTER_ENERGY = 2;
        int PLOT_COPLANARITY    = 3;
        int PLOT_ENERGY_SUM     = 4;
        int PLOT_ENERGY_DIFF    = 5;
        int PLOT_ENERGY_SLOPE   = 6;
        int PLOT_SEED_DIST      = 0;
        int PLOT_ENERGY_SUM_2D  = 1;
        
        // Define the internal plot names.
        String[] plotNameInternal1D = new String[7];
        String[] plotNameInternal2D = new String[2];
        plotNameInternal1D[PLOT_HIT_COUNT]      = "Cluster Hit Count";
        plotNameInternal1D[PLOT_SEED_ENERGY]    = "Cluster Seed Energy";
        plotNameInternal1D[PLOT_CLUSTER_ENERGY] = "Cluster Total Energy";
        plotNameInternal1D[PLOT_COPLANARITY]    = "Pair Coplanarity";
        plotNameInternal1D[PLOT_ENERGY_SUM]     = "Pair Energy Sum";
        plotNameInternal1D[PLOT_ENERGY_DIFF]    = "Pair Energy Difference";
        plotNameInternal1D[PLOT_ENERGY_SLOPE]   = "Pair Energy Slope";
        plotNameInternal2D[PLOT_SEED_DIST]      = "Cluster Seed";
        plotNameInternal2D[PLOT_ENERGY_SUM_2D]  = "Pair Energy Sum 2D";
        
        // Define the plot display names.
        String[] plotName1D = new String[7];
        String[] plotName2D = new String[2];
        for(int j = 0; j < plotNameInternal1D.length; j++) {
            plotName1D[j] = plotNameInternal1D[j];
        }
        for(int j = 0; j < plotNameInternal2D.length; j++) {
            plotName2D[j] = plotNameInternal2D[j];
        }
        plotName1D[PLOT_ENERGY_SUM]    = "1D Pair Energy Sum";
        plotName2D[PLOT_SEED_DIST]     = "Cluster Seed Distribution";
        plotName2D[PLOT_ENERGY_SUM_2D] = "2D Pair Energy Sum";
        
        String[] xTitles1D = new String[plotName1D.length];
        String[] xTitles2D = new String[plotName2D.length];
        xTitles1D[PLOT_HIT_COUNT]      = "Hit Count";
        xTitles1D[PLOT_SEED_ENERGY]    = "Seed Energy (GeV)";
        xTitles1D[PLOT_CLUSTER_ENERGY] = "Cluster Energy (GeV)";
        xTitles1D[PLOT_COPLANARITY]    = "Coplanarity Angle (Degrees)";
        xTitles1D[PLOT_ENERGY_SUM]     = "Energy Sum (GeV)";
        xTitles1D[PLOT_ENERGY_DIFF]    = "Energy Difference (GeV)";
        xTitles1D[PLOT_ENERGY_SLOPE]   = "Energy Slope (GeV)";
        xTitles2D[PLOT_SEED_DIST]      = "x-Index";
        xTitles2D[PLOT_ENERGY_SUM_2D]  = "First Cluster Energy (GeV)";
        String yTitle1D = "Count";
        String[] yTitles2D = new String[plotName2D.length];
        yTitles2D[PLOT_SEED_DIST]      = "y-Index";
        yTitles2D[PLOT_ENERGY_SUM_2D]  = "Second Cluster Energy (GeV)";
        
        // Define axis ranges.
        double[] axisRanges1D = new double[plotName1D.length];
        axisRanges1D[PLOT_HIT_COUNT]      = -1;
        axisRanges1D[PLOT_SEED_ENERGY]    = 1.1;
        axisRanges1D[PLOT_CLUSTER_ENERGY] = 1.1;
        axisRanges1D[PLOT_COPLANARITY]    = 180;
        axisRanges1D[PLOT_ENERGY_SUM]     = 2.2;
        axisRanges1D[PLOT_ENERGY_DIFF]    = 1.1;
        axisRanges1D[PLOT_ENERGY_SLOPE]   = 2.4;
        double[] xAxisRanges2D = new double[plotName2D.length];
        double[] yAxisRanges2D = new double[plotName2D.length];
        xAxisRanges2D[PLOT_SEED_DIST]      = -1;
        xAxisRanges2D[PLOT_ENERGY_SUM_2D]  = 1.1;
        yAxisRanges2D[PLOT_SEED_DIST]      = -1;
        yAxisRanges2D[PLOT_ENERGY_SUM_2D]  = 1.1;
        
        // Define the plot names.
        String[][] plotLocations1D = new String[plotsDir.length][plotNameInternal1D.length];
        String[][] plotLocations2D = new String[plotsDir.length][plotNameInternal2D.length];
        for(int i = 0; i < plotsDir.length; i++) {
            for(int j = 0; j < plotNameInternal1D.length; j++) {
                plotLocations1D[i][j] = plotsDir[i] + plotNameInternal1D[j];
            }
        }
        for(int i = 0; i < plotsDir.length; i++) {
            for(int j = 0; j < plotNameInternal2D.length; j++) {
                plotLocations2D[i][j] = plotsDir[i] + plotNameInternal2D[j];
            }
        }
        
        // Create a plot formatting module.
        PlotFormatModule module = new PlotFormatModule();
        
        // Load the plot objects.
        for(int i = 0; i < plotName1D.length; i++) {
            // Get the uncut and triggered plots.
            IHistogram1D uncutPlot = (IHistogram1D) tree.find(plotLocations1D[UNCUT][i]);
            IHistogram1D triggeredPlot = (IHistogram1D) tree.find(plotLocations1D[TRIGGERED][i] + " (Passed All Cuts)");
            
            // Make a formatted plot for each.
            FormattedPlot1D uncutFormattedPlot;
            FormattedPlot1D triggeredFormattedPlot;
            if(axisRanges1D[i] != -1) {
                uncutFormattedPlot = new FormattedPlot1D(uncutPlot, PlotsFormatter.ColorStyle.GREY, xTitles1D[i], yTitle1D, plotName1D[i] + " (No Cuts)", axisRanges1D[i]);
                triggeredFormattedPlot = new FormattedPlot1D(triggeredPlot, PlotsFormatter.ColorStyle.MS_GREEN, xTitles1D[i], yTitle1D, plotName1D[i] + " (Triggered)", axisRanges1D[i]);
            } else {
                uncutFormattedPlot = new FormattedPlot1D(uncutPlot, PlotsFormatter.ColorStyle.GREY, xTitles1D[i], yTitle1D, plotName1D[i] + " (No Cuts)");
                triggeredFormattedPlot = new FormattedPlot1D(triggeredPlot, PlotsFormatter.ColorStyle.MS_GREEN, xTitles1D[i], yTitle1D, plotName1D[i] + " (Triggered)");
            }
            
            // Add the plots to the module.
            module.addPlot1D(uncutFormattedPlot);
            module.addPlot1D(triggeredFormattedPlot);
        }
        for(int i = 0; i < plotName2D.length; i++) {
            // Get the uncut and triggered plots.
            IHistogram2D uncutPlot = (IHistogram2D) tree.find(plotLocations2D[UNCUT][i]);
            IHistogram2D triggeredPlot = (IHistogram2D) tree.find(plotLocations2D[TRIGGERED][i] + " (Passed All Cuts)");
            
            // Make a formatted plot for each.
            FormattedPlot2D uncutFormattedPlot;
            FormattedPlot2D triggeredFormattedPlot;
            if(xAxisRanges2D[i] != -1) {
                uncutFormattedPlot = new FormattedPlot2D(uncutPlot, true, xTitles2D[i], yTitles2D[i], plotName2D[i] + " (No Cuts)", xAxisRanges2D[i], yAxisRanges2D[i]);
                triggeredFormattedPlot = new FormattedPlot2D(triggeredPlot, true, xTitles2D[i], yTitles2D[i], plotName2D[i] + " (Triggered)", xAxisRanges2D[i], yAxisRanges2D[i]);
            } else {
                uncutFormattedPlot = new FormattedPlot2D(uncutPlot, true, xTitles2D[i], yTitles2D[i], plotName2D[i] + " (No Cuts)");
                triggeredFormattedPlot = new FormattedPlot2D(triggeredPlot, true, xTitles2D[i], yTitles2D[i], plotName2D[i] + " (Triggered)");
            }
            
            // Add the plots to the module.
            module.addPlot2D(uncutFormattedPlot);
            module.addPlot2D(triggeredFormattedPlot);
        }
        
        // Save the plots.
        module.savePlots("C:\\Users\\Kyle\\Desktop\\EnergyShift\\MonteCarlo\\Trident\\Trigger\\");
    }
}