package org.hps.conditions;

import junit.framework.TestCase;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableConstants;
import org.hps.conditions.database.TableMetaData;
import org.hps.conditions.svt.SvtGain;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;

/**
 * Test some basic operations of {@link org.hps.conditions.api.ConditionsObject}
 * using the {@link org.hps.conditions.svt.SvtGain} type.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsObjectTest extends TestCase {

    public void testBasicOperations() throws ConditionsObjectException {

        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        // Create a new collection, setting its table meta data and collection
        // ID.
        TableMetaData tableMetaData = conditionsManager.findTableMetaData(TableConstants.SVT_GAINS);
        int collectionId = conditionsManager.getNextCollectionID(tableMetaData.getTableName());
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
            System.out.println("inserted row " + gain.getRowId() + " into table " + gain.getTableMetaData().getTableName() + " with collection ID " + gain.getCollectionId());
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }

        // Select the gain that was just inserted.
        SvtGain selectGain = new SvtGain();
        selectGain.setRowId(gain.getRowId());
        selectGain.setTableMetaData(tableMetaData);
        selectGain.select();
        // TODO: Check values here against the original object.

        // Update the value in the database.
        double newValue = 2.345;
        gain.setFieldValue("gain", 2.345);
        gain.update();
        System.out.println("updated gain to new value " + newValue);

        // Delete the object.
        gain.delete();
        assertEquals("The deleted object still has a valid row ID.", -1, gain.getRowId());

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
