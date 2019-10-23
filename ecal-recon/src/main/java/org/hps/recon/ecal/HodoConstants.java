/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package org.hps.recon.ecal;

/**
 *
 * @author rafopar
 */
public class HodoConstants {
    
    public static final int TET_AllCh = 12;
    
    public static final int NSA = 11;  // # of samples after the threshold crossings
    public static final int NSB = 4;   // # of samples before the threshold crossing
    public static final int NSPerSample = 4;  // ns per ADC sample
    public static final int NMax_peak = 4;    // Maximum number of peaks in the readout window
    public static final double cl_hit_dtMax = 18; // in [ns]. The time difference between hits in the tile should be below this number
    public static final int nGoForNextPeak = 4;   // When threshold crossing is found, it will search for the next threshold crossing after X sample
    
    // ========== When adding hit energies of two holes from the same tile we calculate it as
    // ========== E_cl = cl_Esum_scale*(E_a + E_b)/2;
    // ========== From cosmic tests in EEL, it was found that if we use cl_Esum_scale = 1.25, then
    // ========== These distributions line up with single hole tile distributions, and they peak at 1000.
    public static final double cl_Esum_scale = 1.25; // 
    
    
    
}
