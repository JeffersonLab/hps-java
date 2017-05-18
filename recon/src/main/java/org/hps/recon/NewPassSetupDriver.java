package org.hps.recon;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 *
 * @author Norman A. Graf
 */
public class NewPassSetupDriver extends Driver {
 
    private String[] _collectionsToDrop = null;
    
    @Override
    protected void process(EventHeader event) {
        // Drop the requested collections
        for(int i=0; i<_collectionsToDrop.length; ++i)
        {
            event.remove(_collectionsToDrop[i]);
        }
    }
    
    public void setCollectionsToDrop(String[] collectionNames)
    {
       _collectionsToDrop = collectionNames; 
        System.out.println("Dropping the following collections from the event:");
       for(int i=0; i<_collectionsToDrop.length; ++i)
       {
           System.out.println(_collectionsToDrop[i]);
       }
    }

}
