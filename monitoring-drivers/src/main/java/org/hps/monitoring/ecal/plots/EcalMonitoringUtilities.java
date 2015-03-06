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
    final static int XHOLEWIDTH = 9;
    final static int XHOLESTART = -10;
    
    private EcalMonitoringUtilities() {        
    }
    
    public static int getRowFromHistoID(int id) {
        return (YOFFSET - (id % (2*YOFFSET+1)));
    }

    public static int getColumnFromHistoID(int id) {
        return ((id / (2*YOFFSET+1)) - XOFFSET);
    }

    public static int getHistoIDFromRowColumn(int row, int column) {
        return (-row + YOFFSET) + (2*YOFFSET+1) * (column + XOFFSET);
    }

    public static Boolean isInHole(int row, int column) {
        if (row == 1 || row == -1) {
            if (column < XHOLESTART+XHOLEWIDTH && column >= XHOLESTART) {
                return true;
            }
        }
        return false;
    }
    
    public static int getChannelIdFromRowColumn(int row, int col)
    {
        int ix = col + XOFFSET + (col>0 ? -1 : 0);
        int iy = row + YOFFSET + (row>0 ? -1 : 0);
        int cid = ix + 2*XOFFSET*(2*YOFFSET-iy-1) + 1;
        if      (row== 1 && col>=XHOLESTART) cid -= XHOLEWIDTH;
        else if (row==-1 && col< XHOLESTART) cid -= XHOLEWIDTH;
        else if (row < 0)                    cid -= 2*XHOLEWIDTH;
        return cid; 
    }
}
