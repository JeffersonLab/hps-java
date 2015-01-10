package org.hps.monitoring.ecal.plots;

/**
 * Some simple utility methods for organizing ECAL monitoring plots.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 */
public final class EcalMonitoringUtilities {

    private EcalMonitoringUtilities() {        
    }
    
    public static int getRowFromHistoID(int id) {
        return (5 - (id % 11));
    }

    public static int getColumnFromHistoID(int id) {
        return ((id / 11) - 23);
    }

    public static int getHistoIDFromRowColumn(int row, int column) {
        return (-row + 5) + 11 * (column + 23);
    }

    public static Boolean isInHole(int row, int column) {
        Boolean ret;
        ret = false;
        if ((row == 1) || (row == -1)) {
            if ((column <= -2) && (column >= -10))
                ret = true;
        }
        return ret;
    }
}
