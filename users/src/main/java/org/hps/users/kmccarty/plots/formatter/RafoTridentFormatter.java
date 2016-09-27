package org.hps.users.kmccarty.plots.formatter;

import java.io.IOException;

import org.hps.users.kmccarty.plots.FormattedPlot1D;
import org.hps.users.kmccarty.plots.FormattedPlot2D;
import org.hps.users.kmccarty.plots.PlotFormatModule;
import org.hps.users.kmccarty.plots.PlotsFormatter.ColorStyle;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;

public class RafoTridentFormatter {
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
		String plotFile = rootDir + "mte-out.aida";
		
		// Get the plots file and open it.
		IAnalysisFactory af = IAnalysisFactory.create();
		ITree tree = af.createTreeFactory().create(plotFile);
		if(tree == null) { throw new IllegalArgumentException("Unable to load plot file."); }
		
		// Declare the histogram names.
		String energySumName = "Energy Sum";
		String timeCoincidenceName = "Time Coincidence";
		String timeEnergy2DName = "Cluster Time vs. Cluster Energy";
		String hCoplanaritySum2DName = "Hardware Coplanarity vs. Energy Sum";
		String coplanaritySum2DName = "Calculated Coplanarity vs. Energy Sum";
		String energySum2DName = "Top Cluster Energy vs. Bottom Cluster Energy";
		String fiducial = " (Fiducial Region)";
		
		// Get the histograms.
		IHistogram1D[] energySum = {
				(IHistogram1D) tree.find("Trident/" + energySumName),
				(IHistogram1D) tree.find("Trident/" + energySumName + fiducial)
		};
		IHistogram1D[] timeCoincidence = {
				(IHistogram1D) tree.find("Trident/" + timeCoincidenceName),
				(IHistogram1D) tree.find("Trident/" + timeCoincidenceName + fiducial)
		};
		IHistogram2D[] coplanaritySum = {
				(IHistogram2D) tree.find("Trident/" + coplanaritySum2DName),
				(IHistogram2D) tree.find("Trident/" + coplanaritySum2DName + fiducial)
		};
		IHistogram2D[] hcoplanaritySum = {
				(IHistogram2D) tree.find("Trident/" + hCoplanaritySum2DName),
				(IHistogram2D) tree.find("Trident/" + hCoplanaritySum2DName + fiducial)
		};
		IHistogram2D[] energySum2D = {
				(IHistogram2D) tree.find("Trident/" + energySum2DName),
				(IHistogram2D) tree.find("Trident/" + energySum2DName + fiducial)
		};
		IHistogram2D[] timeEnergy = {
				(IHistogram2D) tree.find("Trident/" + timeEnergy2DName),
				(IHistogram2D) tree.find("Trident/" + timeEnergy2DName + fiducial)
		};
		
		// Define the scaling factors for each plot.
		double scaleFactor = 19000.0 / 9736969.0;
		
		// Define the plot titles and arrays for 1D plots.
		IHistogram1D[][] plots = { energySum, timeCoincidence  };
		String titles[] = { energySumName, timeCoincidenceName, coplanaritySum2DName, hCoplanaritySum2DName, energySum2DName, timeEnergy2DName };
		String[] xTitles = { "Energy (GeV)", "Time Difference (ns)" };
		String yTitle = "Rate (Hz)";
		
		// Define the plot titles and arrays for 2D plots.
		IHistogram2D[][] plots2D = { coplanaritySum, hcoplanaritySum, energySum2D, timeEnergy };
		String[] titles2D = { coplanaritySum2DName, hCoplanaritySum2DName, energySum2DName, timeEnergy2DName };
		String[] xTitles2D = { "Coplanarity (Degrees)", "Coplanarity (Degrees)", "Top Cluster Energy (GeV)", "Time Coincidence (ns)" };
		String[] yTitles2D = { "Energy Sum (GeV)", "Energy Sum (GeV)", "Bottom Cluster Energy (GeV)", "Energy Sum (GeV)" };
		String zTitle2D = "Rate (Hz)";
		
		// Create a plot formatting module.
		PlotFormatModule module = new PlotFormatModule();
		
		// Define the plot color.
		ColorStyle plotColor = ColorStyle.MS_BLUE;
		
		// Define the plots to be read.
		for(int i = 0; i < plots.length; i++) {
			plots[i][0].scale(scaleFactor);
			plots[i][1].scale(scaleFactor);
			module.addPlot1D(new FormattedPlot1D(plots[i][0], plotColor, xTitles[i], yTitle, titles[i]));
			module.addPlot1D(new FormattedPlot1D(plots[i][1], plotColor, xTitles[i],  yTitle, titles[i] + fiducial));
		}
		for(int i = 0; i < plots2D.length; i++) {
			plots2D[i][0].scale(scaleFactor);
			plots2D[i][1].scale(scaleFactor);
			module.addPlot2D(new FormattedPlot2D(plots2D[i][0], false, xTitles2D[i], yTitles2D[i], titles2D[i]));
			module.addPlot2D(new FormattedPlot2D(plots2D[i][1], false, xTitles2D[i], yTitles2D[i], titles2D[i] + fiducial));
		}
		
		// Display the plots.
		//module.displayPlots();
		module.savePlots("C:\\Users\\Kyle\\Desktop\\EnergyShift\\run-5772\\RafoPlots\\");
		module.exportPlots("C:\\Users\\Kyle\\Desktop\\EnergyShift\\run-5772\\RafoPlots\\");
		
		// Close the tree.
		tree.close();
	}
}