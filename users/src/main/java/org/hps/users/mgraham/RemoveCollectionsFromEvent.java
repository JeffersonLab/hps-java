package org.hps.users.mgraham;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 *
 * @author mgraham
 */
public class RemoveCollectionsFromEvent extends Driver {

    protected Set<String> collections = new HashSet<String>();

    public RemoveCollectionsFromEvent() {
    }

    public void RemoveCollectionsFromEvent(String[] collectionNames) {
        this.collections = new HashSet<String>(Arrays.asList(collectionNames));
    }
    
      public void setCollectionNames(String[] collectionNames) {
        this.collections = new HashSet<String>(Arrays.asList(collectionNames));
    }

    protected void process(EventHeader event) {
        for (String col : collections)
            if (event.hasItem(col))
                event.remove(col);
    }
}
