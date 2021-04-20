package org.hps.recon.filtering;

import java.util.List;
import org.lcsim.event.EventHeader;

/**
 * Accept events where the specified collection exists and is of the required
 * size range.
 */
public class CollectionSizeFilter extends EventReconFilter {

    private String collectionName = "UnconstrainedV0Candidates";
    private int minSize = 1;
    private int maxSize = Integer.MAX_VALUE;

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (event.hasCollection(Object.class, collectionName)) {
            List<Object> collection = event.get(Object.class, collectionName);
            if (collection.size() < minSize || collection.size() > maxSize) {
                skipEvent();
            }
        } else {
            skipEvent();
        }
        incrementEventPassed();
    }
}
