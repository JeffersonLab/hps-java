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
