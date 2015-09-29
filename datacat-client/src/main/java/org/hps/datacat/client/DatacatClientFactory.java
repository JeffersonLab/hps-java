package org.hps.datacat.client;


/**
 * Factory class for providing user access to interfaces that have protected implementations.
 * 
 * @author Jeremy McCormick, SLAC
 */
public final class DatacatClientFactory {
        
    /**
     * Create a data catalog client with default parameters.
     * 
     * @return the datacat client
     */
    public DatacatClient createClient() {
        return new DatacatClientImpl();
    }
    
    /**
     * Create a data catalog client with specified parameters.
     * 
     * @param url the URL of the datacat server
     * @param site the site (e.g. <code>SLAC</code>, <code>JLAB</code> etc.)
     * @param rootDir the root directory in the data catalog
     * @return the data catalog client
     */
    public DatacatClient createClient(String url, DatasetSite site, String rootDir) {
        return new DatacatClientImpl(url, site, rootDir);
    }
}
