package org.hps.users.kmccarty.plots.formatter;

import jas.hist.JASHist1DHistogramStyle;
import jas.hist.JASHist2DHistogramStyle;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hep.aida.IAnalysisFactory;
import hep.aida.IBaseHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IManagedObject;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;
import hep.aida.ref.plotter.PlotterRegion;

public class TriggerPlotsFormat {
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
		String rootDir = "D:\\cygwin64\\home\\Kyle\\"; //plots\\no-cuts\\2-hit\\";
		
		// Define the new name of the file containing the trigger plots.
		String[] plotFile = {
				rootDir + "5568-ana.aida"
				//rootDir + "background-ana_triggerPlots.aida"
				//rootDir + "15MeV-ana_triggerPlots.aida",
				//rootDir + "30MeV-ana_triggerPlots.aida",
				//rootDir + "40MeV-ana_triggerPlots.aida",
				//rootDir + "50MeV-ana_triggerPlots.aida"
		};
		
		// Define the names of each plot. This will be used for the
		// legend in the case of multiple plots.
		String[] treeName = {
			"Background",
			"15 MeV A'",
			"30 MeV A'",
			"40 MeV A'",
			"50 MeV A'"
		};
		
		// Define the color style for the plots.
		ColorStyle[] dataColorStyle = {
				ColorStyle.GREY,
				ColorStyle.MS_GREEN,
				ColorStyle.MS_BLUE,
				ColorStyle.MS_ORANGE,
				ColorStyle.MS_RED
		};
		
		// Get the plots file and open it.
		IAnalysisFactory af = IAnalysisFactory.create();
		ITree[] tree = new ITree[plotFile.length];
		for(int i = 0; i < plotFile.length; i++) {
			tree[i] = af.createTreeFactory().create(plotFile[i]);
			if(tree[i] == null) { throw new IllegalArgumentException("Unable to load plot file."); }
		}
		
		// Get a list of all the histograms in the file.
		List<List<String>> treeHistograms = new ArrayList<List<String>>(plotFile.length);
		for(int i = 0; i < plotFile.length; i++) {
			treeHistograms.add(getHistograms(tree[i]));//, "/PassedAll/"));
		}
		
		// Create a plotter factory.
		IPlotterFactory plotterFactory = af.createPlotterFactory();
		
		// Plot each histogram and format it.
		for(String histogram : treeHistograms.get(0)) {
			// Get the plot from the tree and verify that it is a 1D
			// or 2D histogram. Other types are not supported.
			IManagedObject histObject = tree[0].find(histogram);
			if(!(histObject instanceof IHistogram1D) && !(histObject instanceof IHistogram2D)) {
				continue;
			}
			
			// Obtain the histogram object.
			IBaseHistogram hist;
			if(histObject instanceof IHistogram1D) { hist = (IHistogram1D) histObject; }
			else { hist = (IHistogram2D) histObject; }
			
			// Define whether this is an overlay plot and whether
			// this is a one or two dimensional plot.
			boolean overlay = plotFile.length > 1;
			boolean twoDimensional = hist instanceof IHistogram2D;
			
			// Generate the plotter and set its title. The plotter will
			// use the title of the first tree's plot.
			String plotTitle = hist.title();
			IPlotter plotter = plotterFactory.create(plotTitle);
			
			// For single plots and one-dimensional overlay plots,
			// there should only be a single plotter region.
			if(!twoDimensional || !overlay) { plotter.createRegions(1); }
			
			// For two-dimensional overlay plots, create a region for
			// each plot individually.
			else { plotter.createRegions(2, (int) Math.ceil(plotFile.length / 2.0)); }
			
			// Find the histogram in each of the trees and plot them
			// all on the same region.
			for(int i = 0; i < plotFile.length; i++) {
				// Get the histogram from the tree.
				IManagedObject treeObject = tree[i].find(histogram);
				IBaseHistogram treeHist;
				if(treeObject instanceof IHistogram1D) { treeHist = (IHistogram1D) treeObject; }
				else { treeHist = (IHistogram2D) treeObject; }
				
				// Display the plot.
				if(treeHist != null) {
					// Set the title of plot to the name associated with
					// its tree. This ensures that the correct name will
					// appear on the legend.
					if(plotFile.length > 1) {
						treeHist.setTitle(treeName[i]);
					}
					
					// Plot the tree's data in the plotter region.
					if(!twoDimensional || !overlay) { plotter.region(0).plot(treeHist); }
					else {
						plotter.region(i).plot(treeHist);
						setDefault2DStyle(((PlotterRegion) plotter.region(i)), dataColorStyle);
					}
				}
			}
			
			// Format the plot region.
			if(!twoDimensional) { setDefault1DStyle(((PlotterRegion) plotter.region(0)), dataColorStyle); }
			else { setDefault2DStyle(((PlotterRegion) plotter.region(0)), dataColorStyle); }
			
			// Show the plotter.
			plotter.region(0).setTitle(plotTitle);
			//plotter.setParameter("plotterWidth", "1600");
			//plotter.setParameter("plotterHeight", "1550");
			plotter.setParameter("plotterWidth", "2000");
			plotter.setParameter("plotterHeight", "1200");
			plotter.show();
		}
		
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
			
			// Set generic axis titles.
			region.getPlot().getXAxis().setLabel("Data Label (Unit)");
			region.getPlot().getYAxis().setLabel("Count");
			
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
	 * @param color - The data color settings to use.
	 */
	private static final void setDefault2DStyle(PlotterRegion region, ColorStyle[] color) {
		// Get the fill style object. 2D plots should never be overlay
		// plots, so there should only ever be one data name.
		JASHist2DHistogramStyle fillStyle = (JASHist2DHistogramStyle) region.getDataForName(region.getAllDataNames()[0]).getStyle();
		
		// Set the fill style for a two-dimensional plot.
		fillStyle.setLogZ(true);
		fillStyle.setHistStyle(JASHist2DHistogramStyle.STYLE_COLORMAP);
		fillStyle.setColorMapScheme(JASHist2DHistogramStyle.COLORMAP_RAINBOW);
		
		// Make the statistics box invisible.
		region.getPlot().getStats().setVisible(false);
		
		// Set the general plot font (which is also the z-axis font).
		region.getPlot().setFont(BASIC_FONT);
		
		// Set the title font.
		region.getPlot().getTitleObject().setFont(TITLE_FONT);
		
		// Set generic axis titles.
		region.getPlot().getXAxis().setLabel("Data Label (Unit)");
		region.getPlot().getYAxis().setLabel("Data Label (Unit)");
		
		// Set the axis tick-mark fonts.
		region.getPlot().getXAxis().setFont(BASIC_FONT);
		region.getPlot().getYAxis().setFont(BASIC_FONT);
		region.getPlot().getXAxis().getLabelObject().setFont(AXIS_FONT);
		region.getPlot().getYAxis().getLabelObject().setFont(AXIS_FONT);
	}
	
	/**
	 * Gets a list of all objects that are not directories in a tree.
	 * @param tree - The tree from which to extract the object names.
	 * @return Returns the object names as <code>String</code> objects
	 * in a <code>List</code> collection.
	 */
	private static final List<String> getHistograms(ITree tree) {
		return getHistograms(tree, "/");
	}
	
	/**
	 * Gets a list of all objects that are not directories in a tree.
	 * @param tree - The tree from which to extract the object names.
	 * @return Returns the object names as <code>String</code> objects
	 * in a <code>List</code> collection.
	 */
	private static final List<String> getHistograms(ITree tree, String rootDir) {
		return getHistograms(tree, rootDir, new ArrayList<String>());
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