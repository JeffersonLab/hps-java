package org.lcsim.geometry.subdetector;

import java.io.InputStream;

import junit.framework.TestCase;

import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.GeometryReader;

public class FieldMapTest extends TestCase {
    
    private static final String[] DETECTORS = {        
        "HPS-EngRun2015-1_5mm-v3-4-fieldmap",
        "HPS-EngRun2015-1_5mm-v4-4-fieldmap",
        "HPS-EngRun2015-1_5mm-v5-0-fieldmap",
        "HPS-EngRun2015-Nominal-v2-fieldmap",
        "HPS-EngRun2015-Nominal-v3-1-fieldmap",
        "HPS-EngRun2015-Nominal-v3-2-fieldmap",
        "HPS-EngRun2015-Nominal-v3-3-fieldmap",
        "HPS-EngRun2015-Nominal-v3-4-fieldmap",
        "HPS-EngRun2015-Nominal-v3-5-0-fieldmap",
        "HPS-EngRun2015-Nominal-v3-5-1-fieldmap",
        "HPS-EngRun2015-Nominal-v3-5-2-fieldmap",
        "HPS-EngRun2015-Nominal-v3-5-3-fieldmap",
        "HPS-EngRun2015-Nominal-v3-fieldmap",
        "HPS-EngRun2015-Nominal-v4-4-fieldmap",
        "HPS-EngRun2015-Nominal-v5-0-fieldmap",
        "HPS-EngRun2015-Nominal-v5-0-twk-fieldmap",
        "HPS-EngRun2015-Nominal-v5-fieldmap",
        "HPS-EngRun2015-Nominal-v6-0-fieldmap",
        "HPS-PhysicsRun2016-Nominal-v4-4-fieldmap",
        "HPS-PhysicsRun2016-Nominal-v5-0-4pt4-fieldmap",
        "HPS-PhysicsRun2016-Nominal-v5-0-6pt6-fieldmap",
        "HPS-PhysicsRun2016-Nominal-v5-0-fieldmap",
        "HPS-PhysicsRun2016-v5-3-fieldmap_globalAlign",
        "HPS-Proposal2014-v4-fieldmap",
        "HPS-Proposal2017-L0-v3-1pt05-fieldmap",
        "HPS-Proposal2017-L0-v3-2pt3-fieldmap",
        "HPS-Proposal2017-L0-v3-4pt4-fieldmap",
        "HPS-Proposal2017-L0-v3-6pt6-fieldmap",
        "HPS-Proposal2017-Nominal-v2-1pt05-fieldmap",
        "HPS-Proposal2017-Nominal-v2-2pt3-fieldmap",
        "HPS-Proposal2017-Nominal-v2-4pt4-fieldmap",
        "HPS-Proposal2017-Nominal-v2-6pt6-fieldmap"
    };
    
    public void testFieldMap() throws Exception {
        GeometryReader geometryReader = new GeometryReader();
        InputStream in = this.getClass().getResourceAsStream("/org/lcsim/geometry/subdetector/FieldMapTest.xml");
        Detector det = geometryReader.read(in);
        FieldMap fieldmap = det.getFieldMap();
        double[] pos = new double[3];
        double[] field = null;
        for (double z = 0.; z < 1000.; z += 10.) {
            pos[2] = z;
            field = fieldmap.getField(pos);
            System.out.println("B-field at z = " + z + " is [ " + field[0] + ", " + field[1] + ", " + field[2] + "] ");
        }
    } 
    
    public void testDetectors() throws Exception {
        GeometryReader geometryReader = new GeometryReader();
        geometryReader.setBuildDetailed(false);
        for (String name : DETECTORS) {
            System.out.println("Reading fieldmap detector: " + name);
            System.out.flush();
            InputStream in = this.getClass().getResourceAsStream("/" + name + "/compact.xml"); 
            try {
                geometryReader.read(in);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            System.out.println();
        }
    }
}