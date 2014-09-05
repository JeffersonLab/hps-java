package org.hps.recon.tracking;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
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

    private final int nPulses;
    final double[] amplitudes;
    final double[] amplitudeErrors;
    private ChannelConstants channelConstants;
    private final double[] sigma = new double[HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES];
    private final double[] y = new double[HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES];
    private int firstUsedSample;
    private int nUsedSamples;
    private int firstFittedPulse;
    private int nFittedPulses;
    private boolean debug = false;
    private static final Logger minuitLoggger = Logger.getLogger("org.freehep.math.minuit");

    public ShaperLinearFitAlgorithm(int nPulses) {
        this.nPulses = nPulses;
        amplitudes = new double[nPulses];
        amplitudeErrors = new double[nPulses];
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
        if (debug) {
            minuitLoggger.setLevel(Level.INFO);
        } else {
            minuitLoggger.setLevel(Level.OFF);
        }
    }

    @Override
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, ChannelConstants constants) {
        return this.fitShape(rth.getADCValues(), constants);
    }

    public Collection<ShapeFitParameters> fitShape(short[] samples, ChannelConstants constants) {
        channelConstants = constants;
        double[] signal = new double[HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES];

        for (int i = 0; i < samples.length; i++) {
            signal[i] = samples[i] - constants.getPedestal();
            sigma[i] = constants.getNoise();
        }

//        if (signal[0]>300.0) {
//            debug = true;
//        }
        firstUsedSample = 0;
        nUsedSamples = samples.length;
        firstFittedPulse = 0;
        nFittedPulses = nPulses;

        if (debug) {
            System.out.print("Signal:\t");
            for (int i = 0; i < signal.length; i++) {
                System.out.format("%f\t", signal[i]);
            }
            System.out.println();
        }

        FunctionMinimum min = doRecursiveFit(signal);
//        if (!min.isValid() && nPulses == 2) {
//            System.out.format("bad fit to %d pulses, chisq %f\n", nPulses, min.fval());
//            if (!debug) {
//                debug = true;
//                doRecursiveFit(signal);
//                debug = false;
//            }
//        }
        double chisq = evaluateMinimum(min);

        ArrayList<ShapeFitParameters> fits = new ArrayList<ShapeFitParameters>();

        for (int i = 0; i < nPulses; i++) {
            ShapeFitParameters fit = new ShapeFitParameters();
            fit.setAmp(amplitudes[i]);
            fit.setAmpErr(amplitudeErrors[i]);
            fit.setChiProb(Gamma.regularizedGammaQ(samples.length - 2 * nPulses, chisq));

            fit.setT0(min.userState().value(i));

            fit.setT0Err(min.userState().error(i));
//        if (min.isValid()) {
//            MnMinos minos = new MnMinos(this, min);
//            MinosError t0err = minos.minos(0);
//            if (t0err.isValid()) {
//                fit.setT0Err((t0err.lower() + t0err.upper()) / 2);
//            }
//        }

//        System.out.println(fit);
            fits.add(fit);
        }
//        debug = false;
        return fits;
    }

    private FunctionMinimum doRecursiveFit(double[] samples) {
        if (nFittedPulses == 1) {
            System.arraycopy(samples, 0, y, 0, samples.length);
            FunctionMinimum fit = minuitFit(null);
            return fit;
        } else {
            FunctionMinimum bestFit = null;
            double bestChisq = Double.POSITIVE_INFINITY;
            double[] fitData = new double[samples.length];
            for (int split = 1; split < y.length; split++) {
                if (debug) {
                    System.out.println("Split\t" + split);
                }

                //use signal as fit input
                System.arraycopy(samples, 0, fitData, 0, samples.length);
                //use first $split samples
                firstUsedSample = 0;
                nUsedSamples = split;
                //fit only the first pulse
                nFittedPulses = 1;
                FunctionMinimum frontFit = doRecursiveFit(fitData);
                if (debug) {
                    System.out.format("front fit:\tt0=%f,\tA=%f,\tchisq=%f\n", frontFit.userState().value(0), amplitudes[firstFittedPulse], frontFit.fval());
                }

                //subtract first pulse from fit input
                for (int i = 0; i < samples.length; i++) {
                    fitData[i] -= amplitudes[firstFittedPulse] * getAmplitude(HPSSVTConstants.SAMPLING_INTERVAL * i - frontFit.userState().value(0), channelConstants);
                }

                if (debug) {
                    System.out.print("Subtracted:\t");
                    for (int i = 0; i < fitData.length; i++) {
                        System.out.format("%f\t", fitData[i]);
                    }
                    System.out.println();
                }
                //use all samples
                firstUsedSample = 0;
                nUsedSamples = y.length;
                //fit the rest of the pulses
                firstFittedPulse++;
                nFittedPulses = nPulses - firstFittedPulse;
                FunctionMinimum backFit = doRecursiveFit(fitData);

                if (debug) {
                    System.out.format("back fit:\tt0=%f,\tA=%f,\tchisq=%f\n", backFit.userState().value(0), amplitudes[firstFittedPulse], backFit.fval());
                }

                //use full signal as fit input
                System.arraycopy(samples, 0, y, 0, samples.length);
                //still using all samples
                //fit all pulses
                firstFittedPulse--;
                nFittedPulses++;
                double[] combinedGuess = new double[nFittedPulses];
                combinedGuess[0] = frontFit.userState().value(0);
                for (int i = 0; i < nFittedPulses - 1; i++) {
                    combinedGuess[i + 1] = backFit.userState().value(i);
                }
                FunctionMinimum combinedFit = minuitFit(combinedGuess);

                if (debug) {
                    System.out.format("combined fit:\tt0=%f,\tA=%f,\tt0=%f,\tA=%f,\tchisq=%f\n", combinedFit.userState().value(0), amplitudes[firstFittedPulse], combinedFit.userState().value(1), amplitudes[firstFittedPulse + 1], combinedFit.fval());
                }

                double newchisq = evaluateMinimum(combinedFit);

                if (newchisq < bestChisq) {
                    bestChisq = newchisq;
                    bestFit = combinedFit;
                }
            }

//            double newchisq = evaluateMinimum(bestFit);
            if (debug) {
                System.out.println("new chisq:\t" + bestChisq);
                System.out.format("best fit:\tt0=%f,\tA=%f,\tt0=%f,\tA=%f,\tchisq=%f\n", bestFit.userState().value(0), amplitudes[firstFittedPulse], bestFit.userState().value(1), amplitudes[firstFittedPulse + 1], bestFit.fval());
            }
            return bestFit;
        }
    }

    private double evaluateMinimum(FunctionMinimum min) {
        double[] times = new double[nFittedPulses];
        for (int i = 0; i < nFittedPulses; i++) {
            times[i] = min.userState().value(i);
        }
        return doLinFit(times);
    }

    private FunctionMinimum minuitFit(double[] guess_t) {
        if (debug) {
            System.out.print("y for fit:\t");
            for (int i = 0; i < y.length; i++) {
                System.out.format("%f\t", y[i]);
            }
            System.out.println();
        }

        if (nFittedPulses == 1) {
            double guess = 0;

            int numPositiveSamples = 0;
            int numBigSamples = 0;
            int lastUsedSample = firstUsedSample + nUsedSamples - 1;
            int firstBigSample = Integer.MAX_VALUE;
            for (int i = 0; i < nUsedSamples; i++) {
                if (y[firstUsedSample + i] > 0) {
                    numPositiveSamples++;
                    if (y[firstUsedSample + i] > 3.0 * sigma[firstUsedSample + i]) {
                        numBigSamples++;
                        if (firstUsedSample + i < firstBigSample) {
                            firstBigSample = firstUsedSample + i;
                        }
                    }
                }
            }
            boolean made_guess = false;
            boolean made_bestfit = false;
            if (nUsedSamples == 1) {
                if (firstUsedSample == 0) {
                    guess = -500.0;
                    made_bestfit = true;
                } else {
                    guess = HPSSVTConstants.SAMPLING_INTERVAL * (firstUsedSample - 0.1);
                    made_bestfit = true;
                }
            } else if (numPositiveSamples == 1 && y[lastUsedSample] > 0) {
                guess = HPSSVTConstants.SAMPLING_INTERVAL * (lastUsedSample - 0.1);
                made_bestfit = true;
            } else if (numBigSamples == 1 && y[lastUsedSample] > 3.0 * sigma[lastUsedSample] && nUsedSamples > 1 && y[lastUsedSample - 1] < 0) {
                guess = HPSSVTConstants.SAMPLING_INTERVAL * (lastUsedSample - 0.1);
                made_bestfit = true;
            } else if (nUsedSamples == 2) {
                guess = HPSSVTConstants.SAMPLING_INTERVAL * (firstUsedSample - 0.1);
                made_guess = true;
            }
            if (made_guess || made_bestfit) {
//                System.out.println("made guess " + guess);
                guess_t = new double[1];
                guess_t[0] = guess;
            }
        }

        MnUserParameters myParams = new MnUserParameters();

        for (int i = 0; i < nFittedPulses; i++) {
            if (guess_t != null && guess_t.length == nFittedPulses) {
                myParams.add("time_" + i, guess_t[i], HPSSVTConstants.SAMPLING_INTERVAL, -500.0, (y.length - 1) * HPSSVTConstants.SAMPLING_INTERVAL);
            } else {
                myParams.add("time_" + i, (i - 1) * HPSSVTConstants.SAMPLING_INTERVAL, HPSSVTConstants.SAMPLING_INTERVAL, -500.0, (y.length - 1) * HPSSVTConstants.SAMPLING_INTERVAL);
            }
        }

        MnSimplex simplex = new MnSimplex(this, myParams, 2);
        FunctionMinimum min = simplex.minimize(0, 0.001);
        return min;
    }

    private double doLinFit(double[] times) {
        if (times.length != nFittedPulses) {
            throw new RuntimeException("wrong number of parameters in doLinFit");
        }
        RealMatrix sc_mat = new Array2DRowRealMatrix(nFittedPulses, nUsedSamples);
        RealVector y_vec = new ArrayRealVector(nUsedSamples);
        RealVector var_vec = new ArrayRealVector(nUsedSamples);

        for (int j = 0; j < nUsedSamples; j++) {
            for (int i = 0; i < nFittedPulses; i++) {
                sc_mat.setEntry(i, j, getAmplitude(HPSSVTConstants.SAMPLING_INTERVAL * (firstUsedSample + j) - times[i], channelConstants) / sigma[firstUsedSample + j]);
            }
            y_vec.setEntry(j, y[firstUsedSample + j] / sigma[firstUsedSample + j]);
            var_vec.setEntry(j, sigma[firstUsedSample + j] * sigma[firstUsedSample + j]);
        }
        RealVector a_vec = sc_mat.operate(y_vec);
        RealMatrix coeff_mat = sc_mat.multiply(sc_mat.transpose());
        DecompositionSolver a_solver;
        RealVector solved_amplitudes = null, amplitude_err = null;
        boolean goodFit = true;
        try {
            CholeskyDecomposition a_cholesky = new CholeskyDecomposition(coeff_mat);
            a_solver = a_cholesky.getSolver();
            solved_amplitudes = a_solver.solve(a_vec);
            amplitude_err = a_solver.solve(sc_mat.operate(var_vec));
            if (solved_amplitudes.getMinValue() < 0) {
                goodFit = false;
            }
        } catch (NonPositiveDefiniteMatrixException e) {
            goodFit = false;
        }

        if (!goodFit) {
            solved_amplitudes = new ArrayRealVector(nFittedPulses, 0.0);
            amplitude_err = new ArrayRealVector(nFittedPulses, Double.POSITIVE_INFINITY);
        }

        double chisq = y_vec.subtract(sc_mat.preMultiply(solved_amplitudes)).getNorm();

        for (int i = 0; i < nFittedPulses; i++) {
            amplitudes[firstFittedPulse + i] = solved_amplitudes.getEntry(i);
            amplitudeErrors[firstFittedPulse + i] = Math.sqrt(amplitude_err.getEntry(i));
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
