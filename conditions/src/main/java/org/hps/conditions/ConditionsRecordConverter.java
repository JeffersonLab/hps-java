package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.ConditionsRecord.ConditionsRecordCollection;
import org.lcsim.conditions.ConditionsManager;

/**
 * Read ConditionsRecord objects from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: ConditionsRecordConverter.java,v 1.5 2013/10/15 23:24:47 jeremy Exp $
 */
class ConditionsRecordConverter extends ConditionsObjectConverter<ConditionsRecordCollection> {

    /**
     * Get the ConditionsRecords for a run based on current configuration of the
     * <code>DatabaseConditionsManager</code>.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The matching ConditionsRecords.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ConditionsRecordCollection getData(ConditionsManager manager, String name) {

        DatabaseConditionsManager databaseConditionsManager = DatabaseConditionsManager.castFrom(manager);
        TableMetaData tableMetaData = databaseConditionsManager.findTableMetaData(name);

        if (tableMetaData == null)
            throw new RuntimeException("Failed to find meta data with key " + name);

        String query = "SELECT * from " + tableMetaData.getTableName() + " WHERE " + "run_start <= " + manager.getRun() + " AND run_end >= " + manager.getRun();

        ResultSet resultSet = databaseConditionsManager.selectQuery(query);

        // Create a collection to return.
        ConditionsObjectCollection collection;
        try {
            collection = tableMetaData.getCollectionClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            while (resultSet.next()) {
                ConditionsObject conditionsRecord = ConditionsObjectUtil.createConditionsObject(resultSet, tableMetaData);
                try {
                    collection.add(conditionsRecord);
                } catch (ConditionsObjectException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error", x);
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

    public boolean allowMultipleCollections() {
        return true;
    }
}