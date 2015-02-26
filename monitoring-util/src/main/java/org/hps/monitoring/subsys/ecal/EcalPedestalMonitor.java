package org.hps.monitoring.subsys.ecal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.plotting.StripChartUtil;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
/*
 * Reads output of org.hps.recon.ecal.RunningPedestalDriver and makes strip charts.
 * 
 * Are we going to lose/reinitialize these plots when run ends?
 * Or can we keep on going off ET-ring across runs?
 * 
 * Baltzell
 */
public class EcalPedestalMonitor extends Driver {

    long previousTime;
    long currentTime;
    int refreshRate=1000; // units = ms
   
    int nDetectorChanges=0;
   
    int maxAge = 999999999;
    int maxCount = 100000;
    int rangeSize = 100000;
   
    // None of this "works":
    //int maxAge = 86400000; // 1 day (units ms)
    //int maxCount = 999999999;//(int)maxAge/refreshRate;
    //int rangeSize = 999999999;//maxAge; // what is this?
   
    final int crates[]={1,2};
    final int slots[]={3,4,5,6,7,8,9,14,15,16,17,18,19,20};
    
    String collectionName = "EcalRunningPedestals";
    
    MonitoringPlotFactory plotFactory = (MonitoringPlotFactory) AIDA.defaultInstance()
            .analysisFactory().createPlotterFactory("ECal Pedestal Monitoring");

    Map<Integer, Map<Integer, JFreeChart>> stripCharts = new HashMap<Integer, Map<Integer, JFreeChart>>();
    Map<Integer, Map<Integer, TimeSeries>> stripCharts2 = new HashMap<Integer, Map<Integer, TimeSeries>>();

    private EcalConditions ecalConditions = null;
    
    public void startOfData() {
        //plotFactory.createStripChart("X","Y",maxAge,maxCount,rangeSize);
        plotFactory.create().show();
        
        //System.out.println("----------------------------    "+maxCount);
    }

    @Override
    public void detectorChanged(Detector detector) {
        
        // this would defeat the purpose.
        if (nDetectorChanges++ > 0) return;
        
        currentTime=0;
        previousTime=0;
        
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        
        // put them in order:
        for (int crate : crates) {
            stripCharts.put(crate,new HashMap<Integer, JFreeChart>());
            stripCharts2.put(crate,new HashMap<Integer, TimeSeries>());
            for (int slot : slots) {
                
                final double ped=getAveragePedestal(crate,slot);
                
                String name = String.format("C%dS%02d",crate,slot);
                JFreeChart stripChart = plotFactory.createStripChart(name,"asdf",
                        maxAge,maxCount,rangeSize);
//                stripChart.getXYPlot().getRangeAxis().setRange(70,140);
//                stripChart.getXYPlot().getRangeAxis().setFixedAutoRange(10);
//                stripChart.getXYPlot().getRangeAxis().setAutoRange(true);
                stripChart.getXYPlot().getRangeAxis().setRangeAboutValue(ped,10);
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

        currentTime=System.currentTimeMillis();
        if (currentTime - previousTime < refreshRate) return;
        previousTime=currentTime;

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
    
    
    public EcalChannel findChannel(int crate, int slot, int chan) {
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            if (crate == cc.getCrate() && slot == cc.getSlot() && chan == cc.getChannel()) {
                return cc;
            }
        }
        throw new RuntimeException(String.format(
                "Could not find channel:  (crate,slot,channel)=(%d,%d,%d)",crate,slot,chan));
    }

    public double getAveragePedestal(int crate,int slot)
    {
        int nsum=0;
        double sum=0;
        for (int chan=0; chan<16; chan++) {
            for (EcalChannel cc : ecalConditions.getChannelCollection()) {
                if (cc.getCrate()!=crate || cc.getSlot()!=slot || cc.getChannel()!=chan) continue;
                sum += getStaticPedestal(crate,slot,chan);
                nsum ++;
            }
        }
        return (nsum>0 ? sum/nsum : sum);
    }

    public double getStaticPedestal(int crate, int slot, int chan) {
        return ecalConditions.getChannelConstants(findChannel(crate,slot,chan)).getCalibration().getPedestal();
    }
}
