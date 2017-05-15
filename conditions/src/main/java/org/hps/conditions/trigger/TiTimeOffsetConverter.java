package org.hps.conditions.trigger;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.database.AbstractConditionsObjectConverter;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.DatabaseUtilities;
import org.lcsim.conditions.ConditionsManager;

public class TiTimeOffsetConverter extends AbstractConditionsObjectConverter<TiTimeOffset> {

    public TiTimeOffset getData(final ConditionsManager manager, final String name) {

        final DatabaseConditionsManager databaseConditionsManager = DatabaseConditionsManager.getInstance();

        // Setup connection if necessary.
        boolean reopenedConnection = false;
        if (!databaseConditionsManager.isConnected()) {
            databaseConditionsManager.openConnection();
            reopenedConnection = true;
        }

        final String query = "SELECT ti_time_offset from ti_time_offsets WHERE run = " + manager.getRun();
        final ResultSet resultSet = databaseConditionsManager.selectQuery(query);
        TiTimeOffset t = null;
        try {
            if (resultSet.next()) {
                t = new TiTimeOffset(resultSet.getLong(1));
            } else {
                throw new RuntimeException("No TiTimeOffset condition exists for run " + manager.getRun());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        DatabaseUtilities.cleanup(resultSet);

        if (reopenedConnection) {
            databaseConditionsManager.closeConnection();
        }

        return t;
    }
    
    public Class<TiTimeOffset> getType() {
        return TiTimeOffset.class;
    }
}
