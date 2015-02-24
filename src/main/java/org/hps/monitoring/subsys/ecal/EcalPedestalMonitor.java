package org.hps.monitoring.subsys.ecal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.plotting.StripChartUtil;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import hep.aida.IPlotterFactory;
/*
 * Reads output of org.hps.recon.ecal.RunningPedestalDriver and makes strip charts.
 * Baltzell
 */
public class EcalPedestalMonitor extends Driver {

    int maxAge = 999999999;
    int maxCount = 100000;
    int rangeSize = 100000;
   
    final int crates[]={1,2};
    final int slots[]={3,4,5,6,7,8,9,14,15,16,17,18,19,20};
    
    String collectionName = "EcalRunningPedestals";
    
//    static {
//        org.hps.monitoring.plotting.MonitoringAnalysisFactory.register();
//    }
    MonitoringPlotFactory plotFactory = (MonitoringPlotFactory) AIDA.defaultInstance()
            .analysisFactory().createPlotterFactory("ECal Pedestal Monitoring");

    Map<Integer, Map<Integer, JFreeChart>> stripCharts = new HashMap<Integer, Map<Integer, JFreeChart>>();
    Map<Integer, Map<Integer, TimeSeries>> stripCharts2 = new HashMap<Integer, Map<Integer, TimeSeries>>();

    private EcalConditions ecalConditions = null;
    
    public void startOfData() {
        //plotFactory.createStripChart("X","Y",maxAge,maxCount,rangeSize);
        plotFactory.create().show();
    }

    @Override
    public void detectorChanged(Detector detector) {
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class,TableConstants.ECAL_CONDITIONS)
                .getCachedData();
        /*
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            final int crate = cc.getCrate();
            final int slot = cc.getSlot();
            if (!stripCharts.containsKey(crate)) {
                stripCharts.put(crate,new HashMap<Integer, JFreeChart>());
            }
            if (!stripCharts2.containsKey(crate)) {
                stripCharts2.put(crate,new HashMap<Integer, TimeSeries>());
            }
            if (!stripCharts.get(crate).containsKey(slot)) {
                String name = String.format("C%dS%02d",crate,slot);
                JFreeChart stripChart = plotFactory.createStripChart(name,"asdf",
                        maxAge,maxCount,rangeSize);
                stripCharts.get(crate).put(slot,stripChart);
                stripCharts2.get(crate).put(slot,StripChartUtil.getTimeSeries(stripChart));
            }
        }
        */
        // put them in order:
        for (int crate : crates) {
            stripCharts.put(crate,new HashMap<Integer, JFreeChart>());
            stripCharts2.put(crate,new HashMap<Integer, TimeSeries>());
            for (int slot : slots) {
                String name = String.format("C%dS%02d",crate,slot);
                JFreeChart stripChart = plotFactory.createStripChart(name,"asdf",
                        maxAge,maxCount,rangeSize);
                stripCharts.get(crate).put(slot,stripChart);
                stripCharts2.get(crate).put(slot,StripChartUtil.getTimeSeries(stripChart));
            }
        }
    }

    @Override
    public void process(EventHeader event) {

        if (!event.hasItem(collectionName)) {
            return;
        }

        // get the running pedestals:
        Map<EcalChannel, Double> peds = (Map<EcalChannel, Double>) event.get(collectionName);

        // tally slot pedestals:
        Map<Integer, Map<Integer, Double>> pedsum = new HashMap<Integer, Map<Integer, Double>>();
        Map<Integer, Map<Integer, Integer>> npedsum = new HashMap<Integer, Map<Integer, Integer>>();
        for (EcalChannel cc : peds.keySet()) {
            final Double ped = peds.get(cc);
            final int crate = cc.getCrate();
            final int slot = cc.getSlot();
            if (!pedsum.containsKey(crate)) {
                pedsum.put(crate,new HashMap<Integer, Double>());
            }
            if (!npedsum.containsKey(crate)) {
                npedsum.put(crate,new HashMap<Integer, Integer>());
            }
            if (!pedsum.get(crate).containsKey(slot)) {
                pedsum.get(crate).put(slot,0.0);
            }
            if (!npedsum.get(crate).containsKey(slot)) {
                npedsum.get(crate).put(slot,0);
            }
            npedsum.get(crate).put(slot,npedsum.get(crate).get(slot) + 1);
            pedsum.get(crate).put(slot,pedsum.get(crate).get(slot) + ped);
        }

        // fill strip charts:
        for (int crate : pedsum.keySet()) {
            for (int slot : pedsum.get(crate).keySet()) {

                final double ped = pedsum.get(crate).get(slot) / npedsum.get(crate).get(slot);
                stripCharts2.get(crate).get(slot).add(new Millisecond(new Date()),ped);
            }
        }

    }
}
