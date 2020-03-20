package org.hps.record.daqconfig2019;

import java.io.PrintStream;


/**
 * Interface <code>DAQConfig2019</code> represents a configuration bank
 * generated from the 2019 DAQ configuration bank. This requires that all
 * implementing classes have the ability to load settings from the DAQ
 * bank parser and also print their contents to the terminal.
 * 
 * @author Tongtong Cao
 */
abstract class IDAQConfig2019 {
    /**
     * Updates the stored settings based on the argument parser.
     * @param parser - The EVIO DAQ bank parser.
     */
    abstract void loadConfig(EvioDAQParser2019 parser);
    
    /**
     * Prints a textual representation of the configuration bank to the
     * terminal.
     */
    public abstract void printConfig(PrintStream ps);
}