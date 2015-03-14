/**
 * 
 */
package org.hps.monitoring.plotting;

import hep.aida.IPlotter;
import hep.aida.IPlotterRegion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class PlotterRegistry {

    Set<IPlotter> plotters = new HashSet<IPlotter>();    
    HashMap<IPlotter, int[]> tabMap = new HashMap<IPlotter, int[]>();
    
    public void clear() {
        plotters.clear();
    }
    
    public IPlotter find(IPlotterRegion region) {
        for (IPlotter plotter : plotters) {
            for (int i = 0; i < plotter.numberOfRegions(); i++) {
                if (plotter.region(i) == region) {
                    return plotter;
                }
            }
        }
        return null;
    }
    
    public void register(IPlotter plotter, int index1, int index2) {
        tabMap.put(plotter, new int[] { index1, index2 });
    }
    
    public IPlotter find(int index1, int index2) {
        for (Entry<IPlotter, int[]> entry : tabMap.entrySet()) {
            int[] indices = entry.getValue();
            if (indices[0] == index1 && indices[1] == index2) {
                return entry.getKey();
            }
        }
        return null;
    }
}