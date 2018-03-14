package org.hps.monitoring.plotting;

import hep.aida.IBaseHistogram;
import hep.aida.IDataPointSet;
import hep.aida.IFunction;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.StandalonePlotter;

public class ChartPopup {

    public void update(PlotterRegion region) {
        IPlotter plotter = new StandalonePlotter();
        plotter.createRegion(0);
        PlotterRegion targetRegion = (PlotterRegion) plotter.region(0);
        targetRegion.setStyle(region.style());
        for (Object object : region.getPlottedObjects()) {
            IPlotterStyle style = region.getState().findPlotterStyle(object);
            if (object instanceof IBaseHistogram) {
                targetRegion.plot((IBaseHistogram) object, style);
            } else if (object instanceof IDataPointSet) {
                targetRegion.plot((IDataPointSet) object, style);
            } else if (object instanceof IFunction) {
                targetRegion.plot((IFunction) object, style);
            }
        }
        plotter.show();
    }
}
