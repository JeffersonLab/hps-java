package org.hps.recon.tracking.lit;

import junit.framework.TestCase;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;

/**
 *
 * @author ngraf
 */
public class DetectorPlaneTest extends TestCase
{

    public void testIt()
    {
        double z = 37.;
        double x0 = .1;
        CartesianThreeVector pos = new CartesianThreeVector(0., 0., z);
        CartesianThreeVector eta = new CartesianThreeVector(0., 0., 1.);

        DetectorPlane p = new DetectorPlane("p1", pos, eta, x0, 0.);
        double measDim = 10.;
        double unMeasDim = 20.;
        p.setMeasuredDimension(measDim);
        p.setUnMeasuredDimension(unMeasDim);
        System.out.println(p);

        //boolean in = p.inBounds(15, 0, z);
    }

    public void testHpsDetector() throws Exception
    {
        String hpsDetectorName = "HPS-EngRun2015-Nominal-v2-fieldmap";
        DatabaseConditionsManager cm = DatabaseConditionsManager.getInstance();
        cm.setDetector(hpsDetectorName, 0);
        Detector det = cm.getDetectorObject();
        HpsDetector hpsdet = new HpsDetector(det);
        System.out.println(hpsdet);
        
        // let's pick a plane
        String planeName = "module_L1t_halfmodule_axial_sensor0";
        DetectorPlane dPlane = hpsdet.getPlane(planeName);
        System.out.println(dPlane);
        CartesianThreeVector o = dPlane.position();
        boolean isInBounds = dPlane.inBounds(o.x(), o.y(), o.z());
        // origin should be in bounds
        System.out.println(isInBounds);
        assertTrue(isInBounds);
        double dy = dPlane.getMeasuredDimension()/2.;
        // slightly more than half the dimension should be out of bounds
        isInBounds = dPlane.inBounds(o.x(), o.y()+1.01*dy, o.z());
        System.out.println(isInBounds);
        assertFalse(isInBounds);
        // slighly less than half the dimension should be in bounds
        isInBounds = dPlane.inBounds(o.x(), o.y()+0.99*dy, o.z());
        System.out.println(isInBounds);
        assertTrue(isInBounds);
    }
}
