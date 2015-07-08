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
 * @author Jeremy McCormick, SLAC
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

        final DummyConditionsObjectCollection collection = this.manager
                .newCollection(DummyConditionsObjectCollection.class);

        int collectionId = this.manager.getCollectionId(collection, "foo bar baz");
        System.out.println("created new collection " + collectionId);

        collectionId = this.manager.getCollectionId(collection, null);
        System.out.println("created new collection " + collectionId);
    }
}
