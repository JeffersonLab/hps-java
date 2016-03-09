package org.hps.users.kmccarty.plots;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ref.plotter.PlotterRegion;

import org.hps.users.kmccarty.plots.PlotsFormatter.ColorStyle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlotFormatModule {
    private String width = "2000";
    private String height = "1200";
    private List<FormattedPlot1D> formattedPlots1D = new ArrayList<FormattedPlot1D>();
    private List<FormattedPlot2D> formattedPlots2D = new ArrayList<FormattedPlot2D>();
    
    public void addPlot1D(FormattedPlot1D plot) {
        formattedPlots1D.add(plot);
    }
    
    public void addPlot2D(FormattedPlot2D plot) {
        formattedPlots2D.add(plot);
    }
    
    public void setDisplayHeight(int height) {
        this.height = "" + height;
    }
    
    public void setDisplayWidth(int width) {
        this.width = "" + width;
    }
    
    public void displayPlots() {
        try { processPlots(null); }
        catch (IOException e) { e.printStackTrace(); }
    }
    
    public void savePlots(String filePath) throws IOException {
        processPlots(filePath);
    }
    
    private void processPlots(String filePath) throws IOException {
        // Create a plotter factory.
        IAnalysisFactory af = IAnalysisFactory.create();
        IPlotterFactory plotterFactory = af.createPlotterFactory();
        
        // Format and display the 1D plots.
        for(FormattedPlot1D formattedPlot : formattedPlots1D) {
            // Get the plot.
            IHistogram1D plot = formattedPlot.getPlot();
            
            // Create a plotter and plotting region for the plot.
            IPlotter plotter = plotterFactory.create(plot.title());
            plotter.createRegions(1);
            plotter.region(0).plot(plot);
            
            // Set the axis range.
            PlotterRegion region = (PlotterRegion) plotter.region(0);
            if(formattedPlot.definesAxisRange()) {
                region.getPlot().getXAxis().setMax(formattedPlot.getAxisRange());
            }
            
            // Format the axis labels.
            region.getPlot().setTitle(formattedPlot.getPlotName());
            region.getPlot().getXAxis().setLabel(formattedPlot.getXAxisName());
            region.getPlot().getYAxis().setLabel(formattedPlot.getYAxisName());
            
            // Format the fonts and general plot presentation.
            PlotsFormatter.setDefault1DStyle(region, new ColorStyle[] { formattedPlot.getColorStyle() });
            
            // Set the plotter dimensions.
            plotter.setParameter("plotterWidth", width);
            plotter.setParameter("plotterHeight", height);
            
            // If the file path is null, display the plots. Otherwise,
            // save them to the destination folder.
            if(filePath == null) { plotter.show(); }
            else {
                File plotFile = new File(filePath + formattedPlot.getPlotName() + ".png");
                if(plotFile.exists()) { plotFile.delete(); }
                plotter.writeToFile(filePath + formattedPlot.getPlotName() + ".png");
                System.out.printf("Saved plot \"%s\" to path: %s%n", formattedPlot.getPlotName(), filePath + formattedPlot.getPlotName() + ".png");
            }
        }
        
        // Format and display the 2D plots.
        for(FormattedPlot2D formattedPlot : formattedPlots2D) {
            // Get the plot.
            IHistogram2D plot = formattedPlot.getPlot();
            
            // Create a plotter and plotting region for the plot.
            IPlotter plotter = plotterFactory.create(formattedPlot.getPlotName());
            plotter.createRegions(1);
            plotter.region(0).plot(plot);
            
            // Set the axis range.
            PlotterRegion region = (PlotterRegion) plotter.region(0);
            if(formattedPlot.definesXAxisRange()) {
                region.getPlot().getXAxis().setMax(formattedPlot.getXAxisRange());
            } if(formattedPlot.definesYAxisRange()) {
                region.getPlot().getYAxis().setMax(formattedPlot.getYAxisRange());
            }
            
            // Format the axis labels.
            region.getPlot().setTitle(formattedPlot.getPlotName());
            region.getPlot().getXAxis().setLabel(formattedPlot.getXAxisName());
            region.getPlot().getYAxis().setLabel(formattedPlot.getYAxisName());
            
            // Format the fonts and general plot presentation.
            PlotsFormatter.setDefault2DStyle(region, formattedPlot.isLogarithmic());
            
            // Set the plotter dimensions.
            plotter.setParameter("plotterWidth", width);
            plotter.setParameter("plotterHeight", height);
            
            // If the file path is null, display the plots. Otherwise,
            // save them to the destination folder.
            if(filePath == null) { plotter.show(); }
            else {
                File plotFile = new File(filePath + formattedPlot.getPlotName() + ".png");
                if(plotFile.exists()) { plotFile.delete(); }
                plotter.writeToFile(filePath + formattedPlot.getPlotName() + ".png");
                System.out.printf("Saved plot \"%s\" to path: %s%n", formattedPlot.getPlotName(), filePath + formattedPlot.getPlotName() + ".png");
            }
        }
    }
    
    public void exportPlots(String filePath) throws IOException {
        // Export the 1D plots in a text format.
        for(FormattedPlot1D plot : formattedPlots1D) {
            exportPlot(filePath, plot);
        }
        
        // Export the 2D plots in a text format.
        for(FormattedPlot2D plot : formattedPlots2D) {
            exportPlot(filePath, plot);
        }
    }
    
    private static final void exportPlot(String filePath, FormattedPlot plot) throws IOException {
        // Check if this is a one or two dimensional plot.
        boolean is1D = plot instanceof FormattedPlot1D;
        
        // Create a file object for the plot.
        String plotPath = filePath + plot.getPlotName() + (is1D ? ".aida1D" : ".aida2D");
        File datFile = new File(plotPath);
        
        // If the plot file already exists, delete it.
        if(datFile.exists()) { datFile.delete(); }
        
        // Create a new file for the plot to occupy.
        datFile.createNewFile();
        
        // Get the textual form of the plot.
        String plotText = null;
        if(is1D) { plotText = toTextFormat(((FormattedPlot1D) plot).getPlot()); }
        else { plotText = toTextFormat(((FormattedPlot2D) plot).getPlot()); }
        
        // Write the plot text to the file.
        BufferedWriter writer = new BufferedWriter(new FileWriter(datFile));
        writer.write(plotText);
        writer.close();
        
        // Note that the file was written.
        System.out.printf("Plot \"%s\" was exported to path: %s%n", plot.getPlotName(), plotPath);
    }
    
    private static final String toTextFormat(IHistogram1D plot) {
        // Create a buffer to hold the converted plot.
        StringBuffer buffer = new StringBuffer();
        
        // Iterate over the bins and output the plot in the format of
        // "[BIN_MEAN] [BIN_VALUE]" with a tab delimiter.
        for(int bin = 0; bin < plot.axis().bins(); bin++) {
            buffer.append(String.format("%f\t%f%n", plot.binMean(bin), plot.binHeight(bin)));
        }
        
        // Return the converted file.
        return buffer.toString();
    }
    
    private static final String toTextFormat(IHistogram2D plot) {
        // Create a buffer to hold the converted plot.
        StringBuffer buffer = new StringBuffer();
        
        // Iterate over the bins and output the plot in the format of
        // "[X_BIN_MEAN] [Y_BIN_MEAN] [BIN_VALUE]" with a tab delimiter.
        for(int xBin = 0; xBin < plot.xAxis().bins(); xBin++) {
            for(int yBin = 0; yBin < plot.yAxis().bins(); yBin++) {
                buffer.append(String.format("%f\t%f\t%f%n", plot.binMeanX(xBin, yBin), plot.binMeanY(xBin, yBin), plot.binHeight(xBin, yBin)));
            }
        }
        
        // Return the converted file.
        return buffer.toString();
    }
}
