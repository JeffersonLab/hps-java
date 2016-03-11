package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * Copy a {@link org.lcsim.event.Cluster} collection to a new collection via the {@link org.lcsim.event.base.BaseCluster} class's copy constructor.
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
     * Copy clusters to a new collection (list).
     *
     * @param clusters the input cluster list
     * @return the output cluster collection from copying the input list
     */
    public List<Cluster> copyClusters(final List<Cluster> clusters) {
        final List<Cluster> newCollection = new ArrayList<Cluster>();
        for (final Cluster cluster : clusters) {
            // Use the base class's copy constructor to make a new cluster from the input.
            final BaseCluster newCluster = new BaseCluster(cluster);

            // Turn off automatic property calculation.
            newCluster.setNeedsPropertyCalculation(false);

            // Add new cluster to output collection.
            newCollection.add(newCluster);
        }
        return newCollection;
    }

    /**
     * Process an event, copying the input collection to the output collection.
     *
     * @param event the LCSim event
     */
    @Override
    public void process(final EventHeader event) {

        // Check if output collection already exists in event which is an error.
        if (event.hasItem(outputCollectionName)) {
            throw new RuntimeException("collection " + outputCollectionName + " already exists in event");
        }

        // Get the input collection.
        final List<Cluster> inputClusterCollection = event.get(Cluster.class, inputCollectionName);

        // Copy to the output collection.
        final List<Cluster> outputClusterCollection = this.copyClusters(inputClusterCollection);

        // Copy input collection's flags.
        int flags = event.getMetaData(inputClusterCollection).getFlags();

        // Set the store hits bit from this Driver's settings.
        if (storeHits) {
            flags = flags | 1 << LCIOConstants.CLBIT_HITS;
        } else {
            flags = flags & 0 << LCIOConstants.CLBIT_HITS;
        }

        // Put the copied collection into the event.
        event.put(outputCollectionName, outputClusterCollection, Cluster.class, flags);
    }

    /**
     * Set the input collection name (source).
     *
     * @param inputCollectionName the input collection name
     */
    public void setInputCollectionName(final String inputCollectionName) {
        this.inputCollectionName = inputCollectionName;
    }

    /**
     * Set the output collection name (target).
     *
     * @param outputCollectionName the output collection name
     */
    public void setOutputCollectionName(final String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }

    /**
     * Set to <code>true</code> to store hits in the output clusters.
     * 
     * @param store <code>true</code> to store hits; <code>false</code> to not store hits
     */
    public void setStoreHits(final boolean storeHits) {
        this.storeHits = storeHits;
    }

    /**
     * Start of data hook which will make sure required arguments are set properly.
     */
    @Override
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
}
