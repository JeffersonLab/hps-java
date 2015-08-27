package org.hps.datacat;

/**
 * Factory class for providing user access to interfaces with protected implementation classes.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class DatacatFactory {
        
    /**
     * Create a datacat client.
     * 
     * @return the datacat client
     */
    public DatacatClient createClient() {
        return new DatacatClientImpl();
    }
}
