package org.hps.monitoring.ecal.plots;

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

/**
 * Some simple utility methods for organizing ECAL monitoring plots.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author <baltzell@jlab.org>
 */
public final class EcalMonitoringUtilities {

    static AIDA aida = AIDA.defaultInstance();
    static double CHANNELS_LOWER_EDGE_1D = 0.5;
    static double CHANNELS_UPPTER_EDGE_1D = 441.5;
    static final String EXTRA_DATA_RELATIONS_NAME = "EcalReadoutExtraDataRelations";

    // FIXME: Copied from EcalRawConverterDriver.
    static int INTEGRAL_WINDOW = 30;

    static int N_BINS_X_2D = 47;

    static int N_BINS_Y_2D = 11;

    static int N_CHANNELS_1D = 442;

    static double X_BIN_EDGE_2D = 23.25;

    final static int XHOLESTART = -10;

    final static int XHOLEWIDTH = 9;

    final static int XOFFSET = 23;
    static double Y_BIN_EDGE_2D = 5.25;
    final static int YOFFSET = 5;

    static IHistogram1D createChannelHistogram1D(final String name) {
        return aida.histogram1D(name, N_CHANNELS_1D, CHANNELS_LOWER_EDGE_1D, CHANNELS_UPPTER_EDGE_1D);
    }

    static IHistogram2D createChannelHistogram2D(final String name) {
        return aida.histogram2D(name, N_BINS_X_2D, -X_BIN_EDGE_2D, X_BIN_EDGE_2D, N_BINS_Y_2D, -Y_BIN_EDGE_2D,
                Y_BIN_EDGE_2D);
    }

    static IProfile1D createChannelProfile1D(final String name) {
        return aida.profile1D(name, N_CHANNELS_1D, CHANNELS_LOWER_EDGE_1D, CHANNELS_UPPTER_EDGE_1D);
    }

    static IProfile2D createChannelProfile2D(final String name) {
        return aida.profile2D(name, N_BINS_X_2D, -X_BIN_EDGE_2D, X_BIN_EDGE_2D, N_BINS_Y_2D, -Y_BIN_EDGE_2D,
                Y_BIN_EDGE_2D);
    }

    public static int getChannelIdFromRowColumn(final int row, final int col) {
        final int ix = col + XOFFSET + (col > 0 ? -1 : 0);
        final int iy = row + YOFFSET + (row > 0 ? -1 : 0);
        int cid = ix + 2 * XOFFSET * (2 * YOFFSET - iy - 1) + 1;
        if (row == 1 && col >= XHOLESTART) {
            cid -= XHOLEWIDTH;
        } else if (row == -1 && col < XHOLESTART) {
            cid -= XHOLEWIDTH;
        } else if (row < 0) {
            cid -= 2 * XHOLEWIDTH;
        }
        return cid;
    }

    // TODO: add more range constants for common quantities like hit energy, timestamp, ADC values, cluster position,
    // etc.

    public static int getColumnFromHistoID(final int id) {
        return id / (2 * YOFFSET + 1) - XOFFSET;
    }

    public static int getHistoIDFromRowColumn(final int row, final int column) {
        return -row + YOFFSET + (2 * YOFFSET + 1) * (column + XOFFSET);
    }

    static IPlotterStyle getPlotterStyle(final IBaseHistogram histogram) {
        final StyleRegistry styleRegistry = StyleRegistry.getStyleRegistry();
        final IStyleStore store = styleRegistry.getStore("DefaultStyleStore");
        IPlotterStyle style = null;
        if (histogram instanceof IHistogram1D || histogram instanceof ICloud1D || histogram instanceof IProfile1D) {
            style = store.getStyle("DefaultHistogram1DStyle");
        } else if (histogram instanceof IHistogram2D || histogram instanceof IProfile2D) {
            style = store.getStyle("DefaultColorMapStyle");
        }
        if (style == null) {
            throw new RuntimeException("A default style could not be found for " + histogram.title());
        }
        return style;
    }

    public static int getRowFromHistoID(final int id) {
        return YOFFSET - id % (2 * YOFFSET + 1);
    }

    public static Boolean isInHole(final int row, final int column) {
        if (row == 1 || row == -1) {
            if ((column < XHOLESTART + XHOLEWIDTH) && (column >= XHOLESTART)) {
                return true;
            }
        } 
        
        if (row == 0) {
            return true;
        } 
        
        if (column == 0) {
            return true;
        }
        return false;
    }

    static IPlotter plot(final IPlotterFactory plotterFactory, final IBaseHistogram histogram, IPlotterStyle style,
            final boolean show) {
        if (style == null) {
            style = getPlotterStyle(histogram);
        }
        final IPlotter plotter = plotterFactory.create(histogram.title());
        plotter.createRegion();
        plotter.region(0).plot(histogram, style);
        if (show) {
            plotter.show();
        }
        return plotter;
    }

    private EcalMonitoringUtilities() {
    }
}
