package org.hps.datacat.client;

/**
 * Factory class for providing user access to interfaces that have protected implementations.
 * 
 * @author Jeremy McCormick, SLAC
 */
public final class DatacatClientFactory {
        
    /**
     * Create a datacat client.
     * 
     * @return the datacat client
     */
    public DatacatClient createClient() {
        return new DatacatClientImpl();
    }
}
