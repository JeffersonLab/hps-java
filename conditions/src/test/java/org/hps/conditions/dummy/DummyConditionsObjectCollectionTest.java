package org.hps.conditions.dummy;

import java.io.File;
import java.sql.Connection;
import java.util.Date;

import junit.framework.TestCase;

import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectUtilities;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.api.TableRegistry;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.dummy.DummyConditionsObject.DummyConditionsObjectCollection;

/**
 * Test collections API using a dummy class.
 *
 */
public class DummyConditionsObjectCollectionTest extends TestCase {

    private Connection connection;
    final TableMetaData tableMetaData = TableRegistry.getTableRegistry().findByTableName("dummy");

    @Override
    public void setUp() {
        // Configure the conditions system.
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setConnectionResource("/org/hps/conditions/config/jeremym_dev_connection.prop");
        manager.setXmlConfig("/org/hps/conditions/config/conditions_database_no_svt.xml");
        this.connection = manager.getConnection();
    }

    public void testBaseConditionsObjectCollection() throws Exception {

        System.out.println("DummyConditionsObjectCollectionTest.testBaseConditionsObjectCollection");

        // Create a new collection.
        final DummyConditionsObjectCollection collection = new DummyConditionsObjectCollection(this.connection,
                this.tableMetaData);

        // Add object to collection.
        final DummyConditionsObject object1 = new DummyConditionsObject(this.connection, this.tableMetaData);
        object1.setFieldValue("dummy", 1.0);
        object1.setFieldValue("dummy_ts", new Date());
        object1.setFieldValue("dummy_dt", new Date());
        collection.add(object1);

        // Add object to collection.
        final DummyConditionsObject object2 = new DummyConditionsObject(this.connection, this.tableMetaData);
        object2.setFieldValue("dummy", 2.0);
        object2.setFieldValue("dummy_ts", new Date());
        object2.setFieldValue("dummy_dt", new Date());
        collection.add(object2);

        // Insert all objects into the database.
        collection.insert();

        System.out.println(collection.size() + " objects inserted into " + collection.getCollectionId());
        System.out.println(collection);

        assertTrue("Collection isNew returned wrong value.", !collection.isNew());

        // Create another collection.
        final DummyConditionsObjectCollection anotherCollection = new DummyConditionsObjectCollection(this.connection,
                this.tableMetaData);

        // Select the previously created objects into this collection by using the collection_id value.
        anotherCollection.select(collection.getCollectionId());
        System.out.println("selected " + anotherCollection.size() + " objects into collection");

        // Change the objects locally.
        anotherCollection.get(0).setFieldValue("dummy", 3.0);
        anotherCollection.get(0).setFieldValue("dummy_ts", new Date());
        anotherCollection.get(0).setFieldValue("dummy_dt", new Date());
        anotherCollection.get(1).setFieldValue("dummy", 4.0);
        anotherCollection.get(1).setFieldValue("dummy_ts", new Date());
        anotherCollection.get(1).setFieldValue("dummy_dt", new Date());

        // Update all objects.
        System.out.println("updating objects from collection " + collection.getCollectionId());
        anotherCollection.update();

        // Delete all objects.
        System.out.println("deleting objects from collection " + collection.getCollectionId());
        collection.delete();
    }

    public void testCsv() throws Exception {

        System.out.println("DummyConditionsObjectCollectionTest.testCsv");

        // Create a new collection.
        final DummyConditionsObjectCollection collection = new DummyConditionsObjectCollection(this.connection,
                this.tableMetaData);

        // Add object to collection.
        final DummyConditionsObject object1 = new DummyConditionsObject(this.connection, this.tableMetaData);
        object1.setFieldValue("dummy", 1.0);
        object1.setFieldValue("dummy_ts", new Date());
        object1.setFieldValue("dummy_dt", new Date());
        collection.add(object1);

        // Add object to collection.
        final DummyConditionsObject object2 = new DummyConditionsObject(this.connection, this.tableMetaData);
        object2.setFieldValue("dummy", 2.0);
        object2.setFieldValue("dummy_ts", new Date());
        object2.setFieldValue("dummy_dt", new Date());
        collection.add(object2);

        // Write to CSV file.
        collection.writeCsv(new File("dummy.csv"));
        System.out.println("wrote CSV values");
        System.out.println(collection);

        // Create an object collection.
        final ConditionsObjectCollection<?> csvCollection = ConditionsObjectUtilities.newCollection("dummy");

        // Load CSV data.
        collection.loadCsv(new File("dummy.csv"));

        System.out.println("loaded CSV values");
        System.out.println(csvCollection);
    }

}
