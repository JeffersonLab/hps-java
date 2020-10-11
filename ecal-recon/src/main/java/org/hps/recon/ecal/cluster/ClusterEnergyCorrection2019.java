package org.hps.recon.ecal.cluster;

import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.hps.detector.ecal.EcalCrystal;
import org.hps.detector.ecal.HPSEcalDetectorElement;
// import org.jdom.DataConversionException;
// import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 * This class handles the cluster energy correction for the 2019 run, to include
 * edge corrections and sampling fractions derived from data.
 * 
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 */
public final class ClusterEnergyCorrection2019 {

    // Variables derived as the difference between data and mc noise in
    // ecal cluster energy resolution.
    static final double A = -0.00000981;
    static final double B = 0.00013725;
    static final double C = 0.000301;

    static boolean hasLoaded = false;

    // These are the three splices used to interpolate the A,B,C parameters for
    // photons
    static PolynomialSplineFunction psf_parA_p = null;
    static PolynomialSplineFunction psf_parB_p = null;
    static PolynomialSplineFunction psf_parC_p = null;

    // These are the three splices used to interpolate the A,B,C parameters for
    // electrons
    static PolynomialSplineFunction psf_parA_em = null;
    static PolynomialSplineFunction psf_parB_em = null;
    static PolynomialSplineFunction psf_parC_em = null;

    // These are the three splices used to interpolate the A,B,C parameters for
    // positrons
    static PolynomialSplineFunction psf_parA_ep = null;
    static PolynomialSplineFunction psf_parB_ep = null;
    static PolynomialSplineFunction psf_parC_ep = null;

    // Calculate the noise factor to smear the Ecal energy by
    private static double calcNoise(double energy) {
        Random r = new Random();
        double noise = r.nextGaussian() * Math.sqrt(A + B * energy + C * Math.pow(energy, 2));
        // System.out.println("energy:\t"+energy+"\tnoise:\t"+noise);
        return noise;
    }

    static final double par_cut = 35;

    /**
     * Calculate the corrected energy for the cluster.
     * 
     * @param cluster The input cluster.
     * @return The corrected energy.
     */
    public static double calculateCorrectedEnergy(HPSEcal3 ecal, Cluster cluster, boolean isMC) {
        double rawE = cluster.getEnergy();
        return computeCorrectedEnergy(ecal, cluster.getParticleId(), rawE, cluster.getPosition()[0],
                cluster.getPosition()[1], isMC);
    }

    /**
     * Calculate the corrected energy for the cluster using track position at ecal.
     * 
     * @param cluster The input cluster.
     * @return The corrected energy.
     */
    public static double calculateCorrectedEnergy(HPSEcal3 ecal, Cluster cluster, double ypos, boolean isMC) {
        double rawE = cluster.getEnergy();
        return computeCorrectedEnergy(ecal, cluster.getParticleId(), rawE, cluster.getPosition()[0], ypos, isMC);
    }

    /**
     * Calculate the corrected energy and set on the cluster.
     * 
     * @param cluster The input cluster.
     */
    public static void setCorrectedEnergy(HPSEcal3 ecal, BaseCluster cluster, boolean isMC) {
        double correctedEnergy = calculateCorrectedEnergy(ecal, cluster, isMC);
        if (isMC) {
            correctedEnergy += calcNoise(correctedEnergy);
        }
        cluster.setEnergy(correctedEnergy);
    }

    /**
     * Calculate the corrected energy and set on the cluster.
     * 
     * @param cluster The input cluster.
     */

    public static void setCorrectedEnergy(HPSEcal3 ecal, BaseCluster cluster, double ypos, boolean isMC) {
        double correctedEnergy = calculateCorrectedEnergy(ecal, cluster, ypos, isMC);
        if (isMC) {
            correctedEnergy += calcNoise(correctedEnergy);
        }
        cluster.setEnergy(correctedEnergy);
    }

    private static int loadDataFromResourceFile(int pdg, boolean isMC) {

        String fname;
        switch (pdg) {
            case 11:
                // electron
                if (isMC) {
                    fname = "2019SF_MC_parameters_electrons.dat";
                } else
                    fname = "2019SF_parameters_electrons.dat";
                break;
            case -11:
                if (isMC) {
                    fname = "2019SF_MC_parameters_positrons.dat";
                } else
                    fname = "2019SF_parameters_positrons.dat";
                break;
            case 22:
                if (isMC) {
                    fname = "2019SF_MC_parameters_photons.dat";
                } else
                    fname = "2019SF_parameters_photons.dat";
                break;
            default:
                fname = "";
        }
        if (fname == "") {
            return -1;
        }

        java.io.InputStream fis = ClusterEnergyCorrection2019.class.getResourceAsStream(fname);
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(fis));

