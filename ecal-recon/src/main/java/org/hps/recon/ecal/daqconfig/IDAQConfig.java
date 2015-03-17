package org.hps.recon.ecal.daqconfig;


/**
 * Interface <code>DAQConfig</code> represents a configuration bank
 * generated from the DAQ configuration bank. This requires that all
 * implementing classes have the ability to load settings from the DAQ
 * bank parser and also print their contents to the terminal.
 * 
 * @author Kyle McCarty
 */
abstract class IDAQConfig {
    /**
     * Updates the stored settings based on the argument parser.
     * @param parser - The EVIO DAQ bank parser.
     */
    abstract void loadConfig(EvioDAQParser parser);
    
    /**
     * Prints a textual representation of the configuration bank to the
     * terminal.
     */
    public abstract void printConfig();
}