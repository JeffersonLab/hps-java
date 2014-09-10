package org.hps.conditions.svt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.config.ResourceConfiguration;

/**
 * Load an SVT configuration XML file into the conditions database from a file.
 */
public class SvtConfigurationLoader {
    
    private static final String INSERT_SQL = 
            "INSERT INTO svt_configurations(collection_id, filename, content) values(?, ?, ?)";
    
    public static void main(String[] args) {
        if (args.length == 0)
            throw new RuntimeException("ERROR: Path to XML configuration file is required!");
        new SvtConfigurationLoader().insert(new File(args[0]));
    }
    
    DatabaseConditionsManager manager;
    
    public SvtConfigurationLoader() {
        // FIXME: Configuration hard-coded here.
        new ResourceConfiguration(
                "/org/hps/conditions/config/conditions_dev.xml",
                "/org/hps/conditions/config/conditions_dev_local.properties").setup();
        manager = DatabaseConditionsManager.getInstance();
        manager.openConnection();
    }
    
    /**
     * Insert an SVT XML configuration file into the conditions database. 
     * @param file The XML configuration file.
     */
    public void insert(File file) {        
        PreparedStatement ps = null;         
        Connection con = manager.getConnection();
        try {
            ps = con.prepareStatement(INSERT_SQL);
            Integer collectionID = manager.getNextCollectionID(TableConstants.SVT_CONFIGURATIONS);
            ps.setString(1, collectionID.toString());
            ps.setString(2, file.getName());
            ps.setBinaryStream(3, new FileInputStream(file));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            manager.closeConnection();
        }
    }    
}