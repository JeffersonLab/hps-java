package org.hps.analysis.dataquality;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * This {@link org.lcsim.util.Driver} sets up the Data Quality Database
 * connection
 *
 * @author Matt Graham <mgraham@slac.stanford.edu>
 * cribbed heavily from {@link org.hps.conditions.ConditionsDriver}
 */
public class DQMDatabaseDriver extends Driver {

    // Static instance of the manager.
    static DQMDatabaseManager manager;

    // Default database connection parameters, which points to the SLAC development database.
    static String _defaultConnectionResource
            = "/org/hps/analysis/dataquality_database_test_connection.properties";

    /**
     * Constructor which initializes the conditions manager with default
     * connection parameters and configuration.
     */
    public DQMDatabaseDriver() throws SQLException {
        System.out.println("DQMDatabaseDriver.ctor");
        manager = new DQMDatabaseManager();
        manager.setConnectionResource(_defaultConnectionResource);
        manager.setup();
        manager.register();
//       ResultSet result=manager.selectQuery("SELECT * from dqm where run=1111;");
        //      result.next();
        //      float  occ = result.getFloat(3);
        //      System.out.println(occ);
    }

    /**
     * Set the connection properties file resource to be used by the manager.
     *
     * @param resource the connection resource
     */
    public void setConnectionResource(String resource) {
        manager.setConnectionResource(resource);
    }

    /**
     * This method updates a new detector with SVT and ECal conditions data.
     */
    public void detectorChanged(Detector detector) {
    }

    public void endOfData() {
//        manager.closeConnection();
    }
}
