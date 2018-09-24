package org.hps.analysis.geometry;

import java.util.Set;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.data.detectors.DetectorDataResources;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;

/**
 *
 * @author Norman Graf
 */
public class TestFieldMap {

    public static void main(String[] args) throws Exception {
        boolean debug = false;
        String detectorName = "HPS-EngRun2015-Nominal-v6-0-fieldmap";
        if (args.length > 0) {
            detectorName = args[0];
        }
        Set<String> availableDetectors = DetectorDataResources.getDetectorNames();
        boolean foundIt = false;
        for (String s : availableDetectors) {
            if (detectorName.equals(s)) {
                foundIt = true;
            }
        }
        if (!foundIt) {
            throw new RuntimeException("Detector name " + detectorName + " not found.");
        }
        final DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.setDetector(detectorName, 5772);
        Detector detector = manager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
        System.out.println(detector.getName());
        FieldMap bFieldMap = detector.getFieldMap();
        double[] pos = new double[3];
        double[] val = new double[3];
        bFieldMap.getField(pos, val);
        for (int i = -500; i < 1300; ++i) {
            pos[2] = i;
            bFieldMap.getField(pos, val);
            //System.out.println(Arrays.toString(pos) + " " + Arrays.toString(val));
            System.out.println(i + " " + val[1]);
        }
    }

}
