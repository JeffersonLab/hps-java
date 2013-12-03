package org.hps.conditions;

import java.io.IOException;
import java.io.InputStream;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;

import org.hps.conditions.DatabaseConditionsReader;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: TestRunConditionsReader.java,v 1.4 2013/10/17 23:04:19 jeremy Exp $
 */
public class TestRunConditionsReader extends DatabaseConditionsReader {

    private String detectorName = null;
    //private int run;

    public TestRunConditionsReader(ConditionsReader reader) {
        super(reader);
    }

    public InputStream open(String name, String type) throws IOException {
        // 1) Check the detector base directory.
        InputStream in = getClass().getResourceAsStream("/" + detectorName + "/" + name + "." + type);
        if (in == null) {
            // 2) Check for embedded jar resources.
            in = getClass().getResourceAsStream("/org/lcsim/hps/calib/testrun/" + name + "." + type);
            if (in == null) {
                // 3) Use super's open method.
                in = super.open(name, type);
                
                // If all of these failed to find conditions, then something went wrong.
                if (in == null) {
                    throw new IOException("Conditions " + name + " for " + detectorName + " with type " + type + " were not found");
                }       
            }
        }
        return in;
    }

    public void close() throws IOException {
    }


    public boolean update(ConditionsManager manager, String detectorName, int run) throws IOException {
//            loadCalibsByRun(run);
//        Detector detector = manager.getCachedConditions(Detector.class,"compact.xml").getCachedData();
//        HPSEcalConditions.detectorChanged(detector, "Ecal");
//        HPSSVTCalibrationConstants.loadCalibration(run);
        this.detectorName = detectorName;
        //this.run = run;
        super.update(manager, detectorName, run);
//        System.out.println(detectorName+run);
        return true;
    }
//        private void loadCalibsByRun(int run) {
//        HPSSVTCalibrationConstants.loadCalibration(run);
//        FieldMap.loadFieldMap(run);
//    }
}
