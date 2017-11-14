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
    
    /**
     * convenience method for getting amplitudes at multiple evenly spaced 
     * time intervals
     * @param t0
     * @param dt
     * @param amplitudes
     */
    public void getAmplitudesPeakNorm(double t0, double dt, double[] amplitudes){
        for(int i = amplitudes.length-1; i>=0; i--){
            amplitudes[i] = getAmplitudePeakNorm(t0+dt*i);
        }
    }

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

        //combinations of tp and tp2:
        private double A, B;
        
        @Override
        public void setParameters(int channel, HpsSiSensor sensor) {
            tp = sensor.getShapeFitParameters(channel)[HpsSiSensor.TP_INDEX];
            tp2 = sensor.getShapeFitParameters(channel)[HpsSiSensor.TP_INDEX + 1];
            peak_t = 3.0 * Math.pow(tp * Math.pow(tp2, 3), 0.25); //approximate solution to exp(x)=1+x+x^2*tp/(2*tp2), where x=(1/tp2-1/tp)*t
            
            A = (Math.pow(tp, 2) / Math.pow(tp - tp2, 3));
            B = (tp - tp2) / (tp * tp2);
            peak_amp = getAmplitudeIntegralNorm(peak_t);
            
        }

        @Override
        public double getAmplitudeIntegralNorm(double time) {
            if (time < 0) {
                return 0;
            }
            //===> return (time / channelConstants.getTp()) * Math.exp(1 - time / channelConstants.getTp());
            //return (Math.pow(tp, 2) / Math.pow(tp - tp2, 3))
            //        * (Math.exp(-time / tp)
            //        - Math.exp(-time / tp2) * (1 + time * (tp - tp2) / (tp * tp2) + 0.5 * Math.pow(time * (tp - tp2) / (tp * tp2), 2)));
            
            return A * (Math.exp(-time / tp)
                            - Math.exp(-time / tp2) * (1 + time * B + 0.5 * time * time * B*B));
                    
            
//            return (time / tp) * Math.exp(1 - time / tp);
        }

        @Override
        public double getAmplitudePeakNorm(double time) {
            //return getAmplitudeIntegralNorm(time) / peak_amp;
            
            //avoid extra function-calls to increase speed.
            // I am only doing this optimization because the profiler told me that
            // this is where almost a third of our processing time is found. --SJP.
            
            
            //returned value is equal to getAmplitudeIntegralNorm(time) / peak_amp;
            
            if (time < 0) {
                return 0;
            }
            
            return A * (Math.exp(-time / tp)
                    - Math.exp(-time / tp2) * (1 + time * B + 0.5 * time * time * B*B))/peak_amp;
        }
        
        @Override
        public void getAmplitudesPeakNorm(double t0, double dt, double[] amplitudes) {
            
            
            double b = Math.exp(-dt/tp);
            double b2 = Math.exp(-dt/tp2);
            
            double time = t0;
            int i;
            for(i = 0; i< amplitudes.length; i++){
                if(time < 0) 
                    amplitudes[i]= 0;
                else
                    break;
                time += dt;
            }
            
            double a = A*Math.exp(-time/tp)/peak_amp;
            double a2 = A*Math.exp(-time/tp2)/peak_amp;

            for(; i< amplitudes.length; i++){
                amplitudes[i] = a - a2 * (1 + time * B + 0.5 * time * time * B*B);
                a*=b;
                a2*=b2;
                time += dt;
                //if(Math.abs(amplitudes[i]-getAmplitudePeakNorm(t0+i*dt))>1e-10)
                  //      System.out.println(amplitudes[i]+ " " + getAmplitudePeakNorm(t0+i*dt)  + " " + (amplitudes[i]-getAmplitudePeakNorm(t0+i*dt)));
            }
            
        }
    }
}
