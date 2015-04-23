package org.hps.recon.tracking;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public abstract class PulseShape {

    public abstract void setParameters(int channel, HpsSiSensor sensor);

    public abstract double getAmplitudePeakNorm(double time);

    public abstract double getAmplitudeIntegralNorm(double time);

//    public abstract double getAmplitude(double time, int channel, HpsSiSensor sensor);
    public static class CRRC extends PulseShape {

        private double tp;

        @Override
        public void setParameters(int channel, HpsSiSensor sensor) {
            tp = sensor.getShapeFitParameters(channel)[HpsSiSensor.TP_INDEX];
        }

        @Override
        public double getAmplitudePeakNorm(double time) {
            if (time < 0) {
                return 0;
            }
            return (time / tp) * Math.exp(1 - time / tp);
        }

        @Override
        public double getAmplitudeIntegralNorm(double time) {
            if (time < 0) {
                return 0;
            }
            return (time / Math.pow(tp, 2)) * Math.exp(-time / tp);
        }
    }

    public static class FourPole extends PulseShape {

        private double tp;
        private double tp2;
        private double peak_t, peak_amp;

        @Override
        public void setParameters(int channel, HpsSiSensor sensor) {
            tp = sensor.getShapeFitParameters(channel)[HpsSiSensor.TP_INDEX];
            tp2 = sensor.getShapeFitParameters(channel)[HpsSiSensor.TP_INDEX + 1];
            peak_t = 3.0 * Math.pow(tp * Math.pow(tp2, 3), 0.25); //approximate solution to exp(x)=1+x+x^2*tp/(2*tp2), where x=(1/tp2-1/tp)*t
            peak_amp = getAmplitudeIntegralNorm(peak_t);
        }

        @Override
        public double getAmplitudeIntegralNorm(double time) {
            if (time < 0) {
                return 0;
            }
            //===> return (time / channelConstants.getTp()) * Math.exp(1 - time / channelConstants.getTp());
            return (Math.pow(tp, 2) / Math.pow(tp - tp2, 3))
                    * (Math.exp(-time / tp)
                    - Math.exp(-time / tp2) * (1 + time * (tp - tp2) / (tp * tp2) + 0.5 * Math.pow(time * (tp - tp2) / (tp * tp2), 2)));
//            return (time / tp) * Math.exp(1 - time / tp);
        }

        @Override
        public double getAmplitudePeakNorm(double time) {
            return getAmplitudeIntegralNorm(time) / peak_amp;
        }
    }
}
