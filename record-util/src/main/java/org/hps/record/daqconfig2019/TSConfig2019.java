package org.hps.record.daqconfig2019;

import java.io.PrintStream;

/**
 * Class <code>TSConfig2019</code> stores TS configuration settings
 * parsed from the an EVIO file. 
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class TSConfig2019 extends IDAQConfig2019 {
    // Store TS prescale setting
    public static final int Single0Top = 0;
    public static final int Single1Top = 1;
    public static final int Single2Top = 2;
    public static final int Single3Top = 3;
    public static final int Single0Bot = 4;
    public static final int Single1Bot = 5;
    public static final int Single2Bot = 6;
    public static final int Single3Bot = 7;
    public static final int Pair0 = 8;
    public static final int Pair1 = 9;
    public static final int Pair2 = 10;
    public static final int Pair3 = 11;
    public static final int LED = 12;
    public static final int Cosmic = 13;
    public static final int Hodoscope = 14;
    public static final int Pulser = 15;
    public static final int Multiplicity0 = 16;
    public static final int Multiplicity1 = 17;
    public static final int FEETop = 18;
    public static final int FEEBot = 19;
    
    private int[] tsPrescales = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    
    @Override
    void loadConfig(EvioDAQParser2019 parser) {
        tsPrescales = parser.TSPrescale;       
    }
    
    /**
     * Get TS precales;
     * @return values of TS prescales
     */
    public int[] getTSPrescales() {
        return tsPrescales;
    }
    
    @Override
    public void printConfig(PrintStream ps) {
        // Print TS prescale information.
        ps.println("TS information:");
        ps.println("\t#\tTrigger\tPrescale");
        String[] triggers = {"Single0Top","Single1Top","Single2Top","Single3Top", "Single0Bot","Single1Bot","Single2Bot","Single3Bot", 
                "Pair0","Pair1", "Pair2","Pair3","LED", "Cosmic", "Hodoscope", "Pulser","Multiplicity0","Multiplicity1","FEETop","FEEBot"};
        for(int i = 0; i< tsPrescales.length; i++ ) {
            ps.printf("\t%d\t%s\t%d%n",i, triggers[i],tsPrescales[i]);
        }
    }
    
    

}
