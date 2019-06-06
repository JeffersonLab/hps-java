package org.hps.recon.tracking.ztrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 *
 * @author ngraf
 */
public class HpsDetectorNavigatorTest extends TestCase {

    /**
     * Test of HpsDetectorNavigator.
     */
    public void testIt() {
        HpsDetector det = new HpsDetector();
        HpsDetectorNavigator nav = new HpsDetectorNavigator(det);

        //create a few MaterialInfo objects...
        double[] zees = {20., 10., 5., 11., 37., 52., 23., 1., 44., 40.};
        int numDet = zees.length;
        for (int i = 0; i < numDet; ++i) {
            DetectorPlane m = new DetectorPlane();
            double z = zees[i];
//            System.out.println(z);
            m.SetZpos(z);
            det.addDetectorPlane(m);
        }

        List<DetectorPlane> planes = det.getPlanes();

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

        ZTrackParam par = new ZTrackParam();
        par.SetZ(0.);

        List<DetectorPlane> intersections = new ArrayList<DetectorPlane>();

        double zOut = 41.;

        System.out.println("Propagating forward between " + par.GetZ() + " and " + zOut);
        Status stat = nav.FindIntersections(par, zOut, intersections);
        assertTrue(stat == Status.SUCCESS);
        assertEquals(intersections.size(), 8);
        for (DetectorPlane m : intersections) {
            double z = m.GetZpos();
            assertTrue(z > par.GetZ());
            assertTrue(z < zOut);
            System.out.println(z);
        }

        intersections.clear();
        par.SetZ(17.);
        zOut = 1.;

        System.out.println("\n\n\n\n");
        System.out.println("Propagating backwards between " + par.GetZ() + " and " + zOut);

        stat = nav.FindIntersections(par, zOut, intersections);
        assertTrue(stat == Status.SUCCESS);
        assertEquals(intersections.size(), 4);
        for (DetectorPlane m : intersections) {
            double z = m.GetZpos();
            assertTrue(z < par.GetZ());
            assertTrue(z >= zOut);
            System.out.println(z);
        }
    }
}
