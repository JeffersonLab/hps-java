package org.hps.conditions;

import java.sql.SQLException;

import junit.framework.TestCase;

import org.hps.conditions.svt.SvtGain;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;

/**
 * Test some basic operations of {@link org.hps.conditions.ConditionsObject}
 * using the {@link org.hps.conditions.svt.SvtGain} type.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class ConditionsObjectTest extends TestCase {
    
    String detectorName = "HPS-conditions-test";
    int runNumber = 1351;
    DatabaseConditionsManager conditionsManager;
    
    public void setUp() {
        // Create and configure the conditions manager.
        conditionsManager = DatabaseConditionsManager.createInstance();
        conditionsManager.configure("/org/hps/conditions/config/conditions_database_dev.xml");
        conditionsManager.setDetectorName(detectorName);
        conditionsManager.setRunNumber(runNumber);
        conditionsManager.setup();
    }
    
    public void testBasicOperations() throws ConditionsObjectException {    
        
        // Create a new collection.
        TableMetaData tableMetaData = conditionsManager.findTableMetaData(TableConstants.SVT_GAINS);
        int collectionId = conditionsManager.getNextCollectionId(tableMetaData.getTableName());        
        SvtGainCollection collection = new SvtGainCollection();
        collection.setTableMetaData(tableMetaData);
        try {
            collection.setCollectionId(collectionId);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }
        collection.setIsReadOnly(false);
                
        // Create a dummy conditions object and add to collection.
        SvtGain gain = new SvtGain();
        gain.setFieldValue("svt_channel_id", 1);
        gain.setFieldValue("gain", 1.234);
        gain.setFieldValue("offset", 5.678);
        collection.add(gain);
        
        // Insert into the database.
        try {
            gain.insert();
            System.out.println("inserted row " + gain.getRowId()  
                    + " into table " + gain.getTableMetaData().getTableName() 
                    + " with collection ID " + gain.getCollectionId());
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }
        
        // Select the gain that was just inserted.
        SvtGain selectGain = new SvtGain();
        selectGain.setRowId(gain.getRowId());
        selectGain.setTableMetaData(tableMetaData);
        selectGain.select();
        
        // Update the value in the database.
        double newValue = 2.345;
        gain.setFieldValue("gain", 2.345);
        gain.update();
        System.out.println("updated gain to new value " + newValue);
        
        // Delete the object.
        gain.delete();
        assertEquals("The deleted object still has a row ID.", -1, gain.getRowId());
        
        // Try an update which should fail.
        try {
            gain.update();
            throw new RuntimeException("Should not get here.");
        } catch (ConditionsObjectException e) {
            System.out.println("caught expected error: " + e.getMessage());
        }
        
        // Try a delete which should fail.
        try {
            gain.delete();
            throw new RuntimeException("Should not get here.");
        } catch (ConditionsObjectException e) {
            System.out.println("caught expected error: " + e.getMessage());
        }
    }

}
