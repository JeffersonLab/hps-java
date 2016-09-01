package org.hps.users.spaul.bh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class WindowSet {
    int N;
    
    double minMass[];
    double maxMass[];
    double resolution[];
    
    WindowSet(String s) throws FileNotFoundException{
        this(new File(s));
    }
    public WindowSet(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        N = scanner.nextInt();
        minMass = new double[N];
        maxMass = new double[N];
        resolution = new double[N];
        for(int i = 0; i< N; i++){
            minMass[i] = scanner.nextDouble();
            maxMass[i] = scanner.nextDouble();
            resolution[i] = scanner.nextDouble();
        }
        printToLatexTable();
        
    }
    void printToLatexTable(){
        System.out.println("window # & center & $\\sigma_M$  & bottom & top & width \\\\");
        for(int i = 0; i<N; i++){
            System.out.printf("%d & %.0f & %.0f & %.0f & %.0f & %.0f \\\\\n", 
                    i+1, (minMass[i]+maxMass[i])/2., resolution[i],
                    minMass[i], maxMass[i], maxMass[i]-minMass[i]);
        }
    }
    
}