        try {
            String line = null;
            String[] arrOfStr;
            line = br.readLine();
            arrOfStr = line.split(" ");
            if (arrOfStr.length != 1) {
                System.out.println(
                        "ClusterEnergyCorrection2019 error reading paramaterization file first line PID: " + pdg);
                return -1;
            }
            Integer N = new Integer(arrOfStr[0]);
            double[] xvals = null;
            double[] yvalsA = null;
            double[] yvalsB = null;
            double[] yvalsC = null;

            xvals = new double[N];
            yvalsA = new double[N];
            yvalsB = new double[N];
            yvalsC = new double[N];
            int iline = 0;
            while ((line = br.readLine()) != null) {
                arrOfStr = line.split(" ");
                if (arrOfStr.length != 4) {
                    System.out.println("ClusterEnergyCorrection2019 error reading paramaterization file PID: " + pdg);
                    return -1;
                }
                xvals[iline] = new Double(arrOfStr[0]);
                yvalsA[iline] = new Double(arrOfStr[1]);
                yvalsB[iline] = new Double(arrOfStr[2]);
                yvalsC[iline] = new Double(arrOfStr[3]);
                iline += 1;
            }
            if (iline != N) {
                System.out
                        .println("Error in number of lines for data: expected " + N + " got " + iline + " PID: " + pdg);
                return -1;
            }

            switch (pdg) {
                case 22:
                    psf_parA_p = new SplineInterpolator().interpolate(xvals, yvalsA);
                    psf_parB_p = new SplineInterpolator().interpolate(xvals, yvalsB);
                    psf_parC_p = new SplineInterpolator().interpolate(xvals, yvalsC);
                    break;
                case 11:
                    psf_parA_em = new SplineInterpolator().interpolate(xvals, yvalsA);
                    psf_parB_em = new SplineInterpolator().interpolate(xvals, yvalsB);
                    psf_parC_em = new SplineInterpolator().interpolate(xvals, yvalsC);
                    break;
                case -11:
                    psf_parA_ep = new SplineInterpolator().interpolate(xvals, yvalsA);
                    psf_parB_ep = new SplineInterpolator().interpolate(xvals, yvalsB);
                    psf_parC_ep = new SplineInterpolator().interpolate(xvals, yvalsC);
                    break;
            }

        } catch (IOException x) {
            System.err.format("ClusterEnergyCorrection2019: error reading parameterization file: %s%n", x);
        }

