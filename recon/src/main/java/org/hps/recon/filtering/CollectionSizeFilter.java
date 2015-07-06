package org.hps.recon.filtering;

import java.util.List;
import org.lcsim.event.EventHeader;

/**
 * Accept events where the specified collection exists and is of at least the
 * required size.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class CollectionSizeFilter extends EventReconFilter {

    private String collectionName = "UnconstrainedV0Candidates";
    private int minSize = 1;

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (event.hasCollection(Object.class, collectionName)) {
            List<Object> collection = event.get(Object.class, collectionName);
            if (collection.size() < minSize) {
                skipEvent();
            }
        } else {
            skipEvent();
        }
        incrementEventPassed();
    }
}
