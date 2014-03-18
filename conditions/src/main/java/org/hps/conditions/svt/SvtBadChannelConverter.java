package org.hps.conditions.svt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObjectException;
import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConditionsRecordCollection;
import org.hps.conditions.ConditionsTableMetaData;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates an {@link SvtBadChannelCollection} representing bad readout channels
 * in the SVT.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtBadChannelConverter extends DatabaseConditionsConverter<SvtBadChannelCollection> {

    public SvtBadChannelConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Create the collection from the conditions database. 
     * @param manager The conditions manager.
     * @param name The name of the conditions set.
     */
    public SvtBadChannelCollection getData(ConditionsManager manager, String name) {

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.
        ConditionsRecordCollection records = ConditionsRecord.find(manager, name);

        SvtBadChannelCollection collection = new SvtBadChannelCollection();
        
        // Loop over ConditionsRecords.  For this particular type of condition, multiple
        // sets of bad channels with overlapping validity are okay.
        for (ConditionsRecord record : records) {
        
            String tableName = record.getTableName();
            int collectionId = record.getCollectionId();
            
            // Query for getting back bad channel records.
            String query = "SELECT id, svt_channel_id FROM " + tableName 
                    + " WHERE collection_id = " + collectionId 
                    + " ORDER BY id ASC";
            
            ResultSet resultSet = ConnectionManager.getConnectionManager().query(query);
            
            // Loop over the records.
            try {
                while (resultSet.next()) {
                    int rowId = resultSet.getInt(1);                    
                    FieldValueMap fieldValues = new FieldValueMap();
                    fieldValues.put("svt_channel_id", resultSet.getInt(2));                    
                    SvtBadChannel newObject = _objectFactory.createObject(SvtBadChannel.class, tableName, rowId, fieldValues, true);                    
                    collection.add(newObject);
                }
            } catch (SQLException x) {
                throw new RuntimeException("Database error", x);
            } catch (ConditionsObjectException x) {
                throw new RuntimeException("Error converting to SvtBadChannel object.", x);
            }
        }
               
        return collection;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<SvtBadChannelCollection> getType() {
        return SvtBadChannelCollection.class;
    }
}
