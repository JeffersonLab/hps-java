/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.ecal;

import java.util.ArrayList;
import org.lcsim.event.RawTrackerHit;
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannelConstants;
import org.lcsim.event.EventHeader;
import java.util.Map;

/**
 *
 * @author rafopar
 */
public class HodoRawConverter {

    private HodoscopeConditions hodoConditions = null;

    /**
     * If true, running pedestal is used.
     */
    private boolean useRunningPedestal = true;

    public int FindThresholdCrossings(RawTrackerHit hit) {

        // Getting the cellID of the hit
        final long cellID = hit.getCellID();

        // ADC values for this hit
        final short samples[] = hit.getADCValues();

        // Number of samples
        int n_samp = samples.length;

        // ==== This list will be populated with threshold crossings
        ArrayList<Integer> thr_crossings = new ArrayList<Integer>();

        //double ped = getPedestal();
        // ===== Loop over samples to determine Threshold Crossings =====
        for (int is = 0; is < n_samp; is++) {

        }

        return 1;
    }

    public double getPedestal(EventHeader event, long cellid) {

        if (useRunningPedestal && event != null) {

            Map<HodoscopeChannel, Double> runningPedMap = (Map<HodoscopeChannel, Double>) event.get("HodoRunningPedestals");
//            System.out.println("cellID in the getPedestal method = " + cellid);
//            System.out.println("Channels in the getPedestal is " + hodoConditions.getChannels());
            HodoscopeChannel chan = hodoConditions.getChannels().findGeometric(cellid);

            System.out.println("chan is " + chan);

            return runningPedMap.get(chan);
        } else {
            return findChannel(cellid).getCalibration().getPedestal();
        }
    }

    public void setConditions(HodoscopeConditions condition) {
        hodoConditions = condition;
    }

    public HodoscopeChannelConstants findChannel(long cellID) {
        return hodoConditions.getChannelConstants(hodoConditions.getChannels().findGeometric(cellID));
    }

}
