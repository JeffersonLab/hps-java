package org.hps.monitoring.ecal.plots;

/**
 * Some simple utility methods for organizing ECAL monitoring plots.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author <baltzell@jlab.org>
 */
public final class EcalMonitoringUtilities {

    final static int XOFFSET = 23;
    final static int YOFFSET = 5;
    
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
    
    public static int getChannelIdFromRowColumn(int row, int col)
    {
        int ix = col + XOFFSET + (col>0 ? -1 : 0);
        int iy = row + YOFFSET + (row>0 ? -1 : 0);
        iy = YOFFSET*2 - iy - 1;
        int cid = ix + 2*XOFFSET*iy + 1;
        if      (row== 1 && col>-10) cid -= 9;
        else if (row==-1 && col<-10) cid -= 9;
        else if (row < 0)            cid -= 18;
        return cid; 
    }
}
