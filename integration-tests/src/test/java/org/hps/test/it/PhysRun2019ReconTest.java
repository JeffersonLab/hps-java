package org.hps.test.it;

import java.io.File;

import org.hps.evio.EvioToLcio;
import org.hps.util.test.TestOutputFile;
import org.hps.util.test.TestUtil;

import junit.framework.TestCase;

/**
 * Runs the 2019 reconstruction
 */
public class PhysRun2019ReconTest extends TestCase {

    static final String testFileName = "hps_010104.00000_1000events.evio";
    static final String detectorName = "HPS-PhysicsRun2019-v1-4pt5";
    static final String steeringFileName = "/org/hps/steering/recon/PhysicsRun2019FullRecon.lcsim";
    private final int nEvents = 1000;

    public void testIt() throws Exception {
        File evioInputFile = TestUtil.downloadTestFile(testFileName);
        File outputFile = new TestOutputFile(PhysRun2019ReconTest.class, "PhysRun2019ReconTest");
        String args[] = {"-r", "-x", steeringFileName, "-d",
            detectorName, "-D", "outputFile=" + outputFile.getPath(), "-n", String.format("%d", nEvents),
            evioInputFile.getPath()};
        EvioToLcio.main(args);
    }
}
