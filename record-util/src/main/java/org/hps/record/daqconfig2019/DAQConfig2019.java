package org.hps.record.daqconfig2019;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


/**
 * Class <code>DAQConfig2019</code> holds all of the supported parameters
 * from the DAQ configuration that exists in EVIO files. These values
 * are stored in various subclasses appropriate to the parameter that
 * are accessed through this primary interface.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class DAQConfig2019 extends IDAQConfig2019 {
    // Store the configuration objects.
    private VTPConfig2019 vtpConfig = new VTPConfig2019();
    private FADCConfigEcal2019 fadcConfigEcal = new FADCConfigEcal2019();
    private FADCConfigHodo2019 fadcConfigHodo = new FADCConfigHodo2019();
    private TSConfig2019 tsConfig = new TSConfig2019();
    
    /**
     * Gets the configuration parameters for the Ecal FADC.
     * @return Returns the Ecal FADC configuration.
     */
    public FADCConfigEcal2019 getEcalFADCConfig() {
        return fadcConfigEcal;
    }
    
    /**
     * Gets the configuration parameters for the Hodoscope FADC.
     * @return Returns the Hodoscope FADC configuration.
     */
    public FADCConfigHodo2019 getHodoFADCConfig() {
        return fadcConfigHodo;
    }
    
    /**
     * Gets the configuration parameters for the SSP.
     * @return Returns the SSP configuration.
     */
    public VTPConfig2019 getVTPConfig() {
        return vtpConfig;
    }
    
    @Override
    public void loadConfig(EvioDAQParser2019 parser) {
        // Pass the configuration parser to the system-specific objects.
        vtpConfig.loadConfig(parser);
        fadcConfigEcal.loadConfig(parser);
        fadcConfigHodo.loadConfig(parser);
        tsConfig.loadConfig(parser);
        
        // Print the loaded configuration to the terminal.
        printConfig(System.out);
    }

    @Override
    public void printConfig(PrintStream ps) {
        // Print the system-specific objects.
        fadcConfigEcal.printConfig(ps);
        ps.println();
        fadcConfigHodo.printConfig(ps);
        ps.println();
        vtpConfig.printConfig(ps);
        ps.println();
        tsConfig.printConfig(ps);
    }

    public String toString() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        printConfig(ps);
        try {
            return os.toString("UTF8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
