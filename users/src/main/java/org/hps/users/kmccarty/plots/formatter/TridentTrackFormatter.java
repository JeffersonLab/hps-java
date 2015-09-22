package org.hps.users.kmccarty.plots.formatter;

import java.io.IOException;

import org.hps.users.kmccarty.plots.PlotsFormatter;
import org.hps.users.kmccarty.plots.PlotsFormatter.ColorStyle;
import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;
import hep.aida.ref.plotter.PlotterRegion;

public class TridentTrackFormatter {
	/**
	 * Loads all plots in a file and formats them according to the
	 * indicated style.
	 * @param args - Unused default executable parameter.
	 * @throws IOException Occurs if there is an issue opening the file.
	 */
	public static void main(String[] args) throws IOException {
		// Define the root directory for the plots.
		String rootDir = "D:\\cygwin64\\home\\Kyle\\tmp\\";
		
		// Define the new name of the file containing the trigger plots.
		String plotFile = rootDir + "trident-out.aida";
		
		// Get the plots file and open it.
		IAnalysisFactory af = IAnalysisFactory.create();
		ITree tree = af.createTreeFactory().create(plotFile);
		if(tree == null) { throw new IllegalArgumentException("Unable to load plot file."); }
		
		// Declare the histogram names.
		String trackName = "Tracks in Event (All)";
		String posTrackName = "Tracks in Event (Positive)";
		String negTrackName = "Tracks in Event (Negative)";
		String posMomentumName = "Momentum (Positive)";
		String negMomentumName = "Momentum (Negative)";
		String energySumName = "Energy Sum";
		String momentumSumName = "Momentum Sum";
		String energyMomentumDiffName = "Energy-Momentum Difference";
		String invariantMassName = "Invariant Mass";
		String energySum2DName = "2D Energy Sum";
		String momentumSum2DName = "2D Momentum Sum";
		String positionName = "Track Cluster Position";
		
		// Get the histograms.
		IHistogram1D[] tracks = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + trackName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + trackName)
		};
		IHistogram1D[] posTracks = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + posTrackName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + posTrackName)
		};
		IHistogram1D[] negTracks = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + negTrackName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + negTrackName)
		};
		IHistogram1D[] posMomentum = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + posMomentumName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + posMomentumName)
		};
		IHistogram1D[] negMomentum = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + negMomentumName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + negMomentumName)
		};
		IHistogram1D[] energySum = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + energySumName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + energySumName)
		};
		IHistogram1D[] momentumSum = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + momentumSumName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + momentumSumName)
		};
		IHistogram1D[] energyMomentumDiff = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + energyMomentumDiffName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + energyMomentumDiffName)
		};
		IHistogram1D[] invariantMass = {
				(IHistogram1D) tree.find("Trident Analysis/All/" + invariantMassName),
				(IHistogram1D) tree.find("Trident Analysis/Cluster/" + invariantMassName)
		};
		IHistogram2D[] energySum2D = {
				(IHistogram2D) tree.find("Trident Analysis/All/" + energySum2DName),
				(IHistogram2D) tree.find("Trident Analysis/Cluster/" + energySum2DName)
		};
		IHistogram2D[] momentumSum2D = {
				(IHistogram2D) tree.find("Trident Analysis/All/" + momentumSum2DName),
				(IHistogram2D) tree.find("Trident Analysis/Cluster/" + momentumSum2DName)
		};
		IHistogram2D[] position = {
				(IHistogram2D) tree.find("Trident Analysis/All/" + positionName),
				(IHistogram2D) tree.find("Trident Analysis/Cluster/" + positionName)
		};
		
		// Re-bin the histograms to have 5-times larger bins. First,
		// get the bin count and upper and lower bounds of the plot.
		int bins = invariantMass[0].axis().bins();
		double low = invariantMass[0].axis().binLowerEdge(0);
		double high = invariantMass[0].axis().binUpperEdge(invariantMass[0].axis().bins() - 1);
		
		// Create new plots with the larger bin sizes.
		AIDA aida = AIDA.defaultInstance();
		IHistogram1D[] newPlot = new IHistogram1D[2];
		newPlot[0] = aida.histogram1D(invariantMassName, bins / 5, low, high);
		newPlot[1] = aida.histogram1D("Cluster " + invariantMassName, bins / 5, low, high);
		
		// Populate the new plots with the data from the old ones.
		for(int j = 0; j < 2; j++) {
			for(int i = 0; i < bins; i++) {
				int entries = invariantMass[j].binEntries(i);
				double center = invariantMass[j].axis().binCenter(i);
				for(int k = 0; k < entries; k++) {
					newPlot[j].fill(center);
				}
			}
		}
		
		// Replace the old plots.
		invariantMass = newPlot;
		
		// Define the scaling factors for each plot.
		double scaleFactor = 1;
		
		// Define the plot titles and arrays for 1D plots.
		IHistogram[][] plots = { tracks, posTracks, negTracks, posMomentum, negMomentum, energySum, momentumSum, energyMomentumDiff, invariantMass };
		String[] titles = { trackName, posTrackName, negTrackName, posMomentumName, negMomentumName, energySumName, momentumSumName,
				energyMomentumDiffName, invariantMassName };
		String[] xTitles = { "Tracks", "Tracks", "Tracks", "Momentum (GeV)", "Momentum (GeV)", "Energy Sum (GeV)", "Momentum Sum (GeV)",
				"|E_Cluster - P_Track| (GeV)", "Invariant Mass (GeV)" };
		String yTitle = "Count";
		
		// Define the plot titles and arrays for 2D plots.
		IHistogram2D[][] plots2D = { energySum2D, momentumSum2D, position };
		String[] titles2D = { energySum2DName, momentumSum2DName, positionName };
		String[] xTitles2D = { "Positive Cluster Energy", "Positive Track Momentum", "x-Index" };
		String[] yTitles2D = { "Negative Cluster Energy", "Negative Track Momentum", "y-Index" };
		String zTitle2D = "Count";
		
		// Create a plotter factory.
		IPlotterFactory plotterFactory = af.createPlotterFactory();
		
		// Format and display the basic histograms.
		for(int i = 0; i < plots.length; i++) {
			for(int j = 0; j < 2; j++) {
				// Scale the histogram by the appropriate scaling factor.
				plots[i][j].scale(1.0 / scaleFactor);
				
				// Create a plotter and plotting region for the plot.
				IPlotter plotter = plotterFactory.create((j == 1 ? "Cluster " : "") + titles[i]);
				plotter.createRegions(1);
				plotter.region(0).plot(plots[i][j]);
				
				// Format the axis labels.
				PlotterRegion region = (PlotterRegion) plotter.region(0);
				region.getPlot().setTitle((j == 1 ? "Cluster " : "") + titles[i]);
				region.getPlot().getXAxis().setLabel(xTitles[i]);
				region.getPlot().getYAxis().setLabel(yTitle);
				
				// Format the fonts and general plot presentation.
				PlotsFormatter.setDefault1DStyle(region, new ColorStyle[] { ColorStyle.GREY });
				
				// Show the plot.
				plotter.setParameter("plotterWidth", "2000");
				plotter.setParameter("plotterHeight", "1200");
				plotter.show();
			}
		}
		
		// Format and display the 2D histogram.
		for(int i = 0; i < plots2D.length; i++) {
			for(int j = 0; j < 2; j++) {
				plots2D[i][j].scale(1.0 / scaleFactor);
				IPlotter plotter2D = plotterFactory.create((j == 1 ? "Cluster " : "") + titles2D[i]);
				plotter2D.createRegions(1);
				plotter2D.region(0).plot(plots2D[i][j]);
				
				// Format the axis labels.
				PlotterRegion region2D = (PlotterRegion) plotter2D.region(0);
				region2D.getPlot().setTitle((j == 1 ? "Cluster " : "") + titles2D[i]);
				region2D.getPlot().getXAxis().setLabel(xTitles2D[i]);
				region2D.getPlot().getYAxis().setLabel(yTitles2D[i]);
				
				// Format the fonts and general plot presentation.
				PlotsFormatter.setDefault2DStyle(region2D, true);
				
				// Show the plot.
				plotter2D.setParameter("plotterWidth", "2000");
				plotter2D.setParameter("plotterHeight", "1200");
				plotter2D.show();
			}
		}
		
		// Close the tree.
		tree.close();
	}
}