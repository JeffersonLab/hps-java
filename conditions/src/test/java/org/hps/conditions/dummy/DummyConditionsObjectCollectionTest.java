package org.hps.conditions.dummy;

import java.sql.Connection;

import junit.framework.TestCase;

import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.api.TableRegistry;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.dummy.DummyConditionsObject.DummyConditionsObjectCollection;

public class DummyConditionsObjectCollectionTest extends TestCase {

    public void testBaseConditionsObjectCollection() throws Exception {

        // Configure the conditions system.
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setConnectionResource("/org/hps/conditions/config/jeremym_dev_connection.prop");
        manager.setXmlConfig("/org/hps/conditions/config/conditions_database_no_svt.xml");
        final Connection connection = manager.getConnection();

        // Setup basic table meta data.
        final TableMetaData tableMetaData = TableRegistry.getTableRegistry().findByTableName("dummy");

        // Create a new collection.
        final DummyConditionsObjectCollection collection = new DummyConditionsObjectCollection(connection,
                tableMetaData);

        // Add object to collection.
        final DummyConditionsObject object1 = new DummyConditionsObject(connection, tableMetaData);
        object1.setFieldValue("dummy", 1.0);
        collection.add(object1);

        // Add object to collection.
        final DummyConditionsObject object2 = new DummyConditionsObject(connection, tableMetaData);
        object2.setFieldValue("dummy", 2.0);
        collection.add(object2);

        // Insert all objects into the database.
        collection.insert();

        System.out.println(collection.size() + " objects inserted into " + collection.getCollectionId());

        assertTrue("Collection isNew returned wrong value.", !collection.isNew());

        // Create another collection.
        final DummyConditionsObjectCollection anotherCollection = new DummyConditionsObjectCollection(connection,
                tableMetaData);

        // Select the previously created objects into this collection by using the collection_id value.
        anotherCollection.select(collection.getCollectionId());
        System.out.println("selected " + anotherCollection.size() + " objects into collection");

        // Change the objects locally.
        anotherCollection.get(0).setFieldValue("dummy", 3.0);
        anotherCollection.get(1).setFieldValue("dummy", 4.0);

        // Update all objects.
        System.out.println("updating objects from collection " + collection.getCollectionId());
        anotherCollection.update();

        // Delete all objects.
        System.out.println("deleting objects from collection " + collection.getCollectionId());
        collection.delete();
    }
}
