package org.hps.datacat.client;

/**
 * Factory class for providing user access to interfaces with protected implementation classes.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class DatacatClientFactory {
        
    /**
     * Create a datacat client.
     * 
     * @return the datacat client
     */
    public DatacatClient createClient() {
        return new DatacatClientImpl();
    }
}
