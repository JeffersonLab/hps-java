package org.hps.users.spaul.bh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Histogram {
    int Nbins;
    Histogram(String s) throws FileNotFoundException{
        this(new File(s));
    }
    double[] h, minMass, maxMass;
    public Histogram(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        Nbins = scanner.nextInt();
        minMass = new double[Nbins];
        maxMass = new double[Nbins];
        h = new double[Nbins];
        for(int i = 0; i< Nbins; i++){
            minMass[i] = scanner.nextDouble();
            maxMass[i] = scanner.nextDouble();
            h[i] = scanner.nextDouble();
        }
    }

}
