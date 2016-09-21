package org.hps.recon.ecal;

public class TimeDependentEcalGains2016 extends TimeDependentEcalGains {
    private long[] rangeStarts = new long[]{
            1457140000,
            1457250000,
            1460200000,
            1460750000,
            1461350000,
            1461460000
    };
    private long[] rangeEnds = new long[]{
            1457230000,
            1457350000,
            1460400000,
            1461000000,
            1461450000,
            1461580000
    };
    private double[] A = new double[]{
            2.2585,
            2.29357,
            2.25799,
            2.26538,
            2.27994,
            2.26131
    };
    private double[] B = new double[]{
            .399416,
            .0388658,
            .117147,
            .163699,
            -.00861751,
            .0794
    };
    private double[] C = new double[]{
            6.574,
            30863,
            39662.9,
            40000,
            15844.2,
            25784.6
    };
    
    private double beamEnergy2016 = 2.306;
    protected double getGain(long timeStamp) {
        for(int i = 0; i<rangeStarts.length; i++){
            if(timeStamp > rangeStarts[i] && timeStamp<rangeEnds[i]){
                
                //this is from fitting the fee peak position as a function of time.  
                double fittedFeePeak = A[i]-B[i]*Math.exp(-(timeStamp-rangeStarts[i])/C[i]);
                return beamEnergy2016/fittedFeePeak;
            }
        }
        return 1;
    }
}
