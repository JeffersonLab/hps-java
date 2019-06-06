package org.hps.recon.tracking.ztrack;

import java.util.Arrays;
import junit.framework.TestCase;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;

/**
 *
 * @author ngraf
 */
public class MagneticFieldInterpolationTest extends TestCase {

    String hpsDetectorName = "HPS-PhysicsRun2019-v1-4pt5";

    public void testIt() throws Exception {
        DatabaseConditionsManager cm = DatabaseConditionsManager.getInstance();
        cm.setDetector(hpsDetectorName, 0);
        Detector det = cm.getDetectorObject();
        FieldMap map = det.getFieldMap();
        double xoffset = 21.17; //mm
        double yoffset = 0.0; //mm
        double zoffset = 457.2; //mm
        double[] field = new double[3];
        double[] pos = new double[3];
        map.getField(new double[]{xoffset, yoffset, zoffset}, field);
        System.out.println("Field at magnet center "+Arrays.toString(field));
        map.getField(pos, field);
        System.out.println("Field at HPS origin "+Arrays.toString(field));
    }
}
