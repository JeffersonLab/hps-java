package org.hps.conditions.dummy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.TestCase;

import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.api.TableRegistry;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * Test object API using a dummy class.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class DummyConditionsObjectTest extends TestCase {

    /**
     * Dummy value.
     */
    private static final double DUMMY_VALUE1 = 1.0;

    /**
     * Another dummy value.
     */
    private static final double DUMMY_VALUE2 = 2.0;

    private static DatabaseConditionsManager manager;

    @Override
    public void setUp() {
        // Configure the conditions system. This uses a local development database that is not globally accessible.
        manager = DatabaseConditionsManager.getInstance();
        manager.setConnectionResource("/org/hps/conditions/config/jeremym_dev_connection.prop");
    }

    /**
     * Cleanup the table used for the test.
     */
    @Override
    public void tearDown() {
        Statement statement = null;
        try {
            statement = manager.getConnection().createStatement();
            statement.executeUpdate("DELETE from dummy");
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        manager.closeConnection();
    }

    /**
     * Perform the test.
     *
     * @throws Exception if errors occurred when performing the test
     */
    public void testBaseConditionsObject() throws Exception {

        // Open the database connection.
        final Connection connection = manager.getConnection();

        // Get table meta data.
        final TableMetaData tableMetaData = TableRegistry.getTableRegistry().findByTableName("dummy");

        assertNotNull("No meta data found for dummy table.", tableMetaData);

        // Insert a new object.
        final DummyConditionsObject newObject = new DummyConditionsObject(connection, tableMetaData);

        assertEquals("The isNew method returned the wrong value.", true, newObject.isNew());

        // The insert operation should fail with an error.
        try {
            newObject.insert();
            throw new RuntimeException("The insert operation on an invalid object should have failed.");
        } catch (final DatabaseObjectException e) {
            System.err.println(e.getMessage());
        }

        // The delete operation should fail with an error.
        try {
            newObject.delete();
            throw new RuntimeException("The delete operation on an invalid object should have failed.");
        } catch (final DatabaseObjectException e) {
            System.err.println(e.getMessage());
        }

        // The update operation should fail with an error.
        try {
            newObject.update();
            throw new RuntimeException("The update operation should have failed.");
        } catch (final DatabaseObjectException e) {
            e.printStackTrace();
        }

        newObject.setFieldValue("collection_id", 42); /* Use an arbitrary collection ID value. */
        newObject.setFieldValue("dummy", DUMMY_VALUE1);
        newObject.insert();
        System.out.println("Inserted object with id " + newObject.getRowId() + " into "
                + newObject.getTableMetaData().getTableName() + " table.");
        assertEquals("The isNew method returned the wrong value.", false, newObject.isNew());
        assertEquals("Object does not have a valid collection after insert.", true, newObject.hasValidCollectionId());

        // Select into another object by ID.
        final DummyConditionsObject anotherObject = new DummyConditionsObject(connection, tableMetaData);
        final int rowId = newObject.getRowId();
        anotherObject.select(rowId);
        System.out.println("Selected row " + rowId + " into another object.");

        // Check that the selection into another object worked.
        assertEquals("Selected object has wrong row id.", newObject.getRowId(), anotherObject.getRowId());
        assertTrue("Select object does not have valid collection.", anotherObject.hasValidCollectionId());
        assertEquals("Selected object has wrong collection id.", newObject.getCollectionId(),
                anotherObject.getCollectionId());
        assertEquals("Selected object has wrong value.", newObject.getDummy(), anotherObject.getDummy());

        // Update the same object.
        newObject.setFieldValue("dummy", DUMMY_VALUE2);
        System.out.println("Set dummy to " + DUMMY_VALUE2 + " in existing object.");
        assertEquals("The value is wrong before update.", DUMMY_VALUE2, newObject.getDummy());
        assertEquals("The updated method returned the wrong value.", true, newObject.update());
        assertEquals("The value is wrong after update.", DUMMY_VALUE2, newObject.getDummy());

        // Update which should be ignored on non-dirty record.
        // assertEquals("The update method should have returned false.", false, newObject.update());

        // Select again into another object.
        anotherObject.select(newObject.getRowId());

        // Select into another object using the row ID.
        assertEquals("Select object has wrong value after update.", newObject.getDummy(), DUMMY_VALUE2);

        // Delete the object.
        newObject.delete();

        System.out.println("Deleted object.");

        assertEquals("The isNew method returned the wrong value after delete.", true, newObject.isNew());

        final boolean selected = anotherObject.select(rowId);
        assertEquals("The select operation returned the wrong value after delete.", false, selected);
    }
}
