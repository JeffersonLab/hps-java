package org.lcsim.geometry.compact.converter.lcdd;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.util.test.TestUtil.TestOutputFile;

public class HPSEcal4LCDDTest extends TestCase {

    public HPSEcal4LCDDTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return new TestSuite(HPSEcal4LCDDTest.class);
    }

    public void test_converter() throws Exception {
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        mgr.setDetector("HPS-PhysicsRun2016-Nominal-v4-4", 0); /*
         * any run number and detector will work here
         */

        //InputStream in = HPSTestRunTracker2014.class.getResourceAsStream("/org/lcsim/geometry/subdetector/HPSEcal4Test.xml");
        InputStream in = this.getClass().getResourceAsStream("/org/lcsim/geometry/subdetector/HPSEcal4Test.xml");

        OutputStream out = new BufferedOutputStream(new FileOutputStream(new TestOutputFile("HPSEcal4Test.lcdd")));
        new Main().convert("HPSEcal4Test", in, out);
    }
}
