package org.hps.monitoring.plotting;

import hep.aida.IPlotter;
import hep.aida.IPlotterRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * This is a global registry of plotters used by the monitoring plot factory.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PlotterRegistry {

    HashMap<IPlotter, int[]> plotterMap = new HashMap<IPlotter, int[]>();

    /**
     * Clear the list of plotters.
     */
    public void clear() {
        System.out.println("clearing PlotterRegistry");
        plotterMap.clear();
    }

    /**
     * Find a plotter that contains the region object.
     * 
     * @param region The plotter region object.
     * @return The plotter that contains this region or null if none.
     */
    public IPlotter find(IPlotterRegion region) {
        for (IPlotter plotter : plotterMap.keySet()) {
            for (int i = 0; i < plotter.numberOfRegions(); i++) {
                if (plotter.region(i) == region) {
                    return plotter;
                }
            }
        }
        return null;
    }

    /**
     * Get the tab indices of the plotter.
     * 
     * @param plotter the plotter to lookup
     * @return the tab indices of the plotter or <code>null</code> if it doesn't exist
     */
    public int[] getTabIndices(IPlotter plotter) {
        return plotterMap.get(plotter);
    }

    /**
     * Register a plotter along with its tab indices.
     * 
     * @param plotter The plotter to register.
     * @param index1 The top tab index.
     * @param index2 The sub-tab index.
     */
    public void register(IPlotter plotter, int index1, int index2) {
        plotterMap.put(plotter, new int[] {index1, index2});
    }

    /**
     * Find a plotter by its tab indices e.g. those that are currently selected in an application.
     * 
     * @param index1 The top tab index.
     * @param index2 The sub-tab index.
     * @return The plotter or null if none found.
     */
    public IPlotter find(int index1, int index2) {
        for (Entry<IPlotter, int[]> entry : plotterMap.entrySet()) {
            int[] indices = entry.getValue();
            if (indices[0] == index1 && indices[1] == index2) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get the current collection of plotters as an unmodifiable collection.
     * 
     * @return The current collection of plotters.
     */
    public List<IPlotter> getPlotters() {
        return Collections.unmodifiableList(new ArrayList<IPlotter>(plotterMap.keySet()));
    }
}