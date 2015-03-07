package org.hps.monitoring.drivers.ecal;

import hep.aida.IBaseHistogram;
import hep.aida.ICloud1D;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile1D;
import hep.aida.IProfile2D;
import hep.aida.ref.plotter.style.registry.IStyleStore;
import hep.aida.ref.plotter.style.registry.StyleRegistry;

import org.lcsim.util.aida.AIDA;

class EcalMonitoringUtilities {
    
    // FIXME: Copied from EcalRawConverterDriver.
    static int INTEGRAL_WINDOW = 30;
    
    static double X_BIN_EDGE_2D = 23.25;
    static double Y_BIN_EDGE_2D = 5.25;
    static int N_BINS_X_2D = 47;
    static int N_BINS_Y_2D = 11;
    
    static int N_CHANNELS_1D = 442;
    
    static double CHANNELS_LOWER_EDGE_1D = 0.5;
    static double CHANNELS_UPPTER_EDGE_1D = 441.5;
    
    static final String EXTRA_DATA_RELATIONS_NAME = "EcalReadoutExtraDataRelations";
    
    // TODO: add more range constants for common quantities like hit energy, timestamp, ADC values, cluster position, etc.
    
    static AIDA aida = AIDA.defaultInstance();     
       
    static IHistogram2D createChannelHistogram2D(String name) {
        return aida.histogram2D(name, N_BINS_X_2D, -X_BIN_EDGE_2D, X_BIN_EDGE_2D, N_BINS_Y_2D, -Y_BIN_EDGE_2D, Y_BIN_EDGE_2D);
    }
    
    static IProfile2D createChannelProfile2D(String name) {
        return aida.profile2D(name, N_BINS_X_2D, -X_BIN_EDGE_2D, X_BIN_EDGE_2D, N_BINS_Y_2D, -Y_BIN_EDGE_2D, Y_BIN_EDGE_2D);
    }
    
    static IHistogram1D createChannelHistogram1D(String name) {
        return aida.histogram1D(name, N_CHANNELS_1D, CHANNELS_LOWER_EDGE_1D, CHANNELS_UPPTER_EDGE_1D);
    }
    
    static IProfile1D createChannelProfile1D(String name) {
        return aida.profile1D(name, N_CHANNELS_1D, CHANNELS_LOWER_EDGE_1D, CHANNELS_UPPTER_EDGE_1D);
    }
    
    static IPlotter plot(IPlotterFactory plotterFactory, IBaseHistogram histogram, IPlotterStyle style, boolean show) {
        if (style == null) {
            style = getPlotterStyle(histogram);
        }
        IPlotter plotter = plotterFactory.create(histogram.title());
        plotter.createRegion();
        plotter.region(0).plot(histogram, style);
        if (show) {
            plotter.show();
        }
        return plotter;
    }
            
    static IPlotterStyle getPlotterStyle(IBaseHistogram histogram) {
        StyleRegistry styleRegistry = StyleRegistry.getStyleRegistry();
        IStyleStore store = styleRegistry.getStore("DefaultStyleStore");
        IPlotterStyle style = null;
        if ((histogram instanceof IHistogram1D) || (histogram instanceof ICloud1D) || (histogram instanceof IProfile1D)) {
            style = store.getStyle("DefaultHistogram1DStyle");
        } else if ((histogram instanceof IHistogram2D) || (histogram instanceof IProfile2D)) {
            style = store.getStyle("DefaultColorMapStyle");
        }
        if (style == null) {
            throw new RuntimeException("A default style could not be found for " + histogram.title());
        }
        return style;
    }
        
}
