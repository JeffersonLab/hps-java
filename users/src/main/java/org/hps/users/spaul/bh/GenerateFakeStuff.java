package org.hps.users.spaul.bh;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class GenerateFakeStuff {
    static  Random random = new Random();
    static PolynomialPlusGaussian pplusg;
    public static void main(String arg[]) throws FileNotFoundException{

        PrintWriter pw1 = new PrintWriter(arg[0]);
        double signalSize = Double.parseDouble(arg[1]);
        pw1.println(1000);
        

        double min = 0;
        double max = 100;
        pplusg = new PolynomialPlusGaussian(3);
        //pplusg.p = new double[]{10000, -300, 20};

        pplusg.p = randomPoly(min, max);
        pplusg.mean = 50;
        pplusg.sigma = 1;
        pplusg.N = signalSize*Math.sqrt(pplusg.get(pplusg.mean));

        System.out.println(pplusg);
        for(int i = 0; i< 1000; i++){
            double x = i/10.;
            double y = pplusg.get(x);

            pw1.println(i/10. + " " + (i+1)/10. + " " + Math.round(y + Math.sqrt(y)*random.nextGaussian()));
        }
        pw1.close();

        PrintWriter pw2 = new PrintWriter("windows.txt");
        pw2.println(90);
        for(int i = 0; i< 90; i++){

            pw2.println(i + " " + (i+10) + "  " + 1);
            
        }
        pw2.close();
    }
    private static double[] randomPoly(double min, double max) {
    
        double[] p = new double[]{10000*(.7+.6*random.nextDouble()), -30*random.nextGaussian(), .2*random.nextGaussian()};
        return p;
    }
}
