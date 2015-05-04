package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.special.Gamma;
import org.hps.readout.svt.HPSSVTConstants;
//===> import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;

/**
 * Fast fitter; currently only fits single hits. Uses Tp from ChannelConstants;
 * fits values and errors for T0 and amplitude.
 *
 * @author Sho Uemura
 */
public class ShaperAnalyticFitAlgorithm implements ShaperFitAlgorithm {

    private boolean debug = false;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, PulseShape shape) {
        short[] samples = rth.getADCValues();
        HpsSiSensor sensor = (HpsSiSensor) rth.getDetectorElement();
        int channel = rth.getIdentifierFieldValue("strip");
        return this.fitShape(channel, samples, sensor);
        //===> return this.fitShape(rth.getADCValues(), constants);
    }

    public Collection<ShapeFitParameters> fitShape(int channel, short[] samples, HpsSiSensor sensor) {
        double minChisq = Double.POSITIVE_INFINITY;
        int bestStart = 0;
        ShapeFitParameters fit = new ShapeFitParameters();
        for (int i = 0; i < samples.length - 2; i++) {
            double chisq = fitSection(channel, samples, sensor, fit, i);
            // System.out.println("i = " + i + ", " + fit);
            if (chisq < minChisq) {
                minChisq = chisq;
                bestStart = i;
            }
        }
        fitSection(channel, samples, sensor, fit, bestStart);
        // System.out.format("%f\t%f\t%f\t%f\t%f\t%f\n", samples[0] - constants.getPedestal(),
        // samples[1] - constants.getPedestal(), samples[2] - constants.getPedestal(), samples[3] -
        // constants.getPedestal(), samples[4] - constants.getPedestal(), samples[5] -
        // constants.getPedestal());
        // System.out.println("start = " + bestStart + ", " + fit);
        ArrayList<ShapeFitParameters> fits = new ArrayList<ShapeFitParameters>();
        fits.add(fit);
        return fits;
    }

    private double fitSection(int channel, short[] samples, HpsSiSensor sensor, ShapeFitParameters fit, int start) {
        int length = samples.length - start;
        double[] y = new double[length];
        double[] t = new double[length];

        double tp = 2.5*Math.pow(sensor.getShapeFitParameters(channel)[HpsSiSensor.TP_INDEX], 0.25) * Math.pow(sensor.getShapeFitParameters(channel)[HpsSiSensor.TP_INDEX + 1], 0.75);

        for (int i = 0; i < length; i++) {
            //===> y[i] = samples[start + i] - constants.getPedestal();
            y[i] = samples[start + i] - sensor.getPedestal(channel, i);
            t[i] = HPSSVTConstants.SAMPLING_INTERVAL * i;
        }

        double[] p = new double[length];
        double[] a = new double[length];
        for (int i = 0; i < length; i++) {
            //===> p[i] = y[i] / constants.getNoise();
            p[i] = y[i] / sensor.getNoise(channel, i);
            //===> a[i] = Math.exp(1 - t[i] / constants.getTp()) / (constants.getTp() * constants.getNoise());

            a[i] = Math.exp(1 - t[i] / tp) / (tp * sensor.getNoise(channel, i));
        }

        double pa, aatt, pat, aat, aa;
        pa = 0;
        aatt = 0;
        pat = 0;
        aat = 0;
        aa = 0;
        for (int i = 0; i < length; i++) {
            pa += p[i] * a[i];
            aatt += a[i] * a[i] * t[i] * t[i];
            pat += p[i] * a[i] * t[i];
            aat += a[i] * a[i] * t[i];
            aa += a[i] * a[i];
        }

        double t0 = (pa * aatt - pat * aat) / (pa * aat - aa * pat);
        //===> double A = pa / ((Math.exp(t0 / constants.getTp()) * (aat - t0 * aa)));
        double A = pa / ((Math.exp(t0 / tp) * (aat - t0 * aa)));

        double time_var = 0;
        double height_var = 0;
        for (int i = 0; i < length; i++) {
            double dt_dp = a[i] * (aatt - t[i] * aat - t0 * (aat - t[i] * aa)) / (pa * aat - aa * pat);
            //===> double dh_dp = (a[i] * Math.exp(-1.0 * t0 / constants.getTp()) + A * dt_dp * aa) / (aat - t0 * aa) - A * dt_dp / constants.getTp();
            double dh_dp = (a[i] * Math.exp(-1.0 * t0 / tp) + A * dt_dp * aa) / (aat - t0 * aa) - A * dt_dp / tp;
            time_var += dt_dp * dt_dp;
            height_var += dh_dp * dh_dp;
        }
        t0 += HPSSVTConstants.SAMPLING_INTERVAL * start;
        fit.setAmp(A);
        fit.setAmpErr(Math.sqrt(height_var));
        fit.setT0(t0);
        fit.setT0Err(Math.sqrt(time_var));

        double chisq = 0;
        for (int i = 0; i < samples.length; i++) {
            double ti = HPSSVTConstants.SAMPLING_INTERVAL * i;
            //===> double fit_y = A * (Math.max(0, (ti - t0)) / constants.getTp()) * Math.exp(1 - (ti - t0) / constants.getTp()) + constants.getPedestal();
            double fit_y = A * (Math.max(0, (ti - t0)) / tp) * Math.exp(1 - (ti - t0) / tp) + sensor.getPedestal(channel, i);
            //===> chisq += Math.pow((fit_y - samples[i]) / constants.getNoise(), 2);
            chisq += Math.pow((fit_y - samples[i]) / sensor.getNoise(channel, i), 2);
        }

        if (A > 0 && chisq < Double.POSITIVE_INFINITY) {
            // return ChisqProb.gammp(samples.length - 2, chisq);
            fit.setChiProb(Gamma.regularizedGammaQ(samples.length - 2, chisq));
            return chisq / (samples.length - 2);
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }
}
