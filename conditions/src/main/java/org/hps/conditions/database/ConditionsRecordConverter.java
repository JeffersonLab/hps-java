package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;
import org.lcsim.conditions.ConditionsManager;

/**
 * Read {@link org.hps.conditions.api.ConditionsRecord} objects from the conditions database.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class ConditionsRecordConverter extends AbstractConditionsObjectConverter<ConditionsRecordCollection> {

    /**
     * Get the ConditionsRecords for a run based on current configuration of the conditions system.
     *
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The matching ConditionsRecords.
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
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
        ConditionsObjectCollection collection;
        try {
            collection = tableMetaData.getCollectionClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            while (resultSet.next()) {
                final ConditionsObject conditionsRecord = new ConditionsRecord();
                conditionsRecord.setConnection(databaseConditionsManager.getConnection());
                conditionsRecord.setTableMetaData(tableMetaData);
                conditionsRecord.select(resultSet.getInt(1));
                try {
                    collection.add(conditionsRecord);
                } catch (final ConditionsObjectException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (final DatabaseObjectException | SQLException e) {
            throw new RuntimeException("Error creating new conditions record.", e);
        }

        // Close the ResultSet and Statement.
        DatabaseUtilities.cleanup(resultSet);

        if (reopenedConnection) {
            databaseConditionsManager.closeConnection();
        }

        return this.getType().cast(collection);
    }

    /**
     * Get the type handled by this converter.
     *
     * @return The type handled by this converter, which is <code>ConditionsRecordCollection</code>.
     */
    @Override
    public Class<ConditionsRecordCollection> getType() {
        return ConditionsRecordCollection.class;
    }
}
