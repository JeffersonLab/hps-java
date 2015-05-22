package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * Copy a {@link org.lcsim.event.Cluster} collection to a new collection via the {@link org.lcsim.event.base.BaseCluster} class's
 * copy constructor.
 * 
 * @author Jeremy McCormick
 */
public class CopyClusterCollectionDriver extends Driver {
    
    /**
     * The input collection name.
     */
    private String inputCollectionName = null;
    
    /**
     * The output collection name.
     */
    private String outputCollectionName = null;
    
    /**
     * Set to <code>true</code> to store hits in the output clusters.
     */
    private boolean storeHits = true;
    
    /**
     * Basic no argument constructor.
     */
    public CopyClusterCollectionDriver() {        
    }
    
    /**
     * Start of data hook which will make sure required arguments are set properly.
     */
    public void startOfData() {
        if (inputCollectionName == null) {
            throw new RuntimeException("inputCollectionName was never set");
        }
        if (outputCollectionName == null) {
            throw new RuntimeException("outputCollectionName was never set");
        }        
        if (inputCollectionName.equals(outputCollectionName)) {
            throw new IllegalArgumentException("inputCollectionName and outputCollectionName are the same");
        }
    }
    
    /**
     * Set the input collection name (source).
     * 
     * @param inputCollectionName the input collection name
     */
    public void setInputCollectionName(String inputCollectionName) {
        this.inputCollectionName = inputCollectionName;
    }
    
    /**
     * Set the output collection name (target).
     * 
     * @param outputCollectionName the output collection name
     */
    public void setOutputCollectionName(String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }
    
    /**
     * Set to <code>true</code> to store hits in the output clusters.
     * 
     * @return <code>true</code> to store hits in the output clusters
     */
    public void setStoreHits(boolean storeHits) {
        this.storeHits = storeHits;
    }
        
    /**
     * Process an event, copying the input collection to the output collection.
     * 
     * @param event the LCSim event
     */
    public void process(EventHeader event) {
        
        // Check if output collection already exists in event which is an error.
        if (event.hasItem(outputCollectionName)) {
            throw new RuntimeException("collection " + outputCollectionName + " already exists in event");
        }
        
        // Get the input collection.
        List<Cluster> inputClusterCollection = event.get(Cluster.class, inputCollectionName);
        
        // Copy to the output collection.
        List<Cluster> outputClusterCollection = copyClusters(inputClusterCollection);
        
        // Copy input collection's flags.
        int flags = event.getMetaData(inputClusterCollection).getFlags();
        
        // Set the store hits bit from this Driver's settings.
        if (storeHits) {
            flags = flags | (1 << LCIOConstants.CLBIT_HITS);
        } else {
            flags = flags & (0 << LCIOConstants.CLBIT_HITS);
        }
        
        // Put the copied collection into the event.
        event.put(outputCollectionName, outputClusterCollection, Cluster.class, flags);
    }
    
    /**
     * Copy clusters to a new collection (list).
     * 
     * @param clusters the input cluster list
     * @return the output cluster collection from copying the input list
     */
    public List<Cluster> copyClusters(List<Cluster> clusters) {
        List<Cluster> newCollection = new ArrayList<Cluster>();
        for (Cluster cluster : clusters) {
            // Use the base class's copy constructor to make a new cluster from the input.
            newCollection.add(new BaseCluster(cluster));
        }
        return newCollection;
    }
}
