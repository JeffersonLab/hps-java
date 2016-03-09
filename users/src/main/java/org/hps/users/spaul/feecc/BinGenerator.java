package org.hps.users.spaul.feecc;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;

public class BinGenerator {
    public static void main(String arg[]) throws FileNotFoundException{
        int nBins = 32;

        PrintStream pw = new PrintStream("generatedbins.txt");
        pw.println(nBins);
        double[] thetaBins = getThetaBins(nBins);
        for(int i = 0; i< nBins; i++){
            double thetaMin = thetaBins[i];
            double thetaMax = thetaBins[i+1];
            double phiBounds[] = getPhiBounds(thetaMin, thetaMax);
            pw.printf("%d %.4f %.4f ", phiBounds.length/2, thetaMin, thetaMax);
            for(int j = 0; j< phiBounds.length; j++){
                pw.printf("%.4f ", phiBounds[j]);
            }
            pw.println();
        }
        ShowCustomBinning.main(new String[]{"generatedbins.txt"});

    }

    private static double[] getThetaBins(int nBins){
        /*double thetaMin = 0.035;
        double dTheta = .2/nBins;
        double[] bins = new double[nBins +1];
        for(int i = 0; i< nBins+1; i++){
            bins[i] = thetaMin+dTheta*i;
        }
        return bins; */
        double thetaMax = .200;
        double thetaMin = .040;

        double[] bins = new double[nBins +1];
        for(int i = 0; i<nBins+1; i++){
            bins[i] = thetaMin+i*(thetaMax-thetaMin)/nBins;
        }
        return bins;
        /*double xMin = 1/(thetaMax*thetaMax);
        double xMax = 1/(thetaMin*thetaMin);
        for(int i = 0; i< nBins+1; i++){
            double x = xMax - i*(xMax-xMin)/nBins;
            bins[i] = Math.pow(x, -.5);
        }
        return bins;*/
    }
    private static double[] getPhiBounds(double thetaMin, double thetaMax){
        double phiBins[] = new double[6];
        double dphi = .01; 
        int edgeNumber = 0;

        boolean prevInRange = false;
        for(double phi = 0; phi< 3.14; phi+= dphi){
            
            // make the angular cuts on the tracks such that the particles that go into that cut 
            // are expected to be within 4 mm (~= 2 times the angular resolution of 1.5 mrad) of 
            // the ecal cuts.  
            double d = 4;
            
            boolean inRange = EcalUtil.fid_ECal_spherical_more_strict(thetaMin, phi, d) && EcalUtil.fid_ECal_spherical_more_strict(thetaMax, phi, d)
                                && EcalUtil.fid_ECal_spherical_more_strict(thetaMin, -phi, d) && EcalUtil.fid_ECal_spherical_more_strict(thetaMax, -phi, d);
            if(inRange && !prevInRange)
                phiBins[edgeNumber++] = phi;
            if(prevInRange && !inRange)
                phiBins[edgeNumber++] = phi-dphi;
            prevInRange = inRange;  
        }
        if(phiBins[2] == 0)
            return new double[]{phiBins[0], phiBins[1]};
        if(phiBins[4] == 0)
            return new double[]{phiBins[0], phiBins[1],phiBins[2], phiBins[3]};
        
        //3 segments: choose the largest two
        if(phiBins[4] != 0 && phiBins[1] - phiBins[0] > phiBins[3]-phiBins[2] && phiBins[5] - phiBins[4] > phiBins[3]-phiBins[2]){
            return new double[]{phiBins[0], phiBins[1],phiBins[4], phiBins[5]};
        }
        if(phiBins[4] != 0 && phiBins[3] - phiBins[2] > phiBins[1]-phiBins[0] && phiBins[5] - phiBins[4] > phiBins[1]-phiBins[0]){
            return new double[]{phiBins[2], phiBins[3],phiBins[4], phiBins[5]};
        }
        return new double[]{phiBins[0], phiBins[1],phiBins[2], phiBins[3]};
        
        
    }

}
