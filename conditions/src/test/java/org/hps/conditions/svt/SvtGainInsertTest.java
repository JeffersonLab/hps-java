package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.TableMetaData;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;


public class SvtGainInsertTest extends TestCase {
    
    public void setUp() {
        // TODO: Setup database connection!!!
    }
    
    public void testGainInsertTest() throws Exception {
        
        // Get manager.
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        
        // Get table info.
        TableMetaData metaData = manager.findTableMetaData(TableConstants.SVT_GAINS);
        int collectionId = manager.getNextCollectionID(metaData.getTableName());
        
        // Setup conditions object collection.
        SvtGainCollection gains = new SvtGainCollection();        
        gains.setCollectionId(collectionId);
        gains.setTableMetaData(metaData);
        gains.setIsReadOnly(false);
        
        // Insert values.
        for (int i=0; i<10; i++) {
            SvtGain gain = new SvtGain();
            gain.setFieldValue("svt_channel_id", 1);
            gain.setFieldValue("gain", 1.0);
            gain.setFieldValue("offset", 0.1);                       
            gains.add(gain);
        }        
                
        // Insert objects into database.
        gains.insert();
    }
}
