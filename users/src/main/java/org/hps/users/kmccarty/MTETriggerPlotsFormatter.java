package org.hps.users.kmccarty;

import java.io.IOException;

import org.hps.users.kmccarty.plots.FormattedPlot1D;
import org.hps.users.kmccarty.plots.FormattedPlot2D;
import org.hps.users.kmccarty.plots.PlotFormatModule;
import org.hps.users.kmccarty.PlotsFormatter.ColorStyle;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;


public class MTETriggerPlotsFormatter {
	public static void main(String[] args) throws IllegalArgumentException, IOException {
		// Define the root directory for the plots.
		String rootDir = "D:\\cygwin64\\home\\Kyle\\";
		
		// Define the new name of the file containing the trigger plots.
		String plotFile = rootDir + "5772-ana.aida";
		
		// Get the plots file and open it.
		IAnalysisFactory af = IAnalysisFactory.create();
		ITree tree = af.createTreeFactory().create(plotFile);
		if(tree == null) { throw new IllegalArgumentException("Unable to load plot file."); }
		
		// Define the 1D trigger plot names for Møllers and tridents.
		String[] plotNames1D = { "Cluster Hit Count", "Cluster Seed Energy", "Cluster Total Energy",
				"Pair Coplanarity", "Pair Energy Difference", "Pair Energy Slope", "Pair Energy Sum" };
		String[] displayNames1D = { "Cluster Hit Count", "Cluster Seed Energy", "Cluster Total Energy",
				"Pair Coplanarity", "Pair Energy Difference", "Pair Energy Slope", "Pair Energy Sum" };
		String[] xAxisNames1D = { "Hit Count", "Seed Energy (GeV)", "Total Energy (GeV)",
				"Coplanarity (Degrees)", "Energy Difference (GeV)", "Energy Slope (GeV)", "Energy Sum (GeV)" };
		String yAxisName1D = "Count";
		
		// Define the 2D trigger plot names for Møllers and tridents.
		String[] plotNames2D = { "Cluster Seed", "Pair Energy Sum 2D" };
		String[] displayNames2D = { "Cluster Seed Distribution", "2D Energy Sum" };
		String[] xAxisNames2D = { "x-Index", "Second Cluster Energy (GeV)" };
		String[] yAxisNames2D = { "y-Index", "First Cluster Energy (GeV)" };
		
		// Define the 1D trigger plot names for elastics.
		String[] plotNamesElastic1D = { "Cluster Hit Count", "Cluster Seed Energy", "Cluster Total Energy" };
		String[] displayNamesElastic1D = { "Cluster Hit Count", "Cluster Seed Energy", "Cluster Total Energy" };
		String[] xAxisNamesElastic1D = { "Hit Count", "Seed Energy (GeV)", "Total Energy (GeV)" };
		String yAxisNameElastic1D = "Count";
		
		// Define the 2D trigger plot names for elastics.
		String[] plotNamesElastic2D = { "Cluster Seed" };
		String[] displayNamesElastic2D = { "Cluster Seed Distribution" };
		String[] xAxisNamesElastic2D = { "x-Index" };
		String[] yAxisNamesElastic2D = { "y-Index" };
		
		// Define the Møller, trident, and elastic prefixes.
		String allPrefix = "All Trigger Plots/Pair Plots/";
		String møllerPrefix = "Møller Trigger Plots/Pair Plots/";
		String tridentPrefix = "Trident Trigger Plots/Pair Plots/";
		String elasticPrefix = "Elastic Trigger Plots/Singles Plots/";
		String allSinglesPrefix = "All Trigger Plots/Singles Plots/";
		
		// Define the plot type prefix.
		String allTypeName = "All Pairs - ";
		String møllerTypeName = "Møller - ";
		String tridentTypeName = "Trident - ";
		String elasticTypeName = "Elastic - ";
		String allSinglesTypeName = "All Singles - ";
		
		// Define the plot type colors.
		ColorStyle allColor = PlotsFormatter.ColorStyle.GREY;
		ColorStyle møllerColor = PlotsFormatter.ColorStyle.MS_BLUE;
		ColorStyle tridentColor = PlotsFormatter.ColorStyle.MS_ORANGE;
		ColorStyle elasticColor = PlotsFormatter.ColorStyle.MS_GREEN;
		
		// Create a plot formatting module.
		PlotFormatModule module = new PlotFormatModule();
		
		// Get the histograms and add them to the module. Start with the
		// trident and Møller plots.
		for(int i = 0; i < plotNames1D.length; i++) {
			// Get the Møller and trident plots.
			IHistogram1D allPlot = (IHistogram1D) tree.find(allPrefix + plotNames1D[i]);
			IHistogram1D møllerPlot = (IHistogram1D) tree.find(møllerPrefix + plotNames1D[i]);
			IHistogram1D tridentPlot = (IHistogram1D) tree.find(tridentPrefix + plotNames1D[i]);
			
			// Make a formatted plot for each.
			FormattedPlot1D allFormattedPlot = new FormattedPlot1D(allPlot, allColor, xAxisNames1D[i], yAxisName1D, allTypeName + displayNames1D[i]);
			FormattedPlot1D møllerFormattedPlot = new FormattedPlot1D(møllerPlot, møllerColor, xAxisNames1D[i], yAxisName1D, møllerTypeName + displayNames1D[i]);
			FormattedPlot1D tridentFormattedPlot = new FormattedPlot1D(tridentPlot, tridentColor, xAxisNames1D[i], yAxisName1D, tridentTypeName + displayNames1D[i]);
			
			// Add them to the module.
			module.addPlot1D(allFormattedPlot);
			module.addPlot1D(møllerFormattedPlot);
			module.addPlot1D(tridentFormattedPlot);
		}
		for(int i = 0; i < plotNames2D.length; i++) {
			// Get the Møller and trident plots.
			IHistogram2D allPlot = (IHistogram2D) tree.find(allPrefix + plotNames2D[i]);
			IHistogram2D møllerPlot = (IHistogram2D) tree.find(møllerPrefix + plotNames2D[i]);
			IHistogram2D tridentPlot = (IHistogram2D) tree.find(tridentPrefix + plotNames2D[i]);
			
			// Make a formatted plot for each.
			FormattedPlot2D allFormattedPlot = new FormattedPlot2D(allPlot, i == 0 ? true : false, xAxisNames2D[i], yAxisNames2D[i], allTypeName + displayNames2D[i]);
			FormattedPlot2D møllerFormattedPlot = new FormattedPlot2D(møllerPlot, i == 0 ? true : false, xAxisNames2D[i], yAxisNames2D[i], møllerTypeName + displayNames2D[i]);
			FormattedPlot2D tridentFormattedPlot = new FormattedPlot2D(tridentPlot, i == 0 ? true : false, xAxisNames2D[i], yAxisNames2D[i], tridentTypeName + displayNames2D[i]);
			
			// Add them to the module.
			module.addPlot2D(allFormattedPlot);
			module.addPlot2D(møllerFormattedPlot);
			module.addPlot2D(tridentFormattedPlot);
		}
		
		// Get the histograms for the elastic plots and add them to the module.
		for(int i = 0; i < plotNamesElastic1D.length; i++) {
			// Get the Møller and trident plots.
			IHistogram1D allPlot = (IHistogram1D) tree.find(allSinglesPrefix + plotNames1D[i]);
			IHistogram1D elasticPlot = (IHistogram1D) tree.find(elasticPrefix + plotNames1D[i]);
			
			// Make a formatted plot for each.
			FormattedPlot1D allFormattedPlot = new FormattedPlot1D(allPlot, allColor, xAxisNamesElastic1D[i], yAxisNameElastic1D,
					allSinglesTypeName + displayNamesElastic1D[i]);
			FormattedPlot1D elasticFormattedPlot = new FormattedPlot1D(elasticPlot, elasticColor, xAxisNamesElastic1D[i], yAxisNameElastic1D,
					elasticTypeName + displayNamesElastic1D[i]);
			
			// Add them to the module.
			module.addPlot1D(allFormattedPlot);
			module.addPlot1D(elasticFormattedPlot);
		}
		for(int i = 0; i < plotNamesElastic2D.length; i++) {
			// Get the Møller and trident plots.
			IHistogram2D allPlot = (IHistogram2D) tree.find(allPrefix + plotNamesElastic2D[i]);
			IHistogram2D elasticPlot = (IHistogram2D) tree.find(møllerPrefix + plotNamesElastic2D[i]);
			
			// Make a formatted plot for each.
			FormattedPlot2D allFormattedPlot = new FormattedPlot2D(allPlot, i == 0 ? true : false, xAxisNamesElastic2D[i], yAxisNamesElastic2D[i],
					allSinglesTypeName + plotNames2D[i]);
			FormattedPlot2D elasticFormattedPlot = new FormattedPlot2D(elasticPlot, i == 0 ? true : false, xAxisNamesElastic2D[i], yAxisNamesElastic2D[i],
					elasticTypeName + displayNamesElastic2D[i]);
			
			// Add them to the module.
			module.addPlot2D(allFormattedPlot);
			module.addPlot2D(elasticFormattedPlot);
		}
		
		// Add the MTE plots to the module.
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Elastic Energy Distribution"), elasticColor,
				"Momentum (GeV)", "Count", "Elastic - Momentum"));
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Elastic Event Tracks"), elasticColor,
				"Tracks", "Count", "Elastic - Tracks in Event"));
		
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Møller Energy Sum Distribution"), møllerColor,
				"Momentum Sum (GeV)", "Count", "Møller - Momentum Sum"));
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Møller Electron Energy Distribution"), møllerColor,
				"Momentum (GeV)", "Count", "Møller - Momentum (Electron)"));
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Møller Time Coincidence Distribution (All Møller Cuts)"), møllerColor,
				"Time (ns)", "Count", "Møller - Time Coincidence"));
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Møller Event Tracks"), møllerColor,
				"Tracks", "Count", "Møller - Tracks in Event"));
		module.addPlot2D(new FormattedPlot2D((IHistogram2D) tree.find("MTE Analysis/Møller 2D Energy Distribution"), false,
				"First Track Momentum (GeV)", "Second Track Momentum (GeV)", "Møller - 2D Momentum Sum"));
		
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Trident Energy Sum Distribution"), tridentColor,
				"Momentum Sum (GeV)", "Count", "Trident - Momentum Sum"));
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Trident Electron Energy Distribution"), tridentColor,
				"Momentum (GeV)", "Count", "Trident - Momentum (Electron)"));
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Trident Positron Energy Distribution"), tridentColor,
				"Momentum (GeV)", "Count", "Trident - Momentum (Positron)"));
		module.addPlot1D(new FormattedPlot1D((IHistogram1D) tree.find("MTE Analysis/Trident Event Tracks"), tridentColor,
				"Tracks", "Count", "Trident - Tracks in Event"));
		module.addPlot2D(new FormattedPlot2D((IHistogram2D) tree.find("MTE Analysis/Trident 2D Energy Distribution"), false,
				"First Track Momentum (GeV)", "Second Track Momentum (GeV)", "Trident - 2D Momentum Sum"));
		
		// Display the plots.
		module.savePlots("C:\\Users\\Kyle\\Desktop\\EnergyShift\\TestPrint\\");
	}
}
