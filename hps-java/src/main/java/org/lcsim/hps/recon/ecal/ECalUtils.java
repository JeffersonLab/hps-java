package org.lcsim.hps.recon.ecal;

import org.lcsim.event.CalorimeterHit;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: ECalUtils.java,v 1.1 2013/02/25 22:39:24 meeg Exp $
 */
public class ECalUtils {

    public static final double GeV = 1.0;
    public static final double MeV = 0.001;
    //parameters for 2014 APDs and preamp
    public static final double riseTime = 10.0; //10 pulse rise time in ns
    public static final double fallTime = 17.0; //17 pulse fall time in ns
    public static final double lightYield = 120. / MeV; // number of photons per GeV
    public static final double quantumEff = 0.7;  // quantum efficiency of the APD
    public static final double surfRatio = (10. * 10.) / (16 * 16); // surface ratio between APD and crystals
    public static final double gainAPD = 150.; // Gain of the APD
    public static final double elemCharge = 1.60217657e-19;
    public static final double gainPreAmpl = 525e12; // Gain of the preamplifier in pC/pC, true value is higher but does not take into account losses
    public static final int nBit = 12;  //number of bits used by the fADC to code a value
    public static final double maxVolt = 2.0;   //maximum volt intput of the fADC
    public static final double Req = 1.0 / 27.5; // equivalent resistance of the amplification chain
    public static final double gainFactor = 2.0 / ((Math.pow(2, nBit) - 1) * Req * lightYield * quantumEff * surfRatio * gainAPD * gainPreAmpl * elemCharge);
    public static final double ecalReadoutPeriod = 4.0; // readout period in ns, it is hardcoded in the public declaration of EcalReadoutDriver. 

    /**
     * Returns the quadrant which contains the ECal cluster
     *
     * @param ecalCluster : ECal cluster
     * @return Quadrant number
     */
    public static int getQuadrant(HPSEcalCluster ecalCluster) {
        return getQuadrant(ecalCluster.getSeedHit());
    }

    public static int getQuadrant(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        return getQuadrant(ix, iy);
    }

    public static int getQuadrant(int x, int y) {
        if (x > 0) {
            if (y > 0) {
                return 1;
            } else {
                return 4;
            }
        } else {
            if (y > 0) {
                return 2;
            } else {
                return 3;
            }
        }
    }

    public static int getHVGroup(int x, int y) {
        int absy = Math.abs(y);
        if (x > 0 || x <= -8) {
            return (23 - Math.abs(x)) / 2 + 1;
        } else {
            if (x == -7 && absy == 5) {
                return 8;
            } else if (x >= -4) {
                return 12 - Math.max(x + 4, absy - 2);
            } else {
                return 12 - Math.max(-5 - x, absy - 2);
            }
        }
    }
}
