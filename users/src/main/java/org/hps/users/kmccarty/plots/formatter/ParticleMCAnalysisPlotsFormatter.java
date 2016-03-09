package org.hps.users.kmccarty.plots.formatter;

import java.io.IOException;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;

import org.hps.users.kmccarty.plots.FormattedPlot1D;
import org.hps.users.kmccarty.plots.FormattedPlot2D;
import org.hps.users.kmccarty.plots.PlotFormatModule;
import org.hps.users.kmccarty.plots.PlotsFormatter.ColorStyle;

public class ParticleMCAnalysisPlotsFormatter {
    public static void main(String[] args) throws IllegalArgumentException, IOException {
        // Define the root directory for the plots.
        String rootDir = "D:\\cygwin64\\home\\Kyle\\";
        
        // Define the new name of the file containing the trigger plots.
        String plotFile = rootDir + "moller-mc-out_triggerPlots.aida";
        
        // Get the plots file and open it.
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create(plotFile);
        if(tree == null) { throw new IllegalArgumentException("Unable to load plot file."); }
        
        // Create a plot formatting module.
        PlotFormatModule module = new PlotFormatModule();
        
        // Define the plot color.
        ColorStyle plotColor = ColorStyle.MS_BLUE;
        
        // Define the plots to be read.
        module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MC Analysis/Electron Energy Distribution"),
                plotColor, "Electron Energy (GeV)", "Count", "Electron Energy Distribution"));
        module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MC Analysis/Electron\\Electron Momentum Sum Distribution"),
                plotColor, "Momentum Sum (GeV)", "Count", "Momentum Sum Distribution"));
        module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MC Analysis/Electron\\Electron Pair Angle Distribution"),
                plotColor, "Momentum Sum (GeV)", "Count", "Pair Angle Distribution"));
        module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MC Analysis/Particle x-Momentum Distribution"),
                plotColor, "Momentum (GeV)", "Count", "Particle x-Momentum Distribution"));
        module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MC Analysis/Particle y-Momentum Distribution"),
                plotColor, "Momentum (GeV)", "Count", "Particle y-Momentum Distribution"));
        module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MC Analysis/Particle z-Momentum Distribution"),
                plotColor, "Momentum (GeV)", "Count", "Particle z-Momentum Distribution"));
        module.addPlot2D(new FormattedPlot2D((IHistogram2D) tree.find("MC Analysis/Electron\\Electron 2D Momentum Distribution"),
                true, "Particle 1 Momentum (GeV)", "Particle 2 Momentum (GeV)", "2D Momentum Sum Distribution"));
        module.addPlot2D(new FormattedPlot2D((IHistogram2D) tree.find("MC Analysis/Particle Momentum Distribution"),
                true, "px (GeV)", "py (GeV)", "Particle x/y Momentum Distribution"));
        
        // Display the plots.
        module.displayPlots();
    }
}