package org.hps.recon.tracking.ztrack;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class SimpleDetectorTest extends TestCase {

    /**
     * Test of addDetectorPlane method, of class SimpleDetector.
     */
    public void testSimpleDetector() {
        SimpleDetector det = new SimpleDetector();

        //create a few MaterialInfo objects...
        double[] zees = {20., 10., 5., 11., 37., 52., 23., 1., 44., 40.};
        int numDet = zees.length;
        for (int i = 0; i < numDet; ++i) {
            MaterialInfo m = new MaterialInfo();
            double z = zees[i];
            System.out.println(z);
            m.SetZpos(z);
            det.addDetectorPlane(m);
        }
        System.out.println("");
        List<MaterialInfo> planes = det.getPlanes();
        assertEquals(numDet, planes.size());

        assertEquals(det.zMin(), 1.);
        assertEquals(det.zMax(), 52.);
        double ztmp = det.zMin();
        for (MaterialInfo m : planes) {
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

    }
}
