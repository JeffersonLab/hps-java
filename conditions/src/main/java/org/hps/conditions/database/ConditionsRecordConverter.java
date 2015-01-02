package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.lcsim.conditions.ConditionsManager;

/**
 * Read ConditionsRecord objects from the conditions database and cache the conditions set.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsRecordConverter extends ConditionsObjectConverter<ConditionsRecordCollection> {

    /**
     * Get the ConditionsRecords for a run based on current configuration of the
     * <code>DatabaseConditionsManager</code>.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The matching ConditionsRecords.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ConditionsRecordCollection getData(ConditionsManager manager, String name) {

        DatabaseConditionsManager databaseConditionsManager = DatabaseConditionsManager.getInstance();
        
        // Setup connection if necessary.
        boolean reopenedConnection = false;
        if (!databaseConditionsManager.isConnected()) {
            databaseConditionsManager.openConnection();
            reopenedConnection = true;
        }
        
        TableMetaData tableMetaData = databaseConditionsManager.findTableMetaData(name);

        if (tableMetaData == null)
            throw new RuntimeException("Failed to find meta data with key " + name);

        String query = "SELECT * from " + tableMetaData.getTableName() + " WHERE " + "run_start <= " + manager.getRun() + " AND run_end >= " + manager.getRun();

        ResultSet resultSet = databaseConditionsManager.selectQuery(query);

        // Create a collection to return.
        AbstractConditionsObjectCollection collection;
        try {
            collection = tableMetaData.getCollectionClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            while (resultSet.next()) {
                ConditionsObject conditionsRecord = ConditionsObjectConverter.createConditionsObject(resultSet, tableMetaData);
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
     * @return The type handled by this converter, which is
     *         <code>ConditionsRecordCollection</code>.
     */
    public Class<ConditionsRecordCollection> getType() {
        return ConditionsRecordCollection.class;
    }
}