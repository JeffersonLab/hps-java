package org.hps.record.daqconfig2019;

import java.io.PrintStream;

/**
 * Class <code>TSConfig2019</code> stores TS configuration settings
 * parsed from the an EVIO file. 
 */

public class TSConfig2019 extends IDAQConfig2019 {
    // Store TS prescale setting
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
