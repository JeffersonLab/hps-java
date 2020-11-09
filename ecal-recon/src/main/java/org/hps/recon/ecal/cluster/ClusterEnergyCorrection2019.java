package org.hps.recon.ecal.cluster;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
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

	public static String name = "ClusterEnergyCorrection2019";

	private static final byte DATA = 0;
	private static final byte MC = 1;

	static boolean[] hasLoaded = new boolean[] { false, false };

	// These are the three splices used to interpolate the A,B,C parameters for
	// photons - determined from MC
	static PolynomialSplineFunction psf_parA_p;
	static PolynomialSplineFunction psf_parB_p;
	static PolynomialSplineFunction psf_parC_p;

	// These are the three splices used to interpolate the A,B,C parameters for
	// electrons
	static PolynomialSplineFunction psf_parA_em;
	static PolynomialSplineFunction psf_parB_em;
	static PolynomialSplineFunction psf_parC_em;

	// These are the three splices used to interpolate the A,B,C parameters for
	// positrons
	static PolynomialSplineFunction psf_parA_ep;
	static PolynomialSplineFunction psf_parB_ep;
	static PolynomialSplineFunction psf_parC_ep;

	// p0 and p1 parameters for data
	static PolynomialSplineFunction psf_parP0;
	static PolynomialSplineFunction psf_parP1;

	private static void loadDataCorrectionParameters() {
		String fname;
		fname = "2019SF_parameters_data.dat";
		java.io.InputStream fis = ClusterEnergyCorrection2019.class.getResourceAsStream(fname);
		java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
		try {
			String[] arrOfStr;
			String line = br.readLine();
			arrOfStr = line.split(" ");
			if (arrOfStr.length != 1) {
				throw new RuntimeException("Error reading resource file 2019SF_parameters_data.dat first line");
			}
			Integer N = new Integer(arrOfStr[0]);
			double[] xvals = new double[N];
			double[] yvalsP0 = new double[N];
			double[] yvalsP1 = new double[N];

			int iline = 0;
			while ((line = br.readLine()) != null) {
				arrOfStr = line.split(" ");
				if (arrOfStr.length != 3) {
					throw new RuntimeException("Error reading resource file 2019SF_parameters_data.dat");
				}
				xvals[iline] = new Double(arrOfStr[0]);
				yvalsP0[iline] = new Double(arrOfStr[1]);
				yvalsP1[iline] = new Double(arrOfStr[2]);
				iline += 1;
			}
			if (iline != N) {
				throw new RuntimeException("Error in # of lines in resource file.  Excepted " + N + ", got " + iline);
			}

			psf_parP0 = new SplineInterpolator().interpolate(xvals, yvalsP0);
			psf_parP1 = new SplineInterpolator().interpolate(xvals, yvalsP1);

		} catch (IOException x) {
			throw new RuntimeException(String.format("Error reading resource file:  %s %s%n", fname, x));
		}

	}

	private static void loadDataFromResourceFile(int pdg) {

		String pname;
		switch (pdg) {
		case 11:
			pname = "electrons";
			break;
		case -11:
			pname = "positrons";
			break;
		case 22:
			pname = "photons";
			break;
		default:
			throw new RuntimeException("Unsupported PID=" + pdg);
		}

		String fname;

		fname = "2019SF_MC_parameters_" + pname + ".dat";

		System.out.println(name + ": Loading resources data:  " + fname);

		java.io.InputStream fis = ClusterEnergyCorrection2019.class.getResourceAsStream(fname);
		java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(fis));

		try {
			String[] arrOfStr;
			String line = br.readLine();
			arrOfStr = line.split(" ");
			if (arrOfStr.length != 1) {
				throw new RuntimeException("Error reading resource file first line PID: " + pdg);
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
					throw new RuntimeException("Error reading resource file first line PID: " + pdg);
				}
				xvals[iline] = new Double(arrOfStr[0]);
				yvalsA[iline] = new Double(arrOfStr[1]);
				yvalsB[iline] = new Double(arrOfStr[2]);
				yvalsC[iline] = new Double(arrOfStr[3]);
				iline += 1;
			}
			if (iline != N) {
				throw new RuntimeException(
						"Error in # of lines in resource file.  Excepted " + N + ", got " + iline + ", PID=" + pdg);
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
			throw new RuntimeException(String.format("Error reading resource file:  %s %s%n", fname, x));
		}
	}

	private static void loadDataFromResourceFiles(int type) {
		if (!hasLoaded[type]) {
			loadDataFromResourceFile(11);
			loadDataFromResourceFile(-11);
			loadDataFromResourceFile(22);

			if (type == DATA) {
				loadDataCorrectionParameters();
			}
		}
		hasLoaded[type] = true;
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

		final int type = isMC ? MC : DATA;

		loadDataFromResourceFiles(type);

		// distance to beam gap edge
		double r = ClusterCorrectionUtilities.computeYDistanceFromEdge(ecal, xpos, ypos);

		// avoid extrapolation all the way to the edge:
		if (r > 65.5) {
			r = 65.5;
		} else if (r < 2.5) {
			r = 2.5;
		}

		if (isMC) {
			switch (pdg) {
			case 11:
				// electron
				return computeCorrectedEnergy(r, rawEnergy, psf_parA_em, psf_parB_em, psf_parC_em, isMC);
			case -11:
				// positron
				return computeCorrectedEnergy(r, rawEnergy, psf_parA_ep, psf_parB_ep, psf_parC_ep, isMC);
			case 22:
				// photon
				return computeCorrectedEnergy(r, rawEnergy, psf_parA_p, psf_parB_p, psf_parC_p, isMC);
			default:
				// unknown
				return rawEnergy;
			}
		} else {
			switch (pdg) {
			case 11:
				// electron
				return computeCorrectedEnergy(r, rawEnergy, psf_parA_em, psf_parB_em, psf_parC_em, isMC);
			case -11:
				// positron
				return computeCorrectedEnergy(r, rawEnergy, psf_parA_ep, psf_parB_ep, psf_parC_ep, isMC);
			case 22:
				// photon
				return computeCorrectedEnergy(r, rawEnergy, psf_parA_p, psf_parB_p, psf_parC_p, isMC);
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
			PolynomialSplineFunction splineB, PolynomialSplineFunction splineC, boolean isMC) {

		double A = splineA.value(y);
		double B = splineB.value(y);
		double C = splineC.value(y);

		double SF, corrEnergy;
		corrEnergy = rawEnergy;
		if (isMC == true) {
			SF = A / rawEnergy + B / Math.sqrt(rawEnergy) + C;
			corrEnergy = rawEnergy / SF;
		} else {
			double p0 = psf_parP0.value(y);
			double p1 = psf_parP1.value(y);
			SF = A / rawEnergy + B / Math.sqrt(rawEnergy) + C;
			SF = SF * (p0 + p1 * rawEnergy);
			corrEnergy = rawEnergy / SF;
		}

		return corrEnergy;
	}

	/**
	 * Calculate the corrected energy for the cluster.
	 * 
	 * @param cluster The input cluster.
	 * @return The corrected energy.
	 */
	public static final double calculateCorrectedEnergy(HPSEcal3 ecal, Cluster cluster, boolean isMC) {
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
	public static final double calculateCorrectedEnergy(HPSEcal3 ecal, Cluster cluster, double ypos, boolean isMC) {
		double rawE = cluster.getEnergy();
		return computeCorrectedEnergy(ecal, cluster.getParticleId(), rawE, cluster.getPosition()[0], ypos, isMC);
	}

	/**
	 * Calculate the corrected energy and set on the cluster.
	 * 
	 * @param cluster The input cluster.
	 */
	public static final void setCorrectedEnergy(HPSEcal3 ecal, BaseCluster cluster, boolean isMC) {
		double correctedEnergy = calculateCorrectedEnergy(ecal, cluster, isMC);
		if (isMC) {
			correctedEnergy += ClusterCorrectionUtilities.calcNoise(correctedEnergy);
		}
		cluster.setEnergy(correctedEnergy);
	}

	/**
	 * Calculate the corrected energy and set on the cluster.
	 * 
	 * @param cluster The input cluster.
	 */
	public static final void setCorrectedEnergy(HPSEcal3 ecal, BaseCluster cluster, double ypos, boolean isMC) {
		double correctedEnergy = calculateCorrectedEnergy(ecal, cluster, ypos, isMC);
		if (isMC) {
			correctedEnergy += ClusterCorrectionUtilities.calcNoise(correctedEnergy);
		}
		cluster.setEnergy(correctedEnergy);
	}

	public static void main(String[] args) {

		int type = DATA;

		loadDataFromResourceFiles(type);

		Random r = new Random();

		for (int ii = 0; ii < 10000; ii++) {
			double y = r.nextDouble() * 63 + 2.5; // 1..66;
			double A = psf_parA_em.value(y);
			double B = psf_parB_em.value(y);
			double C = psf_parC_em.value(y);
			System.out.println(y + " " + A + " " + B + " " + C);
		}

		for (int ii = 0; ii < 10000; ii++) {
			double y = r.nextDouble() * 63 + 2.5; // 1..66;
			double A = psf_parA_ep.value(y);
			double B = psf_parB_ep.value(y);
			double C = psf_parC_ep.value(y);
			System.out.println(y + " " + A + " " + B + " " + C);
		}

		for (int ii = 0; ii < 10000; ii++) {
			double y = r.nextDouble() * 63 + 2.5; // 2.5..65.5;
			double A = psf_parA_p.value(y);
			double B = psf_parB_p.value(y);
			double C = psf_parC_p.value(y);
			System.out.println(y + " " + A + " " + B + " " + C);
		}

	}
}
