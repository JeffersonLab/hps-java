package org.hps.conditions.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsTag;
import org.hps.conditions.api.ConditionsTag.ConditionsTagCollection;
import org.lcsim.conditions.ConditionsManager;

/**
 * Convert records in the <i>conditions_tags</i> table to a conditions object collection. 
 * 
 * @author Jeremy McCormick, SLAC
 */
public class ConditionsTagConverter extends AbstractConditionsObjectConverter<ConditionsTagCollection>  {
    
    /**
     * SQL SELECT string.
     */
    private static final String SELECT_SQL = "SELECT conditions_id, tag from conditions_tags where tag = ?";
   
    /**
     * Get a {@link org.hps.conditions.api.ConditionsTag.ConditionsTagCollection} which specifies a group of 
     * collections that are tagged in the <i>conditions_tags</i> table in the database.  The <code>name</code> 
     * argument is the tag name.
     *
     * @param manager the current conditions manager
     * @param name the name of the conditions set
     * @return the matching <code>ConditionsRecord</code> objects
     */
    @Override
    public ConditionsTagCollection getData(final ConditionsManager manager, final String name) {

        ConditionsTagCollection conditionsTagCollection = new ConditionsTagCollection();
        DatabaseConditionsManager dbConditionsManager = DatabaseConditionsManager.class.cast(manager);
        if (dbConditionsManager == null) {
            throw new IllegalArgumentException("The conditions manager has the wrong type.");
        }
        boolean openedConnection = dbConditionsManager.openConnection();
        Connection connection = DatabaseConditionsManager.getInstance().getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(SELECT_SQL);
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Integer conditionsId = resultSet.getInt(1);
                String tag = resultSet.getString(2);
                ConditionsTag record = new ConditionsTag(conditionsId, tag);
                conditionsTagCollection.add(record);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        } finally {
            dbConditionsManager.closeConnection(openedConnection);
        }
        if (conditionsTagCollection.size() == 0) {
            throw new IllegalArgumentException("The conditions tag " + name + " does not exist in the database.");
        }
        return conditionsTagCollection;
    }

    /**
     * Get the type handled by this converter.
     *
     * @return The type handled by this converter, which is <code>ConditionsRecordCollection</code>.
     */
    @Override
    public Class<ConditionsTagCollection> getType() {
        return ConditionsTagCollection.class;
    }    
}
