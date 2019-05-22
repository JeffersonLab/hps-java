package org.hps.recon.tracking.ztrack;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;

/**
 *
 * @author ngraf
 */
public class HpsDetectorTest extends TestCase {

    public void testIt() {
        HpsDetector det = new HpsDetector();
//        double fieldValue = -.5;
//        det.setMagneticField(new ConstantMagneticField(0., fieldValue, 0.));
        //create a few dummy DetectorPlane objects...
        double[] zees = {20., 10., 5., 11., 37., 52., 23., 1., 44., 40.};
        int numDet = zees.length;
        for (int i = 0; i < numDet; ++i) {
            DetectorPlane m = new DetectorPlane();
            double z = zees[i];
            System.out.println(z);
            m.SetZpos(z);
            det.addDetectorPlane(m);
        }
        System.out.println("");
        List<DetectorPlane> planes = det.getPlanes();
        assertEquals(numDet, planes.size());

        assertEquals(det.zMin(), 1.);
        assertEquals(det.zMax(), 52.);
        double ztmp = det.zMin();
        for (DetectorPlane m : planes) {
            assertTrue(m.GetZpos() >= ztmp);
            ztmp = m.GetZpos();
            System.out.println(planes.indexOf(m) + " : " + m.GetZpos());
        }

        // going forward...
        double zStart = 37.;
        double zEnd = 52.;
        int[] indices = new int[2];
        det.indicesInRange(zStart, zEnd, indices);
        System.out.println(Arrays.toString(indices));
        assertEquals(indices[0], 7);
        assertEquals(indices[1], 9);

        //reverse
        zStart = 23.;
        zEnd = 5.0;
        det.indicesInRange(zStart, zEnd, indices);
        System.out.println(Arrays.toString(indices));
        assertEquals(indices[0], 4);
        assertEquals(indices[1], 1);

        // test field
//        CbmLitField field = det.magneticField();
//        double[] b = new double[3];
//        field.GetFieldValue(0., 0., 0., b);
//        assertEquals(b[0], 0.);
//        assertEquals(b[1], fieldValue);
//        assertEquals(b[2], 0.);
        // now for the real HPS detector...
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        String[] detectors = {"HPS-EngRun2015-Nominal-v0","HPS-PhysicsRun2019-v1-4pt5"};
        for (String detectorName : detectors) {
            try {
                manager.setDetector(detectorName, 5772);
            } catch (ConditionsManager.ConditionsNotFoundException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
            Detector d = manager.getDetectorObject();//getCachedConditions(Detector.class, "compact.xml").getCachedData();

            System.out.println(d.getName());

            HpsDetector fullHpsDetector = new HpsDetector(d);
            System.out.println("Full HpsDetector for " + detectorName + " " + fullHpsDetector);
        }
    }
}
