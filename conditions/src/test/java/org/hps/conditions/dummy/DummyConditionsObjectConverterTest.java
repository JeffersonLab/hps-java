package org.hps.conditions.dummy;

import junit.framework.TestCase;

import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.api.TableRegistry;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.dummy.DummyConditionsObject.DummyConditionsObjectCollection;

/**
 * Test converter API using a dummy class.
 * 
 */
public class DummyConditionsObjectConverterTest extends TestCase {

    public void testConditionsObjectConverter() throws Exception {

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setConnectionResource("/org/hps/conditions/config/jeremym_dev_connection.prop");
        manager.setXmlConfig("/org/hps/conditions/config/conditions_database_no_svt.xml");
        manager.registerConditionsConverter(new DummyConditionsObjectConverter());
        manager.setDetector("HPS-dummy-detector", 1);
        manager.openConnection();

        final TableMetaData tableMetaData = TableRegistry.getTableRegistry().findByTableName("dummy");

        final DummyConditionsObjectCollection newCollection = new DummyConditionsObjectCollection();
        newCollection.setCollectionId(42);
        newCollection.setTableMetaData(tableMetaData);
        newCollection.setConnection(manager.getConnection());

        final DummyConditionsObject object = new DummyConditionsObject(manager.getConnection(), tableMetaData);
        object.setFieldValue("dummy", 1.2345);
        newCollection.add(object);

        try {
            newCollection.insert();

            final DummyConditionsObjectCollection readCollection = manager.getCachedConditions(
                    DummyConditionsObjectCollection.class, "dummy").getCachedData();

            System.out.println("got dummy collection " + readCollection.getCollectionId() + " with "
                    + readCollection.size() + " objects");
        } finally {
            System.out.println("deleting collection " + newCollection.getCollectionId());
            newCollection.delete();
        }
    }
}
