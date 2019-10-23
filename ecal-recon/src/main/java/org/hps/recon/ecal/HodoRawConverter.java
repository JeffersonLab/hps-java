/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.ecal;

import org.lcsim.event.RawTrackerHit;
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannelConstants;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import java.util.ArrayList;
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
    private boolean useUserGain = false;
    private double userGains = 0;
    private int tet = HodoConstants.TET_AllCh;

    public ArrayList<Integer> FindThresholdCrossings(RawTrackerHit hit, double ped) {

        // Getting the cellID of the hit
        final long cellID = hit.getCellID();

        // ADC values for this hit
        final short samples[] = hit.getADCValues();

        // Number of samples
        int n_samp = samples.length;

        // ==== This list will be populated with threshold crossings
        ArrayList<Integer> thr_crossings = new ArrayList<Integer>();

        double Threshold = ped + tet;

        // special case, first sample is above threshold:
        if (samples[0] > Threshold) {
            thr_crossings.add(0);
        }

        //double ped = getPedestal();
        // ===== Loop over samples to determine Threshold Crossings =====
        for (int is = 1; is < n_samp; is++) {

            if (samples[is] > Threshold && samples[is - 1] <= Threshold) {

                // found one:
                thr_crossings.add(is);

                // search for next threshold crossing begins at end of this
                // pulse:
                is = is + HodoConstants.nGoForNextPeak - 1;

                // Don't find more than NMax_peak peaks
                if (thr_crossings.size() >= HodoConstants.NMax_peak) {
                    break;
                }

            }

        }

        return thr_crossings;
    }

    public ArrayList<CalorimeterHit> getCaloHits(RawTrackerHit hit, ArrayList<Integer> thr_crosings, double ped) {

        // Getting the cellID of the hit
        final long cellID = hit.getCellID();

        // ADC values for this hit
        final short samples[] = hit.getADCValues();

        int n_samp = samples.length;

        double gain = 0;
        if (useUserGain == true) {
            gain = this.userGains;
        } else {
            gain = findChannel(cellID).getGain().getGain();
        }
        //System.out.println("The Gains = " + findChannel(cellID).getGain().toString());

        ArrayList<CalorimeterHit> curHits = new ArrayList<CalorimeterHit>();

        // ==== Will loop over threshold crossings, and for each of thr_crossing will calculate
        // ==== the integrated signal
        for (int crs_time : thr_crosings) {

            int samp_Left = java.lang.Math.max(0, crs_time - HodoConstants.NSB);
            int samp_Right = java.lang.Math.min(n_samp - 1, crs_time + HodoConstants.NSA - 1);

            double ADC_Sum = 0;
            for (int is = samp_Left; is <= samp_Right; is++) {
                ADC_Sum = ADC_Sum + samples[is];
            }

            // ==== Now subtract pedestal from the ADC Sum ====
            ADC_Sum = ADC_Sum - ped * (samp_Right - samp_Left + 1);

            double Energy = ADC_Sum * gain;
            double time = crs_time * HodoConstants.NSPerSample;

            //System.out.println("time = " + time + "     gain = " + gain + "        Energy = " + Energy + "ADC Sum is " + ADC_Sum);
            curHits.add(CalorimeterHitUtilities.create(Energy, time, cellID));
        }

        return curHits;
    }

    public double getPedestal(EventHeader event, long cellid) {

        if (useRunningPedestal && event != null) {

            Map<HodoscopeChannel, Double> runningPedMap = (Map<HodoscopeChannel, Double>) event.get("HodoRunningPedestals");
            HodoscopeChannel chan = hodoConditions.getChannels().findGeometric(cellid);

            return runningPedMap.get(chan);
        } else {
            return findChannel(cellid).getCalibration().getPedestal();
        }
    }

    public void setConditions(HodoscopeConditions condition) {
        hodoConditions = condition;
    }

    public HodoscopeChannelConstants findChannel(long cellID) {
        //System.out.println(hodoConditions.getChannels().findGeometric(cellID));
        return hodoConditions.getChannelConstants(hodoConditions.getChannels().findGeometric(cellID));
    }

    // =========== Computed Hodoscop identifiers from cellID
    public int[] getHodoIdentifiers(long cellID) {

        int[] hodo_ids = new int[4];
        hodo_ids[0] = hodoConditions.getChannels().findGeometric(cellID).getIX();
        hodo_ids[1] = hodoConditions.getChannels().findGeometric(cellID).getIY();
        hodo_ids[2] = hodoConditions.getChannels().findGeometric(cellID).getLayer();
        hodo_ids[3] = hodoConditions.getChannels().findGeometric(cellID).getHole();

        return hodo_ids;
    }

    public void setUseRunningPedestal(boolean useRunningPedestal) {
        this.useRunningPedestal = useRunningPedestal;
    }

    public void setTETAllChannels(int arg_tet) {
        this.tet = arg_tet;
    }

    public void setUseUserGain(double a_usergain) {

        this.userGains = a_usergain;
        useUserGain = true;
    }

}
