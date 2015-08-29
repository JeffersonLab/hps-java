package org.hps.run.webapp;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public final class DatabaseUtilities {

    private static String DATASOURCE_CONTEXT = "java:comp/env/jdbc/hps_run_db_dev";
    
    public static DataSource getDataSource() {
        DataSource dataSource = null;
        try {
            dataSource = (DataSource) new InitialContext().lookup(DATASOURCE_CONTEXT);
        } catch (final NamingException e) {
            throw new RuntimeException("Error creating data source.");
        }
        if (dataSource == null) {
            throw new IllegalStateException("Data source not found");
        }
        return dataSource;
    }
}
