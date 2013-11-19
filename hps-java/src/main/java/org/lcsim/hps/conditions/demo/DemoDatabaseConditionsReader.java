package org.lcsim.hps.conditions.demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;

/**
 * HPS-specific conditions reader. Wraps around any standard reader and adds an
 * ability to fetch calibration data from the database.
 * <p>
 * TEMPORARY IMPLEMENTATION - DEMO.
 * 
 * @author onoprien
 * @version $Id: DemoDatabaseConditionsReader.java,v 1.3 2013/09/23 18:19:52 jeremy Exp $
 */
public class DemoDatabaseConditionsReader extends ConditionsReader {

    // -- Private parts :
    // ----------------------------------------------------------

    static ConditionsConverter _converterCalibration = new CalibrationConverter();

    private final ConditionsReader _reader;

    private String _detectorName;
    private int _minRun = Integer.MAX_VALUE;
    private int _maxRun = Integer.MIN_VALUE;

    private final HashMap<String, byte[]> _propCache = new HashMap<String, byte[]>();

    // -- Construction and initialization :
    // ----------------------------------------

    public DemoDatabaseConditionsReader(ConditionsReader reader) {
        _reader = reader;
    }

    // -- Updating to a new detector/run :
    // -----------------------------------------

    public boolean update(ConditionsManager manager, String detectorName, int run) throws IOException {
        
        if (_detectorName == null) {
            _detectorName = detectorName;
        } else {
            if (!_detectorName.equals(detectorName))
                throw new IllegalArgumentException();
        }
        if (run <= _maxRun && run >= _minRun)
            return false;
        manager.registerConditionsConverter(_converterCalibration);

        _propCache.clear();

        // Open connection to the db

        Properties connectionProps = new Properties();
        connectionProps.put("user", "rd_hps_cond_ro");
        connectionProps.put("password", "2jumpinphotons.");
        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:mysql://mysql-node03.slac.stanford.edu:3306/", connectionProps);
        } catch (SQLException x) {
            throw new IOException("Failed to connect to database", x);
        }

        // Fetch whatever database-kept conditions should be accessible through
        // ConditionsSet interface.
        // This needs to be optimized once the db structure and data requirement
        // are known.
        // (can be templated if the db structure is standardized)

        String query = "SELECT data_ident, run_start, run_end FROM rd_hps_cond.conditions_test WHERE " + "run_start <= " + run + " AND run_end >= " + run + " AND level = 'PROD'";

        Statement stmt = null;
        int count = 0;
        String data = null;
        try {
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                count++;
                data = rs.getString(1);
                _minRun = rs.getInt(2);
                _maxRun = rs.getInt(3);
                
                System.out.println("data, minRun, maxRun: " + data + ", " +_minRun + ", " + _maxRun);
            }
        } catch (SQLException x) {
            throw new IOException("Failed to execute query", x);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException x) {
                    throw new IOException("Failed to close statement", x);
                }
            }
        }

        // Need to get requirements from HPS people - how is this case supposed
        // to be handled ?
        if (count != 1)
            throw new IOException("Found " + count + " valid calibrations");

        Properties p = new Properties();
        String[] d = data.trim().split(":");
        try {
            p.put("table", d[0]);
            p.put("column", d[1]);
            p.put("id", d[2]);
        } catch (IndexOutOfBoundsException x) {
            throw new IOException("Illegal data_ident format", x);
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        p.store(stream, null);
        _propCache.put("calibration", stream.toByteArray());

        // Close connection to the db

        try {
            con.close();
        } catch (SQLException x) {
            throw new IOException("Failed to close connection", x);
        }

        return true;
    }

    // -- Implementing ConditionsReader :
    // ------------------------------------------

    public void close() throws IOException {
        _reader.close();
    }

    public InputStream open(String name, String type) throws IOException {
        byte[] ba = _propCache.get(name);
        if (ba == null) {
            return _reader.open(name, type);
        } else {
            return new ByteArrayInputStream(ba);
        }
    }

    // -----------------------------------------------------------------------------
}
