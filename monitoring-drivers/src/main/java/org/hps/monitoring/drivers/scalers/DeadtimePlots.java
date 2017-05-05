package org.hps.monitoring.drivers.scalers;

import java.util.Date;

import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.plotting.StripChartUpdater;
import org.hps.monitoring.plotting.ValueProvider;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalerUtilities;
import org.hps.record.scalers.ScalerUtilities.LiveTimeIndex;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * Make a strip chart for DAQ deadtime from the info in the scaler block. 
 */
public class DeadtimePlots extends Driver {

//    static final int REFRESH_RATE = 10 * 1000; // units = ms
//    static final double DOMAIN_SIZE = 4 * 60 * 60 * 1000; // x-axis range (ms)
      double DOMAIN_SIZE =  10 * 60 * 1000; // 10 minutes
    //String scalarsName = "Scalars";

    int events;

    double fcupTdc;
    double fcupTrg;
    StripChartUpdater updater;
    JFreeChart deadtimes;
    
    public void setTimeAxisRange(double width){
        this.DOMAIN_SIZE=width;
    }

    public void startOfData() {
//        plotFactory.createStripChart(
//                "DAQ Deadtime",
//                "deadtime",
//                2,
//                new String[]{"fcupTdc", "fcupTrg"},
//                100,
//                new Second(),
//                new DeadtimeProvider(),
//                50000L);

        MonitoringPlotFactory plotFactory
            = (MonitoringPlotFactory) AIDA.defaultInstance().analysisFactory().createPlotterFactory("Deadtime Monitoring");
        deadtimes = plotFactory.createTimeSeriesChart(
                "DAQ Deadtime",
                "Livetime",
                2, new String[]{"fcupTdc", "fcupTrg"},
                DOMAIN_SIZE);
        deadtimes.getXYPlot().getRangeAxis().setRange(0.5, 1.0);
    }

    @Override
    public void process(EventHeader event) {
        ScalerData data = ScalerData.read(event);
        if (data != null) {
            fcupTdc = ScalerUtilities.getLiveTime(data, LiveTimeIndex.FCUP_TDC); // etc. }
            fcupTrg = ScalerUtilities.getLiveTime(data, LiveTimeIndex.FCUP_TRG); // etc. }

            // fill strip charts:
            long now = System.currentTimeMillis();

            TimeSeriesCollection cc = (TimeSeriesCollection) deadtimes.getXYPlot().getDataset();

            DateAxis ax = (DateAxis) deadtimes.getXYPlot().getDomainAxis();
            ax.setRange(now - DOMAIN_SIZE, now);

            cc.getSeries(0).addOrUpdate(new Second(new Date()), fcupTdc);
            cc.getSeries(1).addOrUpdate(new Second(new Date()), fcupTrg);
        }
    }

    public void endOfData() {
        if(updater!=null)
            updater.stop(); 
    }

    class DeadtimeProvider implements ValueProvider {

        public float[] getValues() {
            return new float[]{(float) fcupTdc, (float) fcupTrg};
        }
    }

}
