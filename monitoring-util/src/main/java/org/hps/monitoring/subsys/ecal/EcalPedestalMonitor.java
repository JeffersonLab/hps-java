package org.hps.monitoring.subsys.ecal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/*
 * Reads output of org.hps.recon.ecal.RunningPedestalDriver and makes strip charts.
 * 
 * Baltzell
 */
public class EcalPedestalMonitor extends Driver {

    static final int REFRESH_RATE = 10*1000; // units = ms
    static final double DOMAIN_SIZE = 4*60*60*1000; // x-axis range (ms)
    static final int crates[]={1,2};
    static final int slots[]={3,4,5,6,7,8,9,14,15,16,17,18,19,20};
    static final String slotNames[]={"Sl3","Sl4","Sl5","Sl6","Sl7","Sl8","Sl9","Sl14","Sl15","Sl16","Sl17","Sl18","Sl19","Sl20"};
    static final String collectionName = "EcalRunningPedestals";
   
    long currentTime;
    long previousTime=0;
    int nDetectorChanges=0;
    private EcalConditions ecalConditions = null;
    List<JFreeChart> charts = new ArrayList<JFreeChart>();
    
    public void detectorChanged(Detector detector) {
        if (nDetectorChanges++ > 0) return;
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        MonitoringPlotFactory plotFactory = 
            (MonitoringPlotFactory) AIDA.defaultInstance().analysisFactory().createPlotterFactory("ECal Pedestal Monitoring");
        for (int crate : crates) {
            charts.add(plotFactory.createTimeSeriesChart(
                    "Crate " + crate,
                    "Offset Pedestal (ADC)", 
                    slots.length, slotNames,
                    DOMAIN_SIZE));
        }
    }
    
    @Override
    public void process(EventHeader event) {

        if (!event.hasItem(collectionName)) return;

        currentTime = System.currentTimeMillis();
        if (currentTime - previousTime < REFRESH_RATE) return;
        previousTime = currentTime;

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
        long now = System.currentTimeMillis();
        for (int crate : pedsum.keySet()) {
            TimeSeriesCollection cc=getTimeSeriesCollection(crate-1);
            JFreeChart chart=charts.get(crate-1);
            DateAxis ax=(DateAxis)chart.getXYPlot().getDomainAxis();
            ax.setRange(now-DOMAIN_SIZE,now);
            for (int slot=0; slot<slots.length; slot++) {
                double ped = pedsum.get(crate).get(slots[slot]) / npedsum.get(crate).get(slots[slot]);
                ped -= getAveragePedestal(crate,slots[slot])-slot+9;
                cc.getSeries(slot).addOrUpdate(new Second(new Date()),ped);
            }
        }
    }
    
    TimeSeriesCollection getTimeSeriesCollection(int chartIndex) {
        return (TimeSeriesCollection) charts.get(chartIndex).getXYPlot().getDataset();
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