        return 0;
    }

    private static int loadDataFromResourceFiles(boolean isMC) {

        if (hasLoaded == false) {
            System.out.println("ClusterEnergyCorrection2019: load resources data");
            int ret_em = loadDataFromResourceFile(11, isMC);
            int ret_ep = loadDataFromResourceFile(-11, isMC);
            int ret_p = loadDataFromResourceFile(22, isMC);
            hasLoaded = true;
            return ((ret_ep == 0) && (ret_em == 0) && (ret_p == 0)) ? 0 : 1;
        } else {
            System.out.println("ClusterEnergyCorrection2019: resource file already loaded");
            return 0;
        }
    }

    /**
     * Calculates energy correction based on cluster raw energy and particle type as
     * per <a href=
     * "https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014"
     * >HPS Note 2014-001</a>
     * 
     * @param pdg       Particle id as per PDG
     * @param rawEnergy Raw Energy of the cluster (sum of hits with shared hit
     *                  distribution)
     * @return Corrected Energy
     */

    private static double computeCorrectedEnergy(HPSEcal3 ecal, int pdg, double rawEnergy, double xpos, double ypos,
            boolean isMC) {
        // distance to beam gap edge
        double r;
        // Get these values from the Ecal geometry:
        HPSEcalDetectorElement detElement = (HPSEcalDetectorElement) ecal.getDetectorElement();
        // double BEAMGAPTOP =
        // 22.3;//ecal.getNode().getChild("layout").getAttribute("beamgapTop").getDoubleValue();//mm
        double BEAMGAPTOP = 20.0;
        try {
            BEAMGAPTOP = ecal.getNode().getChild("layout").getAttribute("beamgapTop").getDoubleValue();
        } catch (Exception e) {
            try {
                BEAMGAPTOP = ecal.getNode().getChild("layout").getAttribute("beamgap").getDoubleValue();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        double BEAMGAPBOT = -20.0;
        try {
            BEAMGAPBOT = -ecal.getNode().getChild("layout").getAttribute("beamgapBottom").getDoubleValue();
        } catch (Exception e) {
            try {
                BEAMGAPBOT = -ecal.getNode().getChild("layout").getAttribute("beamgap").getDoubleValue();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        double BEAMGAPTOPC = BEAMGAPTOP + 13.0;// mm
        double BEAMGAPBOTC = BEAMGAPBOT - 13.0;// mm
        // x-coordinates of crystals on either side of row 1 cut out
        EcalCrystal crystalM = detElement.getCrystal(-11, 1);
        Hep3Vector posM = crystalM.getPositionFront();
        EcalCrystal crystalP = detElement.getCrystal(-1, 1);
        Hep3Vector posP = crystalP.getPositionFront();

        if ((xpos < posM.x()) || (xpos > posP.x())) {
            if (ypos > 0) {
                r = Math.abs(ypos - BEAMGAPTOP);
            } else {
                r = Math.abs(ypos - BEAMGAPBOT);
            }
        }
        // crystals above row 1 cut out
        else {
            if (ypos > 0) {
                if (ypos > (par_cut + BEAMGAPTOP)) {
                    r = Math.abs(ypos - BEAMGAPTOP);
                } else {
                    r = Math.abs(ypos - BEAMGAPTOPC);
                }
            } else {
                if (ypos > (-par_cut + BEAMGAPBOT)) {
                    r = Math.abs(ypos - BEAMGAPBOTC);
                } else {
                    r = Math.abs(ypos - BEAMGAPBOT);
                }
            }
        }

        // Eliminates corrections at outermost edges to negative cluster energies
        // 66 for positrons, 69 is safe for electrons and photons
        // Also check the spline range;
        if (r > 65.5) {
            r = 65.5;
        }
        if (r<2.5) {
            r=2.5;
        }
        if (isMC) {
            switch (pdg) {
                case 11:
                    // electron
                    return computeCorrectedEnergy(r, rawEnergy, psf_parA_em, psf_parB_em, psf_parC_em);
                case -11:
                    // positron
                    return computeCorrectedEnergy(r, rawEnergy, psf_parA_ep, psf_parB_ep, psf_parC_ep);
                case 22:
                    // photon
                    return computeCorrectedEnergy(r, rawEnergy, psf_parA_p, psf_parB_p, psf_parC_p);
                default:
                    // unknown
                    return rawEnergy;
            }
        } else {
            switch (pdg) {
                case 11:
                    // electron
                    return computeCorrectedEnergy(r, rawEnergy, psf_parA_em, psf_parB_em, psf_parC_em);
                case -11:
                    // positron
                    return computeCorrectedEnergy(r, rawEnergy, psf_parA_ep, psf_parB_ep, psf_parC_ep);
                case 22:
                    // photon
                    return computeCorrectedEnergy(r, rawEnergy, psf_parA_p, psf_parB_p, psf_parC_p);
                default:
                    // unknown
                    return rawEnergy;
            }
        }
    }

    /**
     * Calculates the energy correction to a cluster given the variables from the
     * fit as per <a href=
     * "https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014"
     * >HPS Note 2014-001</a> Note that this is correct as there is a typo in the
     * formula print in the note.
     * 
     * @param rawEnergy Raw energy of the cluster
     * @param A,B,C     from fitting in note
     * @return Corrected Energy
     */
    private static double computeCorrectedEnergy(double y, double rawEnergy, PolynomialSplineFunction splineA,
            PolynomialSplineFunction splineB, PolynomialSplineFunction splineC) {

        double A = splineA.value(y);
        double B = splineB.value(y);
        double C = splineC.value(y);

        double SF = A / rawEnergy + B / Math.sqrt(rawEnergy) + C;
        double corrEnergy = rawEnergy / SF;
        return corrEnergy;

    }

    public static void main(String[] args) {
        System.out.println("main");
        loadDataFromResourceFiles(true);
        loadDataFromResourceFiles(true);

        Random r = new Random();

        for (int ii = 0; ii < 10000; ii++) {
            double y = r.nextDouble() * 63 + 2.5; // 1..66;
            double A=psf_parA_em.value(y);
            double B=psf_parB_em.value(y);
            double C=psf_parC_em.value(y);
            System.out.println(y+" "+A+" "+B+" "+C);
        }
        
        for (int ii = 0; ii < 10000; ii++) {
            double y = r.nextDouble() * 63 + 2.5; // 1..66;
            double A=psf_parA_ep.value(y);
            double B=psf_parB_ep.value(y);
            double C=psf_parC_ep.value(y);
            System.out.println(y+" "+A+" "+B+" "+C);
        }
        
        for (int ii = 0; ii < 10000; ii++) {
            double y = r.nextDouble() * 63 + 2.5; // 2.5..65.5;
            double A=psf_parA_p.value(y);
            double B=psf_parB_p.value(y);
            double C=psf_parC_p.value(y);
            System.out.println(y+" "+A+" "+B+" "+C);
        }
        
    }
}
