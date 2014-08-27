package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.NonPositiveDefiniteMatrixException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Gamma;
import org.freehep.math.minuit.FCNBase;
import org.freehep.math.minuit.FunctionMinimum;
import org.freehep.math.minuit.MinosError;
import org.freehep.math.minuit.MnMinos;
import org.freehep.math.minuit.MnSimplex;
import org.freehep.math.minuit.MnUserParameters;
import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.hps.conditions.deprecated.HPSSVTConstants;
import org.lcsim.event.RawTrackerHit;

/**
 * Fast fitter; currently only fits single hits. Uses Tp from ChannelConstants;
 * fits values and errors for T0 and amplitude.
 *
 * @author Sho Uemura
 */
public class ShaperLinearFitAlgorithm implements ShaperFitAlgorithm, FCNBase {

    final int nPeaks;
    final double[] amplitudes;
    final double[] amplitudeErrors;
    private ChannelConstants channelConstants;
    private double[] y;
    private double[] sigma;
    private int usedSamples[];

    public ShaperLinearFitAlgorithm() {
        nPeaks = 1;
        amplitudes = new double[nPeaks];
        amplitudeErrors = new double[nPeaks];
    }

    public ShaperLinearFitAlgorithm(int nPeaks) {
        this.nPeaks = nPeaks;
        amplitudes = new double[nPeaks];
        amplitudeErrors = new double[nPeaks];
    }

    @Override
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, ChannelConstants constants) {
        return this.fitShape(rth.getADCValues(), constants);
    }

    public Collection<ShapeFitParameters> fitShape(short[] samples, ChannelConstants constants) {
        channelConstants = constants;
        y = new double[samples.length];
        sigma = new double[samples.length];
        usedSamples = new int[samples.length];
        for (int i = 0; i < samples.length; i++) {
            y[i] = samples[i] - constants.getPedestal();
            sigma[i] = constants.getNoise();
            usedSamples[i] = i;
        }

        MnUserParameters myParams = new MnUserParameters();

        myParams.add("time", 0.0, HPSSVTConstants.SAMPLING_INTERVAL, -500.0, (samples.length - 1) * HPSSVTConstants.SAMPLING_INTERVAL);

        MnSimplex simplex = new MnSimplex(this, myParams, 2);
        FunctionMinimum min = simplex.minimize();

        ShapeFitParameters fit = new ShapeFitParameters();

        fit.setAmp(amplitudes[0]);
        fit.setAmpErr(amplitudeErrors[0]);
        fit.setChiProb(Gamma.regularizedGammaQ(samples.length - 2, min.fval()));
        
        fit.setT0(min.userState().value(0));

        fit.setT0Err(HPSSVTConstants.SAMPLING_INTERVAL);
//        if (min.isValid()) {
//            MnMinos minos = new MnMinos(this, min);
//            MinosError t0err = minos.minos(0);
//            if (t0err.isValid()) {
//                fit.setT0Err((t0err.lower() + t0err.upper()) / 2);
//            }
//        }

//        System.out.println(fit);
        ArrayList<ShapeFitParameters> fits = new ArrayList<ShapeFitParameters>();
        fits.add(fit);
        return fits;
    }

    private double doLinFit(double[] times) {
        RealMatrix sc_mat = new Array2DRowRealMatrix(nPeaks, usedSamples.length);
        RealVector y_vec = new ArrayRealVector(usedSamples.length);
        RealVector var_vec = new ArrayRealVector(usedSamples.length);

        for (int j = 0; j < usedSamples.length; j++) {
            for (int i = 0; i < times.length; i++) {
                sc_mat.setEntry(i, usedSamples[j], getAmplitude(HPSSVTConstants.SAMPLING_INTERVAL * usedSamples[j] - times[i], channelConstants) / sigma[usedSamples[j]]);
            }
            y_vec.setEntry(usedSamples[j], y[usedSamples[j]] / sigma[usedSamples[j]]);
            var_vec.setEntry(usedSamples[j], sigma[usedSamples[j]] * sigma[usedSamples[j]]);
        }
        RealVector a_vec = sc_mat.operate(y_vec);
        RealMatrix coeff_mat = sc_mat.multiply(sc_mat.transpose());
        DecompositionSolver a_solver;
        RealVector solved_amplitudes, amplitude_err;
        try {
            CholeskyDecomposition a_cholesky = new CholeskyDecomposition(coeff_mat);
            a_solver = a_cholesky.getSolver();
            solved_amplitudes = a_solver.solve(a_vec);
            amplitude_err = a_solver.solve(sc_mat.operate(var_vec));
        } catch (NonPositiveDefiniteMatrixException e) {
            solved_amplitudes = new ArrayRealVector(nPeaks, 0.0);
            amplitude_err = new ArrayRealVector(nPeaks, Double.POSITIVE_INFINITY);
        }

        double chisq = y_vec.subtract(sc_mat.preMultiply(solved_amplitudes)).getNorm();

        for (int i = 0; i < times.length; i++) {
            amplitudes[i] = solved_amplitudes.getEntry(i);
            amplitudeErrors[i] = Math.sqrt(amplitude_err.getEntry(i));
        }
        return chisq;
    }

    private static double getAmplitude(double time, ChannelConstants channelConstants) {
        if (time < 0) {
            return 0;
        }
        return (time / channelConstants.getTp()) * Math.exp(1 - time / channelConstants.getTp());
    }

    @Override
    public double valueOf(double[] times) {
        return doLinFit(times);
    }
}
