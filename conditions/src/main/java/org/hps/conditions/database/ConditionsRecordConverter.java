package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.lcsim.conditions.ConditionsManager;

/**
 * Read {@link org.hps.conditions.api.ConditionsRecord} objects from the conditions database.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ConditionsRecordConverter extends AbstractConditionsObjectConverter<ConditionsRecordCollection> {

    /**
     * Get the ConditionsRecords for a run based on current configuration of the conditions system.
     *
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The matching ConditionsRecords.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ConditionsRecordCollection getData(final ConditionsManager manager, final String name) {

        final DatabaseConditionsManager databaseConditionsManager = DatabaseConditionsManager.getInstance();

        // Setup connection if necessary.
        boolean reopenedConnection = false;
        if (!databaseConditionsManager.isConnected()) {
            databaseConditionsManager.openConnection();
            reopenedConnection = true;
        }

        final TableMetaData tableMetaData = databaseConditionsManager.findTableMetaData(name);

        if (tableMetaData == null) {
            throw new RuntimeException("Failed to find meta data with key " + name);
        }

        final String query = "SELECT * from " + tableMetaData.getTableName() + " WHERE " + "run_start <= "
                + manager.getRun() + " AND run_end >= " + manager.getRun();

        final ResultSet resultSet = databaseConditionsManager.selectQuery(query);

        // Create a collection to return.
        BaseConditionsObjectCollection collection;
        try {
            collection = tableMetaData.getCollectionClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            while (resultSet.next()) {
                final ConditionsObject conditionsRecord = AbstractConditionsObjectConverter.createConditionsObject(resultSet,
                        tableMetaData);
                collection.add(conditionsRecord);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error", x);
        }

        // Close the ResultSet and Statement.
        DatabaseUtilities.cleanup(resultSet);

        if (reopenedConnection) {
            databaseConditionsManager.closeConnection();
        }

        return getType().cast(collection);
    }

    /**
     * Get the type handled by this converter.
     * 
     * @return The type handled by this converter, which is <code>ConditionsRecordCollection</code>.
     */
    public Class<ConditionsRecordCollection> getType() {
        return ConditionsRecordCollection.class;
    }
}
