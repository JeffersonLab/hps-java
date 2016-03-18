package org.hps.recon.ecal;

import org.hps.conditions.database.DatabaseConditionsManager;

public final class EcalTimeWalk {

    // TODO:
    // Put parameters in the conditions database once functional form is fixed.

    // From a fit of Pass0 Run 3261 (Mode-3 FADC):
    // (p0+p1*e+e^2*p2) * exp(-(p3*e+p4*e^2+p5*e^4))
    private static final double[] pars = { 3.64218e+01, -4.60756e+02,
            9.18743e+03, 3.73873e+01, -6.57130e+01, 1.07182e+02 };

    private EcalTimeWalk() {
    }

    /**
     * Perform Time Walk Correction
     * 
     * @param time
     *            - FADC Mode-3 Hit time (ns)
     * @param energy
     *            - Pulse energy (GeV)
     * @return corrected time (ns)
     */
    public static final double correctTimeWalk(double time, double energy) {
        final double poly1 = pars[0] + pars[1] * energy + pars[2] * energy
                * energy;
        final double poly2 = pars[3] * energy + pars[4] * energy * energy
                + pars[5] * Math.pow(energy, 4);
        return time - poly1 * Math.exp(-poly2);
    }

    /*
     * Time walk parameters for pulse fitting
     */

    private static final double[] par = {
            DatabaseConditionsManager
                    .getInstance()
                    .getCachedConditions(
                            org.hps.conditions.ecal.EcalTimeWalk.class,
                            "ecal_time_walk").getCachedData().getP0(),
            DatabaseConditionsManager
                    .getInstance()
                    .getCachedConditions(
                            org.hps.conditions.ecal.EcalTimeWalk.class,
                            "ecal_time_walk").getCachedData().getP1(),
            DatabaseConditionsManager
                    .getInstance()
                    .getCachedConditions(
                            org.hps.conditions.ecal.EcalTimeWalk.class,
                            "ecal_time_walk").getCachedData().getP2(),
            DatabaseConditionsManager
                    .getInstance()
                    .getCachedConditions(
                            org.hps.conditions.ecal.EcalTimeWalk.class,
                            "ecal_time_walk").getCachedData().getP3(),
            DatabaseConditionsManager
                    .getInstance()
                    .getCachedConditions(
                            org.hps.conditions.ecal.EcalTimeWalk.class,
                            "ecal_time_walk").getCachedData().getP4() };

    /**
     * Perform Time Walk Correction for Mode 1 hits using pulse fitting
     * 
     * @param time
     *            - FADC Mode 1 hit time from pulse fitting (ns)
     * @param energy
     *            - Pulse energy from pulse fitting (GeV)
     * @return corrected time (ns)
     */
    public static final double correctTimeWalkPulseFitting(double time,
            double energy) {
        final double polyA = par[0] + par[1] * energy;
        final double polyB = par[2] + par[3] * energy + par[4]
                * Math.pow(energy, 2);
        return time - (Math.exp(polyA) + polyB);
    }

}
