package org.hps.recon.ecal.cluster;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 * This class handles the cluster energy correction for the 2019 run, to include
 * edge corrections and sampling fractions derived from data.
 * 
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 */
public final class ClusterEnergyCorrection2019 extends AbsClusterEnergyCorrection {

    public static String name = "ClusterEnergyCorrection2019";

    private static boolean hasLoaded = false;

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

    private static void loadDataFromResourceFile(int pdg, boolean isMC) {

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
                throw new RuntimeException("Cannot determine resource filename for PID="+pdg);
        }

        java.io.InputStream fis = ClusterEnergyCorrection2019.class.getResourceAsStream(fname);
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(fis));

        try {
            String[] arrOfStr;
            String line = br.readLine();
            arrOfStr = line.split(" ");
            if (arrOfStr.length != 1) {
                throw new RuntimeException("Error reading resource file first line PID: "+pdg);
            }
            Integer N = new Integer(arrOfStr[0]);
            double[] xvals = new double[N];
            double[] yvalsA = new double[N];
            double[] yvalsB = new double[N];
            double[] yvalsC = new double[N];

            int iline = 0;
            while ((line = br.readLine()) != null) {
                arrOfStr = line.split(" ");
                if (arrOfStr.length != 4) {
                    throw new RuntimeException("Error reading resource file first line PID: "+pdg);
                }
                xvals[iline] = new Double(arrOfStr[0]);
                yvalsA[iline] = new Double(arrOfStr[1]);
                yvalsB[iline] = new Double(arrOfStr[2]);
                yvalsC[iline] = new Double(arrOfStr[3]);
                iline += 1;
            }
            if (iline != N) {
                throw new RuntimeException("Error in # of lines in resource file.  Excepted "+N+", got "+iline+", PID="+pdg);
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
            throw new RuntimeException(String.format("Error reading resource file:  %s %s%n",fname,x));
        }
    }

    private static void loadDataFromResourceFiles(boolean isMC) {
        if (hasLoaded == false) {
            System.out.println(name+": loading resources data ...");
            loadDataFromResourceFile(11, isMC);
            loadDataFromResourceFile(-11, isMC);
            loadDataFromResourceFile(22, isMC);
            hasLoaded = true;
        }
    }

    /**
     * Calculates energy correction based on cluster raw energy and particle type as
     * per <a href=
     * "https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014"
     * >HPS Note 2014-001</a>
     * 
     * @param ecal
     * @param pdg       Particle id as per PDG
     * @param rawEnergy Raw Energy of the cluster (sum of hits with shared hit
     *                  distribution)
     * @param xpos
     * @param ypos
     * @param isMC
     * @return Corrected Energy
     */

    public static double computeCorrectedEnergy(HPSEcal3 ecal, int pdg, double rawEnergy, double xpos, double ypos,
            boolean isMC) {
        // distance to beam gap edge
        double r = ClusterCorrectionUtilities.computeYDistanceFromEdge(ecal,xpos,ypos);

        // avoid extrapolation all the way to the edge:    
        if (r > 65.5) {
            r = 65.5;
        }
        else if (r < 2.5) {
            r = 2.5;
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
