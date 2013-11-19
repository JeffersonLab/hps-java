package org.lcsim.hps.conditions.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;

import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsSet;

/**
 * <tt>ConditionsConverter</tt> for {@link Calibration} class.
 * <p>
 * TEMPORARY IMPLEMENTATION - DEMO.
 *
 * @author onoprien
 * @version $Id: CalibrationConverter.java,v 1.1 2013/09/18 02:33:16 jeremy Exp $
 */
public class CalibrationConverter implements ConditionsConverter<Calibration> {

  public Calibration getData(ConditionsManager manager, String name) {
    Connection con = null;
    Statement stmt = null;
    try {
      ConditionsSet cs = manager.getConditions("calibration");
      Properties connectionProps = new Properties();
      connectionProps.put("user", "rd_hps_cond_ro");
      connectionProps.put("password", "2jumpinphotons.");
      con = DriverManager.getConnection("jdbc:mysql://mysql-node03.slac.stanford.edu:3306/", connectionProps);
      String query = "SELECT channel_id, aValue FROM rd_hps_cond."+ cs.getString("table") +" WHERE "+
                     cs.getString("column") +" = "+ cs.getString("id");
      stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      HashMap<Integer, Double> data = new HashMap<Integer, Double>();
      int maxChannel = -1;
      while (rs.next()) {
        int channel_id = rs.getInt(1);
        if (channel_id > maxChannel) maxChannel = channel_id;
        double aValue = rs.getDouble(2);
        data.put(channel_id, aValue);
      }
      return new Calibration(data, maxChannel);
    } catch (SQLException x) {
      if (stmt != null) {
        try {stmt.close();} catch (Exception xx) {}
      }
      if (con != null) {
        try {con.close();} catch (Exception xx) {}
      }
      throw new RuntimeException("Database error", x);
    }
  }

  public Class getType() {
    return Calibration.class;
  }
  
}
