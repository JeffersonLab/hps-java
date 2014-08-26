package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.freehep.math.minuit.FCNBase;
import org.freehep.math.minuit.FunctionMinimum;
import org.freehep.math.minuit.MinosError;
import org.freehep.math.minuit.MnMinos;
import org.freehep.math.minuit.MnSimplex;
import org.freehep.math.minuit.MnUserParameters;
import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.hps.conditions.deprecated.HPSSVTConstants;
import org.lcsim.event.RawTrackerHit;
//import org.lcsim.math.chisq.ChisqProb;

/**
 * Fast fitter; currently only fits single hits. Uses Tp from ChannelConstants;
 * fits values and errors for T0 and amplitude.
 *
 * @author Sho Uemura
 */
public class ShaperLinearFitAlgorithm implements ShaperFitAlgorithm, FCNBase {

    final int nPeaks;
    final double[] times;
    final double[] amplitudes;
    final double[] amplitudeErrors;
    private ChannelConstants channelConstants;
    private double[] y;
    private double[] sigma;
    private int usedSamples[];

    public ShaperLinearFitAlgorithm() {
        nPeaks = 1;
        times = new double[nPeaks];
        amplitudes = new double[nPeaks];
        amplitudeErrors = new double[nPeaks];
    }

    public ShaperLinearFitAlgorithm(int nPeaks) {
        this.nPeaks = nPeaks;
        times = new double[nPeaks];
        amplitudes = new double[nPeaks];
        amplitudeErrors = new double[nPeaks];
    }

    @Override
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, ChannelConstants constants) {
        short[] samples = rth.getADCValues();
        return this.fitShape(samples, constants);
    }

    public Collection<ShapeFitParameters> fitShape(short[] samples, ChannelConstants constants) {
        channelConstants = constants;
        y = new double[samples.length];
        sigma = new double[samples.length];
        usedSamples = new int[samples.length];
        for (int i = 0; i < samples.length; i++) {
            y[i] = samples[i];
            sigma[i] = constants.getNoise();
            usedSamples[i] = i;
        }

        MnUserParameters myParams = new MnUserParameters();

        myParams.add("time", 0.0, HPSSVTConstants.SAMPLING_INTERVAL, -500.0, (samples.length - 1) * HPSSVTConstants.SAMPLING_INTERVAL);

        MnSimplex simplex = new MnSimplex(this, myParams, 2);
        FunctionMinimum min = simplex.minimize();

        MnMinos minos = new MnMinos(this, min);
        MinosError t0err = minos.minos(0);

        ShapeFitParameters fit = new ShapeFitParameters();

        fit.setAmp(amplitudes[0]);
        fit.setAmpErr(amplitudes[0]);
        fit.setChiSq(min.fval());
        fit.setT0(times[0]);
        fit.setT0Err((t0err.lower() + t0err.upper()) / 2);

        // System.out.format("%f\t%f\t%f\t%f\t%f\t%f\n", samples[0] - constants.getPedestal(),
        // samples[1] - constants.getPedestal(), samples[2] - constants.getPedestal(), samples[3] -
        // constants.getPedestal(), samples[4] - constants.getPedestal(), samples[5] -
        // constants.getPedestal());
        // System.out.println("start = " + bestStart + ", " + fit);
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
        CholeskyDecomposition a_qr = new CholeskyDecomposition(coeff_mat);
        RealVector solved_amplitudes = a_qr.getSolver().solve(a_vec);
        RealVector amplitude_err = a_qr.getSolver().solve(var_vec);
        for (int i = 0; i < times.length; i++) {
            amplitudes[i] = solved_amplitudes.getEntry(i);
            amplitudeErrors[i] = Math.sqrt(amplitude_err.getEntry(i));
        }
        return y_vec.subtract(sc_mat.operate(solved_amplitudes)).getNorm();
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
