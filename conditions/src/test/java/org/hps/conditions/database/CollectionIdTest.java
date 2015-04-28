/**
 *
 */
package org.hps.conditions.database;

import java.sql.SQLException;

import junit.framework.TestCase;

import org.hps.conditions.dummy.DummyConditionsObject.DummyConditionsObjectCollection;

/**
 * Test adding a new collection and getting its unique ID.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class CollectionIdTest extends TestCase {

    /**
     * The conditions manager.
     */
    private DatabaseConditionsManager manager;

    /**
     * Setup the test.
     */
    @Override
    public void setUp() {
        this.manager = DatabaseConditionsManager.getInstance();
        this.manager.setConnectionResource("/org/hps/conditions/config/jeremym_dev_connection.prop");
        this.manager.openConnection();
    }

    /**
     * Tear down the test.
     */
    @Override
    public void tearDown() {
        this.manager.closeConnection();
    }

    /**
     * Run the test.
     *
     * @throws SQLException if there is an error executing SQL queries
     */
    public void testCollectionId() throws SQLException {
    	
    	DummyConditionsObjectCollection collection = manager.newCollection(DummyConditionsObjectCollection.class);
    	
        int collectionId = this.manager.getCollectionId(collection, "test add", "foo bar baz");
        System.out.println("created new collection " + collectionId);

        collectionId = this.manager.getCollectionId(collection, null, null);
        System.out.println("created new collection " + collectionId);
    }
}
